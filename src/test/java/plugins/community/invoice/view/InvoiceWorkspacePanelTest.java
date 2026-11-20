package plugins.community.invoice.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.Test;
import platform.api.application.ApplicationResult;
import platform.api.i18n.PluginMessages;
import plugins.community.invoice.application.CreateInvoiceCommand;
import plugins.community.invoice.application.InvoiceDetailsDto;
import plugins.community.invoice.application.InvoiceDocumentService;
import plugins.community.invoice.application.InvoiceDto;
import plugins.community.invoice.application.InvoicePdfDto;
import plugins.community.invoice.application.InvoiceService;
import plugins.community.invoice.application.InvoiceTemplateDto;
import plugins.community.invoice.application.InvoiceBrandingDto;
import plugins.community.invoice.application.InvoiceMailDraftDto;
import plugins.community.invoice.application.InvoiceMailService;
import plugins.community.invoice.application.InvoiceMailTemplateDto;
import plugins.community.invoice.application.SmtpSettingsDto;

public class InvoiceWorkspacePanelTest {
    @Test public void invoicePreviewRendersTheGeneratedPdfInsideTheWorkspace() throws Exception {
        AtomicReference<InvoicePreviewPanel> preview = new AtomicReference<InvoicePreviewPanel>();
        SwingUtilities.invokeAndWait(() -> preview.set(new InvoicePreviewPanel(
                new StubDocumentService(), new KeyMessages(), 1L, () -> { })));

        assertTrue(findAll(preview.get(), JLabel.class).stream()
                .anyMatch(label -> label.getIcon() instanceof ImageIcon));
        assertTrue(findButton(preview.get(), "invoice.back").getParent() != null);
    }

    @Test
    public void showsInvoicesAndOpensTheDraftEditor() throws Exception {
        AtomicReference<InvoiceWorkspacePanel> workspace = new AtomicReference<InvoiceWorkspacePanel>();
        SwingUtilities.invokeAndWait(() -> workspace.set(new InvoiceWorkspacePanel(new StubInvoiceService(),
                new StubDocumentService(), new KeyMessages())));

        SwingUtilities.invokeAndWait(() -> {
            JTable table = find(workspace.get(), JTable.class);
            assertEquals(1, table.getRowCount());
            assertEquals("RE-2026-0001", table.getValueAt(0, 0));
            assertEquals("invoice.status.finalized", table.getValueAt(0, 6));
            assertTrue(findButton(workspace.get(), "invoice.preview").getParent() != null);
            assertTrue(findButton(workspace.get(), "invoice.pdf").getParent() != null);
            assertEquals(null, findOptionalButton(workspace.get(), "invoice.new"));
            assertEquals(null, findOptionalButton(workspace.get(), "invoice.template"));
        });
    }

    @Test public void opensDedicatedCreateTemplateAndSearchViews() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            InvoiceWorkspacePanel create = new InvoiceWorkspacePanel(new StubInvoiceService(),
                    new StubDocumentService(), new KeyMessages(), InvoiceWorkspacePanel.InitialView.CREATE);
            assertTrue(findButton(create, "invoice.saveDraft").getParent() != null);
            assertTrue(findAll(create, javax.swing.JTextField.class).stream()
                    .anyMatch(field -> "Default Seller GmbH".equals(field.getText())));
            InvoiceWorkspacePanel template = new InvoiceWorkspacePanel(new StubInvoiceService(),
                    new StubDocumentService(), new KeyMessages(), InvoiceWorkspacePanel.InitialView.TEMPLATE);
            assertTrue(findButton(template, "invoice.templateSave").getParent() != null);
            assertTrue(findButton(template, "invoice.templateRefreshPdfPreview").getParent() != null);
            assertEquals(null, findOptionalButton(template, "invoice.templatePdfPreview"));
            assertTrue(findAll(template, JLabel.class).stream()
                    .anyMatch(label -> label.getIcon() instanceof ImageIcon));
            findButton(template, "invoice.templateSave").doClick();
            assertTrue(findButton(template, "invoice.templateSave").getParent() != null);
            InvoiceWorkspacePanel search = new InvoiceWorkspacePanel(new StubInvoiceService(),
                    new StubDocumentService(), new KeyMessages(), InvoiceWorkspacePanel.InitialView.SEARCH);
            assertTrue(find(search, javax.swing.JTextField.class).getParent() != null);
            InvoiceWorkspacePanel branding = new InvoiceWorkspacePanel(new StubInvoiceService(),
                    new StubDocumentService(), new KeyMessages(), InvoiceWorkspacePanel.InitialView.BRANDING);
            assertTrue(findButton(branding, "invoice.logo.restoreDefault").getParent() != null);
            assertTrue(findButton(branding, "invoice.logo.choose").getParent() != null);
            assertTrue(findButton(branding, "invoice.letterheadSave").getParent() != null);
            assertTrue(find(branding, javax.swing.JEditorPane.class).getText().contains("letterhead"));
        });
    }

    @Test public void opensSmtpSettingsWithSaveAndConnectionTest() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            InvoiceWorkspacePanel smtp = new InvoiceWorkspacePanel(new StubInvoiceService(),
                    new StubDocumentService(), null, new StubMailService(), new KeyMessages(),
                    InvoiceWorkspacePanel.InitialView.SMTP_SETTINGS);
            assertTrue(findButton(smtp, "invoice.smtpSave").getParent() != null);
            assertTrue(findButton(smtp, "invoice.smtpTest").getParent() != null);
            assertEquals("smtp.example.org", findAll(smtp, javax.swing.JTextField.class).get(0).getText());
        });
    }

    private static final class StubDocumentService implements InvoiceDocumentService {
        @Override public ApplicationResult<InvoiceTemplateDto> template() {
            return ApplicationResult.success(new InvoiceTemplateDto("<html><body>{{lines}}</body></html>", false, null));
        }
        @Override public ApplicationResult<InvoiceTemplateDto> saveTemplate(String source) { return template(); }
        @Override public ApplicationResult<InvoiceTemplateDto> restoreDefaultTemplate() { return template(); }
        @Override public ApplicationResult<InvoiceBrandingDto> branding() {
            return ApplicationResult.success(new InvoiceBrandingDto(null, null, 40, "LEFT",
                    "Default Seller GmbH", "Seller Street", "TAX", "Footer"));
        }
        @Override public ApplicationResult<InvoiceBrandingDto> saveLogo(byte[] content, String mediaType,
                int widthMillimeters, String alignment) {
            return ApplicationResult.success(new InvoiceBrandingDto(content, mediaType, widthMillimeters, alignment));
        }
        @Override public ApplicationResult<InvoiceBrandingDto> saveBranding(InvoiceBrandingDto branding) {
            return ApplicationResult.success(branding);
        }
        @Override public ApplicationResult<InvoiceBrandingDto> restoreDefaultLogo() { return branding(); }
        @Override public ApplicationResult<InvoiceBrandingDto> removeLogo() { return branding(); }
        @Override public ApplicationResult<String> previewBrandingHtml(InvoiceBrandingDto branding) {
            return ApplicationResult.success("<html><body>letterhead</body></html>");
        }
        @Override public ApplicationResult<String> previewTemplateHtml(String source) {
            return ApplicationResult.success("<html><body>sample</body></html>");
        }
        @Override public ApplicationResult<InvoicePdfDto> previewTemplatePdf(String source) {
            return ApplicationResult.success(new InvoicePdfDto("preview.pdf", testPdf()));
        }
        @Override public ApplicationResult<String> previewHtml(long invoiceId) {
            return ApplicationResult.success("<html><body>preview</body></html>");
        }
        @Override public ApplicationResult<InvoicePdfDto> previewPdf(long invoiceId) {
            return ApplicationResult.success(new InvoicePdfDto("invoice-preview.pdf", testPdf()));
        }
        @Override public ApplicationResult<InvoicePdfDto> createPdf(long invoiceId) {
            return ApplicationResult.success(new InvoicePdfDto("invoice.pdf", new byte[] { 1 }));
        }

        private byte[] testPdf() {
            try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                document.addPage(new PDPage());
                document.save(output);
                return output.toByteArray();
            } catch (java.io.IOException failure) {
                throw new IllegalStateException(failure);
            }
        }
    }

    private static JButton findButton(Container root, String text) {
        JButton button = findOptionalButton(root, text);
        if (button != null) return button;
        throw new AssertionError("Button not found: " + text);
    }

    private static JButton findOptionalButton(Container root, String text) {
        for (JButton button : findAll(root, JButton.class)) {
            if (text.equals(button.getText())) return button;
        }
        return null;
    }

    private static <T extends Component> T find(Container root, Class<T> type) {
        List<T> matches = findAll(root, type);
        if (matches.isEmpty()) throw new AssertionError(type.getSimpleName() + " not found");
        return matches.get(0);
    }

    private static <T extends Component> List<T> findAll(Container root, Class<T> type) {
        java.util.ArrayList<T> matches = new java.util.ArrayList<T>();
        for (Component component : root.getComponents()) {
            if (type.isInstance(component)) matches.add(type.cast(component));
            if (component instanceof Container) matches.addAll(findAll((Container) component, type));
        }
        return matches;
    }

    private static final class KeyMessages implements PluginMessages {
        @Override public Locale locale() { return Locale.ENGLISH; }
        @Override public String text(String key, Object... arguments) { return key; }
    }

    private static final class StubInvoiceService implements InvoiceService {
        private final InvoiceDto invoice = new InvoiceDto(1L, "FINALIZED", "RE-2026-0001", "Example GmbH",
                LocalDate.of(2026, 9, 12), new BigDecimal("100.00"), new BigDecimal("19.00"),
                new BigDecimal("119.00"));

        @Override public ApplicationResult<InvoiceDto> createDraft(CreateInvoiceCommand command) {
            return ApplicationResult.success(invoice);
        }
        @Override public ApplicationResult<InvoiceDto> updateDraft(long id, CreateInvoiceCommand command) {
            return ApplicationResult.success(invoice);
        }
        @Override public ApplicationResult<List<InvoiceDto>> list() {
            return ApplicationResult.success(Arrays.asList(invoice));
        }
        @Override public ApplicationResult<InvoiceDto> find(long id) {
            return ApplicationResult.success(invoice);
        }
        @Override public ApplicationResult<InvoiceDetailsDto> details(long id) {
            return ApplicationResult.failure(platform.api.application.ApplicationErrorCode.NOT_FOUND, "unused");
        }
        @Override public ApplicationResult<InvoiceDto> finalizeDraft(long id, String series, LocalDate issueDate) {
            return ApplicationResult.success(invoice);
        }
    }

    private static final class StubMailService implements InvoiceMailService {
        private final SmtpSettingsDto smtp = new SmtpSettingsDto("smtp.example.org", 587,
                "rechnung@example.org", "mailer", true, true);
        @Override public ApplicationResult<InvoiceMailTemplateDto> template() { return ApplicationResult.failure(platform.api.application.ApplicationErrorCode.NOT_FOUND, "unused"); }
        @Override public ApplicationResult<InvoiceMailTemplateDto> saveTemplate(InvoiceMailTemplateDto value) { return ApplicationResult.failure(platform.api.application.ApplicationErrorCode.NOT_FOUND, "unused"); }
        @Override public ApplicationResult<InvoiceMailTemplateDto> restoreDefaultTemplate() { return ApplicationResult.failure(platform.api.application.ApplicationErrorCode.NOT_FOUND, "unused"); }
        @Override public ApplicationResult<InvoiceMailDraftDto> draft(long invoiceId) { return ApplicationResult.failure(platform.api.application.ApplicationErrorCode.NOT_FOUND, "unused"); }
        @Override public ApplicationResult<Void> send(InvoiceMailDraftDto draft, boolean xml) { return ApplicationResult.failure(platform.api.application.ApplicationErrorCode.NOT_FOUND, "unused"); }
        @Override public ApplicationResult<SmtpSettingsDto> smtpSettings() { return ApplicationResult.success(smtp); }
        @Override public ApplicationResult<SmtpSettingsDto> saveSmtpSettings(SmtpSettingsDto value, char[] password) { return ApplicationResult.success(value); }
        @Override public ApplicationResult<Void> testSmtpSettings(SmtpSettingsDto value, char[] password) { return ApplicationResult.success(null); }
    }
}
