/**
 * author: Marcel Genzmehr
 * 18.08.2011
 */
package org.docear.plugin.core.workspace.creator;

import org.docear.plugin.core.workspace.node.LinkTypeLiteratureAnnotationsNode;
import org.freeplane.n3.nanoxml.XMLElement;
import org.freeplane.plugin.workspace.URIUtils;
import org.freeplane.plugin.workspace.model.AWorkspaceNodeCreator;
import org.freeplane.plugin.workspace.model.AWorkspaceTreeNode;

/**
 * 
 */
public class LinkTypeLiteratureAnnotationsCreator extends AWorkspaceNodeCreator {
	public static final String LINK_TYPE_LITERATUREANNOTATIONS = LinkTypeLiteratureAnnotationsNode.TYPE;
	
	/***********************************************************************************
	 * CONSTRUCTORS
	 **********************************************************************************/

	/***********************************************************************************
	 * METHODS
	 **********************************************************************************/

	

	/***********************************************************************************
	 * REQUIRED METHODS FOR INTERFACES
	 **********************************************************************************/
	
	public AWorkspaceTreeNode getNode(XMLElement data) {
		String type = data.getAttribute("type", LINK_TYPE_LITERATUREANNOTATIONS);
		LinkTypeLiteratureAnnotationsNode node = new LinkTypeLiteratureAnnotationsNode(type);
		//TODO: add missing attribute handling
		String path = data.getAttribute("path", null);
		String name = data.getAttribute("name", null);
		if (path == null || name == null) {
			return null;
		}	
		node.setLinkPath(URIUtils.createURI(path));		
		node.setName(name);
		return node;
	}
	
}
