package eu.europa.esig.dss.cookbook.example.snippets;

import java.awt.Color;

import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.model.pades.SignatureImageParameters;
import eu.europa.esig.dss.model.pades.SignatureImageParameters.VisualSignatureAlignmentHorizontal;
import eu.europa.esig.dss.model.pades.SignatureImageParameters.VisualSignatureAlignmentVertical;
import eu.europa.esig.dss.model.pades.SignatureImageParameters.VisualSignatureRotation;

public class PAdESVisibleSignatureSnippet {
	
	public void demo() {

		// tag::visibleSigParams[]
		// Instantiate PAdES-specific parameters
		PAdESSignatureParameters padesSignatureParameters = new PAdESSignatureParameters();
		
		// tag::positioning[]
		
		// Object containing a list of visible signature parameters
		SignatureImageParameters signatureImageParameters = new SignatureImageParameters();
		
		// Allows defining of a specific page in a PDF document where the signature must be placed. 
		// The counting of pages starts from 1 (the first page) 
		// (the default value = 1).
		signatureImageParameters.setPage(1);
		
		// Absolute positioning functions, allowing to specify a margin between 
		// the left page side and the top page side respectively, and
		// a signature field (if no rotation and alignment is applied).
		signatureImageParameters.setxAxis(10);
		signatureImageParameters.setyAxis(10);
		
		// Allows alignment of a signature field horizontally to a page. Allows the following values:
		/* _NONE_ (_DEFAULT value._ None alignment is applied, coordinates are counted from the left page side);
		   _LEFT_ (the signature is aligned to the left side, coordinated are counted from the left page side);
		   _CENTER_ (the signature is aligned to the center of the page, coordinates are counted automatically);
		   _RIGHT_ (the signature is aligned to the right side, coordinated are counted from the right page side). */
		signatureImageParameters.setAlignmentHorizontal(VisualSignatureAlignmentHorizontal.CENTER);
		
		// Allows alignment of a signature field vertically to a page. Allows the following values:
		/* _NONE_ (_DEFAULT value._ None alignment is applied, coordinated are counted from the top side of a page);
		   _TOP_ (the signature is aligned to a top side, coordinated are counted from the top page side);
		   _MIDDLE_ (the signature aligned to a middle of a page, coordinated are counted automatically);
		   _BOTTOM_ (the signature is aligned to a bottom side, coordinated are counted from the bottom page side). */
		signatureImageParameters.setAlignmentVertical(VisualSignatureAlignmentVertical.TOP);
		
		// Rotates the signature field and changes the coordinates' origin respectively to its values as following:
		/* _NONE_ (_DEFAULT value._ No rotation is applied. The origin of coordinates begins from the top left corner of a page);
		   _AUTOMATIC_ (Rotates a signature field respectively to the page's rotation. Rotates the signature field on the same value as a defined in a PDF page);
		   _ROTATE_90_ (Rotates a signature field for a 90&#176; clockwise. Coordinates' origin begins from top right page corner);
		   _ROTATE_180_ (Rotates a signature field for a 180&#176; clockwise. Coordinates' origin begins from the bottom right page corner);
		   _ROTATE_270_ (Rotates a signature field for a 270&#176; clockwise. Coordinates' origin begins from the bottom left page corner). */
		signatureImageParameters.setRotation(VisualSignatureRotation.AUTOMATIC);
		
		padesSignatureParameters.setImageParameters(signatureImageParameters);
		
		// end::positioning[]
		
		// tag::dimensions[]
		
		// Allows specifying of a precise signature field's width in pixels. 
		// If not defined, the default image/text width will be used.
		signatureImageParameters.setWidth(100);
		
		// Allows specifying of a precise signature field's height in pixels. 
		// If not defined, the default image/text height will be used.
		signatureImageParameters.setHeight(125);
		
		// Defines a zoom of the image. The value is applied to width and height of a signature field. 
		// The value must be defined in percentage (default value is 100, no zoom is applied).
		signatureImageParameters.setZoom(50);
		
		// Specifies a background color for a signature field.
		signatureImageParameters.setBackgroundColor(Color.GREEN);
		
		// end::dimensions[]
		
		// end::visibleSigParams[]
		
	}

}
