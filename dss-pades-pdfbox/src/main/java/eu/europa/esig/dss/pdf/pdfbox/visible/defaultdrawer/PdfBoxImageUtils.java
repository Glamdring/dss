package eu.europa.esig.dss.pdf.pdfbox.visible.defaultdrawer;

import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.Locale;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * Class with utility methods for drawing on a PDF
 *
 */
public class PdfBoxImageUtils {

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
