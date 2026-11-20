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
package plugins.core.users.controller;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Observable;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import core.Companyware;
import core.ApplicationContextProvider;
import core.TextMessages;
import core.security.PasswordPolicy;
import platform.api.security.AccountRecoveryService;
import plugins.core.login.controller.LoginController;
import models.user.UserModel;
import pluginmanager.plugininterfaces.PluginManager;
import pluginmanager.plugininterfaces.Service;
import models.user.Repository;
import plugins.core.frame.controller.FrameController;
import plugins.core.frame.view.Frame;
import plugins.core.frame.view.JOutlookBar;
import plugins.core.users.view.Users;
import platform.api.security.AuditEvent;
import platform.api.security.AuditSink;
import platform.api.security.AuthorizationService;
import platform.api.security.AuthenticationResult;
import platform.api.security.AuthenticationService;
import platform.api.security.SecurityServiceIds;
import platform.api.i18n.LocalizationService;
import platform.api.i18n.LocalizationServiceIds;
import platform.api.theme.ThemeRegistry;
import platform.api.ui.PlatformServiceIds;

public class UsersController extends Observable implements Service, MouseListener, ActionListener{

	private static final Log log = LogFactory.getLog(UsersController.class);
	private UserModel model;
	private Users view;
	private PluginManager pm;
	private String username;
	private final AuditSink audit;
	private final AuthorizationService authorization;
	private final AuthenticationService authentication;
	private final LocalizationService localization;
	private final ThemeRegistry themes;
	
	public UsersController(PluginManager pm) {
		this.pm = pm;
		this.model = new UserModel();
		this.pm.registerService("UsersController",this);
		view = new Users(pm);
		model.addObserver(view);
		audit = pm.getService(SecurityServiceIds.AUDIT).orElse(null);
		authorization = pm.getService(SecurityServiceIds.AUTHORIZATION).orElse(null);
		authentication = pm.getService(SecurityServiceIds.AUTHENTICATION).orElse(null);
		localization = pm.getService(LocalizationServiceIds.LOCALIZATION).orElse(null);
		themes = pm.getService(PlatformServiceIds.THEME_REGISTRY).orElse(null);
	}
	
	@Override
	public void mouseClicked(MouseEvent event) {
		// Navigation is handled on mousePressed so the first physical click is
		// sufficient even while Swing is changing focus between content panels.
	}

	private void navigate(JLabel label) {
		switch (label.getName()) {
			case "useroverview":{
				this.createUsersOverview();
				break;
			}
			case "adduser":{
				this.createAddUserview();
				break;
			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent event) {
		JLabel label = (JLabel) event.getSource();
		label.setCursor(new Cursor(Cursor.HAND_CURSOR));
	}

	@Override
	public void mouseExited(MouseEvent event) {
		JLabel label = (JLabel) event.getSource();
		label.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}

	@Override
	public void mousePressed(MouseEvent event) {
		JLabel label = (JLabel) event.getSource();
		outlookBar().setSelectedEntry(label.getName());
		navigate(label);
	}

	@Override
	public void mouseReleased(MouseEvent event) {
		// TODO Auto-generated method stub
	}

	@SuppressWarnings("deprecation")
	@Override
	public void actionPerformed(ActionEvent e) {
		log.info("test action userscontroller");
		String command = e.getActionCommand().toString();
		UserModel user = null;
		this.model.printOut("ACTION: " + e.getActionCommand().toString());

		if (command.equals("profile")) {
			if (authorization != null && authorization.currentIdentity().isPresent()) {
				UserModel profile = Repository.getUserByUsername(authorization.currentIdentity().get().username());
				if (profile != null) {
					outlookBar().setSelectedEntry("profile");
					view.profileView(profile);
				}
			}
			return;
		}
		if (command.equals("logout")) {
			LoginController login = (LoginController) pm.getService("LoginController");
			if (login != null) login.logoutAndShowLogin();
			return;
		}
		if (command.equals("recovery-key")) {
			rotateRecoveryKey();
			return;
		}
		if (command.equals("verify-email")) { verifyEmail(); return; }
		if (command.equals("saveprofile")) {
			saveProfile();
			return;
		}
		if (command.equals("profile-language")) {
			changeProfileLanguage();
			return;
		}
		if (command.equals("profile-theme")) {
			if (themes != null && view.getProfileTheme() != null) themes.activate(view.getProfileTheme());
			return;
		}
		
		if(command.equals("adduser")){
			user = new UserModel();
			user.setRoles("USER");
			user.setPermissions("");
			user.setEmailVerified(false);
		}
		if(command.equals("saveuser")){
			user = Repository.getUserByUsername(this.username);
		}
		if(command.equals("adduser")||command.equals("saveuser")){
			if(user != null){
				user.setUsername(view.getUsernameTextField().getText());
				user.setName(view.getNameTextField().getText());
				String oldEmail = user.getEmail() == null ? "" : user.getEmail().trim();
				String enteredEmail = view.getEmailTextField().getText() == null ? "" : view.getEmailTextField().getText().trim();
				user.setEmail(enteredEmail);
				if (!oldEmail.equalsIgnoreCase(enteredEmail)) user.setEmailVerified(false);
				user.setActive(view.getActiveCheckbox().isSelected());
				char[] pw = view.getPasswordField().getPassword();
				if (pw.length > 0) {
					if (!PasswordPolicy.accepts(pw)) {
						showError(t("security.passwordPolicy"));
						java.util.Arrays.fill(pw, '\0');
						return;
					}
					user.setPassword(this.encoder().encode(new String(pw)));
					user.setPasswordChangeRequired(true);
				}
				java.util.Arrays.fill(pw, '\0');
				Repository.save(user);
				if (audit != null) audit.record(new AuditEvent("user-save", actor(), "success:" + user.getUsername()));
				Companyware.getContainer().setSuccessMessage(t("security.userSaved"), pm);
			}
		}
		if(command.equals("Benutzerverwaltung")){
	    		this.createUsersOverview();
	    		FrameController frameController = (FrameController)pm.getService("FrameController");
	    		Frame frameView = (Frame)frameController.getView();
	    		JOutlookBar outlookBar = frameView.getOutlookBar();
	    		outlookBar.setVisibleBar(1);
		}
	}

	private void saveProfile() {
		if (authorization == null || authentication == null || !authorization.currentIdentity().isPresent()) return;
		String currentUser = authorization.currentIdentity().get().username();
		UserModel user = Repository.getUserByUsername(currentUser);
		if (user == null) return;
		char[] current = view.getCurrentPasswordField().getPassword();
		char[] replacement = view.getNewPasswordField().getPassword();
		char[] confirmation = view.getConfirmPasswordField().getPassword();
		try {
			boolean passwordChanged = false;
			if (replacement.length > 0 || current.length > 0 || confirmation.length > 0) {
				if (!java.util.Arrays.equals(replacement, confirmation)) {
					showError(t("security.passwordMismatch")); return;
				}
				if (!PasswordPolicy.accepts(replacement)) {
					showError(t("security.passwordPolicy")); return;
				}
				AuthenticationResult changed = authentication.changePassword(currentUser, current, replacement);
				current = null; replacement = null;
				if (!changed.isAuthenticated()) {
					showError(t("security.passwordFailed")); return;
				}
				passwordChanged = true;
			}
			if (passwordChanged) user = Repository.getUserByUsername(currentUser);
			user.setName(view.getNameTextField().getText());
			String previousEmail = user.getEmail() == null ? "" : user.getEmail().trim();
			String newEmail = view.getEmailTextField().getText() == null ? "" : view.getEmailTextField().getText().trim();
			user.setEmail(newEmail);
			if (!previousEmail.equalsIgnoreCase(newEmail)) user.setEmailVerified(false);
			Repository.save(user);
			if (audit != null) audit.record(new AuditEvent("profile-save", currentUser, "success"));
			Companyware.getContainer().setSuccessMessage(t("security.profileSaved"), pm);
		} finally {
			if (current != null) java.util.Arrays.fill(current, '\0');
			if (replacement != null) java.util.Arrays.fill(replacement, '\0');
			java.util.Arrays.fill(confirmation, '\0');
		}
	}

	private void verifyEmail() {
		if (authorization == null || !authorization.currentIdentity().isPresent()) return;
		AccountRecoveryService recovery = pm.getService(SecurityServiceIds.RECOVERY_KEYS).orElse(null);
		if (recovery == null) { showError(t("security.recoveryUnavailable")); return; }
		String username = authorization.currentIdentity().get().username();
		java.awt.Component parent = ((FrameController) pm.getService("FrameController")).getView();
		if (recovery.emailVerified(username)) { JOptionPane.showMessageDialog(parent, t("security.emailAlreadyVerified")); return; }
		recovery.requestEmailVerification(username);
		JOptionPane.showMessageDialog(parent, t("security.emailVerificationSent"));
		String token = JOptionPane.showInputDialog(parent, t("security.emailVerificationPaste"));
		if (token != null && recovery.verifyEmail(token)) JOptionPane.showMessageDialog(parent, t("security.emailVerificationSuccess"));
		else if (token != null) showError(t("security.emailVerificationDenied"));
	}

	private void rotateRecoveryKey() {
		if (authorization == null || !authorization.currentIdentity().isPresent()) return;
		AccountRecoveryService recovery = pm.getService(SecurityServiceIds.RECOVERY_KEYS).orElse(null);
		if (recovery == null) { showError(t("security.recoveryUnavailable")); return; }
		if (JOptionPane.showConfirmDialog(((FrameController) pm.getService("FrameController")).getView(),
				t("security.recoveryRotateWarning"), t("security.recoveryTitle"), JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) return;
		String key = recovery.rotate(authorization.currentIdentity().get().username());
		javax.swing.JTextArea value = new javax.swing.JTextArea(key, 3, 48);
		value.setEditable(false); value.setLineWrap(true); value.setWrapStyleWord(true);
		JOptionPane.showMessageDialog(((FrameController) pm.getService("FrameController")).getView(),
				new Object[] { t("security.recoveryShownOnce"), new javax.swing.JScrollPane(value) },
				t("security.recoveryTitle"), JOptionPane.INFORMATION_MESSAGE);
		value.setText(""); key = null;
	}

	private void changeProfileLanguage() {
		if (localization == null || authorization == null || !authorization.currentIdentity().isPresent()) return;
		localization.setLocale(view.getProfileLocale());
		ApplicationContextProvider.getContext().getBean(TextMessages.class).setLocale(localization.locale());
		plugins.core.menu.view.Menu menu = (plugins.core.menu.view.Menu) pm.getService("MenuView");
		if (menu != null) menu.refreshTexts();
		outlookBar().refreshTexts();
		outlookBar().setFooterText(localization.messages("i18n.profile").text("profile.action"));
		outlookBar().setLogoutFooterText(localization.messages("i18n.profile").text("profile.logout"));
		UserModel user = Repository.getUserByUsername(authorization.currentIdentity().get().username());
		if (user != null) {
			user.setLocale(localization.locale().toLanguageTag());
			Repository.save(user);
			view.profileView(user);
		}
	}

	public void clearSessionState() { view.clearSessionCredentials(); }

	private void showError(String message) {
		JOptionPane.showMessageDialog(((FrameController) pm.getService("FrameController")).getView(),
				message, t("security.inputCheck"), JOptionPane.WARNING_MESSAGE);
	}

	private String t(String key) { return localization.messages("i18n.security").text(key); }

	private JOutlookBar outlookBar() {
		return ((FrameController) pm.getService("FrameController")).getView().getOutlookBar();
	}

	private String actor() {
		return authorization == null || !authorization.currentIdentity().isPresent()
				? null : authorization.currentIdentity().get().username();
	}
	
	public void setUser(String username){
		this.username = username;
		model = Repository.getUserByUsername(username);
	}
	
	public void createUsersOverview(){
		view.createTableOverview();
	}
	
	public void createAddUserview(){
		view.addUserView();
	}
	
	public void editCustomerView(){
		view.editUserView(model);
	}
	
	public BCryptPasswordEncoder encoder() {
	    return new BCryptPasswordEncoder();
	}
}
