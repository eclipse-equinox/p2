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
import java.net.*;
import org.eclipse.core.runtime.Assert;

/**
 * A utility class for manipulating URLs. This class works around some of the
 * broken behavior of the java.net.URL class.
 */
public class URLUtil {
	/**
	 * Returns the canonical form of the given URL. This eliminates extra slashes
	 * and converts local file system paths to canonical form for file: URLs. If any
	 * failure occurs while converting to canonical form the original URL is returned.
	 * @param location The location to convert to canonical form; must not be null
	 * @return The location in canonical form
	 */
	public static URL toCanonicalURL2(URL location) {
		Assert.isNotNull(location);
		File file = URLUtil.toFile(location);
		if (file != null) {
			try {
				return file.getCanonicalFile().toURL();
			} catch (Exception e) {
				//we made a best effort, just return the original location
				return location;
			}
		}
		//non-local URL, just remove trailing slash
		String external = location.toExternalForm();
		if (!external.endsWith("/")) //$NON-NLS-1$
			return location;
		try {
			return new URL(external.substring(0, external.length() - 1));
		} catch (MalformedURLException e) {
			//ignore and return original location
			return location;
		}
	}

	/*
	 * Compares two URL for equality.
	 * Return false if one of them is null
	 */
	public static boolean sameURL(URL url1, URL url2) {
		if (url1 == url2)
			return true;
		if (url1 == null || url2 == null)
			return false;
		try {
			if (toURI(url1).equals(toURI(url2)))
				return true;
		} catch (URISyntaxException e) {
			//fall through below
		}

		// check if we have two local file references that are case variants
		File file1 = toFile(url1);
		return file1 == null ? false : file1.equals(toFile(url2));
	}

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

	/**
	 * Returns the URL as a URI. This method will handle broken URLs that are
	 * not properly encoded (for example they contain unencoded space characters).
	 */
	public static URI toURI(URL url) throws URISyntaxException {
		try {
			return new URI(url.toExternalForm());
		} catch (URISyntaxException e) {
			//try multi-argument URI constructor to perform encoding
			return new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
		}
	}
}
