package plugins.community.invoice.infrastructure;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import plugins.community.invoice.application.InvoiceDetailsDto;
import plugins.community.invoice.application.InvoiceLineDto;

/** Validates controlled XHTML/CSS and substitutes escaped invoice snapshot values. */
public final class InvoiceTemplateEngine {
    private static final int MAX_LENGTH = 262144;
    private static final Pattern TOKEN = Pattern.compile("\\{\\{([A-Za-z][A-Za-z0-9.]*)\\}\\}");
    private static final Set<String> TOKENS = immutable("invoice.number", "invoice.issueDate",
            "invoice.serviceDate", "invoice.currency", "invoice.net", "invoice.tax", "invoice.gross",
            "invoice.paymentTerms", "seller.name", "seller.address", "seller.taxId", "customer.name",
            "customer.address", "company.logo", "company.footer", "lines");
    private static final Set<String> ELEMENTS = immutable("html", "head", "meta", "title", "style",
            "body", "div", "span", "p", "br", "strong", "em", "h1", "h2", "h3", "table",
            "thead", "tbody", "tfoot", "tr", "th", "td", "ul", "ol", "li");
    private static final Set<String> ATTRIBUTES = immutable("xmlns", "charset", "class", "style",
            "colspan", "rowspan");

    public void validate(String source) {
        if (source == null || source.trim().isEmpty()) throw new IllegalArgumentException("template is required");
        if (source.length() > MAX_LENGTH) throw new IllegalArgumentException("template is too large");
        String lower = source.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("<!doctype") || lower.contains("<!entity") || lower.contains("url(")
                || lower.contains("@import") || lower.contains("expression(") || lower.contains("javascript:")
                || lower.contains("data:"))
            throw new IllegalArgumentException("template contains external or executable content");
        Matcher tokens = TOKEN.matcher(source);
        while (tokens.find()) if (!TOKENS.contains(tokens.group(1)))
            throw new IllegalArgumentException("unsupported placeholder: " + tokens.group());
        if (!source.contains("{{lines}}")) throw new IllegalArgumentException("template must contain {{lines}}");
        Document document = parse(source);
        inspect(document.getDocumentElement());
    }

    public String render(String source, InvoiceDetailsDto invoice) {
        return render(source, invoice, "");
    }

    public String render(String source, InvoiceDetailsDto invoice, String companyLogoMarkup) {
        return render(source, invoice, companyLogoMarkup, "");
    }

    public String render(String source, InvoiceDetailsDto invoice, String companyLogoMarkup,
            String companyFooter) {
        validate(source);
        boolean hasFooterToken = source.contains("{{company.footer}}");
        String html = source;
        html = replace(html, "invoice.number", invoice.summary().invoiceNumber() == null
                ? "DRAFT-" + invoice.summary().id() : invoice.summary().invoiceNumber());
        html = replace(html, "invoice.issueDate", date(invoice.summary().issueDate()));
        html = replace(html, "invoice.serviceDate", date(invoice.serviceDate()));
        html = replace(html, "invoice.currency", invoice.currency());
        html = replace(html, "invoice.net", money(invoice.summary().net()));
        html = replace(html, "invoice.tax", money(invoice.summary().tax()));
        html = replace(html, "invoice.gross", money(invoice.summary().gross()));
        html = markup(html, "invoice.paymentTerms", multiline(invoice.paymentTerms()));
        html = replace(html, "seller.name", invoice.sellerName());
        html = markup(html, "seller.address", multiline(invoice.sellerAddress()));
        html = replace(html, "seller.taxId", invoice.sellerTaxId());
        html = replace(html, "customer.name", invoice.summary().customerName());
        html = markup(html, "customer.address", multiline(invoice.customerAddress()));
        html = markup(html, "company.logo", companyLogoMarkup == null ? "" : companyLogoMarkup);
        String footerMarkup = multiline(companyFooter);
        html = markup(html, "company.footer", footerMarkup);
        html = html.replace("{{lines}}", lines(invoice));
        if (!hasFooterToken && !footerMarkup.isEmpty())
            html = html.replace("</body>", "<div class=\"footer\">" + footerMarkup + "</div></body>");
        return html;
    }

    private static Document parse(String source) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            try { factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); } catch (IllegalArgumentException ignored) { }
            try { factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); } catch (IllegalArgumentException ignored) { }
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(source)));
        } catch (Exception invalid) {
            throw new IllegalArgumentException("template must be well-formed XHTML", invalid);
        }
    }

    private static void inspect(Node node) {
        if (node instanceof Element) {
            String name = node.getLocalName() == null ? node.getNodeName() : node.getLocalName();
            if (!ELEMENTS.contains(name.toLowerCase(java.util.Locale.ROOT)))
                throw new IllegalArgumentException("unsupported element: " + name);
            NamedNodeMap attributes = node.getAttributes();
            for (int index = 0; index < attributes.getLength(); index++) {
                Node attribute = attributes.item(index);
                String attributeName = attribute.getNodeName();
                if (!ATTRIBUTES.contains(attributeName.toLowerCase(java.util.Locale.ROOT)))
                    throw new IllegalArgumentException("unsupported attribute: " + attributeName);
            }
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) inspect(child);
    }

    private static String lines(InvoiceDetailsDto invoice) {
        StringBuilder html = new StringBuilder();
        for (InvoiceLineDto line : invoice.lines()) html.append("<tr><td>").append(escape(line.description()))
                .append("</td><td class=\"number\">").append(escape(decimal(line.quantity())))
                .append("</td><td class=\"number\">").append(escape(money(line.unitNet())))
                .append("</td><td class=\"number\">").append(escape(decimal(line.taxRate()))).append(" %")
                .append("</td><td class=\"number\">").append(escape(money(line.net())))
                .append("</td></tr>");
        return html.toString();
    }

    private static String replace(String source, String token, String value) {
        return source.replace("{{" + token + "}}", escape(value));
    }
    private static String markup(String source, String token, String value) {
        return source.replace("{{" + token + "}}", value);
    }
    private static String multiline(String value) { return escape(value).replace("\r\n", "<br />").replace("\n", "<br />"); }
    private static String date(LocalDate date) { return date == null ? "" : DateTimeFormatter.ofPattern("dd.MM.yyyy").format(date); }
    private static String money(BigDecimal value) { return value == null ? "" : value.setScale(2, RoundingMode.HALF_UP).toPlainString(); }
    private static String decimal(BigDecimal value) { return value == null ? "" : value.stripTrailingZeros().toPlainString(); }
    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
    @SafeVarargs private static <T> Set<T> immutable(T... values) {
        return Collections.unmodifiableSet(new HashSet<T>(Arrays.asList(values)));
    }
}
