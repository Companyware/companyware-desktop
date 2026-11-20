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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import pluginmanager.plugininterfaces.PluginManager;
import plugins.core.users.view.WhiteButton;
import platform.api.ui.CompanywareIcons;

public class ImageButtonTableEditor extends DefaultCellEditor{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Log log = LogFactory.getLog(ImageButtonTableEditor.class);
	
	private JButton button;
	private String label;
	private boolean clicked;
	private int row, col;
	private JTable table;
	private PluginManager pm;
	private Icon icon;

	public ImageButtonTableEditor(JCheckBox checkBox, PluginManager pm)
	{
		super(checkBox);
		this.pm = pm;
		button = new WhiteButton();
		this.icon = CompanywareIcons.DELETE;
    	button.setIcon(this.icon);
		button.setActionCommand("button renderer");
		button.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				button.setForeground(table.getForeground());
				button.setBackground(table.getBackground());
				log.info("delete action");
				log.info("ACTION: " + e.getActionCommand().toString());
				fireEditingStopped();
			}
		});
	}
	public JButton getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
	{
		this.table = table;
		this.row = row;
		this.col = column;
		
		label = (value == null) ? "" : value.toString();
		log.info("label is set, but icon should");
	//	button.setText(label);
		button.setIcon(this.icon);
		button.setBackground(table.getBackground());
		clicked = true;
		return button;
	}
	public Object getCellEditorValue()
	{
		if (clicked)
		{
			log.info(row);
			log.info(col);
			
			log.info("user edit");
	//		JOptionPane.showInputDialog(button, "Column with Value: "+table.getValueAt(row, 0) + " -  Clicked!");

		}
		clicked = false;
		log.info(label);
		return label;
	}

	public boolean stopCellEditing()
	{
		clicked = false;
		return super.stopCellEditing();
	}

	protected void fireEditingStopped()
	{
		super.fireEditingStopped();
	}
}
