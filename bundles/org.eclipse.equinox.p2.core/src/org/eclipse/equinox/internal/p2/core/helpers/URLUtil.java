/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.core.helpers;

import java.io.File;
import java.net.URL;

/**
 * A utility class for manipulating URLs.
 */
public class URLUtil {
	/*
	 * Compares two URL for equality.
	 * Return false if one of them is null
	 */
	public static boolean sameURL(URL url1, URL url2) {
		if (url1 == url2)
			return true;
		if (url1 == null || url2 == null)
			return false;
		if (url1.equals(url2))
			return true;

		// check if we have two local file references that are case variants
		File file1 = toFile(url1);
		return file1 == null ? false : file1.equals(toFile(url2));
	}

	/**
	 * Returns the URL as a local file, or <code>null</code> if the given
	 * URL does not represent a local file.
	 * @param url The url to return the file file
	 * @return The local file corresponding to the given url, or <code>null</code>
	 */
	public static File toFile(URL url) {
		return "file".equalsIgnoreCase(url.getProtocol()) ? new File(url.getFile()) : null; //$NON-NLS-1$
	}
}
