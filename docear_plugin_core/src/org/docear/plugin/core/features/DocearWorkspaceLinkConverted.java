package org.docear.plugin.core.features;


public class DocearWorkspaceLinkConverted implements IRequiredConversion {
	
	private boolean needsBackup = true;
	
	/***********************************************************************************
	 * CONSTRUCTORS
	 **********************************************************************************/

	/***********************************************************************************
	 * METHODS
	 **********************************************************************************/

	/***********************************************************************************
	 * REQUIRED METHODS FOR INTERFACES
	 **********************************************************************************/
	
	public boolean requiresBackup() {
		return needsBackup ;
	}

	public String getBackupLabel() {
		return "convert_links";
	}
}
