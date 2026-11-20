package plugins.community.invoice.view;

import java.awt.FlowLayout;
import java.time.LocalDate;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JFileChooser;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import platform.api.application.ApplicationResult;
import platform.api.i18n.PluginMessages;
import platform.api.ui.StandardView;
import plugins.community.invoice.application.InvoiceDetailsDto;
import plugins.community.invoice.application.InvoiceDocumentService;
import plugins.community.invoice.application.InvoiceDto;
import plugins.community.invoice.application.InvoicePdfDto;
import plugins.community.invoice.application.InvoiceService;
import plugins.community.invoice.application.EInvoiceArchiveDto;
import plugins.community.invoice.application.EInvoiceService;
import plugins.community.invoice.application.InvoiceMailService;
import plugins.community.invoice.application.InvoiceMailDraftDto;
import javax.swing.JCheckBox;
import javax.swing.JTextArea;

/** Localized table workflow for invoices and finalization. */
final class InvoiceOverviewPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final InvoiceService invoices;
    private final InvoiceDocumentService documents;
    private final EInvoiceService eInvoices;
    private final InvoiceMailService mail;
    private final PluginMessages messages;
    private final Consumer<InvoiceDetailsDto> editor;
    private final LongConsumer preview;
    private final List<InvoiceDto> rows = new ArrayList<InvoiceDto>();
    private final DefaultTableModel model;
    private final JTable table;
    private final JTextField filter = new JTextField(18);

    InvoiceOverviewPanel(InvoiceService invoices, InvoiceDocumentService documents, PluginMessages messages,
            Consumer<InvoiceDetailsDto> editor, LongConsumer preview, boolean searchVisible) {
        this(invoices, documents, null, null, messages, editor, preview, searchVisible);
    }

    InvoiceOverviewPanel(InvoiceService invoices, InvoiceDocumentService documents, EInvoiceService eInvoices,
            PluginMessages messages, Consumer<InvoiceDetailsDto> editor, LongConsumer preview, boolean searchVisible) {
        this(invoices, documents, eInvoices, null, messages, editor, preview, searchVisible);
    }

    InvoiceOverviewPanel(InvoiceService invoices, InvoiceDocumentService documents, EInvoiceService eInvoices,
            InvoiceMailService mail, PluginMessages messages, Consumer<InvoiceDetailsDto> editor, LongConsumer preview, boolean searchVisible) {
        super(new java.awt.BorderLayout());
        this.invoices = invoices;
        this.documents = documents;
        this.eInvoices = eInvoices;
        this.mail = mail;
        this.messages = messages;
        this.editor = editor;
        this.preview = preview;
        model = new DefaultTableModel(new Object[] { messages.text("invoice.number"),
                messages.text("invoice.customer"), messages.text("invoice.issueDate"),
                messages.text("invoice.net"), messages.text("invoice.tax"),
                messages.text("invoice.gross"), messages.text("invoice.status") }, 0) {
            private static final long serialVersionUID = 1L;
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(28);
        filter.getDocument().addDocumentListener(new SimpleDocumentListener(this::applyFilter));
        add(StandardView.create(messages.text(searchVisible ? "invoice.searchTitle" : "invoice.overview"),
                new JScrollPane(table), actions(searchVisible)),
                java.awt.BorderLayout.CENTER);
        refresh();
        if (searchVisible) javax.swing.SwingUtilities.invokeLater(() -> filter.requestFocusInWindow());
    }

    private JPanel actions(boolean searchVisible) {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        if (searchVisible) {
            actions.add(new JLabel(messages.text("invoice.search")));
            actions.add(filter);
        }
        JButton edit = new JButton(messages.text("invoice.edit"));
        edit.addActionListener(event -> editSelected());
        actions.add(edit);
        JButton finalize = new JButton(messages.text("invoice.finalize"));
        finalize.addActionListener(event -> finalizeSelected());
        actions.add(finalize);
        JButton previewButton = new JButton(messages.text("invoice.preview"));
        previewButton.addActionListener(event -> previewSelected());
        actions.add(previewButton);
        JButton pdf = new JButton(messages.text("invoice.pdf"));
        pdf.addActionListener(event -> exportSelected());
        actions.add(pdf);
        if (eInvoices != null) {
            JButton eInvoice = new JButton(messages.text("invoice.einvoice"));
            eInvoice.addActionListener(event -> exportEInvoice());
            actions.add(eInvoice);
            JButton correction = new JButton(messages.text("invoice.correction"));
            correction.addActionListener(event -> createCorrection());
            actions.add(correction);
        }
        if (mail != null) { JButton send = new JButton(messages.text("invoice.mailSend")); send.addActionListener(event -> sendSelected()); actions.add(send); }
        return actions;
    }

    private void refresh() {
        ApplicationResult<List<InvoiceDto>> result = invoices.list();
        if (!result.isSuccess()) { error(result.message()); return; }
        rows.clear();
        rows.addAll(result.valueOrThrow());
        model.setRowCount(0);
        for (InvoiceDto invoice : rows) model.addRow(new Object[] {
                invoice.invoiceNumber() == null ? messages.text("invoice.draft") : invoice.invoiceNumber(),
                invoice.customerName(), invoice.issueDate() == null ? "" : invoice.issueDate(),
                invoice.net(), invoice.tax(), invoice.gross(), status(invoice.status()) });
    }

    private void applyFilter() {
        @SuppressWarnings("unchecked")
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) table.getRowSorter();
        String value = filter.getText().trim();
        sorter.setRowFilter(value.isEmpty() ? null : RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(value)));
    }

    private InvoiceDto selected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) { error(messages.text("invoice.select")); return null; }
        return rows.get(table.convertRowIndexToModel(viewRow));
    }

    private void editSelected() {
        InvoiceDto selected = selected();
        if (selected == null) return;
        if (!"DRAFT".equals(selected.status())) { error(messages.text("invoice.readonly")); return; }
        ApplicationResult<InvoiceDetailsDto> result = invoices.details(selected.id());
        if (result.isSuccess()) editor.accept(result.valueOrThrow()); else error(result.message());
    }

    private void finalizeSelected() {
        InvoiceDto selected = selected();
        if (selected == null) return;
        if (!"DRAFT".equals(selected.status())) { error(messages.text("invoice.alreadyFinal")); return; }
        JTextField series = new JTextField("RE", 10);
        JTextField date = new JTextField(LocalDate.now().toString(), 10);
        JPanel fields = new JPanel(new java.awt.GridLayout(2, 2, 8, 8));
        fields.add(new JLabel(messages.text("invoice.series"))); fields.add(series);
        fields.add(new JLabel(messages.text("invoice.issueDate"))); fields.add(date);
        int choice = JOptionPane.showConfirmDialog(this, fields, messages.text("invoice.finalizeConfirm"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;
        try {
            ApplicationResult<InvoiceDto> result = invoices.finalizeDraft(selected.id(), series.getText(),
                    LocalDate.parse(date.getText().trim()));
            if (!result.isSuccess()) { error(result.message()); return; }
            refresh();
            JOptionPane.showMessageDialog(this,
                    messages.text("invoice.finalized") + " " + result.valueOrThrow().invoiceNumber());
        } catch (RuntimeException invalid) { error(messages.text("invoice.invalidDate")); }
    }

    private void previewSelected() {
        InvoiceDto selected = selected();
        if (selected != null) preview.accept(selected.id());
    }

    private void exportSelected() {
        InvoiceDto selected = selected();
        if (selected == null) return;
        ApplicationResult<InvoicePdfDto> result = documents.createPdf(selected.id());
        if (!result.isSuccess()) { error(result.message()); return; }
        InvoicePdfDto pdf = result.valueOrThrow();
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File(pdf.filename()));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            java.nio.file.Path target = chooser.getSelectedFile().toPath();
            if (Files.exists(target) && JOptionPane.showConfirmDialog(this,
                    messages.text("invoice.pdfOverwrite"), messages.text("invoice.pdf"),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) return;
            Files.write(target, pdf.content());
            JOptionPane.showMessageDialog(this, messages.text("invoice.pdfSaved"));
        } catch (java.io.IOException failure) { error(messages.text("invoice.pdfSaveFailed")); }
    }

    private void exportEInvoice() {
        InvoiceDto selected = selected(); if (selected == null) return;
        ApplicationResult<EInvoiceArchiveDto> result = eInvoices.archive(selected.id());
        if (!result.isSuccess()) { error(result.message()); return; }
        EInvoiceArchiveDto artifact = result.valueOrThrow();
        JFileChooser chooser = new JFileChooser(); chooser.setSelectedFile(new java.io.File(artifact.filename()));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            java.nio.file.Path pdf = chooser.getSelectedFile().toPath();
            String base = pdf.getFileName().toString().replaceFirst("(?i)\\.pdf$", "");
            java.nio.file.Path xml = pdf.resolveSibling(base + "-xrechnung.xml");
            if ((Files.exists(pdf) || Files.exists(xml)) && JOptionPane.showConfirmDialog(this,
                    messages.text("invoice.pdfOverwrite"), messages.text("invoice.einvoice"),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) return;
            Files.write(pdf, artifact.pdf()); Files.write(xml, artifact.xml());
            JOptionPane.showMessageDialog(this, messages.text("invoice.einvoiceSaved") + "\nSHA-256: " + artifact.sha256());
        } catch (java.io.IOException failure) { error(messages.text("invoice.pdfSaveFailed")); }
    }

    private void createCorrection() {
        InvoiceDto selected = selected(); if (selected == null) return;
        String reason = JOptionPane.showInputDialog(this, messages.text("invoice.correctionReason"),
                messages.text("invoice.correction"), JOptionPane.WARNING_MESSAGE);
        if (reason == null) return;
        ApplicationResult<InvoiceDetailsDto> result = eInvoices.createCorrectionDraft(selected.id(), reason);
        if (!result.isSuccess()) { error(result.message()); return; }
        refresh(); editor.accept(result.valueOrThrow());
    }

    private void sendSelected() {
        InvoiceDto selected = selected(); if (selected == null) return;
        ApplicationResult<InvoiceMailDraftDto> result = mail.draft(selected.id()); if (!result.isSuccess()) { error(result.message()); return; }
        InvoiceMailDraftDto draft = result.valueOrThrow(); JTextField recipient = new JTextField(draft.recipient()); JTextField subject = new JTextField(draft.subject()); JTextArea body = new JTextArea(draft.body(), 12, 52); body.setLineWrap(true); body.setWrapStyleWord(true); JCheckBox xml = new JCheckBox(messages.text("invoice.mailAttachXml"), true);
        JPanel fields = new JPanel(new java.awt.BorderLayout(0, 8)); JPanel top = new JPanel(new java.awt.GridLayout(4, 1, 0, 4)); top.add(new JLabel(messages.text("invoice.mailRecipient"))); top.add(recipient); top.add(new JLabel(messages.text("invoice.mailSubject"))); top.add(subject); fields.add(top, java.awt.BorderLayout.NORTH); fields.add(new JScrollPane(body), java.awt.BorderLayout.CENTER); fields.add(xml, java.awt.BorderLayout.SOUTH);
        if (JOptionPane.showConfirmDialog(this, fields, messages.text("invoice.mailReview"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        ApplicationResult<Void> sent = mail.send(new InvoiceMailDraftDto(selected.id(), recipient.getText(), subject.getText(), body.getText()), xml.isSelected());
        if (sent.isSuccess()) JOptionPane.showMessageDialog(this, messages.text("invoice.mailSent")); else error(sent.message());
    }

    private String status(String value) {
        return messages.text("invoice.status." + value.toLowerCase(java.util.Locale.ROOT));
    }
    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, messages.text("invoice.error"), JOptionPane.ERROR_MESSAGE);
    }
}
