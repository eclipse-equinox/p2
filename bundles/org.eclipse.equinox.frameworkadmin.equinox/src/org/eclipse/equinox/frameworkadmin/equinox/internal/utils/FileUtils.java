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
			// TODO Auto-generated catch block
			//e.printStackTrace();
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

		return Utils.getEclipsePluginFullLocation(location, pluginsDir);
		//		String ret = location;
		//		if (location.startsWith("reference:"))
		//			ret = location.substring("reference:".length());
		//		if (location.startsWith("initial@"))
		//			ret = location.substring("initial@".length());
		//		if (ret == location)
		//			return ret;
		//		return getRealLocation(ret);
	}

	public static String getRealLocation(Manipulator manipulator, final String location, boolean useEclipse) {
		String ret = location;
		if (location.startsWith("reference:"))
			ret = location.substring("reference:".length());
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
}
