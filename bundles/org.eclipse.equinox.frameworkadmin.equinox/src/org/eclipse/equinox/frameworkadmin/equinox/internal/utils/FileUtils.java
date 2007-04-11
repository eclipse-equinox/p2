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
package org.eclipse.equinox.frameworkadmin.equinox.internal.utils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;

import org.eclipse.equinox.frameworkadmin.LauncherData;
import org.eclipse.equinox.frameworkadmin.Manipulator;
import org.eclipse.equinox.frameworkadmin.equinox.internal.EquinoxConstants;
import org.eclipse.equinox.internal.frameworkadmin.utils.Utils;

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
			if (useEclipse)
				return FileUtils.getEclipseRealLocation(manipulator, location);
			else
				return location;
		return getRealLocation(manipulator, ret, useEclipse);
	}

	public static boolean copy(File source, File target) throws IOException {
		//try {
		target.getParentFile().mkdirs();
		target.createNewFile();
		transferStreams(new FileInputStream(source), new FileOutputStream(target));
		//		} catch (FileNotFoundException e) {
		//			e.printStackTrace();
		//			return false;
		//		} catch (IOException e) {
		//			e.printStackTrace();
		//			return false;
		//		}
		return true;
	}

	/**
	 * Transfers all available bytes from the given input stream to the given
	 * output stream. Regardless of failure, this method closes both streams.
	 * 
	 * @param source
	 * @param destination
	 * @throws IOException
	 */
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
				e.printStackTrace();
				// ignore
			}
			try {
				destination.close();
			} catch (IOException e) {
				e.printStackTrace();// ignore
			}
		}
	}

	/**
	 * If a bundle of the specified location is in the Eclipse plugin format (plugin-name_version.jar),
	 * return version string.Otherwise, return null;
	 * 
	 * @param url
	 * @param pluginName
	 * @return version string. If invalid format, return null. 
	 */
	private static String getEclipseJarNamingVersion(URL url, final String pluginName) {
		String location = url.getFile();
		if (!File.separator.equals("/"))
			location = Utils.replaceAll(location, File.separator, "/");
		String filename = null;
		if (location.indexOf(":") == -1)
			filename = location;
		else
			filename = location.substring(location.lastIndexOf(":") + 1);

		if (location.indexOf("/") == -1)
			filename = location;
		else
			filename = location.substring(location.lastIndexOf("/") + 1);
		// filename must be "jarName"_"version".jar
		//System.out.println("filename=" + filename);
		if (!filename.endsWith(".jar"))
			return null;
		filename = filename.substring(0, filename.lastIndexOf(".jar"));
		//System.out.println("filename=" + filename);
		if (filename.lastIndexOf("_") == -1)
			return null;
		String version = filename.substring(filename.lastIndexOf("_") + 1);
		filename = filename.substring(0, filename.lastIndexOf("_"));
		//System.out.println("filename=" + filename);
		if (filename.indexOf("_") != -1)
			return null;
		if (!filename.equals(pluginName))
			return null;
		return version;
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
				String version = getEclipseJarNamingVersion(url, pluginName);
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

class EclipseVersion implements Comparable {
	int major = 0;
	int minor = 0;
	int service = 0;
	String qualifier = null;

	EclipseVersion(String version) {
		StringTokenizer tok = new StringTokenizer(version, ".");
		if (!tok.hasMoreTokens())
			return;
		this.major = Integer.parseInt(tok.nextToken());
		if (!tok.hasMoreTokens())
			return;
		this.minor = Integer.parseInt(tok.nextToken());
		if (!tok.hasMoreTokens())
			return;
		this.service = Integer.parseInt(tok.nextToken());
		if (!tok.hasMoreTokens())
			return;
		this.qualifier = tok.nextToken();
	}

	public int compareTo(Object obj) {
		EclipseVersion target = (EclipseVersion) obj;
		if (target.major > this.major)
			return -1;
		if (target.major < this.major)
			return 1;
		if (target.minor > this.minor)
			return -1;
		if (target.minor < this.minor)
			return 1;
		if (target.service > this.service)
			return -1;
		if (target.service < this.service)
			return 1;
		return 0;
	}

}
