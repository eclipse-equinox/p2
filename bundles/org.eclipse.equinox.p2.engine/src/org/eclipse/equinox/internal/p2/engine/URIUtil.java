/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.io.File;
import java.net.*;

/**
 * A utility class for manipulating URIs. This class works around some of the
 * undesirable behavior of the {@link java.net.URI} class, and provides
 * additional path manipulation methods that are not available on the URI class.
 * <p>
 * Note: Class copied (and trimmed) from org.eclipse.equinox.common (URIUtil 1.18) 
 * in order to fix https://bugs.eclipse.org/342542.
 * 
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class URIUtil {

	private static final String UNC_PREFIX = "//"; //$NON-NLS-1$
	private static final String SCHEME_FILE = "file"; //$NON-NLS-1$

	private URIUtil() {
		// prevent instantiation
	}

	/**
	 * Ensures the given path string starts with exactly four leading slashes.
	 */
	private static String ensureUNCPath(String path) {
		int len = path.length();
		StringBuffer result = new StringBuffer(len);
		for (int i = 0; i < 4; i++) {
			// if we have hit the first non-slash character, add another leading
			// slash
			if (i >= len || result.length() > 0 || path.charAt(i) != '/')
				result.append('/');
		}
		result.append(path);
		return result.toString();
	}

	/**
	 * Returns whether the given URI refers to a local file system URI.
	 * 
	 * @param uri
	 *            The URI to check
	 * @return <code>true</code> if the URI is a local file system location, and
	 *         <code>false</code> otherwise
	 */
	private static boolean isFileURI(URI uri) {
		return SCHEME_FILE.equalsIgnoreCase(uri.getScheme());
	}

	/**
	 * Returns the URI as a local file, or <code>null</code> if the given URI
	 * does not represent a local file.
	 * 
	 * @param uri
	 *            The URI to return the file for
	 * @return The local file corresponding to the given URI, or
	 *         <code>null</code>
	 */
	public static File toFile(URI uri) {
		if (!isFileURI(uri))
			return null;
		// assume all illegal characters have been properly encoded, so use URI
		// class to unencode
		return new File(uri.getSchemeSpecificPart());
	}

	/**
	 * Returns the URL as a URI. This method will handle URLs that are not
	 * properly encoded (for example they contain unencoded space characters).
	 * 
	 * @param url
	 *            The URL to convert into a URI
	 * @return A URI representing the given URL
	 */
	public static URI toURI(URL url) throws URISyntaxException {
		// URL behaves differently across platforms so for file: URLs we parse
		// from string form
		if (SCHEME_FILE.equals(url.getProtocol())) {
			String pathString = url.toExternalForm().substring(5);
			// ensure there is a leading slash to handle common malformed URLs
			// such as file:c:/tmp
			if (pathString.indexOf('/') != 0)
				pathString = '/' + pathString;
			else if (pathString.startsWith(UNC_PREFIX) && !pathString.startsWith(UNC_PREFIX, 2)) {
				// URL encodes UNC path with two slashes, but URI uses four (see
				// bug 207103)
				pathString = ensureUNCPath(pathString);
			}
			return new URI(SCHEME_FILE, null, pathString, null);
		}
		try {
			return new URI(url.toExternalForm());
		} catch (URISyntaxException e) {
			// try multi-argument URI constructor to perform encoding
			return new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
		}
	}
}