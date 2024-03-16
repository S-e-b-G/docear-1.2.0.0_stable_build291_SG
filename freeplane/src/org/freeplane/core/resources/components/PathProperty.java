/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is modified by Dimitry Polivaev in 2008.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.core.resources.components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.util.Arrays;
import java.util.Locale;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.MenuBuilder;
import org.freeplane.core.util.FileUtils;
import org.freeplane.core.util.TextUtils;

import com.jgoodies.forms.builder.DefaultFormBuilder;

/**
 * A PathProperty has an absolute path as a model. It's visual representation
 * is a text field containing the path and a 'browse' button.
 * It's configuration has (non-mandatory) options:
 *  - dir [true|false]: if the configured path is a directory
 *  - suffixes: a comma-separated string containing allowed suffixes (without the dot)
 */
public class PathProperty extends PropertyBean implements IPropertyControl {
	private class SelectFileAction implements ActionListener {
		public void actionPerformed(final ActionEvent e) {
			final Object source = e.getSource();
			if (source == selectButton) {
				final JFileChooser chooser = createFileChooser();
				int result = chooser.showOpenDialog(chooser);
				if (result == JFileChooser.APPROVE_OPTION)
					setValue(chooser.getSelectedFile().getAbsolutePath());
			}
		}
	}

	final private boolean isDir;
	final private boolean showHidden;
	final private String[] suffixes;	
	private String path;
	private JTextField filenameField;
	private JButton selectButton;
	private JLabel nameLabel;

	public PathProperty(final String name, final boolean isDir, final boolean showHidden, final String[] suffixes) {
		super(name);
		this.isDir = isDir;
		this.showHidden = showHidden;
		this.suffixes = suffixes;
	}

	private JFileChooser createFileChooser() {
		final JFileChooser fileChooser = new JFileChooser();
		if (path != null) {
			fileChooser.setSelectedFile(new File(path));
		}
		FileFilter filter = null;
		if (showHidden) {
			fileChooser.setFileHidingEnabled(!showHidden);
		}
		if (isDir) {
		    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		}
		else if (suffixes != null && suffixes.length > 0) {
			
			filter = new FileFilter() {
				@Override
				public String getDescription() {
					String result = "";
					for(String s : Arrays.asList(suffixes)){
						if(result.length() <= 0){
							result = "*." + s.toLowerCase(Locale.ENGLISH);
						}
						else{
							result += "; *." + s.toLowerCase(Locale.ENGLISH);
						}
					}
					return result;
				}

				@Override
				public boolean accept(File f) {
					if(f == null) return false;
					if(f.isDirectory()) return true;
					String extension = FileUtils.getExtension(f);
					for (String suffix : suffixes) {
						if (suffix.equalsIgnoreCase(extension))
							return true;
					}
					return false;
				}
			};
		}
		fileChooser.setFileFilter(filter);
		fileChooser.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent pE) {
				firePropertyChangeEvent();
			}
		});
		return fileChooser;
	}

	@Override
	public String getValue() {
		return path;
	}

	public void layout(final DefaultFormBuilder builder) {
		final Box box = Box.createHorizontalBox();
		box.setBorder(new EmptyBorder(5, 0, 5, 0));
		filenameField = new JTextField();
		filenameField.setText(path);
		filenameField.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
				final String text = filenameField.getText();
				if (text == null || text.length() == 0) {
					filenameField.setText(path);
					JOptionPane.showConfirmDialog(e.getComponent(), TextUtils.getText("OptionPanel.path_property_may_not_be_empty"), "", JOptionPane.WARNING_MESSAGE);
				}
				else {
					path = text;
				}
			}
			public void focusGained(FocusEvent e) {
			}
		});
		box.add(filenameField);
		box.add(Box.createHorizontalStrut(3));
		selectButton = new JButton();
		MenuBuilder.setLabelAndMnemonic(selectButton, TextUtils.getText("browse"));
		selectButton.addActionListener(new SelectFileAction());
		selectButton.setMaximumSize(new Dimension(1000, 1000));
		box.add(selectButton);
		this.nameLabel = layout(builder, box);
	}

	public void setEnabled(final boolean enabled) {
		if (selectButton != null)
			selectButton.setEnabled(enabled);
		if (filenameField != null)
			filenameField.setEnabled(enabled);
		if (nameLabel != null)
			nameLabel.setEnabled(enabled);
	}

	@Override
	public void setValue(String value) {
		if (value != null) {
			value = value.replace("{freeplaneuserdir}", ResourceController.getResourceController()
			    .getFreeplaneUserDirectory());
			this.path = value;
		}
		else {
			this.path = null;
		}
		if (filenameField != null)
			filenameField.setText(path);
	}

	@Override
    protected Component[] getComponents() {
	    return new Component[]{filenameField, selectButton};
    }
}
