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

	private String getCommandLine(BundleInfo bundleInfo, final URL baseUrl) {
		URL bundleUrl = null;
		try {
			bundleUrl = new URL(bundleInfo.getLocation());
		} catch (MalformedURLException e) {
			Log.log(LogService.LOG_ERROR, this, "getCommandLine()", "bundleInfo=" + bundleInfo, e);
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

	private Properties getConfigProps(BundleInfo[] bInfos, ConfigData configData, boolean relative, File fwJar) {

		Properties props = new Properties();

		props.setProperty(EquinoxConstants.PROP_BUNDLES_STARTLEVEL, Integer.toString(configData.getInitialBundleStartLevel()));
		props.setProperty(EquinoxConstants.PROP_INITIAL_STARTLEVEL, Integer.toString(configData.getBeginingFwStartLevel()));
		URL fwJarUrl = null;
		try {
			fwJarUrl = Utils.getUrl("file", null, fwJar.getAbsolutePath());
		} catch (MalformedURLException e) {
			// Never happens
			e.printStackTrace();
		}

		String fwJarSt = fwJarUrl.getFile();
		if (!fwJarSt.startsWith("/"))
			fwJarSt = "/" + fwJarSt;

		if (bInfos != null) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < bInfos.length; i++) {
				String location = bInfos[i].getLocation();
				if (location.startsWith("file:")) {
					location = location.substring("file:".length());
					if (!location.startsWith("/"))
						location = "/" + location;
					if (fwJarSt.equals(location))
						continue;
					bInfos[i].setLocation("file:" + location);
				}
				sb.append(getCommandLine(bInfos[i], null));
				if (i + 1 < bInfos.length)
					sb.append(",");
			}
			props.setProperty(EquinoxConstants.PROP_BUNDLES, sb.toString());
		}
		props = Utils.appendProperties(props, configData.getFwIndependentProps());

		props = Utils.appendProperties(props, configData.getFwDependentProps());
		//props.setProperty(EquinoxConstants.AOL, EquinoxConstants.AOL);
		return props;
	}

	private boolean isFwDependent(String key) {
		// TODO This algorithm is temporal. 
		if (key.startsWith(EquinoxConstants.PROP_EQUINOX_DEPENDENT_PREFIX))
			return true;
		return false;
	}

	/**
	 * inputFile must be not a directory but a file.
	 * 
	 * @param configData
	 * @param inputFile
	 * @throws IOException
	 */
	public void readFwConfig(ConfigData configData, File inputFile) throws IOException {
		if (inputFile.isDirectory())
			throw new IllegalArgumentException("inputFile:" + inputFile + " must not be a directory.");

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
				if (this.isFwDependent(key)) {
					configData.setFwDependentProp(key, value);
				} else
					configData.setFwIndependentProp(key, value);
			}
		}

		Log.log(LogService.LOG_INFO, "Config file(" + inputFile.getAbsolutePath() + ") is read successfully.");
	}

	public void saveFwConfig(BundleInfo[] bInfos, Manipulator manipulator, boolean backup, boolean relative) throws IOException {//{
		ConfigData configData = manipulator.getConfigData();
		LauncherData launcherData = manipulator.getLauncherData();
		File fwJar = EquinoxBundlesState.getFwJar(launcherData);

		File outputFile = launcherData.getFwConfigLocation();

		if (outputFile.isDirectory())
			outputFile = new File(outputFile, EquinoxConstants.CONFIG_INI);
		String header = "This properties were written by " + this.getClass().getName();

		//		configData.fwJar = fwJar;
		//		((EquinoxFwConfigInfoImpl) fwConfigData).validate(relative);
		Properties configProps = this.getConfigProps(bInfos, configData, relative, fwJar);
		if (configProps == null || configProps.size() == 0) {
			Log.log(LogService.LOG_WARNING, this, "saveFwConfig() ", "configProps is empty");
			return;
		}
		Utils.createParentDir(outputFile);

		if (DEBUG)
			Utils.printoutProperties(System.out, "configProps", configProps);
		// Properties newProps = reverseProps(configProps);

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
			Log.log(LogService.LOG_INFO, "configProps is stored successfully.");
			//} catch (SecurityException se) {
			//	throw new ManipulatorException("File " + outputFile + " cannot be created because of lack of permission", se, ManipulatorException.OTHERS);
			//		} catch (FileNotFoundException fnfe) {
			//			throw new ManipulatorException("File " + outputFile + " cannot be found", fnfe, ManipulatorException.OTHERS);
			//		} catch (IOException ioe) {
			//			throw new ManipulatorException("Error occured during writing File " + outputFile, ioe, ManipulatorException.OTHERS);
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

	/**
	 * @param value
	 * @throws ManipulatorException
	 */
	private void setInstallingBundles(ConfigData configData, String value) throws NumberFormatException {
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
}
