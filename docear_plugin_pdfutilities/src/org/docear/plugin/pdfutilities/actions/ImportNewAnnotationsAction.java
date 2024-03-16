package org.docear.plugin.pdfutilities.actions;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.docear.plugin.pdfutilities.features.AnnotationID;
import org.docear.plugin.pdfutilities.features.AnnotationModel;
import org.docear.plugin.pdfutilities.features.AnnotationNodeModel;
import org.docear.plugin.pdfutilities.features.IAnnotation;
import org.docear.plugin.pdfutilities.features.IAnnotation.AnnotationType;
import org.docear.plugin.pdfutilities.map.AnnotationController;
import org.docear.plugin.pdfutilities.pdf.DocumentReadOnlyException;
import org.docear.plugin.pdfutilities.pdf.PdfAnnotationImporter;
import org.docear.plugin.pdfutilities.pdf.ReadOnlyExceptionWarningHandler;
import org.docear.plugin.pdfutilities.ui.conflict.ImportConflictDialog;
import org.docear.plugin.pdfutilities.util.MonitoringUtils;
import org.freeplane.core.ui.EnabledAction;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.plugin.workspace.URIUtils;

@EnabledAction(checkOnNodeChange=true)
public class ImportNewAnnotationsAction extends ImportAnnotationsAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String KEY = "ImportNewAnnotationsAction";

	@SuppressWarnings("serial")
	public ImportNewAnnotationsAction() {
		super(KEY);
		this.setEnableType(new ArrayList<AnnotationType>(){{ add(AnnotationType.PDF_FILE); }});
	}

	public void actionPerformed(ActionEvent event) {
		
		NodeModel selected = Controller.getCurrentController().getSelection().getSelected();
		if(selected == null){
			return;
		}
		
		else{
			URI uri = URIUtils.getAbsoluteURI(selected);
            try {
            	PdfAnnotationImporter importer = new PdfAnnotationImporter();
            	ReadOnlyExceptionWarningHandler warningHandler = new ReadOnlyExceptionWarningHandler();
				warningHandler.prepare();
				while(warningHandler.retry()) {
					try {
						Collection<AnnotationModel> annotations = importer.importAnnotations(uri);				
						Map<AnnotationID, Collection<AnnotationNodeModel>> oldAnnotations = MonitoringUtils.getOldAnnotationsFromCurrentMap();				
						annotations = AnnotationController.markNewAnnotations(annotations, oldAnnotations);
						Map<AnnotationID, Collection<IAnnotation>> conflicts = AnnotationController.getConflictedAnnotations(annotations, oldAnnotations);
						if(conflicts.size() > 0){
							ImportConflictDialog dialog = new ImportConflictDialog(Controller.getCurrentController().getViewController().getJFrame(), conflicts);
							dialog.showDialog();
						}
						//System.gc();
		                MonitoringUtils.insertNewChildNodesFrom(annotations, selected.isLeft(), selected, selected);
		                warningHandler.consume();
					} catch (DocumentReadOnlyException e) {
						if(warningHandler.skip()) {
							break;
						}					
						warningHandler.showDialog(URIUtils.getFile(uri));
					}
				}
			} catch (IOException e) {
				LogUtils.severe("ImportAllAnnotationsAction IOException at URI("+uri+"): ", e); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

	}

}
