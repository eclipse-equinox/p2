/*******************************************************************************
 * Copyright (c) 2009, 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.discovery;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

/**
 * @author David Green
 * @author Steffen Pingel
 */
public class DiscoveryImages {

	private static final String T_OVR_32 = "ovr32"; //$NON-NLS-1$

	private static final String T_OBJ_16 = "obj16"; //$NON-NLS-1$

	private static final String T_WIZBAN = "wizban"; //$NON-NLS-1$

	private static final String T_TOOL = "etool16"; //$NON-NLS-1$

	private static final URL baseURL = Platform.getBundle(DiscoveryUi.ID_PLUGIN).getEntry("/icons/"); //$NON-NLS-1$

	/**
	 * image descriptor for a warning overlay suitable for use with 32x32 images.
	 */
	public static final ImageDescriptor OVERLAY_WARNING_32 = create(T_OVR_32, "message_warning.gif"); //$NON-NLS-1$

	public static final ImageDescriptor BANNER_DISOVERY = create(T_WIZBAN, "banner-discovery.png"); //$NON-NLS-1$

	public static final ImageDescriptor IU_AVAILABLE = create(T_OBJ_16, "iu_disabled_obj.gif"); //$NON-NLS-1$

	public static final ImageDescriptor IU_INSTALLED = create(T_OBJ_16, "iu_obj.gif"); //$NON-NLS-1$

	public static final ImageDescriptor IU_UPDATABLE = create(T_OBJ_16, "iu_update_obj.gif"); //$NON-NLS-1$

	public static final ImageDescriptor MESSAGE_INFO = create(T_OBJ_16, "message_info.gif"); //$NON-NLS-1$

	public static final ImageDescriptor FIND_CLEAR = create(T_TOOL, "find-clear.gif"); //$NON-NLS-1$

	public static final ImageDescriptor FIND_CLEAR_DISABLED = create(T_TOOL, "find-clear-disabled.gif"); //$NON-NLS-1$

	private static ImageRegistry imageRegistry;

	private static ImageDescriptor create(String prefix, String name) {
		try {
			return ImageDescriptor.createFromURL(makeIconFileURL(prefix, name));
		} catch (MalformedURLException e) {
			return ImageDescriptor.getMissingImageDescriptor();
		}
	}

	private static URL makeIconFileURL(String prefix, String name) throws MalformedURLException {
		if (baseURL == null) {
			throw new MalformedURLException();
		}

		StringBuilder buffer = new StringBuilder(prefix);
		buffer.append('/');
		buffer.append(name);
		return new URL(baseURL, buffer.toString());
	}

	/**
	 * Lazily initializes image map.
	 * 
	 * @param imageDescriptor
	 * @return Image
	 */
	public static Image getImage(ImageDescriptor imageDescriptor) {
		ImageRegistry imageRegistry = getImageRegistry();
		Image image = imageRegistry.get("" + imageDescriptor.hashCode()); //$NON-NLS-1$
		if (image == null) {
			image = imageDescriptor.createImage(true);
			imageRegistry.put("" + imageDescriptor.hashCode(), image); //$NON-NLS-1$
		}
		return image;
	}

	private static ImageRegistry getImageRegistry() {
		if (imageRegistry == null) {
			imageRegistry = new ImageRegistry();
		}
		return imageRegistry;
	}

}
