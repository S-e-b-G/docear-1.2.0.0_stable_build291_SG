package org.docear.plugin.pdfutilities.map;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.docear.pdf.PdfDataExtractor;
import org.docear.plugin.core.DocearController;
import org.docear.plugin.core.util.HtmlUtils;
import org.docear.plugin.pdfutilities.features.AnnotationConverter;
import org.docear.plugin.pdfutilities.features.AnnotationID;
import org.docear.plugin.pdfutilities.features.AnnotationModel;
import org.docear.plugin.pdfutilities.features.AnnotationNodeModel;
import org.docear.plugin.pdfutilities.features.AnnotationXmlBuilder;
import org.docear.plugin.pdfutilities.features.IAnnotation;
import org.docear.plugin.pdfutilities.features.IAnnotation.AnnotationType;
import org.docear.plugin.pdfutilities.pdf.CachedHashItem;
import org.freeplane.core.extension.IExtension;
import org.freeplane.core.io.ReadManager;
import org.freeplane.core.io.WriteManager;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.plugin.workspace.URIUtils;

public class AnnotationController implements IExtension{
	
	private final static Set<IAnnotationImporter> annotationImporters = new HashSet<IAnnotationImporter>();
	private final static Map<String, CachedHashItem> documentHashMap = new HashMap<String, CachedHashItem>();
	private final static Map<String, String> documentTitleMap = new HashMap<String, String>();
	private static final String NO_HASH_AVAILABLE = "";
	private static Executor executor = Executors.newFixedThreadPool(2);
	
	public static void addAnnotationImporter(IAnnotationImporter annotationImporter) {
		annotationImporters.add(annotationImporter);
	}
	
	public static Set<IAnnotationImporter> getAnnotationImporters() {
		return annotationImporters; 
	}
	
	public static AnnotationController getController() {
		return getController(Controller.getCurrentModeController());
	}

	public static AnnotationController getController(ModeController modeController) {
		return (AnnotationController) modeController.getExtension(AnnotationController.class);
	}
	public static void install( final AnnotationController annotationController) {
		Controller.getCurrentModeController().addExtension(AnnotationController.class, annotationController);
	}
	
	public AnnotationController(final ModeController modeController){
		final MapController mapController = modeController.getMapController();
		final ReadManager readManager = mapController.getReadManager();
		final WriteManager writeManager = mapController.getWriteManager();
		AnnotationXmlBuilder builder = new AnnotationXmlBuilder();
		builder.registerBy(readManager, writeManager);
		DocearController.getController().getLifeCycleObserver().addMapLifeCycleListener(new AnnotationConverter());
	}
	
	public static void markNewAnnotations(AnnotationModel importedAnnotation, Map<AnnotationID, Collection<AnnotationNodeModel>> oldAnnotations){
		for(AnnotationModel child : importedAnnotation.getChildren()){
			AnnotationController.markNewAnnotations(child, oldAnnotations);			
		}
		if(oldAnnotations.containsKey(importedAnnotation.getAnnotationID())){						
			importedAnnotation.setNew(false);			
		}
		else{
			importedAnnotation.setNew(true);
		}		
	}
	
	public static Collection<AnnotationModel> markNewAnnotations(Collection<AnnotationModel> importedAnnotations, Map<AnnotationID, Collection<AnnotationNodeModel>> oldAnnotations){
		for(AnnotationModel annotation : importedAnnotations){
			AnnotationController.markNewAnnotations(annotation, oldAnnotations);
		}
		return importedAnnotations;
	}
	
	public static Map<AnnotationID, Collection<IAnnotation>> getConflictedAnnotations(Collection<AnnotationModel> importedAnnotations, Map<AnnotationID, Collection<AnnotationNodeModel>> oldAnnotations) {
		Map<AnnotationID, Collection<IAnnotation>> result = new HashMap<AnnotationID, Collection<IAnnotation>>();
		for(AnnotationModel annotation : importedAnnotations){
			addConflictedAnnotations(getConflictedAnnotations(annotation, oldAnnotations), result);		
		}
		return result;
	}

	public static Map<AnnotationID, Collection<IAnnotation>> getConflictedAnnotations(AnnotationModel importedAnnotation, Map<AnnotationID, Collection<AnnotationNodeModel>> oldAnnotations) {
		Map<AnnotationID, Collection<IAnnotation>> result = new HashMap<AnnotationID, Collection<IAnnotation>>();
		if(oldAnnotations.containsKey(importedAnnotation.getAnnotationID())){
			for(AnnotationNodeModel oldAnnotation : oldAnnotations.get(importedAnnotation.getAnnotationID())){
				String oldAnnotationWithoutHTML = HtmlUtils.extractText(oldAnnotation.getTitle());
				String importedAnnotationTitle = importedAnnotation.getTitle().replace("\r", "").replace("\n", "").replace("\t", "").replace(" ", "");
				String oldAnnotationTitle = oldAnnotation.getTitle().replace("\r", "").replace("\n", "").replace("\t", "").replace(" ", "");
				oldAnnotationWithoutHTML = oldAnnotationWithoutHTML.replace("\r", "").replace("\n", "").replace("\t", "").replace(" ", "");
				if(!importedAnnotationTitle.equals(oldAnnotationTitle) && !importedAnnotationTitle.equals(oldAnnotationWithoutHTML) && !oldAnnotation.getAnnotationType().equals(AnnotationType.PDF_FILE) && !importedAnnotation.getAnnotationType().equals(AnnotationType.TRUE_HIGHLIGHTED_TEXT)){
					importedAnnotation.setConflicted(true);					
				}
				
			}
		}
		if(importedAnnotation.isConflicted()){
			addConflictedAnnotation(importedAnnotation, result);
			for(AnnotationNodeModel oldAnnotation : oldAnnotations.get(importedAnnotation.getAnnotationID())){
				addConflictedAnnotation(oldAnnotation, result);
			}
		}
		for(AnnotationModel child : importedAnnotation.getChildren()){
			addConflictedAnnotations(getConflictedAnnotations(child, oldAnnotations), result);
		}
		return result;
	}
	
	public static AnnotationModel getModel(final NodeModel node, boolean update) {
		AnnotationModel annotation = (AnnotationModel) node.getExtension(AnnotationModel.class);
		if(annotation == null && update){
			AnnotationController.setModel(node);
			annotation = (AnnotationModel) node.getExtension(AnnotationModel.class);
		}
		return annotation;
	}
	
	public static int getAnnotationPosition(NodeModel node){
		AnnotationModel annotation = AnnotationController.getModel(node, false);
    	if(annotation != null && annotation.getParent() != null){
    		return annotation.getParent().getChildIndex(annotation);
    	}
    	return -1;
	}
	
	private static boolean isPdfFile(File file) {
		if (file == null) {
			return false;
		}
		return file.getName().toLowerCase().endsWith(".pdf");
	}
	
	public static AnnotationNodeModel getAnnotationNodeModel(final NodeModel node){
		IAnnotation annotation = AnnotationController.getModel(node, true);
		URI uri = URIUtils.getAbsoluteURI(node);
		File file = URIUtils.getFile(uri);
		if(annotation != null && file == null){
			setModel(node, null);
			return null;
		}
		//DOCEAR - FixMe: what about the source uri? 
		AnnotationNodeModel model;
		if(annotation != null && annotation.getAnnotationType() != null && !annotation.getAnnotationType().equals(AnnotationType.PDF_FILE)) {	
			model = new AnnotationNodeModel(node, annotation);
			//model.setSource(uri);
			return model;
		}		
		if(annotation != null && file != null && annotation.getAnnotationType().equals(AnnotationType.PDF_FILE)) {
			model = new AnnotationNodeModel(node, annotation);
			//model.setSource(uri);
			return model; 
		}		
		if(annotation == null && file != null && file.getName().equals(node.getText()) && isPdfFile(file)){		
			annotation = new AnnotationModel(0);
			((AnnotationModel)annotation).setSource(uri);
		
			model = new AnnotationNodeModel(node, annotation);
			model.setAnnotationType(AnnotationType.PDF_FILE);
			//model.setSource(uri);
			return model; 
		}
		if(annotation == null && file != null && file.getName().equals(node.getText()) && !isPdfFile(file)){
			annotation = new AnnotationModel(0);
			((AnnotationModel)annotation).setSource(uri);
			model = new AnnotationNodeModel(node, annotation);
			model.setAnnotationType(AnnotationType.FILE);
			return model; 
		}
		return null;
	}

	public static IAnnotation setModel(final NodeModel node, final IAnnotation annotationModel) {
		if(annotationModel == null) {
			return (IAnnotation) node.removeExtension(AnnotationModel.class);
		}
		else {
			return (IAnnotation) node.putExtension(annotationModel);
		}
//		final IAnnotation oldAnnotationModel = (IAnnotation) node.getExtension(AnnotationModel.class);
//		if (annotationModel != null && oldAnnotationModel == null) {
//			node.putExtension(annotationModel);
//		}
//		else if (annotationModel == null && oldAnnotationModel != null) {
//			node.removeExtension(AnnotationModel.class);
//		}
//		else if(annotationModel == null && oldAnnotationModel == null){
//			node.removeExtension(AnnotationModel.class);
//		}
	}
	
	private static void setModel(final NodeModel node){
		URI uri = URIUtils.getAbsoluteURI(node);
		File file = URIUtils.getFile(uri);
		if(!isPdfFile(file)){
			return;
		}		
		for(IAnnotationImporter importer : annotationImporters) {
			try {			
				importer.searchAnnotation(uri, node);
			
			} catch (Exception e) {			
				LogUtils.warn("org.docear.plugin.core.mindmap.AnnotationController.setModel: "+e.getMessage());
			}
		}		
	}
	
	public static void addConflictedAnnotation(IAnnotation annotation, Map<AnnotationID, Collection<IAnnotation>> result){
		if(result.containsKey(annotation.getAnnotationID())){
			boolean add = true;
			for(IAnnotation conflict: result.get(annotation.getAnnotationID())){
				if(annotation instanceof AnnotationModel && !(annotation instanceof AnnotationNodeModel) && conflict instanceof AnnotationModel){
					add = false;
					break;
				}
				if(annotation instanceof AnnotationNodeModel && conflict instanceof AnnotationNodeModel && ((AnnotationNodeModel) annotation).getNode().equals(((AnnotationNodeModel) conflict).getNode())){
					add = false;
					break;
				}
			}
			if(add){
				result.get(annotation.getAnnotationID()).add(annotation);
			}
		}
		else{
			result.put(annotation.getAnnotationID(), new ArrayList<IAnnotation>());
			result.get(annotation.getAnnotationID()).add(annotation);
		}
	}
	
	public static void addConflictedAnnotations(Map<AnnotationID, Collection<IAnnotation>> conflicts, Map<AnnotationID, Collection<IAnnotation>> result){
		for(AnnotationID id :conflicts.keySet()){
			if(result.containsKey(id)){
				for(IAnnotation conflict : conflicts.get(id)){
					addConflictedAnnotation(conflict, result);
				}
				//result.get(id).addAll(conflicts.get(id));
			}
			else{
				result.put(id, conflicts.get(id));
			}
		}
	}
	
	public static void registerDocumentHash(final URI uri, final String hash) {
		if(uri == null || hash == null) {
			return;
		}
		final File file = new File(uri);
		final long lastModified = file.lastModified();
		CachedHashItem hashItem = documentHashMap.get(file.toString());
		
		if(hashItem == null) {
			documentHashMap.put(file.toString(), new CachedHashItem(hash, System.currentTimeMillis()));
			executeHashConfirmation(file, lastModified);
		}
		else {	
			if(hashItem.getLastUpdate() < lastModified) {
				documentHashMap.put(file.toString(), new CachedHashItem(hashItem.getHashCode(), System.currentTimeMillis()));
				executeHashConfirmation(file, lastModified);
			}			
		}		
	}

	private static void executeHashConfirmation(final File file, final long lastModified) {
		if(file.exists()) {
			executor.execute(new Runnable() {
				public void run() {
					updateDocumentHashCache(file, lastModified);
				}
				
				public String toString() {
					return ""+file;
				}
			});
		}
	}
	
	public static String getDocumentHash(URI uri) {
		if(uri == null) {
			return null;
		}
		String hash = null;
		
		File file = new File(uri);
		
		if(!file.exists() || !file.getName().toLowerCase().endsWith(".pdf")) {
			return null;
		}
		
		long lastModified = file.lastModified();
				
		CachedHashItem hashItem = documentHashMap.get(file.toString());		
		if(hashItem == null || (hashItem.getLastUpdate() < lastModified)) {
			hash = updateDocumentHashCache(file, lastModified);
		}
		else {
			hash = hashItem.getHashCode();
		}
		
		if(NO_HASH_AVAILABLE.equals(hash)) {
			hash = null;
		}
		
		return hash; 
	}
	
	public static String getDocumentTitle(URI uri) {
		String title = null;
		
		if(uri == null) {
			return title;
		}
		
		String hashCode = getDocumentHash(uri);
		
		if(hashCode != null) {
			synchronized (documentTitleMap) {
				title = documentTitleMap.get(hashCode);
			}
		}
		return title;
	}
	
	private static String updateDocumentHashCache(final File file, final long lastModified) {
		String hashCode = null;
		String title = null;
		try {
			PdfDataExtractor extractor = new PdfDataExtractor(file);
			try {
				title = extractor.extractTitle();
				hashCode = extractor.getUniqueHashCode();
			}
			finally {
				extractor.close();
				extractor = null;
			}
			if(hashCode == null ) {
				hashCode = NO_HASH_AVAILABLE;
			}
			else {
				synchronized (documentTitleMap) {
					//keep existing titles: could be a user input or retrieved via bibtex or sth else
					if(!documentTitleMap.containsKey(hashCode)) {
						documentTitleMap.put(hashCode, title);
					}
				}
			}
			CachedHashItem newItem = new CachedHashItem(hashCode, lastModified);
			synchronized (documentHashMap) {
				documentHashMap.put(file.toString(), newItem);
			}
		}
		catch (Exception e) {
			hashCode = NO_HASH_AVAILABLE;
			LogUtils.info("could not extract unique file hash: "+file+"\n"+ e.getMessage());			
		}
		
		return hashCode;
	}

	public void updateIndex(File nuFile, File oldFile) {
		if(nuFile == null || oldFile == null) {
			return;
		}
		Map<File, File> fileMap = new HashMap<File, File>();
		if(!nuFile.isDirectory()) {				
			fileMap.put(oldFile, nuFile);			
		}
		else{
			Collection<File> files = FileUtils.listFiles(nuFile, null, true);			
			for(File file : files){
				String oldPath = file.getPath().replace(nuFile.getPath(), oldFile.getPath());
				fileMap.put(new File(oldPath), file);				
			}			
		}
		synchronized (documentHashMap) {
			for(Entry<File, File> entry : fileMap.entrySet()) {
				if(documentHashMap.containsKey(entry.getKey().toString())) {
					CachedHashItem hashItem = documentHashMap.remove(entry.getKey().toString());
					documentHashMap.put(entry.getValue().toString(), hashItem);
				}
			}
		}
	}

}
