package eu.europa.esig.dss.pades;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Test;

import eu.europa.esig.dss.DSSDocument;
import eu.europa.esig.dss.DigestAlgorithm;
import eu.europa.esig.dss.InMemoryDocument;
import eu.europa.esig.dss.SignatureImagePageRange;
import eu.europa.esig.dss.SignatureImageParameters;
import eu.europa.esig.dss.SignatureImageParameters.VisualSignaturePagePlacement;
import eu.europa.esig.dss.SignatureImageTextParameters;
import eu.europa.esig.dss.SignatureImageTextParameters.SignerPosition;
import eu.europa.esig.dss.SignatureLevel;
import eu.europa.esig.dss.SignatureValue;
import eu.europa.esig.dss.ToBeSigned;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.signature.PKIFactoryAccess;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;

public class PAdESVisibleSignatureAndStampTest extends PKIFactoryAccess {

	@Test
	public void visibleSignatureAndStampTest() throws FileNotFoundException, IOException {
		byte[] pdfBytes = IOUtils.toByteArray(PAdESVisibleSignatureAndStampTest.class.getResourceAsStream("/multi_page.pdf"));
		DSSDocument document = new InMemoryDocument(new ByteArrayInputStream(pdfBytes));
		byte[] imageBytes = IOUtils.toByteArray(PAdESVisibleSignatureAndStampTest.class.getResourceAsStream("/small-red.jpg"));
		DSSDocument image = new InMemoryDocument(new ByteArrayInputStream(imageBytes));

		DSSDocument signedDocument = signDocumentWithStamps(document, image);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		IOUtils.copy(signedDocument.openStream(), baos);
		
		PDDocument result = PDDocument.load(baos.toByteArray());
		
		assertThat(result.getNumberOfPages(), equalTo(3));
		assertThat(result.getPage(0).getAnnotations().size(), equalTo(3));
		assertThat(result.getPage(1).getAnnotations().size(), equalTo(3));
		assertThat(result.getPage(2).getAnnotations().size(), equalTo(1));
		// commented-out piece for manual testing
//		try (FileOutputStream fos = new FileOutputStream("c:\\tmp\\out.pdf")) {
//			IOUtils.copy(signedDocument.openStream(), fos);
//		}
	}

	@Test
	public void multipleVisibleSignatureAndStampTest() throws FileNotFoundException, IOException {
		byte[] pdfBytes = IOUtils.toByteArray(PAdESVisibleSignatureAndStampTest.class.getResourceAsStream("/multi_page.pdf"));
		DSSDocument document = new InMemoryDocument(new ByteArrayInputStream(pdfBytes));
		byte[] imageBytes = IOUtils.toByteArray(PAdESVisibleSignatureAndStampTest.class.getResourceAsStream("/small-red.jpg"));
		DSSDocument image = new InMemoryDocument(new ByteArrayInputStream(imageBytes));
		
		DSSDocument signedDocument = signDocumentWithStamps(document, image);
		DSSDocument twiceSignedDocument = signDocumentWithStamps(signedDocument, image);

		PDDocument twiceSigned = PDDocument.load(twiceSignedDocument.openStream());
		assertThat(twiceSigned.getSignatureFields().size(), equalTo(2));
		
		// commented-out piece for manual testing
//		try (FileOutputStream fos = new FileOutputStream("c:\\tmp\\out-twice.pdf")) {
//			IOUtils.copy(twiceSignedDocument.openStream(), fos);
//		}
	}
	
	private DSSDocument signDocumentWithStamps(DSSDocument document, DSSDocument image) {
		PAdESSignatureParameters params = new PAdESSignatureParameters();
		params.setSignatureSubFilter("adbe.pkcs7.detached");
		
		params.setSignatureImageParameters(new SignatureImageParameters());
		params.getSignatureImageParameters().setImage(image);
		params.getSignatureImageParameters().setTextParameters(new SignatureImageTextParameters());
		params.getSignatureImageParameters().getTextParameters().setText("%CN_1%\n%CN_2%");
		params.getSignatureImageParameters().getTextParameters().setSignerNamePosition(SignerPosition.FOREGROUND);
		params.getSignatureImageParameters().setTextRightParameters(new SignatureImageTextParameters());
		params.getSignatureImageParameters().getTextRightParameters().setText("Signature created by\nTest\nDate: %DateTimeWithTimeZone%");
		params.getSignatureImageParameters().setPageRange(new SignatureImagePageRange());
		params.getSignatureImageParameters().setxAxis(25);
		params.getSignatureImageParameters().setyAxis(15);
		params.getSignatureImageParameters().setWidth(100);
		params.getSignatureImageParameters().setHeight(30);
		params.getSignatureImageParameters().setPagePlacement(VisualSignaturePagePlacement.SINGLE_PAGE);
		params.getSignatureImageParameters().setPage(3);
		
		params.setStampImageParameters(new SignatureImageParameters());
		params.getStampImageParameters().setImage(image);
		params.getStampImageParameters().setTextParameters(new SignatureImageTextParameters());
		params.getStampImageParameters().getTextParameters().setText("%CN_1%\n%CN_2%");
		params.getStampImageParameters().getTextParameters().setSignerNamePosition(SignerPosition.FOREGROUND);
		params.getStampImageParameters().setTextRightParameters(new SignatureImageTextParameters());
		params.getStampImageParameters().getTextRightParameters().setText("Signature created by\nTest\nDate: %DateTimeWithTimeZone%");
		params.getStampImageParameters().setPageRange(new SignatureImagePageRange());
		params.getStampImageParameters().setxAxis(25);
		params.getStampImageParameters().setyAxis(15);
		params.getStampImageParameters().setWidth(100);
		params.getStampImageParameters().setHeight(30);
		params.getStampImageParameters().setPagePlacement(VisualSignaturePagePlacement.RANGE);
		params.getStampImageParameters().getPageRange().setPages(Arrays.asList(1, 2));
		
		params.bLevel().setSigningDate(new Date());
		params.setSigningCertificate(getSigningCert());
		params.setCertificateChain(getCertificateChain());
		params.setSignatureLevel(SignatureLevel.PAdES_BASELINE_B);
		params.setDigestAlgorithm(DigestAlgorithm.SHA512);

		PAdESService service = new PAdESService(new CommonCertificateVerifier());

		ToBeSigned dataToSign = service.getDataToSign(document, params);
		SignatureValue signatureValue = getToken().sign(dataToSign, params.getDigestAlgorithm(), getPrivateKeyEntry());
		DSSDocument signedDocument = service.signDocument(document, params, signatureValue);
		return signedDocument;
	}
	
	@Override
	protected String getSigningAlias() {
		return GOOD_USER;
	}
}
