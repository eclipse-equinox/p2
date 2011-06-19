/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others. All rights reserved.
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
import org.osgi.framework.Version;

public class SimpleConfiguratorUtils {

	private static final String UNC_PREFIX = "//";
	private static final String VERSION_PREFIX = "#version=";
	public static final String ENCODING_UTF8 = "#encoding=UTF-8";
	public static final Version COMPATIBLE_VERSION = new Version(1, 0, 0);

	private static final String FILE_SCHEME = "file";
	private static final String REFERENCE_PREFIX = "reference:";
	private static final String FILE_PREFIX = "file:";
	private static final String COMMA = ",";
	private static final String ENCODED_COMMA = "%2C";

	public static List readConfiguration(URL url, URI base) throws IOException {
		InputStream stream = null;
		try {
			stream = url.openStream();
		} catch (IOException e) {
			// if the exception is a FNF we return an empty bundle list
			if (e instanceof FileNotFoundException)
				return Collections.EMPTY_LIST;
			throw e;
		}

		try {
			return readConfiguration(stream, base);
		} finally {
			stream.close();
		}
	}

	/**
	 * Read the configuration from the given InputStream
	 * 
	 * @param stream - the stream is always closed 
	 * @param base
	 * @return List of {@link BundleInfo}
	 * @throws IOException
	 */
	public static List readConfiguration(InputStream stream, URI base) throws IOException {
		List bundles = new ArrayList();

		BufferedInputStream bufferedStream = new BufferedInputStream(stream);
		String encoding = determineEncoding(bufferedStream);
		BufferedReader r = new BufferedReader(encoding == null ? new InputStreamReader(bufferedStream) : new InputStreamReader(bufferedStream, encoding));

		String line;
		try {
			while ((line = r.readLine()) != null) {
				line = line.trim();
				//ignore any comment or empty lines
				if (line.length() == 0)
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

	/*
	 * We expect the first line of the bundles.info to be 
	 *    #encoding=UTF-8
	 * if it isn't, then it is an older bundles.info and should be 
	 * read with the default encoding
	 */
	private static String determineEncoding(BufferedInputStream stream) {
		byte[] utfBytes = ENCODING_UTF8.getBytes();
		byte[] buffer = new byte[utfBytes.length];

		int bytesRead = -1;
		stream.mark(utfBytes.length + 1);
		try {
			bytesRead = stream.read(buffer);
		} catch (IOException e) {
			//do nothing
		}

		if (bytesRead == utfBytes.length && Arrays.equals(utfBytes, buffer))
			return "UTF-8";

		//if the first bytes weren't the encoding, need to reset
		try {
			stream.reset();
		} catch (IOException e) {
			// nothing
		}
		return null;
	}

	public static void parseCommentLine(String line) {
		// version
		if (line.startsWith(VERSION_PREFIX)) {
			String version = line.substring(VERSION_PREFIX.length()).trim();
			if (!COMPATIBLE_VERSION.equals(new Version(version)))
				throw new IllegalArgumentException("Invalid version: " + version);
		}
	}

	public static BundleInfo parseBundleInfoLine(String line, URI base) {
		// symbolicName,version,location,startLevel,markedAsStarted
		StringTokenizer tok = new StringTokenizer(line, COMMA);
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
			//if the file is a UNC path, insert extra leading // if needed to make a valid URI (see bug 207103)
			if (scheme == null) {
				if (location.startsWith(UNC_PREFIX) && !location.startsWith(UNC_PREFIX, 2))
					location = UNC_PREFIX + location;
			} else {
				//insert UNC prefix after the scheme
				if (location.startsWith(UNC_PREFIX, colon + 1) && !location.startsWith(UNC_PREFIX, colon + 3))
					location = location.substring(0, colon + 3) + location.substring(colon + 1);
			}
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

	// This will produce an unencoded URL string
	public static String getBundleLocation(BundleInfo bundle, boolean useReference) {
		URI location = bundle.getLocation();
		String scheme = location.getScheme();
		String host = location.getHost();
		String path = location.getPath();

		if (location.getScheme() == null) {
			URI baseLocation = bundle.getBaseLocation();
			if (baseLocation != null && baseLocation.getScheme() != null) {
				scheme = baseLocation.getScheme();
				host = baseLocation.getHost();
			}
		}

		String bundleLocation = null;
		try {
			URL bundleLocationURL = new URL(scheme, host, path);
			bundleLocation = bundleLocationURL.toExternalForm();

		} catch (MalformedURLException e1) {
			bundleLocation = location.toString();
		}

		if (useReference && bundleLocation.startsWith(FILE_PREFIX))
			bundleLocation = REFERENCE_PREFIX + bundleLocation;
		return bundleLocation;
	}
}
