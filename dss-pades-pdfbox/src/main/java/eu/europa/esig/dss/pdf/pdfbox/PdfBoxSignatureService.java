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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.RandomAccessBuffer;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.pdfwriter.COSWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
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

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.MimeType;
import eu.europa.esig.dss.model.SignatureImagePageRange;
import eu.europa.esig.dss.model.pades.SignatureImageParameters;
import eu.europa.esig.dss.model.pades.SignatureImageParameters.VisualSignaturePagePlacement;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.model.x509.Token;
import eu.europa.esig.dss.pades.CertificationPermission;
import eu.europa.esig.dss.pades.PAdESCommonParameters;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.SignatureFieldParameters;
import eu.europa.esig.dss.pdf.AbstractPDFSignatureService;
import eu.europa.esig.dss.pdf.DSSDictionaryCallback;
import eu.europa.esig.dss.pdf.PAdESConstants;
import eu.europa.esig.dss.pdf.PDFServiceMode;
import eu.europa.esig.dss.pdf.PdfDict;
import eu.europa.esig.dss.pdf.PdfDocTimestampRevision;
import eu.europa.esig.dss.pdf.PdfDssDict;
import eu.europa.esig.dss.pdf.PdfSigDictWrapper;
import eu.europa.esig.dss.pdf.PdfSignatureRevision;
import eu.europa.esig.dss.pdf.pdfbox.visible.PdfBoxSignatureDrawer;
import eu.europa.esig.dss.pdf.pdfbox.visible.PdfBoxSignatureDrawerFactory;
import eu.europa.esig.dss.pdf.pdfbox.visible.defaultdrawer.DefaultDrawerImageUtils;
import eu.europa.esig.dss.pdf.pdfbox.visible.defaultdrawer.DefaultPdfBoxVisibleSignatureDrawer;
import eu.europa.esig.dss.pdf.pdfbox.visible.defaultdrawer.SignatureImageAndPosition;
import eu.europa.esig.dss.pdf.pdfbox.visible.defaultdrawer.SignatureImageAndPositionProcessor;
import eu.europa.esig.dss.pdf.visible.ImageAndResolution;
import eu.europa.esig.dss.pdf.visible.ImageUtils;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.x509.CertificatePool;
import eu.europa.esig.dss.spi.x509.revocation.crl.CRLToken;
import eu.europa.esig.dss.spi.x509.revocation.ocsp.OCSPToken;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.PdfRevision;
import eu.europa.esig.dss.validation.PdfSignatureDictionary;

public class PdfBoxSignatureService extends AbstractPDFSignatureService {

	private static final Logger LOG = LoggerFactory.getLogger(PdfBoxSignatureService.class);

	/**
	 * Constructor for the PdfBoxSignatureService
	 * 
	 * @param serviceMode
	 *                               current instance is used to generate
	 *                               DocumentTypestamp or Signature signature layer
	 * 
	 */
	public PdfBoxSignatureService(PDFServiceMode serviceMode, PdfBoxSignatureDrawerFactory signatureDrawerFactory) {
		super(serviceMode, signatureDrawerFactory);
	}

	@Override
	public byte[] digest(final DSSDocument toSignDocument, final PAdESCommonParameters parameters,
	        final DigestAlgorithm digestAlgorithm, boolean timestamping) {

		final byte[] signatureValue = DSSUtils.EMPTY_BYTE_ARRAY;
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				InputStream is = toSignDocument.openStream();
				PDDocument pdDocument = loadDocument(parameters, timestamping, is)) {
			final byte[] digest = signDocumentAndReturnDigest(parameters, signatureValue, outputStream, pdDocument);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Base64 messageDigest : {}", Utils.toBase64(digest));
			}
			return digest;
		} catch (IOException e) {
			throw new DSSException(e);
		}
	}
	
	private PDDocument loadDocument(final PAdESSignatureParameters parameters, boolean timestamping, InputStream in) throws IOException {
	    if (!timestamping) {
            return loadAndStampDocument(in, parameters);
        } else {
            return PDDocument.load(in);
        }
	}

	private PDDocument loadAndStampDocument(InputStream pdfData, PAdESSignatureParameters parameters)
            throws IOException {
        byte[] pdfBytes = IOUtils.toByteArray(pdfData);
        PDDocument pdDocument = PDDocument.load(pdfBytes);
        if (parameters.getStampImageParameters() != null) {
            for (PDPage page : pdDocument.getPages()) {
                // reset existing annotations (needed in order to have the stamps added)
                page.setAnnotations(null);
            }
            // adjust coordinates (make negative values relative to the bottom or right edge)
            // according to the first page
            adjustCoordinateParameters(parameters, pdDocument.getPage(0));
            
            // reset document outline (needed in order to have the stamps added)
            pdDocument.getDocumentCatalog().setDocumentOutline(null);
            List<PDAnnotation> annotations = addStamps(pdDocument, parameters);

            setDocumentId(parameters, pdDocument);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (COSWriter writer = new COSWriter(baos, new RandomAccessBuffer(pdfBytes))) {
                // force-add the annotations (wouldn't be saved in incremental updates otherwise)
                annotations.forEach(ann -> addObjectToWrite(writer, ann.getCOSObject()));

                // technically the same as saveIncremental but with more control
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
                    PDPage currentPage = pdDocument.getPage(DefaultPdfBoxVisibleSignatureDrawer.getPage(parameters.getPage(), pdDocument.getNumberOfPages()));
                    float y = DefaultDrawerImageUtils.convertNegativeAxisValue(parameters.getyAxis(), currentPage.getCropBox().getHeight());

                    ImageAndResolution ires = DefaultDrawerImageUtils.create(parameters, 
                            signatureParameters.getSigningCertificate(), signatureParameters.bLevel().getSigningDate());

                    // calculate height based on width and ratio and vice versa
                    if (parameters.getWidth() != 0 && parameters.getHeight() == 0 && ires.getRatio() != 0) {
                        parameters.setHeight((int) (parameters.getWidth() / ires.getRatio()));
                    }

                    if (parameters.getWidth() == 0 && parameters.getHeight() != 0 && ires.getRatio() != 0) {
                        parameters.setWidth((int) (parameters.getHeight() * ires.getRatio()));
                    }

                    SignatureImageAndPosition position = SignatureImageAndPositionProcessor.process(
                            parameters, pdDocument, ires, DefaultPdfBoxVisibleSignatureDrawer.getPage(parameters.getPage(), pdDocument.getNumberOfPages()));

                    // we need to invert the Y so that it starts from the top rather than from the bottom
                    y = currentPage.getCropBox().getHeight() - position.getY() - parameters.getHeight();

                    PDRectangle rect = new PDRectangle(position.getX(), y, parameters.getWidth(), parameters.getHeight());

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


	private boolean placeSignatureOnPage(int page, int total, SignatureImageParameters signatureImageParameters) {
        if (signatureImageParameters.getPagePlacement() == VisualSignaturePagePlacement.SINGLE_PAGE
                && page == DefaultPdfBoxVisibleSignatureDrawer.getPage(signatureImageParameters.getPage(), total)) {
            return true;
        } else if (signatureImageParameters.getPagePlacement() == VisualSignaturePagePlacement.ALL_PAGES) {
            return true;
        } else if (signatureImageParameters.getPagePlacement() == VisualSignaturePagePlacement.RANGE) {
            SignatureImagePageRange range = signatureImageParameters.getPageRange();
            if (range.getPages() == null || range.getPages().isEmpty() || range.getPages().contains(page + 1)) {
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
            ImageAndResolution ires = DefaultDrawerImageUtils.create(signatureImageParameters, signingCertificate, signingDate);

            int pageIndex = DefaultPdfBoxVisibleSignatureDrawer.getPage(signatureImageParameters.getPage(), doc.getNumberOfPages());
            
            SignatureImageAndPosition signatureImageAndPosition = SignatureImageAndPositionProcessor
                    .process(signatureImageParameters, doc, ires, pageIndex);

            PDVisibleSignDesigner visibleSig = new PDVisibleSignDesigner(doc,
                    new ByteArrayInputStream(signatureImageAndPosition.getSignatureImage()), 
                    pageIndex + 1);

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

            visibleSig.xAxis(signatureImageAndPosition.getX());
            visibleSig.yAxis(signatureImageAndPosition.getY());

            visibleSig.zoom(signatureImageParameters.getZoom() - 100); // pdfbox is 0 based

            PDVisibleSigProperties signatureProperties = new PDVisibleSigProperties();
            signatureProperties.visualSignEnabled(true).setPdVisibleSignature(visibleSig).buildSignature();
            
            options.setVisualSignature(signatureProperties);
            options.setPage(pageIndex); // DSS-1138
        }
    }
        
	private void setDocumentId(PAdESSignatureParameters parameters, PDDocument pdDocument) {
        // the document needs to have an ID, if not a ID based on the current system
        // time is used, and then the digest of the signed data is different
        if (pdDocument.getDocumentId() == null) {

            final byte[] documentIdBytes = DSSUtils.digest(DigestAlgorithm.SHA256,
                    parameters.bLevel().getSigningDate().toString().getBytes());
            pdDocument.setDocumentId(DSSUtils.toLong(documentIdBytes));
        }
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

        // Create a PDFormXObject
        PDFormXObject form = new PDFormXObject(pdDocument);
        form.setResources(new PDResources());
        form.setBBox(rect);
        form.setFormType(1);

        PDImageXObject ximage = PDImageXObject.createFromByteArray(pdDocument, image, "stamp");

        try (OutputStream formStream = form.getStream().createOutputStream()) {
            DefaultDrawerImageUtils.drawXObject(ximage, form.getResources(), formStream, rect.getLowerLeftX(), rect.getLowerLeftY(),
                    rect.getWidth(), rect.getHeight());
        }

        PDAppearanceStream myDic = new PDAppearanceStream(form.getCOSObject());
        PDAppearanceDictionary appearance = new PDAppearanceDictionary(new COSDictionary());
        appearance.setNormalAppearance(myDic);
        stamp.setAppearance(appearance);
        return stamp;
    }
	
	@Override
	public DSSDocument sign(final DSSDocument toSignDocument, final byte[] signatureValue, final PAdESSignatureParameters parameters,
			final DigestAlgorithm digestAlgorithm, boolean timestamping) {

		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				InputStream is = toSignDocument.openStream();
				PDDocument pdDocument = loadDocument(parameters, timestamping, is)) {

			signDocumentAndReturnDigest(parameters, signatureValue, baos, pdDocument);

			DSSDocument signature = new InMemoryDocument(baos.toByteArray());
			signature.setMimeType(MimeType.PDF);
			return signature;
		} catch (IOException e) {
			throw new DSSException(e);
		}
	}

	private void adjustCoordinateParameters(final PAdESCommonParameters parameters, PDPage currentPage) {
	    float x = ImageUtils.convertNegativeAxisValue(parameters.getSignatureImageParameters().getxAxis(), currentPage.getCropBox().getWidth());
        float y = ImageUtils.convertNegativeAxisValue(parameters.getSignatureImageParameters().getyAxis(), currentPage.getCropBox().getHeight());
        parameters.getSignatureImageParameters().setxAxis(x);
        parameters.getSignatureImageParameters().setyAxis(y);
        for (SignatureImageParameters params : parameters.getStampImageParameters()) {
            float stampX = ImageUtils.convertNegativeAxisValue(params.getxAxis(), currentPage.getCropBox().getWidth());
            float stampY = ImageUtils.convertNegativeAxisValue(params.getyAxis(), currentPage.getCropBox().getHeight());
            params.setxAxis(stampX);
            params.setyAxis(stampY);
        }

    }

    private byte[] signDocumentAndReturnDigest(final PAdESCommonParameters parameters, final byte[] signatureBytes, final OutputStream fileOutputStream,
			final PDDocument pdDocument, final DigestAlgorithm digestAlgorithm) {

		final MessageDigest digest = DSSUtils.getMessageDigest(parameters.getDigestAlgorithm());
		SignatureInterface signatureInterface = new SignatureInterface() {

			@Override
			public byte[] sign(InputStream content) throws IOException {

				byte[] b = new byte[4096];
				int count;
				while ((count = content.read(b)) > 0) {
					digest.update(b, 0, count);
				}
				return signatureBytes;
			}
		};

		final PDSignature pdSignature = createSignatureDictionary(parameters, pdDocument);
		try (SignatureOptions options = new SignatureOptions()) {
			options.setPreferredSignatureSize(parameters.getContentSize());

			SignatureImageParameters imageParameters = parameters.getImageParameters();
			if (imageParameters != null && signatureDrawerFactory != null) {
				PdfBoxSignatureDrawer signatureDrawer = (PdfBoxSignatureDrawer) signatureDrawerFactory
						.getSignatureDrawer(imageParameters);
				signatureDrawer.init(imageParameters, pdDocument, options, parameters.getSigningCertificate(), 
				        parameters.bLevel().getSigningDate());
				signatureDrawer.draw();
			}
			
			pdDocument.addSignature(pdSignature, signatureInterface, options);
			
			saveDocumentIncrementally(parameters, fileOutputStream, pdDocument);
			
			return digest.digest();
		} catch (IOException e) {
			throw new DSSException(e);
		}
	}

	private PDSignature createSignatureDictionary(final PAdESCommonParameters parameters, PDDocument pdDocument) {

		PDSignature signature;
		if (!isDocumentTimestampLayer() && Utils.isStringNotEmpty(parameters.getFieldId())) {
			signature = findExistingSignature(pdDocument, parameters.getFieldId());
		} else {
			signature = new PDSignature();
		}

		COSName currentType = COSName.getPDFName(getType());
		signature.setType(currentType);
		
		if (Utils.isStringNotEmpty(parameters.getFilter())) {
			signature.setFilter(COSName.getPDFName(parameters.getFilter()));
		}
		// sub-filter for basic and PAdES Part 2 signatures
		if (Utils.isStringNotEmpty(parameters.getSubFilter())) {
			signature.setSubFilter(COSName.getPDFName(parameters.getSubFilter()));
		}

		if (COSName.SIG.equals(currentType)) {
			
			PAdESSignatureParameters signatureParameters = (PAdESSignatureParameters) parameters;

			if (Utils.isStringNotEmpty(signatureParameters.getSignerName())) {
				signature.setName(signatureParameters.getSignerName());
			}

			if (Utils.isStringNotEmpty(signatureParameters.getContactInfo())) {
				signature.setContactInfo(signatureParameters.getContactInfo());
			}

			if (Utils.isStringNotEmpty(signatureParameters.getLocation())) {
				signature.setLocation(signatureParameters.getLocation());
			}

			if (Utils.isStringNotEmpty(signatureParameters.getReason())) {
				signature.setReason(signatureParameters.getReason());
			}

			CertificationPermission permission = signatureParameters.getPermission();
			// A document can contain only one signature field that contains a DocMDP
			// transform method;
			// it shall be the first signed field in the document.
			if (permission != null && !containsFilledSignature(pdDocument)) {
				setMDPPermission(pdDocument, signature, permission.getCode());
			}

			// the signing date, needed for valid signature
			final Calendar cal = Calendar.getInstance();
			final Date signingDate = parameters.getSigningDate();
			cal.setTime(signingDate);
			signature.setSignDate(cal);
		}

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
					throw new DSSException("The signature field '" + sigFieldName + "' can not be signed since its already signed.");
				}
			}
		}
		throw new DSSException("The signature field '" + sigFieldName + "' does not exist.");
	}

	private boolean containsFilledSignature(PDDocument pdDocument) {
		try {
			List<PDSignature> signatures = pdDocument.getSignatureDictionaries();
			for (PDSignature pdSignature : signatures) {
				if (pdSignature.getCOSObject().containsKey(COSName.BYTERANGE)) {
					return true;
				}
			}
			return false;
		} catch (IOException e) {
			LOG.warn("Cannot read the existing signature(s)", e);
			return false;
		}
	}

	/**
	 * Set the access permissions granted for this document in the DocMDP transform
	 * parameters dictionary. Details are described in the table "Entries in the
	 * DocMDP transform parameters dictionary" in the PDF specification.
	 *
	 * @param doc
	 *                          The document.
	 * @param signature
	 *                          The signature object.
	 * @param accessPermissions
	 *                          The permission value (1, 2 or 3).
	 */
	public void setMDPPermission(PDDocument doc, PDSignature signature, int accessPermissions) {
		COSDictionary sigDict = signature.getCOSObject();

		// DocMDP specific stuff
		COSDictionary transformParameters = new COSDictionary();
		transformParameters.setItem(COSName.TYPE, COSName.getPDFName("TransformParams"));
		transformParameters.setInt(COSName.P, accessPermissions);
		transformParameters.setName(COSName.V, "1.2");
		transformParameters.setNeedToBeUpdated(true);

		COSDictionary referenceDict = new COSDictionary();
		referenceDict.setItem(COSName.TYPE, COSName.getPDFName("SigRef"));
		referenceDict.setItem("TransformMethod", COSName.DOCMDP);
		referenceDict.setItem("TransformParams", transformParameters);
		referenceDict.setNeedToBeUpdated(true);

		COSArray referenceArray = new COSArray();
		referenceArray.add(referenceDict);
		sigDict.setItem("Reference", referenceArray);
		referenceArray.setNeedToBeUpdated(true);

		// Document Catalog
		COSDictionary catalogDict = doc.getDocumentCatalog().getCOSObject();
		COSDictionary permsDict = new COSDictionary();
		catalogDict.setItem(COSName.PERMS, permsDict);
		permsDict.setItem(COSName.DOCMDP, signature);
		catalogDict.setNeedToBeUpdated(true);
		permsDict.setNeedToBeUpdated(true);
	}

	public void saveDocumentIncrementally(PAdESCommonParameters parameters, OutputStream outputStream, PDDocument pdDocument) throws DSSException {
		try {
			// the document needs to have an ID, if not a ID based on the current system
			// time is used, and then the
			// digest of the signed data is different
			if (pdDocument.getDocumentId() == null) {
				pdDocument.setDocumentId(parameters.getSigningDate().getTime());
			}
			pdDocument.saveIncremental(outputStream);
		} catch (IOException e) {
			throw new DSSException(e);
		}
	}

	@Override
	protected List<PdfRevision> getSignatures(CertificatePool validationCertPool, DSSDocument document) {
		List<PdfRevision> signatures = new ArrayList<>();
		try (InputStream is = document.openStream(); PDDocument doc = PDDocument.load(is)) {

			final PdfDssDict dssDictionary = PdfBoxUtils.getDSSDictionary(doc);
			
			Map<PdfSignatureDictionary, List<String>> sigDictionaries = extractSigDictionaries(doc);

			for (Map.Entry<PdfSignatureDictionary, List<String>> sigDictEntry : sigDictionaries.entrySet()) {
				PdfSignatureDictionary signatureDictionary = sigDictEntry.getKey();
				List<String> fieldNames = sigDictEntry.getValue();
				try {
					final int[] byteRange = signatureDictionary.getSignatureByteRange();

					validateByteRange(byteRange);

					final byte[] cms = signatureDictionary.getContents();
					byte[] signedContent = new byte[] {};
					if (!isContentValueEqualsByteRangeExtraction(document, byteRange, cms, fieldNames)) {
						LOG.warn("Signature {} is skipped. SIWA detected !", fieldNames);
					} else {
						signedContent = getSignedContent(document, byteRange);
					}

					boolean coverAllOriginalBytes = isSignatureCoversWholeDocument(document, byteRange);
					PdfRevision signatureInfo = null;
					
					if (isDocTimestamp(signatureDictionary)) {
						
						PdfDssDict timestampedDssDictionary = null;
						
						// LT or LTA
						if (dssDictionary != null) {
							// check is DSS dictionary already exist
							timestampedDssDictionary = getDSSDictionaryPresentInRevision(getOriginalBytes(byteRange, signedContent));
						}
						signatureInfo = new PdfDocTimestampRevision(cms, signatureDictionary, timestampedDssDictionary, fieldNames,
								validationCertPool, signedContent, coverAllOriginalBytes);
						
					} else if (isSignature(signatureDictionary)) {
						signatureInfo = new PdfSignatureRevision(cms, signatureDictionary, dssDictionary, fieldNames, 
								validationCertPool, signedContent, coverAllOriginalBytes);
						
					} else {
						LOG.warn("The entry {} is skipped. A signature dictionary entry with a type '{}' and subFilter '{}' is not acceptable configuration!",
								fieldNames, signatureDictionary.getType(), signatureDictionary.getSubFilter());
						
					}

					if (signatureInfo != null) {
						signatures.add(signatureInfo);
					}
					
				} catch (Exception e) {
					String errorMessage = "Unable to parse signature {} . Reason : {}";
					if (LOG.isDebugEnabled()) {
						LOG.error(errorMessage, fieldNames, e.getMessage(), e);
					} else {
						LOG.error(errorMessage, fieldNames, e.getMessage() );
					}
					
				}
			}
			linkSignatures(signatures);
			
		} catch (Exception e) {
			throw new DSSException("Cannot analyze signatures : " + e.getMessage(), e);
		}

		return signatures;
	}
	
	private Map<PdfSignatureDictionary, List<String>> extractSigDictionaries(PDDocument doc) throws IOException {
		Map<PdfSignatureDictionary, List<String>> pdfDictionaries = new LinkedHashMap<>();
		Map<Long, PdfSignatureDictionary> pdfObjectDictMap = new LinkedHashMap<>();

		List<PDSignatureField> pdSignatureFields = doc.getSignatureFields();
		if (Utils.isCollectionNotEmpty(pdSignatureFields)) {
			LOG.debug("{} signature(s) found", pdSignatureFields.size());
			
			for (PDSignatureField signatureField : pdSignatureFields) {
				
				String signatureFieldName = signatureField.getPartialName();

				COSObject sigDictObject = signatureField.getCOSObject().getCOSObject(COSName.V);
				if (sigDictObject == null || !(sigDictObject.getObject() instanceof COSDictionary)) {
					LOG.warn("Signature field with name '{}' does not contain a signature", signatureFieldName);
					continue;
				}
				
				long sigDictNumber = sigDictObject.getObjectNumber();
				PdfSignatureDictionary signature = pdfObjectDictMap.get(sigDictNumber);
				if (signature == null) {					
					PdfDict dictionary = new PdfBoxDict((COSDictionary)sigDictObject.getObject(), doc);
					signature = new PdfSigDictWrapper(dictionary);
					
					pdfDictionaries.put(signature, new ArrayList<>(Arrays.asList(signatureFieldName)));
					pdfObjectDictMap.put(sigDictNumber, signature);
					
				} else {
					List<String> fieldNameList = pdfDictionaries.get(signature);
					fieldNameList.add(signatureFieldName);
					LOG.warn("More than one field refers to the same signature dictionary: {}!", fieldNameList);
					
				}
				
			}	
		}
		
		return pdfDictionaries;
	}
	
	private boolean isSignatureCoversWholeDocument(DSSDocument document, int[] byteRange) {
		try (InputStream is = document.openStream()) {
			long originalBytesLength = Utils.getInputStreamSize(is);
			// /ByteRange [0 575649 632483 10206]
			long beforeSignatureLength = (long)byteRange[1] - byteRange[0];
			long expectedCMSLength = (long)byteRange[2] - byteRange[1] - byteRange[0];
			long afterSignatureLength = byteRange[3];
			long totalCoveredByByteRange = beforeSignatureLength + expectedCMSLength + afterSignatureLength;

			return (originalBytesLength == totalCoveredByByteRange);
		} catch (IOException e) {
			LOG.warn("Cannot determine the original file size for the document. Reason : {}", e.getMessage());
			return false;
		}
	}

	private PdfDssDict getDSSDictionaryPresentInRevision(byte[] originalBytes) {
		try (PDDocument doc = PDDocument.load(originalBytes)) {
			return PdfBoxUtils.getDSSDictionary(doc);
		} catch (Exception e) {
			LOG.warn("Cannot check in previous revisions if DSS dictionary already exist : " + e.getMessage(), e);
			return null;
		}
	}

	@Override
	public DSSDocument addDssDictionary(DSSDocument document, List<DSSDictionaryCallback> callbacks) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); InputStream is = document.openStream(); PDDocument pdDocument = PDDocument.load(is)) {

			if (Utils.isCollectionNotEmpty(callbacks)) {
				final COSDictionary cosDictionary = pdDocument.getDocumentCatalog().getCOSObject();
				cosDictionary.setItem(PAdESConstants.DSS_DICTIONARY_NAME, buildDSSDictionary(pdDocument, callbacks));
				cosDictionary.setNeedToBeUpdated(true);
			}

			pdDocument.saveIncremental(baos);

			DSSDocument inMemoryDocument = new InMemoryDocument(baos.toByteArray());
			inMemoryDocument.setMimeType(MimeType.PDF);
			return inMemoryDocument;

		} catch (Exception e) {
			throw new DSSException(e);
		}
	}

	private COSDictionary buildDSSDictionary(PDDocument pdDocument, List<DSSDictionaryCallback> callbacks) throws IOException {
		COSDictionary dss = new COSDictionary();

		Map<String, Long> knownObjects = buildKnownObjects(callbacks);

		Map<String, COSStream> streams = new HashMap<>();

		Set<CRLToken> allCrls = new HashSet<>();
		Set<OCSPToken> allOcsps = new HashSet<>();
		Set<CertificateToken> allCertificates = new HashSet<>();

		COSDictionary vriDictionary = new COSDictionary();
		for (DSSDictionaryCallback callback : callbacks) {
			COSDictionary sigVriDictionary = new COSDictionary();
			sigVriDictionary.setDirect(true);

			Set<CertificateToken> currentCerts = callback.getCertificates();
			if (Utils.isCollectionNotEmpty(currentCerts)) {
				allCertificates.addAll(currentCerts);
				sigVriDictionary.setItem(PAdESConstants.CERT_ARRAY_NAME_VRI, buildArray(pdDocument, streams, currentCerts, knownObjects));
			}

			List<OCSPToken> currentOcsps = callback.getOcsps();
			if (Utils.isCollectionNotEmpty(currentOcsps)) {
				allOcsps.addAll(currentOcsps);
				sigVriDictionary.setItem(PAdESConstants.OCSP_ARRAY_NAME_VRI, buildArray(pdDocument, streams, currentOcsps, knownObjects));
			}

			List<CRLToken> currentCrls = callback.getCrls();
			if (Utils.isCollectionNotEmpty(currentCrls)) {
				allCrls.addAll(currentCrls);
				sigVriDictionary.setItem(PAdESConstants.CRL_ARRAY_NAME_VRI, buildArray(pdDocument, streams, currentCrls, knownObjects));
			}

			// We can't use CMSSignedData, the pdSignature content is trimmed (000000)
			String vriKey = callback.getSignature().getVRIKey();
			vriDictionary.setItem(vriKey, sigVriDictionary);
		}
		dss.setItem(PAdESConstants.VRI_DICTIONARY_NAME, vriDictionary);

		if (Utils.isCollectionNotEmpty(allCertificates)) {
			dss.setItem(PAdESConstants.CERT_ARRAY_NAME_DSS, buildArray(pdDocument, streams, allCertificates, knownObjects));
		}

		if (Utils.isCollectionNotEmpty(allOcsps)) {
			dss.setItem(PAdESConstants.OCSP_ARRAY_NAME_DSS, buildArray(pdDocument, streams, allOcsps, knownObjects));
		}

		if (Utils.isCollectionNotEmpty(allCrls)) {
			dss.setItem(PAdESConstants.CRL_ARRAY_NAME_DSS, buildArray(pdDocument, streams, allCrls, knownObjects));
		}

		return dss;
	}

	private COSArray buildArray(PDDocument pdDocument, Map<String, COSStream> streams, Collection<? extends Token> tokens, Map<String, Long> knownObjects)
			throws IOException {
		COSArray array = new COSArray();
		// avoid duplicate CRLs
		List<String> currentObjIds = new ArrayList<>();
		for (Token token : tokens) {
			String digest = getTokenDigest(token);
			if (!currentObjIds.contains(digest)) {
				Long objectNumber = knownObjects.get(digest);
				if (objectNumber == null ) {
					COSStream stream = streams.get(digest);
					if (stream == null) {
						stream = pdDocument.getDocument().createCOSStream();
						try (OutputStream unfilteredStream = stream.createOutputStream()) {
							unfilteredStream.write(token.getEncoded());
							unfilteredStream.flush();
						}
						streams.put(digest, stream);
					}
					array.add(stream);
				} else {
					COSObject foundCosObject = getByObjectNumber(pdDocument, objectNumber);
					array.add(foundCosObject);
				}
				currentObjIds.add(digest);
			}
		}
		return array;
	}

	private COSObject getByObjectNumber(PDDocument pdDocument, Long objectNumber) {
		List<COSObject> objects = pdDocument.getDocument().getObjects();
		for (COSObject cosObject : objects) {
			if (cosObject.getObjectNumber() == objectNumber) {
				return cosObject;
			}
		}
		return null;
	}

	@Override
	public List<String> getAvailableSignatureFields(DSSDocument document) {
		List<String> result = new ArrayList<>();
		try (InputStream is = document.openStream(); PDDocument pdfDoc = PDDocument.load(is)) {
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
		try (InputStream is = document.openStream(); PDDocument pdfDoc = PDDocument.load(is);
				ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			
			PDPage page = pdfDoc.getPage(parameters.getPage());
			
			PDDocumentCatalog catalog = pdfDoc.getDocumentCatalog();
	        catalog.getCOSObject().setNeedToBeUpdated(true);

			PDAcroForm acroForm = catalog.getAcroForm();
			if (acroForm == null) {
				acroForm = new PDAcroForm(pdfDoc);
				catalog.setAcroForm(acroForm);
				
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
			PDRectangle rect = new PDRectangle(parameters.getOriginX(), parameters.getOriginY(), parameters.getWidth(), parameters.getHeight());
			widget.setRectangle(rect);
			widget.setPage(page);
			page.getAnnotations().add(widget);
			
			acroForm.getFields().add(signatureField);

			acroForm.getCOSObject().setNeedToBeUpdated(true);
			signatureField.getCOSObject().setNeedToBeUpdated(true);
			page.getCOSObject().setNeedToBeUpdated(true);
			
			pdfDoc.saveIncremental(baos);
			
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
}
