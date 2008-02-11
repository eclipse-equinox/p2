/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.frameworkadmin.equinox.utils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.equinox.internal.frameworkadmin.equinox.EquinoxConstants;
import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;
import org.eclipse.equinox.internal.provisional.frameworkadmin.Manipulator;

public class FileUtils {

	public static String getEclipseRealLocation(final Manipulator manipulator, final String location) {
		try {
			new URL(location);
			return location;
		} catch (MalformedURLException e) {
			// just ignore.
		}
		if (location.indexOf(":") >= 0)
			return location;

		LauncherData launcherData = manipulator.getLauncherData();
		File home = launcherData.getHome();
		File pluginsDir = null;
		if (home != null)
			pluginsDir = new File(home, EquinoxConstants.PLUGINS_DIR);
		else if (launcherData.getLauncher() != null)
			pluginsDir = new File(launcherData.getLauncher().getParentFile(), EquinoxConstants.PLUGINS_DIR);
		else if (launcherData.getFwJar() != null)
			pluginsDir = launcherData.getFwJar().getParentFile();
		String pluginName = getPluginName(location);
		String ret = getEclipsePluginFullLocation(pluginName, pluginsDir);
		return ret;
	}

	private static String getPluginName(final String location) {
		int position = location.indexOf("_");
		String pluginName = location;
		if (position >= 0)
			pluginName = location.substring(0, position);
		return pluginName;
	}

	public static String getRealLocation(Manipulator manipulator, final String location, boolean useEclipse) {
		if (location == null)
			return null;
		String ret = location;
		if (location.startsWith("reference:")) {
			ret = location.substring("reference:".length());
			if (ret.endsWith(".jar/")) {
				ret = ret.substring(0, ret.length() - "/".length());
				if (ret.startsWith("file:"))
					ret = ret.substring("file:".length());
			}
		}
		if (location.startsWith("initial@"))
			ret = location.substring("initial@".length());

		if (ret == location)
			return useEclipse ? FileUtils.getEclipseRealLocation(manipulator, location) : location;
		return getRealLocation(manipulator, ret, useEclipse);
	}

	private static String replaceAll(String st, String oldSt, String newSt) {
		if (oldSt.equals(newSt))
			return st;
		int index = -1;
		while ((index = st.indexOf(oldSt)) != -1) {
			st = st.substring(0, index) + newSt + st.substring(index + oldSt.length());
		}
		return st;
	}

	/**
	 * If a bundle of the specified location is in the Eclipse plugin format (either plugin-name_version.jar 
	 * or as a folder named plugin-name_version ), return version string.Otherwise, return null;
	 * 
	 * @param url
	 * @param pluginName
	 * @return version string. If invalid format, return null. 
	 */
	private static String getEclipseNamingVersion(URL url, final String pluginName, boolean isFile) {
		String location = url.getFile();
		location = replaceAll(location, File.separator, "/");
		String filename = null;
		if (location.indexOf(":") == -1)
			filename = location;
		else
			filename = location.substring(location.lastIndexOf(":") + 1);

		// filename must be "jarName"_"version".jar
		if (isFile) {
			if (!filename.endsWith(".jar"))
				return null;
			filename = filename.substring(0, filename.lastIndexOf(".jar"));
		} else {
			// directory - remove trailing slash
			filename = filename.substring(0, filename.length() - 1);
		}

		if (filename.indexOf("/") != -1)
			filename = filename.substring(filename.lastIndexOf("/") + 1);

		if (!filename.startsWith(pluginName))
			return null;

		int pluginnameLength = pluginName.length();
		if (filename.length() <= pluginnameLength || filename.charAt(pluginName.length()) != '_')
			return null;

		return filename.substring(pluginnameLength + 1);
	}

	public static String getEclipsePluginFullLocation(String pluginName, File bundlesDir) {
		File[] lists = bundlesDir.listFiles();
		URL ret = null;
		EclipseVersion maxVersion = null;
		if (lists == null)
			return null;

		for (int i = 0; i < lists.length; i++) {
			try {
				URL url = lists[i].toURL();
				String version = getEclipseNamingVersion(url, pluginName, lists[i].isFile());
				if (version != null) {
					EclipseVersion eclipseVersion = new EclipseVersion(version);
					if (maxVersion == null || eclipseVersion.compareTo(maxVersion) > 0) {
						ret = url;
						maxVersion = eclipseVersion;
					}
				}
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}
		return (ret == null ? null : ret.toExternalForm());
	}
}
