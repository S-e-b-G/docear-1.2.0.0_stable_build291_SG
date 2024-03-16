package org.docear.plugin.bibtex.jabref;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.ws.rs.core.UriBuilder;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.labelPattern.LabelPatternUtil;

import org.apache.commons.io.FilenameUtils;
import org.docear.plugin.bibtex.JabRefProjectExtension;
import org.docear.plugin.bibtex.Reference;
import org.docear.plugin.bibtex.Reference.Item;
import org.docear.plugin.bibtex.ReferencesController;
import org.docear.plugin.core.features.DocearMapModelExtension;
import org.docear.plugin.core.features.MapModificationSession;
import org.docear.plugin.core.util.NodeUtilities;
import org.docear.plugin.core.workspace.model.DocearWorkspaceProject;
import org.freeplane.core.util.Compat;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.attribute.Attribute;
import org.freeplane.features.attribute.AttributeController;
import org.freeplane.features.attribute.NodeAttributeTableModel;
import org.freeplane.features.link.LinkController;
import org.freeplane.features.link.LinkModel;
import org.freeplane.features.link.NodeLinkModel;
import org.freeplane.features.link.NodeLinks;
import org.freeplane.features.link.mindmapmode.MLinkController;
import org.freeplane.features.map.INodeView;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.url.UrlManager;
import org.freeplane.plugin.workspace.URIUtils;
import org.freeplane.plugin.workspace.WorkspaceController;
import org.freeplane.plugin.workspace.features.WorkspaceMapModelExtension;
import org.freeplane.plugin.workspace.model.project.AWorkspaceProject;
import org.freeplane.view.swing.map.NodeView;

public class JabRefAttributes {
	private static Boolean updateNodeLock = false;

	private boolean nodeDirty = false;

	private HashMap<String, String> valueAttributes = new HashMap<String, String>();
	private String keyAttribute;

	public JabRefAttributes() {
		registerAttributes();
	}

	public void registerAttributes() {
		this.keyAttribute = TextUtils.getText("bibtex_key");

		this.valueAttributes.put("authors", "author");
		this.valueAttributes.put("title", "title");
		this.valueAttributes.put("year", "year");
		this.valueAttributes.put("journal", "journal");
	}

	public String getKeyAttribute() {
		return keyAttribute;
	}

	public HashMap<String, String> getValueAttributes() {
		return valueAttributes;
	}

	public String getBibtexKey(NodeModel node) {
		return getAttributeValue(node, this.keyAttribute);
	}

	public String getAttributeValue(NodeModel node, String attributeName) {
		NodeAttributeTableModel attributeTable = (NodeAttributeTableModel) node.getExtension(NodeAttributeTableModel.class);
		if (attributeTable == null) {
			return null;
		}
		for (Attribute attribute : attributeTable.getAttributes()) {
			if (attribute.getName().equals(attributeName)) {
				return attribute.getValue().toString();
			}
		}

		return null;
	}

	public boolean isReferencing(BibtexEntry entry, NodeModel node) {
		String nodeKey = getBibtexKey(node);
		String entryKey = entry.getCiteKey();
		if (nodeKey != null && entryKey != null && nodeKey.equals(entryKey)) {
			return true;
		}
		return false;
	}

	public void setReferenceToNode(BibtexEntry entry) throws ResolveDuplicateEntryAbortedException {
		NodeModel node = Controller.getCurrentModeController().getMapController().getSelectedNode();
		try {
			WorkspaceMapModelExtension mapExt = WorkspaceController.getMapModelExtension(node.getMap());
			AWorkspaceProject project = mapExt.getProject();
			if(project != null) {
				JabRefProjectExtension ext = (JabRefProjectExtension) project.getExtensions(JabRefProjectExtension.class);
				if(ext != null) {
					setReferenceToNode(new Reference(ext.getBaseHandle().getBasePanel(), entry), node);
					return;
				}
			}
			
			setReferenceToNode(new Reference(ReferencesController.getController().getJabrefWrapper().getBasePanel(), entry), node);
			
		}
		catch(Exception e) {
			LogUtils.warn("JabRefAttributes.setReferenceToNode()");
		}
	}

	public void removeReferenceFromNode(NodeModel node) {
		for(INodeView nodeView : node.getViewers()) {
			if(nodeView instanceof NodeView) {
				NodeAttributeTableModel attributeTable = ((NodeView) nodeView).getAttributeView().getAttributes();
				if (attributeTable == null) {
					continue;
				}
				List<String> keys = attributeTable.getAttributeKeyList();
				for (String attributeKey : keys) {
					if (this.valueAttributes.containsKey(attributeKey) || this.keyAttribute.equals(attributeKey)) {
						int pos = attributeTable.getAttributePosition(attributeKey);
						if(pos > -1) {
							try {
								AttributeController.getController(MModeController.getMModeController()).performRemoveRow(attributeTable, pos);
							}
							catch (Exception e) {
								//DOCEAR - ignore for now 
								//the whole attribute removing is completely messed up
								
								//LogUtils.info("not found ("+attributeKey+"): "+pos);
								//LogUtils.warn(e);
							}
						}
					}
				}
				if(attributeTable.getRowCount() <= 0) {
					((NodeView) nodeView).getAttributeView().viewRemoved();
				}
			}
		}		
		
	}

	@SuppressWarnings("unchecked")
	public boolean updateReferenceToNode(Reference reference, NodeModel node) throws ResolveDuplicateEntryAbortedException {
		if (updateNodeLock) {
			return false;
		}
		synchronized (updateNodeLock) {	
			updateNodeLock = true;
		}
		boolean changes = false;
		try {
			MapModificationSession session = node.getMap().getExtension(DocearMapModelExtension.class).getMapModificationSession();
			Set<String> ignoresPdf = null;
			if (session != null) {
				ignoresPdf = (Set<String>) session.getSessionObject(MapModificationSession.FILE_IGNORE_LIST);
				if (ignoresPdf == null) {
					ignoresPdf = new HashSet<String>();
					session.putSessionObject(MapModificationSession.FILE_IGNORE_LIST, ignoresPdf);
				}
			}
			Set<String> ignoresUrl = null;
			if (session != null) {
				ignoresUrl = (Set<String>) session.getSessionObject(MapModificationSession.URL_IGNORE_LIST);
				if (ignoresUrl == null) {
					ignoresUrl = new HashSet<String>();
					session.putSessionObject(MapModificationSession.URL_IGNORE_LIST, ignoresUrl);
				}
			}
			for (URI uri : reference.getUris()) {
				File file = UrlManager.getController().getAbsoluteFile(node.getMap(), uri);
				URL url = null;
				if (file == null) {
					try {
						url = uri.toURL();
					}
					catch (MalformedURLException e) {
						LogUtils.warn(e);
					}
				}
				try {
					if (ignoresPdf != null) {
						if (file != null) {
							if (ignoresPdf.contains(file.getName())) {
								throw new ResolveDuplicateEntryAbortedException(file);
							}
						}
					}
					else if (ignoresUrl != null) {
						if (url != null) {
							if (ignoresUrl.contains(url.toExternalForm())) {
								throw new ResolveDuplicateEntryAbortedException(url);
							}
						}
					}

				}
				catch (NullPointerException e) {
					LogUtils.warn("org.docear.plugin.bibtex.jabrefe.JabRefAttributes.updateReferenceToNode: " + e.getMessage());
				}
				catch (ResolveDuplicateEntryAbortedException ex) {
					if (ignoresPdf != null) {
						if (file != null) {
							ignoresPdf.add(file.getName());
						}
					}
					throw ex;
				}
			}

			NodeUtilities.setAttributeValue(node, reference.getKey().getName(), reference.getKey().getValue(), true);
			NodeUtilities.updateAttributeList();

			NodeAttributeTableModel attributeTable = (NodeAttributeTableModel) node.getExtension(NodeAttributeTableModel.class);
			if (attributeTable == null) {
				return false;
			}

			AttributeController attributeController = AttributeController.getController(MModeController.getMModeController());
			Vector<Attribute> attributes = attributeTable.getAttributes();
			ArrayList<Item> inserts = new ArrayList<Item>();
			for (Item item : reference.getAttributes()) {
				boolean found = false;
				for (int i = 0; i < attributes.size() && !found; i++) {
					try {
					Attribute attribute = attributes.get(i);
					if (attribute.getName().equals(item.getName())) {
						found = true;
						if (item.getValue() == null) {
							attributeController.performRemoveRow(attributeTable, i);
							changes = true;
						}
						else if (!attribute.getValue().equals(item.getValue())) {
							attributeController.performSetValueAt(attributeTable, item.getValue(), i, 1);
							attribute.setValue(item.getValue());
							changes = true;
						}
					}
					}
					catch (Exception e) {
						LogUtils.warn("Exception in org.docear.plugin.bibtex.jabref.JabRefAttributes.updateReferenceToNode(): "+ e.getMessage());
					}
				}
				if (!found && item.getValue() != null) {
					inserts.add(item);
				}
			}

			int i = attributes.size();
			for (Item item : inserts) {
				changes = true;
				try {
					AttributeController.getController(MModeController.getMModeController()).performInsertRow(attributeTable, i, item.getName(), item.getValue());
				}
				catch (Throwable ignore) {
					// probably just another swing with threading issue that can (hopefully) be ignored
				}
				i++;
			}
		}
		finally {
			try {
				// do not overwrite existing links
				NodeLinks nodeLinks = NodeLinks.getLinkExtension(node);
				if (nodeLinks == null || nodeLinks.getHyperLink() == null) {
					// add link to node
					if (reference.getUris().size() > 0) {
						((MLinkController) MLinkController.getController()).setLinkTypeDependantLink(node, reference.getUris().iterator().next());
						changes = true;
					}
					else {
						URL url = reference.getUrl();
						if (url != null) {						
							((MLinkController) MLinkController.getController()).setLinkTypeDependantLink(node, URI.create(url.toExternalForm()));
	
							changes = true;
						}
					}
				}
			}
			finally {
				synchronized (updateNodeLock) {
					updateNodeLock = false;
				}
			}
		}

		return changes;
	}

	public boolean setReferenceToNode(BibtexEntry entry, NodeModel node) throws ResolveDuplicateEntryAbortedException {
		//return setReferenceToNode(new Reference(entry), node);
		try {
			WorkspaceMapModelExtension mapExt = WorkspaceController.getMapModelExtension(node.getMap());
			AWorkspaceProject project = mapExt.getProject();
			if(project != null) {
				JabRefProjectExtension ext = (JabRefProjectExtension) project.getExtensions(JabRefProjectExtension.class);
				if(ext != null) {
					return setReferenceToNode(new Reference(ext.getBaseHandle().getBasePanel(), entry), node);
				}
			}
			
			return setReferenceToNode(new Reference(ReferencesController.getController().getJabrefWrapper().getBasePanel(), entry), node);
		}
		catch(Exception e) {
			LogUtils.warn("JabRefAttributes.setReferenceToNode(): "+e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	public boolean setReferenceToNode(Reference reference, NodeModel node) throws ResolveDuplicateEntryAbortedException {
		return updateReferenceToNode(reference, node);
	}

	public void removeFileromBibtexEntry(File file, BibtexEntry entry) {
		String filename = file.getName();
		FileListTableModel model = new FileListTableModel();
		String oldVal = entry.getField(GUIGlobals.FILE_FIELD);
		if (oldVal == null) {
			return;
		}
		model.setContent(oldVal);
		for (int i = 0; i < model.getRowCount(); i++) {
			FileListEntry fle = model.getEntry(i);
			File f = new File(fle.getLink());
			if (filename.equals(f.getName())) {
				model.removeEntry(i);
				LogUtils.info(oldVal + " <--> " + model.getStringRepresentation());
				i--;
			}
		}
		entry.setField(GUIGlobals.FILE_FIELD, model.getStringRepresentation());

	}

	public void removeUrlFromBibtexEntry(URL url, BibtexEntry entry) {
		String field = entry.getField("url");
		if (url != null && field != null && field.equals(url.toString())) {
			entry.setField("url", null);
		}
	}



	
	// public void resolveDuplicateLinks(BibtexEntry entry) throws
	// InterruptedException {
	// for (String s : retrieveFileLinksFromEntry(entry)) {
	// try {
	// resolveDuplicateLinks(new File(s));
	// }
	// catch (Exception ex) {
	// LogUtils.warn("org.docear.plugin.bibtex.jabref.JabRefAttributes.resolveDuplicateLinks: "
	// + ex.getMessage());
	// }
	// }
	// }

	public void removeLinkFromNode(NodeModel node) {
		for (LinkModel linkModel : NodeLinks.getLinkExtension(node).getLinks()) {
			if (linkModel instanceof NodeLinkModel) {
				((MLinkController) LinkController.getController()).removeArrowLink((NodeLinkModel) linkModel);
			}
		}
	}

	

	// FIXME: not used yet --> implement functionality into
	// findBibtexEntryForPDF
	public BibtexEntry findBibtexEntryForURL(URI nodeUri, MapModel map, boolean ignoreDuplicates) throws ResolveDuplicateEntryAbortedException {
		WorkspaceMapModelExtension mapExt = WorkspaceController.getMapModelExtension(map);
		if(mapExt == null || mapExt.getProject() == null || !mapExt.getProject().isLoaded()) {
			//DOCEAR - todo: what to do?
			return null;
		}
		else {    			
			JabRefProjectExtension prjExt = (JabRefProjectExtension) mapExt.getProject().getExtensions(JabRefProjectExtension.class);
			if(prjExt == null) {
				return null;
			}
			else {
				ReferencesController.getController().getJabrefWrapper().getJabrefFrame().showBasePanel(prjExt.getBaseHandle().getBasePanel());
				
			}
		}
		
		BibtexDatabase database = ReferencesController.getController().getJabrefWrapper().getDatabase();
		if (database == null || nodeUri == null) {
			return null;
		}

		MapModificationSession session = map.getExtension(DocearMapModelExtension.class).getMapModificationSession();

		Set<String> ignores = null;
		if (session != null) {
			ignores = (Set<String>) session.getSessionObject(MapModificationSession.URL_IGNORE_LIST);
			if (ignores == null) {
				ignores = new HashSet<String>();
				session.putSessionObject(MapModificationSession.URL_IGNORE_LIST, ignores);
			}
		}

		URL nodeUrl = null;
		try {
			nodeUrl = nodeUri.toURL();
		}
		catch (Exception e1) {
			LogUtils.info(e1.getMessage());
			return null;
		}
		try {
			if (ignores != null) {
				if (nodeUrl != null && !nodeUrl.toExternalForm().isEmpty()) {
					if (ignores.contains(nodeUrl.toExternalForm())) {
						throw new ResolveDuplicateEntryAbortedException(nodeUrl);
					}
				}
			}
			if (!ignoreDuplicates) {
				DuplicateResolver.getDuplicateResolver().resolveDuplicateLinks(nodeUrl);
			}

			for (BibtexEntry entry : database.getEntries()) {
				String entryUrlField = entry.getField("url");
				if (entryUrlField != null && !entryUrlField.isEmpty()) {
					URI entryUri = null;
					try {
						entryUri = URI.create(entryUrlField);
					}
					catch (Exception e) {
						LogUtils.warn("org.docear.plugin.bibtex.jabref.JabRefAttributes.findBibtexEntryForURL: " + e.getMessage());
						continue;
					}
					String entryScheme = entryUri.getScheme();
					String nodeScheme = nodeUri.getScheme();

					if (entryScheme != null && nodeScheme != null && !entryScheme.equals(nodeScheme)) {
						continue;
					}

					String entryUriString = entryUri.toString();
					if (entryScheme != null) {
						entryUriString = entryUriString.substring(entryScheme.length() + 3);
					}

					String nodeUriString = nodeUri.toString();
					if (nodeScheme != null) {
						nodeUriString = nodeUriString.substring(nodeScheme.length() + 3);
					}

					if (entryUriString.equals(nodeUriString)) {
						return entry;
					}
				}
			}
		}
		catch (ResolveDuplicateEntryAbortedException e) {
			if (ignores != null) {
				if (nodeUrl != null && !nodeUrl.toExternalForm().isEmpty()) {
					ignores.add(nodeUrl.toExternalForm());
				}
			}
			throw e;

		}
		return null;
	}

	public BibtexEntry findBibtexEntryForPDF(URI uri, MapModel map) throws ResolveDuplicateEntryAbortedException {
		return findBibtexEntryForPDF(uri, map, false);
	}

	public BibtexEntry findBibtexEntryForPDF(URI uri, MapModel map, boolean ignoreDuplicates) throws ResolveDuplicateEntryAbortedException {
		WorkspaceMapModelExtension mapExt = WorkspaceController.getMapModelExtension(map);
		if(mapExt == null || mapExt.getProject() == null || !mapExt.getProject().isLoaded()) {
			//DOCEAR - todo: what to do?
			return null;
		}
		else {    			
			JabRefProjectExtension prjExt = (JabRefProjectExtension) mapExt.getProject().getExtensions(JabRefProjectExtension.class);
			if(prjExt == null) {
				//DOCEAR - todo: what to do?
				return null;
			}
			ReferencesController.getController().getJabrefWrapper().getJabrefFrame().showBasePanel(prjExt.getBaseHandle().getBasePanel());
		}
		
		BibtexDatabase database = ReferencesController.getController().getJabrefWrapper().getDatabase();
		if (database == null) {
			return null;
		}
		// file name linked in a node
		File nodeFile = UrlManager.getController().getAbsoluteFile(map, uri);
		if (nodeFile == null || nodeFile.getAbsolutePath().isEmpty()) {
			return null;
		}
		String nodeFileName = nodeFile.getName();
		String baseName = FilenameUtils.removeExtension(nodeFileName);

		MapModificationSession session = map.getExtension(DocearMapModelExtension.class).getMapModificationSession();

		Set<String> ignores = null;
		if (session != null) {
			ignores = (Set<String>) session.getSessionObject(MapModificationSession.FILE_IGNORE_LIST);
			if (ignores == null) {
				ignores = new HashSet<String>();
				session.putSessionObject(MapModificationSession.FILE_IGNORE_LIST, ignores);
			}
		}

		try {
			if (ignores != null) {
				if (nodeFileName != null) {
					if (ignores.contains(nodeFileName)) {
						throw new ResolveDuplicateEntryAbortedException(nodeFile);
					}
				}
			}
			if (!ignoreDuplicates) {
				DuplicateResolver.getDuplicateResolver().resolveDuplicateLinks(nodeFile);
			}

			for (BibtexEntry entry : database.getEntries()) {
				String jabrefFiles = entry.getField(GUIGlobals.FILE_FIELD);
				if (jabrefFiles != null) {
					// path linked in jabref
					for (String jabrefFile : parsePathNames(entry, jabrefFiles)) {
						if (jabrefFile.endsWith(nodeFileName)) {
							return entry;
						}
					}
				}
			}
		}
		catch (ResolveDuplicateEntryAbortedException e) {
			if (ignores != null) {
				if (nodeFileName != null) {
					ignores.add(nodeFileName);
				}
			}
			throw e;
		}

		BibtexEntry entry = database.getEntryByKey(baseName);
		return entry;
	}

	public static ArrayList<String> parsePathNames(BibtexEntry entry, String path) {
		ArrayList<String> fileNames = new ArrayList<String>();

		ArrayList<String> paths = extractPaths(path);
		if (path == null) {
			LogUtils.warn("Could not extract path from: " + entry.getCiteKey());
			return fileNames;
		}
		if (paths == null || paths.size() == 0) {
			return fileNames;
		}
		for (String s : paths) {
			try {
				if (Compat.isWindowsOS()) {
					fileNames.add(new File(s).getPath());
				}
				else {
					// DOCEAR - maybe no escape removal -> could cause problems
					// like in win os
					fileNames.add(new File(removeEscapingCharacter(s)).getPath());
				}
			}
			catch (Exception e) {
				continue;
			}
		}
		return fileNames;
	}

	public ArrayList<URI> parsePaths(DocearWorkspaceProject project, BibtexEntry entry, String pathInBibtexFile) {
		ArrayList<URI> uris = new ArrayList<URI>();
		ArrayList<String> paths = extractPaths(pathInBibtexFile);

		for (String path : paths) {
			if (path == null) {
				LogUtils.warn("Could not extract path from: " + entry.getCiteKey());
				continue;
			}
			path = removeEscapingCharacter(path);
			if (isAbsolutePath(path) && (new File(path)).exists()) {
				uris.add(new File(path).toURI());
			}
			else {
				URI absUri = URIUtils.getAbsoluteURI(project.getBibtexDatabase());

				final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
				Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
				URI pdfUri = absUri.resolve(UriBuilder.fromPath(path).build());
				Thread.currentThread().setContextClassLoader(contextClassLoader);
				File file = null;
				try {
					file = new File(pdfUri);
				}
				catch (IllegalArgumentException e) {
					LogUtils.warn(e.getMessage() + " for: " + path);
				}
				if (file != null && file.exists()) {
					uris.add(pdfUri);
				}
			}
		}
		return uris;
	}

	private static boolean isAbsolutePath(String path) {
		return path.matches("^/.*") || path.matches("^[a-zA-Z]:.*");
	}

	private static String removeEscapingCharacter(String string) {
		return string.replaceAll("([^\\\\]{1,1})[\\\\]{1}", "$1");
	}

	public static ArrayList<String> extractPaths(String fileField) {
		ArrayList<String> paths = new ArrayList<String>();

		if (fileField != null) {
			FileListTableModel model = new FileListTableModel();
			model.setContent(fileField);

			for (int i = 0; i < model.getRowCount(); i++) {
				paths.add(model.getEntry(i).getLink());
			}
		}

		return paths;
	}

	public void generateBibtexEntry(BibtexEntry entry) {
		BibtexDatabase database = ReferencesController.getController().getJabrefWrapper().getDatabase();
		LabelPatternUtil.makeLabel(Globals.prefs.getKeyPattern(), database, entry);
	}

	public boolean isNodeDirty() {
		return nodeDirty;
	}

	public void setNodeDirty(boolean nodeDirty) {
		this.nodeDirty = nodeDirty;
	}

}
