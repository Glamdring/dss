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
package eu.europa.esig.dss.pdf.pdfbox.visible;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

<<<<<<< HEAD:dss-pades/src/main/java/eu/europa/esig/dss/pdf/pdfbox/SignatureImageAndPositionProcessor.java
import eu.europa.esig.dss.SignatureImageParameters;
import eu.europa.esig.dss.SignatureImageParameters.VisualSignatureAlignmentHorizontal;
import eu.europa.esig.dss.SignatureImageParameters.VisualSignatureAlignmentVertical;
import eu.europa.esig.dss.pades.signature.visible.ImageAndResolution;
import eu.europa.esig.dss.pades.signature.visible.ImageUtils;
=======
>>>>>>> b24548fbe36b84bb32fd0a0902c84ba90242b603:dss-pades-pdfbox/src/main/java/eu/europa/esig/dss/pdf/pdfbox/visible/SignatureImageAndPositionProcessor.java
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import eu.europa.esig.dss.pades.SignatureImageParameters;
import eu.europa.esig.dss.pdf.visible.ImageAndResolution;
import eu.europa.esig.dss.pdf.visible.ImageUtils;

public final class SignatureImageAndPositionProcessor {

	private SignatureImageAndPositionProcessor() {
	}

    private static final int ANGLE_360 = 360;
    private static final int ANGLE_90 = 90;
    private static final int ANGLE_180 = 180;
    private static final int ANGLE_270 = 270;

    private static final String SUPPORTED_ANGLES_ERROR_MESSAGE = "rotation angle must be 90, 180, 270 or 360 (0)";
    private static final String SUPPORTED_VERTICAL_ALIGNMENT_ERROR_MESSAGE = "not supported vertical alignment: ";
    private static final String SUPPORTED_HORIZONTAL_ALIGNMENT_ERROR_MESSAGE = "not supported horizontal alignment: ";

<<<<<<< HEAD:dss-pades/src/main/java/eu/europa/esig/dss/pdf/pdfbox/SignatureImageAndPositionProcessor.java
    public static SignatureImageAndPosition process(final SignatureImageParameters signatureImageParameters, final PDDocument doc, final ImageAndResolution ires, int page, float x, float y) throws IOException {
        BufferedImage visualImageSignature = ImageIO.read(ires.getInputStream());
        PDPage pdPage = doc.getPages().get(page);

        int rotate = getRotation(signatureImageParameters.getRotation(), pdPage);
        if(rotate != ANGLE_360) {
            visualImageSignature = ImageUtils.rotate(visualImageSignature, rotate);
        }

        x = processX(rotate, ires, visualImageSignature, pdPage, signatureImageParameters, x, y);
        y = processY(rotate, ires, visualImageSignature, pdPage, signatureImageParameters, x, y);

        ByteArrayOutputStream visualImageSignatureOutputStream = new ByteArrayOutputStream();
        String imageType = "jpg";
        if(visualImageSignature.getColorModel().hasAlpha()) {
            imageType = "png";
        }
        ImageIO.write(visualImageSignature, imageType, visualImageSignatureOutputStream);

        return new SignatureImageAndPosition(x, y, visualImageSignatureOutputStream.toByteArray());
=======
    public static SignatureImageAndPosition process(final SignatureImageParameters signatureImageParameters, final PDDocument doc, final ImageAndResolution ires) throws IOException {
		try (InputStream is = ires.getInputStream()) {
			BufferedImage visualImageSignature = ImageIO.read(is);
			PDPage pdPage = doc.getPages().get(signatureImageParameters.getPage() - 1);

			int rotate = getRotation(signatureImageParameters.getRotation(), pdPage);
			if (rotate != ANGLE_360) {
				visualImageSignature = ImageUtils.rotate(visualImageSignature, rotate);
			}

			float x = processX(rotate, ires, visualImageSignature, pdPage, signatureImageParameters);
			float y = processY(rotate, ires, visualImageSignature, pdPage, signatureImageParameters);

			ByteArrayOutputStream visualImageSignatureOutputStream = new ByteArrayOutputStream();
			String imageType = "jpg";
			if (visualImageSignature.getColorModel().hasAlpha()) {
				imageType = "png";
			}
			ImageIO.write(visualImageSignature, imageType, visualImageSignatureOutputStream);

			return new SignatureImageAndPosition(x, y, visualImageSignatureOutputStream.toByteArray());
		}
>>>>>>> b24548fbe36b84bb32fd0a0902c84ba90242b603:dss-pades-pdfbox/src/main/java/eu/europa/esig/dss/pdf/pdfbox/visible/SignatureImageAndPositionProcessor.java
    }

    private static float processX(int rotate, ImageAndResolution ires, BufferedImage visualImageSignature, PDPage pdPage, SignatureImageParameters signatureImageParameters, float x, float y) {

        PDRectangle mediaBox = pdPage.getMediaBox();

        SignatureImageParameters.VisualSignatureAlignmentHorizontal alignmentHorizontal = getVisualSignatureAlignmentHorizontal(signatureImageParameters);
        SignatureImageParameters.VisualSignatureAlignmentVertical alignmentVertical = getVisualSignatureAlignmentVertical(signatureImageParameters);
        
        switch (rotate) {
            case ANGLE_90:
                x = processXAngle90(mediaBox, ires, alignmentVertical, x, y, visualImageSignature);
                break;
            case ANGLE_180:
                x = processXAngle180(mediaBox, ires, alignmentHorizontal, x, y, visualImageSignature);
                break;
            case ANGLE_270:
                x = processXAngle270(mediaBox, ires, alignmentVertical, x, y, visualImageSignature);
                break;
            case ANGLE_360:
                x = processXAngle360(mediaBox, ires, alignmentHorizontal, x, y, visualImageSignature);
                break;
            default:
                throw new IllegalStateException(SUPPORTED_ANGLES_ERROR_MESSAGE);
        }

        return x;
    }

    private static float processY(int rotate, ImageAndResolution ires, BufferedImage visualImageSignature, PDPage pdPage, SignatureImageParameters signatureImageParameters, float x, float y) {

        PDRectangle mediaBox = pdPage.getMediaBox();

        SignatureImageParameters.VisualSignatureAlignmentHorizontal alignmentHorizontal = getVisualSignatureAlignmentHorizontal(signatureImageParameters);
        SignatureImageParameters.VisualSignatureAlignmentVertical alignmentVertical = getVisualSignatureAlignmentVertical(signatureImageParameters);
        
        switch (rotate) {
            case ANGLE_90:
                y = processYAngle90(mediaBox, ires, alignmentHorizontal, x, y, visualImageSignature);
                break;
            case ANGLE_180:
                y = processYAngle180(mediaBox, ires, alignmentVertical, x, y, visualImageSignature);
                break;
            case ANGLE_270:
                y = processYAngle270(mediaBox, ires, alignmentHorizontal, x, y, visualImageSignature);
                break;
            case ANGLE_360:
                y = processYAngle360(mediaBox, ires, alignmentVertical, x, y, visualImageSignature);
                break;
            default:
                throw new IllegalStateException(SUPPORTED_ANGLES_ERROR_MESSAGE);
        }

        return y;
    }

    private static float processXAngle90(PDRectangle mediaBox, ImageAndResolution ires, 
    		VisualSignatureAlignmentVertical alignmentVertical, float x, float y,
    		BufferedImage visualImageSignature) {

        switch (alignmentVertical) {
            case TOP:
            case NONE:
                x = mediaBox.getWidth() - ires.toXPoint(visualImageSignature.getWidth()) - y;
                break;
            case MIDDLE:
                x = (mediaBox.getWidth() - ires.toXPoint(visualImageSignature.getWidth())) / 2;
                break;
            case BOTTOM:
                x = y;
                break;
            default:
                throw new IllegalStateException(SUPPORTED_VERTICAL_ALIGNMENT_ERROR_MESSAGE + alignmentVertical.name());
        }

        return x;
    }

    private static float processXAngle180(PDRectangle mediaBox, ImageAndResolution ires, 
    		VisualSignatureAlignmentHorizontal alignmentHorizontal, float x, float y,
    		BufferedImage visualImageSignature) {
        switch (alignmentHorizontal) {
            case LEFT:
            case NONE:
                x = mediaBox.getWidth() - ires.toXPoint(visualImageSignature.getWidth()) - x;
                break;
            case CENTER:
                x = (mediaBox.getWidth() - ires.toXPoint(visualImageSignature.getWidth())) / 2;
                break;
            case RIGHT:
                break;
            default:
                throw new IllegalStateException(SUPPORTED_HORIZONTAL_ALIGNMENT_ERROR_MESSAGE + alignmentHorizontal.name());
        }

        return x;
    }

    private static float processXAngle270(PDRectangle mediaBox, ImageAndResolution ires, 
    		VisualSignatureAlignmentVertical alignmentVertical, float x, float y,
    		BufferedImage visualImageSignature) {

        switch (alignmentVertical) {
            case TOP:
            case NONE:
                x = y;
                break;
            case MIDDLE:
                x = (mediaBox.getWidth() - ires.toXPoint(visualImageSignature.getWidth())) / 2;
                break;
            case BOTTOM:
                x = mediaBox.getWidth() - ires.toXPoint(visualImageSignature.getWidth()) - y;
                break;
            default:
                throw new IllegalStateException(SUPPORTED_VERTICAL_ALIGNMENT_ERROR_MESSAGE + alignmentVertical.name());
        }

        return x;
    }

    private static float processXAngle360(PDRectangle mediaBox, ImageAndResolution ires, 
    		VisualSignatureAlignmentHorizontal alignmentHorizontal, float x, float y,
    		BufferedImage visualImageSignature) {

        switch (alignmentHorizontal) {
            case LEFT:
            case NONE:
                break;
            case CENTER:
                x = (mediaBox.getWidth() - ires.toXPoint(visualImageSignature.getWidth())) / 2;
                break;
            case RIGHT:
                x = mediaBox.getWidth() -ires.toXPoint(visualImageSignature.getWidth()) - x;
                break;
            default:
                throw new IllegalStateException(SUPPORTED_HORIZONTAL_ALIGNMENT_ERROR_MESSAGE + alignmentHorizontal.name());
        }

        return x;
    }

    private static float processYAngle90(PDRectangle mediaBox, ImageAndResolution ires, 
    		VisualSignatureAlignmentHorizontal alignmentHorizontal, float x, float y, 
    		BufferedImage visualImageSignature) {

        switch (alignmentHorizontal) {
            case LEFT:
            case NONE:
                y = x;
                break;
            case CENTER:
                y = (mediaBox.getHeight() - ires.toXPoint(visualImageSignature.getHeight())) / 2;
                break;
            case RIGHT:
                y = mediaBox.getHeight() - ires.toYPoint(visualImageSignature.getHeight()) - x;
                break;
            default:
                throw new IllegalStateException(SUPPORTED_HORIZONTAL_ALIGNMENT_ERROR_MESSAGE + alignmentHorizontal.name());
        }

        return y;
    }

    private static float processYAngle180(PDRectangle mediaBox, ImageAndResolution ires, 
    		VisualSignatureAlignmentVertical alignmentVertical, float x, float y,
    		BufferedImage visualImageSignature) {

        switch (alignmentVertical) {
            case TOP:
            case NONE:
                y = mediaBox.getHeight() - ires.toYPoint(visualImageSignature.getHeight()) - y;
                break;
            case MIDDLE:
                y = (mediaBox.getHeight() - ires.toYPoint(visualImageSignature.getHeight())) / 2;
                break;
            case BOTTOM:
                break;
            default:
                throw new IllegalStateException(SUPPORTED_VERTICAL_ALIGNMENT_ERROR_MESSAGE + alignmentVertical.name());
        }

        return y;
    }

    private static float processYAngle270(PDRectangle mediaBox, ImageAndResolution ires, 
    		VisualSignatureAlignmentHorizontal alignmentHorizontal, float x, float y,
    		BufferedImage visualImageSignature) {

        switch (alignmentHorizontal) {
            case LEFT:
            case NONE:
                y = mediaBox.getHeight() - ires.toYPoint(visualImageSignature.getHeight()) - x;
                break;
            case CENTER:
                y = (mediaBox.getHeight() - ires.toXPoint(visualImageSignature.getHeight())) / 2;
                break;
            case RIGHT:
                y = x;
                break;
            default:
                throw new IllegalStateException(SUPPORTED_HORIZONTAL_ALIGNMENT_ERROR_MESSAGE + alignmentHorizontal.name());
        }

        return y;
    }

    private static float processYAngle360(PDRectangle mediaBox, ImageAndResolution ires, 
    		VisualSignatureAlignmentVertical alignmentVertical, float x, float y,
    		BufferedImage visualImageSignature) {

        switch (alignmentVertical) {
            case TOP:
            case NONE:
                break;
            case MIDDLE:
                y = (mediaBox.getHeight() - ires.toYPoint(visualImageSignature.getHeight())) / 2;
                break;
            case BOTTOM:
                y = mediaBox.getHeight() - ires.toYPoint(visualImageSignature.getHeight()) - y;
                break;
            default:
                throw new IllegalStateException(SUPPORTED_VERTICAL_ALIGNMENT_ERROR_MESSAGE + alignmentVertical.name());
        }

        return y;
    }

    private static SignatureImageParameters.VisualSignatureAlignmentVertical getVisualSignatureAlignmentVertical(SignatureImageParameters signatureImageParameters) {
        SignatureImageParameters.VisualSignatureAlignmentVertical alignmentVertical = signatureImageParameters.getAlignmentVertical();
        if(alignmentVertical == null) {
            alignmentVertical = SignatureImageParameters.VisualSignatureAlignmentVertical.NONE;
        }

        return alignmentVertical;
    }

    private static SignatureImageParameters.VisualSignatureAlignmentHorizontal getVisualSignatureAlignmentHorizontal(SignatureImageParameters signatureImageParameters) {
        SignatureImageParameters.VisualSignatureAlignmentHorizontal alignmentHorizontal = signatureImageParameters.getAlignmentHorizontal();
        if(alignmentHorizontal == null) {
            alignmentHorizontal = SignatureImageParameters.VisualSignatureAlignmentHorizontal.NONE;
        }

        return alignmentHorizontal;
    }

    private static boolean needRotation(SignatureImageParameters.VisualSignatureRotation visualSignatureRotation) {
        return visualSignatureRotation != null && !SignatureImageParameters.VisualSignatureRotation.NONE.equals(visualSignatureRotation);
    }

    private static int getRotation(SignatureImageParameters.VisualSignatureRotation visualSignatureRotation, PDPage pdPage) {
        int rotate = ANGLE_360;

        if(needRotation(visualSignatureRotation)) {
            switch (visualSignatureRotation) {
                case AUTOMATIC:
                    rotate = ANGLE_360 - pdPage.getRotation();
                    break;
                case ROTATE_90:
                    rotate = ANGLE_90;
                    break;
                case ROTATE_180:
                    rotate = ANGLE_180;
                    break;
                case ROTATE_270:
                    rotate = ANGLE_270;
                    break;
                default:
                    throw new IllegalStateException(SUPPORTED_ANGLES_ERROR_MESSAGE);
            }
        }

        return rotate;
    }
}
