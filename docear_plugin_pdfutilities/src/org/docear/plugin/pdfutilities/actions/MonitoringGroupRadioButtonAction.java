package org.docear.plugin.pdfutilities.actions;

import java.awt.event.ActionEvent;

import org.docear.plugin.core.util.NodeUtilities;
import org.docear.plugin.pdfutilities.util.MonitoringUtils;
import org.freeplane.features.map.INodeSelectionListener;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;

public class MonitoringGroupRadioButtonAction extends GroupRadioButtonAction implements INodeSelectionListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String attributeKey;
	private Object value;
	
	

	public MonitoringGroupRadioButtonAction(String key, String attributeKey, Object value, ModeController modeController) {
		super(key);
		this.attributeKey = attributeKey;
		this.value = value;
		modeController.getMapController().addNodeSelectionListener(this);
	}	
	
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		NodeModel selected = Controller.getCurrentController().getSelection().getSelected();
		if(MonitoringUtils.isMonitoringNode(selected)){
			NodeUtilities.setAttribute(selected, this.attributeKey, this.value);
		}
	}

	public void onDeselect(NodeModel node) {
	}

	public void onSelect(NodeModel node) {
		this.setSelected(false);
		if(MonitoringUtils.isMonitoringNode(node)){
			Object value = NodeUtilities.getAttributeValue(node, this.attributeKey);
			if(value != null && value.equals(this.value)){
				this.setSelected(true);
			}
		}
	}

}
