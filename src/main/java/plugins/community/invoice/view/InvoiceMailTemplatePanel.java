package plugins.community.invoice.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import platform.api.application.ApplicationResult;
import platform.api.i18n.PluginMessages;
import platform.api.ui.StandardView;
import plugins.community.invoice.application.InvoiceMailService;
import plugins.community.invoice.application.InvoiceMailTemplateDto;

/** Usable subject/body editor for invoice mails. */
final class InvoiceMailTemplatePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final InvoiceMailService mail;
    private final PluginMessages messages;
    private final JTextField subject = new JTextField();
    private final JTextArea body = new JTextArea();
    InvoiceMailTemplatePanel(InvoiceMailService mail, PluginMessages messages) {
        super(new BorderLayout()); this.mail = mail; this.messages = messages;
        body.setLineWrap(true); body.setWrapStyleWord(true);
        JPanel editor = new JPanel(new BorderLayout(0, 8));
        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 4));
        heading.add(new JLabel(messages.text("invoice.mailSubject"))); heading.add(subject);
        editor.add(heading, BorderLayout.NORTH); editor.add(new JScrollPane(body), BorderLayout.CENTER);
        editor.add(new JLabel(messages.text("invoice.mailTemplateHelp")), BorderLayout.SOUTH);
        add(StandardView.create(messages.text("invoice.mailTemplate"), editor, actions()), BorderLayout.CENTER); load();
    }
    private JPanel actions() { JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); JButton restore = new JButton(messages.text("invoice.templateRestore")); restore.addActionListener(e -> restore()); JButton save = new JButton(messages.text("invoice.templateSave")); save.addActionListener(e -> save()); panel.add(restore); panel.add(save); return panel; }
    private void load() { ApplicationResult<InvoiceMailTemplateDto> result = mail.template(); if (result.isSuccess()) set(result.valueOrThrow()); else error(result.message()); }
    private void save() { ApplicationResult<InvoiceMailTemplateDto> result = mail.saveTemplate(new InvoiceMailTemplateDto(subject.getText(), body.getText())); if (result.isSuccess()) { set(result.valueOrThrow()); JOptionPane.showMessageDialog(this, messages.text("invoice.mailTemplateSaved")); } else error(result.message()); }
    private void restore() { if (JOptionPane.showConfirmDialog(this, messages.text("invoice.templateRestoreConfirm"), messages.text("invoice.templateRestore"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) return; ApplicationResult<InvoiceMailTemplateDto> result = mail.restoreDefaultTemplate(); if (result.isSuccess()) set(result.valueOrThrow()); else error(result.message()); }
    private void set(InvoiceMailTemplateDto value) { subject.setText(value.subject()); body.setText(value.body()); body.setCaretPosition(0); }
    private void error(String value) { JOptionPane.showMessageDialog(this, value, messages.text("invoice.error"), JOptionPane.ERROR_MESSAGE); }
}
