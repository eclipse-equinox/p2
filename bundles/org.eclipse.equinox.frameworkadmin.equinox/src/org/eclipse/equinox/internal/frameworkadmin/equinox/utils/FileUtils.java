/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Pascal Rapicault - Support for bundled macosx http://bugs.eclipse.org/57349
 *******************************************************************************/
package org.eclipse.equinox.internal.frameworkadmin.equinox.utils;

import java.io.*;
import java.net.*;
import java.util.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.frameworkadmin.equinox.EquinoxConstants;
import org.eclipse.equinox.internal.frameworkadmin.equinox.ParserUtils;
import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;
import org.eclipse.osgi.service.environment.Constants;
import org.osgi.framework.Version;

public class FileUtils {
	private static String FILE_SCHEME = "file"; //$NON-NLS-1$
	private static String FILE_PROTOCOL = "file:"; //$NON-NLS-1$
	private static String REFERENCE_PROTOCOL = "reference:"; //$NON-NLS-1$
	private static String INITIAL_PREFIX = "initial@"; //$NON-NLS-1$

	// based on org.eclipse.core.runtime.adaptor.EclipseStarter#searchForBundle
	public static URI getEclipseRealLocation(Manipulator manipulator, String location) {
		// if this is some form of URL just return it
		try {
			new URL(location);
			return URIUtil.makeAbsolute(new URI(location),
					ParserUtils
							.getOSGiInstallArea(Arrays.asList(manipulator.getLauncherData().getProgramArgs()),
									manipulator.getConfigData().getProperties(), manipulator.getLauncherData())
							.toURI());
		} catch (URISyntaxException e) {
			// expected
		} catch (MalformedURLException e) {
			// expected
		}

		File base = new File(location);
		if (!base.isAbsolute()) {
			String pluginsDir = getSysPath(manipulator);
			if (pluginsDir == null)
				return null;
			base = new File(pluginsDir, location);
		}

		return getEclipsePluginFullLocation(base.getName(), base.getParentFile());
	}

	// This mimics the logic of EclipseStarter#getSysPath();
	private static String getSysPath(final Manipulator manipulator) {
		Properties properties = manipulator.getConfigData().getProperties();
		String path = (String) properties.get(EquinoxConstants.PROP_OSGI_SYSPATH);
		if (path != null)
			return path;
		path = (String) properties.get(EquinoxConstants.PROP_OSGI_FW);
		if (path != null) {
			if (path.startsWith(FILE_PROTOCOL))
				path = path.substring(FILE_PROTOCOL.length());
			File file = new File(path);
			return file.getParentFile().getAbsolutePath();
		}

		LauncherData launcherData = manipulator.getLauncherData();
		File home = launcherData.getHome();
		File pluginsDir = null;
		if (home != null)
			pluginsDir = new File(home, EquinoxConstants.PLUGINS_DIR);
		else if (launcherData.getFwJar() != null)
			pluginsDir = launcherData.getFwJar().getParentFile();
		else if (launcherData.getLauncher() != null) {
			File launcherDir = null;
			if (Constants.OS_MACOSX.equals(launcherData.getOS())) {
				IPath launcherPath = IPath.fromOSString(launcherData.getLauncher().getAbsolutePath());
				if (launcherPath.segmentCount() > 2) {
					launcherPath = launcherPath.removeLastSegments(2).append("Eclipse"); //$NON-NLS-1$
					launcherDir = launcherPath.toFile();
				}
			} else
				launcherDir = launcherData.getLauncher().getParentFile();
			pluginsDir = new File(launcherDir, EquinoxConstants.PLUGINS_DIR);
		}
		if (pluginsDir != null)
			return pluginsDir.getAbsolutePath();
		return null;
	}

	public static String removeEquinoxSpecificProtocols(String location) {
		if (location == null)
			return null;
		String ret = location;
		if (location.startsWith(REFERENCE_PROTOCOL))
			ret = location.substring(REFERENCE_PROTOCOL.length());
		else if (location.startsWith(INITIAL_PREFIX))
			ret = location.substring(INITIAL_PREFIX.length());
		return ret;
	}

	public static URI getRealLocation(Manipulator manipulator, final String location) {
		return FileUtils.getEclipseRealLocation(manipulator, removeEquinoxSpecificProtocols(location));
	}

	/**
	 * If a bundle of the specified location is in the Eclipse plugin format (either
	 * plugin-name_version.jar or as a folder named plugin-name_version ), return
	 * version string.Otherwise, return null;
	 *
	 * @return version string. If invalid format, return null.
	 */
	private static Version getVersion(String version) {
		if (version.length() == 0)
			return Version.emptyVersion;

		if (version.endsWith(".jar")) //$NON-NLS-1$
			version = version.substring(0, version.length() - 4);

		try {
			return new Version(version);
		} catch (IllegalArgumentException e) {
			// bad format
			return null;
		}
	}

	/**
	 * Find the named plugin in the given bundlesDir
	 * 
	 * @param pluginName
	 * @param bundlesDir
	 * @return a URL string for the found plugin, or null
	 */
	// Based on org.eclipse.core.runtime.adaptor.EclipseStarter#searchFor
	public static URI getEclipsePluginFullLocation(String pluginName, File bundlesDir) {
		if (bundlesDir == null)
			return null;
		File[] candidates = bundlesDir.listFiles();
		if (candidates == null)
			return null;

		File result = null;
		Version maxVersion = null;

		for (File candidate : candidates) {
			String candidateName = candidate.getName();
			if (!candidateName.startsWith(pluginName))
				continue;
			if (candidateName.length() > pluginName.length() && candidateName.charAt(pluginName.length()) != '_') {
				// allow jar file with no _version tacked on the end
				if (!candidate.isFile() || (candidateName.length() != 4 + pluginName.length())
						|| !candidateName.endsWith(".jar")) { //$NON-NLS-1$
					continue;
				}
			}
			String candidateVersion = ""; //$NON-NLS-1$
			if (candidateName.length() > pluginName.length() + 1 && candidateName.charAt(pluginName.length()) == '_')
				candidateVersion = candidateName.substring(pluginName.length() + 1);
			Version currentVersion = getVersion(candidateVersion);
			if (currentVersion == null)
				continue;
			if (maxVersion == null || maxVersion.compareTo(currentVersion) < 0) {
				maxVersion = currentVersion;
				result = candidate;
			}
		}
		return result != null ? result.getAbsoluteFile().toURI() : null;
	}

	public static URI fromPath(String path) throws URISyntaxException {
		if (path.startsWith(FILE_PROTOCOL)) {
			try {
				return new URI(path);
			} catch (URISyntaxException e) {
				path = path.substring(FILE_PROTOCOL.length() + 1);
			}
		}

		File f = new File(path);
		if (f.isAbsolute())
			return f.toURI();
		return URIUtil.fromString(FILE_PROTOCOL + path);
	}

	public static String toPath(URI uri) {
		if (!FILE_SCHEME.equalsIgnoreCase(uri.getScheme()))
			return new File(URIUtil.toUnencodedString(uri)).getPath();
		return URIUtil.toFile(uri).getAbsolutePath();
	}

	public static String toFileURL(URI uri) {
		if (uri.getScheme() != null)
			return URIUtil.toUnencodedString(uri);
		return FILE_PROTOCOL + URIUtil.toUnencodedString(uri);
	}

	public static URI fromFileURL(String url) throws URISyntaxException {
		if (url.startsWith(FILE_PROTOCOL)) {
			return URIUtil.fromString(new File(url.substring(FILE_PROTOCOL.length())).isAbsolute() ? url
					: url.substring(FILE_PROTOCOL.length()));
		}
		throw new URISyntaxException(url, "Not a file url"); //$NON-NLS-1$
	}

	/**
	 * Loads an ini file, returning a list of all non-blank lines in the file. Like
	 * eclipseConfig.c/readConfigFile() comment lines ('#' its first character) are
	 * skipped too.
	 */
	public static List<String> loadFile(File file) throws IOException {
		try (BufferedReader br = new BufferedReader(new FileReader(file));) {
			String line;
			List<String> list = new ArrayList<>();
			while ((line = br.readLine()) != null) {
				// skip whitespace and comments
				String stripped = line.strip();
				if (!stripped.isEmpty() && !line.startsWith("#")) {//$NON-NLS-1$
					list.add(line);
				}
			}
			return list;
		}
	}

}
