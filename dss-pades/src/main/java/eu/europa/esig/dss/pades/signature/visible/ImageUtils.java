package eu.europa.esig.dss.pades.signature.visible;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import eu.europa.esig.dss.DSSDocument;
import eu.europa.esig.dss.DSSException;
import eu.europa.esig.dss.FileDocument;
import eu.europa.esig.dss.InMemoryDocument;
import eu.europa.esig.dss.MimeType;
import eu.europa.esig.dss.SignatureImageParameters;
import eu.europa.esig.dss.SignatureImageTextParameters;
import eu.europa.esig.dss.SignatureImageTextParameters.SignerPosition;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.x509.CertificateToken;

/**
 * A static utilities that helps in creating ImageAndResolution
 * 
 * @author pakeyser
 *
 */
public class ImageUtils {

	private static final Logger LOG = LoggerFactory.getLogger(ImageUtils.class);

	private static final int DPI = 300;

	private static final int[] IMAGE_TRANSPARENT_TYPES;

	static {
		int[] imageAlphaTypes = new int[] { BufferedImage.TYPE_4BYTE_ABGR, BufferedImage.TYPE_4BYTE_ABGR_PRE, BufferedImage.TYPE_INT_ARGB,
				BufferedImage.TYPE_INT_ARGB_PRE };
		Arrays.sort(imageAlphaTypes);
		IMAGE_TRANSPARENT_TYPES = imageAlphaTypes;
	}

	private ImageUtils() {
	}
	
	public static ImageAndResolution create(final SignatureImageParameters imageParameters, CertificateToken signingCertificate, Date signingDate, File signatureImageDir) throws IOException {
		SignatureImageTextParameters textLeftParameters = imageParameters.getTextParameters();
		
		// the image can be specified either as a DSSDocument or as RemoteDocument. In the latter case, we convert it
		DSSDocument image = imageParameters.getImage();
		if (image == null && imageParameters.getImageDocument() != null) {
			if (imageParameters.getImageDocument().getBytes() != null) {
				image = new InMemoryDocument(imageParameters.getImageDocument().getBytes(), 
						imageParameters.getImageDocument().getName(), 
						imageParameters.getImageDocument().getMimeType());
			} else if (signatureImageDir != null){
				image = new FileDocument(new File(signatureImageDir, imageParameters.getImageDocument().getName()));
			}
		}
		
		if (textLeftParameters != null && Utils.isStringNotEmpty(textLeftParameters.getText())) {
			Color textBackground = textLeftParameters.getBackgroundColor();
			if (textLeftParameters.getSignerNamePosition() == SignerPosition.FOREGROUND) {
				// fully transparent if two text images are going to be merged (their transparency is set when merging)
				// partially-transparent if only the left image is going to be used
				if (imageParameters.getTextRightParameters() != null) {
					textBackground = new Color(255, 255, 255, 0);
				} else {
					textBackground = new Color(255, 255, 255, imageParameters.getBackgroundOpacity());
				}
			}
			
			String transformedText = transformText(textLeftParameters.getText(), imageParameters.getDateFormat(), signingDate, signingCertificate);
			BufferedImage buffImg = ImageTextWriter.createTextImage(transformedText, textLeftParameters.getFont(), textLeftParameters.getTextColor(),
					textBackground, getDpi(imageParameters.getDpi()), textLeftParameters.getSignerTextHorizontalAlignment(), textLeftParameters.getRightPadding());
			
			// in case there's a right side configured, join it with the left side of the text to form a single text image
			SignatureImageTextParameters textRightParameters = imageParameters.getTextRightParameters();
			if (textRightParameters != null) {
				String transformedRightText = transformText(textRightParameters.getText(), imageParameters.getDateFormat(), signingDate, signingCertificate);
				BufferedImage rightImg = ImageTextWriter.createTextImage(transformedRightText, textRightParameters.getFont(), textRightParameters.getTextColor(),
						textBackground, getDpi(imageParameters.getDpi()), textRightParameters.getSignerTextHorizontalAlignment(), 0);
				
				buffImg = ImagesMerger.mergeOnRight(buffImg, rightImg, 
						new Color(255, 255, 255, imageParameters.getBackgroundOpacity()), 
						imageParameters.getSignerTextImageVerticalAlignment());
			}
			
			if (image != null) {
				try (InputStream is = image.openStream()) {
					if (is != null) {
						switch (textLeftParameters.getSignerNamePosition()) {
						case LEFT:
							buffImg = ImagesMerger.mergeOnRight(ImageIO.read(is), buffImg, textLeftParameters.getBackgroundColor(),
									imageParameters.getSignerTextImageVerticalAlignment());
							break;
						case RIGHT:
							buffImg = ImagesMerger.mergeOnRight(buffImg, ImageIO.read(is), textLeftParameters.getBackgroundColor(),
									imageParameters.getSignerTextImageVerticalAlignment());
							break;
						case TOP:
							buffImg = ImagesMerger.mergeOnTop(ImageIO.read(is), buffImg, textLeftParameters.getBackgroundColor());
							break;
						case BOTTOM:
							buffImg = ImagesMerger.mergeOnTop(buffImg, ImageIO.read(is), textLeftParameters.getBackgroundColor());
							break;
						case FOREGROUND:
							buffImg = ImagesMerger.mergeOnBackground(buffImg, ImageIO.read(is));
							break;
						default:
							break;
						}
					}
				}
			}
			return convertToInputStream(buffImg, getDpi(imageParameters.getDpi()));
		}

		// Image only
		return readAndDisplayMetadata(image);
	}

	private static String transformText(String text, String dateFormat, Date signingDate, CertificateToken signingCertificate) {
		if (signingCertificate != null) {
			try {
				String principal = signingCertificate.getSubjectX500Principal().getName().replace("+", ",");
				LdapName ldapName = new LdapName(principal);
				String[] names = ldapName.getRdns().stream()
						.filter(rdn -> rdn.getType().equals("CN"))
						.map(Rdn::getValue)
						.map(String.class::cast)
						.findFirst().orElse("").split(" ");
				text = text.replace("%CN_1%", names[0]);
				if (names.length > 1) {
					text = text.replace("%CN_2%", names[1]);
				} else {
					text = text.replace("%CN_2%", "");
				}
				if (names.length > 2) {
					text = text.replace("%CN_3%", names[2]);
				} else {
					text = text.replace("%CN_3%", "");
				}
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		if (signingDate == null) {
			signingDate = new Date();
		}
		if (text.contains("%DateTimeWithTimeZone%")) {
			SimpleDateFormat df = new SimpleDateFormat(dateFormat);
			text = text.replace("%DateTimeWithTimeZone%", df.format(signingDate));
		}
		return text;
	}

	/**
	 * This method returns the image size with the original parameters (the generation uses DPI)
	 * 
	 * @param imageParameters
	 *            the image parameters
	 * @return a Dimension object
	 * @throws IOException
	 */
	static Dimension getOptimalSize(SignatureImageParameters imageParameters) throws IOException {
		int width = 0;
		int height = 0;

		DSSDocument docImage = imageParameters.getImage();
		if (docImage != null) {
			BufferedImage image = ImageIO.read(docImage.openStream());
			width = image.getWidth();
			height = image.getHeight();
		}

		SignatureImageTextParameters textParamaters = imageParameters.getTextParameters();
		if ((textParamaters != null) && !textParamaters.getText().isEmpty()) {
			Dimension textDimension = getTextDimension(textParamaters.getText(), textParamaters.getFont(), imageParameters.getDpi());
			switch (textParamaters.getSignerNamePosition()) {
			case LEFT:
			case RIGHT:
				width += textDimension.width;
				height = Math.max(height, textDimension.height);
				break;
			case TOP:
			case BOTTOM:
				width = Math.max(width, textDimension.width);
				height += textDimension.height;
				break;
			default:
				break;
			}

		}

		float ration = getRation(imageParameters.getDpi());
		return new Dimension(Math.round(width / ration), Math.round(height / ration));
	}

	private static ImageAndResolution readAndDisplayMetadata(DSSDocument image) throws IOException {
		if (isImageWithContentType(image, MimeType.JPEG)) {
			return readAndDisplayMetadataJPEG(image);
		} else if (isImageWithContentType(image, MimeType.PNG)) {
			return readAndDisplayMetadataPNG(image);
		}
		throw new DSSException("Unsupported image type");
	}

	private static boolean isImageWithContentType(DSSDocument image, MimeType expectedContentType) {
		if (image.getMimeType() != null) {
			return expectedContentType == image.getMimeType();
		} else {
			String contentType = null;
			try {
				contentType = Files.probeContentType(Paths.get(image.getName()));
			} catch (IOException e) {
				LOG.warn("Unable to retrieve the content-type : " + e.getMessage());
			}
			return Utils.areStringsEqual(expectedContentType.getMimeTypeString(), contentType);
		}
	}

	private static ImageAndResolution readAndDisplayMetadataJPEG(DSSDocument image) throws IOException {
		try (InputStream is = image.openStream(); ImageInputStream iis = ImageIO.createImageInputStream(is)) {

			ImageReader reader = getImageReader("jpeg");
			// attach source to the reader
			reader.setInput(iis, true);

			// read metadata of first image
			IIOMetadata metadata = reader.getImageMetadata(0);

			Element root = (Element) metadata.getAsTree("javax_imageio_jpeg_image_1.0");

			NodeList elements = root.getElementsByTagName("app0JFIF");

			Element e = (Element) elements.item(0);
			int x = Integer.parseInt(e.getAttribute("Xdensity"));
			int y = Integer.parseInt(e.getAttribute("Ydensity"));

			return new ImageAndResolution(image.openStream(), x, y);
		}
	}

	private static ImageAndResolution readAndDisplayMetadataPNG(DSSDocument image) throws IOException {
		try (InputStream is = image.openStream(); ImageInputStream iis = ImageIO.createImageInputStream(is)) {

			ImageReader reader = getImageReader("png");
			// attach source to the reader
			reader.setInput(iis, true);

			// read metadata of first image
			IIOMetadata metadata = reader.getImageMetadata(0);

			int hdpi = 96, vdpi = 96;
			double mm2inch = 25.4;

			Element node = (Element) metadata.getAsTree("javax_imageio_1.0");
			NodeList lst = node.getElementsByTagName("HorizontalPixelSize");
			if (lst != null && lst.getLength() == 1) {
				hdpi = (int) (mm2inch / Float.parseFloat(((Element) lst.item(0)).getAttribute("value")));
			}

			lst = node.getElementsByTagName("VerticalPixelSize");
			if (lst != null && lst.getLength() == 1) {
				vdpi = (int) (mm2inch / Float.parseFloat(((Element) lst.item(0)).getAttribute("value")));
			}

			return new ImageAndResolution(image.openStream(), hdpi, vdpi);
		}
	}

	private static Dimension getTextDimension(String text, Font font, Integer dpi) {
		float fontSize = Math.round((font.getSize() * getDpi(dpi)) / (float) ImageTextWriter.PDF_DEFAULT_DPI);
		Font largerFont = font.deriveFont(fontSize);
		return ImageTextWriter.computeSize(largerFont, text);
	}

	private static ImageAndResolution convertToInputStream(BufferedImage buffImage, int dpi) throws IOException {
		if (isTransparent(buffImage)) {
			return convertToInputStreamPNG(buffImage, dpi);
		} else {
			return convertToInputStreamJPG(buffImage, dpi);
		}
	}

	private static ImageAndResolution convertToInputStreamJPG(BufferedImage buffImage, int dpi) throws IOException {
		ImageWriter writer = getImageWriter("jpeg");

		JPEGImageWriteParam jpegParams = (JPEGImageWriteParam) writer.getDefaultWriteParam();
		jpegParams.setCompressionMode(JPEGImageWriteParam.MODE_EXPLICIT);
		jpegParams.setCompressionQuality(1);

		ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
		IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, jpegParams);

		initDpiJPG(metadata, dpi);

		return getImageAndResolution(buffImage, dpi, writer, jpegParams, metadata);
	}

	private static void initDpiJPG(IIOMetadata metadata, int dpi) throws IIOInvalidTreeException {
		Element tree = (Element) metadata.getAsTree("javax_imageio_jpeg_image_1.0");
		Element jfif = (Element) tree.getElementsByTagName("app0JFIF").item(0);
		jfif.setAttribute("Xdensity", Integer.toString(dpi));
		jfif.setAttribute("Ydensity", Integer.toString(dpi));
		jfif.setAttribute("resUnits", "1"); // density is dots per inch
		metadata.setFromTree("javax_imageio_jpeg_image_1.0", tree);
	}

	private static ImageAndResolution convertToInputStreamPNG(BufferedImage buffImage, int dpi) throws IOException {
		ImageWriter writer = getImageWriter("png");

		ImageWriteParam imageWriterParams = writer.getDefaultWriteParam();

		ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_ARGB);
		IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, imageWriterParams);

		initDpiPNG(metadata, dpi);

		return getImageAndResolution(buffImage, dpi, writer, imageWriterParams, metadata);
	}

	private static ImageAndResolution getImageAndResolution(BufferedImage buffImage, int dpi, ImageWriter writer, ImageWriteParam imageWriterParams,
			IIOMetadata metadata) throws IOException {
		try (ByteArrayOutputStream os = new ByteArrayOutputStream(); ImageOutputStream imageOs = ImageIO.createImageOutputStream(os)) {
			writer.setOutput(imageOs);
			writer.write(metadata, new IIOImage(buffImage, null, metadata), imageWriterParams);
			InputStream is = new ByteArrayInputStream(os.toByteArray());
			return new ImageAndResolution(is, dpi, dpi);
		}
	}

	private static void initDpiPNG(IIOMetadata metadata, int dpi) throws IIOInvalidTreeException {

		// for PNG, it's dots per millimeter
		double dotsPerMilli = 1.0 * dpi / 25.4;

		IIOMetadataNode horiz = new IIOMetadataNode("HorizontalPixelSize");
		horiz.setAttribute("value", Double.toString(dotsPerMilli));

		IIOMetadataNode vert = new IIOMetadataNode("VerticalPixelSize");
		vert.setAttribute("value", Double.toString(dotsPerMilli));

		IIOMetadataNode dim = new IIOMetadataNode("Dimension");
		dim.appendChild(horiz);
		dim.appendChild(vert);

		IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
		root.appendChild(dim);

		metadata.mergeTree("javax_imageio_1.0", root);
	}

	static int getDpi(Integer dpi) {
		int result = DPI;
		if (dpi != null && dpi.intValue() > 0) {
			result = dpi.intValue();
		}
		return result;
	}

	private static float getRation(Integer dpi) {
		float floatDpi = getDpi(dpi);
		return floatDpi / ImageTextWriter.PDF_DEFAULT_DPI;
	}

	public static boolean isTransparent(BufferedImage bufferedImage) {
		int type = bufferedImage.getType();

		return Arrays.binarySearch(IMAGE_TRANSPARENT_TYPES, type) > -1;
	}

	public static void initRendering(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
	}

	public static BufferedImage rotate(BufferedImage image, double angle) {
		double sin = Math.abs(Math.sin(Math.toRadians(angle))), cos = Math.abs(Math.cos(Math.toRadians(angle)));

		int w = image.getWidth();
		int h = image.getHeight();

		int neww = (int) Math.floor(w * cos + h * sin);
		int newh = (int) Math.floor(h * cos + w * sin);

		BufferedImage result = new BufferedImage(neww, newh, image.getType());
		Graphics2D g = result.createGraphics();

		g.translate((neww - w) / 2, (newh - h) / 2);
		g.rotate(Math.toRadians(angle), w / 2, h / 2);
		g.drawRenderedImage(image, null);
		g.dispose();

		return result;
	}

	private static ImageWriter getImageWriter(String type) {
		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(type);
		if (!writers.hasNext()) {
			throw new DSSException("No writer for '" + type + "' found");
		}
		return writers.next();
	}

	private static ImageReader getImageReader(String type) {
		Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(type);
		if (!readers.hasNext()) {
			throw new DSSException("No reader for '" + type + "' found");
		}
		// pick the first available ImageReader
		return readers.next();
	}
	
	private static final String SAVE_GRAPHICS_STATE = "q\n";
	private static final String RESTORE_GRAPHICS_STATE = "Q\n";
	private static final String CONCATENATE_MATRIX = "cm\n";
	private static final String XOBJECT_DO = "Do\n";
	private static final String SPACE = " ";
	private static final NumberFormat formatDecimal = NumberFormat.getNumberInstance(Locale.US);
	
	public static void drawXObject(PDImageXObject xobject, PDResources resources, OutputStream os, 
            float x, float y, float width, float height) throws IOException {
		COSName xObjectId = resources.add(xobject);

        appendRawCommands(os, SAVE_GRAPHICS_STATE);
        appendRawCommands(os, formatDecimal.format(width));
        appendRawCommands(os, SPACE);
        appendRawCommands(os, formatDecimal.format(0));
        appendRawCommands(os, SPACE);
        appendRawCommands(os, formatDecimal.format(0));
        appendRawCommands(os, SPACE);
        appendRawCommands(os, formatDecimal.format(height));
        appendRawCommands(os, SPACE);
        appendRawCommands(os, formatDecimal.format(x));
        appendRawCommands(os, SPACE);
        appendRawCommands(os, formatDecimal.format(y));
        appendRawCommands(os, SPACE);
        appendRawCommands(os, CONCATENATE_MATRIX);
        appendRawCommands(os, SPACE);
        appendRawCommands(os, "/");
        appendRawCommands(os, xObjectId.getName());
        appendRawCommands(os, SPACE);
        appendRawCommands(os, XOBJECT_DO);
        appendRawCommands(os, SPACE);
        appendRawCommands(os, RESTORE_GRAPHICS_STATE);
    }

	private static void appendRawCommands(OutputStream os, String commands) throws IOException {
		os.write(commands.getBytes("ISO-8859-1"));
	}

}
