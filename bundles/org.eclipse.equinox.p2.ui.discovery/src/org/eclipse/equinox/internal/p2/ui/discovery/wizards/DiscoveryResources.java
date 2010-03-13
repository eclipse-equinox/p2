/*******************************************************************************
 * Copyright (c) 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.discovery.wizards;

import java.net.URL;
import org.eclipse.equinox.internal.p2.discovery.AbstractCatalogSource;
import org.eclipse.equinox.internal.p2.discovery.model.Icon;
import org.eclipse.equinox.internal.p2.ui.discovery.DiscoveryImages;
import org.eclipse.equinox.internal.p2.ui.discovery.DiscoveryUi;
import org.eclipse.jface.resource.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

/**
 * @author Steffen Pingel
 */
public class DiscoveryResources {

	private final ResourceManager resourceManager;

	private final FontDescriptor h1FontDescriptor;

	private final Display display;

	private final FontDescriptor h2FontDescriptor;

	private final RGB disabledColorDescriptor;

	public DiscoveryResources(Display display) {
		this.display = display;
		this.resourceManager = new LocalResourceManager(JFaceResources.getResources(display));
		this.h1FontDescriptor = createFontDescriptor(SWT.BOLD, 1.35f);
		this.h2FontDescriptor = createFontDescriptor(SWT.BOLD, 1.25f);
		this.disabledColorDescriptor = new RGB(0x69, 0x69, 0x69);
	}

	public Cursor getHandCursor() {
		return display.getSystemCursor(SWT.CURSOR_HAND);
	}

	private FontDescriptor createFontDescriptor(int style, float heightMultiplier) {
		Font baseFont = JFaceResources.getDialogFont();
		FontData[] fontData = baseFont.getFontData();
		FontData[] newFontData = new FontData[fontData.length];
		for (int i = 0; i < newFontData.length; i++) {
			newFontData[i] = new FontData(fontData[i].getName(), (int) (fontData[i].getHeight() * heightMultiplier), fontData[i].getStyle() | style);
		}
		return FontDescriptor.createFrom(newFontData);
	}

	public void dispose() {
		resourceManager.dispose();
	}

	public Font getHeaderFont() {
		return resourceManager.createFont(h1FontDescriptor);
	}

	public Font getSmallHeaderFont() {
		return resourceManager.createFont(h2FontDescriptor);
	}

	public Image getInfoImage() {
		return resourceManager.createImage(DiscoveryImages.MESSAGE_INFO);
	}

	public Image getIconImage(AbstractCatalogSource discoverySource, Icon icon, int dimension, boolean fallback) {
		String imagePath;
		switch (dimension) {
			case 64 :
				imagePath = icon.getImage64();
				if (imagePath != null || !fallback) {
					break;
				}
			case 48 :
				imagePath = icon.getImage48();
				if (imagePath != null || !fallback) {
					break;
				}
			case 32 :
				imagePath = icon.getImage32();
				break;
			default :
				throw new IllegalArgumentException();
		}
		if (imagePath != null && imagePath.length() > 0) {
			URL resource = discoverySource.getResource(imagePath);
			if (resource != null) {
				ImageDescriptor descriptor = ImageDescriptor.createFromURL(resource);
				return resourceManager.createImage(descriptor);
			}
		}
		return null;
	}

	public Color getGradientBeginColor() {
		return DiscoveryUi.getCommonsColors().getGradientBegin();
	}

	public Color getGradientEndColor() {
		return DiscoveryUi.getCommonsColors().getGradientEnd();
	}

	public Color getColorDisabled() {
		return resourceManager.createColor(disabledColorDescriptor);
	}

	public Image getUpdateImage() {
		return resourceManager.createImage(DiscoveryImages.IU_UPDATABLE);
	}

}
