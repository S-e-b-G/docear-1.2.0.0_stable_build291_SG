package org.docear.plugin.bibtex.listeners;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.TreeMap;

import javax.swing.SwingUtilities;

import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.export.DocearReferenceUpdateController;

import org.docear.plugin.bibtex.JabRefProjectExtension;
import org.docear.plugin.bibtex.ReferencesController;
import org.docear.plugin.bibtex.jabref.ResolveDuplicateEntryAbortedException;
import org.freeplane.features.map.INodeSelectionListener;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.plugin.workspace.WorkspaceController;
import org.freeplane.plugin.workspace.features.WorkspaceMapModelExtension;
import org.freeplane.plugin.workspace.model.project.AWorkspaceProject;

public class MapViewListener implements MouseListener, INodeSelectionListener {

	private void handleEvent() {
		if(ReferencesController.getController().getJabrefWrapper().getBasePanel() != null) {
			BasePanel bp = ReferencesController.getController().getJabrefWrapper().getBasePanel();
			WorkspaceMapModelExtension ext = WorkspaceController.getMapModelExtension(Controller.getCurrentController().getMap());
			if(ext != null) {
				AWorkspaceProject project = ext.getProject();
				if(project != null && project.isLoaded()) {
					JabRefProjectExtension jpe = (JabRefProjectExtension) project.getExtensions(JabRefProjectExtension.class);
					if(jpe != null && !jpe.getBaseHandle().getBasePanel().equals(bp)) {						
						jpe.selectBasePanel();			
					}
				}
			}
		}
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

				ReferencesController referencesController = ReferencesController.getController();

				if (referencesController.getInChange() != null) {
					referencesController.setInChange(null);
					ReferencesController.getController().getJabrefWrapper().getBasePanel().runCommand("save");
				}

				if (referencesController.getInAdd() != null) {
					BibtexEntry addedEntry = ReferencesController.getController().getAddedEntry();
					try {
						if (addedEntry != null) {
							ReferencesController.getController().setAddedEntry(null);
							generateKeyIfNeeded(addedEntry);
							//JabRefAttributes jabRefAttributes = ReferencesController.getController().getJabRefAttributes();

							//jabRefAttributes.resolveDuplicateLinks(addedEntry);
						}
						addToNodes(referencesController.getInAdd());
					}
//					catch (InterruptedException ex) {
//						System.out.println("MApViewListener interrupted");
//						return;
//					}
					finally {
						referencesController.setInAdd(null);
						ReferencesController.getController().getJabrefWrapper().getBasePanel().runCommand("save");
					}
				}
				if(ReferencesController.getController().getJabrefWrapper().getBasePanel() != null) {
					BibtexEntry[] selectedEntries = ReferencesController.getController().getJabrefWrapper().getBasePanel().getSelectedEntries();
					if (selectedEntries != null && selectedEntries.length == 1) {
						BibtexEntry entry = selectedEntries[0];
						generateKeyIfNeeded(entry);
					}
				}
			}
		});
	}

	private void generateKeyIfNeeded(BibtexEntry entry) {
		if (entry.getCiteKey() != null && entry.getCiteKey().trim().length() > 0) {
			return;
		}		
		ReferencesController.getController().getJabRefAttributes().generateBibtexEntry(entry);		
	}

	private void addToNodes(MapModel mapModel) {
		if (DocearReferenceUpdateController.isLocked()) {
			return;
		}
		
		DocearReferenceUpdateController.lock();
		try {
			WorkspaceMapModelExtension mapExt = WorkspaceController.getMapModelExtension(mapModel);
    		if(mapExt == null || mapExt.getProject() == null || !mapExt.getProject().isLoaded()) {
    			//DOCEAR - todo: what to do?
    		}
    		else {    			
    			JabRefProjectExtension prjExt = (JabRefProjectExtension) mapExt.getProject().getExtensions(JabRefProjectExtension.class);
    			ReferencesController.getController().getJabrefWrapper().getJabrefFrame().showBasePanel(prjExt.getBaseHandle().getBasePanel());
    		}
			BibtexDatabase database = ReferencesController.getController().getJabrefWrapper().getDatabase();

			TreeMap<String, BibtexEntry> entryNodeTuples = new TreeMap<String, BibtexEntry>();

			for (BibtexEntry entry : database.getEntries()) {
				String s = entry.getField("docear_add_to_node");
				if (s != null) {
					String[] nodeIds = s.split(",");
					for (String nodeId : nodeIds) {
						entryNodeTuples.put(nodeId, entry);
						NodeModel node = mapModel.getNodeForID(nodeId);
						if (node != null) {
							try {
								ReferencesController.getController().getJabRefAttributes().setReferenceToNode(entry, node);
							}
							catch (ResolveDuplicateEntryAbortedException e) {
							}
						}
					}
				}
				entry.setField("docear_add_to_node", null);
			}
		}
		finally {
			DocearReferenceUpdateController.unlock();
		}
	}

	public void mouseClicked(MouseEvent e) {
		handleEvent();
	}

	public void mousePressed(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void onDeselect(NodeModel node) {
	}

	public void onSelect(NodeModel node) {
		handleEvent();
	}

}
