package org.docear.plugin.services;

import java.net.Authenticator;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.docear.plugin.core.DocearService;
import org.docear.plugin.core.IDocearControllerExtension;
import org.docear.plugin.services.features.io.DocearProxyAuthenticator;
import org.freeplane.core.resources.IFreeplanePropertyListener;
import org.freeplane.core.resources.OptionPanelController;
import org.freeplane.core.resources.OptionPanelController.PropertyLoadListener;
import org.freeplane.core.resources.ResourceBundles;
import org.freeplane.core.resources.components.IPropertyControl;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.osgi.framework.BundleContext;

public class Activator extends DocearService {
	private static final String DEFAULT_LANGUAGE = "en";
	
	public void stop(BundleContext context) throws Exception {
	}

	public void startService(BundleContext context, ModeController modeController) {
		ServiceController.initialize(modeController);
	}

	protected Collection<IDocearControllerExtension> getControllerExtensions() {
		List<IDocearControllerExtension> controllerExtensions = new ArrayList<IDocearControllerExtension>();
		controllerExtensions.add(new IDocearControllerExtension() {
			
			public void installExtension(BundleContext context, Controller controller) {
				setLanguage();
				
				final OptionPanelController optionController = controller.getOptionPanelController();
				
				optionController.addPropertyLoadListener(new PropertyLoadListener() {			
					public void propertiesLoaded(Collection<IPropertyControl> properties) {
						setLanguage();
					}
				});
				
				controller.getResourceController().addPropertyChangeListener(new IFreeplanePropertyListener() {
					
					public void propertyChanged(String propertyName, String newValue, String oldValue) {
						if(propertyName.equalsIgnoreCase("language")){
							setLanguage();
						}
					}
				});
				
				//proxy server handling
				Authenticator.setDefault(new DocearProxyAuthenticator());
				if(DocearProxyAuthenticator.useProxyServer()) {
					DocearProxyAuthenticator.requestAuthenticationData();
				}
				LogUtils.info("Docear Services controller extension initiated.");
			}
		});
		return controllerExtensions;
	}
	
	private void setLanguage() {
		ResourceBundles resBundle = ((ResourceBundles)Controller.getCurrentController().getResourceController().getResources());
		String lang = resBundle.getLanguageCode();
		if (lang == null || lang.equals(ResourceBundles.LANGUAGE_AUTOMATIC)) {
			lang = DEFAULT_LANGUAGE;
		}
		
		URL res = this.getClass().getResource("/translations/Resources_"+lang+".properties");
		if (res == null) {
			lang = DEFAULT_LANGUAGE;
			res = this.getClass().getResource("/translations/Resources_"+lang+".properties");
		}
		
		if (res == null) {
			return;
		}
					
		resBundle.addResources(resBundle.getLanguageCode(), res);
	}

}
