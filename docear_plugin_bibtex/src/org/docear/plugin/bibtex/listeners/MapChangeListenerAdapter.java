package org.docear.plugin.bibtex.listeners;

import java.io.File;
import java.net.URI;
import java.util.Set;

import javax.swing.SwingUtilities;

import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.export.DocearReferenceUpdateController;

import org.docear.plugin.bibtex.JabRefProjectExtension;
import org.docear.plugin.bibtex.ReferenceUpdater;
import org.docear.plugin.bibtex.ReferencesController;
import org.docear.plugin.bibtex.jabref.JabRefAttributes;
import org.docear.plugin.bibtex.jabref.ResolveDuplicateEntryAbortedException;
import org.docear.plugin.core.DocearController;
import org.docear.plugin.core.features.DocearMapModelExtension;
import org.docear.plugin.core.features.MapModificationSession;
import org.docear.plugin.core.logging.DocearLogger;
import org.docear.plugin.core.mindmap.MindmapUpdateController;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.map.AMapChangeListenerAdapter;
import org.freeplane.features.map.MapChangeEvent;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeChangeEvent;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.url.UrlManager;
import org.freeplane.plugin.workspace.WorkspaceController;
import org.freeplane.plugin.workspace.features.WorkspaceMapModelExtension;

public class MapChangeListenerAdapter extends AMapChangeListenerAdapter {

	public void mapChanged(MapChangeEvent event) {
		try {
			WorkspaceMapModelExtension wmme = WorkspaceController.getMapModelExtension(event.getMap());
			if(wmme != null && wmme.getProject() != null && wmme.getProject().getExtensions(JabRefProjectExtension.class) != null) {
				wmme.getProject().getExtensions(JabRefProjectExtension.class).selectBasePanel();
			}
		}
		catch(Exception e) {
			DocearLogger.warn(e);
		}
	}

	public void onNodeDeleted(NodeModel parent, NodeModel child, int index) {
	}

	public void onNodeInserted(NodeModel parent, NodeModel child, int newIndex) {	
	}

	public void onNodeMoved(NodeModel oldParent, int oldIndex, NodeModel newParent, NodeModel child, int newIndex) {
	}

	public void onPreNodeDelete(NodeModel oldParent, NodeModel selectedNode, int index) {
	}

	public void onPreNodeMoved(NodeModel oldParent, int oldIndex, NodeModel newParent, NodeModel child, int newIndex) {
	}

	public void nodeChanged(NodeChangeEvent event) {
		if (DocearController.getController().getSemaphoreController().isLocked("MindmapUpdate")) {
			return;
		}
		if (event.getProperty().equals(NodeModel.HYPERLINK_CHANGED)) {
			URI newUri = (URI) event.getNewValue();
			if (newUri != null) {
				//extracted to pdf-utilities 
//				try{
//					if(new PdfFileFilter().accept(Tools.getFilefromUri(Tools.getAbsoluteUri(newUri)))){
//						if(AnnotationController.getModel(event.getNode(), false) == null){
//							AnnotationModel model = new AnnotationModel();
//							model.setAnnotationID(new AnnotationID(newUri, 0));
//							model.setAnnotationType(AnnotationType.PDF_FILE);
//							AnnotationController.setModel(event.getNode(), model);
//						}
//					}
//				}
//				catch(Exception e){
//					LogUtils.warn(e);
//				}
				JabRefAttributes jabRefAttributes = ReferencesController.getController().getJabRefAttributes();
				MapModificationSession session = event.getNode().getMap().getExtension(DocearMapModelExtension.class).getMapModificationSession();

				Set<String> ignores = null;
				String nodeFileName = null;
				if (session != null) {
					File nodeFile = UrlManager.getController().getAbsoluteFile(event.getNode().getMap(), newUri);
					if (nodeFile != null) {
						nodeFileName = nodeFile.getName();
						ignores = (Set<String>) session.getSessionObject(MapModificationSession.FILE_IGNORE_LIST);
					}
					else {
						ignores = (Set<String>) session.getSessionObject(MapModificationSession.URL_IGNORE_LIST);
					}
					 
					
				}
				try {
					
					BibtexEntry entry = jabRefAttributes.findBibtexEntryForPDF(newUri, event.getNode().getMap());
					if (entry == null) {
						entry = jabRefAttributes.findBibtexEntryForURL(newUri, event.getNode().getMap(), false);
					}
					if (entry != null) {					
						jabRefAttributes.setReferenceToNode(entry, event.getNode());
						if (!DocearController.getController().getSemaphoreController().isLocked("waitingReferenceUpdater") || jabRefAttributes.isNodeDirty()) {
							jabRefAttributes.setNodeDirty(false);
							DocearController.getController().getSemaphoreController().lock("waitingReferenceUpdater");
    						SwingUtilities.invokeLater(new Runnable() {					
    							@Override
    							public void run() {
    								if (DocearReferenceUpdateController.isLocked() || DocearController.getController().getSemaphoreController().isLocked("workingReferenceUpdater")) {
    									return;
    								}
    								
    								try {
    									MindmapUpdateController mindmapUpdateController = new MindmapUpdateController(false);
    									mindmapUpdateController.addMindmapUpdater(new ReferenceUpdater(TextUtils.getText("update_references_open_mindmaps")));
    									mindmapUpdateController.updateCurrentMindmap(true);
    								}
    								finally {
    									DocearController.getController().getSemaphoreController().unlock("waitingReferenceUpdater");
    									DocearController.getController().getSemaphoreController().unlock("workingReferenceUpdater");
    								}
    							}
    						});
						}
					}
				}
				catch (ResolveDuplicateEntryAbortedException e) {
					LogUtils.info("MapChangeListenerAdapter.nodeChanged interrupted");
					if(ignores != null) {
						if(nodeFileName != null) {
							ignores.add(nodeFileName);
						}
						else {
							ignores.add(newUri.toString());
						}
					}
					return;
				}
			}
			//moved to pdf-utilities
//			if(newUri == null && AnnotationController.getModel(event.getNode(), false) != null){
//				AnnotationController.setModel(event.getNode(), null);
//			}
		}
	}

	public void onCreate(MapModel map) {
	}

	public void onRemove(MapModel map) {
	}

	public void onSavedAs(MapModel map) {
		ReferencesController.getController().getJabrefWrapper().getJabrefFrame();
		try {
			saveJabrefDatabase();
		}
		catch (Throwable ex) {
			ex.printStackTrace();
		}

	}

	public void onSaved(MapModel map) {
		ReferencesController.getController().getJabrefWrapper().getJabrefFrame();
		try {
			try {
				ReferencesController.getController().getJabrefWrapper().getBasePanel().getMainTable().setFocusable(false);
			}
			catch(Exception e) {
				LogUtils.info("MapChangeListenerAdapter.onSaved(): "+e.getMessage());
			}
			saveJabrefDatabase();
		}
		catch (Throwable ex) {
			LogUtils.warn("MapChangeListenerAdapter.onSaved(): "+ex.getMessage());
		}
	}

	private void saveJabrefDatabase() {
		BasePanel basePanel = ReferencesController.getController().getJabrefWrapper().getBasePanel();
		if(basePanel != null && basePanel.isBaseChanged()) {
			basePanel.runCommand("save");
		}
		
		ReferencesController.getController().getJabrefWrapper().getBasePanel().getMainTable().setFocusable(true);
	}
}
