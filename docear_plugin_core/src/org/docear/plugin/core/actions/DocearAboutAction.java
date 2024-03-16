package org.docear.plugin.core.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Properties;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

import org.docear.plugin.core.DocearController;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.Compat;
import org.freeplane.core.util.FreeplaneVersion;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.help.AboutAction;

public class DocearAboutAction extends AboutAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DocearAboutAction() {
		super();
	}

	public void actionPerformed(final ActionEvent e) {
		ResourceController resourceController = ResourceController.getResourceController();

		Properties about_props = new Properties();
		try {
			about_props.load(this.getClass().getResourceAsStream("/about.properties"));
		}
		catch (IOException e1) {
			LogUtils.warn("DOCEAR: could not load core \"about\" properties");
		}
		String programmer = about_props.getProperty("docear_programmer");
		String copyright = about_props.getProperty("docear_copyright") + Calendar.getInstance().get(Calendar.YEAR);

		String aboutText = TextUtils.getRawText("docear_about");
		MessageFormat formatter;
		try {
			formatter = new MessageFormat(aboutText);
			DocearController ctrl = DocearController.getController();
			String info = ctrl.getVersion().toString();
			aboutText = formatter.format(new Object[] { info, copyright, programmer });
		}
		catch (IllegalArgumentException ex) {
			LogUtils.severe("wrong format " + aboutText + " for property " + "docear_about", ex);
		}

		Box box = Box.createVerticalBox();
		addMessage(box, aboutText);
		addUri(box, "http://docear.org", "http://docear.org");

		addMessage(box, " "); // separator gap
		addFormattedMessage(box, "docear.about.freeplane.text", "");
		addUri(box, resourceController.getProperty("homepage_url"), "Freeplane " + FreeplaneVersion.getVersion().toString());

		addMessage(box, " "); // separator gap
		addFormattedMessage(box, "docear.about.jabref.text", "");
		addUri(box, "http://jabref.sourceforge.net/", "JabRef");

		addMessage(box, " "); // separator gap
		// addMessage(box, FreeplaneVersion.getVersion().getRevision());
		addFormattedMessage(box, "java_version", Compat.JAVA_VERSION);
		String installDir = new File(ResourceController.getResourceController().getResourceBaseDir()).getParentFile().getAbsolutePath();
		addFormattedMessage(box, "docear.main_resource_directory", installDir);

		addMessage(box, " "); // separator gap
		addUri(box, resourceController.getProperty("icons_url"), TextUtils.getText("docear_icons"));
		addMessage(box, " "); // separator gap
		addUri(box, resourceController.getProperty("license_url"), TextUtils.getText("license"));
		addMessage(box, TextUtils.removeTranslateComment(TextUtils.getText("license_text")));

		JOptionPane.showMessageDialog(UITools.getFrame(), box, TextUtils.getText("AboutAction.text"), JOptionPane.INFORMATION_MESSAGE);
	}

	private void addFormattedMessage(Box box, String format, String parameter) {
		box.add(new JLabel(TextUtils.format(format, parameter)));
	}

	private void addMessage(Box box, String localMessage) {
		box.add(new JLabel(localMessage));
	}

	private void addUri(Box box, String uriString, String message) {
		try {
			URI uri = new URI(uriString);
			JButton uriButton = UITools.createHtmlLinkStyleButton(uri, message);
			uriButton.setHorizontalAlignment(SwingConstants.LEADING);
			box.add(uriButton);
		}
		catch (URISyntaxException e1) {
		}
	}

}
