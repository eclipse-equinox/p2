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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.internal.frameworkadmin.equinox.utils.FileUtils;
import org.eclipse.equinox.internal.frameworkadmin.utils.Utils;
import org.eclipse.equinox.internal.provisional.frameworkadmin.LauncherData;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.log.LogService;

public class EclipseLauncherParser {

	private String[] getConfigFileLines(LauncherData launcherData, File outputFile, boolean relative) {
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
				String path = ""; //$NON-NLS-1$
				//if (relative)
				//	path = Utils.getRelativePath(launcherData.getFwJar(), outputFile.getParentFile());
				//else
				path = launcherData.getFwJar().getAbsolutePath();
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

	private void parseCmdLine(LauncherData launcherData, String[] lines) {
		//Log.log(LogService.LOG_DEBUG, "inputFile=" + inputFile.getAbsolutePath());
		//		final File launcherFile = launcherData.getLauncher();
		final File launcherConfigFile = EquinoxManipulatorImpl.getLauncherConfigLocation(launcherData);

		boolean clean = launcherData.isClean();
		boolean needToUpdate = false;
		File fwPersistentDataLoc = launcherData.getFwPersistentDataLocation();
		File fwConfigLocation = launcherData.getFwConfigLocation();
		if (fwPersistentDataLoc == null) {
			if (fwConfigLocation == null) {
				fwPersistentDataLoc = new File(launcherConfigFile.getParent(), EquinoxConstants.DEFAULT_CONFIGURATION);
				fwConfigLocation = fwPersistentDataLoc;
				needToUpdate = true;
			} else {
				fwPersistentDataLoc = fwConfigLocation;
				needToUpdate = true;
			}
		} else {
			if (fwConfigLocation == null) {
				fwConfigLocation = fwPersistentDataLoc;
				needToUpdate = true;
			}
		}

		File fwJar = launcherData.getFwJar();
		if (fwJar == null) {
			String location = FileUtils.getEclipsePluginFullLocation(EquinoxConstants.FW_SYMBOLIC_NAME, new File(launcherConfigFile.getParent(), EquinoxConstants.PLUGINS_DIR));
			if (location != null)
				try {
					fwJar = new File(new URL(location).getFile());
					launcherData.setFwJar(fwJar);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		// launcherData.initialize(); // reset except launcherFile.

		//		launcherData.setLauncher(launcherFile);
		boolean vmArgsFlag = false;

		for (int i = 0; i < lines.length; i++) {
			final String line = lines[i].trim();
			if (line.startsWith("#")) //$NON-NLS-1$
				continue;
			if (line.length() == 0)
				continue;
			if (vmArgsFlag) {
				launcherData.addJvmArg(line);
				continue;
			}
			if (line.equalsIgnoreCase(EquinoxConstants.OPTION_VMARGS)) {
				vmArgsFlag = true;
				continue;
			}
			if (line.equalsIgnoreCase(EquinoxConstants.OPTION_CONFIGURATION)) {
				final String nextLine = lines[++i].trim();
				File file = new File(nextLine);
				if (!file.isAbsolute())
					file = new File(launcherConfigFile.getParent() + File.separator + nextLine);
				fwPersistentDataLoc = file;
				needToUpdate = true;
				continue;
			} else if (line.equalsIgnoreCase(EquinoxConstants.OPTION_CLEAN)) {
				clean = true;
				needToUpdate = true;
				continue;
			} else if (line.equalsIgnoreCase(EquinoxConstants.OPTION_VM)) {
				final String nextLine = lines[++i].trim();
				File file = new File(nextLine);
				if (!file.isAbsolute()) {
					file = new File(launcherConfigFile.getAbsolutePath() + File.separator + nextLine);
				}
				launcherData.setJvm(file);
				continue;
			} else if (line.equalsIgnoreCase(EquinoxConstants.OPTION_FW)) {
				final String nextLine = lines[++i].trim();
				File file = new File(nextLine);
				if (!file.isAbsolute()) {
					file = new File(launcherConfigFile.getAbsolutePath() + File.separator + nextLine);
				}
				launcherData.setFwJar(file);
				continue;
			} else {
				launcherData.addProgramArg(lines[i]);
				//				Log.log(LogService.LOG_WARNING, this, "parseCmdLine(String[] lines, File inputFile)", "Unsupported by current impl:line=" + line);
			}
		}
		if (needToUpdate) {
			launcherData.setFwPersistentDataLocation(fwPersistentDataLoc, clean);
			launcherData.setFwConfigLocation(fwPersistentDataLoc);
		}
	}

	public void read(LauncherData launcherData) throws IOException {
		final File launcherConfigFile = EquinoxManipulatorImpl.getLauncherConfigLocation(launcherData);
		if (launcherConfigFile == null)
			throw new IllegalStateException(Messages.exception_launcherLocationNotSet);
		if (!launcherConfigFile.exists())
			return;

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(launcherConfigFile));

			String line;
			List list = new LinkedList();
			while ((line = br.readLine()) != null) {
				list.add(line);
			}
			String[] lines = (String[]) list.toArray(new String[list.size()]);
			String osgiInstallArea = getLauncher(lines) != null ? EquinoxManipulatorImpl.makeAbsolute(getLauncher(lines).getPath(), launcherData.getLauncher().getParentFile().getAbsolutePath()) : null;

			String resolveNextLine = null;
			for (int i = 0; i < lines.length; i++) {
				if (resolveNextLine != null) {
					lines[i] = EquinoxManipulatorImpl.makeAbsolute(lines[i], resolveNextLine);
					resolveNextLine = null;
				} else {
					resolveNextLine = needsPathResolution(lines[i], osgiInstallArea, launcherData.getLauncher().getParentFile().getAbsolutePath() + File.separator);
				}
			}
			this.parseCmdLine(launcherData, lines);
		} finally {
			if (br != null)
				br.close();
		}
		Log.log(LogService.LOG_INFO, NLS.bind(Messages.log_configFile, launcherConfigFile.getAbsolutePath()));
	}

	private File getLauncher(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals(EquinoxConstants.OPTION_STARTUP) && i + 1 < args.length && args[i + 1].charAt(1) != '-') {
				IPath parentFolder = new Path(args[i + 1]).removeLastSegments(1);
				if (parentFolder.lastSegment().equals("plugins")) //$NON-NLS-1$
					return parentFolder.removeLastSegments(1).toFile();
				return parentFolder.toFile();
			}
		}
		return null;
	}

	//Return the base against which the path needs to be resolved, if resolution is needed.
	private String needsPathResolution(String entry, String osgiInstallArea, String launcherFolder) {
		if (EquinoxConstants.OPTION_CONFIGURATION.equalsIgnoreCase(entry))
			return osgiInstallArea;
		if ("--launcher.library".equalsIgnoreCase(entry)) //$NON-NLS-1$
			return launcherFolder;
		if (EquinoxConstants.OPTION_STARTUP.equalsIgnoreCase(entry))
			return launcherFolder;
		if (EquinoxConstants.OPTION_FW.equalsIgnoreCase(entry))
			return osgiInstallArea != null ? osgiInstallArea : launcherFolder;
		return null;
	}

	public void save(EquinoxLauncherData launcherData, boolean relative, boolean backup) throws IOException {
		File launcherConfigFile = EquinoxManipulatorImpl.getLauncherConfigLocation(launcherData);

		if (launcherConfigFile == null)
			throw new IllegalStateException(Messages.exception_launcherLocationNotSet);
		Utils.createParentDir(launcherConfigFile);
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

			String launcherFolder = launcherData.getLauncher().getParentFile().getAbsolutePath();
			String[] lines = this.getConfigFileLines(launcherData, launcherConfigFile, relative);
			String osgiInstallArea = getLauncher(lines) != null ? EquinoxManipulatorImpl.makeAbsolute(getLauncher(lines).getPath(), launcherFolder) : launcherFolder;
			String resolveNextLine = null;
			for (int i = 0; i < lines.length; i++) {
				if (resolveNextLine != null) {
					lines[i] = EquinoxManipulatorImpl.makeRelative(lines[i], resolveNextLine);
					resolveNextLine = null;
				} else {
					resolveNextLine = needsPathResolution(lines[i], osgiInstallArea, launcherFolder + File.separator);
					if (EquinoxConstants.OPTION_CONFIGURATION.equalsIgnoreCase(lines[i])) {
						resolveNextLine = null;

						//We don't write -configuration when it is the default value
						File defaultConfigArea = new File(osgiInstallArea, "configuration"); //$NON-NLS-1$
						File specifiedArea = new File(lines[i + 1]);
						if (defaultConfigArea.equals(specifiedArea)) {
							i++;
							continue;
						}

						Path configLocation = new Path(lines[i + 1]);
						Path osgiPath = new Path(osgiInstallArea);
						int commonSegments = osgiPath.matchingFirstSegments(configLocation.removeLastSegments(1));
						if (commonSegments == configLocation.segmentCount() - 1) {
							String path = ""; //$NON-NLS-1$
							for (int j = osgiPath.segmentCount() - commonSegments; j != 0; j--) {
								path += "../"; //$NON-NLS-1$
							}
							path += configLocation.lastSegment();
							lines[i + 1] = path;
						}
					}
				}
				bw.write(lines[i]);
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
