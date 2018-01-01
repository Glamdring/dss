package eu.europa.esig.dss.signature;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logsentinel.ApiCallbackAdapter;
import com.logsentinel.ApiException;
import com.logsentinel.LogSentinelClient;
import com.logsentinel.client.model.ActionData;
import com.logsentinel.client.model.ActorData;
import com.logsentinel.client.model.AuditLogEntryType;
import com.logsentinel.client.model.LogResponse;

import eu.europa.esig.dss.ASiCContainerType;
import eu.europa.esig.dss.AbstractSignatureParameters;
import eu.europa.esig.dss.DSSDocument;
import eu.europa.esig.dss.DSSException;
import eu.europa.esig.dss.DSSUtils;
import eu.europa.esig.dss.InMemoryDocument;
import eu.europa.esig.dss.RemoteCertificate;
import eu.europa.esig.dss.RemoteDocument;
import eu.europa.esig.dss.RemoteSignatureParameters;
import eu.europa.esig.dss.SignatureForm;
import eu.europa.esig.dss.asic.ASiCWithCAdESSignatureParameters;
import eu.europa.esig.dss.asic.ASiCWithXAdESSignatureParameters;
import eu.europa.esig.dss.cades.CAdESSignatureParameters;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.x509.CertificateToken;
import eu.europa.esig.dss.xades.XAdESSignatureParameters;

public class AbstractRemoteSignatureServiceImpl {

    private LogSentinelClient logSentinelClient;
    
    private static final Logger LOG = LoggerFactory.getLogger(AbstractRemoteSignatureServiceImpl.class);
    
	protected AbstractSignatureParameters getASiCSignatureParameters(AbstractSignatureParameters parameters, ASiCContainerType asicContainerType,
			SignatureForm signatureForm) {
		switch (signatureForm) {
		case CAdES:
			ASiCWithCAdESSignatureParameters asicWithCAdESParameters = new ASiCWithCAdESSignatureParameters();
			asicWithCAdESParameters.aSiC().setContainerType(asicContainerType);
			parameters = asicWithCAdESParameters;
			break;
		case XAdES:
			ASiCWithXAdESSignatureParameters asicWithXAdESParameters = new ASiCWithXAdESSignatureParameters();
			asicWithXAdESParameters.aSiC().setContainerType(asicContainerType);
			parameters = asicWithXAdESParameters;
			break;
		default:
			throw new DSSException("Unrecognized format (XAdES or CAdES are allowed with ASiC) : " + signatureForm);
		}
		return parameters;
	}

	protected AbstractSignatureParameters createParameters(RemoteSignatureParameters remoteParameters) {
		AbstractSignatureParameters parameters = null;
		ASiCContainerType asicContainerType = remoteParameters.getAsicContainerType();
		SignatureForm signatureForm = remoteParameters.getSignatureLevel().getSignatureForm();
		if (asicContainerType != null) {
			parameters = getASiCSignatureParameters(parameters, asicContainerType, signatureForm);
		} else {
			switch (signatureForm) {
			case CAdES:
				parameters = new CAdESSignatureParameters();
				break;
			case PAdES:
				PAdESSignatureParameters padesParams = new PAdESSignatureParameters();
				padesParams.setSignatureImageParameters(remoteParameters.getSignatureImageParameters());
				padesParams.setStampImageParameters(remoteParameters.getStampImageParameters());
				padesParams.setSignatureSize(9472 * 2); // double reserved space for signature
				parameters = padesParams;
				break;
			case XAdES:
				parameters = new XAdESSignatureParameters();
				break;
			default:
				throw new DSSException("Unsupported signature form : " + signatureForm);
			}
		}

		fillParameters(parameters, remoteParameters);

		return parameters;
	}

	protected void fillParameters(AbstractSignatureParameters parameters, RemoteSignatureParameters remoteParameters) {
		parameters.setBLevelParams(remoteParameters.bLevel());
		parameters.setDetachedContents(createDSSDocuments(remoteParameters.getDetachedContents()));
		parameters.setDigestAlgorithm(remoteParameters.getDigestAlgorithm());
		parameters.setEncryptionAlgorithm(remoteParameters.getEncryptionAlgorithm());
		parameters.setSignatureLevel(remoteParameters.getSignatureLevel());
		parameters.setSignaturePackaging(remoteParameters.getSignaturePackaging());
		parameters.setSignatureTimestampParameters(remoteParameters.getSignatureTimestampParameters());
		parameters.setArchiveTimestampParameters(remoteParameters.getArchiveTimestampParameters());
		parameters.setContentTimestampParameters(remoteParameters.getContentTimestampParameters());
		parameters.setSignWithExpiredCertificate(remoteParameters.isSignWithExpiredCertificate());

		RemoteCertificate signingCertificate = remoteParameters.getSigningCertificate();
		if (signingCertificate != null) { // extends do not require signing certificate
			CertificateToken loadCertificate = DSSUtils.loadCertificate(signingCertificate.getEncodedCertificate());
			parameters.setSigningCertificate(loadCertificate);
		}

		List<RemoteCertificate> remoteCertificateChain = remoteParameters.getCertificateChain();
		if (Utils.isCollectionNotEmpty(remoteCertificateChain)) {
			List<CertificateToken> certificateChain = new LinkedList<CertificateToken>();
			for (RemoteCertificate remoteCertificate : remoteCertificateChain) {
				certificateChain.add(DSSUtils.loadCertificate(remoteCertificate.getEncodedCertificate()));
			}
			parameters.setCertificateChain(certificateChain);
		}
	}

	protected List<DSSDocument> createDSSDocuments(List<RemoteDocument> remoteDocuments) {
		if (Utils.isCollectionNotEmpty(remoteDocuments)) {
			List<DSSDocument> dssDocuments = new ArrayList<DSSDocument>();
			for (RemoteDocument remoteDocument : remoteDocuments) {
				dssDocuments.add(createDSSDocument(remoteDocument));
			}
			return dssDocuments;
		}
		return null;
	}

	protected InMemoryDocument createDSSDocument(RemoteDocument remoteDocument) {
		if (remoteDocument != null) {
			InMemoryDocument dssDocument = new InMemoryDocument(remoteDocument.getBytes());
			dssDocument.setMimeType(remoteDocument.getMimeType());
			dssDocument.setAbsolutePath(remoteDocument.getAbsolutePath());
			dssDocument.setName(remoteDocument.getName());
			return dssDocument;
		}
		return null;
	}
	
	public void setLogSentinelClient(LogSentinelClient logSentinelClient) {
        this.logSentinelClient = logSentinelClient;
    }
    
	/**
	 * Send audit trail information to the LogSentinel audit trail service for secure storing
	 * 
	 * @param document
	 * @param params
	 */
	protected void logSigningRequest(DSSDocument document, RemoteSignatureParameters params) {
	    if (logSentinelClient == null) {
	        return;
	    }
	    
	    CertificateToken loadCertificate = DSSUtils.loadCertificate(params.getSigningCertificate().getEncodedCertificate());
	    
	    String principal = loadCertificate.getSubjectX500Principal().getName().replace("+", ",");
        LdapName ldapName;
        try {
            ldapName = new LdapName(principal);
        } catch (InvalidNameException ex) {
            throw new DSSException(ex);
        }
        String signerNames = ldapName.getRdns().stream()
                .filter(rdn -> rdn.getType().equals("CN"))
                .map(Rdn::getValue)
                .map(String.class::cast)
                .findFirst().orElse("");
                
	    ActorData actor = new ActorData(loadCertificate.getCertificate().getSerialNumber().toString());
	    actor.setActorDisplayName(signerNames);
	    
	    ActionData action = new ActionData(document.getDigest(params.getDigestAlgorithm()));
	    action.setAction("SIGN");
	    action.setEntityType("DOCUMENT");
	    action.setEntityId(document.getName());
	    action.setEntryType(AuditLogEntryType.BUSINESS_LOGIC_ENTRY);
	    
	    try {
            logSentinelClient.getAuditLogActions().logAsync(actor, action, new ApiCallbackAdapter<LogResponse>() {
                @Override
                public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
                    LOG.error("Failed to log request", e);
                }
            });
        } catch (ApiException e) {
            LOG.error("Failed to log request", e);
        }
	}

}
