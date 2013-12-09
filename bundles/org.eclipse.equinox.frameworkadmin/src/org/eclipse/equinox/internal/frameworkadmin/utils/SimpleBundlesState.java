/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.frameworkadmin.utils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.osgi.framework.Constants;

/**
 * This implementation of BundlesState doesn't support any of
 * - resolving bundles,
 * - retrieving fw persistent data. 
 * 
 * This implementation can be used for those cases.
 *
 */

public class SimpleBundlesState implements BundlesState {
	public static final BundleInfo[] NULL_BUNDLEINFOS = new BundleInfo[0];

	/**
	 * Check if the specified FrameworkAdmin is available.
	 * 
	 * @param fwAdmin
	 * @throws FrameworkAdminRuntimeException
	 */
	public static void checkAvailability(FrameworkAdmin fwAdmin) throws FrameworkAdminRuntimeException {
		if (!fwAdmin.isActive())
			throw new FrameworkAdminRuntimeException("FrameworkAdmin creates this object is no more available.", FrameworkAdminRuntimeException.FRAMEWORKADMIN_UNAVAILABLE); //$NON-NLS-1$
	}

	/**
	 *  
	 * @param launcherData
	 * @return File of fwJar to be used.
	 * @throws IOException 
	 */
	static File getFwJar(LauncherData launcherData) {
		if (launcherData.getFwJar() != null)
			return launcherData.getFwJar();
		return null;
	}

	private final String systemBundleSymbolicName;

	private final String systemBundleName;

	private final String systemBundleVendor;
	List<BundleInfo> bundleInfosList = new LinkedList<BundleInfo>();

	FrameworkAdmin fwAdmin = null;

	Manipulator manipulator = null;

	/**
	 * If the manifest of the target fw implementation has Constants.BUNDLE_SYMBOLICNAME header,
	 * this constructor should be used. 
	 * 
	 * @param ManipulatorAdmin
	 * @param Manipulator
	 * @param systemBundleSymbolicName
	 */
	public SimpleBundlesState(FrameworkAdmin ManipulatorAdmin, Manipulator Manipulator, String systemBundleSymbolicName) {
		super();
		this.fwAdmin = ManipulatorAdmin;
		//		 copy Manipulator object for avoiding modifying the parameters of the Manipulator.
		this.manipulator = ManipulatorAdmin.getManipulator();
		this.manipulator.setConfigData(Manipulator.getConfigData());
		this.manipulator.setLauncherData(Manipulator.getLauncherData());
		this.systemBundleSymbolicName = systemBundleSymbolicName;
		this.systemBundleName = null;
		this.systemBundleVendor = null;
		initialize();
	}

	/**
	 * If the manifest of the target fw implementation has not Constants.BUNDLE_SYMBOLICNAME header
	 * but , Constants.BUNDLE_NAME and BUNDLE_VERSION, 
	 * this constructor should be used. 
	 * 
	 * @param ManipulatorAdmin
	 * @param Manipulator
	 * @param systemBundleName
	 * @param systemBundleVender
	 */
	public SimpleBundlesState(FrameworkAdmin ManipulatorAdmin, Manipulator Manipulator, String systemBundleName, String systemBundleVender) {
		super();
		this.fwAdmin = ManipulatorAdmin;
		//		 copy Manipulator object for avoiding modifying the parameters of the Manipulator.
		this.manipulator = ManipulatorAdmin.getManipulator();
		this.manipulator.setConfigData(Manipulator.getConfigData());
		this.manipulator.setLauncherData(Manipulator.getLauncherData());
		this.systemBundleSymbolicName = null;
		this.systemBundleName = systemBundleName;
		this.systemBundleVendor = systemBundleVender;
		initialize();
	}

	public BundleInfo[] getExpectedState() throws FrameworkAdminRuntimeException {
		if (!fwAdmin.isActive())
			throw new FrameworkAdminRuntimeException("FrameworkAdmin creates this object is no more available.", FrameworkAdminRuntimeException.FRAMEWORKADMIN_UNAVAILABLE); //$NON-NLS-1$
		return Utils.getBundleInfosFromList(this.bundleInfosList);
	}

	/* 
	 * Just return required bundles.
	 * 
	 * @see org.eclipse.equinox.internal.provisional.frameworkadmin.BundlesState#getPrerequisteBundles(org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo)
	 */
	public BundleInfo[] getPrerequisteBundles(BundleInfo bInfo) {
		URI location = bInfo.getLocation();
		final String requiredBundles = Utils.getManifestMainAttributes(location, Constants.REQUIRE_BUNDLE);
		if (requiredBundles == null)
			return new BundleInfo[] {this.getSystemBundle()};

		String[] clauses = Utils.getClauses(requiredBundles);
		List<String> list = new LinkedList<String>();
		for (int i = 0; i < clauses.length; i++)
			list.add(Utils.getPathFromClause(clauses[i]));

		List<BundleInfo> ret = new LinkedList<BundleInfo>();
		ret.add(this.getSystemBundle());
		for (Iterator<BundleInfo> ite = this.bundleInfosList.iterator(); ite.hasNext();) {
			BundleInfo currentBInfo = ite.next();
			URI currentLocation = currentBInfo.getLocation();
			String currentSymbolicName = Utils.getManifestMainAttributes(currentLocation, Constants.BUNDLE_SYMBOLICNAME);
			if (currentSymbolicName == null)
				continue;
			currentSymbolicName = Utils.getPathFromClause(currentSymbolicName);
			for (Iterator<String> ite2 = list.iterator(); ite2.hasNext();) {
				String symbolicName = ite2.next();
				if (symbolicName.equals(currentSymbolicName)) {
					ret.add(currentBInfo);
					break;
				}
			}
		}
		return Utils.getBundleInfosFromList(ret);
	}

	public BundleInfo getSystemBundle() {
		if (this.systemBundleSymbolicName == null) {
			for (Iterator<BundleInfo> ite = this.bundleInfosList.iterator(); ite.hasNext();) {
				BundleInfo bInfo = ite.next();
				//			if (bInfo.getStartLevel() != 1)
				//				return null;;
				URI location = bInfo.getLocation();
				String bundleName = Utils.getManifestMainAttributes(location, Constants.BUNDLE_NAME);
				if (systemBundleName.equals(bundleName)) {
					String bundleVendor = Utils.getManifestMainAttributes(location, Constants.BUNDLE_VENDOR);
					if (systemBundleVendor.equals(bundleVendor))
						return bInfo;
				}
			}
			return null;
		}
		for (Iterator<BundleInfo> ite = this.bundleInfosList.iterator(); ite.hasNext();) {
			BundleInfo bInfo = ite.next();
			URI location = bInfo.getLocation();
			String symbolicName = Utils.getManifestMainAttributes(location, Constants.BUNDLE_SYMBOLICNAME);
			symbolicName = Utils.getPathFromClause(symbolicName);
			if (this.systemBundleSymbolicName.equals(symbolicName))
				return bInfo;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public BundleInfo[] getSystemFragmentedBundles() {
		BundleInfo systemBInfo = this.getSystemBundle();
		if (systemBInfo == null)
			return NULL_BUNDLEINFOS;

		@SuppressWarnings("rawtypes")
		List list = new LinkedList();
		for (Iterator<BundleInfo> ite = this.bundleInfosList.iterator(); ite.hasNext();) {
			BundleInfo bInfo = ite.next();
			URI location = bInfo.getLocation();
			String manifestVersion = Utils.getManifestMainAttributes(location, Constants.BUNDLE_MANIFESTVERSION);
			if (manifestVersion == null)
				continue;
			if (manifestVersion.equals("1") || manifestVersion.equals("1.0")) //$NON-NLS-1$//$NON-NLS-2$
				continue;

			String fragmentHost = Utils.getManifestMainAttributes(location, Constants.FRAGMENT_HOST);
			if (fragmentHost == null)
				continue;
			int index = fragmentHost.indexOf(";"); //$NON-NLS-1$
			if (index == -1)
				continue;
			String symbolicName = fragmentHost.substring(0, index).trim();
			String parameter = fragmentHost.substring(index + 1).trim();
			// TODO What to do ,in case of alias name of system bundle is not used ?
			if (symbolicName.equals(Constants.SYSTEM_BUNDLE_SYMBOLICNAME))
				if (parameter.equals(Constants.EXTENSION_DIRECTIVE + ":=" + Constants.EXTENSION_FRAMEWORK)) { //$NON-NLS-1$
					list.add(location);
					break;
				}
		}
		return Utils.getBundleInfosFromList(list);
	}

	public String[] getUnsatisfiedConstraints(BundleInfo bInfo) throws FrameworkAdminRuntimeException {
		throw new FrameworkAdminRuntimeException("getUnsatisfiedConstraints(BundleInfo bInfo) is not supported in this implementation", FrameworkAdminRuntimeException.UNSUPPORTED_OPERATION); //$NON-NLS-1$
	}

	private void initialize() {
		this.bundleInfosList.clear();
		LauncherData launcherData = manipulator.getLauncherData();
		ConfigData configData = manipulator.getConfigData();
		File fwJar = getFwJar(launcherData);

		if (fwJar == null)
			throw new IllegalStateException("launcherData.getLauncherConfigFile() == null && fwJar is not set."); //$NON-NLS-1$
		// No fw persistent data location is taken into consideration.

		BundleInfo[] bInfos = configData.getBundles();
		for (int j = 0; j < bInfos.length; j++)
			this.installBundle(bInfos[j]);

		if (getSystemBundle() == null) {
			BundleInfo sysBInfo = new BundleInfo(launcherData.getFwJar().toURI(), 0, true);
			sysBInfo.setBundleId(0);
			this.installBundle(sysBInfo);
		}
	}

	public void installBundle(BundleInfo bInfo) throws FrameworkAdminRuntimeException {

		URI newLocation = bInfo.getLocation();
		Dictionary<String, String> newManifest = Utils.getOSGiManifest(newLocation);
		if (newManifest == null) {
			// TODO log something here
			return;
		}
		String newSymbolicName = newManifest.get(Constants.BUNDLE_SYMBOLICNAME);
		String newVersion = newManifest.get(Constants.BUNDLE_VERSION);
		//System.out.println("> currentInstalledBundles.length=" + currentInstalledBundles.length);
		boolean found = false;
		for (Iterator<BundleInfo> ite = this.bundleInfosList.iterator(); ite.hasNext();) {
			BundleInfo currentBInfo = ite.next();
			URI location = currentBInfo.getLocation();
			if (newLocation.equals(location)) {
				found = true;
				break;
			}
			Dictionary<String, String> manifest = Utils.getOSGiManifest(location);
			String symbolicName = manifest.get(Constants.BUNDLE_SYMBOLICNAME);
			String version = manifest.get(Constants.BUNDLE_VERSION);
			if (newSymbolicName != null && newVersion != null)
				if (newSymbolicName.equals(symbolicName) && newVersion.equals(version)) {
					found = true;
					break;
				}
		}
		if (!found) {
			this.bundleInfosList.add(bInfo);
		}
	}

	//	public String toString() {
	//		if (state == null)
	//			return null;
	//		StringBuffer sb = new StringBuffer();
	//		BundleDescription[] bundleDescriptions = state.getBundles();
	//		for (int i = 0; i < bundleDescriptions.length; i++) {
	//			sb.append(bundleDescriptions[i].getBundleId() + ":");
	//			sb.append(bundleDescriptions[i].toString() + "(");
	//			sb.append(bundleDescriptions[i].isResolved() + ")");
	//			String[] ees = bundleDescriptions[i].getExecutionEnvironments();
	//			for (int j = 0; j < ees.length; j++)
	//				sb.append(ees[j] + " ");
	//			sb.append("\n");
	//		}
	//		sb.append("PlatformProperties:\n");
	//		Dictionary[] dics = state.getPlatformProperties();
	//		for (int i = 0; i < dics.length; i++) {
	//			for (Enumeration enum = dics[i].keys(); enum.hasMoreElements();) {
	//				String key = (String) enum.nextElement();
	//				String value = (String) dics[i].get(key);
	//				sb.append(" (" + key + "," + value + ")\n");
	//			}
	//		}
	//		sb.append("\n");
	//		return sb.toString();
	//	}

	public boolean isFullySupported() {
		return false;
	}

	public boolean isResolved() throws FrameworkAdminRuntimeException {
		throw new FrameworkAdminRuntimeException("isResolved() is not supported in this implementation", FrameworkAdminRuntimeException.UNSUPPORTED_OPERATION); //$NON-NLS-1$
	}

	public boolean isResolved(BundleInfo bInfo) throws FrameworkAdminRuntimeException {
		throw new FrameworkAdminRuntimeException("isResolved(BundleInfo bInfo) is not supported in this implementation", FrameworkAdminRuntimeException.UNSUPPORTED_OPERATION); //$NON-NLS-1$
	}

	public void resolve(boolean increment) throws FrameworkAdminRuntimeException {
		throw new FrameworkAdminRuntimeException("resolve(boolean increment) is not supported in this implementation", FrameworkAdminRuntimeException.UNSUPPORTED_OPERATION); //$NON-NLS-1$
	}

	public void uninstallBundle(BundleInfo bInfo) throws FrameworkAdminRuntimeException {
		URI targetLocation = bInfo.getLocation();
		int index = -1;
		for (Iterator<BundleInfo> ite = this.bundleInfosList.iterator(); ite.hasNext();) {
			index++;
			BundleInfo currentBInfo = ite.next();
			URI location = currentBInfo.getLocation();
			if (targetLocation.equals(location)) {
				break;
			}
		}
		if (index != -1)
			this.bundleInfosList.remove(index);
	}

}
