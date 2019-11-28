package eu.europa.esig.dss.ws.validation.common;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.Enumeration;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.RpcClient;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.model.pades.DSSJavaFont;
import eu.europa.esig.dss.model.pades.SignatureImageParameters;
import eu.europa.esig.dss.model.pades.SignatureImageParameters.VisualSignaturePagePlacement;
import eu.europa.esig.dss.model.pades.SignatureImageTextParameters;
import eu.europa.esig.dss.model.pades.SignatureImageTextParameters.SignerTextHorizontalAlignment;
import eu.europa.esig.dss.model.pades.SignatureImageTextParameters.SignerTextPosition;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.xades.XAdESSignatureParameters;
import eu.europa.esig.dss.xades.signature.XAdESService;

public class ReportSigner {

    private static final Logger logger = LoggerFactory.getLogger(ReportSigner.class);
    
    private PAdESService padesService;
    
    private XAdESService xadesService;
    
    private String signingCertificateJksPath;
    
    private String signingCertificateJksPass;
    
    private X509Certificate signingCertificate;
    private Certificate[] signingCertificateChain;
    
    private Connection amqpConnection;
    
    private String rabbitMqExchange;
    
    private String rabbitMqRoutingKey;
    
    private String signatureImageDir;
    
    private ObjectMapper objectMapper = new ObjectMapper();
    
    public void init() throws IOException, CertificateException {
        if (StringUtils.isEmpty(signingCertificateJksPath)) {
            return;
        }
        
        try {
            KeyStore store = KeyStore.getInstance("JKS");
            store.load(new FileInputStream(signingCertificateJksPath), signingCertificateJksPass.toCharArray());
            Enumeration<String> aliases = store.aliases();
            signingCertificate = (X509Certificate) store.getCertificate(aliases.nextElement());
            signingCertificateChain = new Certificate[store.size() - 1];
            int i = 0;
            while (aliases.hasMoreElements()) {
                signingCertificateChain[i++] = store.getCertificate(aliases.nextElement());
            }
            
        } catch (Exception ex) {
            logger.warn("Failed to find validation certificate from path " + signingCertificateJksPath, ex);
        }
    }
    
    public void signReportXml(String xml, OutputStream outputStream, String sessionId) throws IOException {
        if (xml == null) {
            return;
        }
        XAdESSignatureParameters params = new XAdESSignatureParameters();
        params.setDigestAlgorithm(DigestAlgorithm.SHA256);
        params.setSignatureLevel(SignatureLevel.XAdES_BASELINE_LT);
        params.setSignaturePackaging(SignaturePackaging.ENVELOPED);
        params.setValidationReportSigning(true);
        if (signingCertificateChain != null) {
            params.setCertificateChain(Stream.of(signingCertificateChain).map(c -> new CertificateToken((X509Certificate) c)).collect(Collectors.toList()));
        }
        if (signingCertificate != null) {
            params.setSigningCertificate(new CertificateToken(signingCertificate));
        }
        
        DSSDocument document = new InMemoryDocument(xml.getBytes(StandardCharsets.UTF_8));
        if (amqpConnection != null) {
            ToBeSigned toBeSigned = xadesService.getDataToSign(document, params);
            SignatureValue signature = new SignatureValue();
            signature.setAlgorithm(SignatureAlgorithm.RSA_SHA256);
            
            byte[] signatureValue = signRemotely(toBeSigned.getBytes(), sessionId + ":" + UUID.randomUUID());
            signature.setValue(signatureValue);
            DSSDocument signed = xadesService.signDocument(document, params, signature);
            IOUtils.copy(signed.openStream(), outputStream);
        } else {
            IOUtils.copy(document.openStream(), outputStream);
        }
    }
    
    public void signReport(byte[] byteArray, OutputStream outputStream, String sessionId) throws IOException {
        PAdESSignatureParameters params = new PAdESSignatureParameters();
        params.bLevel().setTrustAnchorBPPolicy(true);
        params.bLevel().setSigningDate(new Date());
        params.setDigestAlgorithm(DigestAlgorithm.SHA256);
        params.setSignWithExpiredCertificate(false);
        params.setSignatureLevel(SignatureLevel.PAdES_BASELINE_LTA);
        params.setSignaturePackaging(SignaturePackaging.ENVELOPED);
        params.setValidationReportSigning(true);
        
        if (signingCertificateChain != null) {
            params.setCertificateChain(Stream.of(signingCertificateChain).map(c -> new CertificateToken((X509Certificate) c)).collect(Collectors.toList()));
        }
        if (signingCertificate != null) {
            params.setSigningCertificate(new CertificateToken(signingCertificate));
        }
        
        SignatureImageParameters signatureParams = createImageParams();
        signatureParams.setPagePlacement(VisualSignaturePagePlacement.SINGLE_PAGE);
        signatureParams.setPage(-1);
        params.setSignatureImageParameters(signatureParams);
        
        DSSDocument document = new InMemoryDocument(byteArray);
        if (amqpConnection != null) {
            ToBeSigned toBeSigned = padesService.getDataToSign(document, params);
            SignatureValue signature = new SignatureValue();
            signature.setAlgorithm(SignatureAlgorithm.RSA_SHA256);
            
            byte[] signatureValue = signRemotely(toBeSigned.getBytes(), sessionId + ":" + UUID.randomUUID());
            signature.setValue(signatureValue);
            DSSDocument signed = padesService.signDocument(document, params, signature);
            IOUtils.copy(signed.openStream(), outputStream);
        } else {
            IOUtils.copy(document.openStream(), outputStream);
        }
    }

    private byte[] signRemotely(byte[] bytes, String transactionId) {
        try {
            byte[] hashBytes = DigestUtils.sha256(bytes);
            Channel channel = amqpConnection.createChannel();
            RpcClient rpcClient = new RpcClient(channel, rabbitMqExchange, rabbitMqRoutingKey);
            String hash = Base64.getMimeEncoder().encodeToString(hashBytes);
            String request = "{\"documentHash\":\"" + hash + "\",\"transactionId\":\"" + transactionId + "\"}";
            logger.info("Calling RabbitMQ for remote signing with request={}", request);
            String response = rpcClient.stringCall(request);
            logger.info("Response received: " + response);
            String signedHash = objectMapper.readTree(response).get("documentHash").asText();
            logger.info("Hash received: " + signedHash);
            return Base64.getMimeDecoder().decode(signedHash);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private SignatureImageParameters createImageParams() {
        SignatureImageParameters imageParams = new SignatureImageParameters();
        try {
            imageParams.setImageDocument(FileUtils.readFileToByteArray(new File(signatureImageDir + "Evrotrust_background.png")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        imageParams.setxAxis(230);
        imageParams.setyAxis(-65);
        imageParams.setWidth(140);
        imageParams.setZoom(100);
        imageParams.setTextParameters(new SignatureImageTextParameters());
        imageParams.getTextParameters().setSignerTextPosition(SignerTextPosition.FOREGROUND);
        imageParams.getTextParameters().setSignerTextHorizontalAlignment(SignerTextHorizontalAlignment.RIGHT);
        imageParams.getTextParameters().setPadding(4);
        imageParams.getTextParameters().setText("Evrotrust Qualified\nValidation Service");
        imageParams.getTextParameters().setFont(new DSSJavaFont("helvetica", Font.PLAIN, 18));
        imageParams.getTextParameters().setBackgroundColor(new Color(255, 255, 255, 0));
        
        imageParams.setTextRightParameters(new SignatureImageTextParameters());
        imageParams.getTextRightParameters().setText("Digitally Signed by Evrotrust\nQualified Validation Authority.\nQualified Time stamped.\n" + 
                "Compliant with eIDAS.");
        imageParams.getTextRightParameters().setSignerTextPosition(SignerTextPosition.FOREGROUND);
        imageParams.getTextRightParameters().setFont(new DSSJavaFont("helvetica", Font.PLAIN, 12));
        imageParams.setDateFormat("dd.MM.yyyy HH:mm:ss XXX''");
        imageParams.getTextRightParameters().setBackgroundColor(new Color(255, 255, 255, 0));
        
        return imageParams;
    }
    
    public void setPadesService(PAdESService padesService) {
        this.padesService = padesService;
    }
    
    public void setXadesService(XAdESService xadesService) {
        this.xadesService = xadesService;
    }

    public void setSigningCertificateJksPath(String signingCertificateJksPath) {
        this.signingCertificateJksPath = signingCertificateJksPath;
    }

    public void setSigningCertificateJksPass(String signingCertificateJksPass) {
        this.signingCertificateJksPass = signingCertificateJksPass;
    }

    public void setSigningCertificate(X509Certificate signingCertificate) {
        this.signingCertificate = signingCertificate;
    }

    public void setSigningCertificateChain(Certificate[] signingCertificateChain) {
        this.signingCertificateChain = signingCertificateChain;
    }

    public void setAmqpConnection(Connection amqpConnection) {
        this.amqpConnection = amqpConnection;
    }

    public void setRabbitMqExchange(String rabbitMqExchange) {
        this.rabbitMqExchange = rabbitMqExchange;
    }

    public void setRabbitMqRoutingKey(String rabbitMqRoutingKey) {
        this.rabbitMqRoutingKey = rabbitMqRoutingKey;
    }

    public void setSignatureImageDir(String signatureImageDir) {
        this.signatureImageDir = signatureImageDir;
    }
    
}
