/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.frameworkadmin.equinox;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.frameworkadmin.equinox.utils.FileUtils;
import org.eclipse.equinox.internal.frameworkadmin.utils.Utils;
import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.log.LogService;

public class EclipseLauncherParser {

	private String[] buildNewCommandLine(LauncherData launcherData, File outputFile) {
		List lines = new LinkedList();

		boolean startUpFlag = false;
		final String[] programArgs = launcherData.getProgramArgs();
		if (programArgs != null && programArgs.length != 0)
			for (int i = 0; i < programArgs.length; i++) {
				if (programArgs[i].equals(EquinoxConstants.OPTION_STARTUP) && (programArgs[i + 1] != null || programArgs[i + 1].length() != 0)) {
					lines.add(programArgs[i]);
					lines.add(programArgs[++i]);
					startUpFlag = true;
				} else
					lines.add(programArgs[i]);
			}
		if (launcherData.isClean())
			lines.add(EquinoxConstants.OPTION_CLEAN);
		File fwPersistentDataLocation = launcherData.getFwPersistentDataLocation();
		File fwConfigLocation = launcherData.getFwConfigLocation();
		if (fwPersistentDataLocation != null) {
			if (fwConfigLocation != null) {
				if (!fwPersistentDataLocation.equals(fwConfigLocation))
					throw new IllegalStateException();
			}
			launcherData.setFwConfigLocation(fwPersistentDataLocation);
		} else if (fwConfigLocation != null)
			launcherData.setFwPersistentDataLocation(fwConfigLocation, launcherData.isClean());

		if (launcherData.getFwConfigLocation() != null) {
			lines.add(EquinoxConstants.OPTION_CONFIGURATION);
			lines.add(launcherData.getFwConfigLocation().getAbsolutePath());
		}

		if (!startUpFlag)
			if (launcherData.getFwJar() != null) {
				lines.add(EquinoxConstants.OPTION_FW);
				String path = launcherData.getFwJar().getAbsolutePath();
				lines.add(path);
			}

		if (launcherData.getJvm() != null) {
			lines.add(EquinoxConstants.OPTION_VM);
			lines.add(launcherData.getJvm().getAbsolutePath());
		}
		final String[] jvmArgs = launcherData.getJvmArgs();
		if (jvmArgs != null && jvmArgs.length != 0) {
			lines.add(EquinoxConstants.OPTION_VMARGS);
			for (int i = 0; i < jvmArgs.length; i++)
				lines.add(jvmArgs[i]);
		}
		String[] ret = new String[lines.size()];
		lines.toArray(ret);
		return ret;
	}

	//this figures out the location of the data area on partial data read from the <eclipse>.ini
	private URI getOSGiInstallArea(String[] lines, LauncherData launcherData) {
		File osgiInstallArea = ParserUtils.getOSGiInstallArea(lines);
		if (osgiInstallArea != null)
			return URIUtil.makeAbsolute(osgiInstallArea.toURI(), launcherData.getLauncher().getParentFile().toURI());
		return null;
	}

	private String[] loadFile(File file) throws IOException {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));

			String line;
			List list = new LinkedList();
			while ((line = br.readLine()) != null) {
				list.add(line);
			}
			return (String[]) list.toArray(new String[list.size()]);
		} finally {
			if (br != null)
				try {
					br.close();
				} catch (IOException e) {
					//Ignore
				}
		}
	}

	void read(File launcherConfigFile, LauncherData launcherData) throws IOException {
		if (!launcherConfigFile.exists())
			return;

		String[] lines = loadFile(launcherConfigFile);

		URI launcherFolder = launcherData.getLauncher().getParentFile().toURI();
		getStartup(lines, launcherFolder);
		URI osgiInstallArea = getOSGiInstallArea(lines, launcherData);
		URI configArea = getConfigurationLocation(lines, osgiInstallArea, launcherData);
		getPersistentDataLocation(lines, osgiInstallArea, configArea, launcherData);
		getLauncherLibrary(lines, launcherFolder);
		getFrameworkJar(lines, launcherFolder, launcherData);
		getJVMArgs(lines, launcherData);
		getVM(lines, launcherFolder, launcherData);

		launcherData.setProgramArgs(lines);

		Log.log(LogService.LOG_INFO, NLS.bind(Messages.log_configFile, launcherConfigFile.getAbsolutePath()));
	}

	private void getPersistentDataLocation(String[] lines, URI osgiInstallArea, URI configArea, LauncherData launcherData) {
		//TODO The setting of the -clean could only do properly once config.ini has been read
		if (launcherData.getFwPersistentDataLocation() == null) {
			launcherData.setFwPersistentDataLocation(URIUtil.toFile(configArea), ParserUtils.isArgumentSet(EquinoxConstants.OPTION_CLEAN, lines));
		}
	}

	private void getVM(String[] lines, URI launcherIniFile, LauncherData launcherData) {
		String vm = ParserUtils.getValueForArgument(EquinoxConstants.OPTION_VM, lines);
		if (vm == null)
			return;

		URI VMFullPath;
		try {
			VMFullPath = URIUtil.makeAbsolute(URIUtil.fromString(vm), launcherIniFile);
			launcherData.setJvm(URIUtil.toFile(VMFullPath));
			ParserUtils.setValueForArgument(EquinoxConstants.OPTION_VM, VMFullPath.toString(), lines);
		} catch (URISyntaxException e) {
			Log.log(LogService.LOG_ERROR, "can't make absolute of:" + vm);
			return;
		}

	}

	private void getJVMArgs(String[] lines, LauncherData launcherData) {
		String[] vmargs = ParserUtils.getMultiValuedArgument(EquinoxConstants.OPTION_VMARGS, lines);
		if (vmargs != null)
			launcherData.setJvmArgs(vmargs);
	}

	private void getFrameworkJar(String[] lines, URI launcherFolder, LauncherData launcherData) {
		File fwJar = launcherData.getFwJar();
		if (fwJar != null)
			return;
		String fwk = ParserUtils.getValueForArgument(EquinoxConstants.OPTION_FW, lines);
		if (fwk == null) {
			//Search the file system using the default location
			URI location = FileUtils.getEclipsePluginFullLocation(EquinoxConstants.FW_SYMBOLIC_NAME, new File(URIUtil.toFile(launcherFolder), EquinoxConstants.PLUGINS_DIR));
			if (location != null)
				launcherData.setFwJar(URIUtil.toFile(location));
			return;
		}
		try {
			URI location = URIUtil.makeAbsolute(URIUtil.fromString(fwk), launcherFolder);
			ParserUtils.setValueForArgument(EquinoxConstants.OPTION_FW, location.toString(), lines);
		} catch (URISyntaxException e) {
			Log.log(LogService.LOG_ERROR, "can't make absolute of:" + fwk);
			return;
		}
	}

	private void setFrameworkJar(String[] lines, URI osgiInstallArea) {
		String fwkJar = ParserUtils.getValueForArgument(EquinoxConstants.OPTION_FW, lines);
		if (fwkJar == null)
			return;

		try {
			URI result = URIUtil.makeRelative(URIUtil.fromString(fwkJar), osgiInstallArea);
			ParserUtils.setValueForArgument(EquinoxConstants.OPTION_FW, URIUtil.toUnencodedString(result), lines);
		} catch (URISyntaxException e) {
			Log.log(LogService.LOG_ERROR, "can't make absolute of:" + fwkJar);
			return;
		}
	}

	private URI getLauncherLibrary(String[] lines, URI launcherFolder) {
		String launcherLibrary = ParserUtils.getValueForArgument(EquinoxConstants.OPTION_LAUNCHER_LIBRARY, lines);
		if (launcherLibrary == null)
			return null;

		URI result = null;
		try {
			result = URIUtil.makeAbsolute(URIUtil.fromString(launcherLibrary), launcherFolder);
			ParserUtils.setValueForArgument(EquinoxConstants.OPTION_LAUNCHER_LIBRARY, result.toString(), lines);
		} catch (URISyntaxException e) {
			Log.log(LogService.LOG_ERROR, "can't make absolute of:" + launcherLibrary);
			return null;
		}
		return result;
	}

	private void setLauncherLibrary(String[] lines, URI launcherFolder) {
		String launcherLibrary = ParserUtils.getValueForArgument(EquinoxConstants.OPTION_LAUNCHER_LIBRARY, lines);
		if (launcherLibrary == null)
			return;

		try {
			URI result = URIUtil.makeRelative(URIUtil.fromString(launcherLibrary), launcherFolder);
			ParserUtils.setValueForArgument(EquinoxConstants.OPTION_LAUNCHER_LIBRARY, URIUtil.toUnencodedString(result), lines);
		} catch (URISyntaxException e) {
			Log.log(LogService.LOG_ERROR, "can't make absolute of:" + launcherLibrary);
			return;
		}
	}

	private URI getConfigurationLocation(String[] lines, URI osgiInstallArea, LauncherData data) {
		String configuration = ParserUtils.getValueForArgument(EquinoxConstants.OPTION_CONFIGURATION, lines);
		if (configuration == null)
			return null;

		URI result = null;
		try {
			result = URIUtil.makeAbsolute(URIUtil.fromString(configuration), osgiInstallArea);
			ParserUtils.setValueForArgument(EquinoxConstants.OPTION_CONFIGURATION, result.toString(), lines);
			data.setFwConfigLocation(URIUtil.toFile(result));
		} catch (URISyntaxException e) {
			Log.log(LogService.LOG_ERROR, "can't make absolute of:" + configuration);
			return null;
		}
		return result;
	}

	private void setConfigurationLocation(String[] lines, URI osgiInstallArea) {
		String configuration = ParserUtils.getValueForArgument(EquinoxConstants.OPTION_CONFIGURATION, lines);
		if (configuration == null)
			return;

		try {
			//FIXME The call 
			String result = URIUtil.toUnencodedString(URIUtil.makeRelative(URIUtil.fromString(configuration), osgiInstallArea));
			if (result.equals("configuration")) {
				ParserUtils.setValueForArgument(EquinoxConstants.OPTION_CONFIGURATION, null, lines);
				for (int i = 0; i < lines.length; i++) {
					if (lines[i] != null && lines[i].equals(EquinoxConstants.OPTION_CONFIGURATION))
						lines[i] = null;
				}
			} else {
				ParserUtils.setValueForArgument(EquinoxConstants.OPTION_CONFIGURATION, result, lines);
			}
		} catch (URISyntaxException e) {
			Log.log(LogService.LOG_ERROR, "can't make absolute of:" + configuration);
			return;
		}
	}

	private URI getStartup(String[] lines, URI launcherFolder) {
		String startup = ParserUtils.getValueForArgument(EquinoxConstants.OPTION_STARTUP, lines);
		if (startup == null)
			return null;

		URI result = null;
		try {
			result = URIUtil.makeAbsolute(URIUtil.fromString(startup), launcherFolder);
			ParserUtils.setValueForArgument(EquinoxConstants.OPTION_STARTUP, result.toString(), lines);
		} catch (URISyntaxException e) {
			Log.log(LogService.LOG_ERROR, "can't make absolute of:" + startup);
			return null;
		}
		return result;
	}

	private void setStartup(String[] lines, URI launcherFolder) {
		String startup = ParserUtils.getValueForArgument(EquinoxConstants.OPTION_STARTUP, lines);
		if (startup == null)
			return;

		try {
			URI result = URIUtil.makeRelative(URIUtil.fromString(startup), launcherFolder);
			ParserUtils.setValueForArgument(EquinoxConstants.OPTION_STARTUP, URIUtil.toUnencodedString(result), lines);
		} catch (URISyntaxException e) {
			Log.log(LogService.LOG_ERROR, "can't make relative of:" + startup);
			return;
		}
	}

	void save(EquinoxLauncherData launcherData, boolean backup) throws IOException {
		File launcherConfigFile = EquinoxManipulatorImpl.getLauncherConfigLocation(launcherData);

		if (launcherConfigFile == null)
			throw new IllegalStateException(Messages.exception_launcherLocationNotSet);
		if (!Utils.createParentDir(launcherConfigFile)) {
			throw new IllegalStateException(Messages.exception_failedToCreateDir);
		}
		//Tweak all the values to make them relative
		File launcherFolder = launcherData.getLauncher().getParentFile();
		String[] lines = buildNewCommandLine(launcherData, launcherConfigFile);
		File osgiInstallArea = ParserUtils.getOSGiInstallArea(launcherData);
		setConfigurationLocation(lines, osgiInstallArea.toURI());
		setLauncherLibrary(lines, launcherFolder.toURI());
		setStartup(lines, launcherFolder.toURI());
		setFrameworkJar(lines, osgiInstallArea.toURI());

		// backup file if exists.		
		if (backup)
			if (launcherConfigFile.exists()) {
				File dest = Utils.getSimpleDataFormattedFile(launcherConfigFile);
				if (!launcherConfigFile.renameTo(dest))
					throw new IOException(NLS.bind(Messages.exception_failedToRename, launcherConfigFile, dest));
				Log.log(LogService.LOG_INFO, this, "save()", NLS.bind(Messages.log_renameSuccessful, launcherConfigFile, dest)); //$NON-NLS-1$
			}

		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(launcherConfigFile));
			for (int j = 0; j < lines.length; j++) {
				bw.write(lines[j]);
				bw.newLine();
			}
			bw.flush();
			Log.log(LogService.LOG_INFO, NLS.bind(Messages.log_launcherConfigSave, launcherConfigFile));
		} finally {
			if (bw != null)
				bw.close();
			File previousLauncherIni = launcherData.getPreviousLauncherIni();
			if (previousLauncherIni != null && !previousLauncherIni.equals(launcherConfigFile))
				previousLauncherIni.delete();
		}
	}
}
