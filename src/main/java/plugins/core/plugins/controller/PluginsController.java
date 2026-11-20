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
package plugins.core.plugins.controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import models.plugin.PluginModel;
import models.plugin.Repository;
import plugins.core.plugins.model.*;
import pluginmanager.plugininterfaces.PluginManager;
import pluginmanager.plugininterfaces.Service;
import pluginmanager.packaging.PluginPackageInspection;
import plugins.core.plugins.view.Plugins;
import platform.api.security.AuditEvent;
import platform.api.security.AuditSink;
import platform.api.security.AuthorizationService;
import platform.api.security.SecurityServiceIds;

public class PluginsController implements ActionListener, Service{

	private static final Log log = LogFactory.getLog(PluginsController.class);
	private PluginsModel model;
	private Plugins view;
	private String name;
	private final PluginManager pluginManager;
	private final AuditSink audit;
	private final AuthorizationService authorization;

	public PluginsController(PluginManager pm) {
		this.pluginManager = pm;
		this.model = new PluginsModel();
		pm.registerService("PluginsController",this);
		view = new Plugins(pm);
		model.addObserver(view);
		audit = pm.getService(SecurityServiceIds.AUDIT).orElse(null);
		authorization = pm.getService(SecurityServiceIds.AUTHORIZATION).orElse(null);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		log.info("test action pluginscontroller");
		this.model.printOut("ACTION: " + e.getActionCommand().toString());
		switch(e.getActionCommand().toString()) {
		case "Pluginmanager":
		{
			this.createPluginManagerOverview();
			break;
		}
		case "installPlugin":
		{
			installPluginPackage();
			break;
		}
		}
	}

	private void installPluginPackage() {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Companyware-Pluginpaket auswählen (.cwp)");
		if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;
		int trust = JOptionPane.showConfirmDialog(null,
				"Installiere dieses lokale Paket nur, wenn du Quelle und Ersteller vertraust.\n"
				+ "Das Paket wird vollständig geprüft; eine digitale Signatur ist noch nicht erforderlich.\n\n"
				+ chooser.getSelectedFile().getAbsolutePath(),
				"Lokales Pluginpaket vertrauen?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		if (trust != JOptionPane.OK_OPTION) return;
		try {
			PluginPackageInspection installed = pluginManager.installTrustedLocal(chooser.getSelectedFile().toPath());
			if (audit != null) audit.record(new AuditEvent("plugin-install", actor(),
					"success:" + installed.getDescriptor().getId().value()));
			createPluginManagerOverview();
			JOptionPane.showMessageDialog(null,
					"Plugin " + installed.getDescriptor().getName() + " "
					+ installed.getDescriptor().getVersion() + " wurde sicher installiert.\n"
					+ "SHA-256: " + installed.getSha256() + "\n\n"
					+ "Das Plugin wurde live erkannt und kann sofort aktiviert werden.",
					"Plugin installiert", JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception failure) {
			if (audit != null) audit.record(new AuditEvent("plugin-install", actor(), "denied"));
			log.error("Plugin package rejected", failure);
			JOptionPane.showMessageDialog(null, failure.getMessage(), "Pluginpaket abgelehnt",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private String actor() {
		return authorization == null || !authorization.currentIdentity().isPresent()
				? null : authorization.currentIdentity().get().username();
	}

	public boolean isJarFile(){
		URL root = getClass().getResource("/");
		return root != null && "jar".equalsIgnoreCase(root.getProtocol());
	}

	public void createPluginManagerOverview(){
		view.createTableOverview();
	}
	
	public void setPlugin(String name){
		this.name = name;
	}
	
	public PluginModel getPluginByName(){
		PluginModel pluginModel = Repository.getPluginByName(this.name);
		return pluginModel;
	}
	
	public void deletePlugin(){
		int confirmation = JOptionPane.showConfirmDialog(null,
				"Plugin " + this.name + " deinstallieren?\n"
				+ "Das Plugin muss deaktiviert sein. Seine Datenbanktabellen bleiben erhalten.",
				"Plugin deinstallieren", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		if (confirmation != JOptionPane.OK_OPTION) return;
		try {
			pluginManager.uninstallPlugin(this.name);
			if (audit != null) audit.record(new AuditEvent("plugin-uninstall", actor(), "success:" + this.name));
			view.createTableOverview();
		} catch (Exception failure) {
			if (audit != null) audit.record(new AuditEvent("plugin-uninstall", actor(), "denied:" + this.name));
			log.error("Unable to uninstall plugin " + this.name, failure);
			JOptionPane.showMessageDialog(null, failure.getMessage(), "Plugin kann nicht deinstalliert werden",
					JOptionPane.ERROR_MESSAGE);
		}
	}
}
