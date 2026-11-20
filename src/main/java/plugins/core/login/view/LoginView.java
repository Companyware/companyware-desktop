package plugins.core.login.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Observable;
import java.util.Observer;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import core.ApplicationContextProvider;
import core.TextMessages;
import core.ui.CompanywareTheme;
import pluginmanager.plugininterfaces.PluginManager;
import plugins.core.login.controller.LoginController;
import platform.api.i18n.LocalizationService;
import platform.api.i18n.LocalizationServiceIds;

public class LoginView extends JDialog implements Observer {
    private static final long serialVersionUID = 1L;

    private final PluginManager pm;
    private JPanel panel;
    private JPanel messagePanel;
    private JPasswordField passwordField;
    private JTextField userTextField;
    private JToggleButton showPassword;
    private JComboBox<LanguageOption> language;
    private boolean rebuilding;
    private boolean userSelectedLanguage;

    public LoginView(PluginManager pm, JFrame frame) {
        super(frame);
        this.pm = pm;
        initLayout();
    }

    public void initLayout() {
        TextMessages messages = ApplicationContextProvider.getContext().getBean(TextMessages.class);
        setTitle(messages.get("loginview.title"));
        setModal(true);
        setAutoRequestFocus(true);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        setSize(new Dimension(860, 520));
        setLocationRelativeTo(getOwner());

        panel = new JPanel(new BorderLayout());
        panel.setBackground(CompanywareTheme.surface());
        setContentPane(panel);

        LoginController loginController = (LoginController) pm.getService("LoginController");
        addWindowListener(loginController);
    }

    public void setContent() {
		String rememberedUser = userTextField == null ? "" : userTextField.getText();
		char[] rememberedPassword = passwordField == null ? new char[0] : passwordField.getPassword();
		panel.removeAll();
		rebuilding = true;
        LoginController controller = (LoginController) pm.getService("LoginController");
        TextMessages messages = ApplicationContextProvider.getContext().getBean(TextMessages.class);
        LocalizationService localization = pm.getService(LocalizationServiceIds.LOCALIZATION).get();

        JPanel brand = new JPanel();
        brand.setBackground(CompanywareTheme.sidebar());
        brand.setPreferredSize(new Dimension(340, 520));
        brand.setBorder(BorderFactory.createEmptyBorder(58, 48, 48, 48));
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));

        JLabel logo = new JLabel("<html>Companyware<br><span style='font-size:10px;font-weight:normal'>Desktop</span></html>",
                CompanywareTheme.logo(58), SwingConstants.LEFT);
        logo.setFont(CompanywareTheme.BODY.deriveFont(java.awt.Font.BOLD, 24f));
        logo.setIconTextGap(14);
        logo.setAlignmentX(LEFT_ALIGNMENT);
        JLabel eyebrow = new JLabel(messages.get("loginview.brand.eyebrow"));
        eyebrow.setFont(CompanywareTheme.LABEL);
        eyebrow.setForeground(CompanywareTheme.accent());
        eyebrow.setAlignmentX(LEFT_ALIGNMENT);
        JLabel claim = new JLabel(messages.get("loginview.brand.claim"));
        claim.setFont(CompanywareTheme.TITLE);
        claim.setForeground(CompanywareTheme.text());
        claim.setAlignmentX(LEFT_ALIGNMENT);
        JLabel copy = new JLabel(messages.get("loginview.brand.copy"));
        copy.setFont(CompanywareTheme.BODY);
        copy.setForeground(CompanywareTheme.muted());
        copy.setAlignmentX(LEFT_ALIGNMENT);

        brand.add(logo);
        brand.add(Box.createVerticalGlue());
        brand.add(eyebrow);
        brand.add(Box.createVerticalStrut(14));
        brand.add(claim);
        brand.add(Box.createVerticalStrut(18));
        brand.add(copy);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(CompanywareTheme.surface());
        form.setBorder(BorderFactory.createEmptyBorder(46, 58, 40, 58));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

		language = new JComboBox<LanguageOption>();
		for (Locale locale : localization.supportedLocales()) language.addItem(new LanguageOption(locale));
		language.setSelectedItem(new LanguageOption(localization.locale()));
		language.setActionCommand("language");
		language.addActionListener(controller);
		gbc.gridy = 0; gbc.insets = new Insets(0, 0, 18, 0);
		form.add(language, gbc);

        JLabel access = new JLabel(messages.get("loginview.access"));
        access.setFont(CompanywareTheme.LABEL);
        access.setForeground(CompanywareTheme.accent());
		gbc.gridy++; gbc.insets = new Insets(0, 0, 12, 0);
        form.add(access, gbc);

        JLabel title = new JLabel(messages.get("loginview.welcome"));
        title.setFont(CompanywareTheme.TITLE);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 8, 0);
        form.add(title, gbc);

        JLabel subtitle = new JLabel(messages.get("loginview.subtitle"));
        subtitle.setForeground(CompanywareTheme.muted());
        gbc.gridy++; gbc.insets = new Insets(0, 0, 24, 0);
        form.add(subtitle, gbc);

        JLabel userLabel = new JLabel(messages.get("loginview.username"));
        userLabel.setFont(CompanywareTheme.LABEL);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 6, 0);
        form.add(userLabel, gbc);

        userTextField = new JTextField();
		userTextField.setText(rememberedUser);
        userTextField.setPreferredSize(new Dimension(360, 44));
        userTextField.addKeyListener(controller);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 16, 0);
        form.add(userTextField, gbc);

        JLabel passwordLabel = new JLabel(messages.get("loginview.password"));
        passwordLabel.setFont(CompanywareTheme.LABEL);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 6, 0);
        form.add(passwordLabel, gbc);

        passwordField = new JPasswordField();
		passwordField.setText(new String(rememberedPassword));
		java.util.Arrays.fill(rememberedPassword, '\0');
        passwordField.setPreferredSize(new Dimension(320, 44));
        passwordField.addKeyListener(controller);
        JPanel passwordInput = new JPanel(new BorderLayout());
        passwordInput.setOpaque(false);
        passwordInput.setPreferredSize(new Dimension(360, 44));
        passwordInput.add(passwordField, BorderLayout.CENTER);

        showPassword = new JToggleButton(CompanywareTheme.eyeIcon(22, 16));
        showPassword.setSelectedIcon(CompanywareTheme.eyeOffIcon(22, 16));
        showPassword.setToolTipText(messages.get("loginview.showpassword"));
        showPassword.getAccessibleContext().setAccessibleName(messages.get("loginview.showpassword"));
        showPassword.setActionCommand("showPassword");
        showPassword.addActionListener(controller);
        showPassword.setFocusable(false);
        showPassword.setPreferredSize(new Dimension(40, 44));
        passwordInput.add(showPassword, BorderLayout.EAST);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 12, 0);
        form.add(passwordInput, gbc);

		JButton recovery = new JButton(messages.get("loginview.recovery"));
		recovery.setActionCommand("recovery");
		recovery.addActionListener(controller);
		recovery.setBorderPainted(false); recovery.setContentAreaFilled(false);
		recovery.setHorizontalAlignment(SwingConstants.LEFT);
		gbc.gridy++; gbc.insets = new Insets(0, -8, 10, 0);
		form.add(recovery, gbc);

        JButton login = new JButton(messages.get("loginview.login") + "  →");
        login.setActionCommand("Login");
        login.setPreferredSize(new Dimension(360, 46));
        login.addActionListener(controller);
        CompanywareTheme.primaryButton(login);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 10, 0);
        form.add(login, gbc);

        JButton cancel = new JButton(messages.get("loginview.cancel"));
        cancel.setActionCommand("Abbrechen");
        cancel.addActionListener(controller);
        CompanywareTheme.secondaryButton(cancel);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 0, 0);
        form.add(cancel, gbc);

        messagePanel = new JPanel();
        messagePanel.setOpaque(false);
        gbc.gridy++; gbc.weighty = 1; gbc.fill = GridBagConstraints.BOTH;
        form.add(messagePanel, gbc);

        panel.add(brand, BorderLayout.WEST);
        panel.add(form, BorderLayout.CENTER);
        getRootPane().setDefaultButton(login);
        revalidate();
        repaint();
		rebuilding = false;
    }

	public Locale getSelectedLocale() {
		LanguageOption selected = (LanguageOption) language.getSelectedItem();
		return selected == null ? Locale.ENGLISH : selected.locale;
	}
	public boolean isRebuilding() { return rebuilding; }
	public boolean isLanguageSelectedByUser() { return userSelectedLanguage; }
	public void markLanguageSelectedByUser() { userSelectedLanguage = true; }

	private static final class LanguageOption {
		private final Locale locale;
		LanguageOption(Locale locale) { this.locale = locale; }
		@Override public String toString() { return displayLanguage(locale); }
		@Override public boolean equals(Object value) {
			return value instanceof LanguageOption && locale.getLanguage().equals(((LanguageOption) value).locale.getLanguage());
		}
		@Override public int hashCode() { return locale.getLanguage().hashCode(); }
	}

	private static String displayLanguage(Locale locale) {
		String name = locale.getDisplayLanguage(locale);
		return name.isEmpty() ? name : name.substring(0, 1).toUpperCase(locale) + name.substring(1);
	}

    public void focusUsername() {
        toFront();
        requestFocus();
        userTextField.selectAll();
        userTextField.requestFocusInWindow();
    }

	public void clearCredentials() {
		if (userTextField != null) userTextField.setText("");
		if (passwordField != null) passwordField.setText("");
	}

    @Override public void update(Observable observable, Object value) { }

    public JPanel getPanel() { return panel; }
    public void setPanel(JPanel panel) { this.panel = panel; }
    public JPanel getMessagePanel() { return messagePanel; }
    public void setMessagePanel(JPanel messagePanel) { this.messagePanel = messagePanel; }
    public JPasswordField getPasswordField() { return passwordField; }
    public void setPasswordField(JPasswordField passwordField) { this.passwordField = passwordField; }
    public JTextField getUserTextField() { return userTextField; }
    public void setUserTextField(JTextField userTextField) { this.userTextField = userTextField; }
    public JToggleButton getShowPassword() { return showPassword; }
    public void setShowPassword(JToggleButton showPassword) { this.showPassword = showPassword; }
}
