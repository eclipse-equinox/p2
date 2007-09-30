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
package org.eclipse.equinox.p2.core.helpers;

import java.io.File;
import java.net.URL;

public class Utils {
	/*
	 * Compares two URL for equality
	 * Return false if one of them is null
	 */
	public static boolean sameURL(URL url1, URL url2) {

		if (url1 == null || url2 == null)
			return false;
		if (url1 == url2)
			return true;
		if (url1.equals(url2))
			return true;

		// check if URL are file: URL as we may
		// have 2 URL pointing to the same featureReference
		// but with different representation
		// (i.e. file:/C;/ and file:C:/)
		if (!"file".equalsIgnoreCase(url1.getProtocol())) //$NON-NLS-1$
			return false;
		if (!"file".equalsIgnoreCase(url2.getProtocol())) //$NON-NLS-1$
			return false;

		File file1 = getFileFor(url1);//new File(url1.getFile());
		File file2 = getFileFor(url2);

		if (file1 == null)
			return false;

		return (file1.equals(file2));
	}

	/*
	 * Method getFileFor.
	 * @param url1
	 * @return File
	 */
	private static File getFileFor(URL url1) {
		return new File(url1.getFile());
	}
}
