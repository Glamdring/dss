package eu.europa.esig.dss.pades.signature.visible;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;

import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Test;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.MimeType;
import eu.europa.esig.dss.model.SignatureImagePageRange;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.model.pades.SignatureImageParameters;
import eu.europa.esig.dss.model.pades.SignatureImageTextParameters;
import eu.europa.esig.dss.model.pades.SignatureImageParameters.VisualSignaturePagePlacement;
import eu.europa.esig.dss.model.pades.SignatureImageTextParameters.SignerTextHorizontalAlignment;
import eu.europa.esig.dss.model.pades.SignatureImageTextParameters.SignerTextPosition;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.test.signature.PKIFactoryAccess;

public class PAdESVisibleSignatureAndStampTest extends PKIFactoryAccess {

	@Test
	public void visibleSignatureAndStampTest() throws FileNotFoundException, IOException {
		byte[] pdfBytes = IOUtils.toByteArray(PAdESVisibleSignatureAndStampTest.class.getResourceAsStream("/multi_page.pdf"));
		DSSDocument document = new InMemoryDocument(new ByteArrayInputStream(pdfBytes));
		byte[] imageBytes = IOUtils.toByteArray(PAdESVisibleSignatureAndStampTest.class.getResourceAsStream("/small-red.jpg"));
		DSSDocument image = new InMemoryDocument(new ByteArrayInputStream(imageBytes));
		image.setMimeType(MimeType.JPEG);
		
		DSSDocument signedDocument = signDocumentWithStamps(document, image, 25);

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
		image.setMimeType(MimeType.JPEG);

		DSSDocument signedDocument = signDocumentWithStamps(document, image, 25);
		DSSDocument twiceSignedDocument = signDocumentWithStamps(signedDocument, image, 125);

		PDDocument twiceSigned = PDDocument.load(twiceSignedDocument.openStream());
		//assertThat(twiceSigned.getSignatureFields().size(), equalTo(6)); // two for signatures and 2*2 for timestamps

		// commented-out piece for manual testing
//		try (FileOutputStream fos = new FileOutputStream("c:\\tmp\\out-twice.pdf")) {
//			IOUtils.copy(twiceSignedDocument.openStream(), fos);
//		}
	}

	private DSSDocument signDocumentWithStamps(DSSDocument document, DSSDocument image, int x) {
		PAdESSignatureParameters params = new PAdESSignatureParameters();
		params.setSubFilter("adbe.pkcs7.detached");

		params.setImageParameters(new SignatureImageParameters());
		params.getImageParameters().setImage(image);
		params.getImageParameters().setTextParameters(new SignatureImageTextParameters());
		params.getImageParameters().getTextParameters().setText("%CN_1%\n%CN_2%");
		params.getImageParameters().getTextParameters().setPadding(30);
		params.getImageParameters().getTextParameters().setSignerTextHorizontalAlignment(SignerTextHorizontalAlignment.RIGHT);
		params.getImageParameters().getTextParameters().setSignerTextPosition(SignerTextPosition.FOREGROUND);
		params.getImageParameters().setTextRightParameters(new SignatureImageTextParameters());
		params.getImageParameters().getTextRightParameters().setText("Signature created by\nTest\nDate: %DateTimeWithTimeZone%");
		params.getImageParameters().setPageRange(new SignatureImagePageRange());
		params.getImageParameters().setxAxis(x);
		params.getImageParameters().setyAxis(-55);
		params.getImageParameters().setWidth(200);
		params.getImageParameters().setPagePlacement(VisualSignaturePagePlacement.SINGLE_PAGE);
		params.getImageParameters().setPage(-1);
		
		SignatureImageParameters stampParams = new SignatureImageParameters();
		stampParams.setImage(image);
		stampParams.setTextParameters(new SignatureImageTextParameters());
		stampParams.getTextParameters().setText("%CN_1%\n%CN_2%");
		stampParams.getTextParameters().setPadding(30);
		stampParams.getTextParameters().setSignerTextHorizontalAlignment(SignerTextHorizontalAlignment.RIGHT);
		stampParams.getTextParameters().setSignerTextPosition(SignerTextPosition.FOREGROUND);
		stampParams.setTextRightParameters(new SignatureImageTextParameters());
		stampParams.getTextRightParameters().setText("Signature created by\nTest\nDate: %DateTimeWithTimeZone%");
		stampParams.setPageRange(new SignatureImagePageRange());
		stampParams.setxAxis(x);
		stampParams.setyAxis(-55);
		stampParams.setWidth(200);
		stampParams.setPagePlacement(VisualSignaturePagePlacement.RANGE);
		stampParams.getPageRange().setAll(true);
		stampParams.getPageRange().setExcludeLast(true);
		stampParams.getPageRange().setExcludeLastCount(1);

		params.setStampImageParameters(Collections.singletonList(stampParams));

		params.bLevel().setSigningDate(new Date());
		params.setSigningCertificate(getSigningCert());
		params.setCertificateChain(getCertificateChain());
		params.setSignatureLevel(SignatureLevel.PAdES_BASELINE_LT);
		params.setSignaturePackaging(SignaturePackaging.ENVELOPING);
		params.setDigestAlgorithm(DigestAlgorithm.SHA512);

		PAdESService service = new PAdESService(getCompleteCertificateVerifier());
		service.setTspSource(getGoodTsa());
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