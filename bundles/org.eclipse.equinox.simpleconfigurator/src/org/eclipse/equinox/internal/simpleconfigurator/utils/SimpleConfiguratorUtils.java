/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * 		IBM Corporation - initial API and implementation
 * 		Red Hat, Inc (Krzysztof Daniel) - Bug 421935: Extend simpleconfigurator to
 * read .info files from many locations
 ******************************************************************************/
package org.eclipse.equinox.internal.simpleconfigurator.utils;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.*;
import org.eclipse.equinox.internal.simpleconfigurator.Activator;
import org.osgi.framework.Version;

public class SimpleConfiguratorUtils {

	private static final String LINK_KEY = "link";
	private static final String LINK_FILE_EXTENSION = ".link";
	private static final String UNC_PREFIX = "//";
	private static final String VERSION_PREFIX = "#version=";
	public static final String ENCODING_UTF8 = "#encoding=UTF-8";
	public static final Version COMPATIBLE_VERSION = new Version(1, 0, 0);

	private static final String FILE_SCHEME = "file";
	private static final String REFERENCE_PREFIX = "reference:";
	private static final String FILE_PREFIX = "file:";
	private static final String COMMA = ",";
	private static final String ENCODED_COMMA = "%2C";

	private static final Set<File> reportedExtensions = Collections.synchronizedSet(new HashSet<>(0));

	public static List<BundleInfo> readConfiguration(URL url, URI base) throws IOException {
		List<BundleInfo> result = new ArrayList<>();

		//old behaviour
		result.addAll(readConfigurationFromFile(url, base));

		if (!Activator.EXTENDED) {
			return result;
		}
		readExtendedConfigurationFiles(result);
		//dedup - some bundles may be listed more than once
		removeDuplicates(result);
		return result;
	}

	public static void removeDuplicates(List<BundleInfo> result) {
		if (result.size() > 1) {
			int index = 0;
			while (index < result.size()) {
				String aSymbolicName = result.get(index).getSymbolicName();
				String aVersion = result.get(index).getVersion();

				for (int i = index + 1; i < result.size();) {
					String bSymbolicName = result.get(i).getSymbolicName();
					String bVersion = result.get(i).getVersion();
					if (aSymbolicName.equals(bSymbolicName) && aVersion.equals(bVersion)) {
						result.remove(i);
					} else {
						i++;
					}
				}

				index++;
			}
		}
	}

	public static void readExtendedConfigurationFiles(List<BundleInfo> result) throws IOException, FileNotFoundException, MalformedURLException {
		//extended behaviour
		List<File> files;
		try {
			files = getInfoFiles();
			for (File info : files) {
				List<BundleInfo> list = readConfigurationFromFile(info.toURL(), info.getParentFile().toURI());
				// extensions are relative to extension root, not to the framework
				// it is necessary to replace relative locations with absolute ones
				for (int i = 0; i < list.size(); i++) {
					BundleInfo singleInfo = list.get(i);
					if (singleInfo.getBaseLocation() != null) {
						singleInfo = new BundleInfo(singleInfo.getSymbolicName(), singleInfo.getVersion(), singleInfo.getBaseLocation().resolve(singleInfo.getLocation()), singleInfo.getStartLevel(), singleInfo.isMarkedAsStarted());
						list.remove(i);
						list.add(i, singleInfo);
					}
				}
				if (Activator.DEBUG) {
					System.out.println("List of bundles to be loaded from " + info.toURL());
					for (BundleInfo b : list) {
						System.out.println(b.getSymbolicName() + "_" + b.getVersion());
					}
				}
				result.addAll(list);
			}
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Couldn't parse simpleconfigurator extensions", e);
		}
	}

	public static ArrayList<File> getInfoFiles() throws IOException, FileNotFoundException, URISyntaxException {
		ArrayList<File> files = new ArrayList<>(1);

		if (Activator.EXTENSIONS != null) {
			//configured simpleconfigurator extensions location
			String stringExtenionLocation = Activator.EXTENSIONS;
			String[] locationToCheck = stringExtenionLocation.split(",");
			for (String location : locationToCheck) {
				files.addAll(getInfoFilesFromLocation(location));
			}
		}
		return files;
	}

	private static ArrayList<File> getInfoFilesFromLocation(String locationToCheck) throws IOException, FileNotFoundException, URISyntaxException {
		ArrayList<File> result = new ArrayList<>(1);

		File extensionsLocation = new File(locationToCheck);

		if (extensionsLocation.exists() && extensionsLocation.isDirectory()) {
			//extension location contains extensions
			File[] extensions = extensionsLocation.listFiles();
			for (File extension : extensions) {
				if (extension.isFile() && extension.getName().endsWith(LINK_FILE_EXTENSION)) {
					Properties link = new Properties();
					try (FileInputStream inStream = new FileInputStream(extension)) {
						link.load(inStream);
					}
					String newInfoName = link.getProperty(LINK_KEY);
					URI newInfoURI = new URI(newInfoName);
					File newInfoFile = null;
					if (newInfoURI.isAbsolute()) {
						newInfoFile = new File(newInfoName);
					} else {
						newInfoFile = new File(extension.getParentFile(), newInfoName);
					}
					if (newInfoFile.exists()) {
						extension = newInfoFile.getParentFile();
					}
				}

				if (extension.isDirectory()) {
					if (Files.isWritable(extension.toPath())) {
						synchronized (reportedExtensions) {
							if (!reportedExtensions.contains(extension)) {
								reportedExtensions.add(extension);
								System.err.println("Fragment directory should be read only " + extension);
							}
						}
						continue;
					}
					File[] listFiles = extension.listFiles();
					// new magic - multiple info files, f.e.
					//   egit.info (git feature)
					//   cdt.link (properties file containing link=path) to other info file
					for (File file : listFiles) {
						//if it is a info file - load it
						if (file.getName().endsWith(".info")) {
							result.add(file);
						}
						// if it is a link - dereference it
					}
				} else if (Activator.DEBUG) {
					synchronized (reportedExtensions) {
						if (!reportedExtensions.contains(extension)) {
							reportedExtensions.add(extension);
							System.out.println("Unrecognized fragment " + extension);
						}
					}
				}
			}
		}
		return result;
	}

	private static List<BundleInfo> readConfigurationFromFile(URL url, URI base) throws IOException {
		InputStream stream = null;
		try {
			stream = url.openStream();
		} catch (IOException e) {
			// if the exception is a FNF we return an empty bundle list
			if (e instanceof FileNotFoundException)
				return Collections.emptyList();
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
	public static List<BundleInfo> readConfiguration(InputStream stream, URI base) throws IOException {
		List<BundleInfo> bundles = new ArrayList<>();

		BufferedInputStream bufferedStream = new BufferedInputStream(stream);
		String encoding = determineEncoding(bufferedStream);

		String line;
		try (BufferedReader r = new BufferedReader(encoding == null ? new InputStreamReader(bufferedStream) : new InputStreamReader(bufferedStream, encoding));) {
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
		boolean markedAsStarted = Boolean.parseBoolean(tok.nextToken());
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

	public static void transferStreams(List<InputStream> sources, OutputStream destination) throws IOException {
		destination = new BufferedOutputStream(destination);
		try {
			for (InputStream source : sources) {
				try (InputStream bufferedSource = new BufferedInputStream(source)) {
					byte[] buffer = new byte[8192];
					while (true) {
						int bytesRead = -1;
						if ((bytesRead = bufferedSource.read(buffer)) == -1)
							break;
						destination.write(buffer, 0, bytesRead);
					}
				}
			}
		} finally {
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

	public static long getExtendedTimeStamp() {
		long regularTimestamp = -1;
		if (Activator.EXTENDED) {
			try {
				ArrayList<File> infoFiles = SimpleConfiguratorUtils.getInfoFiles();
				for (File f : infoFiles) {
					long infoFileLastModified = getFileLastModified(f);
					// pick latest modified always
					if (infoFileLastModified > regularTimestamp) {
						regularTimestamp = infoFileLastModified;
					}
				}
			} catch (IOException | URISyntaxException e) {
				if (Activator.DEBUG) {
					e.printStackTrace();
				}
			}
			if (Activator.DEBUG) {
				System.out.println("Fragments timestamp: " + regularTimestamp);
			}
		}
		return regularTimestamp;
	}

	public static long getFileLastModified(File file) {
		long lastModified = file.lastModified();
		if (lastModified == 0) {
			try {
				// Note that "ctime" is different to a file's creation time (on posix
				// platforms creation time is a synonym for last modified time)
				FileTime ctime = (FileTime) Files.getAttribute(file.toPath(), "unix:ctime");
				lastModified = ctime.toMillis();
			} catch (UnsupportedOperationException | IllegalArgumentException | IOException e) {
				// We expect this attribute to not exist on non-posix platforms like Windows
			}
		}
		return lastModified;
	}
}
