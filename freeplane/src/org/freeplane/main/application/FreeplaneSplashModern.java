/*
 * SimplyHTML, a word processor based on Java, HTML and CSS
 * Copyright (C) 2002 Ulrich Hilger
 * Copyright (C) 2006 Karsten Pawlik
 * Copyright (C) 2006 Dimitri Polivaev
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.freeplane.main.application;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.InputStream;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JWindow;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.util.FileUtils;
import org.freeplane.core.util.FreeplaneVersion;
import org.freeplane.features.mode.Controller;

/**
 * Class that displays a splash screen
 * Is run in a separate thread so that the applet continues to load in the background
 * @author Karsten Pawlik
 * 
 */
public class FreeplaneSplashModern extends JWindow {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Font versionTextFont = null;
	private final String description = ResourceController.getResourceController().getProperty("freeplane_description");
	private final String copyright = ResourceController.getResourceController().getProperty("freeplane_copyright");
	private final FreeplaneVersion version = FreeplaneVersion.getVersion(); 
	private String freeplaneNumber = version.numberToString();
	private String status = version.getType().toUpperCase();
	private final String appName;

	public FreeplaneSplashModern(final JFrame frame) {
		super(frame);
		//splashResource = ResourceController.getResourceController().getResource("/images/Freeplane_splash.png");
		appName = ResourceController.getResourceController().getProperty("ApplicationName", "Freeplane");
		//DOCEAR: synch with plugin/app start			
		if(appName != null && !"freeplane".equals(appName.toLowerCase())) {
			freeplaneNumber = ResourceController.getResourceController().getProperty(appName.toLowerCase()+"_version");
			status = ResourceController.getResourceController().getProperty(appName.toLowerCase()+"_version_status");
			if(freeplaneNumber == null) {
				freeplaneNumber = "";
				status = "";
			}
			if(status == null) {
				status = "";
			}
		}
		splashResource = ResourceController.getResourceController().getResource("/images/" + appName + "_splash.png");
		splashImage = new ImageIcon(splashResource);
		getRootPane().setOpaque(false);
		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		final Dimension labelSize = new Dimension(splashImage.getIconWidth(), splashImage.getIconHeight());
		setLocation(screenSize.width / 2 - (labelSize.width / 2), screenSize.height / 2 - (labelSize.height / 2));
		setSize(labelSize);
	}

	private void createVersionTextFont() {
		if(versionTextFont != null){
			return;
		}
	    InputStream fontInputStream = null;
		try {
			fontInputStream = ResourceController.getResourceController().getResource("/fonts/intuitive-subset.ttf")
			    .openStream();
			versionTextFont = Font.createFont(Font.TRUETYPE_FONT, fontInputStream);
		}
		catch (final Exception e) {
			versionTextFont = new Font("Arial", Font.PLAIN, 12);
		}
		finally {
			FileUtils.silentlyClose(fontInputStream);
		}
    }

	private final ImageIcon splashImage;

	private Integer mWidth3;
	private URL splashResource;

	@Override
	public void paint(final Graphics g) {
		final Graphics2D g2 = (Graphics2D) g;
		splashImage.paintIcon(this, g2, 0, 0);
		if(splashResource.getProtocol().equals("file"))
			return;
		if("Freeplane".equals(appName)) {
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			final FreeplaneVersion version = FreeplaneVersion.getVersion();
			final String freeplaneNumber = version.numberToString();
			final String status = version.getType().toUpperCase();
			{
				g2.setColor(Color.WHITE);
				int xCoordinate = 10;
				final int yCoordinate = getSize().height - 10;
				g2.drawString(description, xCoordinate, yCoordinate);
				if (mWidth3 == null) {
					mWidth3 = new Integer(g2.getFontMetrics().stringWidth(copyright));
//		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
//		final FreeplaneVersion version = FreeplaneVersion.getVersion();
//		final String versionString = getVersionText(version);
//		g2.setColor(Color.BLACK);
//		final int xCoordinate = 164;
//		final int yCoordinate = 194;
//		createVersionTextFont();
//		final float versionFontSize;
//		versionFontSize = 15;
//		g2.setFont(versionTextFont.deriveFont(versionFontSize));
//		g2.drawString(versionString, xCoordinate, yCoordinate);
				}
			}
		}
	}

	private String getVersionText(final FreeplaneVersion version) {
	    final String freeplaneNumber = version.numberToString();
		final String status = version.getType().toUpperCase();
		if("".equals(status))
			return freeplaneNumber;
		else{
			final String versionString = freeplaneNumber + " " + status;
			return versionString;
		}
	}

	@Override
	public void setVisible(final boolean b) {
		super.setVisible(b);
		if (b) {
			getRootPane().paintImmediately(0, 0, getWidth(), getHeight());
		}
	}
	
	static public void main(String[] args){
		ApplicationResourceController applicationResourceController = new ApplicationResourceController();
		Controller controller = new Controller(applicationResourceController);
		Controller.setCurrentController(controller);
		FreeplaneSplashModern freeplaneSplashModern = new FreeplaneSplashModern(null);
		freeplaneSplashModern.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				System.out.println("x = " + e.getX() + " y = " + e.getY());
				if(e.getClickCount() == 2)
					System.exit(0);
			}
		});
		freeplaneSplashModern.setVisible(true);
	}
}
