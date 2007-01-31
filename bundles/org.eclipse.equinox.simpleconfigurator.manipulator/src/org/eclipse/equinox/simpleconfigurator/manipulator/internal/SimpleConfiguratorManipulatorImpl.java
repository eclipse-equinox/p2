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
package org.eclipse.equinox.simpleconfigurator.manipulator.internal;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.eclipse.equinox.configurator.ConfiguratorManipulator;
import org.eclipse.equinox.frameworkadmin.*;
import org.eclipse.equinox.internal.frameworkadmin.utils.Utils;
import org.eclipse.equinox.internal.simpleconfigurator.utils.SimpleConfiguratorConstants;
import org.eclipse.equinox.internal.simpleconfigurator.utils.SimpleConfiguratorUtils;
import org.osgi.framework.Constants;

public class SimpleConfiguratorManipulatorImpl implements ConfiguratorManipulator {
	class LocationInfo {
		String[] prerequisiteLocations = null;
		String systemBundleLocation = null;
		String[] systemFragmentedBundleLocations = null;
	}

	final static boolean DEBUG = false;

	static String CONFIG_LOCATION = SimpleConfiguratorConstants.CONFIG_LIST;

	private static final BundleInfo[] NULL_BUNDLEINFOS = new BundleInfo[0];

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
			if (fwConfigLoc.isDirectory())
				baseDir = fwConfigLoc;
			else
				baseDir = fwConfigLoc.getParentFile();
		}
		try {
			return (new File(baseDir, "SimpleConfigurator.txt").toURL());
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
		boolean found = false;
		for (int i = 0; i < bInfos.length; i++) {
			if (isTargetConfiguratorBundle(bInfos[i].getLocation())) {
				if (!bInfos[i].isResolved())
					return false;
				//TODO confirm that startlevel of configurator bundle must be no larger than beginning start level of fw. However, there is no way to know the start level of cached ones.
				found = true;
				break;
			}
		}
		return found;
	}

	private static boolean isTargetConfiguratorBundle(String location) {
		final String symbolic = Utils.getManifestMainAttributes(location, Constants.BUNDLE_SYMBOLICNAME);
		return (SimpleConfiguratorUtils.SERVICE_PROP_VALUE_CONFIGURATOR_SYMBOLICNAME.equals(symbolic));
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

	private boolean devideBundleInfos(Manipulator manipulator, List setToInitialConfig, List setToSimpleConfig, int initialBSL) throws IOException {
		BundlesState state = manipulator.getBundlesState();
		BundleInfo[] targetBundleInfos = null;
		if (state.isFullySupported())
			targetBundleInfos = state.getExpectedState();
		else
			targetBundleInfos = manipulator.getConfigData().getBundles();
		BundleInfo configuratorBInfo = null;
		for (int i = 0; i < targetBundleInfos.length; i++) {
			if (isTargetConfiguratorBundle(targetBundleInfos[i].getLocation())) {
				if (targetBundleInfos[i].isMarkedAsStarted()) {
					configuratorBInfo = targetBundleInfos[i];
					break;
				}
			}
		}
		if (configuratorBInfo == null)
			return false;

		if (state.isFullySupported())
			state.resolve(false);

		LocationInfo info = new LocationInfo();
		setSystemBundles(state, info);
		setPrerequisiteBundles(configuratorBInfo, state, info);

		SortedMap bslToList = getSortedMap(initialBSL, targetBundleInfos);

		algorithm(initialBSL, bslToList, configuratorBInfo, setToInitialConfig, setToSimpleConfig, info);
		return true;
	}

	private SortedMap getSortedMap(int initialSl, BundleInfo[] bInfos) {
		SortedMap bslToList = new TreeMap();
		for (int i = 0; i < bInfos.length; i++) {
			Integer sL = Integer.valueOf(bInfos[i].getStartLevel());
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

	private BundleInfo[] readConfiguration(URL url) throws IOException {
		if (url == null)
			return NULL_BUNDLEINFOS;

		List bundleInfoList = SimpleConfiguratorUtils.readConfiguration(url);
		return Utils.getBundleInfosFromList(bundleInfoList);
	}

	public BundleInfo[] save(Manipulator manipulator, boolean backup) throws IOException {
		List setToInitialConfig = new LinkedList();
		List setToSimpleConfig = new LinkedList();
		ConfigData configData = manipulator.getConfigData();

		if (!devideBundleInfos(manipulator, setToInitialConfig, setToSimpleConfig, configData.getInitialBundleStartLevel()))
			return NULL_BUNDLEINFOS;

		if (DEBUG) {
			System.out.println("setToInitialConfig=\n" + SimpleConfiguratorUtils.getListSt(setToInitialConfig));
			System.out.println("setToSimpleConfig=\n" + SimpleConfiguratorUtils.getListSt(setToSimpleConfig));
		}
		URL configuratorConfigUrl = getConfigLocation(manipulator);
		if (!configuratorConfigUrl.getProtocol().equals("file"))
			new IllegalStateException("configuratorConfigUrl should start with \"file\".\nconfiguratorConfigUrl=" + configuratorConfigUrl);
		File outputFile = new File(configuratorConfigUrl.getFile());
		this.saveConfiguration(setToSimpleConfig, outputFile, backup);
		configData.setFwIndependentProp(SimpleConfiguratorConstants.PROP_KEY_CONFIGURL, outputFile.toURL().toExternalForm());
		return orderingInitialConfig(setToInitialConfig);
	}

	private void saveConfiguration(List bundleInfoList, File outputFile, boolean backup) throws IOException {
		if (DEBUG)
			System.out.println("saveConfiguration(List bundleInfoList, File outputFile, boolean backup): outFile=" + outputFile.getAbsolutePath());
		BufferedWriter bw;
		if (backup)
			if (outputFile.exists()) {
				File dest = Utils.getSimpleDataFormattedFile(outputFile);
				if (!outputFile.renameTo(dest))
					throw new IOException("Fail to rename from (" + outputFile + ") to (" + dest + ")");
			}

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

			bw.write(location + ",");
			bw.write(bInfo.getStartLevel() + "," + bInfo.isMarkedAsStarted());
			bw.newLine();
		}
		bw.flush();
		bw.close();

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
		boolean exclusiveInstallation = Boolean.parseBoolean(properties.getProperty(SimpleConfiguratorConstants.PROP_KEY_EXCLUSIVE_INSTALLATION));
		URL configuratorConfigUrl = getConfigLocation(manipulator);

		BundleInfo[] toInstall = this.readConfiguration(configuratorConfigUrl);

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

		for (int i = 0; i < toInstall.length; i++)
			bundleState.installBundle(toInstall[i]);

		if (exclusiveInstallation)
			for (Iterator ite = toUninstall.iterator(); ite.hasNext();) {
				BundleInfo bInfo = (BundleInfo) ite.next();
				bundleState.uninstallBundle(bInfo);
			}

		bundleState.resolve(true);
		manipulator.getConfigData().setBundles(bundleState.getExpectedState());
	}

}