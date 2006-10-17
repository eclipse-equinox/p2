/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.configurationManipulator;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.core.internal.utils.AlienStateReader;
import org.eclipse.core.internal.utils.BundleSearch;
import org.eclipse.core.simpleConfigurator.ConfigurationConstants;
import org.osgi.framework.Bundle;

// Do we need to be able to add bundle without having the bytes avaialbe?
public class ConfigurationEditor {
	static String CONFIG_LOCATION = ConfigurationConstants.CONFIGURATOR_FOLDER + '/' + ConfigurationConstants.CONFIG_LIST;
	static String INI_FILE_LOCATION = ConfigurationConstants.CONFIG_INI;
	static String LAUNCHER_SUFFIX = ".ini";
	static String DEFAULT_LAUNCHER_NAME = "eclipse";

	public static final String PROP_FRAMEWORK = "osgi.framework"; //$NON-NLS-1$
	public static final String PROP_SPLASH_PATH = "osgi.splashPath"; //$NON-NLS-1$
	public static final String PROP_SPLASH_LOCATION = "osgi.splashLocation"; //$NON-NLS-1$
	public static final String PROP_BUNDLES = "osgi.bundles"; //$NON-NLS-1$
	public static final String PROP_BUNDLES_EXTRADATA = "osgi.bundles.extraData"; //$NON-NLS-1$

	private static final String PROP_LAUNCHER_PATH = "osgi.launcherPath"; //$NON-NLS-1$
	private static final String PROP_LAUNCHER_NAME = "osgi.launcherIni"; //$NON-NLS-1$

	private static final String SIMPLE_CONFIGURATOR_ID = "org.eclipse.core.simpleConfigurator";

	Collection bundles = new ArrayList();
	File configurationLocation;
	File launcherLocation;

	//Information from the config.ini
	String osgiFramework;
	String osgiSplashPath;
	String osgiSplashLocation;
	Properties configProperties = new Properties();

	//Information from the <exe>.ini
	String vmLocation;
	List vmArgs = new ArrayList();
	List programArgs = new ArrayList();
	String configurationTorun;
	String launcherName;

	public ConfigurationEditor(File configLocation, File launcherLocation) {
		configurationLocation = configLocation;
		this.launcherLocation = launcherLocation;
		try {
			BundleInfo[] existingBundles = new ConfigurationReader(configLocation.toURL()).getExpectedState();
			bundles.addAll(Arrays.asList(existingBundles));
			// TODO Need to think about the consequence of adding directly to the bundles collectino directly. Does the order matter?
			loadConfigIni();
			spoofupConfigurationDataFromBundleList();
			loadExeIni();
			loadStateFile(); 
		} catch (MalformedURLException e) {
			//TODO may log something
			//Ignore
		}
	}

	private void spoofupConfigurationDataFromBundleList() {
		if (bundles.size() != 0)
			return;
		if (configProperties.getProperty(PROP_BUNDLES) != null) {
			List bundleInfos = deserializeBundleList(configProperties.getProperty(PROP_BUNDLES), null);
			bundles.addAll(bundleInfos);
		}
	}

	public File getConfigurationLocation() {
		return configurationLocation;
	}

	public boolean isRunning() {
		File[] match = new File(configurationLocation, "org.eclipse.osgi/.manager").listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if (name.endsWith(".instance")) {
					return true;
				}
				return false;
			}
		});
		if (match == null || match.length == 0)
			return false;

		if (match[0].delete())
			return false;

		return true;
	}

	public void save() {
		saveList(bundles);
		saveExeIni();
		saveConfigIni(computeBundleList());
	}

	public void add(BundleInfo bundle) {
		bundles.add(bundle);
		if ("org.eclipse.osgi".equals(bundle.getSymbolicName())) {
			setFrameworkLocation(bundle.getLocation());
			return;
			//			setProperty("eclipse.product", "org.eclipse.platform.ide");
		}
		//		if ("org.eclipse.core.runtime".equals(bundle.getSymbolicName())) {
		//			addToBundleList(bundle);
		//		} else 
		//			if ("org.eclipse.core.simpleConfigurator".equals(bundle.getSymbolicName())) {
		//				
		//			}
		//			addToBundleList(bundle);
		//		} else 
		//		} else if ("org.eclipse.platform".equals(bundle.getSymbolicName())) {
		//			// todo this is hard-coded for now. We need to get the location of the primary
		//			// feature's plug-in but the bundle info doesn't tell us if we have a plug-in or a feature.
		//			String location = System.getProperty("cic.eclipse.repoLocation");
		//			if (location != null) {
		//				location = "file:/" + location + "/plugins/org.eclipse.platform_3.1.0.jar";
		//				setSplashPath(location);
		//			}
		//		}
	}

	private boolean launcherParameterSet() {
		return (vmArgs != null && vmArgs.size() != 0) || vmLocation != null || (programArgs != null && programArgs.size() != 0);
	}

	private void cleanupLauncherFile(String fileName) {
		if (fileName == null)
			return;
		File configFile = new File(launcherLocation, fileName + LAUNCHER_SUFFIX);
		configFile.delete();
	}

	private void saveExeIni() {
		if (launcherLocation == null)
			return;

		if (!launcherParameterSet()) {
			cleanupLauncherFile(launcherName);
			cleanupLauncherFile(configProperties.getProperty(PROP_LAUNCHER_NAME));
			return;
		}

		if (launcherName == null)
			launcherName = DEFAULT_LAUNCHER_NAME;
		try {
			File configFile = new File(launcherLocation, launcherName + LAUNCHER_SUFFIX);
			configFile.getParentFile().mkdirs();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile)));
			try {
				if (programArgs != null && programArgs.size() != 0) {
					for (Iterator iter = programArgs.iterator(); iter.hasNext();) {
						writer.write(((String) iter.next()).trim());
						writer.newLine();
					}
				}
				if (vmLocation != null) {
					writer.write("-vm");
					writer.newLine();
					writer.write(vmLocation);
					writer.newLine();
				}
				if (configurationTorun != null) {
					writer.write("-configuration");
					writer.newLine();
					writer.write(getConfigurationFile().getAbsolutePath());
				}
				if (vmArgs != null && vmArgs.size() != 0) {
					writer.write("-vmargs");
					writer.newLine();
					for (Iterator iter = vmArgs.iterator(); iter.hasNext();) {
						writer.write(((String) iter.next()).trim());
						writer.newLine();
					}
				}
				writer.flush();
			} finally {
				try {
					writer.close();
				} catch (IOException ex) {
					// ignore
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (launcherName != configProperties.getProperty(PROP_LAUNCHER_NAME))
				cleanupLauncherFile(configProperties.getProperty(PROP_LAUNCHER_NAME));
		}
	}

	private File getConfigurationFile() {
		return new File(configurationLocation, CONFIG_LOCATION);
	}

	private void saveList(Collection toPersist) {
		try {
			File configFile = getConfigurationFile();
			configFile.getParentFile().mkdirs();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile)));
			try {
				for (Iterator iter = toPersist.iterator(); iter.hasNext();) {
					BundleInfo toWrite = (BundleInfo) iter.next();
					writer.write(toWrite.getSymbolicName() + ',' + toWrite.getVersion() + ',' + (toWrite.getLocation() == null ? "" : toWrite.getLocation()) + ',' + toWrite.getStartLevel() + ',' + toWrite.expectedState());
					writer.newLine();
				}
				writer.flush();
			} finally {
				try {
					writer.close();
				} catch (IOException ex) {
					// ignore
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * Return the list of bundles that should go on the osgi.bundles property.
	 * This list should be constituted of:
	 * - the simple configurator (if it is present)
	 * - all the plug-ins that have a start level inferior to the start level of the simple configurator
	 * - all the required plug-ins from the previous set
	 **/
	private Collection computeBundleList() {
		//Set of startLevel --> ArrayList
		Map bundlesByStartLevel = new HashMap(5);
		int configuratorStartLevel = BundleInfo.NO_LEVEL; //used to remember the start level of the configurator and to short-circuit the string comparison

		//First, sort the bundles by start level
		for (Iterator iter = bundles.iterator(); iter.hasNext();) {
			BundleInfo entry = (BundleInfo) iter.next();
			int startLevel = entry.getStartLevel();
			//TODO Do we need to take into account if the simple configurator is started?
			if (configuratorStartLevel == BundleInfo.NO_LEVEL && SIMPLE_CONFIGURATOR_ID.equals(entry.getSymbolicName())) {
				configuratorStartLevel = startLevel;
			}
			ArrayList toAdd = (ArrayList) bundlesByStartLevel.get(new Integer(startLevel));
			if (toAdd == null) {
				toAdd = new ArrayList();
				bundlesByStartLevel.put(new Integer(startLevel), toAdd);
			}
			toAdd.add(entry);
		}

		//The configurator is not part of the bundles
		if (configuratorStartLevel == BundleInfo.NO_LEVEL) {
			return bundles;
		}

		//Second, create the resulting lists
		Collection toBundleList = new ArrayList();

		if (defaultStartLevel != 0 && defaultStartLevel < configuratorStartLevel)
			toBundleList.addAll((ArrayList) bundlesByStartLevel.remove(new Integer(defaultStartLevel)));

		for (Iterator iter = bundlesByStartLevel.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			int currentStartLevel = ((Integer) entry.getKey()).intValue();
			if (currentStartLevel <= configuratorStartLevel && currentStartLevel != BundleInfo.NO_LEVEL) {
				toBundleList.addAll((ArrayList) entry.getValue());
			}
		}

		return toBundleList;
	}

	private void saveConfigIni(Collection toPersist) {
		try {
			if (osgiFramework != null)
				configProperties.put(PROP_FRAMEWORK, osgiFramework);
			if (osgiSplashPath != null)
				configProperties.put(PROP_SPLASH_PATH, osgiSplashPath);
			if (osgiSplashLocation != null)
				configProperties.put(PROP_SPLASH_LOCATION, osgiSplashLocation);
			if (launcherName != null)
				configProperties.put(PROP_LAUNCHER_NAME, launcherName);
			if (launcherLocation != null)
				configProperties.put(PROP_LAUNCHER_PATH, launcherLocation.getAbsolutePath());

			String[] bundlesInfo = serializeBundleList(toPersist);
			configProperties.put(PROP_BUNDLES, bundlesInfo[0]);
			configProperties.put(PROP_BUNDLES_EXTRADATA, bundlesInfo[1]);
			OutputStream output = new BufferedOutputStream(new FileOutputStream(new File(configurationLocation, INI_FILE_LOCATION)));
			try {
				configProperties.store(output, "");
			} finally {
				try {
					output.close();
				} catch (IOException ex) {
					// ignore
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String[] serializeBundleList(Collection toPersist) {
		//TODO Need to know what to do with the bundles that are loaded from the fwk and do not have a location
		StringBuffer bundleList = new StringBuffer();
		StringBuffer bundleListExtraData = new StringBuffer();

		for (Iterator iter = toPersist.iterator(); iter.hasNext();) {
			BundleInfo toWrite = (BundleInfo) iter.next();
			if (toWrite.getLocation() == null)
				continue;
			String location = toWrite.getLocation();
			int startLevel = toWrite.getStartLevel();
			int expected = toWrite.expectedState();
			bundleList.append(location);
			if (startLevel != BundleInfo.NO_LEVEL)
				bundleList.append('@').append(startLevel).append(expected == Bundle.ACTIVE ? ":start" : "");
			bundleList.append(',');
			bundleListExtraData.append(toWrite.getLocation()).append(',').append(toWrite.getSymbolicName()).append(',').append(toWrite.getVersion()).append(',');
		}
		return new String[] {bundleList.toString(), bundleListExtraData.toString()};
	}

	private Map parseAdditionalData(String data) {
		if (data == null)
			return new Properties();

		String[] splitData = data.split(",");
		Map result = new HashMap();
		for (int i = 0; i < splitData.length; i = i + 3) {
			result.put(splitData[i], new String[] {splitData[i + 1], splitData[i + 2]});
		}
		return result;
	}

	private List deserializeBundleList(String bundleList, String additionalData) {
		String[] bundles = getArrayFromList(bundleList, ",");
		List bundleInfos = new ArrayList(bundles.length);

		for (int i = 0; i < bundles.length; i++) {
			BundleInfo toAdd = new BundleInfo();
			int index = bundles[i].indexOf('@');
			if (index >= 0) {
				int startIndex = bundles[i].indexOf(':', index);

				if (startIndex > 0 && bundles[i].substring(startIndex + 1).equalsIgnoreCase("start"))
					toAdd.setExpectedState(Bundle.ACTIVE);
				else
					startIndex = bundles[i].length();

				toAdd.setStartLevel(Integer.parseInt(bundles[i].substring(index + 1, startIndex)));
			} else {
				index = bundles[i].length();
			}
			String location = bundles[i].substring(0, index);
			String verifiedLocation;
			try {
				URL foundLocation = BundleSearch.searchForBundle(location, pluginFolder());
				if (foundLocation != null)
					verifiedLocation = foundLocation.toExternalForm();
				else 
					verifiedLocation = location;
			} catch (MalformedURLException e) {
				verifiedLocation = location;
			}
			toAdd.setLocation(verifiedLocation);

			String[] nameAndVersion = getActualBundleNameAndVersion(verifiedLocation);
			toAdd.setSymbolicName(nameAndVersion[0]);
			toAdd.setVersion(nameAndVersion[1]);

			bundleInfos.add(toAdd);
		}
		return bundleInfos;
	}

	private String[] getActualBundleNameAndVersion(String location) { //TODO this should just read the manifest.
		if (location.endsWith(".jar"))
			location = location.substring(0, location.length() - 4);
		if (location.endsWith("/"))
			location = location.substring(0, location.length() -1);
		
		int versionSeparator = location.lastIndexOf('_');
		int bundleNameStart = location.lastIndexOf('/');
		if (bundleNameStart == location.length())
			bundleNameStart = location.lastIndexOf('/', location.length() - 1);
		if (bundleNameStart == -1)
			bundleNameStart = location.lastIndexOf('\\');
		if (bundleNameStart == -1)
			bundleNameStart = 0;

		String name = null;
		String version = null;

		if (versionSeparator == -1) {
			name = location.substring(bundleNameStart + 1, location.length());
			return new String[] {name, version};
		}

		name = location.substring(bundleNameStart + 1, versionSeparator);
		version = location.substring(versionSeparator + 1, location.length());
		return new String[] {name, version};

	}

	private String pluginFolder() {
		if (osgiFramework == null)
			return new File(getConfigurationLocation().getParentFile(), "plugins/").toString();

		return "";
		//TODO Need to compute the plugin folder from the location of the framework if the framework location is provided.
		//Is there in the EclipseStarter code a property that indicates where to search for the bundles, or is it always relative to the fwk?
		//		try {
		//			new URL(osgiFramework)
		//			String result = BundleSearch.searchForBundle(osgiFramework, "").toExternalForm();
		//			if (result != null)
		//				return result;
		//			else 
		//				return "";
		//		} catch (MalformedURLException e) {
		//			return "";
		//		}
	}

	private void processAdditionalDataFromBundleList(BundleInfo bundle, Map additionalData) {
		String[] bundleData = (String[]) additionalData.get(bundle.getLocation());
		if (bundleData == null)
			return; //TODO Here instead we may want to look into the bundle

		if (bundleData[0] != null)
			bundle.setSymbolicName(bundleData[0]);
		if (bundleData[1] != null)
			bundle.setVersion(bundleData[1]);
	}

	private void loadConfigIni() {
		BufferedInputStream inputStream = null;
		try {
			inputStream = new BufferedInputStream(new FileInputStream(new File(configurationLocation, INI_FILE_LOCATION)));
			configProperties.load(inputStream);
		} catch (FileNotFoundException e) {
			//TODO to log in debug mode
			//System.out.println("The config.ini file has not been found. This is fine.");
		} catch (IOException e) {
			//TODO To log in debug mode
			e.printStackTrace();
		} finally {
			if (inputStream != null)
				try {
					inputStream.close();
				} catch (IOException e) {
					//Ignore
				}
		}
		osgiFramework = (String) configProperties.remove(PROP_FRAMEWORK);
		osgiSplashPath = (String) configProperties.remove(PROP_SPLASH_PATH);
		osgiSplashLocation = (String) configProperties.remove(PROP_SPLASH_LOCATION);
		//		bundles.addAll(deserializeBundleList((String) configProperties.remove(PROP_BUNDLES), (String) configProperties.remove(PROP_BUNDLES_EXTRADATA)));
		launcherName = configProperties.getProperty(PROP_LAUNCHER_NAME);

		//If the launcher location has not been specified, see if one can be derived from properties in the config.ini
		if (launcherLocation == null) {
			String launcherPath = configProperties.getProperty(PROP_LAUNCHER_PATH);
			if (launcherPath != null)
				launcherLocation = new File(launcherPath);
		}
	}

	private void loadExeIni() {
		BufferedReader reader = null;
		try {
			if (launcherName == null)
				return;
			File exeFile = new File(launcherLocation, launcherName + LAUNCHER_SUFFIX);
			if (!exeFile.exists())
				return;
			reader = new BufferedReader(new FileReader(exeFile));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.equals("-vm")) {
					vmLocation = reader.readLine();
					continue;
				}
				if (line.equals("-vmargs"))
					continue;
				vmArgs.add(line);
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					//ignore
				}
		}
	}

	private void loadStateFile() {
		AlienStateReader alienState = new AlienStateReader(configurationLocation, null);
		BundleInfo[] installedBundles = alienState.getBundles();

		if(installedBundles == null)
			return;
		//Add to the list of bundles the one that are not already present.
		//We assume that if a bundle is present in the state and in the list it is because 
		//it has been installed by the configurator
		for (int i = 0; i < installedBundles.length; i++) {
			if (! bundles.contains(installedBundles[i]))
				bundles.add(installedBundles[i]);
		}
		//TODO Here we may want to merge the bundle info with ones that would have been loaded in the previous steps in order to have more data
		//We might want to do a quick bundle search
	}
	
	private static String[] getArrayFromList(String prop, String separator) {
		if (prop == null || prop.trim().equals("")) //$NON-NLS-1$
			return new String[0];
		Vector list = new Vector();
		StringTokenizer tokens = new StringTokenizer(prop, separator); //$NON-NLS-1$
		while (tokens.hasMoreTokens()) {
			String token = tokens.nextToken().trim();
			if (!token.equals("")) //$NON-NLS-1$
				list.addElement(token);
		}
		return list.isEmpty() ? new String[0] : (String[]) list.toArray(new String[list.size()]);
	}

	public void remove(BundleInfo toRemove) {
		bundles.remove(toRemove);
	}

//	public boolean validate(String os, String ws, String arch, String nl, String jre) {
//		ConfigurationState state = new ConfigurationState();
//		for (Iterator iter = bundles.iterator(); iter.hasNext();) {
//			BundleInfo toAdd = (BundleInfo) iter.next();
//			String location = toAdd.getLocation();
//			if (location.startsWith("reference:file:")) //TODO Need to find a better way to do this
//				location = location.substring(15);
//			state.addBundle(new File(location));
//		}
//		state.resolveState(os, ws, arch, nl, jre);
//		if (state.getState().getResolvedBundles().length != state.getState().getBundles().length)
//			return false;
//		return true;
//	}

	public void setFrameworkLocation(String path) {
		osgiFramework = path;
	}

	//	public void addToBundleList(BundleInfo bundle) {
	//		bundleList.add(bundle);
	//	}
	//
	//	public void removeFromBundleList(BundleInfo bundle) {
	//		bundleList.remove(bundle);
	//	}

	public void setProperty(String key, String value) {
		configProperties.put(key, value);
	}

	public void setSplashPath(String path) {
		osgiSplashPath = path;
	}

	public void setSplashLocation(String path) {
		osgiSplashLocation = path;
	}

	public void addVMArg(String arg) {
		vmArgs.add(arg);
	}

	public void removeVMArg(String arg) {
		vmArgs.remove(arg);
	}

	public String[] getVMArgs() {
		return (String[]) vmArgs.toArray(new String[vmArgs.size()]);
	}

	public void setVMLocation(String path) {
		vmLocation = path;
	}

	public void setInitialStartLevel() {
		throw new UnsupportedOperationException();
	}

	public void setLauncherName(String name) {
		launcherName = name;
	}

	public void addProgramArgument(String arg) {
		programArgs.add(arg);
	}

	public void removeProgramArgument(String arg) {
		programArgs.remove(arg);
	}

	public String[] getProgramArguments() {
		return (String[]) programArgs.toArray(new String[programArgs.size()]);
	}

	int defaultStartLevel;

	public void setDefaultStartLevel(int defaultStartLevel) {
		this.defaultStartLevel = defaultStartLevel;
	}
	
	public BundleInfo[] getBundles() {
		return (BundleInfo[]) bundles.toArray(new BundleInfo[bundles.size()]);
	}
	
	public BundleInfo[] resolve() {
		return null;
	}
}
