/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Pascal Rapicault - Support for bundled macosx http://bugs.eclipse.org/57349
 *******************************************************************************/
package org.eclipse.equinox.internal.frameworkadmin.equinox;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.frameworkadmin.equinox.utils.FileUtils;
import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.log.LogService;

public class ParserUtils {
	private static final String FILE_PROTOCOL = "file:"; //$NON-NLS-1$

	public static File getOSGiInstallArea(List<String> programArgs, Properties properties, LauncherData launcherData) {
		if (launcherData == null)
			return null;

		URI base = null;
		if (launcherData.getLauncher() != null)
			base = launcherData.getLauncher().getParentFile().toURI();
		else if (launcherData.getHome() != null)
			base = launcherData.getHome().toURI();
		File result = getOSGiInstallArea(programArgs, properties, base);
		if (result != null)
			return result;

		if (launcherData.getHome() != null) {
			return launcherData.getHome();
		}

		if (launcherData.getFwJar() != null)
			return fromOSGiJarToOSGiInstallArea(launcherData.getFwJar().getAbsolutePath());

		File launcherFile = launcherData.getLauncher();
		if (launcherFile != null) {
			if (Constants.OS_MACOSX.equals(launcherData.getOS())) { //
				//TODO We are going to change this - the equinox launcher will look 3 levels up on the mac when going from executable to launcher.jar
				//see org.eclipse.equinox.executable/library/eclipse.c : findStartupJar();
				IPath launcherPath = new Path(launcherFile.getAbsolutePath());
				if (launcherPath.segmentCount() > 2) {
					//removing "MacOS/eclipse" from the end of the path
					launcherPath = launcherPath.removeLastSegments(2).append("Eclipse"); //$NON-NLS-1$
					return launcherPath.toFile();
				}
			}
			return launcherFile.getParentFile();
		}
		return null;
	}

	public static URI getFrameworkJar(List<String> lines, URI launcherFolder) {
		String fwk = ParserUtils.getValueForArgument(EquinoxConstants.OPTION_FW, lines);
		if (fwk == null) {
			//Search the file system using the default location
			URI location = FileUtils.getEclipsePluginFullLocation(EquinoxConstants.FW_SYMBOLIC_NAME, new File(URIUtil.toFile(launcherFolder), EquinoxConstants.PLUGINS_DIR));
			if (location != null)
				return location;
			return null;
		}
		try {
			return URIUtil.makeAbsolute(URIUtil.fromString(fwk), launcherFolder);
		} catch (URISyntaxException e) {
			Log.log(LogService.LOG_ERROR, NLS.bind(Messages.exception_createAbsoluteURI, fwk, launcherFolder));
			return null;
		}
	}

	//This method should only be used to determine the osgi install area when reading the eclipse.ini
	public static File getOSGiInstallArea(List<String> args, Properties properties, URI base) {
		if (args == null)
			return null;
		String install = getValueForArgument(EquinoxConstants.OPTION_INSTALL, args);
		if (install == null && properties != null)
			install = properties.getProperty("osgi.install.area"); //$NON-NLS-1$

		if (install != null) {
			if (install.startsWith(FILE_PROTOCOL))
				install = install.substring(FILE_PROTOCOL.length() + 1);
			File installFile = new File(install);
			if (installFile.isAbsolute())
				return installFile;
			return URIUtil.toFile(URIUtil.makeAbsolute(installFile.toURI(), base));
		}

		String startup = getValueForArgument(EquinoxConstants.OPTION_STARTUP, args);
		if (startup != null && base != null) {
			if (startup.startsWith(FILE_PROTOCOL)) {
				try {
					URI startupURI = new URI(startup);
					startup = new File(startupURI).getAbsolutePath();
				} catch (URISyntaxException e) {
					startup = startup.substring(FILE_PROTOCOL.length() + 1);
				}
			}

			File osgiInstallArea = fromOSGiJarToOSGiInstallArea(startup);
			if (osgiInstallArea.isAbsolute())
				return osgiInstallArea;

			File baseFile = new File(base);
			return new File(baseFile, osgiInstallArea.getPath());
		}
		return null;
	}

	public static File fromOSGiJarToOSGiInstallArea(String path) {
		IPath parentFolder = new Path(path).removeLastSegments(1);
		if ("plugins".equalsIgnoreCase(parentFolder.lastSegment())) //$NON-NLS-1$
			return parentFolder.removeLastSegments(1).toFile();
		return parentFolder.toFile();
	}

	public static boolean isArgumentSet(String arg, List<String> args) {
		if (arg == null || args == null)
			return false;
		for (int i = 0; i < args.size(); i++) {
			if (args.get(i) == null)
				continue;
			if ((args.get(i)).equalsIgnoreCase(arg)) {
				return true;
			}
		}
		return false;
	}

	public static String getValueForArgument(String arg, List<String> args) {
		if (arg == null || args == null)
			return null;
		for (int i = 0; i < args.size(); i++) {
			if (args.get(i) == null)
				continue;
			if ((args.get(i)).equalsIgnoreCase(arg)) {
				if (i + 1 < args.size()) {
					String value = args.get(i + 1);
					if (value != null && value.length() > 0 && value.charAt(0) != '-')
						return value;
				}
			}
		}
		return null;
	}

	public static boolean setValueForArgument(String arg, String value, List<String> args) {
		if (arg == null || args == null)
			return false;

		for (int i = 0; i < args.size(); i++) {
			if (args.get(i) == null)
				continue;
			String currentArg = (args.get(i)).trim();
			if (currentArg.equalsIgnoreCase(arg)) {
				if (i + 1 < args.size()) {
					String nextArg = args.get(i + 1);
					if (nextArg == null || nextArg.charAt(0) != '-') {
						args.set(i + 1, value);
					} else {
						args.add(i + 1, value);
					}
					return true;
				}
				// else just append the value on the end
				args.add(value);
				return true;
			}
		}
		args.add(arg);
		args.add(value);
		return true;
	}

	public static boolean removeArgument(String arg, List<String> args) {
		if (arg == null || args == null)
			return false;
		for (int i = 0; i < args.size(); i++) {
			if (args.get(i) == null)
				continue;
			String currentArg = (args.get(i)).trim();
			if (currentArg.equalsIgnoreCase(arg)) {
				args.set(i, null);
				while (i + 1 < args.size() && args.get(i + 1) != null && (args.get(i + 1)).charAt(0) != '-') {
					args.set(i + 1, null);
					i++;
				}
			}
		}
		return false;
	}
}
