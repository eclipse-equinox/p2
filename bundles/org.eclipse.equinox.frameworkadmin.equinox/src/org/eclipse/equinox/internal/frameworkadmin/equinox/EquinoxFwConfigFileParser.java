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
import java.net.*;
import java.util.Enumeration;
import java.util.Properties;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.frameworkadmin.equinox.utils.FileUtils;
import org.eclipse.equinox.internal.frameworkadmin.utils.Utils;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public class EquinoxFwConfigFileParser {
	private static final String CONFIG_DIR = "@config.dir/"; //$NON-NLS-1$
	private static final String KEY_ECLIPSE_PROV_DATA_AREA = "eclipse.p2.data.area"; //$NON-NLS-1$
	private static final String KEY_ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR_CONFIGURL = "org.eclipse.equinox.simpleconfigurator.configUrl"; //$NON-NLS-1$
	private static final String KEY_OSGI_BUNDLES = "osgi.bundles"; //$NON-NLS-1$

	private static boolean DEBUG = false;
	private static String USE_REFERENCE_STRING = null;

	public EquinoxFwConfigFileParser(BundleContext context) {
		if (context != null)
			USE_REFERENCE_STRING = context.getProperty(EquinoxConstants.PROP_KEY_USE_REFERENCE);

	}

	private static String getCommandLine(BundleInfo bundleInfo) {
		String location = bundleInfo.getLocation().toString();
		if (location == null)
			return null;
		boolean useReference = true;
		if (location.startsWith("file:")) { //$NON-NLS-1$
			if (USE_REFERENCE_STRING != null && USE_REFERENCE_STRING.equals("false")) //$NON-NLS-1$
				useReference = false;
		}

		try {
			new URL(location);
		} catch (MalformedURLException e) {
			Log.log(LogService.LOG_ERROR, "EquinoxFwConfigFileParser.getCommandLine():bundleInfo=" + bundleInfo, e); //$NON-NLS-1$
			//			Never happen. ignore.
		}
		if (useReference)
			if (!location.startsWith("reference:")) //$NON-NLS-1$
				location = "reference:" + location; //$NON-NLS-1$

		int startLevel = bundleInfo.getStartLevel();
		boolean toBeStarted = bundleInfo.isMarkedAsStarted();

		StringBuffer sb = new StringBuffer();
		//		if (baseUrl != null && bundleUrl.getProtocol().equals("file")) {
		//			String bundlePath = bundleUrl.toString();
		//			bundlePath = Utils.getRelativePath(bundleUrl, baseUrl);
		//			sb.append(bundlePath);
		//		} else
		sb.append(location);
		if (startLevel == BundleInfo.NO_LEVEL && !toBeStarted)
			return sb.toString();
		sb.append('@');
		if (startLevel != BundleInfo.NO_LEVEL)
			sb.append(startLevel);
		if (toBeStarted)
			sb.append(":start"); //$NON-NLS-1$
		return sb.toString();
	}

	private static Properties getConfigProps(BundleInfo[] bInfos, ConfigData configData, LauncherData launcherData, boolean relative, File fwJar) {
		Properties props = new Properties();

		if (configData.getInitialBundleStartLevel() != BundleInfo.NO_LEVEL)
			props.setProperty(EquinoxConstants.PROP_BUNDLES_STARTLEVEL, Integer.toString(configData.getInitialBundleStartLevel()));
		if (configData.getBeginingFwStartLevel() != BundleInfo.NO_LEVEL)
			props.setProperty(EquinoxConstants.PROP_INITIAL_STARTLEVEL, Integer.toString(configData.getBeginingFwStartLevel()));

		final File launcher = launcherData.getLauncher();
		if (launcher != null) {
			String launcherName = launcher.getName();
			if (launcherName.endsWith(EquinoxConstants.EXE_EXTENSION)) {
				props.setProperty(EquinoxConstants.PROP_LAUNCHER_NAME, launcherName.substring(0, launcherName.lastIndexOf(EquinoxConstants.EXE_EXTENSION)));
				props.setProperty(EquinoxConstants.PROP_LAUNCHER_PATH, launcher.getParentFile().getAbsolutePath());
			}
		}

		String fwJarSt = null;
		try {
			if (fwJar != null) {
				fwJarSt = fwJar.toURL().toExternalForm();
			}
		} catch (MalformedURLException e) {
			// Never happens
			e.printStackTrace();
		}

		if (bInfos != null) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < bInfos.length; i++) {
				normalizeLocation(bInfos[i]);
				if (fwJarSt != null && fwJarSt.equals(bInfos[i].getLocation()))
					continue; //framework jar should not appear in the bundles list
				sb.append(getCommandLine(bInfos[i]));
				if (i + 1 < bInfos.length)
					sb.append(',');
			}
			props.setProperty(EquinoxConstants.PROP_BUNDLES, sb.toString());

		}
		//TODO The following merging operations are suspicious.
		props = Utils.appendProperties(props, configData.getFwIndependentProps());

		props = Utils.appendProperties(props, configData.getFwDependentProps());

		//Deal with the fw jar and ensure it is not set. 
		//TODO This can't be done before because of the previous calls to appendProperties 
		if (fwJarSt != null)
			props.setProperty(EquinoxConstants.PROP_OSGI_FW, fwJarSt /* fwJar.getAbsolutePath() */);
		else
			props.remove(EquinoxConstants.PROP_OSGI_FW);

		return props;
	}

	private static boolean getMarkedAsStartedFormat(String msg, String original) {
		if (msg == null)
			return false;
		msg = msg.trim();
		int colon = msg.indexOf(":"); //$NON-NLS-1$
		if (colon > -1) {
			return msg.substring(colon + 1).equals("start"); //$NON-NLS-1$
		}
		return msg.equals("start"); //$NON-NLS-1$
	}

	private static int getStartLevel(String msg, String original) {
		if (msg == null)
			return BundleInfo.NO_LEVEL;
		msg = msg.trim();
		int colon = msg.indexOf(":"); //$NON-NLS-1$
		if (colon > 0) {
			try {
				return Integer.parseInt(msg.substring(0, colon));
			} catch (NumberFormatException e) {
				return BundleInfo.NO_LEVEL;
			}
		}
		return BundleInfo.NO_LEVEL;
	}

	static boolean isFwDependent(String key) {
		// TODO This algorithm is temporal. 
		if (key.startsWith(EquinoxConstants.PROP_EQUINOX_DEPENDENT_PREFIX))
			return true;
		return false;
	}

	// TODO: Is this redundant now
	private static void normalizeLocation(BundleInfo bInfo) {

		// TODO: I believe this is redundant now
		//		String location = bInfo.getLocation();
		//		try {
		//			if (location.startsWith("file:")) { //$NON-NLS-1$
		//				bInfo.setLocation(new URL(location).toExternalForm());
		//			} else {
		//				bInfo.setLocation(new File(location).toURL().toExternalForm());
		//			}
		//		} catch (MalformedURLException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}

		//			location = location.substring("file:".length());
		//			if (!location.startsWith("/"))
		//				location = "/" + location;
		//			//		if (fwJarSt != null)
		//			//			if (fwJarSt.equals(location))
		//			//				continue;
		//			location = Utils.replaceAll(location, File.separator, "/");
		//			//String jarName = location.substring(location.lastIndexOf("/") + 1);
		//			//		if (jarName.startsWith(EquinoxConstants.FW_JAR_PLUGIN_NAME))
		//			//			continue;
		//			bInfo.setLocation("file:" + location);
		//		}
	}

	/**
	 * @param value
	 */
	private static void setInstallingBundles(Manipulator manipulator, String value) throws NumberFormatException {
		ConfigData configData = manipulator.getConfigData();
		if (value != null) {
			String[] bInfoStrings = Utils.getTokens(value, ","); //$NON-NLS-1$
			for (int i = 0; i < bInfoStrings.length; i++) {
				String token = bInfoStrings[i].trim();
				token = FileUtils.getRealLocation(manipulator, token, false).trim();

				int indexI = token.indexOf("@"); //$NON-NLS-1$
				String location = (indexI == -1) ? token : token.substring(0, indexI);
				String realLocation = FileUtils.getEclipseRealLocation(manipulator, location);
				String slAndFlag = (indexI > -1) ? token.substring(indexI + 1) : null;

				boolean markedAsStarted = getMarkedAsStartedFormat(slAndFlag, token);
				int startLevel = getStartLevel(slAndFlag, token);

				if (realLocation != null)
					try {
						configData.addBundle(new BundleInfo(URIUtil.fromString(realLocation), startLevel, markedAsStarted));
					} catch (URISyntaxException e) {
						e.printStackTrace();
						throw new IllegalStateException("Error creating location" + e.getMessage());
					}
				else {
					//TODO: Pascal to look at this
					// was --> configData.addBundle(new BundleInfo(location, null, null, startLevel, markedAsStarted));
					//AN : if realLocation is null, we couldn't find it on disk, we probably only have a symbolic id,
					//we are working off of a bundles.list from the config.ini
					configData.addBundle(new BundleInfo(location, null, null, startLevel, markedAsStarted));
				}
			}
		}
	}

	/**
	 * inputFile must be not a directory but a file.
	 * 
	 * @param manipulator
	 * @param inputFile
	 * @throws IOException
	 */
	public void readFwConfig(Manipulator manipulator, File inputFile) throws IOException {
		if (inputFile.isDirectory())
			throw new IllegalArgumentException(NLS.bind(Messages.exception_inputFileIsDirectory, inputFile));

		//Initialize data structures
		ConfigData configData = manipulator.getConfigData();
		LauncherData launcherData = manipulator.getLauncherData();
		configData.initialize();

		// load configuration properties
		Properties props = loadProperties(inputFile);

		//Start figuring out stuffs 
		URI rootURL = launcherData.getLauncher() != null ? launcherData.getLauncher().getParentFile().toURI() : null;

		Properties sharedConfigProperties = getSharedConfiguration(props.getProperty(EquinoxConstants.PROP_SHARED_CONFIGURATION_AREA), getOSGiInstallArea(manipulator.getLauncherData()));
		if (sharedConfigProperties != null) {
			sharedConfigProperties.putAll(props);
			props = sharedConfigProperties;
		}

		//Extracting fwkJar location needs to be done first 
		String launcherName = null;
		String launcherPath = null;
		configData.setBundles(null);

		File fwJar = null;
		if (props.getProperty(EquinoxConstants.PROP_OSGI_FW) != null) {
			URI absoluteFwJar = null;
			try {
				absoluteFwJar = URIUtil.makeAbsolute(new URI(props.getProperty(EquinoxConstants.PROP_OSGI_FW)), getOSGiInstallArea(launcherData).toURI());
			} catch (URISyntaxException e) {
				// TODO Can we do anything to fix this?
			}
			if (absoluteFwJar != null) {
				props.setProperty(EquinoxConstants.PROP_OSGI_FW, absoluteFwJar.toString());
				String fwJarString = props.getProperty(EquinoxConstants.PROP_OSGI_FW);
				if (fwJarString != null) {
					fwJar = URIUtil.toFile(absoluteFwJar);
					if (fwJar == null)
						throw new IllegalStateException("Can't determinate the osgi.install area");
					launcherData.setFwJar(fwJar);
					configData.addBundle(new BundleInfo(absoluteFwJar));
				}
			}
		}
		try {
			props = makeAbsolute(props, rootURL, fwJar, inputFile.getParentFile(), getOSGiInstallArea(manipulator.getLauncherData()));
		} catch (URISyntaxException e) {
			// TODO should we instead be catching this inside makeAbsolute?
		}

		for (Enumeration enumeration = props.keys(); enumeration.hasMoreElements();) {
			String key = (String) enumeration.nextElement();
			String value = props.getProperty(key);
			if (key.equals(EquinoxConstants.PROP_BUNDLES_STARTLEVEL))
				configData.setInitialBundleStartLevel(Integer.parseInt(value));
			else if (key.equals(EquinoxConstants.PROP_INITIAL_STARTLEVEL)) {
				configData.setBeginningFwStartLevel(Integer.parseInt(value));
			} else if (key.equals(EquinoxConstants.PROP_BUNDLES)) {
				setInstallingBundles(manipulator, value);
			} else {
				if (isFwDependent(key)) {
					configData.setFwDependentProp(key, value);
				} else
					configData.setFwIndependentProp(key, value);
				if (key.equals(EquinoxConstants.PROP_LAUNCHER_NAME))
					if (launcherData.getLauncher() == null)
						launcherName = value;
				if (key.equals(EquinoxConstants.PROP_LAUNCHER_PATH))
					if (launcherData.getLauncher() == null)
						launcherPath = value;
			}
		}
		if (launcherName != null && launcherPath != null) {
			launcherData.setLauncher(new File(launcherPath, launcherName + EquinoxConstants.EXE_EXTENSION));
		}

		Log.log(LogService.LOG_INFO, NLS.bind(Messages.log_configFile, inputFile.getAbsolutePath()));
	}

	private static Properties loadProperties(File inputFile) throws FileNotFoundException, IOException {
		Properties props = new Properties();
		InputStream is = null;
		try {
			is = new FileInputStream(inputFile);
			props.load(is);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				Log.log(LogService.LOG_WARNING, NLS.bind(Messages.log_failed_reading_properties, inputFile));
			}
			is = null;
		}
		return props;
	}

	private File findSharedConfigIniFile(URL rootURL, String sharedConfigurationArea) {
		URL sharedConfigurationURL = null;
		try {
			sharedConfigurationURL = new URL(sharedConfigurationArea);
		} catch (MalformedURLException e) {
			// unexpected since this was written by the framework
			Log.log(LogService.LOG_WARNING, NLS.bind(Messages.log_shared_config_url, sharedConfigurationArea));
			return null;
		}

		// check for a relative URL
		if (!sharedConfigurationURL.getPath().startsWith("/")) { //$NON-NLS-1$
			if (!rootURL.getProtocol().equals(sharedConfigurationURL.getProtocol())) {
				Log.log(LogService.LOG_WARNING, NLS.bind(Messages.log_shared_config_relative_url, rootURL.toExternalForm(), sharedConfigurationArea));
				return null;
			}
			try {
				sharedConfigurationURL = new URL(rootURL, sharedConfigurationArea);
			} catch (MalformedURLException e) {
				// unexpected since this was written by the framework
				Log.log(LogService.LOG_WARNING, NLS.bind(Messages.log_shared_config_relative_url, rootURL.toExternalForm(), sharedConfigurationArea));
				return null;
			}
		}
		File sharedConfigurationFolder = new File(sharedConfigurationURL.getPath());
		if (!sharedConfigurationFolder.isDirectory()) {
			Log.log(LogService.LOG_WARNING, NLS.bind(Messages.log_shared_config_file_missing, sharedConfigurationFolder));
			return null;
		}

		File sharedConfigIni = new File(sharedConfigurationFolder, EquinoxConstants.CONFIG_INI);
		if (!sharedConfigIni.exists()) {
			Log.log(LogService.LOG_WARNING, NLS.bind(Messages.log_shared_config_file_missing, sharedConfigIni));
			return null;
		}

		return sharedConfigIni;
	}

	private static Properties makeRelative(Properties props, URI rootURI, File fwJar, File configArea, File osgiInstallArea) throws URISyntaxException {
		if (props.getProperty(EquinoxConstants.PROP_LAUNCHER_PATH) != null)
			props.setProperty(EquinoxConstants.PROP_LAUNCHER_PATH, URIUtil.makeRelative(URIUtil.fromString(props.getProperty(EquinoxConstants.PROP_LAUNCHER_PATH)), rootURI).toString());

		if (props.getProperty(KEY_ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR_CONFIGURL) != null) {
			props.setProperty(KEY_ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR_CONFIGURL, URIUtil.makeRelative(URIUtil.fromString(props.getProperty(KEY_ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR_CONFIGURL)), configArea.toURI()).toString());
		}

		if (props.getProperty(EquinoxConstants.PROP_OSGI_FW) != null && osgiInstallArea != null) {
			props.setProperty(EquinoxConstants.PROP_OSGI_FW, URIUtil.makeRelative(URIUtil.fromString(props.getProperty(EquinoxConstants.PROP_OSGI_FW)), osgiInstallArea.toURI()).toString());
		}

		if (props.getProperty(KEY_ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR_CONFIGURL) != null) {
			props.setProperty(KEY_ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR_CONFIGURL, URIUtil.makeRelative(URIUtil.fromString(props.getProperty(KEY_ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR_CONFIGURL)), configArea.toURI()).toString());
		}

		if (props.getProperty(KEY_ECLIPSE_PROV_DATA_AREA) != null) {
			String dataArea = props.getProperty(KEY_ECLIPSE_PROV_DATA_AREA);
			if (dataArea != null) {
				String result = URIUtil.makeRelative(URIUtil.fromString(dataArea), configArea.toURI()).toString();
				//We only relativize up to the level where the p2 and config folder are siblings (e.g. foo/p2 and foo/config)
				if (result.startsWith("../..")) //$NON-NLS-1$
					result = dataArea;
				else if (!result.equals(dataArea))
					result = CONFIG_DIR + result.substring(5);
				props.setProperty(KEY_ECLIPSE_PROV_DATA_AREA, result);
			}
		}

		String value = props.getProperty(KEY_OSGI_BUNDLES);
		if (value != null && fwJar != null) {
			File parent = fwJar.getParentFile();
			if (parent != null)
				props.setProperty(KEY_OSGI_BUNDLES, URIUtil.makeRelative(URIUtil.fromString(value), parent.toURI()).toString());
		}
		return props;
	}

	private static Properties makeAbsolute(Properties props, URI rootURL, File fwJar, File configArea, File osgiInstallArea) throws URISyntaxException {
		if (props.getProperty(EquinoxConstants.PROP_LAUNCHER_PATH) != null)
			props.setProperty(EquinoxConstants.PROP_LAUNCHER_PATH, URIUtil.makeAbsolute(URIUtil.fromString(props.getProperty(EquinoxConstants.PROP_LAUNCHER_PATH)), rootURL).toString());

		if (props.getProperty(KEY_ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR_CONFIGURL) != null) {
			props.setProperty(KEY_ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR_CONFIGURL, URIUtil.makeAbsolute(URIUtil.fromString(props.getProperty(KEY_ORG_ECLIPSE_EQUINOX_SIMPLECONFIGURATOR_CONFIGURL)), configArea.toURI()).toString());
		}

		if (props.getProperty(KEY_ECLIPSE_PROV_DATA_AREA) != null) {
			String url = props.getProperty(KEY_ECLIPSE_PROV_DATA_AREA);
			if (url != null) {
				if (url.startsWith(CONFIG_DIR))
					url = "file:" + url.substring(CONFIG_DIR.length()); //$NON-NLS-1$
				props.setProperty(KEY_ECLIPSE_PROV_DATA_AREA, URIUtil.makeAbsolute(URIUtil.fromString(url), configArea.toURI()).toString());
			}
		}
		return props;
	}

	public static File getOSGiInstallArea(LauncherData launcherData) {
		if (launcherData == null)
			return null;
		String[] args = launcherData.getProgramArgs();
		if (args == null)
			return null;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-startup") && i + 1 < args.length && args[i + 1].charAt(1) != '-') { //$NON-NLS-1$
				return fromOSGiJarToOSGiInstallArea(args[i + 1]);
			}
		}
		if (launcherData.getFwJar() != null)
			return fromOSGiJarToOSGiInstallArea(launcherData.getFwJar().getAbsolutePath());
		if (launcherData.getLauncher() != null)
			return launcherData.getLauncher().getParentFile();
		return null;
	}

	private static File fromOSGiJarToOSGiInstallArea(String path) {
		IPath parentFolder = new Path(path).removeLastSegments(1);
		if (parentFolder.lastSegment().equals("plugins")) //$NON-NLS-1$
			return parentFolder.removeLastSegments(1).toFile();
		return parentFolder.toFile();
	}

	public void saveFwConfig(BundleInfo[] bInfos, Manipulator manipulator, boolean backup, boolean relative) throws IOException {//{
		ConfigData configData = manipulator.getConfigData();
		LauncherData launcherData = manipulator.getLauncherData();
		File fwJar = EquinoxBundlesState.getFwJar(launcherData, configData);
		launcherData.setFwJar(fwJar);
		File outputFile = launcherData.getFwConfigLocation();
		if (outputFile.exists()) {
			if (outputFile.isFile()) {
				if (!outputFile.getName().equals(EquinoxConstants.CONFIG_INI))
					throw new IllegalStateException(NLS.bind(Messages.exception_fwConfigLocationName, outputFile.getAbsolutePath(), EquinoxConstants.CONFIG_INI));
			} else { // Directory
				outputFile = new File(outputFile, EquinoxConstants.CONFIG_INI);
			}
		} else {
			if (!outputFile.getName().equals(EquinoxConstants.CONFIG_INI)) {
				if (!outputFile.mkdir())
					throw new IOException(NLS.bind(Messages.exception_failedToCreateDir, outputFile));
				outputFile = new File(outputFile, EquinoxConstants.CONFIG_INI);
			}
		}
		String header = "This configuration file was written by: " + this.getClass().getName(); //$NON-NLS-1$

		Properties configProps = getConfigProps(bInfos, configData, launcherData, relative, fwJar);
		if (configProps == null || configProps.size() == 0) {
			Log.log(LogService.LOG_WARNING, this, "saveFwConfig() ", Messages.log_configProps); //$NON-NLS-1$
			return;
		}
		Utils.createParentDir(outputFile);

		if (DEBUG)
			Utils.printoutProperties(System.out, "configProps", configProps); //$NON-NLS-1$

		if (backup)
			if (outputFile.exists()) {
				File dest = Utils.getSimpleDataFormattedFile(outputFile);
				if (!outputFile.renameTo(dest))
					throw new IOException(NLS.bind(Messages.exception_failedToRename, outputFile, dest));
				Log.log(LogService.LOG_INFO, this, "saveFwConfig()", NLS.bind(Messages.log_renameSuccessful, outputFile, dest)); //$NON-NLS-1$
			}

		FileOutputStream out = null;
		try {
			out = new FileOutputStream(outputFile);
			try {
				configProps = makeRelative(configProps, launcherData.getLauncher().getParentFile().toURI(), fwJar, outputFile.getParentFile(), getOSGiInstallArea(manipulator.getLauncherData()));
			} catch (URISyntaxException e) {
				// TODO Should we instead be catching this exception inside makeRelative?
			}
			filterPropertiesFromSharedArea(configProps, launcherData);
			configProps.store(out, header);
			Log.log(LogService.LOG_INFO, NLS.bind(Messages.log_fwConfigSave, outputFile));
		} finally {
			try {
				out.flush();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			out = null;
		}
	}

	private void filterPropertiesFromSharedArea(Properties configProps, LauncherData launcherData) {
		//Remove from the config file that we are about to write the properties that are unchanged compared to what is in the base 
		Properties sharedConfigProperties = getSharedConfiguration(configProps.getProperty(EquinoxConstants.PROP_SHARED_CONFIGURATION_AREA), getOSGiInstallArea(launcherData));
		if (sharedConfigProperties == null)
			return;
		Enumeration keys = configProps.propertyNames();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			String sharedValue = sharedConfigProperties.getProperty(key);
			if (sharedValue == null)
				continue;
			if (equalsIgnoringSeparators(sharedValue, configProps.getProperty(key)))
				configProps.remove(key);
		}
	}

	private boolean equalsIgnoringSeparators(String s1, String s2) {
		if (s1 == null && s2 == null)
			return true;
		if (s1 == null || s2 == null)
			return false;
		StringBuffer sb1 = new StringBuffer(s1);
		StringBuffer sb2 = new StringBuffer(s2);
		canonicalizePathsForComparison(sb1);
		canonicalizePathsForComparison(sb2);
		return sb1.toString().equals(sb2.toString());
	}

	private void canonicalizePathsForComparison(StringBuffer s) {
		final String[] tokens = new String[] {"\\\\", "\\", "//", "/"}; //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
		for (int t = 0; t < tokens.length; t++) {
			int idx = s.length();
			for (int i = s.length(); i != 0 && idx != -1; i--) {
				idx = s.toString().lastIndexOf(tokens[t], idx);
				if (idx != -1)
					s.replace(idx, idx + tokens[t].length(), "^"); //$NON-NLS-1$
			}
		}
	}

	private Properties getSharedConfiguration(String sharedConfigurationArea, File baseFile) {
		if (sharedConfigurationArea == null)
			return null;
		File sharedConfigIni;
		try {
			sharedConfigIni = findSharedConfigIniFile(baseFile.toURL(), sharedConfigurationArea);
		} catch (MalformedURLException e) {
			return null;
		}
		if (sharedConfigIni != null && sharedConfigIni.exists())
			try {
				return loadProperties(sharedConfigIni);
			} catch (FileNotFoundException e) {
				return null;
			} catch (IOException e) {
				return null;
			}
		return null;
	}
}
