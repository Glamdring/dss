package eu.europa.esig.dss.model.pades;

import java.io.File;
import java.util.Base64;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.model.InMemoryDocument;

public class DSSDocumentAdapter extends XmlAdapter<String, DSSDocument> {

    public static String imageDir; 
    
    @Override
    public DSSDocument unmarshal(String v) throws Exception {
        if (v.contains("file:")) {
            String fileName = v.trim().replace("file:", "");
            return new FileDocument(new File(imageDir, fileName));
        } else {
            return new InMemoryDocument(Base64.getDecoder().decode(v));
        }
    }

    @Override
    public String marshal(DSSDocument v) throws Exception {
        // not needed
        return null;
    }

}
