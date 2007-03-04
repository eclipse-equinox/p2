/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.frameworkadmin.equinox.internal;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import org.eclipse.equinox.frameworkadmin.*;
import org.eclipse.equinox.internal.frameworkadmin.utils.Utils;
import org.osgi.service.log.LogService;

class EquinoxFwConfigFileParser {
	private static boolean DEBUG = false;

	private static String getCommandLine(BundleInfo bundleInfo, final URL baseUrl) {
		URL bundleUrl = null;
		try {
			bundleUrl = new URL(bundleInfo.getLocation());
		} catch (MalformedURLException e) {
			Log.log(LogService.LOG_ERROR, "EquinoxFwConfigFileParser.getCommandLine():bundleInfo=" + bundleInfo, e);
			//			Never happen. ignore.
		}
		int startLevel = bundleInfo.getStartLevel();
		boolean toBeStarted = bundleInfo.isMarkedAsStarted();

		StringBuffer sb = new StringBuffer();
		//		if (baseUrl != null && bundleUrl.getProtocol().equals("file")) {
		//			String bundlePath = bundleUrl.toString();
		//			bundlePath = Utils.getRelativePath(bundleUrl, baseUrl);
		//			sb.append(bundlePath);
		//		} else
		sb.append(bundleUrl.toString());
		if (startLevel == BundleInfo.NO_LEVEL && !toBeStarted)
			return sb.toString();
		sb.append("@");
		if (startLevel != BundleInfo.NO_LEVEL)
			sb.append(startLevel);
		if (toBeStarted)
			sb.append(":start");
		return sb.toString();
	}

	private static Properties getConfigProps(BundleInfo[] bInfos, ConfigData configData, LauncherData launcherData, boolean relative, File fwJar) {

		Properties props = new Properties();

		if (configData.getInitialBundleStartLevel() != BundleInfo.NO_LEVEL)
			props.setProperty(EquinoxConstants.PROP_BUNDLES_STARTLEVEL, Integer.toString(configData.getInitialBundleStartLevel()));
		if (configData.getBeginingFwStartLevel() != BundleInfo.NO_LEVEL)
			props.setProperty(EquinoxConstants.PROP_INITIAL_STARTLEVEL, Integer.toString(configData.getBeginingFwStartLevel()));
		String fwJarSt = null;
		if (fwJar != null) {
			URL fwJarUrl = null;
			try {
				fwJarUrl = Utils.getUrl("file", null, fwJar.getAbsolutePath());
			} catch (MalformedURLException e) {
				// Never happens
				e.printStackTrace();
			}
			fwJarSt = fwJarUrl.getFile();

			if (!fwJarSt.startsWith("/"))
				fwJarSt = "/" + fwJarSt;
			props.setProperty(EquinoxConstants.PROP_OSGI_FW, fwJar.getAbsolutePath());
		}

		final File launcher = launcherData.getLauncher();
		if (launcher != null) {
			String launcherName = launcher.getName();
			if (launcherName.endsWith(EquinoxConstants.EXE_EXTENSION)) {
				props.setProperty(EquinoxConstants.PROP_LAUNCHER_NAME, launcherName.substring(0, launcherName.lastIndexOf(EquinoxConstants.EXE_EXTENSION)));
				props.setProperty(EquinoxConstants.PROP_LAUNCHER_PATH, launcher.getParentFile().getAbsolutePath());
			}
		}

		if (bInfos != null) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < bInfos.length; i++) {
				normalizeLocation(bInfos[i]);
				sb.append(getCommandLine(bInfos[i], null));
				if (i + 1 < bInfos.length)
					sb.append(",");
			}
			props.setProperty(EquinoxConstants.PROP_BUNDLES, sb.toString());
			setOSGiBundlesExtraData(props, bInfos);

		}
		props = Utils.appendProperties(props, configData.getFwIndependentProps());

		props = Utils.appendProperties(props, configData.getFwDependentProps());
		//props.setProperty(EquinoxConstants.AOL, EquinoxConstants.AOL);
		return props;
	}

	private static String getExtraDataCommandLine(BundleInfo bundleInfo, final URL baseUrl) {
		StringBuffer sb = new StringBuffer();
		sb.append(bundleInfo.getLocation()).append(',').append(bundleInfo.getSymbolicName()).append(',').append(bundleInfo.getVersion());
		return sb.toString();
	}

	private static boolean getMarkedAsStartedFormat(String msg, String original) {
		//boolean ret = false;
		if (msg == null)
			return false;
		msg = msg.trim();
		if (msg.equals("start")) {
			//ret = true;
			return true;
		}
		if (!msg.equals(""))
			new IllegalArgumentException("Invalid Format =" + original);
		return false;
	}

	static boolean isFwDependent(String key) {
		// TODO This algorithm is temporal. 
		if (key.startsWith(EquinoxConstants.PROP_EQUINOX_DEPENDENT_PREFIX))
			return true;
		return false;
	}

	private static void normalizeLocation(BundleInfo bInfo) {
		String location = bInfo.getLocation();
		if (location.startsWith("file:")) {
			location = location.substring("file:".length());
			if (!location.startsWith("/"))
				location = "/" + location;
			//		if (fwJarSt != null)
			//			if (fwJarSt.equals(location))
			//				continue;
			location = Utils.replaceAll(location, File.separator, "/");
			//String jarName = location.substring(location.lastIndexOf("/") + 1);
			//		if (jarName.startsWith(EquinoxConstants.FW_JAR_PLUGIN_NAME))
			//			continue;
			bInfo.setLocation("file:" + location);
		}
	}

	/**
	 * @param value
	 * @throws ManipulatorException
	 */
	private static void setInstallingBundles(ConfigData configData, String value) throws NumberFormatException {
		if (value != null) {
			String[] bInfoStrings = Utils.getTokens(value, ",");
			configData.setBundles(null);
			for (int i = 0; i < bInfoStrings.length; i++) {
				int indexI = bInfoStrings[i].indexOf("@");
				if (indexI == -1) {
					configData.addBundle(new BundleInfo(bInfoStrings[i]));
					//	configData.installingBundlesList.add(new BundleInfo(this.convertUrl(bInfoStrings[i])));
					continue;
				}
				String location = bInfoStrings[i].substring(0, indexI);
				//					URL url = this.convertUrl(bInfoStrings[i].substring(0, indexI));
				String slAndFlag = bInfoStrings[i].substring(indexI + "@".length());
				boolean markedAsStarted = false;
				int startLevel = -1;
				int indexJ = slAndFlag.indexOf(":");
				if (indexJ == -1) {
					markedAsStarted = getMarkedAsStartedFormat(slAndFlag, bInfoStrings[i]);
					// 3 or start
					//					try {
					//startLevel = Integer.parseInt(slAndFlag);
					configData.addBundle(new BundleInfo(location, markedAsStarted));
					continue;
					//					} catch (NumberFormatException nfe) {
					//						throw new ManipulatorException("Invalid Format of bInfoStrings[" + i + "]=" + bInfoStrings[i], nfe, ManipulatorException.OTHERS);
					//					}
				} else if (indexJ == 0) {
					markedAsStarted = getMarkedAsStartedFormat(slAndFlag.substring(indexJ + ":".length()), bInfoStrings[i]);
					configData.addBundle(new BundleInfo(location, startLevel, markedAsStarted));
					continue;
				}
				//				try {
				startLevel = Integer.parseInt(slAndFlag.substring(0, indexJ));
				markedAsStarted = getMarkedAsStartedFormat(slAndFlag.substring(indexJ + ":".length()), bInfoStrings[i]);
				configData.addBundle(new BundleInfo(location, startLevel, markedAsStarted));
				//				} catch (NumberFormatException nfe) {
				//					throw new ManipulatorException("Invalid Format of bInfoStrings[" + i + "]=" + bInfoStrings[i], nfe, ManipulatorException.OTHERS);
				//				}
			}
		}
	}

	private static void setOSGiBundlesExtraData(Properties props, BundleInfo[] bInfos) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < bInfos.length; i++) {
			normalizeLocation(bInfos[i]);
			sb.append(getExtraDataCommandLine(bInfos[i], null));
			if (i + 1 < bInfos.length)
				sb.append(",");
		}
		props.setProperty(EquinoxConstants.PROP_BUNDLES_EXTRADATA, sb.toString());

	}

	/**
	 * inputFile must be not a directory but a file.
	 * 
	 * @param configData
	 * @param inputFile
	 * @throws IOException
	 */
	public void readFwConfig(Manipulator manipulator, File inputFile) throws IOException {
		if (inputFile.isDirectory())
			throw new IllegalArgumentException("inputFile:" + inputFile + " must not be a directory.");

		ConfigData configData = manipulator.getConfigData();
		LauncherData launcherData = manipulator.getLauncherData();
		configData.initialize();

		Properties props = new Properties();
		InputStream is = null;
		try {
			is = new FileInputStream(inputFile);
			props.load(is);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			is = null;
		}

		String launcherName = null;
		String launcherPath = null;
		for (Enumeration enumeration = props.keys(); enumeration.hasMoreElements();) {
			String key = (String) enumeration.nextElement();
			String value = props.getProperty(key);
			if (key.equals(EquinoxConstants.PROP_BUNDLES_STARTLEVEL))
				configData.setInitialBundleStartLevel(Integer.parseInt(value));
			else if (key.equals(EquinoxConstants.PROP_INITIAL_STARTLEVEL)) {
				configData.setBeginningFwStartLevel(Integer.parseInt(value));
			} else if (key.equals(EquinoxConstants.PROP_BUNDLES)) {
				setInstallingBundles(configData, value);
			} else {
				if (isFwDependent(key)) {
					configData.setFwDependentProp(key, value);
				} else
					configData.setFwIndependentProp(key, value);
				if (key.equals(EquinoxConstants.PROP_OSGI_FW))
					if (launcherData.getFwJar() == null)
						launcherData.setFwJar(new File(value));
				if (key.equals(EquinoxConstants.PROP_LAUNCHER_NAME))
					if (launcherData.getLauncher() == null)
						launcherName = value;
				if (key.equals(EquinoxConstants.PROP_LAUNCHER_PATH))
					if (launcherData.getLauncher() == null)
						launcherPath = value;
			}
		}
		if (launcherName != null && launcherPath != null)
			launcherData.setLauncher(new File(launcherPath, launcherName + EquinoxConstants.EXE_EXTENSION));

		Log.log(LogService.LOG_INFO, "Config file(" + inputFile.getAbsolutePath() + ") is read successfully.");
	}

	public void saveFwConfig(BundleInfo[] bInfos, Manipulator manipulator, boolean backup, boolean relative) throws IOException {//{
		ConfigData configData = manipulator.getConfigData();
		LauncherData launcherData = manipulator.getLauncherData();
		File fwJar = EquinoxBundlesState.getFwJar(launcherData, configData);
		if (fwJar != null)
			launcherData.setFwJar(fwJar);
		File outputFile = launcherData.getFwConfigLocation();
		if (outputFile.exists()) {
			if (outputFile.isFile()) {
				if (!outputFile.getName().equals(EquinoxConstants.CONFIG_INI))
					throw new IllegalStateException("launcherData.getFwConfigLocation() is a File but its name doesn't equal " + EquinoxConstants.CONFIG_INI);
			} else { // Directory
				outputFile = new File(outputFile, EquinoxConstants.CONFIG_INI);
			}
		} else {
			if (!outputFile.getName().equals(EquinoxConstants.CONFIG_INI)) {
				if (!outputFile.mkdir())
					throw new IOException("Fail to mkdir (" + outputFile + ")");
				outputFile = new File(outputFile, EquinoxConstants.CONFIG_INI);
			}
		}
		String header = "This properties were written by " + this.getClass().getName();

		Properties configProps = this.getConfigProps(bInfos, configData, launcherData, relative, fwJar);
		if (configProps == null || configProps.size() == 0) {
			Log.log(LogService.LOG_WARNING, this, "saveFwConfig() ", "configProps is empty");
			return;
		}
		Utils.createParentDir(outputFile);

		if (DEBUG)
			Utils.printoutProperties(System.out, "configProps", configProps);

		if (backup)
			if (outputFile.exists()) {
				File dest = Utils.getSimpleDataFormattedFile(outputFile);
				if (!outputFile.renameTo(dest))
					throw new IOException("Fail to rename from (" + outputFile + ") to (" + dest + ")");
				Log.log(LogService.LOG_INFO, this, "saveFwConfig()", "Succeed to rename from (" + outputFile + ") to (" + dest + ")");
			}

		FileOutputStream out = null;
		try {
			out = new FileOutputStream(outputFile);
			configProps.store(out, header);
			Log.log(LogService.LOG_INFO, "FwConfig is saved successfully into:" + outputFile);
		} finally {
			try {
				out.flush();
				out.close();
				//				Log.log(LogService.LOG_INFO, "out is closed successfully.");
			} catch (IOException e) {
				e.printStackTrace();
			}
			out = null;
		}
	}
}
