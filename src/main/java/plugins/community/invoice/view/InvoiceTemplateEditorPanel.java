package plugins.community.invoice.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import platform.api.application.ApplicationResult;
import platform.api.i18n.PluginMessages;
import platform.api.ui.StandardView;
import plugins.community.invoice.application.InvoiceDocumentService;
import plugins.community.invoice.application.InvoiceTemplateDto;
import plugins.community.invoice.application.InvoicePdfDto;

/** Source editor for the controlled XHTML/CSS invoice template. */
final class InvoiceTemplateEditorPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final InvoiceDocumentService documents;
    private final PluginMessages messages;
    private final Runnable close;
    private final JTextArea source = new JTextArea();
    private final PdfPagePreview preview = new PdfPagePreview();
    private final JLabel previewStatus = new JLabel(" ");

    InvoiceTemplateEditorPanel(InvoiceDocumentService documents, PluginMessages messages, Runnable close) {
        super(new BorderLayout());
        this.documents = documents;
        this.messages = messages;
        this.close = close;
        source.setTabSize(2);
        source.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 13));
        JTextArea help = new JTextArea(messages.text("invoice.templateHelp"));
        help.setEditable(false);
        help.setRows(4);
        help.setLineWrap(true);
        help.setWrapStyleWord(true);
        JPanel sourcePanel = new JPanel(new BorderLayout(0, 6));
        sourcePanel.add(new JLabel(messages.text("invoice.templateSource")), BorderLayout.NORTH);
        sourcePanel.add(new JScrollPane(source), BorderLayout.CENTER);
        sourcePanel.add(new JScrollPane(help), BorderLayout.SOUTH);
        JPanel previewPanel = new JPanel(new BorderLayout(0, 6));
        previewPanel.add(new JLabel(messages.text("invoice.templatePreview")), BorderLayout.NORTH);
        JScrollPane previewScroll = new JScrollPane(preview);
        previewScroll.getVerticalScrollBar().setUnitIncrement(24);
        previewPanel.add(previewScroll, BorderLayout.CENTER);
        previewPanel.add(previewStatus, BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sourcePanel, previewPanel);
        split.setResizeWeight(0.5);
        split.setContinuousLayout(true);
        add(StandardView.create(messages.text("invoice.templateTitle"), split, actions()), BorderLayout.CENTER);
        load();
    }

    private JPanel actions() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton restore = new JButton(messages.text("invoice.templateRestore"));
        restore.addActionListener(event -> restore());
        JButton cancel = new JButton(messages.text("invoice.cancel"));
        cancel.addActionListener(event -> close.run());
        JButton save = new JButton(messages.text("invoice.templateSave"));
        save.addActionListener(event -> save());
        JButton refresh = new JButton(messages.text("invoice.templateRefreshPdfPreview"));
        refresh.addActionListener(event -> refreshPreview());
        actions.add(restore); actions.add(refresh); actions.add(cancel); actions.add(save);
        return actions;
    }

    private void load() {
        ApplicationResult<InvoiceTemplateDto> result = documents.template();
        if (result.isSuccess()) source.setText(result.valueOrThrow().source()); else error(result.message());
        source.setCaretPosition(0);
        refreshPreview();
    }

    private void save() {
        ApplicationResult<InvoiceTemplateDto> result = documents.saveTemplate(source.getText());
        if (!result.isSuccess()) { error(result.message()); return; }
        source.setText(result.valueOrThrow().source());
        source.setCaretPosition(0);
        refreshPreview();
        previewStatus.setText(messages.text("invoice.templateSaved"));
    }

    private void restore() {
        int choice = JOptionPane.showConfirmDialog(this, messages.text("invoice.templateRestoreConfirm"),
                messages.text("invoice.templateRestore"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;
        ApplicationResult<InvoiceTemplateDto> result = documents.restoreDefaultTemplate();
        if (!result.isSuccess()) { error(result.message()); return; }
        source.setText(result.valueOrThrow().source());
        source.setCaretPosition(0);
        refreshPreview();
    }

    private void refreshPreview() {
        ApplicationResult<InvoicePdfDto> result = documents.previewTemplatePdf(source.getText());
        if (!result.isSuccess()) {
            previewStatus.setText(messages.text("invoice.templatePreviewInvalid") + " " + result.message());
            return;
        }
        try {
            preview.setPdf(result.valueOrThrow().content());
            previewStatus.setText(messages.text("invoice.templatePreviewReady"));
        } catch (IOException failure) {
            previewStatus.setText(messages.text("invoice.templatePreviewInvalid") + " " + failure.getMessage());
        }
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, messages.text("invoice.error"), JOptionPane.ERROR_MESSAGE);
    }
}
