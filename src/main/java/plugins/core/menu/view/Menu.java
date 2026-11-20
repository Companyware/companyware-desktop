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
package plugins.core.menu.view;

import java.awt.BorderLayout;
import java.util.Observable;
import java.util.Observer;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import core.ApplicationContextProvider;
import core.TextMessages;
import pluginmanager.plugininterfaces.PluginManager;
import pluginmanager.plugininterfaces.Service;
import plugins.core.frame.controller.FrameController;
import plugins.core.menu.controller.MenuController;
import platform.api.i18n.LocalizationServiceIds;
import platform.api.ui.PlatformServiceIds;
import platform.api.ui.UiContributionSpec;
import platform.api.ui.UiContributionType;
import platform.api.ui.UiRegistry;

public class Menu  implements Observer,Service {

	private JMenuBar menubar;
	private JMenu file;
	private JMenu settings;
	private JMenu help;
	private JMenuItem open;
	private JMenuItem finish;
	private JMenuItem logout;
	private JMenuItem faq;
	private JMenuItem about;
	private final Map<JMenuItem, String> localizedPluginItems = new LinkedHashMap<JMenuItem, String>();
	private final Map<String, JMenuItem> contributedItems = new LinkedHashMap<String, JMenuItem>();
	private final PluginManager pm;
	private static final Log log = LogFactory.getLog(Menu.class);

	public Menu(PluginManager pm) {
		this.pm = pm;
		MenuController menuController = (MenuController)pm.getService("MenuController");
		FrameController frameController = (FrameController)pm.getService("FrameController");
		pm.registerService("MenuView", this);
        menubar = new JMenuBar();
        
        TextMessages service = ApplicationContextProvider.getContext().getBean(TextMessages.class);
  
        file = new JMenu(service.get("menu.file"));
        settings = new JMenu(service.get("menu.settings"));
        help = new JMenu(service.get("menu.help"));
        
        finish = new JMenuItem(service.get("menu.exit"));
        finish.setActionCommand("exit");
        finish.addActionListener(menuController);
        logout = new JMenuItem(service.get("menu.logout"));
        logout.setActionCommand("logout");
        logout.addActionListener(menuController);
      
        about = new JMenuItem(service.get("menu.about"));
        about.addActionListener(menuController);
        
        menubar.add(file);
        menubar.add(settings);
        menubar.add(help);
        
        file.add(logout);
        file.addSeparator();
        file.add(finish);
        help.add(about);
        
        frameController.getView().add(menubar,BorderLayout.NORTH);
		UiRegistry registry = pm.getService(PlatformServiceIds.UI_REGISTRY).get();
		registry.onChange(() -> synchronizeContributions());
		pm.getService(LocalizationServiceIds.LOCALIZATION).get().onChange(() -> refreshTexts());
		synchronizeContributions();
	}

	private void synchronizeContributions() {
		UiRegistry registry = pm.getService(PlatformServiceIds.UI_REGISTRY).get();
		Set<String> active = new HashSet<String>();
		for (UiContributionSpec contribution : registry.contributions(UiContributionType.MENU)) {
			String key = contribution.owner().value() + ":" + contribution.id();
			active.add(key);
			JMenuItem item = contributedItems.get(key);
			if (item == null) {
				item = new JMenuItem(contribution.title());
				item.addActionListener(event -> contribution.execute());
				menu(contribution.target()).add(item);
				contributedItems.put(key, item);
			} else item.setText(contribution.title());
		}
		for (String key : new HashSet<String>(contributedItems.keySet())) {
			if (active.contains(key)) continue;
			JMenuItem item = contributedItems.remove(key);
			if (item != null && item.getParent() != null) item.getParent().remove(item);
		}
		menubar.revalidate();
		menubar.repaint();
	}

	private JMenu menu(String path) {
		if ("file".equalsIgnoreCase(path)) return file;
		if ("help".equalsIgnoreCase(path)) return help;
		return settings;
	}

	public JMenu getSettings() {
		return settings;
	}

	public void refreshTexts() {
		TextMessages service = ApplicationContextProvider.getContext().getBean(TextMessages.class);
		file.setText(service.get("menu.file"));
		settings.setText(service.get("menu.settings"));
		help.setText(service.get("menu.help"));
		finish.setText(service.get("menu.exit"));
		logout.setText(service.get("menu.logout"));
		about.setText(service.get("menu.about"));
		for (Map.Entry<JMenuItem, String> item : localizedPluginItems.entrySet()) {
			item.getKey().setText(service.get(item.getValue()));
		}
		for (UiContributionSpec contribution : pm.getService(PlatformServiceIds.UI_REGISTRY).get()
				.contributions(UiContributionType.MENU)) {
			JMenuItem item = contributedItems.get(contribution.owner().value() + ":" + contribution.id());
			if (item != null) item.setText(contribution.title());
		}
		menubar.revalidate(); menubar.repaint();
	}

	/** Registers a legacy plugin menu item for immediate runtime translation updates. */
	public void addLocalizedItem(JMenu parent, JMenuItem item, String messageKey) {
		localizedPluginItems.put(item, messageKey);
		parent.add(item);
	}

	public void setSettings(JMenu settings) {
		this.settings = settings;
	}

	public JMenuBar getMenubar() {
		return menubar;
	}

	public void setMenubar(JMenuBar menubar) {
		this.menubar = menubar;
	}

	public JMenu getFile() {
		return file;
	}

	public void setFile(JMenu file) {
		this.file = file;
	}

	public JMenu getHelp() {
		return help;
	}

	public void setHelp(JMenu help) {
		this.help = help;
	}

	public JMenuItem getOpen() {
		return open;
	}

	public void setOpen(JMenuItem open) {
		this.open = open;
	}

	public JMenuItem getFinish() {
		return finish;
	}

	public void setFinish(JMenuItem finish) {
		this.finish = finish;
	}

	public JMenuItem getFaq() {
		return faq;
	}

	public void setFaq(JMenuItem faq) {
		this.faq = faq;
	}

	public JMenuItem getAbout() {
		return about;
	}

	public void setAbout(JMenuItem about) {
		this.about = about;
	}

	@Override
	public void update(Observable arg0, Object arg1) {
		// TODO Auto-generated method stub	
	}
}
