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
package plugins.core.login.controller;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import core.Companyware;
import core.ApplicationContextProvider;
import core.ApplicationShutdown;
import core.TextMessages;
import core.security.PasswordPolicy;
import models.user.UserModel;
import platform.api.ui.LoginPrompt;
import platform.api.security.AuthenticationResult;
import platform.api.security.AuthenticationService;
import platform.api.security.AuthorizationService;
import platform.api.security.MutableSecretProvider;
import platform.api.security.AccountRecoveryService;
import platform.api.security.RecoveryResult;
import platform.api.security.SecurityServiceIds;
import platform.api.i18n.LocalizationService;
import platform.api.i18n.LocalizationServiceIds;
import platform.api.ui.PlatformServiceIds;
import pluginmanager.plugininterfaces.PluginManager;
import pluginmanager.plugininterfaces.Service;
import plugins.core.frame.controller.FrameController;
import plugins.core.frame.view.Frame;
import plugins.core.login.view.LoginView;

public class LoginController implements ActionListener, Service, LoginPrompt, WindowListener, KeyListener{
	private static final Log log = LogFactory.getLog(LoginController.class);
	private LoginView view; 
	private FrameController frameController;
	private PluginManager pm;
	private AuthenticationService authentication;
	private AuthorizationService authorization;
	private LocalizationService localization;
	/**
	 * 
	 * 
	 */
	public LoginController(PluginManager pm) {
		pm.registerService("LoginController",this);
		frameController = (FrameController)pm.getService("FrameController");
		JFrame frame =  frameController.getView();
		this.view = new LoginView(pm, frame);
		this.view.setContent();
		this.pm = pm;
		this.authentication = pm.getService(SecurityServiceIds.AUTHENTICATION).orElse(null);
		this.authorization = pm.getService(SecurityServiceIds.AUTHORIZATION).orElse(null);
		this.localization = pm.getService(LocalizationServiceIds.LOCALIZATION).orElse(null);
		pm.getService(PlatformServiceIds.THEME_REGISTRY).ifPresent(
				themes -> themes.onChange(view::setContent));
	}
	public LoginView getView() {
		return view;
	}
	public void setView(LoginView view) {
		this.view = view;
	}
	@Override
	public void show() {
		view.setVisible(true);
	}
	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
		Frame frame =  frameController.getView();
		frame.setVisible(false);
		frame.dispose();
		ApplicationShutdown.request();
	}
	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void windowOpened(WindowEvent e) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				view.focusUsername();
			}
		});
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand().toString();
		if (command.equals("language") && !view.isRebuilding()) {
			view.markLanguageSelectedByUser();
			localization.setLocale(view.getSelectedLocale());
			ApplicationContextProvider.getContext().getBean(TextMessages.class).setLocale(localization.locale());
			refreshShellTexts();
			view.setContent();
			view.focusUsername();
			return;
		}
		if(command.equals("Login")){
			String userName = this.view.getUserTextField().getText();
			char[] password = this.view.getPasswordField().getPassword();
			boolean checkUser = this.login(userName, password);
			if(checkUser){
				activateUserLocale(userName);
				this.view.setVisible(false);
				this.view.dispose();
			}
			else{
				this.setFailureMessage();
			}
		}
		if (command.equals("recovery")) {
			showRecovery();
			return;
		}
		if(command.equals("Abbrechen")){
			Frame frame =  frameController.getView();
			frame.setVisible(false);
			frame.dispose();
			ApplicationShutdown.request();
		}
		if(command.equals("showPassword")){
			if(this.view.getShowPassword().isSelected()){
				this.view.getPasswordField().setEchoChar((char) 0);
			}
			else{
				this.view.getPasswordField().setEchoChar('*');
			}
		}
	}

	public boolean checkUser(String userName, String password){
		if (authentication != null) return login(userName, password.toCharArray());
		try {
			UserModel user = models.user.Repository.getActiveUserByUsername(userName);
			if(user == null){
				return false;
			}
			if(Companyware.getContainer().encoder().matches(password, user.getPassword())){
				return true;
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return false;
	}

	private boolean login(String userName, char[] password) {
		if (authentication == null) return checkUser(userName, String.valueOf(password));
		AuthenticationResult result = authentication.authenticate(userName, password);
		if (result.status() == AuthenticationResult.Status.CREDENTIAL_CHANGE_REQUIRED) {
			return requirePasswordChange(userName);
		}
		return result.isAuthenticated();
	}

	private boolean requirePasswordChange(String username) {
		while (true) {
			JPasswordField current = new JPasswordField();
			JPasswordField replacement = new JPasswordField();
			JPasswordField confirmation = new JPasswordField();
			JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
			panel.add(new JLabel(t("security.currentPassword"))); panel.add(current);
			panel.add(new JLabel(t("security.newPassword"))); panel.add(replacement);
			panel.add(new JLabel(t("security.confirmPassword"))); panel.add(confirmation);
			int choice = JOptionPane.showConfirmDialog(view, panel,
					t("security.changeRequired"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
			if (choice != JOptionPane.OK_OPTION) return false;
			char[] newPassword = replacement.getPassword();
			char[] repeated = confirmation.getPassword();
			char[] currentPassword = current.getPassword();
			if (!java.util.Arrays.equals(newPassword, repeated)) {
				clear(currentPassword, newPassword, repeated);
				JOptionPane.showMessageDialog(view, t("security.passwordMismatch"));
				continue;
			}
			if (!PasswordPolicy.accepts(newPassword)) {
				clear(currentPassword, newPassword, repeated);
				JOptionPane.showMessageDialog(view, t("security.passwordPolicy"));
				continue;
			}
			if (java.util.Arrays.equals(currentPassword, newPassword)) {
				clear(currentPassword, newPassword, repeated);
				JOptionPane.showMessageDialog(view, t("security.passwordDifferent"));
				continue;
			}
			java.util.Arrays.fill(repeated, '\0');
			AuthenticationResult changed = authentication.changePassword(username, currentPassword, newPassword);
			if (changed.isAuthenticated()) return true;
			JOptionPane.showMessageDialog(view, t("security.passwordFailed"));
		}
	}

	private static void clear(char[]... values) {
		for (char[] value : values) if (value != null) java.util.Arrays.fill(value, '\0');
	}

	/** One logout path shared by the File menu and the persistent footer action. */
	public void logoutAndShowLogin() {
		int choice = JOptionPane.showConfirmDialog(frameController.getView(), t("security.logoutWarning"),
				t("security.logoutTitle"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		if (choice != JOptionPane.OK_OPTION) return;
		frameController.getView().getOutlookBar().setNavigationEnabled(false);
		plugins.core.users.controller.UsersController users = (plugins.core.users.controller.UsersController) pm.getService("UsersController");
		if (users != null) users.clearSessionState();
		frameController.clearSessionView();
		pm.getService(SecurityServiceIds.MUTABLE_SECRETS).ifPresent(MutableSecretProvider::clearSessionValues);
		if (authentication != null) authentication.logout();
		LoginView previous = view;
		view = new LoginView(pm, frameController.getView());
		view.setContent();
		if (previous != null) { previous.clearCredentials(); previous.setVisible(false); previous.dispose(); }
		try { view.setVisible(true); }
		finally { frameController.getView().getOutlookBar().setNavigationEnabled(
				authorization != null && authorization.currentIdentity().isPresent()); }
	}

	private void showRecovery() {
		AccountRecoveryService recovery = pm.getService(SecurityServiceIds.RECOVERY_KEYS).orElse(null);
		if (recovery == null) { JOptionPane.showMessageDialog(view, t("security.recoveryUnavailable")); return; }
		Object[] choices = { t("security.recoveryByEmail"), t("security.recoveryByKey") };
		int mode = JOptionPane.showOptionDialog(view, t("security.recoveryChoose"), t("security.recoveryTitle"),
				JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, choices, choices[0]);
		if (mode < 0) return;
		if (mode == 0) {
			String email = JOptionPane.showInputDialog(view, t("security.recoveryEmail"));
			if (email != null) {
				recovery.requestEmailRecovery(email);
				JOptionPane.showMessageDialog(view, t("security.recoveryEmailNeutral"));
			}
			return;
		}
		javax.swing.JTextField key = new javax.swing.JTextField();
		JPasswordField password = new JPasswordField();
		JPasswordField confirmation = new JPasswordField();
		JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
		panel.add(new JLabel(t("security.recoveryNeutralHelp")));
		panel.add(new JLabel(t("security.recoveryKeyOrLink"))); panel.add(key);
		panel.add(new JLabel(t("security.newPassword"))); panel.add(password);
		panel.add(new JLabel(t("security.confirmPassword"))); panel.add(confirmation);
		if (JOptionPane.showConfirmDialog(view, panel, t("security.recoveryTitle"),
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
		char[] replacement = password.getPassword(); char[] repeated = confirmation.getPassword();
		try {
			if (!java.util.Arrays.equals(replacement, repeated) || !PasswordPolicy.accepts(replacement)) {
				JOptionPane.showMessageDialog(view, t("security.recoveryDenied")); return;
			}
			RecoveryResult result = recovery.reset(key.getText(), replacement);
			replacement = null;
			if (!result.isSuccess()) { JOptionPane.showMessageDialog(view, t("security.recoveryDenied")); return; }
			view.getUserTextField().setText(result.username());
			JOptionPane.showMessageDialog(view, t("security.recoverySuccess"));
		} finally { if (replacement != null) java.util.Arrays.fill(replacement, '\0'); java.util.Arrays.fill(repeated, '\0'); key.setText(""); }
	}
	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
	}
	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			String userName = this.view.getUserTextField().getText();
			char[] password = this.view.getPasswordField().getPassword();
			boolean checkUser = this.login(userName, password);
			if(checkUser){
				activateUserLocale(userName);
				this.view.setVisible(false);
				this.view.dispose();
			}
			else{
				this.setFailureMessage();
			}
		}
	}

	private void activateUserLocale(String username) {
		if (localization == null) return;
		UserModel user = models.user.Repository.getUserByUsername(username);
		if (user != null) {
			if (!view.isLanguageSelectedByUser() && user.getLocale() != null && !user.getLocale().trim().isEmpty()) {
				localization.setLocale(java.util.Locale.forLanguageTag(user.getLocale()));
				ApplicationContextProvider.getContext().getBean(TextMessages.class).setLocale(localization.locale());
			} else {
				user.setLocale(localization.locale().toLanguageTag());
				models.user.Repository.save(user);
			}
			refreshShellTexts();
		}
	}

	private void refreshShellTexts() {
		plugins.core.menu.view.Menu menu = (plugins.core.menu.view.Menu) pm.getService("MenuView");
		if (menu != null) menu.refreshTexts();
		Frame frame = frameController.getView();
		if (frame != null && frame.getOutlookBar() != null) {
			frame.getOutlookBar().refreshTexts();
			frame.getOutlookBar().setFooterText(
					localization.messages("i18n.profile").text("profile.action"));
			frame.getOutlookBar().setLogoutFooterText(
					localization.messages("i18n.profile").text("profile.logout"));
		}
	}
	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
	}
	
	public void setFailureMessage(){
		FrameController frameController = (FrameController)pm.getService("FrameController");
		JPanel bottom = frameController.getView().getBottomPanel();
		String message = t("security.loginFailed");
	    JLabel labelBottom = new JLabel(message);
	    JOptionPane.showMessageDialog(frameController.getView(), message, "Info", JOptionPane.INFORMATION_MESSAGE);
		bottom.setLayout(new FlowLayout(FlowLayout.LEFT));
		bottom.removeAll();
		bottom.add(labelBottom);
		bottom.revalidate();
		bottom.repaint();
	}

	private String t(String key) { return localization.messages("i18n.security").text(key); }
}
