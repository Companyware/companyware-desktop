package plugins.community.invoice.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import platform.api.application.ApplicationResult;
import platform.api.i18n.PluginMessages;
import platform.api.ui.StandardView;
import plugins.community.invoice.application.InvoiceMailService;
import plugins.community.invoice.application.SmtpSettingsDto;

/** User-facing SMTP endpoint configuration; passwords remain process-local secrets. */
final class InvoiceSmtpSettingsPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final InvoiceMailService mail;
    private final PluginMessages messages;
    private final JTextField host = new JTextField(28);
    private final JSpinner port = new JSpinner(new SpinnerNumberModel(587, 1, 65535, 1));
    private final JTextField fromName = new JTextField(28);
    private final JTextField from = new JTextField(28);
    private final JTextField username = new JTextField(28);
    private final JPasswordField password = new JPasswordField(28);
    private final JCheckBox startTls = new JCheckBox();
    private final JLabel passwordState = new JLabel();
    private final JLabel result = new JLabel(" ");

    InvoiceSmtpSettingsPanel(InvoiceMailService mail, PluginMessages messages) {
        super(new BorderLayout()); this.mail = mail; this.messages = messages;
        JPanel form = new JPanel(new GridBagLayout());
        addRow(form, 0, "invoice.smtpHost", host);
        addRow(form, 1, "invoice.smtpPort", port);
        addRow(form, 2, "invoice.smtpFromName", fromName);
        addRow(form, 3, "invoice.smtpFrom", from);
        addRow(form, 4, "invoice.smtpUsername", username);
        addRow(form, 5, "invoice.smtpPassword", password);
        addRow(form, 6, "invoice.smtpEncryption", startTls);
        GridBagConstraints state = constraints(1, 7); state.fill = GridBagConstraints.HORIZONTAL; form.add(passwordState, state);
        GridBagConstraints help = constraints(0, 8); help.gridwidth = 2; help.fill = GridBagConstraints.HORIZONTAL;
        help.insets = new Insets(18, 0, 0, 0); form.add(new JLabel(messages.text("invoice.smtpPasswordHelp")), help);
        GridBagConstraints status = constraints(0, 9); status.gridwidth = 2; status.fill = GridBagConstraints.HORIZONTAL;
        status.insets = new Insets(12, 0, 0, 0); form.add(result, status);
        JPanel wrapper = new JPanel(new BorderLayout()); wrapper.add(form, BorderLayout.NORTH);
        add(StandardView.create(messages.text("invoice.smtpSettings"), wrapper, actions()), BorderLayout.CENTER);
        load();
    }

    private JPanel actions() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton test = new JButton(messages.text("invoice.smtpTest")); test.addActionListener(e -> test());
        JButton save = new JButton(messages.text("invoice.smtpSave")); save.addActionListener(e -> save());
        panel.add(test); panel.add(save); return panel;
    }
    private void addRow(JPanel panel, int row, String key, java.awt.Component field) {
        panel.add(new JLabel(messages.text(key)), constraints(0, row));
        GridBagConstraints value = constraints(1, row); value.weightx = 1; value.fill = GridBagConstraints.HORIZONTAL; panel.add(field, value);
    }
    private static GridBagConstraints constraints(int x, int y) {
        GridBagConstraints value = new GridBagConstraints(); value.gridx = x; value.gridy = y;
        value.anchor = GridBagConstraints.WEST; value.insets = new Insets(4, x == 0 ? 0 : 12, 4, 0); return value;
    }
    private void load() {
        ApplicationResult<SmtpSettingsDto> loaded = mail.smtpSettings();
        if (!loaded.isSuccess()) { error(messages.text("invoice.smtpLoadFailed")); return; }
        set(loaded.valueOrThrow());
    }
    private void set(SmtpSettingsDto value) {
        host.setText(value.host()); port.setValue(value.port()); fromName.setText(value.fromName()); from.setText(value.from());
        username.setText(value.username()); startTls.setSelected(value.startTls()); password.setText("");
        passwordState.setText(messages.text(value.passwordAvailable() ? "invoice.smtpPasswordAvailable" : "invoice.smtpPasswordMissing"));
    }
    private SmtpSettingsDto input() {
        return new SmtpSettingsDto(host.getText(), ((Number) port.getValue()).intValue(), fromName.getText(), from.getText(),
                username.getText(), startTls.isSelected(), false);
    }
    private void save() {
        char[] secret = password.getPassword();
        ApplicationResult<SmtpSettingsDto> saved = mail.saveSmtpSettings(input(), secret);
        if (!saved.isSuccess()) { error(localize(saved.message())); return; }
        set(saved.valueOrThrow()); result.setText(messages.text("invoice.smtpSaved"));
    }
    private void test() {
        char[] secret = password.getPassword();
        ApplicationResult<Void> tested = mail.testSmtpSettings(input(), secret);
        password.setText("");
        if (!tested.isSuccess()) { error(messages.text("invoice.smtpTestFailed", tested.message())); return; }
        result.setText(messages.text("invoice.smtpTestSucceeded"));
    }
    private String localize(String value) { return value == null || value.trim().isEmpty() ? messages.text("invoice.smtpSaveFailed") : value; }
    private void error(String value) { JOptionPane.showMessageDialog(this, value, messages.text("invoice.error"), JOptionPane.ERROR_MESSAGE); }
}
