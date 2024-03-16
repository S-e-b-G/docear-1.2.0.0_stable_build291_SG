package org.docear.plugin.bibtex;

import java.util.Collection;

import org.docear.plugin.core.DocearService;
import org.docear.plugin.core.IDocearControllerExtension;
import org.freeplane.features.mode.ModeController;
import org.osgi.framework.BundleContext;

public class Activator extends DocearService {

	public void startService(BundleContext context, ModeController modeController) {
		new ReferencesController(modeController);
	}

	public void stop(BundleContext context) throws Exception {
		
	}

	protected Collection<IDocearControllerExtension> getControllerExtensions() {
		return null;
	}

	
}
