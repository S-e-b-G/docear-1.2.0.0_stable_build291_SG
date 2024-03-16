/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Dimitry Polivaev
 *
 *  This file author is Dimitry Polivaev
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;

import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.mode.Controller;

import com.jgoodies.forms.builder.DefaultFormBuilder;

/**
 * @author Stefan Langer
 */
public class ActionProperty extends PropertyBean implements IPropertyControl {
	private Icon icon;
	private String labelText;
	private String actionCommand;
	JButton mButton = new JButton();	
	
	/**
	 */
	public ActionProperty(final String name, final String actionCommand) {
		super(name);
		this.actionCommand = actionCommand;
	}

	@Override
	public String getValue() {
		return mButton.getText();
	}

	public void layout(final DefaultFormBuilder builder) {
		mButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				AFreeplaneAction action = Controller.getCurrentController().getAction(getName());
				if(action == null) {
					return;
				}
				action.actionPerformed(event);		
			}
		});
		mButton.setActionCommand(this.actionCommand);
		if (labelText == null) {
			labelText = TextUtils.getOptionalText(getLabel());
		}
		if (icon != null) {
			mButton.setIcon(icon);
		}
		mButton.setText(labelText);
		mButton.setEnabled(true);		
		
		builder.append("", mButton);
	}

	public void setEnabled(final boolean pEnabled) {
		mButton.setEnabled(pEnabled);
	}

	public void setImageIcon(final Icon icon) {
		this.icon = icon;
	}

	public void setLabelText(final String labelText) {
		this.labelText = labelText;
	}
	
	public Component getActionComponent() {
		return mButton;
	}

	@Override
	public void setValue(final String value) {
	}

	@Override
    protected Component[] getComponents() {
	    return new Component[]{mButton};
    }
}
