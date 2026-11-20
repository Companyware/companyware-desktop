package plugins.community.invoice.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import platform.api.application.ApplicationResult;
import platform.api.i18n.PluginMessages;
import platform.api.ui.StandardView;
import plugins.community.invoice.application.InvoiceDocumentService;
import plugins.community.invoice.application.InvoicePdfDto;

/** Read-only in-app preview rendered from the actual generated PDF bytes. */
final class InvoicePreviewPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    InvoicePreviewPanel(InvoiceDocumentService documents, PluginMessages messages,
            long invoiceId, Runnable close) {
        super(new BorderLayout());
        PdfPagePreview pages = new PdfPagePreview();
        ApplicationResult<InvoicePdfDto> result = documents.previewPdf(invoiceId);
        if (result.isSuccess()) {
            try { pages.setPdf(result.valueOrThrow().content()); }
            catch (IOException failure) { showError(messages.text("invoice.pdfPreviewFailed"), messages); }
        } else showError(result.message(), messages);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton back = new JButton(messages.text("invoice.back"));
        back.addActionListener(event -> close.run());
        actions.add(back);
        JScrollPane scroll = new JScrollPane(pages);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        add(StandardView.create(messages.text("invoice.previewTitle"), scroll, actions),
                BorderLayout.CENTER);
    }

    private void showError(String message, PluginMessages messages) {
        JOptionPane.showMessageDialog(this, message, messages.text("invoice.error"),
                JOptionPane.ERROR_MESSAGE);
    }
}
