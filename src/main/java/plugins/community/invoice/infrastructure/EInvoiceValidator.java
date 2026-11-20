package plugins.community.invoice.infrastructure;

import java.io.ByteArrayInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

/** Defensive XML and basic business-data validation before immutable archival. */
final class EInvoiceValidator {
    void validate(byte[] xml, String expectedNumber) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        String root = document.getDocumentElement().getLocalName();
        if (!("Invoice".equals(root) || "CreditNote".equals(root) || "CrossIndustryInvoice".equals(root)))
            throw new IllegalArgumentException("Unsupported e-invoice document");
        if (!new String(xml, java.nio.charset.StandardCharsets.UTF_8).contains(">" + expectedNumber + "<"))
            throw new IllegalArgumentException("Invoice number is missing from e-invoice XML");
    }
}
