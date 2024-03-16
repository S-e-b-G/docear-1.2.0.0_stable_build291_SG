/**
 * author: Marcel Genzmehr
 * 14.12.2011
 */
package org.docear.plugin.core.features;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.freeplane.core.extension.IExtension;
import org.freeplane.core.io.IAttributeWriter;
import org.freeplane.core.io.IElementWriter;
import org.freeplane.core.io.ITreeWriter;
import org.freeplane.core.io.WriteManager;
import org.freeplane.core.util.FreeplaneVersion;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.link.LinkController;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.MapWriter;
import org.freeplane.features.map.NodeModel;

/**
 * 
 */
public class DocearMapWriter extends MapWriter {
	protected static final String USAGE_COMMENT = "<!--To view this file, download Docear - The Academic Literature Suite from http://www.docear.org -->"
			+ System.getProperty("line.separator");

	private final WriteManager writeManager;
	private List<IElementWriter> mapWriter = new ArrayList<IElementWriter>();
	private List<IAttributeWriter> attributeWriter = new ArrayList<IAttributeWriter>();
	
	/***********************************************************************************
	 * CONSTRUCTORS
	 **********************************************************************************/
	/**
	 * @param mapController
	 */

	public DocearMapWriter(MapController mapController) {
		super(mapController);
		writeManager = mapController.getWriteManager();
	}

	/***********************************************************************************
	 * METHODS
	 **********************************************************************************/
	public void setMapWriteHandler() {		
		Iterator<IElementWriter> writerHandle = writeManager.getElementWriters().iterator("map");
		mapWriter.clear();
		while(writerHandle.hasNext()) {
			mapWriter.add(writerHandle.next());
		}
		for(IElementWriter writer : mapWriter) {
			writeManager.removeElementWriter("map", writer);
		}
		writeManager.addElementWriter("map", this);
		
		Iterator<IAttributeWriter> attributeHandle = writeManager.getAttributeWriters().iterator("map");
		attributeWriter.clear();
		while(attributeHandle.hasNext()) {
			attributeWriter.add(attributeHandle.next());
		}
		for(IAttributeWriter writer : attributeWriter) {
			writeManager.removeAttributeWriter("map", writer);
		}
		writeManager.addAttributeWriter("map", this);
	}
	
	
	public void resetMapWriteHandler() {
		writeManager.removeElementWriter("map", this);
		for(IElementWriter writer : mapWriter) {
			writeManager.addElementWriter("map", writer);
		}
		mapWriter.clear();
		
		writeManager.removeAttributeWriter("map", this);
		for(IAttributeWriter writer : attributeWriter) {
			writeManager.addAttributeWriter("map", writer);
		}
		attributeWriter.clear();
	}
	
	public void writeContent(final ITreeWriter writer, final Object node, final String tag) throws IOException {
		writer.addElementContent(USAGE_COMMENT);
		final MapModel map = (MapModel) node;
		writer.addExtensionNodes(map, Arrays.asList(map.getExtensions().values().toArray(new IExtension[] {})));
		final NodeModel rootNode = map.getRootNode();
		writeNode(writer, rootNode, isSaveInvisible(), true);
	}
	
	public void writeAttributes(final ITreeWriter writer, final Object userObject, final String tag) {
		final MapModel map = (MapModel) userObject;
		final DocearMapModelExtension modelExtension = DocearMapModelController.getModel(map);
		
		if (modelExtension == null) {			
			writer.addAttribute("version", FreeplaneVersion.XML_VERSION);		
		}
		else {	
			if(modelExtension.isIncompatible()){
				map.removeExtension(DocearMapModelExtension.class);	
				writer.addAttribute("version", "0.9.0");	
			}
			else{
				final String version = modelExtension.getVersion();
				if (version != null && version.trim().length() > 0) {			
					writer.addAttribute("version", "docear " + version);			
				} else {
					//DOCEAR - fixme: version not set, why and what to do?
					LogUtils.warn("version is null! This should not happen!");
				}

				if (map.getFile() != null) {			
					URI mapUri = LinkController.toLinkTypeDependantURI(map.getFile(), map.getFile(), LinkController.LINK_ABSOLUTE);
					modelExtension.setUri(mapUri);
				}
			}			
		}
		writer.addExtensionAttributes(map, Arrays.asList(map.getExtensions().values().toArray(new IExtension[] {})));
		if(modelExtension != null && modelExtension.isIncompatible()){
			map.addExtension(modelExtension);
		}
	}

	/***********************************************************************************
	 * REQUIRED METHODS FOR INTERFACES
	 **********************************************************************************/
}
