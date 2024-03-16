package org.docear.plugin.pdfutilities.map;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.docear.plugin.core.mindmap.AMindmapUpdater;
import org.docear.plugin.core.util.HtmlUtils;
import org.docear.plugin.pdfutilities.features.AnnotationModel;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.plugin.workspace.URIUtils;

public class AnnotationModelUpdater extends AMindmapUpdater {
	
	Map<URI, List<AnnotationModel>> importedPdfs = new HashMap<URI, List<AnnotationModel>>();

	public AnnotationModelUpdater(String title) {
		super(title);		
	}

	private boolean updateNode(NodeModel node) {
		boolean changed = false;
		URI uri = URIUtils.getAbsoluteURI(node);
		File file = URIUtils.getFile(uri);
		if(file != null && file.getName().toLowerCase().endsWith(".pdf") && AnnotationController.getModel(node, false) == null){
			try {
				if(!importedPdfs.containsKey(uri)) {
					for(IAnnotationImporter importer : AnnotationController.getAnnotationImporters()) {
						AnnotationModel pdf = importer.importPdf(uri);
						importedPdfs.put(uri, this.getPlainAnnotationList(pdf));
					}
				}	
				String nodeTextWithoutHTML = HtmlUtils.extractText(node.getText());
				String nodeText = node.getText().replace("\r", "").replace("\n", "").replace("\t", "").replace(" ", "");
				nodeTextWithoutHTML = nodeTextWithoutHTML.replace("\r", "").replace("\n", "").replace("\t", "").replace(" ", "");
				for(AnnotationModel annotation : importedPdfs.get(uri)){					
					String importedAnnotationTitle = annotation.getTitle().replace("\r", "").replace("\n", "").replace("\t", "").replace(" ", "");
					
					if(importedAnnotationTitle.equals(nodeText) || importedAnnotationTitle.equals(nodeTextWithoutHTML)){
						AnnotationController.setModel(node, annotation);	
						changed = true;
						break;
					}
				}
			} catch (Exception e) {
				LogUtils.warn(e);
			}
		}
		return changed;
	}
	
	private List<AnnotationModel> getPlainAnnotationList(AnnotationModel root){
		List<AnnotationModel> result = new ArrayList<AnnotationModel>();
		result.add(root);
		for(AnnotationModel child : root.getChildren()){
			result.addAll(this.getPlainAnnotationList(child));						
		}
		return result;
	}

	@Override
	public boolean updateMindmap(MapModel map) {
		return updateNodesRecursive(map.getRootNode());		
	}
	
	/**
	 * @param node
	 * @return
	 */
	private boolean updateNodesRecursive(NodeModel node) {
		boolean changes = false;
		for(NodeModel child : node.getChildren()) {
			changes = changes | updateNodesRecursive(child);
		}
		changes = changes | updateNode(node);
		return changes;
	}
}
