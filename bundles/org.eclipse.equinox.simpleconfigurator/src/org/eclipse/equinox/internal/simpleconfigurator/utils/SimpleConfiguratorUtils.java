/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.simpleconfigurator.utils;

import java.io.*;
import java.net.*;
import java.util.*;

public class SimpleConfiguratorUtils {

	private static final String VERSION_PREFIX = "#version=";
	private static final String VERSION_1 = "1";
	private static final String FILE_SCHEME = "file";
	private static final String REFERENCE_PREFIX = "reference:";
	private static final String FILE_PREFIX = "file:";
	private static final String COMMA = ",";
	private static final String ENCODED_COMMA = "%2C";

	public static List readConfiguration(URL url, URI base) throws IOException {
		List bundles = new ArrayList();
		BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream()));
		try {
			String line;
			while ((line = r.readLine()) != null) {
				line = line.trim();
				//ignore any comment or empty lines
				if (line.length() == 0) //$NON-NLS-1$
					continue;

				if (line.startsWith("#")) {//$NON-NLS-1$
					parseCommentLine(line);
					continue;
				}

				BundleInfo bundleInfo = parseBundleInfoLine(line, base);
				if (bundleInfo != null)
					bundles.add(bundleInfo);
			}
		} finally {
			try {
				r.close();
			} catch (IOException ex) {
				// ignore
			}
		}
		return bundles;
	}

	public static void parseCommentLine(String line) {
		// version
		if (line.startsWith(VERSION_PREFIX)) {
			String version = line.substring(VERSION_PREFIX.length());
			if (!VERSION_1.equals(version))
				throw new IllegalArgumentException("Invalid version: " + version);
		}
	}

	public static BundleInfo parseBundleInfoLine(String line, URI base) {
		// symbolicName,version,location,startLevel,markedAsStarted
		StringTokenizer tok = new StringTokenizer(line, COMMA); //$NON-NLS-1$
		int numberOfTokens = tok.countTokens();
		if (numberOfTokens < 5)
			throw new IllegalArgumentException("Line does not contain at least 5 tokens: " + line);

		String symbolicName = tok.nextToken().trim();
		String version = tok.nextToken().trim();
		URI location = parseLocation(tok.nextToken().trim());
		int startLevel = Integer.parseInt(tok.nextToken().trim());
		boolean markedAsStarted = Boolean.valueOf(tok.nextToken()).booleanValue();
		BundleInfo result = new BundleInfo(symbolicName, version, location, startLevel, markedAsStarted);
		if (!location.isAbsolute())
			result.setBaseLocation(base);
		return result;
	}

	public static URI parseLocation(String location) {
		// decode any commas we previously encoded when writing this line
		int encodedCommaIndex = location.indexOf(ENCODED_COMMA);
		while (encodedCommaIndex != -1) {
			location = location.substring(0, encodedCommaIndex) + COMMA + location.substring(encodedCommaIndex + 3);
			encodedCommaIndex = location.indexOf(ENCODED_COMMA);
		}

		if (File.separatorChar != '/') {
			int colon = location.indexOf(':');
			String scheme = colon < 0 ? null : location.substring(0, colon);
			if (scheme == null || scheme.equals(FILE_SCHEME))
				location = location.replace(File.separatorChar, '/');
		}

		try {
			URI uri = new URI(location);
			if (!uri.isOpaque())
				return uri;
		} catch (URISyntaxException e1) {
			// this will catch the use of invalid URI characters (e.g. spaces, etc.)
			// ignore and fall through
		}

		try {
			return URIUtil.fromString(location);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid location: " + location);
		}
	}

	public static void transferStreams(InputStream source, OutputStream destination) throws IOException {
		source = new BufferedInputStream(source);
		destination = new BufferedOutputStream(destination);
		try {
			byte[] buffer = new byte[8192];
			while (true) {
				int bytesRead = -1;
				if ((bytesRead = source.read(buffer)) == -1)
					break;
				destination.write(buffer, 0, bytesRead);
			}
		} finally {
			try {
				source.close();
			} catch (IOException e) {
				// ignore
			}
			try {
				destination.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	public static String getBundleLocation(BundleInfo bundle, boolean useReference) {
		String bundleLocation = null;
		URI location = bundle.getLocation();
		try {
			if (location.getScheme() != null)
				bundleLocation = URIUtil.toURL(location).toExternalForm();
			else {
				URI baseLocation = bundle.getBaseLocation();
				if (baseLocation != null && baseLocation.getScheme() != null) {
					String scheme = baseLocation.getScheme();
					String host = baseLocation.getHost();
					String path = location.getPath();
					URL bundleLocationURL = new URL(scheme, host, path);
					bundleLocation = bundleLocationURL.toExternalForm();
				}
			}
		} catch (MalformedURLException e1) {
			// fall through
		}
		if (bundleLocation == null)
			bundleLocation = location.toString();

		if (useReference && bundleLocation.startsWith(FILE_PREFIX)) //$NON-NLS-1$
			bundleLocation = REFERENCE_PREFIX + bundleLocation; //$NON-NLS-1$
		return bundleLocation;
	}
}
