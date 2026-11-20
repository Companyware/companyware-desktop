package plugins.community.invoice.view;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import platform.api.i18n.PluginMessages;
import plugins.community.invoice.application.InvoiceDetailsDto;
import plugins.community.invoice.application.InvoiceDocumentService;
import plugins.community.invoice.application.InvoiceService;
import plugins.community.invoice.application.EInvoiceService;
import plugins.community.invoice.application.InvoiceMailService;

/** Switches between the invoice overview and the draft editor. */
public final class InvoiceWorkspacePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final InvoiceService invoices;
    private final InvoiceDocumentService documents;
    private final EInvoiceService eInvoices;
    private final InvoiceMailService mail;
    private final PluginMessages messages;

    public enum InitialView { OVERVIEW, CREATE, TEMPLATE, BRANDING, SEARCH, MAIL_TEMPLATE, SMTP_SETTINGS }

    public InvoiceWorkspacePanel(InvoiceService invoices, InvoiceDocumentService documents, PluginMessages messages) {
        this(invoices, documents, null, null, messages, InitialView.OVERVIEW);
    }

    public InvoiceWorkspacePanel(InvoiceService invoices, InvoiceDocumentService documents, PluginMessages messages,
            InitialView initialView) {
        this(invoices, documents, null, null, messages, initialView);
    }

    public InvoiceWorkspacePanel(InvoiceService invoices, InvoiceDocumentService documents, EInvoiceService eInvoices,
            PluginMessages messages, InitialView initialView) {
        this(invoices, documents, eInvoices, null, messages, initialView);
    }

    public InvoiceWorkspacePanel(InvoiceService invoices, InvoiceDocumentService documents, EInvoiceService eInvoices,
            InvoiceMailService mail, PluginMessages messages, InitialView initialView) {
        super(new BorderLayout());
        this.invoices = invoices;
        this.documents = documents;
        this.eInvoices = eInvoices;
        this.mail = mail;
        this.messages = messages;
        if (initialView == InitialView.CREATE) showEditor(null);
        else if (initialView == InitialView.TEMPLATE) showTemplateEditor();
        else if (initialView == InitialView.BRANDING) showBranding();
        else if (initialView == InitialView.MAIL_TEMPLATE) showMailTemplate();
        else if (initialView == InitialView.SMTP_SETTINGS) showSmtpSettings();
        else showOverview(initialView == InitialView.SEARCH);
    }

    private void showOverview() {
        showOverview(false);
    }

    private void showOverview(boolean search) {
        replace(new InvoiceOverviewPanel(invoices, documents, eInvoices, mail, messages, this::showEditor,
                this::showPreview, search));
    }

    private void showEditor(InvoiceDetailsDto invoice) {
        replace(new InvoiceEditorPanel(invoices, documents, messages, invoice, this::showOverview));
    }

    private void showPreview(long invoiceId) {
        replace(new InvoicePreviewPanel(documents, messages, invoiceId, this::showOverview));
    }

    private void showTemplateEditor() {
        replace(new InvoiceTemplateEditorPanel(documents, messages, this::showOverview));
    }

    private void showBranding() {
        replace(new InvoiceBrandingPanel(documents, messages));
    }

    private void showMailTemplate() { replace(new InvoiceMailTemplatePanel(mail, messages)); }
    private void showSmtpSettings() { replace(new InvoiceSmtpSettingsPanel(mail, messages)); }

    private void replace(JPanel panel) {
        removeAll();
        add(panel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }
}
