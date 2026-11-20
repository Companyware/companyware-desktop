package plugins.community.invoice.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import platform.api.application.ApplicationResult;
import platform.api.i18n.PluginMessages;
import platform.api.ui.StandardView;
import plugins.community.invoice.application.CreateInvoiceCommand;
import plugins.community.invoice.application.InvoiceDetailsDto;
import plugins.community.invoice.application.InvoiceDto;
import plugins.community.invoice.application.InvoiceLineDto;
import plugins.community.invoice.application.InvoiceLineInput;
import plugins.community.invoice.application.InvoiceService;
import plugins.community.invoice.application.InvoiceDocumentService;
import plugins.community.invoice.application.InvoiceBrandingDto;

/** Form adapter for creating and updating invoice drafts. */
final class InvoiceEditorPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final InvoiceService invoices;
    private final PluginMessages messages;
    private final InvoiceDetailsDto existing;
    private final Runnable close;
    private final JTextField sellerName = new JTextField(24);
    private final JTextArea sellerAddress = new JTextArea(3, 24);
    private final JTextField sellerTaxId = new JTextField(24);
    private final JTextField customerName = new JTextField(24);
    private final JTextArea customerAddress = new JTextArea(3, 24);
    private final JTextField serviceDate = new JTextField(LocalDate.now().toString(), 12);
    private final JTextField currency = new JTextField("EUR", 6);
    private final JTextArea paymentTerms = new JTextArea(3, 24);
    private final DefaultTableModel lineModel;

    InvoiceEditorPanel(InvoiceService invoices, InvoiceDocumentService documents, PluginMessages messages,
            InvoiceDetailsDto existing, Runnable close) {
        super(new BorderLayout());
        this.invoices = invoices;
        this.messages = messages;
        this.existing = existing;
        this.close = close;
        lineModel = new DefaultTableModel(new Object[] { messages.text("invoice.description"),
                messages.text("invoice.quantity"), messages.text("invoice.unitNet"),
                messages.text("invoice.taxRate") }, 0);
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.add(headerForm(), BorderLayout.NORTH);
        content.add(lines(), BorderLayout.CENTER);
        add(StandardView.create(existing == null ? messages.text("invoice.new") : messages.text("invoice.edit"),
                content, actions()), BorderLayout.CENTER);
        if (existing == null) { applyLetterheadDefaults(documents); addLine(); } else populate(existing);
    }

    private void applyLetterheadDefaults(InvoiceDocumentService documents) {
        ApplicationResult<InvoiceBrandingDto> result = documents.branding();
        if (!result.isSuccess()) return;
        InvoiceBrandingDto value = result.valueOrThrow();
        sellerName.setText(value.sellerName()); sellerAddress.setText(value.sellerAddress());
        sellerTaxId.setText(value.sellerTaxId());
    }

    private JPanel headerForm() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 6, 3, 6); c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        int row = 0;
        add(form, c, row++, "invoice.sellerName", sellerName, "invoice.customer", customerName);
        add(form, c, row++, "invoice.sellerAddress", new JScrollPane(sellerAddress),
                "invoice.customerAddress", new JScrollPane(customerAddress));
        add(form, c, row++, "invoice.sellerTaxId", sellerTaxId, "invoice.serviceDate", serviceDate);
        add(form, c, row++, "invoice.currency", currency, "invoice.paymentTerms", new JScrollPane(paymentTerms));
        return form;
    }

    private void add(JPanel panel, GridBagConstraints base, int row, String leftKey,
            java.awt.Component left, String rightKey, java.awt.Component right) {
        GridBagConstraints c = (GridBagConstraints) base.clone();
        c.gridy = row; c.gridx = 0; c.weightx = 0; panel.add(new JLabel(messages.text(leftKey)), c);
        c.gridx = 1; c.weightx = 1; panel.add(left, c);
        c.gridx = 2; c.weightx = 0; panel.add(new JLabel(messages.text(rightKey)), c);
        c.gridx = 3; c.weightx = 1; panel.add(right, c);
    }

    private JPanel lines() {
        JTable table = new JTable(lineModel);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton add = new JButton(messages.text("invoice.lineAdd"));
        add.addActionListener(event -> addLine());
        JButton remove = new JButton(messages.text("invoice.lineRemove"));
        remove.addActionListener(event -> { if (table.getSelectedRow() >= 0) lineModel.removeRow(table.getSelectedRow()); });
        buttons.add(add); buttons.add(remove);
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.add(buttons, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel actions() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton cancel = new JButton(messages.text("invoice.cancel"));
        cancel.addActionListener(event -> close.run());
        JButton save = new JButton(messages.text("invoice.saveDraft"));
        save.addActionListener(event -> save());
        actions.add(cancel); actions.add(save);
        return actions;
    }

    private void addLine() { lineModel.addRow(new Object[] { "", "1", "0.00", "19" }); }

    private void populate(InvoiceDetailsDto invoice) {
        sellerName.setText(invoice.sellerName()); sellerAddress.setText(invoice.sellerAddress());
        sellerTaxId.setText(invoice.sellerTaxId()); customerName.setText(invoice.summary().customerName());
        customerAddress.setText(invoice.customerAddress()); serviceDate.setText(invoice.serviceDate().toString());
        currency.setText(invoice.currency()); paymentTerms.setText(invoice.paymentTerms());
        for (InvoiceLineDto line : invoice.lines()) lineModel.addRow(new Object[] { line.description(),
                line.quantity(), line.unitNet(), line.taxRate() });
    }

    private void save() {
        try {
            List<InvoiceLineInput> lines = new ArrayList<InvoiceLineInput>();
            for (int row = 0; row < lineModel.getRowCount(); row++) lines.add(new InvoiceLineInput(
                    value(row, 0), decimal(row, 1), decimal(row, 2), decimal(row, 3)));
            CreateInvoiceCommand command = new CreateInvoiceCommand(sellerName.getText(), sellerAddress.getText(),
                    sellerTaxId.getText(), customerName.getText(), customerAddress.getText(),
                    LocalDate.parse(serviceDate.getText().trim()), currency.getText(), paymentTerms.getText(), lines);
            ApplicationResult<InvoiceDto> result = existing == null ? invoices.createDraft(command)
                    : invoices.updateDraft(existing.summary().id(), command);
            if (!result.isSuccess()) { error(result.message()); return; }
            close.run();
        } catch (RuntimeException invalid) { error(messages.text("invoice.invalidInput") + " " + invalid.getMessage()); }
    }

    private String value(int row, int column) {
        Object value = lineModel.getValueAt(row, column);
        return value == null ? "" : value.toString().trim();
    }
    private BigDecimal decimal(int row, int column) { return new BigDecimal(value(row, column).replace(',', '.')); }
    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, messages.text("invoice.error"), JOptionPane.ERROR_MESSAGE);
    }
}
