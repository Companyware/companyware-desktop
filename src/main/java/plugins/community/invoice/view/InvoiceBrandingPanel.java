package plugins.community.invoice.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.nio.file.Files;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import platform.api.application.ApplicationResult;
import platform.api.i18n.PluginMessages;
import platform.api.ui.StandardView;
import plugins.community.invoice.application.InvoiceBrandingDto;
import plugins.community.invoice.application.InvoiceDocumentService;

/** Graphical editor for invoice letterhead defaults and full-page preview. */
final class InvoiceBrandingPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final InvoiceDocumentService documents;
    private final PluginMessages messages;
    private final JLabel logo = new JLabel();
    private final JLabel status = new JLabel(" ");
    private final JSpinner width = new JSpinner(new SpinnerNumberModel(40, 10, 80, 1));
    private final JComboBox<String> alignment;
    private final JTextField sellerName = new JTextField(24);
    private final JTextArea sellerAddress = new JTextArea(4, 24);
    private final JTextField sellerTaxId = new JTextField(24);
    private final JTextArea footer = new JTextArea(4, 24);
    private final JEditorPane preview = new JEditorPane();
    private final SwingHtmlPreviewSupport previewSupport = new SwingHtmlPreviewSupport();
    private byte[] content;
    private String mediaType;

    InvoiceBrandingPanel(InvoiceDocumentService documents, PluginMessages messages) {
        super(new BorderLayout());
        this.documents = documents;
        this.messages = messages;
        alignment = new JComboBox<String>(new String[] { messages.text("invoice.logo.left"),
                messages.text("invoice.logo.center"), messages.text("invoice.logo.right") });
        logo.setHorizontalAlignment(JLabel.CENTER); logo.setVerticalAlignment(JLabel.CENTER);
        logo.setBorder(BorderFactory.createEtchedBorder());
        preview.setEditable(false); preview.setContentType("text/html");
        sellerAddress.setLineWrap(true); sellerAddress.setWrapStyleWord(true);
        footer.setLineWrap(true); footer.setWrapStyleWord(true);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editor(), previewPanel());
        split.setResizeWeight(0.34); split.setContinuousLayout(true);
        add(StandardView.create(messages.text("invoice.branding"), split, actions()), BorderLayout.CENTER);
        load();
    }

    private JPanel editor() {
        JPanel fields = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4); c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;
        add(fields, c, row++, "invoice.sellerName", sellerName);
        add(fields, c, row++, "invoice.sellerAddress", new JScrollPane(sellerAddress));
        add(fields, c, row++, "invoice.sellerTaxId", sellerTaxId);
        add(fields, c, row++, "invoice.letterheadFooter", new JScrollPane(footer));
        JPanel logoOptions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        logoOptions.add(new JLabel(messages.text("invoice.logo.width"))); logoOptions.add(width);
        logoOptions.add(new JLabel(messages.text("invoice.logo.alignment"))); logoOptions.add(alignment);
        c.gridy = row++; c.gridx = 0; c.gridwidth = 2; c.weightx = 1; fields.add(logoOptions, c);
        c.gridy = row; c.weighty = 1; c.fill = GridBagConstraints.BOTH; fields.add(logo, c);
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel(messages.text("invoice.letterheadDetails")), BorderLayout.NORTH);
        panel.add(fields, BorderLayout.CENTER); panel.add(status, BorderLayout.SOUTH);
        return panel;
    }

    private void add(JPanel panel, GridBagConstraints base, int row, String key, java.awt.Component field) {
        GridBagConstraints c = (GridBagConstraints) base.clone();
        c.gridy = row; c.gridx = 0; c.weightx = 0; panel.add(new JLabel(messages.text(key)), c);
        c.gridx = 1; c.weightx = 1; panel.add(field, c);
    }

    private JPanel previewPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.add(new JLabel(messages.text("invoice.letterheadPreview")), BorderLayout.NORTH);
        panel.add(new JScrollPane(preview), BorderLayout.CENTER);
        return panel;
    }

    private JPanel actions() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton restore = new JButton(messages.text("invoice.logo.restoreDefault")); restore.addActionListener(event -> restoreDefaultLogo());
        JButton choose = new JButton(messages.text("invoice.logo.choose")); choose.addActionListener(event -> choose());
        JButton remove = new JButton(messages.text("invoice.logo.remove")); remove.addActionListener(event -> remove());
        JButton refresh = new JButton(messages.text("invoice.templateRefreshPreview")); refresh.addActionListener(event -> refreshPreview());
        JButton save = new JButton(messages.text("invoice.letterheadSave")); save.addActionListener(event -> save());
        actions.add(restore); actions.add(choose); actions.add(remove); actions.add(refresh); actions.add(save);
        return actions;
    }

    private void load() {
        ApplicationResult<InvoiceBrandingDto> result = documents.branding();
        if (!result.isSuccess()) { error(result.message()); return; }
        apply(result.valueOrThrow()); refreshPreview();
    }

    private void choose() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter(messages.text("invoice.logo.files"), "png", "jpg", "jpeg"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            if (Files.size(chooser.getSelectedFile().toPath()) > 2L * 1024L * 1024L)
                throw new IllegalArgumentException(messages.text("invoice.logo.tooLarge"));
            content = Files.readAllBytes(chooser.getSelectedFile().toPath());
            String name = chooser.getSelectedFile().getName().toLowerCase(java.util.Locale.ROOT);
            mediaType = name.endsWith(".png") ? "image/png" : "image/jpeg";
            showLogo(); refreshPreview(); status.setText(messages.text("invoice.logo.notSaved"));
        } catch (Exception failure) { error(failure.getMessage() == null ? messages.text("invoice.logo.readFailed") : failure.getMessage()); }
    }

    private void save() {
        ApplicationResult<InvoiceBrandingDto> result = documents.saveBranding(value());
        if (!result.isSuccess()) { error(result.message()); return; }
        apply(result.valueOrThrow()); refreshPreview(); status.setText(messages.text("invoice.letterheadSaved"));
    }

    private void remove() {
        if (JOptionPane.showConfirmDialog(this, messages.text("invoice.logo.removeConfirm"), messages.text("invoice.logo.remove"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) return;
        ApplicationResult<InvoiceBrandingDto> result = documents.removeLogo();
        if (!result.isSuccess()) { error(result.message()); return; }
        apply(result.valueOrThrow()); refreshPreview(); status.setText(messages.text("invoice.logo.removed"));
    }

    private void restoreDefaultLogo() {
        if (JOptionPane.showConfirmDialog(this, messages.text("invoice.logo.restoreDefaultConfirm"),
                messages.text("invoice.logo.restoreDefault"), JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) return;
        ApplicationResult<InvoiceBrandingDto> result = documents.restoreDefaultLogo();
        if (!result.isSuccess()) { error(result.message()); return; }
        apply(result.valueOrThrow()); refreshPreview(); status.setText(messages.text("invoice.logo.defaultRestored"));
    }

    private InvoiceBrandingDto value() {
        return new InvoiceBrandingDto(content, mediaType, ((Number) width.getValue()).intValue(), alignment(),
                sellerName.getText(), sellerAddress.getText(), sellerTaxId.getText(), footer.getText());
    }

    private void apply(InvoiceBrandingDto value) {
        content = value.logo(); mediaType = value.mediaType(); width.setValue(value.widthMillimeters());
        alignment.setSelectedIndex("CENTER".equals(value.alignment()) ? 1 : "RIGHT".equals(value.alignment()) ? 2 : 0);
        sellerName.setText(value.sellerName()); sellerAddress.setText(value.sellerAddress());
        sellerTaxId.setText(value.sellerTaxId()); footer.setText(value.footer()); showLogo();
    }

    private void refreshPreview() {
        ApplicationResult<String> result = documents.previewBrandingHtml(value());
        if (!result.isSuccess()) { status.setText(result.message()); return; }
        try {
            preview.setText(previewSupport.prepare(result.valueOrThrow())); preview.setCaretPosition(0);
            status.setText(messages.text("invoice.letterheadPreviewReady"));
        } catch (Exception failure) { status.setText(failure.getMessage()); }
    }

    private void showLogo() {
        if (content == null || content.length == 0) { logo.setIcon(null); logo.setText(messages.text("invoice.logo.empty")); return; }
        javax.swing.ImageIcon original = new javax.swing.ImageIcon(content);
        int previewWidth = Math.min(280, Math.max(80, ((Number) width.getValue()).intValue() * 4));
        int previewHeight = Math.max(1, original.getIconHeight() * previewWidth / Math.max(1, original.getIconWidth()));
        logo.setText(null); logo.setIcon(new javax.swing.ImageIcon(original.getImage().getScaledInstance(previewWidth, previewHeight, Image.SCALE_SMOOTH)));
    }

    private String alignment() { return alignment.getSelectedIndex() == 1 ? "CENTER" : alignment.getSelectedIndex() == 2 ? "RIGHT" : "LEFT"; }
    private void error(String message) { JOptionPane.showMessageDialog(this, message, messages.text("invoice.error"), JOptionPane.ERROR_MESSAGE); }
    @Override public void removeNotify() { previewSupport.close(); super.removeNotify(); }
}
