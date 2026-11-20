package plugins.community.invoice.infrastructure;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;

/** Adds EN-16931 XML and the PDF/A-3 identification required by a ZUGFeRD document. */
final class PdfA3InvoiceEmbedder {
    byte[] embed(byte[] sourcePdf, byte[] zugferdXml) throws Exception {
        try (PDDocument document = PDDocument.load(sourcePdf); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            PDComplexFileSpecification specification = new PDComplexFileSpecification();
            specification.setFile("factur-x.xml"); specification.setFileUnicode("factur-x.xml");
            specification.getCOSObject().setName("AFRelationship", "Alternative");
            PDEmbeddedFile embedded = new PDEmbeddedFile(document, new ByteArrayInputStream(zugferdXml));
            embedded.setSubtype("text/xml"); embedded.setSize(zugferdXml.length);
            specification.setEmbeddedFile(embedded); specification.setEmbeddedFileUnicode(embedded);
            PDEmbeddedFilesNameTreeNode tree = new PDEmbeddedFilesNameTreeNode();
            tree.setNames(Collections.singletonMap("factur-x.xml", specification));
            PDDocumentNameDictionary names = new PDDocumentNameDictionary(catalog); names.setEmbeddedFiles(tree);
            catalog.setNames(names);
            COSArray associated = new COSArray(); associated.add(specification.getCOSObject());
            catalog.getCOSObject().setItem(COSName.getPDFName("AF"), associated);
            PDMetadata metadata = new PDMetadata(document);
            metadata.importXMPMetadata(xmp().getBytes(StandardCharsets.UTF_8)); catalog.setMetadata(metadata);
            PDOutputIntent intent = new PDOutputIntent(document,
                    new ByteArrayInputStream(ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData()));
            intent.setInfo("sRGB IEC61966-2.1"); intent.setOutputCondition("sRGB IEC61966-2.1");
            intent.setOutputConditionIdentifier("sRGB IEC61966-2.1"); intent.setRegistryName("http://www.color.org");
            catalog.addOutputIntent(intent);
            document.setVersion(1.7f); document.save(output); return output.toByteArray();
        }
    }

    private static String xmp() {
        return "<?xpacket begin=\"\uFEFF\" id=\"W5M0MpCehiHzreSzNTczkc9d\"?><x:xmpmeta xmlns:x=\"adobe:ns:meta/\"><rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><rdf:Description rdf:about=\"\" xmlns:pdfaid=\"http://www.aiim.org/pdfa/ns/id/\" pdfaid:part=\"3\" pdfaid:conformance=\"B\"/><rdf:Description rdf:about=\"\" xmlns:fx=\"urn:factur-x:pdfa:CrossIndustryDocument:invoice:1p0#\" fx:DocumentType=\"INVOICE\" fx:DocumentFileName=\"factur-x.xml\" fx:Version=\"1.0\" fx:ConformanceLevel=\"EN 16931\"/></rdf:RDF></x:xmpmeta><?xpacket end=\"w\"?>";
    }
}
