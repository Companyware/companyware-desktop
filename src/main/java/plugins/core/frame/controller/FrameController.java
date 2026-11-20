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
/**
 * 
 */
package plugins.core.frame.controller;

import plugins.core.frame.view.Frame;

import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JComponent;
import java.awt.BorderLayout;

import platform.api.ui.DesktopShell;
import platform.api.ui.UiContributionSpec;
import platform.api.ui.UiContributionType;
import platform.api.ui.UiRegistry;
import pluginmanager.plugininterfaces.Service;

/**
 * @author mbauer
 *
 */
public class FrameController implements Service, DesktopShell {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Frame view;
	private final UiRegistry uiRegistry;
	/**
	 * 
	 */
	public FrameController() {
		this(null);
	}

	public FrameController(UiRegistry uiRegistry) {
		view = new Frame();
		this.uiRegistry = uiRegistry;
		if (uiRegistry != null) {
			uiRegistry.onChange(this::refreshSdkNavigation);
			refreshSdkNavigation();
		}
	}

	private void refreshSdkNavigation() {
		java.util.List<UiContributionSpec> views = uiRegistry.contributions(UiContributionType.VIEW);
		java.util.List<UiContributionSpec> navigation = uiRegistry.contributions(UiContributionType.NAVIGATION);
		view.getOutlookBar().replaceSdkNavigation(navigation, views, this::showView);
	}

	public void refreshTheme() { view.applyTheme(); }
	
	public Frame getView() {
		return view;
	}
	
	public void setView(Frame view) {
		this.view = view;
	}

	@Override
	public void show() {
		view.setVisible(true);
	}

	@Override
	public void showMessage(String message) {
		JPanel bottom = view.getBottomPanel();
		JOptionPane.showMessageDialog(view, message, "Info", JOptionPane.INFORMATION_MESSAGE);
		bottom.setLayout(new FlowLayout(FlowLayout.LEFT));
		bottom.removeAll();
		bottom.add(new JLabel(message));
		bottom.revalidate();
		bottom.repaint();
	}

	@Override public void showView(JComponent component) {
		view.getOutlookBar().selectSdkView(component);
		view.getCenterPanel().removeAll();
		view.getCenterPanel().add(component, BorderLayout.CENTER);
		view.getCenterPanel().revalidate();
		view.getCenterPanel().repaint();
	}

	/** Clears every authenticated-user UI surface without stopping the application runtime. */
	public void clearSessionView() {
		view.getCenterPanel().removeAll();
		view.getBottomPanel().removeAll();
		view.getOutlookBar().clearSessionState();
		view.getCenterPanel().revalidate(); view.getCenterPanel().repaint();
		view.getBottomPanel().revalidate(); view.getBottomPanel().repaint();
	}
}
