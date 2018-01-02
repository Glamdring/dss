/**
 * DSS - Digital Signature Services
 * Copyright (C) 2015 European Commission, provided under the CEF programme
 *
 * This file is part of the "DSS - Digital Signature Services" project.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package eu.europa.esig.dss.pdf.pdfbox;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.RandomAccessBuffer;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.pdfwriter.COSWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionNamed;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationPopup;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationRubberStamp;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSigProperties;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSignDesigner;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.esig.dss.DSSASN1Utils;
import eu.europa.esig.dss.DSSDocument;
import eu.europa.esig.dss.DSSException;
import eu.europa.esig.dss.DSSUtils;
import eu.europa.esig.dss.DigestAlgorithm;
import eu.europa.esig.dss.InMemoryDocument;
import eu.europa.esig.dss.MimeType;
import eu.europa.esig.dss.SignatureImagePageRange;
import eu.europa.esig.dss.SignatureImageParameters;
import eu.europa.esig.dss.SignatureImageParameters.VisualSignaturePagePlacement;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.SignatureFieldParameters;
import eu.europa.esig.dss.pades.signature.visible.ImageAndResolution;
import eu.europa.esig.dss.pades.signature.visible.ImageUtils;
import eu.europa.esig.dss.pdf.DSSDictionaryCallback;
import eu.europa.esig.dss.pdf.PDFSignatureService;
import eu.europa.esig.dss.pdf.PdfDict;
import eu.europa.esig.dss.pdf.PdfDssDict;
import eu.europa.esig.dss.pdf.PdfSignatureInfo;
import eu.europa.esig.dss.pdf.PdfSignatureOrDocTimestampInfo;
import eu.europa.esig.dss.pdf.PdfSignatureOrDocTimestampInfoComparator;
import eu.europa.esig.dss.pdf.SignatureValidationCallback;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.x509.CertificatePool;
import eu.europa.esig.dss.x509.CertificateToken;
import eu.europa.esig.dss.x509.Token;
import eu.europa.esig.dss.x509.crl.CRLToken;
import eu.europa.esig.dss.x509.ocsp.OCSPToken;

class PdfBoxSignatureService implements PDFSignatureService {

	private static final Logger LOG = LoggerFactory.getLogger(PdfBoxSignatureService.class);

	private File pdfSignatureImageDir;
	
	@Override
	public byte[] digest(final InputStream toSignDocument, final PAdESSignatureParameters parameters,
			final DigestAlgorithm digestAlgorithm, boolean timestamping) throws DSSException {

		final byte[] signatureValue = DSSUtils.EMPTY_BYTE_ARRAY;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		PDDocument pdDocument = null;
		try {
			if (!timestamping) {
				pdDocument = loadAndStampDocument(toSignDocument, parameters);
			} else {
				pdDocument = PDDocument.load(toSignDocument);
			}
			PDSignature pdSignature = createSignatureDictionary(parameters, pdDocument);

			return signDocumentAndReturnDigest(parameters, signatureValue, outputStream, pdDocument, pdSignature, digestAlgorithm);
		} catch (IOException e) {
			throw new DSSException(e);
		} finally {
			Utils.closeQuietly(pdDocument);
			Utils.closeQuietly(outputStream);
		}
	}

	@Override
	public void sign(final InputStream pdfData, final byte[] signatureValue, final OutputStream signedStream,
			final PAdESSignatureParameters parameters, final DigestAlgorithm digestAlgorithm, boolean timestamping) throws DSSException {

		PDDocument pdDocument = null;
		try {
			if (!timestamping) {
				pdDocument = loadAndStampDocument(pdfData, parameters);
			} else {
				pdDocument = PDDocument.load(pdfData);
			}
			final PDSignature pdSignature = createSignatureDictionary(parameters, pdDocument);
			signDocumentAndReturnDigest(parameters, signatureValue, signedStream, pdDocument, pdSignature,
					digestAlgorithm);
		} catch (IOException e) {
			throw new DSSException(e);
		} finally {
			Utils.closeQuietly(pdDocument);
		}
	}

	private PDDocument loadAndStampDocument(InputStream pdfData, PAdESSignatureParameters parameters)
			throws IOException {
		byte[] pdfBytes = IOUtils.toByteArray(pdfData);
		PDDocument pdDocument = PDDocument.load(pdfBytes);
		if (parameters.getStampImageParameters() != null) {
			List<PDAnnotation> annotations = addStamps(pdDocument, parameters);
			
			setDocumentId(parameters, pdDocument);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (COSWriter writer = new COSWriter(baos, new RandomAccessBuffer(pdfBytes))) {
				// force-add the annotations (wouldn't be saved in incremental updates otherwise)
				annotations.forEach(ann -> addObjectToWrite(writer, ann.getCOSObject()));
				
				writer.write(pdDocument);
			}
			pdDocument.close();
			pdDocument = PDDocument.load(baos.toByteArray());
		}
		return pdDocument;
	}

	private void addObjectToWrite(COSWriter writer, COSDictionary cosObject) {
		// the COSWriter does not expose the addObjectToWrite method, so we need reflection to add the annotations
		try {
			Method method = writer.getClass().getDeclaredMethod("addObjectToWrite", COSBase.class);
			method.setAccessible(true);
			method.invoke(writer, cosObject);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private List<PDAnnotation> getExistingAnnotations(PDDocument pdDocument) throws IOException {
		List<PDAnnotation> annotations = new ArrayList<>();
		for (PDPage page : pdDocument.getPages()) {
			annotations.addAll(page.getAnnotations());
		}
		return annotations;
	}

	private List<PDAnnotation> addStamps(PDDocument pdDocument, PAdESSignatureParameters signatureParameters)
			throws FileNotFoundException, IOException {
		int totalPages = pdDocument.getNumberOfPages();
		List<PDAnnotation> result = new ArrayList<>();
		List<SignatureImageParameters> parametersList = signatureParameters.getStampImageParameters();
		for (int page = 0; page < totalPages; page++) {
			for (SignatureImageParameters parameters : parametersList) {
				if (parameters != null && placeSignatureOnPage(page, totalPages, parameters)) {
	
					ImageAndResolution ires = ImageUtils.create(parameters, 
							signatureParameters.getSigningCertificate(), 
							signatureParameters.bLevel().getSigningDate(),
							pdfSignatureImageDir);
					
					// calculate height based on width and ratio and vice versa
					if (parameters.getWidth() != 0 && parameters.getHeight() == 0 && ires.getRatio() != 0) {
						parameters.setHeight((int) (parameters.getWidth() / ires.getRatio()));
					}
					
					if (parameters.getWidth() == 0 && parameters.getHeight() != 0 && ires.getRatio() != 0) {
						parameters.setWidth((int) (parameters.getHeight() * ires.getRatio()));
					}
					
					SignatureImageAndPosition position = SignatureImageAndPositionProcessor.process(
							parameters, pdDocument,	ires, getPage(parameters.getPage(), pdDocument.getNumberOfPages()));
	
					PDRectangle rect = new PDRectangle(position.getX(), position.getY(), parameters.getWidth(),
							parameters.getHeight());
					
					PDAnnotationLink link = addLink(pdDocument, page, rect);
					PDAnnotationRubberStamp stamp = createStamp(pdDocument, page, rect, position.getSignatureImage());
					PDAnnotationPopup popup = addPopup(pdDocument, page, rect, stamp);
					stamp.setPopup(popup);
					
					pdDocument.getPage(page).getAnnotations().add(stamp);
					
					pdDocument.getPage(page).getCOSObject().setNeedToBeUpdated(true);
					
					result.add(stamp);
					result.add(popup);
					result.add(link);
				}
			}
		}
		return result;
	}

	private PDAnnotationPopup addPopup(PDDocument pdDocument, int page, PDRectangle rect,
			PDAnnotationRubberStamp stamp) throws IOException {
		PDAnnotationPopup popup = new PDAnnotationPopup();
		popup.setParent(stamp);
		popup.setRectangle(rect);
		popup.setContents("Signature");
		popup.setPrinted(true);
		popup.setPage(pdDocument.getPage(page));
		popup.getCOSObject().setNeedToBeUpdated(true);
		popup.setAnnotationName("SignaturePopup");
		pdDocument.getPage(page).getAnnotations().add(popup);
		return popup;
	}

	private PDAnnotationLink addLink(PDDocument pdDocument, int page, PDRectangle rect) throws IOException {
		PDAnnotationLink link = new PDAnnotationLink();
		link.setRectangle(rect);
		link.setPage(pdDocument.getPage(page));
		PDActionNamed action = new PDActionNamed();
		action.setN("ShowHideSignatures");
		link.setAction(action);
		link.setPrinted(true);
		link.setBorderStyle(new PDBorderStyleDictionary());
		link.getBorderStyle().setWidth(0);
		link.getCOSObject().setNeedToBeUpdated(true);
		link.setAnnotationName("SignatureLink");
		pdDocument.getPage(page).getAnnotations().add(link);
		return link;
	}

	private PDAnnotationRubberStamp createStamp(final PDDocument pdDocument, int page, PDRectangle rect, byte[] image) throws FileNotFoundException, IOException {
		
		PDAnnotationRubberStamp stamp = new PDAnnotationRubberStamp();
		stamp.setRectangle(rect);
		stamp.setInvisible(false);
		stamp.setBorderStyle(new PDBorderStyleDictionary());
		stamp.getBorderStyle().setWidth(0);
		stamp.setHidden(false);
		stamp.setSubject("Subject"); //TODO actual subject?
		stamp.setPrinted(true);
		stamp.setPage(pdDocument.getPage(page));
		stamp.getCOSObject().setNeedToBeUpdated(true);
		stamp.setAnnotationName("Signature");
		
		// Create a PDFormXObject
        PDFormXObject form = new PDFormXObject(pdDocument);
        form.setResources(new PDResources());
        form.setBBox(rect);
    	form.setFormType(1);

    	PDImageXObject ximage = PDImageXObject.createFromByteArray(pdDocument, image, "stamp");
    	
    	try (OutputStream formStream = form.getStream().createOutputStream()) {
			ImageUtils.drawXObject(ximage, form.getResources(), formStream, rect.getLowerLeftX(), rect.getLowerLeftY(),
					rect.getWidth(), rect.getHeight());
    	}
    	
    	PDAppearanceStream myDic = new PDAppearanceStream(form.getCOSObject());
        PDAppearanceDictionary appearance = new PDAppearanceDictionary(new COSDictionary());
    	appearance.setNormalAppearance(myDic);
    	stamp.setAppearance(appearance);
		return stamp;
	}
	
	private byte[] signDocumentAndReturnDigest(final PAdESSignatureParameters parameters, final byte[] signatureBytes,
			final OutputStream fileOutputStream, final PDDocument pdDocument, final PDSignature pdSignature,
			final DigestAlgorithm digestAlgorithm) throws DSSException {

		SignatureOptions options = new SignatureOptions();
		try {

			LocalSignatureInterface signatureInterface = new LocalSignatureInterface(digestAlgorithm, signatureBytes);
			
			options.setPreferredSignatureSize(parameters.getSignatureSize());
			// 0 means no visible signature
			if (parameters.getSignatureImageParameters() != null && parameters.getSignatureImageParameters().getPage() != 0) {
				fillImageParameters(pdDocument, parameters, options);
			}
			pdDocument.addSignature(pdSignature, signatureInterface, options);

			saveDocumentIncrementally(parameters, fileOutputStream, pdDocument);
			final byte[] digestValue = signatureInterface.getDigest().digest();
			if (LOG.isDebugEnabled()) {
				LOG.debug("Digest to be signed: " + Utils.toHex(digestValue));
			}
			return digestValue;
		} catch (IOException e) {
			throw new DSSException(e);
		} finally {
			Utils.closeQuietly(options.getVisualSignature());
		}
	}

	private boolean placeSignatureOnPage(int page, int total, SignatureImageParameters signatureImageParameters) {
		if (signatureImageParameters.getPagePlacement() == VisualSignaturePagePlacement.SINGLE_PAGE
				&& page == getPage(signatureImageParameters.getPage(), total)) {
			return true;
		} else if (signatureImageParameters.getPagePlacement() == VisualSignaturePagePlacement.ALL_PAGES) {
			return true;
		} else if (signatureImageParameters.getPagePlacement() == VisualSignaturePagePlacement.RANGE) {
			SignatureImagePageRange range = signatureImageParameters.getPageRange();
			if (range.getPages().isEmpty() || range.getPages().contains(page + 1)) {
				if (range.isExcludeLast() && total - range.getExcludeLastCount() < page + 1) {
					return false;
				} else if (range.isExcludeFirst() && range.getExcludeFirstCount() <= page + 1) {
					return false;
				}
				return true;
			}
		}
		return false;
	}

	private int getPage(int configuredPage, int total) {
		// negative values are counted from the end of the document, e.g. -1 is the last page.
		return configuredPage >= 0 ? configuredPage - 1 : total - Math.abs(configuredPage);
	}

	protected void fillImageParameters(final PDDocument doc, final PAdESSignatureParameters signatureParameters,
			SignatureOptions options) throws IOException {
		SignatureImageParameters signatureImageParameters = signatureParameters.getSignatureImageParameters();
		fillImageParameters(doc, signatureImageParameters, options, signatureParameters.getSigningCertificate(),
				signatureParameters.bLevel().getSigningDate());
	}

	protected void fillImageParameters(final PDDocument doc, final SignatureImageParameters signatureImageParameters,
			SignatureOptions options, CertificateToken signingCertificate, Date signingDate) throws IOException {
		if (signatureImageParameters != null) {
			// DSS-747. Using the DPI resolution to convert java size to dot
			ImageAndResolution ires = ImageUtils.create(signatureImageParameters, signingCertificate, signingDate, pdfSignatureImageDir);

			SignatureImageAndPosition signatureImageAndPosition = SignatureImageAndPositionProcessor
					.process(signatureImageParameters, doc, ires, getPage(signatureImageParameters.getPage(), doc.getNumberOfPages()));

			PDVisibleSignDesigner visibleSig = new PDVisibleSignDesigner(doc,
					new ByteArrayInputStream(signatureImageAndPosition.getSignatureImage()), 
					getPage(signatureImageParameters.getPage(), doc.getNumberOfPages()) + 1);


			// calculate height based on width and ratio and vice versa
			if (signatureImageParameters.getWidth() != 0 && signatureImageParameters.getHeight() == 0 && ires.getRatio() != 0) {
				signatureImageParameters.setHeight((int) (signatureImageParameters.getWidth() / ires.getRatio()));
			}
			
			if (signatureImageParameters.getWidth() == 0 && signatureImageParameters.getHeight() != 0 && ires.getRatio() != 0) {
				signatureImageParameters.setWidth((int) (signatureImageParameters.getHeight() * ires.getRatio()));
			}
			
			if (signatureImageParameters.getWidth() != 0 && signatureImageParameters.getHeight() != 0) {
				visibleSig.width(signatureImageParameters.getWidth());
				visibleSig.height(signatureImageParameters.getHeight());
			} else {
				visibleSig.width(ires.toXPoint(visibleSig.getWidth()));
				visibleSig.height(ires.toYPoint(visibleSig.getHeight()));
			}

			PDPage page = doc.getPage(getPage(signatureImageParameters.getPage(), doc.getNumberOfPages()));
			visibleSig.xAxis(signatureImageAndPosition.getX());
			visibleSig.yAxis(page.getCropBox().getHeight() - signatureImageAndPosition.getY() - visibleSig.getHeight());
			
			visibleSig.zoom(signatureImageParameters.getZoom() - 100); // pdfbox is 0 based

			PDVisibleSigProperties signatureProperties = new PDVisibleSigProperties();
			signatureProperties.visualSignEnabled(true).setPdVisibleSignature(visibleSig).buildSignature();

			options.setVisualSignature(signatureProperties);
			options.setPage(getPage(signatureImageParameters.getPage(), doc.getNumberOfPages())); // DSS-1138
		}
	}

	private PDSignature createSignatureDictionary(final PAdESSignatureParameters parameters, PDDocument pdDocument) {

		PDSignature signature;
		if ((parameters.getSignatureFieldId() != null) && (!parameters.getSignatureFieldId().isEmpty())) {
			signature = findExistingSignature(pdDocument, parameters.getSignatureFieldId());
		} else {
			signature = new PDSignature();
		}

		signature.setType(getType());
		// signature.setName(String.format("SD-DSS Signature %s",
		// parameters.getDeterministicId()));
		Date date = parameters.bLevel().getSigningDate();
		String encodedDate = " "
				+ Utils.toHex(DSSUtils.digest(DigestAlgorithm.SHA1, Long.toString(date.getTime()).getBytes()));
		CertificateToken token = parameters.getSigningCertificate();
		if (token == null) {
			signature.setName("Unknown signer" + encodedDate);
		} else {
			String shortName = DSSASN1Utils.getHumanReadableName(parameters.getSigningCertificate()) + encodedDate;
			signature.setName(shortName);
		}

		signature.setFilter(getFilter(parameters));
		// sub-filter for basic and PAdES Part 2 signatures
		signature.setSubFilter(getSubFilter(parameters));

		if (COSName.SIG.equals(getType())) {
			if (Utils.isStringNotEmpty(parameters.getContactInfo())) {
				signature.setContactInfo(parameters.getContactInfo());
			}

			if (Utils.isStringNotEmpty(parameters.getLocation())) {
				signature.setLocation(parameters.getLocation());
			}

			if (Utils.isStringNotEmpty(parameters.getReason())) {
				signature.setReason(parameters.getReason());
			}
		}

		// the signing date, needed for valid signature
		final Calendar cal = Calendar.getInstance();
		final Date signingDate = parameters.bLevel().getSigningDate();
		cal.setTime(signingDate);
		signature.setSignDate(cal);
		return signature;
	}

	private PDSignature findExistingSignature(PDDocument doc, String sigFieldName) {
		PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
		if (acroForm != null) {
			PDSignatureField signatureField = (PDSignatureField) acroForm.getField(sigFieldName);
			if (signatureField != null) {
				PDSignature signature = signatureField.getSignature();
				if (signature == null) {
					signature = new PDSignature();
					signatureField.getCOSObject().setItem(COSName.V, signature);
					return signature;
				} else {
					throw new DSSException(
							"The signature field '" + sigFieldName + "' can not be signed since its already signed.");
				}
			}
		}
		throw new DSSException("The signature field '" + sigFieldName + "' does not exist.");
	}

	public void saveDocumentIncrementally(PAdESSignatureParameters parameters, OutputStream outputStream, PDDocument pdDocument)
			throws DSSException {
		try {
			setDocumentId(parameters, pdDocument);
			pdDocument.saveIncremental(outputStream);
		} catch (IOException e) {
			throw new DSSException(e);
		}
	}

	private void setDocumentId(PAdESSignatureParameters parameters, PDDocument pdDocument) {
		// the document needs to have an ID, if not a ID based on the current system
		// time is used, and then the
		// digest of the signed data is different
		if (pdDocument.getDocumentId() == null) {

			final byte[] documentIdBytes = DSSUtils.digest(DigestAlgorithm.SHA256,
					parameters.bLevel().getSigningDate().toString().getBytes());
			pdDocument.setDocumentId(DSSUtils.toLong(documentIdBytes));
		}
	}

	protected COSName getType() {
		return COSName.SIG;
	}

	protected COSName getFilter(PAdESSignatureParameters parameters) {
		if (Utils.isStringNotEmpty(parameters.getSignatureFilter())) {
			return COSName.getPDFName(parameters.getSignatureFilter());
		}
		return PDSignature.FILTER_ADOBE_PPKLITE;
	}

	protected COSName getSubFilter(PAdESSignatureParameters parameters) {
		if (Utils.isStringNotEmpty(parameters.getSignatureSubFilter())) {
			return COSName.getPDFName(parameters.getSignatureSubFilter());
		}
		return PDSignature.SUBFILTER_ETSI_CADES_DETACHED;
	}

	@Override
	public void validateSignatures(CertificatePool validationCertPool, DSSDocument document,
			SignatureValidationCallback callback) throws DSSException {
		// recursive search of signature
		InputStream inputStream = document.openStream();
		try {
			List<PdfSignatureOrDocTimestampInfo> signaturesFound = 
					getSignatures(validationCertPool, Utils.toByteArray(inputStream));
			for (PdfSignatureOrDocTimestampInfo pdfSignatureOrDocTimestampInfo : signaturesFound) {
				callback.validate(pdfSignatureOrDocTimestampInfo);
			}
		} catch (IOException e) {
			LOG.error("Cannot validate signatures : " + e.getMessage(), e);
		}

		Utils.closeQuietly(inputStream);
	}

	private List<PdfSignatureOrDocTimestampInfo> getSignatures(CertificatePool validationCertPool,
			byte[] originalBytes) {
		List<PdfSignatureOrDocTimestampInfo> signatures = new ArrayList<PdfSignatureOrDocTimestampInfo>();
		PDDocument doc = null;
		try {
			doc = PDDocument.load(originalBytes);

			List<PDSignature> pdSignatures = doc.getSignatureDictionaries();
			if (Utils.isCollectionNotEmpty(pdSignatures)) {
				LOG.debug("{} signature(s) found", pdSignatures.size());

				PdfDict catalog = new PdfBoxDict(doc.getDocumentCatalog().getCOSObject(), doc);
				PdfDssDict dssDictionary = PdfDssDict.extract(catalog);

				for (PDSignature signature : pdSignatures) {
					String subFilter = signature.getSubFilter();

					COSDictionary dict = signature.getCOSObject();
					COSString item = (COSString) dict.getDictionaryObject(COSName.CONTENTS);
					byte[] cms = item.getBytes();

					byte[] cmsWithByteRange = signature.getContents(originalBytes);

					if (!Arrays.equals(cmsWithByteRange, cms)) {
						LOG.warn("The byte range doesn't match found /Content value!");
					}

					if (Utils.isStringEmpty(subFilter) || Utils.isArrayEmpty(cms)) {
						LOG.warn("Wrong signature with empty subfilter or cms.");
						continue;
					}

					byte[] signedContent = signature.getSignedContent(originalBytes);
					int[] byteRange = signature.getByteRange();

					PdfDict signatureDictionary = new PdfBoxDict(signature.getCOSObject(), doc);
					PdfSignatureOrDocTimestampInfo signatureInfo = null;
					if (PdfBoxDocTimeStampService.SUB_FILTER_ETSI_RFC3161.getName().equals(subFilter)) {
						boolean isArchiveTimestamp = false;

						// LT or LTA
						if (dssDictionary != null) {
							// check is DSS dictionary already exist
							if (isDSSDictionaryPresentInPreviousRevision(getOriginalBytes(byteRange, signedContent))) {
								isArchiveTimestamp = true;
							}
						}

						signatureInfo = new PdfBoxDocTimestampInfo(validationCertPool, signature, signatureDictionary,
								dssDictionary, cms, signedContent, isArchiveTimestamp);
					} else {
						signatureInfo = new PdfBoxSignatureInfo(validationCertPool, signature, signatureDictionary,
								dssDictionary, cms, signedContent);
					}

					if (signatureInfo != null) {
						signatures.add(signatureInfo);
					}
				}
				Collections.sort(signatures, new PdfSignatureOrDocTimestampInfoComparator());
				linkSignatures(signatures);

				for (PdfSignatureOrDocTimestampInfo sig : signatures) {
					LOG.debug("Signature " + sig.uniqueId() + " found with byteRange "
							+ Arrays.toString(sig.getSignatureByteRange()) + " (" + sig.getSubFilter() + ")");
				}
			}

		} catch (Exception e) {
			LOG.warn("Cannot analyze signatures : " + e.getMessage(), e);
		} finally {
			Utils.closeQuietly(doc);
		}

		return signatures;
	}

	/**
	 * This method links previous signatures to the new one. This is useful to get
	 * revision number and to know if a TSP is over the DSS dictionary
	 */
	private void linkSignatures(List<PdfSignatureOrDocTimestampInfo> signatures) {
		List<PdfSignatureOrDocTimestampInfo> previousList = new ArrayList<PdfSignatureOrDocTimestampInfo>();
		for (PdfSignatureOrDocTimestampInfo sig : signatures) {
			if (Utils.isCollectionNotEmpty(previousList)) {
				for (PdfSignatureOrDocTimestampInfo previous : previousList) {
					previous.addOuterSignature(sig);
				}
			}
			previousList.add(sig);
		}
	}

	private boolean isDSSDictionaryPresentInPreviousRevision(byte[] originalBytes) {
		PDDocument doc = null;
		PdfDssDict dssDictionary = null;
		try {
			doc = PDDocument.load(originalBytes);
			List<PDSignature> pdSignatures = doc.getSignatureDictionaries();
			if (Utils.isCollectionNotEmpty(pdSignatures)) {
				PdfDict catalog = new PdfBoxDict(doc.getDocumentCatalog().getCOSObject(), doc);
				dssDictionary = PdfDssDict.extract(catalog);
			}
		} catch (Exception e) {
			LOG.warn("Cannot check in previous revisions if DSS dictionary already exist : " + e.getMessage(), e);
		} finally {
			Utils.closeQuietly(doc);
		}

		return dssDictionary != null;
	}

	private byte[] getOriginalBytes(int[] byteRange, byte[] signedContent) {
		final int length = byteRange[1];
		final byte[] result = new byte[length];
		System.arraycopy(signedContent, 0, result, 0, length);
		return result;
	}

	@Override
	public void addDssDictionary(InputStream inputStream, OutputStream outputStream,
			List<DSSDictionaryCallback> callbacks) {
		PDDocument pdDocument = null;
		try {
			pdDocument = PDDocument.load(inputStream);
			if (Utils.isCollectionNotEmpty(callbacks)) {
				final COSDictionary cosDictionary = pdDocument.getDocumentCatalog().getCOSObject();
				cosDictionary.setItem("DSS", buildDSSDictionary(callbacks));
				cosDictionary.setNeedToBeUpdated(true);
			}

			pdDocument.saveIncremental(outputStream);

		} catch (Exception e) {
			throw new DSSException(e);
		} finally {
			Utils.closeQuietly(pdDocument);
		}
	}

	private COSDictionary buildDSSDictionary(List<DSSDictionaryCallback> callbacks) throws Exception {
		COSDictionary dss = new COSDictionary();

		Map<String, COSStream> streams = new HashMap<String, COSStream>();

		Set<CRLToken> allCrls = new HashSet<CRLToken>();
		Set<OCSPToken> allOcsps = new HashSet<OCSPToken>();
		Set<CertificateToken> allCertificates = new HashSet<CertificateToken>();

		COSDictionary vriDictionary = new COSDictionary();
		for (DSSDictionaryCallback callback : callbacks) {
			COSDictionary sigVriDictionary = new COSDictionary();
			sigVriDictionary.setDirect(true);

			if (Utils.isCollectionNotEmpty(callback.getCertificates())) {
				COSArray vriCertArray = new COSArray();
				for (CertificateToken token : callback.getCertificates()) {
					vriCertArray.add(getStream(streams, token));
					allCertificates.add(token);
				}
				sigVriDictionary.setItem("Cert", vriCertArray);
			}

			if (Utils.isCollectionNotEmpty(callback.getOcsps())) {
				COSArray vriOcspArray = new COSArray();
				for (OCSPToken token : callback.getOcsps()) {
					vriOcspArray.add(getStream(streams, token));
					allOcsps.add(token);
				}
				sigVriDictionary.setItem("OCSP", vriOcspArray);
			}

			if (Utils.isCollectionNotEmpty(callback.getCrls())) {
				COSArray vriCrlArray = new COSArray();
				for (CRLToken token : callback.getCrls()) {
					vriCrlArray.add(getStream(streams, token));
					allCrls.add(token);
				}
				sigVriDictionary.setItem("CRL", vriCrlArray);
			}

			// We can't use CMSSignedData, the pdSignature content is trimmed (000000)
			PdfSignatureInfo pdfSignatureInfo = callback.getSignature().getPdfSignatureInfo();
			final byte[] digest = DSSUtils.digest(DigestAlgorithm.SHA1, pdfSignatureInfo.getContent());
			String hexHash = Utils.toHex(digest).toUpperCase();

			vriDictionary.setItem(hexHash, sigVriDictionary);
		}
		dss.setItem("VRI", vriDictionary);

		if (Utils.isCollectionNotEmpty(allCertificates)) {
			COSArray arrayAllCerts = new COSArray();
			for (CertificateToken token : allCertificates) {
				arrayAllCerts.add(getStream(streams, token));
			}
			dss.setItem("Certs", arrayAllCerts);
		}

		if (Utils.isCollectionNotEmpty(allOcsps)) {
			COSArray arrayAllOcsps = new COSArray();
			for (OCSPToken token : allOcsps) {
				arrayAllOcsps.add(getStream(streams, token));
			}
			dss.setItem("OCSPs", arrayAllOcsps);
		}

		if (Utils.isCollectionNotEmpty(allCrls)) {
			COSArray arrayAllCrls = new COSArray();
			for (CRLToken token : allCrls) {
				arrayAllCrls.add(getStream(streams, token));
			}
			dss.setItem("CRLs", arrayAllCrls);
		}

		return dss;
	}

	private COSStream getStream(Map<String, COSStream> streams, Token token) throws IOException {
		COSStream stream = streams.get(token.getDSSIdAsString());

		if (stream == null) {
			stream = new COSStream();

			try (OutputStream unfilteredStream = stream.createOutputStream()) {
				unfilteredStream.write(token.getEncoded());
				unfilteredStream.flush();
			}
			streams.put(token.getDSSIdAsString(), stream);
		}
		return stream;
	}

	@Override
	public List<String> getAvailableSignatureFields(DSSDocument document) throws DSSException {
		List<String> result = new ArrayList<String>();
		try (InputStream is = document.openStream()) {
			PDDocument pdfDoc = PDDocument.load(is);
			List<PDSignatureField> signatureFields = pdfDoc.getSignatureFields();
			for (PDSignatureField pdSignatureField : signatureFields) {
				PDSignature signature = pdSignatureField.getSignature();
				if (signature == null) {
					result.add(pdSignatureField.getPartialName());
				}
			}
		} catch (Exception e) {
			throw new DSSException("Unable to determine signature fields", e);
		}
		return result;
	}

	@Override
	public DSSDocument addNewSignatureField(DSSDocument document, SignatureFieldParameters parameters) {
		DSSDocument newPdfDoc = null;
		try (InputStream is = document.openStream()) {
			PDDocument pdfDoc = PDDocument.load(is);
			PDPage page = pdfDoc.getPage(parameters.getPage());

			PDAcroForm acroForm = pdfDoc.getDocumentCatalog().getAcroForm();
			if (acroForm == null) {
				acroForm = new PDAcroForm(pdfDoc);
				pdfDoc.getDocumentCatalog().setAcroForm(acroForm);

				// Set default appearance
				PDResources resources = new PDResources();
				resources.put(COSName.getPDFName("Helv"), PDType1Font.HELVETICA);
				acroForm.setDefaultResources(resources);
				acroForm.setDefaultAppearance("/Helv 0 Tf 0 g");
			}

			PDSignatureField signatureField = new PDSignatureField(acroForm);
			if (Utils.isStringNotBlank(parameters.getName())) {
				signatureField.setPartialName(parameters.getName());
			}
			PDAnnotationWidget widget = signatureField.getWidgets().get(0);
			PDRectangle rect = new PDRectangle(parameters.getOriginX(), parameters.getOriginY(), parameters.getWidth(),
					parameters.getHeight());
			widget.setRectangle(rect);
			widget.setPage(page);
			page.getAnnotations().add(widget);
			acroForm.getFields().add(signatureField);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			pdfDoc.save(baos);
			pdfDoc.close();
			newPdfDoc = new InMemoryDocument(baos.toByteArray(), "new-document.pdf", MimeType.PDF);

		} catch (Exception e) {
			throw new DSSException("Unable to add a new signature fields", e);
		}
		return newPdfDoc;
	}

	public static class LocalSignatureInterface implements SignatureInterface {
		private final MessageDigest digest;
		private final byte[] signatureBytes;
		
		public LocalSignatureInterface(DigestAlgorithm digestAlgorithm, byte[] signatureBytes) {
			digest = DSSUtils.getMessageDigest(digestAlgorithm);
			this.signatureBytes = signatureBytes;
		}
		
		@Override
		public byte[] sign(InputStream content) throws IOException {
			byte[] b = new byte[4096];
			int count;
			while ((count = content.read(b)) > 0) {
				digest.update(b, 0, count);
			}
			return signatureBytes;
		}
		
		public MessageDigest getDigest() {
			return digest;
		}
	};

	public static class LocalCOSWriter extends COSWriter {

		public LocalCOSWriter(OutputStream os, RandomAccessRead randomAccessRead) throws IOException {
			super(os, randomAccessRead);
		}

		public byte[] getFullOutput() {
			return ((ByteArrayOutputStream) getOutput()).toByteArray();
		}
	}

	@Override
	public void setPdfSignatureImageDir(File dir) {
		pdfSignatureImageDir = dir;
	}
}