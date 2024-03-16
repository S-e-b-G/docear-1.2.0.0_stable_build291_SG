package org.docear.plugin.pdfutilities.listener;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;

import org.docear.plugin.core.CoreConfiguration;
import org.docear.plugin.core.mindmap.AMindmapUpdater;
import org.docear.plugin.core.mindmap.MindmapUpdateController;
import org.docear.plugin.pdfutilities.PdfUtilitiesController;
import org.docear.plugin.pdfutilities.features.ISplmmMapsConvertListener;
import org.docear.plugin.pdfutilities.features.SplmmMapsConvertEvent;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.attribute.Attribute;
import org.freeplane.features.attribute.AttributeController;
import org.freeplane.features.attribute.NodeAttributeTableModel;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.plugin.workspace.URIUtils;

public class MonitorungNodeUpdater extends AMindmapUpdater implements ISplmmMapsConvertListener {
	public static final String MON_AUTO = "mon_auto";
	
	private final static Attribute keyAttribute = new Attribute("splmm_dirmon_path", PdfUtilitiesController.MON_INCOMING_FOLDER); //$NON-NLS-1$
	private static HashMap<String, String> monitoringAttributes;
	private static ArrayList<Attribute> newMonitoringAttributes;
	
	public MonitorungNodeUpdater(String title) {
		super(title);	
		monitoringAttributes = new HashMap<String, String>();		
		MonitorungNodeUpdater.monitoringAttributes.put("splmm_dirmon_auto", PdfUtilitiesController.MON_AUTO); //$NON-NLS-1$
		MonitorungNodeUpdater.monitoringAttributes.put("splmm_dirmon_subdirs", PdfUtilitiesController.MON_SUBDIRS); //$NON-NLS-1$
		
		newMonitoringAttributes = new ArrayList<Attribute>();
		newMonitoringAttributes.add(new Attribute(PdfUtilitiesController.MON_MINDMAP_FOLDER, CoreConfiguration.LIBRARY_PATH));
		newMonitoringAttributes.add(new Attribute(PdfUtilitiesController.MON_FLATTEN_DIRS, 0));		
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

	private boolean updateNode(NodeModel node) {		
		boolean isOldMinitoringNode = false;
		
		NodeAttributeTableModel attributeTable = AttributeController.getController(MModeController.getMModeController()).createAttributeTableModel(node);
		if (attributeTable == null) {
			return false;
		}
		
		for (int i=0; i<attributeTable.getRowCount(); i++) {
			Attribute attribute = attributeTable.getAttribute(i);
			
			if (attribute.getName().equals(keyAttribute.getName())) {
				isOldMinitoringNode = true;
				AttributeController.getController(MModeController.getMModeController()).performSetValueAt(attributeTable, keyAttribute.getValue(), i, 0);
				try {
					String path = (String) attribute.getValue();
					URI uri = new File(path).toURI();
					if (uri.getScheme().length() == 1) {
						throw new Exception("absolut windows paths do not work in linux!"); //$NON-NLS-1$
					}
					uri = URIUtils.getRelativeURI(node.getMap().getFile(), URIUtils.getAbsoluteFile(uri));
					AttributeController.getController(MModeController.getMModeController()).performSetValueAt(attributeTable, uri, i, 1);
				}
				catch(Exception e) {					
					LogUtils.warn(e);
				}				
			}
			
			String newAttributeName = monitoringAttributes.get(attribute.getName());			
			if (newAttributeName != null) {
				int value = Integer.parseInt((String) attribute.getValue());
				isOldMinitoringNode = true;
				AttributeController.getController(MModeController.getMModeController()).performSetValueAt(attributeTable, newAttributeName, i, 0);
				AttributeController.getController(MModeController.getMModeController()).performSetValueAt(attributeTable, value, i, 1);
			}
		}
		
		if (isOldMinitoringNode) {
			for (Attribute attribute : newMonitoringAttributes) {
				AttributeController.getController(MModeController.getMModeController()).performInsertRow(attributeTable, attributeTable.getRowCount(), attribute.getName(), attribute.getValue());				
			}
		}
		
		return isOldMinitoringNode;
	}

	public void mapsConvert(SplmmMapsConvertEvent event) {
		MindmapUpdateController mindmapUpdateController = (MindmapUpdateController) event.getObject();
		mindmapUpdateController.addMindmapUpdater(this);
	}
	
	



}
