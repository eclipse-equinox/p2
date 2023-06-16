/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.update;

import java.io.File;
import java.net.URL;
import org.eclipse.core.runtime.*;

/**
 * Note the methods on this class have inconsistent and unintuitive behaviour. However
 * they are unchanged for backwards compatibility. Clients should really use
 * {@link URIUtil} instead.
 * @since 1.0
 */
public class PathUtil {
	public static boolean isWindows = System.getProperty("os.name").startsWith("Win"); //$NON-NLS-1$ //$NON-NLS-2$	

	private static final String FILE_PROTOCOL = "file:"; //$NON-NLS-1$

	public static String makeRelative(String urlString, URL rootURL) {
		// we only traffic in file: URLs
		int index = urlString.indexOf(FILE_PROTOCOL);
		if (index == -1)
			return urlString;
		index = index + 5;

		// ensure we have an absolute path to start with
		boolean done = false;
		URL url = null;
		String file = urlString;
		while (!done) {
			try {
				url = new URL(file);
				file = url.getFile();
			} catch (java.net.MalformedURLException e) {
				done = true;
			}
		}
		if (url == null || !new File(url.getFile()).isAbsolute())
			return urlString;

		String rootString = rootURL.toExternalForm();
		return urlString.substring(0, index) + makeRelative(urlString.substring(index), rootString.substring(rootString.indexOf(FILE_PROTOCOL) + 5));
	}

	public static String makeRelative(String original, String rootPath) {
		IPath path = IPath.fromOSString(original);
		// ensure we have an absolute path to start with
		if (!path.isAbsolute())
			return original;

		//Returns the original string if no relativization has been done
		String result = makeRelative(path, IPath.fromOSString(rootPath));
		return path.toOSString().equals(result) ? original : result;
	}

	/*
	 * Make the given path relative to the specified root, if applicable. If not, then
	 * return the path as-is.
	 */
	private static String makeRelative(IPath toRel, IPath base) {
		//can't make relative if devices are not equal
		final String device = toRel.getDevice();
		if (device != base.getDevice() && (device == null || !device.equalsIgnoreCase(base.getDevice())))
			return toRel.toOSString();
		int i = base.matchingFirstSegments(toRel);
		if (i == 0) {
			return toRel.toOSString();
		}
		String result = ""; //$NON-NLS-1$
		for (int j = 0; j < base.segmentCount() - i; j++) {
			result += ".." + IPath.SEPARATOR; //$NON-NLS-1$
		}
		if (i == toRel.segmentCount())
			return "."; //$NON-NLS-1$
		//TODO This will return mixed path with some / and some \ on windows!!
		result += toRel.setDevice(null).removeFirstSegments(i).toOSString();
		return result;
	}

	/*
	 * Make the given path absolute to the specified root, if applicable. If not, then
	 * return the path as-is.
	 * 
	 * Method similar to one from SimpleConfigurationManipulatorImpl.
	 */
	public static String makeAbsolute(String original, String rootPath) {
		IPath path = IPath.fromOSString(original);
		// ensure we have a relative path to start with
		if (path.isAbsolute())
			return original;
		IPath root = IPath.fromOSString(rootPath);
		return root.addTrailingSeparator().append(original.replace(':', '}')).toOSString().replace('}', ':');
	}

	public static String makeAbsolute(String urlString, URL rootURL) {
		// we only traffic in file: URLs
		int index = urlString.indexOf(FILE_PROTOCOL);
		if (index == -1)
			return urlString;
		index = index + 5;

		// ensure we have a relative path to start with
		boolean done = false;
		URL url = null;
		String file = urlString;
		while (!done) {
			try {
				url = new URL(file);
				file = url.getFile();
			} catch (java.net.MalformedURLException e) {
				done = true;
			}
		}
		if (url == null || new File(url.getFile()).isAbsolute())
			return urlString;

		return urlString.substring(0, index - 5) + makeAbsolute(urlString.substring(index), rootURL.toExternalForm());
	}
}
