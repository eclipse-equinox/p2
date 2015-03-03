/*******************************************************************************
 * Copyright (c) 2007, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Pascal Rapicault - Support for bundled macosx http://bugs.eclipse.org/57349
 *     Christian Georgi - Relativize VM path https://bugs.eclipse.org/bugs/437680
 *******************************************************************************/
package org.eclipse.equinox.internal.frameworkadmin.equinox;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.frameworkadmin.equinox.utils.FileUtils;
import org.eclipse.equinox.internal.frameworkadmin.utils.Utils;
import org.eclipse.equinox.internal.provisional.frameworkadmin.FrameworkAdminRuntimeException;
import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.log.LogService;

public class EclipseLauncherParser {
	private static final String CONFIGURATION_FOLDER = "configuration"; //$NON-NLS-1$

	//this figures out the location of the data area on partial data read from the <eclipse>.ini
	private URI getOSGiInstallArea(List<String> lines, URI base, LauncherData launcherData) {
		//does the eclipse.ini say anything for osgi.install.area?
		File osgiInstallArea = ParserUtils.getOSGiInstallArea(lines, null, base);
		if (osgiInstallArea == null) {
			//otherwise use the launcherData to figure it out
			osgiInstallArea = ParserUtils.getOSGiInstallArea(lines, null, launcherData);
		}
		if (osgiInstallArea != null)
			return URIUtil.makeAbsolute(osgiInstallArea.toURI(), base);
		return null;
	}

	private void setInstall(List<String> lines, LauncherData launcherData, File launcherFolder) {
		if (launcherData.getFwConfigLocation() == null || launcherData.getFwJar() == null) {
			ParserUtils.removeArgument(EquinoxConstants.OPTION_INSTALL, lines);
			return;
		}
		if (Constants.OS_MACOSX.equals(launcherData.getOS())) {
			if (!new File(ParserUtils.fromOSGiJarToOSGiInstallArea(launcherData.getFwJar().getAbsolutePath()), "../MacOS").equals(launcherFolder)) {
				ParserUtils.setValueForArgument(EquinoxConstants.OPTION_INSTALL, launcherFolder.getAbsolutePath().replace('\\', '/'), lines);
			}
		} else if (!ParserUtils.fromOSGiJarToOSGiInstallArea(launcherData.getFwJar().getAbsolutePath()).equals(launcherFolder)) {
			ParserUtils.setValueForArgument(EquinoxConstants.OPTION_INSTALL, launcherFolder.getAbsolutePath().replace('\\', '/'), lines);
		}
	}

	void read(File launcherConfigFile, LauncherData launcherData) throws IOException {
		if (!launcherConfigFile.exists())
			return;

		List<String> lines = FileUtils.loadFile(launcherConfigFile);

		URI launcherFolder = launcherData.getLauncher().getParentFile().toURI();
		getStartup(lines, launcherFolder);
		getFrameworkJar(lines, launcherFolder, launcherData);
		URI osgiInstallArea = getOSGiInstallArea(lines, launcherFolder, launcherData);
		if (osgiInstallArea == null) {
			osgiInstallArea = launcherData.getFwJar() != null ? ParserUtils.fromOSGiJarToOSGiInstallArea(launcherData.getFwJar().getAbsolutePath()).toURI() : launcherFolder;
		}
		URI configArea = getConfigurationLocation(lines, osgiInstallArea, launcherData);
		if (configArea == null)
			throw new FrameworkAdminRuntimeException(Messages.exception_nullConfigArea, ""); //$NON-NLS-1$
		getPersistentDataLocation(lines, osgiInstallArea, configArea, launcherData);
		getLauncherLibrary(lines, launcherFolder);
		getJVMArgs(lines, launcherData);
		getProgramArgs(lines, launcherData);
		getVM(lines, launcherFolder, launcherData);

		Log.log(LogService.LOG_INFO, NLS.bind(Messages.log_configFile, launcherConfigFile.getAbsolutePath()));
	}

	private void getFrameworkJar(List<String> lines, URI launcherFolder, LauncherData launcherData) {
		File fwJar = launcherData.getFwJar();
		if (fwJar != null)
			return;
		URI location = ParserUtils.getFrameworkJar(lines, launcherFolder);
		if (location != null)
			launcherData.setFwJar(URIUtil.toFile(location));
	}

	private void getPersistentDataLocation(List<String> lines, URI osgiInstallArea, URI configArea, LauncherData launcherData) {
		//TODO The setting of the -clean could only do properly once config.ini has been read
		if (launcherData.getFwPersistentDataLocation() == null) {
			launcherData.setFwPersistentDataLocation(URIUtil.toFile(configArea), ParserUtils.isArgumentSet(EquinoxConstants.OPTION_CLEAN, lines));
		}
	}

	private void getVM(List<String> lines, URI launcherFolder, LauncherData launcherData) {
		String vm = ParserUtils.getValueForArgument(EquinoxConstants.OPTION_VM, lines);
		if (vm == null)
			return;

		URI VMFullPath;
		try {
			VMFullPath = URIUtil.makeAbsolute(FileUtils.fromPath(vm), launcherFolder);
			launcherData.setJvm(URIUtil.toFile(VMFullPath));
			ParserUtils.setValueForArgument(EquinoxConstants.OPTION_VM, VMFullPath.toString(), lines);
		} catch (URISyntaxException e) {
			Log.log(LogService.LOG_ERROR, NLS.bind(Messages.log_failed_make_absolute, vm));
			return;
		}
	}

	private void setVM(List<String> lines, File vm, URI launcherFolder, File installHome) {
		if (vm == null) {
			if (ParserUtils.getValueForArgument(EquinoxConstants.OPTION_VM, lines) != null)
				return;

			ParserUtils.removeArgument(EquinoxConstants.OPTION_VM, lines);
			return;
		}

		URI vmRelativePath = null;
		if (vm.isAbsolute()) {
			// Bug 437680: Correctly relativize on MacOS
			// Example:         (traditional layout)                     (bundled layout)
			//   Install home:  install/                                 Eclipse.app/
			//   Launcher:        Eclipse.app/Contents/MacOS/              Contents/MacOS/
			//   VM:              jre/                                     jre/
			//   Result:        ../../../jre                             ../../jre
			URI vmRelativePathToHome = installHome.toURI().relativize(vm.toURI());
			if (vmRelativePathToHome.isAbsolute()) {
				// VM is not below the install root -> use absolute path
				vmRelativePath = vmRelativePathToHome;
			} else {
				// make VM path relative to launcher folder (which is different to the install root in MacOS installs)
				vmRelativePath = URIUtil.makeRelative(vm.toURI(), launcherFolder);
			}
		} else {
			//For relative files, File#toURI will create an absolute URI by resolving against the current working directory, we don't want that
			String path = vm.getPath().replace('\\', '/');
			try {
				vmRelativePath = URIUtil.fromString(path);
			} catch (URISyntaxException e) {
				vmRelativePath = launcherFolder.relativize(vm.toURI());
			}
		}

		ParserUtils.setValueForArgument(EquinoxConstants.OPTION_VM, FileUtils.toPath(vmRelativePath).replace('\\', '/'), lines);
	}

	private void getJVMArgs(List<String> lines, LauncherData launcherData) {
		ArrayList<String> vmargs = new ArrayList<String>(lines.size());
		boolean foundVmArgs = false;
		for (Iterator<String> iterator = lines.iterator(); iterator.hasNext();) {
			String line = iterator.next();
			if (!foundVmArgs) {
				if (EquinoxConstants.OPTION_VMARGS.equals(line))
					foundVmArgs = true;
				continue;
			}
			vmargs.add(line);
		}

		launcherData.setJvmArgs(null);
		launcherData.setJvmArgs(vmargs.toArray(new String[vmargs.size()]));
	}

	private void setJVMArgs(List<String> lines, LauncherData launcherData) {
		ParserUtils.removeArgument(EquinoxConstants.OPTION_VMARGS, lines);
		if (launcherData.getJvmArgs() == null || launcherData.getJvmArgs().length == 0)
			return;
		String[] args = launcherData.getJvmArgs();
		lines.add(EquinoxConstants.OPTION_VMARGS);
		for (int i = 0; i < args.length; i++) {
			lines.add(args[i]);
		}
	}

	private void getProgramArgs(List<String> lines, LauncherData launcherData) {
		ArrayList<String> args = new ArrayList<String>(lines.size());
		for (Iterator<String> iterator = lines.iterator(); iterator.hasNext();) {
			String line = iterator.next();
			if (EquinoxConstants.OPTION_VMARGS.equals(line))
				break;
			args.add(line);
		}
		launcherData.setProgramArgs(null);
		launcherData.setProgramArgs(args.toArray(new String[args.size()]));
	}

	private URI getLauncherLibrary(List<String> lines, URI launcherFolder) {
		String launcherLibrary = ParserUtils.getValueForArgument(EquinoxConstants.OPTION_LAUNCHER_LIBRARY, lines);
		if (launcherLibrary == null)
			return null;
		URI result = null;
		try {
			result = URIUtil.makeAbsolute(FileUtils.fromPath(launcherLibrary), launcherFolder);
			ParserUtils.setValueForArgument(EquinoxConstants.OPTION_LAUNCHER_LIBRARY, result.toString(), lines);
		} catch (URISyntaxException e) {
			Log.log(LogService.LOG_ERROR, NLS.bind(Messages.log_failed_make_absolute, launcherLibrary));
			return null;
		}
		return result;
	}

	private void setLauncherLibrary(List<String> lines, URI launcherFolder) {
		String launcherLibrary = ParserUtils.getValueForArgument(EquinoxConstants.OPTION_LAUNCHER_LIBRARY, lines);
		if (launcherLibrary == null)
			return;

		try {
			URI result = URIUtil.makeRelative(FileUtils.fromPath(launcherLibrary), launcherFolder);
			ParserUtils.setValueForArgument(EquinoxConstants.OPTION_LAUNCHER_LIBRARY, FileUtils.toPath(result).replace('\\', '/'), lines);
		} catch (URISyntaxException e) {
			Log.log(LogService.LOG_ERROR, NLS.bind(Messages.log_failed_make_absolute, launcherLibrary));
			return;
		}
	}

	private URI getConfigurationLocation(List<String> lines, URI osgiInstallArea, LauncherData data) {
		String configuration = ParserUtils.getValueForArgument(EquinoxConstants.OPTION_CONFIGURATION, lines);
		if (configuration == null)
			try {
				return URIUtil.makeAbsolute(new URI(CONFIGURATION_FOLDER), osgiInstallArea);
			} catch (URISyntaxException e1) {
				//ignore
			}

		URI result = null;
		try {
			result = URIUtil.makeAbsolute(FileUtils.fromPath(configuration), osgiInstallArea);
			ParserUtils.setValueForArgument(EquinoxConstants.OPTION_CONFIGURATION, result.toString(), lines);
			data.setFwConfigLocation(URIUtil.toFile(result));
		} catch (URISyntaxException e) {
			Log.log(LogService.LOG_ERROR, NLS.bind(Messages.log_failed_make_absolute, configuration));
			return null;
		}
		return result;
	}

	private void setConfigurationLocation(List<String> lines, URI osgiInstallArea, LauncherData data) {
		String result = FileUtils.toPath(URIUtil.makeRelative(data.getFwConfigLocation().toURI(), osgiInstallArea));
		//We don't write the default
		if (CONFIGURATION_FOLDER.equals(result)) {
			if (ParserUtils.getValueForArgument(EquinoxConstants.OPTION_CONFIGURATION, lines) != null)
				ParserUtils.removeArgument(EquinoxConstants.OPTION_CONFIGURATION, lines);
			return;
		}

		if (ParserUtils.getValueForArgument(EquinoxConstants.OPTION_CONFIGURATION, lines) == null) {
			ParserUtils.setValueForArgument(EquinoxConstants.OPTION_CONFIGURATION, result.replace('\\', '/'), lines);
		}
		return;
	}

	private URI getStartup(List<String> lines, URI launcherFolder) {
		String startup = ParserUtils.getValueForArgument(EquinoxConstants.OPTION_STARTUP, lines);
		if (startup == null)
			return null;

		URI result = null;
		try {
			result = URIUtil.makeAbsolute(FileUtils.fromPath(startup), launcherFolder);
			ParserUtils.setValueForArgument(EquinoxConstants.OPTION_STARTUP, result.toString(), lines);
		} catch (URISyntaxException e) {
			Log.log(LogService.LOG_ERROR, NLS.bind(Messages.log_failed_make_absolute, startup));
			return null;
		}
		return result;
	}

	private void setStartup(List<String> lines, URI launcherFolder) {
		String startup = ParserUtils.getValueForArgument(EquinoxConstants.OPTION_STARTUP, lines);
		if (startup == null)
			return;

		try {
			URI result = URIUtil.makeRelative(FileUtils.fromPath(startup), launcherFolder);
			ParserUtils.setValueForArgument(EquinoxConstants.OPTION_STARTUP, FileUtils.toPath(result).replace('\\', '/'), lines);
		} catch (URISyntaxException e) {
			Log.log(LogService.LOG_ERROR, NLS.bind(Messages.log_failed_make_relative, startup));
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
		List<String> newlines = new ArrayList<String>();
		newlines.addAll(Arrays.asList(launcherData.getProgramArgs()));

		setStartup(newlines, launcherFolder.toURI());
		setInstall(newlines, launcherData, launcherFolder);
		//Get the osgi install area
		File osgiInstallArea = ParserUtils.getOSGiInstallArea(newlines, null, launcherData);
		//setInstall(lines, osgiInstallArea, launcherFolder);
		setConfigurationLocation(newlines, osgiInstallArea.toURI(), launcherData);
		setLauncherLibrary(newlines, launcherFolder.toURI());
		//		setFrameworkJar(newlines, launcherData.getFwJar());
		setVM(newlines, launcherData.getJvm(), launcherFolder.toURI(), launcherData.getHome());

		//We are done, let's update the program args in the launcher data
		launcherData.setProgramArgs(null);
		launcherData.setProgramArgs(newlines.toArray(new String[newlines.size()]));

		//append jvm args
		setJVMArgs(newlines, launcherData);

		// backup file if exists.		
		if (backup)
			if (launcherConfigFile.exists()) {
				File dest = Utils.getSimpleDataFormattedFile(launcherConfigFile);
				if (!launcherConfigFile.renameTo(dest))
					throw new IOException(NLS.bind(Messages.exception_failedToRename, launcherConfigFile, dest));
				Log.log(LogService.LOG_INFO, this, "save()", NLS.bind(Messages.log_renameSuccessful, launcherConfigFile, dest)); //$NON-NLS-1$
			}

		//only write the file if we actually have content
		if (newlines.size() > 0) {
			BufferedWriter bw = null;
			try {
				bw = new BufferedWriter(new FileWriter(launcherConfigFile));
				for (int j = 0; j < newlines.size(); j++) {
					String arg = newlines.get(j);
					if (arg == null)
						continue;
					bw.write(arg);
					bw.newLine();
				}
				bw.flush();
				Log.log(LogService.LOG_INFO, NLS.bind(Messages.log_launcherConfigSave, launcherConfigFile));
			} finally {
				if (bw != null)
					bw.close();
			}
		}
		File previousLauncherIni = launcherData.getPreviousLauncherIni();
		if (previousLauncherIni != null && !previousLauncherIni.equals(launcherConfigFile))
			previousLauncherIni.delete();
		launcherData.setLauncherConfigLocation(launcherConfigFile);
	}
}
