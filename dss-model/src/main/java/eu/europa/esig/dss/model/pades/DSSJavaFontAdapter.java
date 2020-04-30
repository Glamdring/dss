package eu.europa.esig.dss.model.pades;

import java.awt.Color;
import java.awt.Font;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class DSSJavaFontAdapter extends XmlAdapter<DSSJavaFont.XMLFont, DSSJavaFont> {

    @Override
    public DSSJavaFont unmarshal(DSSJavaFont.XMLFont font) throws Exception {
        return new DSSJavaFont(font.getName(), getStyle(font.getType()), font.getSize());
    }

    private int getStyle(String type) {
        if (type == null || type.equals("NORMAL")) {
            return Font.PLAIN;
        } else if (type.equals("ITALIC")) {
            return Font.ITALIC;
        } else if (type.equals("BOLD")) {
            return Font.BOLD;
        } else {
            return Font.PLAIN;
        }
    }

    @Override
    public DSSJavaFont.XMLFont marshal(DSSJavaFont v) throws Exception {
        // not needed
        return null;
    }
}
