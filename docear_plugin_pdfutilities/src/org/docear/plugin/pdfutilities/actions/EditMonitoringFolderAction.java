package org.docear.plugin.pdfutilities.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;

import org.docear.plugin.core.DocearController;
import org.docear.plugin.core.logger.DocearLogEvent;
import org.docear.plugin.core.util.NodeUtilities;
import org.docear.plugin.core.workspace.AVirtualDirectory;
import org.docear.plugin.pdfutilities.PdfUtilitiesController;
import org.docear.plugin.pdfutilities.util.MonitoringUtils;
import org.freeplane.core.ui.EnabledAction;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.link.mindmapmode.MLinkController;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;

@EnabledAction(checkOnNodeChange=true)
public class EditMonitoringFolderAction extends AbstractMonitoringAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String KEY = "EditMonitoringFolderAction";

	public EditMonitoringFolderAction() {
		super(KEY);		
	}

	public void actionPerformed(ActionEvent e) {
		JFileChooser fileChooser;
		File value = MonitoringUtils.getPdfDirFromMonitoringNode((Controller.getCurrentController().getSelection().getSelected()));
		if(value != null && !(value instanceof AVirtualDirectory)){
			fileChooser = new JFileChooser(value);
		}
		else{
			fileChooser = new JFileChooser();
		}		
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setDialogTitle(TextUtils.getText("AddMonitoringFolderAction_dialog_title")); //$NON-NLS-1$
		int result = fileChooser.showOpenDialog(Controller.getCurrentController().getViewController().getJFrame());
        if(result == JFileChooser.APPROVE_OPTION){
        	File f = fileChooser.getSelectedFile();
        	URI pdfDir = MLinkController.toLinkTypeDependantURI(Controller.getCurrentController().getMap().getFile(), f);
        	fileChooser.setDialogTitle(TextUtils.getText("AddMonitoringFolderAction_dialog_title_mindmaps")); //$NON-NLS-1$
        	result = fileChooser.showOpenDialog(Controller.getCurrentController().getViewController().getJFrame());
        	if(result == JFileChooser.APPROVE_OPTION){
        		URI mindmapDir = MLinkController.toLinkTypeDependantURI(Controller.getCurrentController().getMap().getFile(), fileChooser.getSelectedFile());
        		NodeModel selected = Controller.getCurrentController().getSelection().getSelected();
        		NodeUtilities.setAttributeValue(selected, PdfUtilitiesController.MON_INCOMING_FOLDER, pdfDir);
        		NodeUtilities.setAttributeValue(selected, PdfUtilitiesController.MON_MINDMAP_FOLDER, mindmapDir);
        		List<NodeModel> list = new ArrayList<NodeModel>();
        		list.add(Controller.getCurrentController().getSelection().getSelected());
        		
        		DocearController.getController().getDocearEventLogger().appendToLog(this, DocearLogEvent.MONITORING_FOLDER_EDIT, f);
        		AddMonitoringFolderAction.updateNodesAgainstMonitoringDir(list, true);
        	}
        }
	}

	@Override
	public void setEnabled() {
		IMapSelection selection = Controller.getCurrentController().getSelection();
		if(selection != null) {
			NodeModel selected = selection.getSelected();
			if(selected != null){
				this.setEnabled(MonitoringUtils.isMonitoringNode(selected));
			}
		}
		this.setEnabled(false);
	}

}
