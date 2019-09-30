package eu.europa.esig.dss.model.pades;

import java.util.Base64;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;

public class DSSDocumentAdapter extends XmlAdapter<String, DSSDocument> {

    @Override
    public DSSDocument unmarshal(String v) throws Exception {
        DSSDocument document = new InMemoryDocument(Base64.getDecoder().decode(v));
        return document;
    }

    @Override
    public String marshal(DSSDocument v) throws Exception {
        // not needed
        return null;
    }

}
