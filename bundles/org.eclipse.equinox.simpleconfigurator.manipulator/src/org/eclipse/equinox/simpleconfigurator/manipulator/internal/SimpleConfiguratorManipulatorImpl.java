/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.simpleconfigurator.manipulator.internal;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.configuratormanipulator.ConfiguratorManipulator;
import org.eclipse.equinox.frameworkadmin.*;
import org.eclipse.equinox.internal.frameworkadmin.utils.Utils;
import org.eclipse.equinox.internal.simpleconfigurator.utils.SimpleConfiguratorConstants;
import org.osgi.framework.Constants;

public class SimpleConfiguratorManipulatorImpl implements ConfiguratorManipulator {
	class LocationInfo {
		String[] prerequisiteLocations = null;
		String systemBundleLocation = null;
		String[] systemFragmentedBundleLocations = null;
	}

	private final static boolean DEBUG = false;

	static String CONFIG_LOCATION = SimpleConfiguratorConstants.CONFIG_LIST;
	private static final String FILE_PROTOCOL = "file:"; //$NON-NLS-1$

	private static final BundleInfo[] NULL_BUNDLEINFOS = new BundleInfo[0];

	private Set manipulators = new HashSet();

	/**	
	 * Return the ConfiguratorConfigLocation which is determined 
	 * by the parameters set in Manipulator. 
	 * 
	 * @param manipulator
	 * @return URL
	 */
	private static URL getConfigLocation(Manipulator manipulator) throws IllegalStateException {
		File fwConfigLoc = manipulator.getLauncherData().getFwConfigLocation();
		File baseDir = null;
		if (fwConfigLoc == null) {
			baseDir = manipulator.getLauncherData().getHome();
			if (baseDir == null) {
				if (manipulator.getLauncherData().getLauncher() != null) {
					baseDir = manipulator.getLauncherData().getLauncher().getParentFile();
				} else {
					throw new IllegalStateException("All of fwConfigFile, home, launcher are not set.");
				}
			}
		} else {
			if (fwConfigLoc.exists())
				if (fwConfigLoc.isDirectory())
					baseDir = fwConfigLoc;
				else
					baseDir = fwConfigLoc.getParentFile();
			else {
				// TODO We need to decide whether launcher data configLocation is the location of a file or a directory 
				if (fwConfigLoc.getName().endsWith(".ini"))
					baseDir = fwConfigLoc.getParentFile();
				else
					baseDir = fwConfigLoc;
			}
		}
		try {
			baseDir = new File(baseDir, SimpleConfiguratorConstants.CONFIGURATOR_FOLDER);
			File targetFile = new File(baseDir, SimpleConfiguratorConstants.CONFIG_LIST);
			try {
				Utils.createParentDir(targetFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
			return targetFile.toURL();
		} catch (MalformedURLException e) {
			// Never happen. ignore.
			e.printStackTrace();
			return null;
		}

	}

	static boolean isPrerequisiteBundles(String location, LocationInfo info) {
		boolean ret = false;

		if (info.prerequisiteLocations == null)
			return false;
		for (int i = 0; i < info.prerequisiteLocations.length; i++)
			if (location.equals(info.prerequisiteLocations[i])) {
				ret = true;
				break;
			}

		return ret;
	}

	static boolean isSystemBundle(String location, LocationInfo info) {
		if (info.systemBundleLocation == null)
			return false;
		if (location.equals(info.systemBundleLocation))
			return true;
		return false;
	}

	static boolean isSystemFragmentBundle(String location, LocationInfo info) {
		boolean ret = false;
		if (info.systemFragmentedBundleLocations == null)
			return false;
		for (int i = 0; i < info.systemFragmentedBundleLocations.length; i++)
			if (location.equals(info.systemFragmentedBundleLocations[i])) {
				ret = true;
				break;
			}
		return ret;
	}

	private static boolean isTargetConfiguratorBundle(BundleInfo[] bInfos) {
		for (int i = 0; i < bInfos.length; i++) {
			if (isTargetConfiguratorBundle(bInfos[i].getLocation())) {
				return true;
				//TODO confirm that startlevel of configurator bundle must be no larger than beginning start level of fw. However, there is no way to know the start level of cached ones.
			}
		}
		return false;
	}

	private static boolean isTargetConfiguratorBundle(String location) {
		final String symbolic = Utils.getPathFromClause(Utils.getManifestMainAttributes(location, Constants.BUNDLE_SYMBOLICNAME));
		return (SimpleConfiguratorConstants.SERVICE_PROP_VALUE_CONFIGURATOR_SYMBOLICNAME.equals(symbolic));
	}

	private void algorithm(int initialSl, SortedMap bslToList, BundleInfo configuratorBInfo, List setToInitialConfig, List setToSimpleConfig, LocationInfo info) {
		int configuratorSL = configuratorBInfo.getStartLevel();

		Integer sL0 = (Integer) bslToList.keySet().iterator().next();// StartLevel == 0;
		List list0 = (List) bslToList.get(sL0);
		if (sL0.intValue() == 0)
			for (Iterator ite2 = list0.iterator(); ite2.hasNext();) {
				BundleInfo bInfo = (BundleInfo) ite2.next();
				if (isSystemBundle(bInfo.getLocation(), info)) {
					setToSimpleConfig.add(bInfo);
					break;
				}
			}

		for (Iterator ite = bslToList.keySet().iterator(); ite.hasNext();) {
			Integer sL = (Integer) ite.next();
			List list = (List) bslToList.get(sL);

			if (sL.intValue() < configuratorSL) {
				for (Iterator ite2 = list.iterator(); ite2.hasNext();) {
					BundleInfo bInfo = (BundleInfo) ite2.next();
					if (!isSystemBundle(bInfo.getLocation(), info))
						setToInitialConfig.add(bInfo);
				}
			} else if (sL.intValue() > configuratorSL) {
				for (Iterator ite2 = list.iterator(); ite2.hasNext();) {
					BundleInfo bInfo = (BundleInfo) ite2.next();
					if (isPrerequisiteBundles(bInfo.getLocation(), info) || isSystemFragmentBundle(bInfo.getLocation(), info))
						if (!isSystemBundle(bInfo.getLocation(), info))
							setToInitialConfig.add(bInfo);
					setToSimpleConfig.add(bInfo);
				}
			} else {
				boolean found = false;
				for (Iterator ite2 = list.iterator(); ite2.hasNext();) {
					BundleInfo bInfo = (BundleInfo) ite2.next();
					if (found) {
						if (!isSystemBundle(bInfo.getLocation(), info))
							if (isPrerequisiteBundles(bInfo.getLocation(), info) || isSystemFragmentBundle(bInfo.getLocation(), info))
								setToInitialConfig.add(bInfo);
						setToSimpleConfig.add(bInfo);
						continue;
					}
					if (isTargetConfiguratorBundle(bInfo.getLocation()))
						found = true;
					else if (!isSystemBundle(bInfo.getLocation(), info))
						setToInitialConfig.add(bInfo);
					setToSimpleConfig.add(bInfo);
				}
			}
		}

		setToInitialConfig.add(configuratorBInfo);
	}

	private boolean checkResolve(BundleInfo bInfo, BundlesState state) {//throws ManipulatorException {
		if (bInfo == null)
			throw new IllegalArgumentException("bInfo is null.");

		if (!state.isResolved())
			state.resolve(false);
		//		if (DEBUG)
		//			System.out.println(state.toString());

		if (!state.isResolved(bInfo)) {
			printoutUnsatisfiedConstraints(bInfo, state);
			return false;
		}
		return true;
	}

	private boolean divideBundleInfos(Manipulator manipulator, List setToInitialConfig, List setToSimpleConfig, final int initialBSL) throws IOException {
		BundlesState state = manipulator.getBundlesState();
		BundleInfo[] targetBundleInfos = null;
		if (state.isFullySupported()) {
			targetBundleInfos = state.getExpectedState();
		} else {
			targetBundleInfos = manipulator.getConfigData().getBundles();
		}
		BundleInfo configuratorBInfo = null;
		for (int i = 0; i < targetBundleInfos.length; i++) {
			if (isTargetConfiguratorBundle(targetBundleInfos[i].getLocation())) {
				if (targetBundleInfos[i].isMarkedAsStarted()) {
					configuratorBInfo = targetBundleInfos[i];
					break;
				}
			}
		}
		if (configuratorBInfo == null && !manipulators.contains(manipulator)) {
			return false;
		} else if (manipulators.contains(manipulator) && targetBundleInfos.length == 0) {
			// Resulting state will have no bundles - so is an uninstall, including
			// uninstall of the configurator. However, we have seen this manipulator
			// before with a target configurator bundle, so allow uninstall to proceed,
			// but only get one chance.
			manipulators.remove(manipulator);
		} else if (!manipulators.contains(manipulator)) {
			manipulators.add(manipulator);
		}

		if (state.isFullySupported()) {
			state.resolve(false);
		}

		LocationInfo info = new LocationInfo();
		setSystemBundles(state, info);
		if (configuratorBInfo != null) {
			setPrerequisiteBundles(configuratorBInfo, state, info);
			SortedMap bslToList = getSortedMap(initialBSL, targetBundleInfos);
			algorithm(initialBSL, bslToList, configuratorBInfo, setToInitialConfig, setToSimpleConfig, info);
		}
		return true;
	}

	private SortedMap getSortedMap(int initialSl, BundleInfo[] bInfos) {
		SortedMap bslToList = new TreeMap();
		for (int i = 0; i < bInfos.length; i++) {
			Integer sL = new Integer(bInfos[i].getStartLevel());
			if (sL.intValue() == BundleInfo.NO_LEVEL)
				sL = new Integer(initialSl);
			List list = (List) bslToList.get(sL);
			if (list == null) {
				list = new LinkedList();
				bslToList.put(sL, list);
			}
			list.add(bInfos[i]);
		}
		return bslToList;
	}

	private BundleInfo[] orderingInitialConfig(List setToInitialConfig) {
		List notToBeStarted = new LinkedList();
		List toBeStarted = new LinkedList();
		for (Iterator ite2 = setToInitialConfig.iterator(); ite2.hasNext();) {
			BundleInfo bInfo = (BundleInfo) ite2.next();
			if (bInfo.isMarkedAsStarted())
				toBeStarted.add(bInfo);
			else
				notToBeStarted.add(bInfo);
		}
		setToInitialConfig.clear();
		setToInitialConfig.addAll(notToBeStarted);
		setToInitialConfig.addAll(toBeStarted);
		return Utils.getBundleInfosFromList(setToInitialConfig);

	}

	private void printoutUnsatisfiedConstraints(BundleInfo bInfo, BundlesState state) {
		if (DEBUG) {
			StringBuffer sb = new StringBuffer();
			sb.append("Missing constraints:\n");
			String[] missings = state.getUnsatisfiedConstraints(bInfo);
			for (int i = 0; i < missings.length; i++)
				sb.append(" " + missings[i] + "\n");
			System.out.println(sb.toString());
		}
	}

	private BundleInfo[] loadConfiguration(URL url, File launcherLocation) throws IOException {
		if (url == null)
			return NULL_BUNDLEINFOS;

		try {
			url.openStream();
		} catch (FileNotFoundException e) {
			return NULL_BUNDLEINFOS;
		}

		List bundleInfoList = readConfiguration(url, launcherLocation);
		return Utils.getBundleInfosFromList(bundleInfoList);
	}

	/**
	 * This method is copied from SimpleConfiguratorUtils class.
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static List readConfiguration(URL url, File base) throws IOException {
		List bundles = new ArrayList();
		try {
			// System.out.println("readConfiguration(URL url):url()=" + url);
			// URL configFileUrl = getConfigFileUrl();
			// URL configFileUrl = Utils.getUrl("file",null,
			// inputFile.getAbsolutePath());
			BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream()));
			// BufferedReader r = new BufferedReader(new FileReader(inputFile));

			String line;
			try {
				URL baseUrl = new URL(url, "./");
				while ((line = r.readLine()) != null) {
					if (line.startsWith("#"))
						continue;
					line = line.trim();// symbolicName,version,location,startlevel,expectedState
					if (line.length() == 0)
						continue;

					// (expectedState is an integer).
					//System.out.println("line=" + line);
					if (line.startsWith(SimpleConfiguratorConstants.PARAMETER_BASEURL + "=")) {
						String baseUrlSt = line.substring((SimpleConfiguratorConstants.PARAMETER_BASEURL + "=").length());
						if (!baseUrlSt.endsWith("/"))
							baseUrlSt += "/";
						baseUrl = new URL(url, baseUrlSt);
						continue;
					}
					StringTokenizer tok = new StringTokenizer(line, ",", true);
					String symbolicName = tok.nextToken();
					if (symbolicName.equals(","))
						symbolicName = null;
					else
						tok.nextToken(); // ,

					String version = tok.nextToken();
					if (version.equals(","))
						version = null;
					else
						tok.nextToken(); // ,

					String urlSt = tok.nextToken();
					if (urlSt.equals(",")) {
						if (symbolicName != null && version != null)
							urlSt = symbolicName + "_" + version + ".jar";
						else
							urlSt = null;
					} else
						tok.nextToken(); // ,
					try {
						new URL(urlSt);
						//						if (DEBUG)
						//							System.out.println("1 urlSt=" + urlSt);
					} catch (MalformedURLException e) {
						urlSt = Utils.getUrlInFull(urlSt, baseUrl).toExternalForm();
						//						if (DEBUG)
						//							System.out.println("2 urlSt=" + urlSt);
					}

					int sl = Integer.parseInt(tok.nextToken().trim());
					tok.nextToken(); // ,
					boolean markedAsStarted = Boolean.valueOf(tok.nextToken()).booleanValue();
					// URL urlBundle = null;
					// try {
					// urlBundle = new URL(urlSt);
					// } catch (MalformedURLException e) {
					// urlBundle = Utils.getFullUrl(urlSt, baseUrl);
					// }

					urlSt = makeAbsolute(urlSt, base != null ? base.toURL() : null);
					BundleInfo bInfo = new BundleInfo(symbolicName, version, urlSt, sl, markedAsStarted);
					bundles.add(bInfo);
					// System.out.println("tail line=" + line);
				}
			} finally {
				try {
					r.close();
				} catch (IOException ex) {
					// ignore
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			// TODO log something
			// bundleInfos = NULL_BUNDLEINFOS;
		}
		return bundles;
		// bundleInfos = (BundleInfo[]) bundles.toArray(new
		// BundleInfo[bundles.size()]);
	}

	public BundleInfo[] save(Manipulator manipulator, boolean backup) throws IOException {
		List setToInitialConfig = new LinkedList();
		List setToSimpleConfig = new LinkedList();
		ConfigData configData = manipulator.getConfigData();

		//try {
		if (!divideBundleInfos(manipulator, setToInitialConfig, setToSimpleConfig, configData.getInitialBundleStartLevel()))
			return configData.getBundles();
		//} catch (Exception e) {
		//	e.printStackTrace();
		//	System.exit(-1);
		//}
		//		if (DEBUG) {
		//			System.out.println("setToInitialConfig=\n" + SimpleConfiguratorUtils.getListSt(setToInitialConfig));
		//			System.out.println("setToSimpleConfig=\n" + SimpleConfiguratorUtils.getListSt(setToSimpleConfig));
		//		}
		URL configuratorConfigUrl = getConfigLocation(manipulator);
		if (!configuratorConfigUrl.getProtocol().equals("file"))
			new IllegalStateException("configuratorConfigUrl should start with \"file\".\nconfiguratorConfigUrl=" + configuratorConfigUrl);
		File outputFile = new File(configuratorConfigUrl.getFile());
		this.saveConfiguration(setToSimpleConfig, outputFile, getOSGiInstallArea(manipulator.getLauncherData()), backup);
		configData.setFwIndependentProp(SimpleConfiguratorConstants.PROP_KEY_CONFIGURL, outputFile.toURL().toExternalForm());
		return orderingInitialConfig(setToInitialConfig);
	}

	public static File getOSGiInstallArea(LauncherData launcherData) {
		if (launcherData == null)
			return null;
		String[] args = launcherData.getProgramArgs();
		if (args == null)
			return null;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-startup") && i + 1 < args.length && args[i + 1].charAt(1) != '-') {
				IPath parentFolder = new Path(args[i + 1]).removeLastSegments(1);
				if (parentFolder.lastSegment().equals("plugins"))
					return parentFolder.removeLastSegments(1).toFile();
				return parentFolder.toFile();
			}
		}
		return launcherData.getLauncher().getParentFile();
	}

	public static void saveConfiguration(List bundleInfoList, File outputFile, File base, boolean backup) throws IOException {
		if (DEBUG) {
			System.out.println("saveConfiguration(List bundleInfoList, File outputFile, boolean backup): outFile=" + outputFile.getAbsolutePath());
		}
		BufferedWriter bw;
		if (backup)
			if (outputFile.exists()) {
				File dest = Utils.getSimpleDataFormattedFile(outputFile);
				if (!outputFile.renameTo(dest)) {
					throw new IOException("Fail to rename from (" + outputFile + ") to (" + dest + ")");
				}
			}

		if (bundleInfoList.size() > 0) {
			Utils.createParentDir(outputFile);
			bw = new BufferedWriter(new FileWriter(outputFile));

			for (Iterator ite = bundleInfoList.iterator(); ite.hasNext();) {
				BundleInfo bInfo = (BundleInfo) ite.next();
				String location = bInfo.getLocation();

				if (bInfo.getSymbolicName() == null)
					bw.write(",");
				else
					bw.write(bInfo.getSymbolicName() + ",");
				if (bInfo.getVersion() == null)
					bw.write(",");
				else
					bw.write(bInfo.getVersion() + ",");

				location = makeRelative(location, base != null ? base.toURL() : null);
				bw.write(location + ",");
				bw.write(bInfo.getStartLevel() + "," + bInfo.isMarkedAsStarted());
				bw.newLine();
			}
			bw.flush();
			bw.close();
		} else {
			if (outputFile.exists()) {
				outputFile.delete();
			}
			File parentDir = outputFile.getParentFile();
			if (parentDir.exists()) {
				parentDir.delete();
			}
		}

	}

	private static String makeRelative(IPath toRel, IPath base) {
		int i = base.matchingFirstSegments(toRel);
		if (i == 0) {
			return toRel.toOSString();
		}
		String result = "";
		for (int j = 0; j < (base.segmentCount() - i); j++) {
			result += ".." + Path.SEPARATOR;
		}
		if (i == toRel.segmentCount())
			return ".";
		result += toRel.setDevice(null).removeFirstSegments(i).toOSString();
		return result;
	}

	public static String makeRelative(String urlString, URL rootURL) {
		// we only traffic in file: URLs
		int index = urlString.indexOf(FILE_PROTOCOL);
		if (index == -1)
			return urlString;
		index = index + 5;

		// ensure we have an absolute path to start with
		boolean done = false;
		URL url = null;
		String file = urlString;
		while (!done) {
			try {
				url = new URL(file);
				file = url.getFile();
			} catch (java.net.MalformedURLException e) {
				done = true;
			}
		}
		if (url == null || !new File(url.getFile()).isAbsolute())
			return urlString;

		String rootString = rootURL.toExternalForm();
		return urlString.substring(0, index) + makeRelative(new Path(urlString.substring(index)), new Path(rootString.substring(rootString.indexOf(FILE_PROTOCOL) + 5)));
	}

	public static String makeAbsolute(String original, String rootPath) {
		IPath path = new Path(original);
		// ensure we have a relative path to start with
		if (path.isAbsolute())
			return original;
		IPath root = new Path(rootPath);
		return root.addTrailingSeparator().append(original.replace(':', '}')).toOSString().replace('}', ':');
	}

	/*
	 * Make the given path absolute to the specified root, if applicable. If not, then
	 * return the path as-is.
	 */
	public static String makeAbsolute(String urlString, URL rootURL) {
		// we only traffic in file: URLs
		int index = urlString.indexOf(FILE_PROTOCOL);
		if (index == -1)
			return urlString;
		index = index + 5;

		// ensure we have a relative path to start with
		boolean done = false;
		URL url = null;
		String file = urlString;
		while (!done) {
			try {
				url = new URL(file);
				file = url.getFile();
			} catch (java.net.MalformedURLException e) {
				done = true;
			}
		}
		if (url == null || new File(url.getFile()).isAbsolute())
			return urlString;

		return urlString.substring(0, index - 5) + makeAbsolute(urlString.substring(index), rootURL.toExternalForm());
	}

	/*
	 * Look at the given strings and return the index of the first character which isn't the same.
	 * 
	 * Method similar to one from SimpleConfigurationManipulatorImpl.
	 */
	private static int commonPrefixEnd(String path, String root, int startIndex, int rootStart) {
		if (startIndex > path.length() || rootStart > root.length())
			return 0;
		int index = startIndex;
		int rootIndex = rootStart;
		while (index < path.length() + startIndex && rootIndex < root.length() && path.charAt(index) == root.charAt(rootIndex)) {
			index++;
			rootIndex++;
		}
		return rootIndex == root.length() ? index : 0;
	}

	void setPrerequisiteBundles(BundleInfo configuratorBundleInfo, BundlesState state, LocationInfo info) {
		if (state.isFullySupported())
			if (!this.checkResolve(configuratorBundleInfo, state)) {
				printoutUnsatisfiedConstraints(configuratorBundleInfo, state);
				return;
			}
		BundleInfo[] prerequisites = state.getPrerequisteBundles(configuratorBundleInfo);
		info.prerequisiteLocations = new String[prerequisites.length];
		for (int i = 0; i < prerequisites.length; i++)
			info.prerequisiteLocations[i] = prerequisites[i].getLocation();
		return;

	}

	void setSystemBundles(BundlesState state, LocationInfo info) {
		BundleInfo systemBundleInfo = state.getSystemBundle();
		if (systemBundleInfo == null) {
			// TODO Log
			//throw new IllegalStateException("There is no systemBundle.\n");
			return;
		}
		if (state.isFullySupported())
			if (!this.checkResolve(systemBundleInfo, state)) {
				printoutUnsatisfiedConstraints(systemBundleInfo, state);
				return;
			}
		info.systemBundleLocation = systemBundleInfo.getLocation();
		BundleInfo[] fragments = state.getSystemFragmentedBundles();
		info.systemFragmentedBundleLocations = new String[fragments.length];
		for (int i = 0; i < fragments.length; i++)
			info.systemFragmentedBundleLocations[i] = fragments[i].getLocation();
	}

	public void updateBundles(Manipulator manipulator) throws IOException {
		if (DEBUG)
			System.out.println("SimpleConfiguratorManipulatorImpl#updateBundles()");

		BundlesState bundleState = manipulator.getBundlesState();

		if (bundleState == null)
			return;
		if (bundleState.isFullySupported())
			bundleState.resolve(true);

		BundleInfo[] currentBInfos = bundleState.getExpectedState();
		if (!isTargetConfiguratorBundle(currentBInfos))
			return;
		Properties properties = new Properties();
		String[] jvmArgs = manipulator.getLauncherData().getJvmArgs();
		for (int i = 0; i < jvmArgs.length; i++)
			if (jvmArgs[i].startsWith("-D")) {
				int index = jvmArgs[i].indexOf("=");
				String key = jvmArgs[i].substring("-D".length(), index);
				String value = jvmArgs[i].substring(index + 1);
				properties.setProperty(key, value);
			}

		Utils.appendProperties(properties, manipulator.getConfigData().getFwIndependentProps());
		boolean exclusiveInstallation = Boolean.valueOf(properties.getProperty(SimpleConfiguratorConstants.PROP_KEY_EXCLUSIVE_INSTALLATION)).booleanValue();
		URL configuratorConfigUrl = getConfigLocation(manipulator);

		BundleInfo[] toInstall = this.loadConfiguration(configuratorConfigUrl, getOSGiInstallArea(manipulator.getLauncherData()));

		List toUninstall = new LinkedList();
		if (exclusiveInstallation)
			for (int i = 0; i < currentBInfos.length; i++) {
				boolean install = false;
				for (int j = 0; j < toInstall.length; j++)
					if (currentBInfos[i].getLocation().equals(toInstall[j].getLocation())) {
						install = true;
						break;
					}
				if (!install)
					toUninstall.add(currentBInfos[i]);
			}

		for (int i = 0; i < toInstall.length; i++) {
			try {
				bundleState.installBundle(toInstall[i]);
			} catch (RuntimeException e) {
				//Ignore
			}
		}
		if (exclusiveInstallation)
			for (Iterator ite = toUninstall.iterator(); ite.hasNext();) {
				BundleInfo bInfo = (BundleInfo) ite.next();
				bundleState.uninstallBundle(bInfo);
			}

		bundleState.resolve(true);
		manipulator.getConfigData().setBundles(bundleState.getExpectedState());
	}

}