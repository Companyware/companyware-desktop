package plugins.community.invoice.infrastructure;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import plugins.community.invoice.application.InvoiceDetailsDto;
import plugins.community.invoice.application.InvoiceLineDto;

/** Deterministic EN-16931 invoice XML writer for XRechnung UBL and ZUGFeRD CII. */
final class EInvoiceXmlGenerator {
    byte[] xrechnung(InvoiceDetailsDto invoice, boolean creditNote, String precedingNumber) {
        validate(invoice);
        String root = creditNote ? "CreditNote" : "Invoice";
        String quantity = creditNote ? "CreditedQuantity" : "InvoicedQuantity";
        String line = creditNote ? "CreditNoteLine" : "InvoiceLine";
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .append('<').append(root).append(" xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:")
                .append(root).append("-2\" xmlns:cac=\"urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2\" xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\">")
                .append(tag("cbc:CustomizationID", "urn:cen.eu:en16931:2017#compliant#urn:xeinkauf.de:kosit:xrechnung_3.0"))
                .append(tag("cbc:ProfileID", "urn:fdc:peppol.eu:2017:poacc:billing:01:1.0"))
                .append(tag("cbc:ID", invoice.summary().invoiceNumber()))
                .append(tag("cbc:IssueDate", invoice.summary().issueDate().toString()))
                .append(tag("cbc:" + (creditNote ? "CreditNoteTypeCode" : "InvoiceTypeCode"), creditNote ? "381" : "380"))
                .append(tag("cbc:DocumentCurrencyCode", invoice.currency()))
                .append(tag("cbc:BuyerReference", "N/A"));
        if (precedingNumber != null) xml.append("<cac:BillingReference><cac:InvoiceDocumentReference>")
                .append(tag("cbc:ID", precedingNumber)).append("</cac:InvoiceDocumentReference></cac:BillingReference>");
        xml.append(party("cac:AccountingSupplierParty", invoice.sellerName(), invoice.sellerAddress(), invoice.sellerTaxId()))
                .append(party("cac:AccountingCustomerParty", invoice.summary().customerName(), invoice.customerAddress(), null))
                .append("<cac:Delivery><cbc:ActualDeliveryDate>").append(invoice.serviceDate()).append("</cbc:ActualDeliveryDate></cac:Delivery>")
                .append("<cac:PaymentTerms>").append(tag("cbc:Note", invoice.paymentTerms())).append("</cac:PaymentTerms>")
                .append("<cac:TaxTotal><cbc:TaxAmount currencyID=\"").append(e(invoice.currency())).append("\">")
                .append(m(invoice.summary().tax())).append("</cbc:TaxAmount></cac:TaxTotal>")
                .append("<cac:LegalMonetaryTotal><cbc:LineExtensionAmount currencyID=\"").append(e(invoice.currency())).append("\">")
                .append(m(invoice.summary().net())).append("</cbc:LineExtensionAmount><cbc:TaxExclusiveAmount currencyID=\"")
                .append(e(invoice.currency())).append("\">").append(m(invoice.summary().net()))
                .append("</cbc:TaxExclusiveAmount><cbc:TaxInclusiveAmount currencyID=\"").append(e(invoice.currency()))
                .append("\">").append(m(invoice.summary().gross())).append("</cbc:TaxInclusiveAmount><cbc:PayableAmount currencyID=\"")
                .append(e(invoice.currency())).append("\">").append(m(invoice.summary().gross())).append("</cbc:PayableAmount></cac:LegalMonetaryTotal>");
        int number = 0;
        for (InvoiceLineDto value : invoice.lines()) xml.append("<cac:").append(line).append('>')
                .append(tag("cbc:ID", String.valueOf(++number))).append("<cbc:").append(quantity).append(" unitCode=\"C62\">")
                .append(d(value.quantity())).append("</cbc:").append(quantity).append("><cbc:LineExtensionAmount currencyID=\"")
                .append(e(invoice.currency())).append("\">").append(m(value.net())).append("</cbc:LineExtensionAmount><cac:Item>")
                .append(tag("cbc:Name", value.description())).append("</cac:Item><cac:Price><cbc:PriceAmount currencyID=\"")
                .append(e(invoice.currency())).append("\">").append(d(value.unitNet())).append("</cbc:PriceAmount></cac:Price></cac:")
                .append(line).append('>');
        return xml.append("</").append(root).append('>').toString().getBytes(StandardCharsets.UTF_8);
    }

    byte[] zugferd(InvoiceDetailsDto invoice, boolean creditNote, String precedingNumber) {
        validate(invoice);
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .append("<rsm:CrossIndustryInvoice xmlns:rsm=\"urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100\" xmlns:ram=\"urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100\" xmlns:udt=\"urn:un:unece:uncefact:data:standard:UnqualifiedDataType:100\">")
                .append("<rsm:ExchangedDocumentContext><ram:GuidelineSpecifiedDocumentContextParameter><ram:ID>urn:cen.eu:en16931:2017</ram:ID></ram:GuidelineSpecifiedDocumentContextParameter></rsm:ExchangedDocumentContext>")
                .append("<rsm:ExchangedDocument>").append(tag("ram:ID", invoice.summary().invoiceNumber()))
                .append(tag("ram:TypeCode", creditNote ? "381" : "380"))
                .append("<ram:IssueDateTime><udt:DateTimeString format=\"102\">")
                .append(invoice.summary().issueDate().format(DateTimeFormatter.BASIC_ISO_DATE)).append("</udt:DateTimeString></ram:IssueDateTime></rsm:ExchangedDocument>")
                .append("<rsm:SupplyChainTradeTransaction>");
        int number = 0;
        for (InvoiceLineDto value : invoice.lines()) xml.append("<ram:IncludedSupplyChainTradeLineItem><ram:AssociatedDocumentLineDocument>")
                .append(tag("ram:LineID", String.valueOf(++number))).append("</ram:AssociatedDocumentLineDocument><ram:SpecifiedTradeProduct>")
                .append(tag("ram:Name", value.description())).append("</ram:SpecifiedTradeProduct><ram:SpecifiedLineTradeAgreement><ram:NetPriceProductTradePrice>")
                .append(tag("ram:ChargeAmount", d(value.unitNet()))).append("</ram:NetPriceProductTradePrice></ram:SpecifiedLineTradeAgreement><ram:SpecifiedLineTradeDelivery><ram:BilledQuantity unitCode=\"C62\">")
                .append(d(value.quantity())).append("</ram:BilledQuantity></ram:SpecifiedLineTradeDelivery><ram:SpecifiedLineTradeSettlement><ram:ApplicableTradeTax><ram:TypeCode>VAT</ram:TypeCode><ram:CategoryCode>S</ram:CategoryCode>")
                .append(tag("ram:RateApplicablePercent", d(value.taxRate()))).append("</ram:ApplicableTradeTax><ram:SpecifiedTradeSettlementLineMonetarySummation>")
                .append(tag("ram:LineTotalAmount", m(value.net()))).append("</ram:SpecifiedTradeSettlementLineMonetarySummation></ram:SpecifiedLineTradeSettlement></ram:IncludedSupplyChainTradeLineItem>");
        xml.append("<ram:ApplicableHeaderTradeAgreement><ram:SellerTradeParty>").append(tag("ram:Name", invoice.sellerName()))
                .append("</ram:SellerTradeParty><ram:BuyerTradeParty>").append(tag("ram:Name", invoice.summary().customerName()))
                .append("</ram:BuyerTradeParty></ram:ApplicableHeaderTradeAgreement><ram:ApplicableHeaderTradeDelivery><ram:ActualDeliverySupplyChainEvent><ram:OccurrenceDateTime><udt:DateTimeString format=\"102\">")
                .append(invoice.serviceDate().format(DateTimeFormatter.BASIC_ISO_DATE)).append("</udt:DateTimeString></ram:OccurrenceDateTime></ram:ActualDeliverySupplyChainEvent></ram:ApplicableHeaderTradeDelivery>")
                .append("<ram:ApplicableHeaderTradeSettlement>").append(tag("ram:InvoiceCurrencyCode", invoice.currency()));
        if (precedingNumber != null) xml.append("<ram:InvoiceReferencedDocument>").append(tag("ram:IssuerAssignedID", precedingNumber)).append("</ram:InvoiceReferencedDocument>");
        xml.append("<ram:SpecifiedTradeSettlementHeaderMonetarySummation>").append(tag("ram:LineTotalAmount", m(invoice.summary().net())))
                .append(tag("ram:TaxBasisTotalAmount", m(invoice.summary().net()))).append("<ram:TaxTotalAmount currencyID=\"")
                .append(e(invoice.currency())).append("\">").append(m(invoice.summary().tax())).append("</ram:TaxTotalAmount>")
                .append(tag("ram:GrandTotalAmount", m(invoice.summary().gross()))).append(tag("ram:DuePayableAmount", m(invoice.summary().gross())))
                .append("</ram:SpecifiedTradeSettlementHeaderMonetarySummation></ram:ApplicableHeaderTradeSettlement></rsm:SupplyChainTradeTransaction></rsm:CrossIndustryInvoice>");
        return xml.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String party(String element, String name, String address, String taxId) {
        String[] lines = address == null ? new String[0] : address.split("\\r?\\n");
        String street = lines.length == 0 ? "" : lines[0];
        String cityLine = lines.length == 0 ? "" : lines[lines.length - 1];
        java.util.regex.Matcher match = java.util.regex.Pattern.compile("^(\\d{5})\\s+(.+)$").matcher(cityLine);
        String zip = match.matches() ? match.group(1) : "00000"; String city = match.matches() ? match.group(2) : cityLine;
        StringBuilder xml = new StringBuilder("<").append(element).append("><cac:Party><cac:PostalAddress>")
                .append(tag("cbc:StreetName", street)).append(tag("cbc:CityName", city)).append(tag("cbc:PostalZone", zip))
                .append("<cac:Country><cbc:IdentificationCode>DE</cbc:IdentificationCode></cac:Country></cac:PostalAddress>")
                .append("<cac:PartyLegalEntity>").append(tag("cbc:RegistrationName", name)).append("</cac:PartyLegalEntity>");
        if (taxId != null && !taxId.trim().isEmpty()) xml.append("<cac:PartyTaxScheme>").append(tag("cbc:CompanyID", taxId))
                .append("<cac:TaxScheme><cbc:ID>VAT</cbc:ID></cac:TaxScheme></cac:PartyTaxScheme>");
        return xml.append("</cac:Party></").append(element).append('>').toString();
    }
    private static void validate(InvoiceDetailsDto value) {
        if (value == null || value.summary().invoiceNumber() == null || value.summary().issueDate() == null)
            throw new IllegalArgumentException("finalized invoice data is required");
        if (value.lines().isEmpty()) throw new IllegalArgumentException("invoice lines are required");
    }
    private static String tag(String name, String value) { return "<" + name + ">" + e(value) + "</" + name + ">"; }
    private static String m(java.math.BigDecimal value) { return value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(); }
    private static String d(java.math.BigDecimal value) { return value.stripTrailingZeros().toPlainString(); }
    private static String e(String value) { return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;"); }
}
