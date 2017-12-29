package eu.europa.esig.dss;

import java.awt.Font;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import eu.europa.esig.dss.FontAdapter.FontValue;

public class FontAdapter extends XmlAdapter<FontValue, Font>{

    @Override
    public Font unmarshal(FontValue v) throws Exception {
        return new Font(v.getName(), v.getType().getConstant(), v.getSize());
    }

    @Override
    public FontValue marshal(Font v) throws Exception {
        FontValue result = new FontValue();
        result.setName(v.getName());
        result.setSize(v.getSize());
        result.setType(v.isBold() ? FontType.BOLD : v.isItalic() ? FontType.ITALIC : FontType.NORMAL);
        return result;
    }

    public static class FontValue {
        private String name;
        private int size;
        private FontType type;
        
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public int getSize() {
            return size;
        }
        public void setSize(int size) {
            this.size = size;
        }
        public FontType getType() {
            return type;
        }
        public void setType(FontType type) {
            this.type = type;
        }
    }
    
    public static enum FontType {
        NORMAL(Font.PLAIN), BOLD(Font.BOLD), ITALIC(Font.ITALIC);
        private final int constant;
        
        private FontType(int constant) {
            this.constant = constant;
        }

        public int getConstant() {
            return constant;
        }
    }
}