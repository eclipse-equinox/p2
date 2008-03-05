/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.update;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * 
 * @since 1.0
 */
public class Utils {
	public static boolean isWindows = System.getProperty("os.name").startsWith("Win"); //$NON-NLS-1$ //$NON-NLS-2$	
	private static boolean init = false;
	private static boolean useEnc = true;

	/*
	 * Copied from UpdateURLDecoder v1.4 in org.eclipse.update.configurator.
	 */
	public static String decode(String s, String enc) throws UnsupportedEncodingException {
		if (!init) {
			init = true;
			try {
				return URLDecoder.decode(s, enc);
			} catch (NoSuchMethodError e) {
				useEnc = false;
			}
		}
		return useEnc ? URLDecoder.decode(s, enc) : URLDecoder.decode(s);
	}

	/*
	 * Copied from Utils v1.32 in org.eclipse.update.configurator.
	 * 
	 * Ensures file: URLs on Windows have the right form (i.e. '/' as segment separator, drive letter in lower case, etc)
	 */
	public static String canonicalizeURL(String url) {
		if (!(isWindows && url.startsWith("file:"))) //$NON-NLS-1$
			return url;
		try {
			String path = new URL(url).getPath();
			// normalize to not have leading / so we can check the form
			File file = new File(path);
			path = file.toString().replace('\\', '/');
			// handle URLs that don't have a path
			if (path.length() == 0)
				return url;
			if (Character.isUpperCase(path.charAt(0))) {
				char[] chars = path.toCharArray();
				chars[0] = Character.toLowerCase(chars[0]);
				path = new String(chars);
				return new File(path).toURL().toExternalForm();
			}
		} catch (MalformedURLException e) {
			// default to original url
		}
		return url;
	}

	/*
	 * Return a boolean value indicating whether or not the given
	 * objects are considered equal.
	 */
	public static boolean equals(Object one, Object two) {
		return one == null ? two == null : one.equals(two);
	}

	/*
	 * Return a boolean value indicating whether or not the given
	 * lists are considered equal.
	 */
	public static boolean equals(Object[] one, Object[] two) {
		if (one == null && two == null)
			return true;
		if (one == null || two == null)
			return false;
		if (one.length != two.length)
			return false;
		for (int i = 0; i < one.length; i++) {
			boolean found = false;
			for (int j = 0; !found && j < two.length; j++)
				found = one[i].equals(two[j]);
			if (!found)
				return false;
		}
		return true;
	}

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
		IPath path = new Path(original);
		// ensure we have an absolute path to start with
		if (!path.isAbsolute())
			return original;

		//Returns the original string if no relativization has been done
		String result = makeRelative(path, new Path(rootPath));
		return path.toOSString().equals(result) ? original : result;
	}

	/*
	 * Make the given path relative to the specified root, if applicable. If not, then
	 * return the path as-is.
	 * 
	 * Method similar to one from SimpleConfigurationManipulatorImpl.
	 */
	private static String makeRelative(IPath toRel, IPath base) {
		int i = base.matchingFirstSegments(toRel);
		if (i == 0) {
			return toRel.toOSString();
		}
		String result = "";
		for (int j = 0; j < (base.segmentCount() - i); j++) {
			result += ".." + Path.SEPARATOR;
		}
		if (i == toRel.segmentCount())
			return ".";
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
		IPath path = new Path(original);
		// ensure we have a relative path to start with
		if (path.isAbsolute())
			return original;
		IPath root = new Path(rootPath);
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
