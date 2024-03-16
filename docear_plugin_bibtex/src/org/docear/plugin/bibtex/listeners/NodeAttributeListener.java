package org.docear.plugin.bibtex.listeners;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;

import org.docear.plugin.bibtex.ReferencesController;
import org.docear.plugin.bibtex.jabref.JabRefAttributes;
import org.freeplane.features.attribute.Attribute;
import org.freeplane.features.attribute.NodeAttributeTableModel;

public class NodeAttributeListener implements TableModelListener {
	//WORKSPACE - DOCEAR todo: make sure the right db is active in jabref
	public void tableChanged(TableModelEvent e) {
		// do not use locking mechanism --> changes made to jabref should change all associated entries in mindmap nodes as well

		// if changes only happened one row and one column
		if (e.getFirstRow() == e.getLastRow() && e.getColumn() > 0) {
			JabRefAttributes jabRefAttributes = ReferencesController.getController().getJabRefAttributes();
			NodeAttributeTableModel table = (NodeAttributeTableModel) e.getSource();
			Attribute attribute = table.getAttribute(e.getFirstRow());
			

			String key = null;
			//only act on known attributes
			if (attribute.getName().equals(jabRefAttributes.getKeyAttribute())) {
				//TODO - what should be done here???				
			}
			else if (jabRefAttributes.getValueAttributes().containsKey(attribute.getName())) {				
				int pos = table.getAttributePosition(jabRefAttributes.getKeyAttribute());
				if (pos >= 0) {
					key = (String) table.getValue(pos);
					
					if (key != null) {
						updateBibtexEntry(key, attribute);
					}
				}
			}
		}
	}

	private void updateBibtexEntry(String key, Attribute attribute) {
		BibtexDatabase database = ReferencesController.getController().getJabrefWrapper().getDatabase();
		
		BibtexEntry entry = database.getEntryByKey(key);
		if (entry != null) {
			ReferencesController.getController().getJabrefWrapper().getBasePanel().markBaseChanged();
			//updating the entry updates it in the database object which is used to rendere jabrefs entry table --> no need to update the jabref view
			entry.setField(ReferencesController.getController().getJabRefAttributes().getValueAttributes().get(attribute.getName()), attribute.getValue().toString());			
		}
	}
	
	

}
