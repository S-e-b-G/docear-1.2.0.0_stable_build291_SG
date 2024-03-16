package org.docear.plugin.pdfutilities.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import javax.swing.SwingUtilities;

import org.docear.plugin.core.ui.SwingWorkerDialog;
import org.docear.plugin.core.ui.SwingWorkerDialogLite;
import org.docear.plugin.core.util.NodeUtilities;
import org.docear.plugin.pdfutilities.PdfUtilitiesController;
import org.docear.plugin.pdfutilities.features.DocearNodeMonitoringExtension.DocearExtensionKey;
import org.docear.plugin.pdfutilities.features.DocearNodeMonitoringExtensionController;
import org.docear.plugin.pdfutilities.util.MonitoringUtils;
import org.freeplane.core.ui.EnabledAction;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.mode.Controller;
import org.freeplane.plugin.workspace.URIUtils;
import org.jdesktop.swingworker.SwingWorker;

@EnabledAction(checkOnNodeChange=true)
public class MonitoringFlattenSubfoldersAction extends DocearAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String KEY = "MonitoringFlattenSubfoldersAction";
	
	public MonitoringFlattenSubfoldersAction() {
		super(KEY);		
	}

	public void actionPerformed(ActionEvent arg0) {
		NodeModel selected = Controller.getCurrentController().getSelection().getSelected();
		int value = NodeUtilities.getAttributeIntValue(selected, PdfUtilitiesController.MON_FLATTEN_DIRS);
		if(value == 0){			
			boolean isFolded = selected.isFolded();
			selected.setFolded(true);
			SwingWorker<Void, Void> thread = getFlattenThread(selected);
			SwingWorkerDialogLite dialog = new SwingWorkerDialogLite(Controller.getCurrentController().getViewController().getFrame());
			dialog.setHeadlineText(TextUtils.getText("MonitoringFlattenSubfoldersAction.0")); //$NON-NLS-1$
			dialog.showDialog(thread);
			NodeUtilities.setAttributeValue(selected, PdfUtilitiesController.MON_FLATTEN_DIRS, 1);				
			selected.setFolded(isFolded);
		}
		else{			
			boolean isFolded = selected.isFolded();
			selected.setFolded(true);
			SwingWorker<Void, Void> thread = getUnFlattenThread(selected);
			SwingWorkerDialogLite dialog = new SwingWorkerDialogLite(Controller.getCurrentController().getViewController().getFrame());
			dialog.setHeadlineText(TextUtils.getText("MonitoringFlattenSubfoldersAction.1")); //$NON-NLS-1$
			dialog.showDialog(thread);
			NodeUtilities.setAttributeValue(selected, PdfUtilitiesController.MON_FLATTEN_DIRS, 0);
			selected.setFolded(isFolded);			
		}		
	}
	
		

	private SwingWorker<Void, Void> getFlattenThread(final NodeModel selected) {
		return new SwingWorker<Void, Void>(){

			@Override
			protected Void doInBackground() throws Exception {
				fireStatusUpdate(SwingWorkerDialog.SET_PROGRESS_BAR_INDETERMINATE, null, null);				
				flattenMonitorNodes(selected, selected.getChildren());				
				removePathNodes(selected);				
				return null;
			}
			
			@Override
		    protected void done() {			
				if(this.isCancelled() || Thread.currentThread().isInterrupted()){					
					this.firePropertyChange(SwingWorkerDialog.IS_DONE, null, TextUtils.getText("MonitoringFlattenSubfoldersAction.2")); //$NON-NLS-1$
				}
				else{
					this.firePropertyChange(SwingWorkerDialog.IS_DONE, null, TextUtils.getText("MonitoringFlattenSubfoldersAction.3")); //$NON-NLS-1$
				}
				
			}			
		};		
	}
	
	private SwingWorker<Void, Void> getUnFlattenThread(final NodeModel selected) {
		return new SwingWorker<Void, Void>(){

			@Override
			protected Void doInBackground() throws Exception {
				fireStatusUpdate(SwingWorkerDialog.SET_PROGRESS_BAR_INDETERMINATE, null, null);				
				Map<NodeModel, Stack<File>> result = new HashMap<NodeModel, Stack<File>>();
				for(NodeModel node : selected.getChildren()){					
					URI uri = URIUtils.getAbsoluteURI(node);
					File file = URIUtils.getFile(uri);
					if(uri == null || file == null || !file.exists() || !file.isFile()){
						continue;
					}
					Stack<File> folderStack = MonitoringUtils.getFolderStructureStack(selected, uri);				
					if(!folderStack.isEmpty()){
						result.put(node, folderStack);
					}					
				}
				for(final Entry<NodeModel, Stack<File>> entry : result.entrySet()){	
					SwingUtilities.invokeAndWait(
					        new Runnable() {
					            public void run(){					            	
					            	NodeModel target = MonitoringUtils.createFolderStructurePath(selected, entry.getValue());
									((MMapController) Controller.getCurrentModeController().getMapController()).moveNode(entry.getKey(), target, target.getChildCount());															
					            }
					        }
					   );
					
				}
				return null;
			}
			
			@Override
		    protected void done() {			
				if(this.isCancelled() || Thread.currentThread().isInterrupted()){					
					this.firePropertyChange(SwingWorkerDialog.IS_DONE, null, TextUtils.getText("MonitoringFlattenSubfoldersAction.4")); //$NON-NLS-1$
				}
				else{
					this.firePropertyChange(SwingWorkerDialog.IS_DONE, null, TextUtils.getText("MonitoringFlattenSubfoldersAction.5")); //$NON-NLS-1$
				}
				
			}			
		};		
	}
	
	private void fireStatusUpdate(final String propertyName, final Object oldValue, final Object newValue) throws InterruptedException, InvocationTargetException{				
		SwingUtilities.invokeAndWait(
		        new Runnable() {
		            public void run(){
		            	firePropertyChange(propertyName, oldValue, newValue);										
		            }
		        }
		   );	
	}

	private void removePathNodes(NodeModel selected) throws InterruptedException, InvocationTargetException {
		List<NodeModel> pathNodes = new ArrayList<NodeModel>();
		for(NodeModel node : selected.getChildren()){
			if(DocearNodeMonitoringExtensionController.containsKey(node, DocearExtensionKey.MONITOR_PATH)){
				pathNodes.add(node);
			}
		}
		for(final NodeModel node : pathNodes){
			SwingUtilities.invokeAndWait(
				new Runnable() {
		            public void run(){							            	
						node.removeFromParent();						            											
		            }
		        }        
			);	
		}
	}
	
	private void flattenMonitorNodes(final NodeModel rootTarget, List<NodeModel> children) throws InterruptedException, InvocationTargetException {
		List<NodeModel> pathNodes = new ArrayList<NodeModel>();
		List<NodeModel> monitorNodes = new ArrayList<NodeModel>();
		for(NodeModel node : children){
			if(DocearNodeMonitoringExtensionController.containsKey(node, DocearExtensionKey.MONITOR_PATH)){
				pathNodes.add(node);
			}
			else{
				monitorNodes.add(node);
			}
		}
		for(final NodeModel node : monitorNodes){
			if(node.getParentNode() != rootTarget){
				SwingUtilities.invokeAndWait(
						new Runnable() {
				            public void run(){							            	
				            	((MMapController) Controller.getCurrentModeController().getMapController()).moveNode(node, rootTarget, rootTarget.getChildCount());	            											
				            }
				        }        
					);
				
			}
		}
		for(NodeModel node : pathNodes){
			flattenMonitorNodes(rootTarget, node.getChildren());
		}
	}	

	@Override
	public void setEnabled(){
		if(Controller.getCurrentController().getSelection() == null) {
			this.setEnabled(false);
			return;
		}
		NodeModel selected = Controller.getCurrentController().getSelection().getSelected();
		if(selected == null){
			this.setEnabled(false);
		}
		else{
			this.setEnabled(MonitoringUtils.isMonitoringNode(selected));
		}
		int value = NodeUtilities.getAttributeIntValue(selected, PdfUtilitiesController.MON_FLATTEN_DIRS);
		if(value == 0){			
			this.setSelected(false);
		}
		else{			
			this.setSelected(true);
		}
	}

}
