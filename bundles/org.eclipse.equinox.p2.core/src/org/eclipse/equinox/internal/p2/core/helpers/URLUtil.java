/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *		compeople AG (Stefan Liebig) - Fix for bug 121201 - Poor performance behind proxy/firewall
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.core.helpers;

import java.io.File;
import java.net.URI;
import java.net.URL;

/**
 * A utility class for manipulating URLs. This class works around some of the
 * broken behavior of the java.net.URL class.
 */
public class URLUtil {

	/**
	 * Returns the URL as a local file, or <code>null</code> if the given
	 * URL does not represent a local file.
	 * @param url The url to return the file for
	 * @return The local file corresponding to the given url, or <code>null</code>
	 */
	public static File toFile(URL url) {
		try {
			if (!"file".equalsIgnoreCase(url.getProtocol())) //$NON-NLS-1$
				return null;
			//assume all illegal characters have been properly encoded, so use URI class to unencode
			return new File(new URI(url.toExternalForm()));
		} catch (Exception e) {
			//URL contains unencoded characters
			return new File(url.getFile());
		}
	}
}
