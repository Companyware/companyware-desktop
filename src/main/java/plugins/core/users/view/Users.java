/*******************************************************************************
 * MIT License
 *
 * Companyware - a java desktop framework for plugins
 *
 * Copyright (c) 2023 mbdus-Softwareentwicklung - Mathias Bauer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package plugins.core.users.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.TableColumnModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import core.ApplicationContextProvider;
import core.TextMessages;
import core.ui.CompanywareTheme;
import models.user.UserModel;
import models.user.Repository;
import pluginmanager.plugininterfaces.PluginManager;
import plugins.core.frame.controller.FrameController;
import plugins.core.frame.view.Frame;
import plugins.core.frame.view.JOutlookBar;
import plugins.core.menu.view.Menu;
import plugins.core.users.controller.ButtonTableEditor;
import plugins.core.users.controller.CheckboxCellEditor;
import plugins.core.users.controller.ImageButtonTableEditor;
import plugins.core.users.controller.UsersController;
import platform.api.i18n.PluginMessages;
import plugins.core.plugins.model.DisplayableObjectTableModel;
import plugins.core.plugins.model.ObjectTableModel;
import platform.api.ui.StandardView;
import platform.api.ui.CompanywareIcons;
import platform.api.plugin.PluginId;
import platform.api.theme.ThemeProvider;
import platform.api.theme.ThemeRegistry;

public class Users  implements Observer {
	private static final Log log = LogFactory.getLog(Users.class);
	private static final int CONTENT_HEADER_HEIGHT = 100;
	private static final int CONTENT_TITLE_HEIGHT = 50;
	
	private PluginManager pm;

	private JTextField nameTextField;
	private JTextField emailTextField;
	private JCheckBox activeCheckbox;
	private JTextField usernameTextField;
	private JPasswordField passwordField;
	private JPasswordField currentPasswordField;
	private JPasswordField newPasswordField;
	private JPasswordField confirmPasswordField;
	private JComboBox<LanguageOption> profileLanguage;
	private JComboBox<ThemeOption> profileTheme;

	
	public Users(PluginManager pm) {
		this.pm = pm;
		this.createMenuEntry();
		this.addOutlookBarEntry();
	}
	
	/**
	 * create menu entry "Pluginmanager"
	 */
	public void createMenuEntry(){
		UsersController usersController = (UsersController)pm.getService("UsersController");
		Menu menuView = (Menu)pm.getService("MenuView");
		if(menuView != null){
			JMenu settings = menuView.getSettings();
			TextMessages service = ApplicationContextProvider.getContext().getBean(TextMessages.class);
			JMenuItem pluginManager = new JMenuItem(service.get("usermanager.menu"));
	        pluginManager.addActionListener(usersController);
	        menuView.addLocalizedItem(settings, pluginManager, "usermanager.menu");
		}
	}
	
	public void addOutlookBarEntry(){
		FrameController frameController = (FrameController)pm.getService("FrameController");
		Frame frameView = (Frame)frameController.getView();
		JOutlookBar outlookBar = frameView.getOutlookBar();
		UsersController usersController = (UsersController)pm.getService("UsersController");
		outlookBar.addBarEntry("settings");
		outlookBar.addBarEntryIcon("settings","adduser", CompanywareIcons.USER_ADD, usersController);
		outlookBar.addBarEntryIcon("settings","useroverview", CompanywareIcons.USERS, usersController);
		outlookBar.addBar("settings", outlookBar.getPanel("settings"),200);
		PluginMessages profileText = pm.getService(platform.api.i18n.LocalizationServiceIds.LOCALIZATION)
				.get().messages("i18n.profile");
		outlookBar.setFooterAction(profileText.text("profile.action"), CompanywareTheme.profileIcon(22),
				"profile", usersController);
		outlookBar.addFooterAction(profileText.text("profile.logout"), null, "logout", usersController);
		outlookBar.setPreferredSize();
	}

	public void profileView(UserModel userModel) {
		UsersController controller = (UsersController) pm.getService("UsersController");
		Frame frame = ((FrameController) pm.getService("FrameController")).getView();
		JPanel panel = new JPanel(new BorderLayout());
		PluginMessages text = pm.getService(platform.api.i18n.LocalizationServiceIds.LOCALIZATION)
				.get().messages("i18n.profile");
		JLabel title = new JLabel(text.text("profile.title"), SwingConstants.CENTER);
		title.setFont(new Font(title.getFont().getName(), Font.BOLD, 20));
		JPanel header = new JPanel(new BorderLayout());
		header.setPreferredSize(new Dimension(100, CONTENT_HEADER_HEIGHT));
		header.add(title, BorderLayout.SOUTH);
		panel.add(header, BorderLayout.NORTH);

		JPanel form = new JPanel(null);
		int center = Math.max(520, frame.getCenterPanel().getWidth()) / 2;
		int y = 55;
		platform.api.i18n.LocalizationService localization = pm.getService(
				platform.api.i18n.LocalizationServiceIds.LOCALIZATION).get();
		JLabel languageLabel = new JLabel(text.text("profile.language"));
		languageLabel.setBounds(center - 260, y, 180, 30);
		profileLanguage = new JComboBox<LanguageOption>();
		for (Locale locale : localization.supportedLocales()) profileLanguage.addItem(new LanguageOption(locale));
		profileLanguage.setSelectedItem(new LanguageOption(localization.locale()));
		profileLanguage.setActionCommand("profile-language");
		profileLanguage.addActionListener(controller);
		profileLanguage.setBounds(center - 70, y, 330, 30);
		form.add(languageLabel); form.add(profileLanguage);
		y += 55;
		JLabel themeLabel = new JLabel(text.text("profile.theme"));
		themeLabel.setBounds(center - 260, y, 180, 30);
		ThemeRegistry themes = pm.getService(platform.api.ui.PlatformServiceIds.THEME_REGISTRY).get();
		profileTheme = new JComboBox<ThemeOption>();
		int selectedTheme = 0;
		for (ThemeProvider provider : themes.providers()) {
			if (!provider.overlay()) {
				if (provider.id().equals(themes.activeTheme())) selectedTheme = profileTheme.getItemCount();
				profileTheme.addItem(new ThemeOption(provider.id(), provider.displayName()));
			}
		}
		profileTheme.setSelectedIndex(selectedTheme);
		profileTheme.setActionCommand("profile-theme");
		profileTheme.addActionListener(controller);
		profileTheme.setBounds(center - 70, y, 330, 30);
		form.add(themeLabel); form.add(profileTheme);
		y += 55;
		nameTextField = addTextRow(form, text.text("profile.name"), userModel.getName(), center, y);
		y += 55;
		emailTextField = addTextRow(form, text.text("profile.email"), userModel.getEmail(), center, y);
		y += 42;
		JLabel verification = new JLabel(text.text(Boolean.TRUE.equals(userModel.getEmailVerified()) ? "profile.emailVerified" : "profile.emailUnverified"));
		verification.setBounds(center - 70, y, 330, 24); form.add(verification); y += 33;
		JButton verifyEmail = new JButton(text.text("profile.emailVerify"));
		verifyEmail.setActionCommand("verify-email"); verifyEmail.addActionListener(controller);
		verifyEmail.setBounds(center - 70, y, 330, 32); form.add(verifyEmail); y += 48;
		currentPasswordField = addPasswordRow(form, text.text("profile.currentPassword"), center, y);
		y += 55;
		newPasswordField = addPasswordRow(form, text.text("profile.newPassword"), center, y);
		y += 55;
		confirmPasswordField = addPasswordRow(form, text.text("profile.confirmPassword"), center, y);
		y += 70;
		JButton save = new JButton(text.text("profile.save"));
		save.setActionCommand("saveprofile");
		save.addActionListener(controller);
		save.setBounds(center - 100, y, 200, 40);
		form.add(save);
		y += 54;
		JButton recovery = new JButton(text.text("profile.recoveryRotate"));
		recovery.setActionCommand("recovery-key"); recovery.addActionListener(controller);
		recovery.setBounds(center - 100, y, 200, 40); form.add(recovery);
		panel.add(form, BorderLayout.CENTER);
		frame.getCenterPanel().removeAll();
		frame.getCenterPanel().add(panel, BorderLayout.CENTER);
		frame.getCenterPanel().revalidate();
		frame.getCenterPanel().repaint();
	}

	private JTextField addTextRow(JPanel panel, String caption, String value, int center, int y) {
		JLabel label = new JLabel(caption);
		label.setBounds(center - 260, y, 180, 30);
		JTextField field = new JTextField(value == null ? "" : value);
		field.setBounds(center - 70, y, 330, 30);
		panel.add(label); panel.add(field);
		return field;
	}

	private JPasswordField addPasswordRow(JPanel panel, String caption, int center, int y) {
		JLabel label = new JLabel(caption);
		label.setBounds(center - 260, y, 180, 30);
		JPasswordField field = new JPasswordField();
		field.setBounds(center - 70, y, 330, 30);
		panel.add(label); panel.add(field);
		return field;
	}

	public JPasswordField getCurrentPasswordField() { return currentPasswordField; }
	public JPasswordField getNewPasswordField() { return newPasswordField; }
	public JPasswordField getConfirmPasswordField() { return confirmPasswordField; }
	public void clearSessionCredentials() {
		if (passwordField != null) passwordField.setText("");
		if (currentPasswordField != null) currentPasswordField.setText("");
		if (newPasswordField != null) newPasswordField.setText("");
		if (confirmPasswordField != null) confirmPasswordField.setText("");
		passwordField = null; currentPasswordField = null; newPasswordField = null; confirmPasswordField = null;
		nameTextField = null; emailTextField = null; usernameTextField = null;
	}
	public Locale getProfileLocale() {
		LanguageOption selected = (LanguageOption) profileLanguage.getSelectedItem();
		return selected == null ? Locale.ENGLISH : selected.locale;
	}
	public PluginId getProfileTheme() {
		ThemeOption selected = (ThemeOption) profileTheme.getSelectedItem();
		return selected == null ? null : selected.id;
	}

	private static final class LanguageOption {
		private final Locale locale;
		LanguageOption(Locale locale) { this.locale = locale; }
		@Override public String toString() { return displayLanguage(locale); }
		@Override public boolean equals(Object value) {
			return value instanceof LanguageOption && locale.getLanguage().equals(((LanguageOption) value).locale.getLanguage());
		}
		@Override public int hashCode() { return locale.getLanguage().hashCode(); }
	}

	private static final class ThemeOption {
		private final PluginId id;
		private final String displayName;
		ThemeOption(PluginId id, String displayName) { this.id = id; this.displayName = displayName; }
		@Override public String toString() { return displayName; }
		@Override public boolean equals(Object value) {
			return value instanceof ThemeOption && id.equals(((ThemeOption) value).id);
		}
		@Override public int hashCode() { return id.hashCode(); }
	}

	private static String displayLanguage(Locale locale) {
		String name = locale.getDisplayLanguage(locale);
		return name.isEmpty() ? name : name.substring(0, 1).toUpperCase(locale) + name.substring(1);
	}
	
	public void createTableOverview(){
		FrameController frameController = (FrameController)pm.getService("FrameController");
		Frame frame = frameController.getView();
		TextMessages text = ApplicationContextProvider.getContext().getBean(TextMessages.class);
        List<UserModel> users=Repository.getUsers();
        
        UIManager.put("Table.alternateRowColor", UIManager.getColor("Table.background"));
        
        ObjectTableModel<UserModel> tableModel = new DisplayableObjectTableModel<>(UserModel.class);
        tableModel.setObjectRows(users);
        JTable table = new JTable(tableModel);
	    table.setDefaultRenderer(Object.class, new ObjectColumnRenderer());
	    table.setDefaultRenderer(Integer.class, new IntegerColumnRenderer());
        table.setDefaultRenderer(Boolean.class, new CheckboxColumnRenderer());
        table.setDefaultRenderer(JCheckBox.class, new ButtonColumnRenderer("Edit"));
        table.setDefaultRenderer(JButton.class, new ImageButtonColumnRenderer("Delete"));
        
        tableModel.addTableModelListener((e)->{table.repaint();});
	    
	    JCheckBox checkBox = new JCheckBox();
	    checkBox.setActionCommand("active");
        checkBox.setHorizontalAlignment(JCheckBox.CENTER);
   //     table.setDefaultEditor(Boolean.class, new DefaultCellEditor(checkBox));
        table.setDefaultEditor(Boolean.class,new CheckboxCellEditor(checkBox,pm));
        
        JCheckBox button = new JCheckBox();
        button.setActionCommand("settings button");
        button.setHorizontalAlignment(JCheckBox.CENTER);
        table.setDefaultEditor(JCheckBox.class, new ButtonTableEditor(button,pm));
        
        JCheckBox imageButton = new JCheckBox();
        imageButton.setActionCommand("delete");
        imageButton.setHorizontalAlignment(JCheckBox.CENTER);
        table.setDefaultEditor(JButton.class, new ImageButtonTableEditor(imageButton,pm));
	    
        table.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        TableColumnModel colModel=table.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(400);
        colModel.getColumn(1).setPreferredWidth(400);    
        colModel.getColumn(2).setPreferredWidth(25);
        colModel.getColumn(3).setPreferredWidth(5);    
        colModel.getColumn(4).setPreferredWidth(5);    
        
	    table.setModel(tableModel);
	    
	    table.setRowHeight(35);
	    
	    JPanel panel = StandardView.create(text.get("usermanager.name"), new JScrollPane(table));
	    frame.getCenterPanel().removeAll();
	    frame.getCenterPanel().add(panel,BorderLayout.CENTER);
	    frame.getCenterPanel().revalidate();
	    frame.getCenterPanel().repaint();
	    frame.setVisible(true);
	}
	
	public void addUserView(){
		UsersController usersController = (UsersController)pm.getService("UsersController");
		FrameController frameController = (FrameController)pm.getService("FrameController");
		Frame frame = frameController.getView();
		TextMessages service = ApplicationContextProvider.getContext().getBean(TextMessages.class);

        JPanel content = new JPanel(new GridBagLayout());
        JPanel formBlock = new JPanel(new BorderLayout(0, 24));
        formBlock.setPreferredSize(new Dimension(920, 360));
        JPanel columns = new JPanel(new java.awt.GridLayout(1, 2, 24, 0));
        JPanel leftContainer = userFormColumn();
        JPanel rightContainer = userFormColumn();

        int row = 0;
        this.nameTextField = new JTextField(24);
        addUserFormRow(leftContainer, row++, service.get("userview.name"), nameTextField);
        this.emailTextField = new JTextField(24);
        addUserFormRow(leftContainer, row++, service.get("userview.email"), emailTextField);
        this.activeCheckbox = new JCheckBox();
        addUserFormRow(leftContainer, row, service.get("userview.active"), activeCheckbox);

        row = 0;
        this.usernameTextField = new JTextField(24);
        addUserFormRow(rightContainer, row++, service.get("userview.username"), usernameTextField);
        this.passwordField = new JPasswordField(24);
        addUserFormRow(rightContainer, row, service.get("userview.password"), passwordField);

        columns.add(leftContainer);
        columns.add(rightContainer);
        formBlock.add(columns, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
        JButton save = new JButton(service.get("userview.saveuser"));
        save.setActionCommand("adduser");
        save.addActionListener(usersController);
        save.setPreferredSize(new Dimension(200, 40));
        buttonPanel.add(save);
        formBlock.add(buttonPanel, BorderLayout.SOUTH);
        content.add(formBlock);

        JPanel panel = StandardView.create(service.get("userview.adduser"), content);

        frame.getCenterPanel().removeAll();
	    frame.getCenterPanel().add(panel,BorderLayout.CENTER);
	    frame.getCenterPanel().revalidate();
	    frame.getCenterPanel().repaint();
	    frame.setVisible(true);
	}

	private JPanel userFormColumn() {
		JPanel column = new JPanel(new GridBagLayout());
		Color borderColor = UIManager.getColor("Component.borderColor");
		if (borderColor == null) {
			borderColor = Color.LIGHT_GRAY;
		}
		column.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(borderColor),
				BorderFactory.createEmptyBorder(28, 30, 28, 30)));
		return column;
	}

	private void addUserFormRow(JPanel column, int row, String caption, javax.swing.JComponent field) {
		GridBagConstraints label = new GridBagConstraints();
		label.gridx = 0; label.gridy = row; label.anchor = GridBagConstraints.LINE_START;
		label.insets = new Insets(10, 0, 10, 18);
		column.add(new JLabel(caption), label);

		GridBagConstraints input = new GridBagConstraints();
		input.gridx = 1; input.gridy = row; input.weightx = 1;
		input.fill = GridBagConstraints.HORIZONTAL;
		input.insets = new Insets(10, 0, 10, 0);
		column.add(field, input);
	}
	
	public JTextField getNameTextField() {
		return nameTextField;
	}

	public void setNameTextField(JTextField nameTextField) {
		this.nameTextField = nameTextField;
	}

	public JTextField getEmailTextField() {
		return emailTextField;
	}

	public void setEmailTextField(JTextField emailTextField) {
		this.emailTextField = emailTextField;
	}

	public JTextField getUsernameTextField() {
		return usernameTextField;
	}

	public void setUsernameTextField(JTextField usernameTextField) {
		this.usernameTextField = usernameTextField;
	}

	public JPasswordField getPasswordField() {
		return passwordField;
	}

	public void setPasswordField(JPasswordField passwordField) {
		this.passwordField = passwordField;
	}
	
	public JCheckBox getActiveCheckbox() {
		return activeCheckbox;
	}

	public void setActiveCheckbox(JCheckBox activeCheckbox) {
		this.activeCheckbox = activeCheckbox;
	}

	public void editUserView(UserModel userModel){
		UsersController usersController = (UsersController)pm.getService("UsersController");
		FrameController frameController = (FrameController)pm.getService("FrameController");
		Frame frame = frameController.getView();
		JPanel panel = new JPanel(new BorderLayout());
		panel.setPreferredSize(new Dimension(frame.getWidth()+frame.getConstCenterWidth()-250,frame.getHeight()+frame.getConstCenterHeight()-100));
		
		JPanel headerPanel = new JPanel(null);
		headerPanel.setPreferredSize(new Dimension(panel.getWidth()-50,CONTENT_HEADER_HEIGHT));
		
		TextMessages service = ApplicationContextProvider.getContext().getBean(TextMessages.class);
		
		JLabel label = new JLabel(service.get("userview.edituser"),SwingConstants.CENTER);
		label.setBounds((frame.getWidth()+frame.getConstCenterWidth()-250)/2-125,
				CONTENT_HEADER_HEIGHT-CONTENT_TITLE_HEIGHT,200,CONTENT_TITLE_HEIGHT);
        label.setFont(new Font(label.getFont().getName(), Font.BOLD, 20));
        headerPanel.add(label);
        panel.add(headerPanel,BorderLayout.NORTH);
        
        //Content
        JPanel buttonPanel = new JPanel(null);
        
        JButton save = new JButton(service.get("userview.saveuser"));
        save.setActionCommand("saveuser");
        save.addActionListener(usersController);
        
        save.setBounds((frame.getWidth()+frame.getConstCenterWidth()-250)/2-125,30,200,40);
   
        buttonPanel.add(save);
        buttonPanel.setPreferredSize(new Dimension(panel.getWidth()-50,100));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JPanel container = new JPanel(new BorderLayout());
        container.setPreferredSize(new Dimension((frame.getWidth()+frame.getConstCenterWidth()-250),frame.getHeight()+frame.getConstCenterHeight()-100));
        JPanel leftContainer = new JPanel(null);
        leftContainer.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JPanel rightContainer = new JPanel(null);
        rightContainer.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        leftContainer.setPreferredSize(new Dimension((frame.getWidth()/2)+frame.getConstCenterWidth()-100,frame.getHeight()+frame.getConstCenterHeight()-100));
        rightContainer.setPreferredSize(new Dimension((frame.getWidth()/2)+frame.getConstCenterWidth()-50,frame.getHeight()+frame.getConstCenterHeight()-100));
		int y = 150;
		
		JLabel nameLabel=new JLabel(service.get("userview.name"));
		nameLabel.setBounds(50,y,150,30);
		this.nameTextField=new JTextField();
		nameTextField.setBounds(200,y,300,30);
		nameTextField.setText(userModel.getName());
		leftContainer.add(nameLabel);
		leftContainer.add(nameTextField);
	    
	    y += 70;
	    JLabel emailLabel=new JLabel(service.get("userview.email"));
	    emailLabel.setBounds(50,y,150,30);
	    this.emailTextField=new JTextField();
	    emailTextField.setBounds(200,y,300,30);
	    emailTextField.setText(userModel.getEmail());
	    leftContainer.add(emailLabel);
	    leftContainer.add(emailTextField);
	    
	    y += 70;
	    JLabel activeLabel=new JLabel(service.get("userview.active"));
	    activeLabel.setBounds(50,y,150,30);
	    this.activeCheckbox=new JCheckBox();
	    activeCheckbox.setBounds(195,y,200,30);
	    activeCheckbox.setSelected(userModel.getActive());
	    leftContainer.add(activeLabel);
	    leftContainer.add(activeCheckbox);
	    
	    y = 150;
	    JLabel usernameLabel=new JLabel(service.get("userview.username"));
	    usernameLabel.setBounds(50,y,150,30);
	    this.usernameTextField=new JTextField();
	    usernameTextField.setBounds(200,y,300,30);
	    usernameTextField.setText(userModel.getUsername());
	    rightContainer.add(usernameLabel);
	    rightContainer.add(usernameTextField);
	    
	    y += 70;
	    JLabel passwordLabel=new JLabel(service.get("userview.password"));
	    passwordLabel.setBounds(50,y,150,30);
	    this.passwordField=new JPasswordField();
	    passwordField.setBounds(200,y,300,30);
//	    passwordField.setText(userModel.getPassword());
	    rightContainer.add(passwordLabel);
	    rightContainer.add(passwordField);
	    
	    container.add(leftContainer,BorderLayout.WEST);
	    container.add(rightContainer,BorderLayout.EAST);
	    
		container.add(buttonPanel,BorderLayout.SOUTH);
	
		panel.add(container, BorderLayout.CENTER);
        
        frame.getCenterPanel().removeAll();
	    frame.getCenterPanel().add(panel,BorderLayout.CENTER);
	    frame.getCenterPanel().revalidate();
	    frame.getCenterPanel().repaint();
	    frame.setVisible(true);
	}

	@Override
	public void update(Observable arg0, Object arg1) {
		// TODO Auto-generated method stub
	}
}
