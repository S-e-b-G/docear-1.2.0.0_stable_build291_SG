package org.docear.plugin.bibtex;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.export.DocearReferenceUpdateController;
import net.sf.jabref.labelPattern.LabelPatternUtil;

import org.docear.plugin.bibtex.jabref.DuplicateResolver;
import org.docear.plugin.bibtex.jabref.JabRefAttributes;
import org.docear.plugin.bibtex.jabref.ResolveDuplicateEntryAbortedException;
import org.docear.plugin.core.DocearController;
import org.docear.plugin.core.features.DocearMapModelExtension;
import org.docear.plugin.core.features.MapModificationSession;
import org.docear.plugin.core.mindmap.AMindmapUpdater;
import org.docear.plugin.core.util.NodeUtilities;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.link.NodeLinks;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.url.UrlManager;
import org.freeplane.plugin.workspace.URIUtils;
import org.freeplane.plugin.workspace.WorkspaceController;
import org.freeplane.plugin.workspace.features.WorkspaceMapModelExtension;
import org.freeplane.view.swing.map.NodeView;

public class ReferenceUpdater extends AMindmapUpdater {

	private final HashMap<BibtexEntry, Set<NodeModel>> referenceNodes;
	private final HashMap<String, BibtexEntry> pdfReferences;
	private final HashMap<String, BibtexEntry> urlReferences;

	private JabRefAttributes jabRefAttributes;
	private BibtexDatabase database;
	private MapModificationSession session;
	

	public ReferenceUpdater(String title) {
		super(title);
		referenceNodes = new HashMap<BibtexEntry, Set<NodeModel>>();
		pdfReferences = new HashMap<String, BibtexEntry>();
		urlReferences = new HashMap<String, BibtexEntry>();
	}

	public boolean updateMindmap(MapModel map) {
		session = map.getExtension(DocearMapModelExtension.class).getMapModificationSession();
		if(session == null) {
			session = new MapModificationSession();
			map.getExtension(DocearMapModelExtension.class).setMapModificationSession(session);
		}
		if (DocearController.getController().getSemaphoreController().isLocked("MindmapUpdate")) {
			return false;
		}
		if(DocearReferenceUpdateController.isLocked()) {
			return false;
		}
		try {
    		DocearReferenceUpdateController.lock();    		
    		DocearController.getController().getSemaphoreController().lock("MindmapUpdate");
    		
    		WorkspaceMapModelExtension mapExt = WorkspaceController.getMapModelExtension(map);
    		if(mapExt == null || mapExt.getProject() == null || !mapExt.getProject().isLoaded()) {
    			//DOCEAR - todo: what to do?
    			return false;
    		}
    		else {    			
    			JabRefProjectExtension prjExt = (JabRefProjectExtension) mapExt.getProject().getExtensions(JabRefProjectExtension.class);
    			if(prjExt == null) {
    				return false;
    			}
    			ReferencesController.getController().getJabrefWrapper().getJabrefFrame().showBasePanel(prjExt.getBaseHandle().getBasePanel());
    		}
    		
    		jabRefAttributes = ReferencesController.getController().getJabRefAttributes();
    		database = ReferencesController.getController().getJabrefWrapper().getDatabase();
    		if (database == null) {
    			return false;
    		}
    		if (this.pdfReferences.size() == 0) {
    			buildPdfIndex();
    		}
    		if (this.urlReferences.size() == 0) {
    			buildUrlIndex();
    		}    		
    		return updateMap(map);
		}
		finally {
			DocearController.getController().getSemaphoreController().unlock("MindmapUpdate");
			DocearReferenceUpdateController.unlock();
		}
		
	}

	private boolean updateMap(MapModel map) {
		referenceNodes.clear();
		buildIndex(map.getRootNode());
		LogUtils.info("Updating references on map with "+nodeNum+" nodes ...");
		long snap = System.currentTimeMillis();
		try {
			return updateReferenceNodes();
		}
		finally {
			LogUtils.info("Updated references on map in "+(System.currentTimeMillis()-snap)/1000+"sec");
		}
	}

	private void buildPdfIndex() {
		if (session.getSessionObject(MapModificationSession.FILE_IGNORE_LIST) == null) {
			session.putSessionObject(MapModificationSession.FILE_IGNORE_LIST, new HashSet<String>());
		}
		for (BibtexEntry entry : database.getEntries()) {
			String paths = entry.getField(GUIGlobals.FILE_FIELD);
			if (paths == null || paths.trim().length() == 0) {
				continue;
			}

			for (String path : JabRefAttributes.parsePathNames(entry, paths)) {
				if (path.isEmpty()) {
					continue;
				}
				String name = new File(path).getName().toLowerCase();
				if(((Set<String>) session.getSessionObject(MapModificationSession.FILE_IGNORE_LIST)).contains(name)) {
					continue;
				}

				if (entry.getCiteKey() == null) {
					LabelPatternUtil.makeLabel(Globals.prefs.getKeyPattern(), database, entry);
				}

				if (this.pdfReferences.get(name) == null) {
					this.pdfReferences.put(name, entry);
				}
				else {
					try {
						BibtexEntry singleEntry = DuplicateResolver.getDuplicateResolver().resolveDuplicateLinks(new File(path));
						this.pdfReferences.put(name, singleEntry);
					}
					catch (ResolveDuplicateEntryAbortedException e) {
						this.pdfReferences.remove(name);
						((Set<String>) session.getSessionObject(MapModificationSession.FILE_IGNORE_LIST)).add(e.getFile().getName());
						LogUtils.info("ignore pdf on mindmap update: " + e.getFile());
					}
				}
			}
		}
	}

	private void buildUrlIndex() {
		if (session.getSessionObject(MapModificationSession.URL_IGNORE_LIST) == null) {
			session.putSessionObject(MapModificationSession.URL_IGNORE_LIST, new HashSet<String>());
		}
		for (BibtexEntry entry : database.getEntries()) {
			String url = entry.getField("url");
			if (url == null || url.trim().length() == 0 || ((Set<String>) session.getSessionObject(MapModificationSession.URL_IGNORE_LIST)).contains(url)) {
				continue;
			}

			if (entry.getCiteKey() == null) {
				LabelPatternUtil.makeLabel(Globals.prefs.getKeyPattern(), database, entry);
			}

			
			if (this.urlReferences.get(url) == null) {			
				this.urlReferences.put(url, entry);
			}
			else {
				try {
					BibtexEntry singleEntry = DuplicateResolver.getDuplicateResolver().resolveDuplicateLinks(new URL(url));
					this.urlReferences.put(url, singleEntry);
				}
				catch (MalformedURLException e) {
					LogUtils.warn(e);
				}
				catch (ResolveDuplicateEntryAbortedException e) {
					this.urlReferences.remove(url);
					((Set<String>) session.getSessionObject(MapModificationSession.URL_IGNORE_LIST)).add(e.getUrl().toExternalForm());
					LogUtils.info("ignore url on mindmap update: " + e.getUrl());
				}

			}
		}
	}
	
	private boolean isIgnored(Reference reference, NodeModel node) {
		if (reference.getUris().size() > 0) {
			File file = UrlManager.getController().getAbsoluteFile(node.getMap(), reference.getUris().iterator().next());
			if (file != null) {
				if (((Set<String>) session.getSessionObject(MapModificationSession.FILE_IGNORE_LIST)).contains(file.getName())) {
					return true;
				}
			}
		}
		
		URL u = reference.getUrl();
		if (u != null) {
			if (((Set<String>) session.getSessionObject(MapModificationSession.URL_IGNORE_LIST)).contains(u.toExternalForm())) {
				return true;
			}
		}
		
		URI uri = URIUtils.getAbsoluteURI(node);
		if (uri != null) {
    		File file = UrlManager.getController().getAbsoluteFile(node.getMap(), uri);				
    		if (file != null) {
    			if (!reference.containsLink(uri)) {
    				return true;
    			}
    			
    			if (file != null) {
    				if (((Set<String>) session.getSessionObject(MapModificationSession.FILE_IGNORE_LIST)).contains(file.getName())) {
    					return true;
    				}
    			}
    		}
    		else {
    			u = null;
    			try {
    				u = uri.toURL();
    			}
    			catch (MalformedURLException e) {
    				LogUtils.warn(e.getMessage());
    			}
    			if (u != null) {
    				if (((Set<String>) session.getSessionObject(MapModificationSession.URL_IGNORE_LIST)).contains(u.toExternalForm())) {
    					return true;
    				}
    			}
    		}
		}
		
		return false;
	}

	private boolean updateReferenceNodes() {		
		boolean changes = false;
		NodeView.setModifyModelWithoutRepaint(true);
		try {
		for (Entry<BibtexEntry, Set<NodeModel>> entry : referenceNodes.entrySet()) {
			// BibtexEntry bibtexEntry = database.getEntryByKey(entry.getKey());
			// if (bibtexEntry != null) {
			BibtexEntry bibtexEntry = entry.getKey();
			try {
				WorkspaceMapModelExtension mapExt = WorkspaceController.getMapModelExtension(entry.getValue().iterator().next().getMap());
				if(mapExt.getProject() == null || !mapExt.getProject().isLoaded()) {
					return false;
				}
				JabRefProjectExtension ext = (JabRefProjectExtension) mapExt.getProject().getExtensions(JabRefProjectExtension.class);

				Reference reference = new Reference(ext.getBaseHandle().getBasePanel(), bibtexEntry);

				for (NodeModel node : entry.getValue()) {
					if (isIgnored(reference, node)) {
						continue;
					}

					String key = jabRefAttributes.getBibtexKey(node);
					try {
						if (key == null) {
							changes = true;
							ReferencesController.getController().getJabRefAttributes().setReferenceToNode(reference, node);
						} else {
							changes = changes | ReferencesController.getController().getJabRefAttributes().setReferenceToNode(bibtexEntry, node);
						}
					} catch (ResolveDuplicateEntryAbortedException e) {
						if (e.getFile() != null) {
							((Set<String>) session.getSessionObject(MapModificationSession.FILE_IGNORE_LIST)).add(e.getFile().getName());
						} else {
							((Set<String>) session.getSessionObject(MapModificationSession.URL_IGNORE_LIST)).add(e.getUrl().toExternalForm());
						}
					}

					if (Thread.currentThread().isInterrupted()) {
						return changes;
					}
				}
			}
			catch (Exception e) {
				LogUtils.warn("ReferenceUpdater.updateReferenceNodes(): " + e.getMessage());
			}
		}
		return changes;
		}
		finally {
			NodeView.setModifyModelWithoutRepaint(false);
			NodeUtilities.updateAttributeList();
		}
	}

	int nodeNum = 0;
	private void buildIndex(NodeModel parent) {
		nodeNum++;
		addNodeToReferenceIndex(parent);

		for (NodeModel child : parent.getChildren()) {
			buildIndex(child);
		}
	}

	private void addNodeToReferenceIndex(NodeModel node) {
		try {
			String key = jabRefAttributes.getBibtexKey(node);

			URI uri = NodeLinks.getLink(node);
			if (uri != null && !uri.toString().startsWith("#")) {
				File file;

				file = UrlManager.getController().getAbsoluteFile(node.getMap(), uri);
				if (file != null) {
					String fileName = file.getName().toLowerCase();
					BibtexEntry entry = this.pdfReferences.get(fileName);
					if (entry != null) {
						addReferenceToIndex(node, entry);
					}
					return;
				}
				
				BibtexEntry entry = this.urlReferences.get(uri.toURL().toExternalForm());
				if (entry != null) {
					addReferenceToIndex(node, entry);
					return;
				}
			}

			if (key != null) {
				BibtexEntry bibtexEntry = database.getEntryByKey(key);
				addReferenceToIndex(node, bibtexEntry);
				return;
			}
		}
		catch (Exception e) {
			LogUtils.warn("referenceupdater uri: " + NodeLinks.getLink(node));
			LogUtils.warn(e);
		}
	}

	private void addReferenceToIndex(NodeModel node, BibtexEntry entry) {
		Set<NodeModel> nodes = referenceNodes.get(entry);
		if (nodes == null) {
			nodes = new HashSet<NodeModel>();
			referenceNodes.put(entry, nodes);
		}
		nodes.add(node);
	}

}
