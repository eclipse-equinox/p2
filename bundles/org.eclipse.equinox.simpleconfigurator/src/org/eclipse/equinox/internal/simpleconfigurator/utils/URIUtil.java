package org.eclipse.equinox.internal.simpleconfigurator.utils;

import java.io.File;
import java.net.*;

public class URIUtil {

	private static final String SCHEME_FILE = "file"; //$NON-NLS-1$

	/**
	 * Returns a URI corresponding to the given unencoded string.
	 * @throws URISyntaxException If the string cannot be formed into a valid URI
	 */
	public static URI fromString(String uriString) throws URISyntaxException {
		int colon = uriString.indexOf(':');
		int hash = uriString.lastIndexOf('#');
		boolean noHash = hash < 0;
		if (noHash)
			hash = uriString.length();
		String scheme = colon < 0 ? null : uriString.substring(0, colon);
		String ssp = uriString.substring(colon + 1, hash);
		String fragment = noHash ? null : uriString.substring(hash + 1);
		//use java.io.File for constructing file: URIs
		if (scheme != null && scheme.equals(SCHEME_FILE)) {
			File file = new File(uriString.substring(5));
			if (file.isAbsolute())
				return file.toURI();
			scheme = null;
			if (File.separatorChar != '/')
				ssp = ssp.replace(File.separatorChar, '/');
		}
		return new URI(scheme, ssp, fragment);
	}

	/*
	 * Compares two URI for equality.
	 * Return false if one of them is null
	 */
	public static boolean sameURI(URI url1, URI url2) {
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
	 * Returns the URI as a local file, or <code>null</code> if the given
	 * URI does not represent a local file.
	 * @param uri The URI to return the file for
	 * @return The local file corresponding to the given URI, or <code>null</code>
	 */
	public static File toFile(URI uri) {
		try {
			if (!SCHEME_FILE.equalsIgnoreCase(uri.getScheme()))
				return null;
			//assume all illegal characters have been properly encoded, so use URI class to unencode
			return new File(uri);
		} catch (IllegalArgumentException e) {
			//File constructor does not support non-hierarchical URI
			String path = uri.getPath();
			//path is null for non-hierarchical URI such as file:c:/tmp
			if (path == null)
				path = uri.getSchemeSpecificPart();
			return new File(path);
		}
	}

	/**
	 * Returns the URL as a URI. This method will handle broken URLs that are
	 * not properly encoded (for example they contain unencoded space characters).
	 */
	public static URI toURI(URL url) throws URISyntaxException {
		//URL behaves differently across platforms so for file: URLs we parse from string form
		if (SCHEME_FILE.equals(url.getProtocol())) {
			String pathString = url.toExternalForm().substring(5);
			//ensure there is a leading slash to handle common malformed URLs such as file:c:/tmp
			if (pathString.indexOf('/') != 0)
				pathString = '/' + pathString;
			return new URI(SCHEME_FILE, pathString, null);
		}
		try {
			return new URI(url.toExternalForm());
		} catch (URISyntaxException e) {
			//try multi-argument URI constructor to perform encoding
			return new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
		}
	}

	/**
	 * Returns a URI as a URL.
	 * 
	 * @throws MalformedURLException 
	 */
	public static URL toURL(URI uri) throws MalformedURLException {
		return new URL(uri.toString());
	}
}
