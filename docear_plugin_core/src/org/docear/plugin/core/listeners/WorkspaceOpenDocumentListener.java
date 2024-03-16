package org.docear.plugin.core.listeners;

import java.io.File;

import org.docear.plugin.core.DocearController;
import org.docear.plugin.core.logger.DocearLogEvent;
import org.freeplane.plugin.workspace.URIUtils;
import org.freeplane.plugin.workspace.WorkspaceController;
import org.freeplane.plugin.workspace.event.IWorkspaceNodeActionListener;
import org.freeplane.plugin.workspace.event.WorkspaceActionEvent;
import org.freeplane.plugin.workspace.io.IFileSystemRepresentation;
import org.freeplane.plugin.workspace.model.AWorkspaceTreeNode;
import org.freeplane.plugin.workspace.nodes.ALinkNode;

public class WorkspaceOpenDocumentListener implements IWorkspaceNodeActionListener {	
	
	public void handleAction(WorkspaceActionEvent event) {
		AWorkspaceTreeNode targetNode = getNodeFromActionEvent(event);
		if(targetNode != null) {
			File f = null;
			if(targetNode instanceof IFileSystemRepresentation) {
				f = ((IFileSystemRepresentation) targetNode).getFile();
			} 
			else if(targetNode instanceof ALinkNode) {				
				f = URIUtils.getAbsoluteFile(((ALinkNode) targetNode).getLinkURI());
			}
			if (f != null && !f.getName().toLowerCase().endsWith(".mm")) {
				DocearController.getController().getDocearEventLogger().appendToLog(this, DocearLogEvent.FILE_OPENED, f);
			}
		}

	}
	
	protected AWorkspaceTreeNode getNodeFromActionEvent(WorkspaceActionEvent e) {
		int x = e.getX();
		int y = e.getY();
		return WorkspaceController.getCurrentModeExtension().getView().getNodeForLocation(x, y);		
	}

}
