package eu.europa.esig.dss;

import java.awt.Color;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class ColorAdapter extends XmlAdapter<Integer, Color> {

    @Override
    public Color unmarshal(Integer sRGB) throws Exception {
        return new Color(sRGB);
    }

    @Override
    public Integer marshal(Color v) throws Exception {
        return v.getRGB();
    }

}
