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
package org.eclipse.equinox.simpleconfigurator.internal;

import java.net.URL;
import java.util.*;

import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.frameworkadmin.utils.Utils;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

class ConfigApplier {
	private BundleContext manipulatingContext;
	URL configLocationURL = null;
	private PackageAdmin adminService = null;
	private StartLevel startLevelService = null;

	ConfigApplier(BundleContext context, SimpleConfiguratorImpl configurator) {
		this.manipulatingContext = context;
		ServiceReference packageAdminRef = manipulatingContext.getServiceReference(PackageAdmin.class.getName());
		if (packageAdminRef == null)
			throw new IllegalStateException("No PackageAdmin service is available.");

		adminService = (PackageAdmin) manipulatingContext.getService(packageAdminRef);

		ServiceReference startLevelRef = manipulatingContext.getServiceReference(StartLevel.class.getName());
		if (startLevelRef == null)
			throw new IllegalStateException("No StartLevelService service is available.");
		startLevelService = (StartLevel) manipulatingContext.getService(startLevelRef);

	}

	void install(BundleInfo[] expectedState, boolean uninstall) {
		Collection toRefresh = new ArrayList();
		Collection toStart = new ArrayList();
		toRefresh.addAll(installBundles(expectedState, toStart));
		if (uninstall)
			toRefresh.addAll(uninstallBundles(expectedState, adminService));
		refreshPackages((Bundle[]) toRefresh.toArray(new Bundle[toRefresh.size()]), manipulatingContext);
		startBundles((Bundle[]) toStart.toArray(new Bundle[toStart.size()]));
		//if time stamps are the same
		//  do nothing
		//  return
		//if list exists
		//  force the list in the fwk
		//else
		//  discover bundles in folders and force the list in the fwk
	}

	private ArrayList installBundles(BundleInfo[] finalList, Collection toStart) {
		ArrayList installed = new ArrayList();
		//printSystemBundle();

		for (int i = 0; i < finalList.length; i++) {
			//TODO here we do not deal with bundles that don't have a symbolic id
			//TODO Need to handle the case where getBundles return multiple value

			Bundle[] matches = null;
			Dictionary manifest = Utils.getOSGiManifest(finalList[i].getLocation());
			String symbolicName = (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME);
			if (symbolicName != null && symbolicName.indexOf(";") != -1)
				symbolicName = symbolicName.substring(0, symbolicName.indexOf(";")).trim();

			String version = (String) manifest.get(Constants.BUNDLE_VERSION);
			if (symbolicName != null && version != null)
				matches = adminService.getBundles(symbolicName, version);

			Bundle current = matches == null ? null : (matches.length == 0 ? null : matches[0]);
			if (current == null) {
				try {
					if (finalList[i].getLocation() == null)
						continue;
					//TODO Need to eliminate System Bundle.
					// If a system bundle doesn't have a SymbolicName header, like Knopflerfish 4.0.0,
					// it will be installed unfortunately. 
					current = manipulatingContext.installBundle(finalList[i].getLocation());
					if (Activator.DEBUG)
						System.out.println("installed bundle:" + finalList[i]);
					installed.add(current);
				} catch (BundleException e) {
					System.err.println("Can't install " + symbolicName + "/" + version + " from location " + finalList[i].getLocation());
					continue;
				}
			}
			int startLevel = finalList[i].getStartLevel();
			if (startLevel != BundleInfo.NO_LEVEL)
				if (current.getBundleId() != 0) {
					String name = current.getSymbolicName();
					try {
						if (startLevel > 0)
							if (!"org.eclipse.core.simpleConfigurator".equals(name))
								startLevelService.setBundleStartLevel(current, startLevel);
					} catch (IllegalArgumentException ex) {
						//TODO Log
						System.err.println("fail to set start level of Bundle:" + finalList[i]);
						ex.printStackTrace();
					}
				}
			if (finalList[i].isMarkedAsStarted()) {
				toStart.add(current);
			}
		}
		return installed;
	}

	private void printSystemBundle() {
		Bundle bundle = manipulatingContext.getBundle(0);
		System.out.println("bundle.getSymbolicName()=" + bundle.getSymbolicName());
		Dictionary headers = bundle.getHeaders();
		System.out.println(headers.size() + ":Headers=");
		for (Enumeration enumeration = headers.keys(); enumeration.hasMoreElements();) {
			Object key = enumeration.nextElement();
			System.out.println(" (" + key + "," + headers.get(key) + ")");
		}
	}

	private void refreshPackages(Bundle[] bundles, BundleContext manipulatingContext) {
		if (bundles.length == 0)
			return;
		ServiceReference packageAdminRef = manipulatingContext.getServiceReference(PackageAdmin.class.getName());
		PackageAdmin packageAdmin = null;
		if (packageAdminRef != null) {
			packageAdmin = (PackageAdmin) manipulatingContext.getService(packageAdminRef);
			if (packageAdmin == null)
				return;
		}
		final boolean[] flag = new boolean[] {false};
		FrameworkListener listener = new FrameworkListener() {
			public void frameworkEvent(FrameworkEvent event) {
				if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
					synchronized (flag) {
						flag[0] = true;
						flag.notifyAll();
					}
				}
			}
		};
		manipulatingContext.addFrameworkListener(listener);
		packageAdmin.refreshPackages(bundles);
		synchronized (flag) {
			while (!flag[0]) {
				try {
					flag.wait();
				} catch (InterruptedException e) {
				}
			}
		}
		//		if (DEBUG) {
		//			for (int i = 0; i < bundles.length; i++) {
		//				System.out.println(SimpleConfiguratorUtils.getBundleStateString(bundles[i]));
		//			}
		//		}
		manipulatingContext.removeFrameworkListener(listener);
		manipulatingContext.ungetService(packageAdminRef);
	}

	private void startBundles(Bundle[] bundles) {
		for (int i = 0; i < bundles.length; i++) {
			Bundle bundle = bundles[i];
			if (bundle.getState() == Bundle.STARTING)
				continue;

			try {
				bundle.start();
				if (Activator.DEBUG)
					System.out.println("started Bundle:" + bundle.getSymbolicName() + "(" + bundle.getLocation() + ":" + bundle.getBundleId() + ")");
			} catch (BundleException e) {
				e.printStackTrace();
				//				FrameworkLogEntry entry = new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, NLS.bind(EclipseAdaptorMsg.ECLIPSE_STARTUP_FAILED_START, bundle.getLocation()), 0, e, null);
				//				log.log(entry);
			}
		}
	}

	/**
	 * Uninstall bundles which are not listed on finalList.  
	 * 
	 * @param finalList bundles list not to be uninstalled.
	 * @param adminService package admin service.
	 * @return Collection HashSet of bundles finally installed.
	 */
	private Collection uninstallBundles(BundleInfo[] finalList, PackageAdmin adminService) {
		Bundle[] allBundles = manipulatingContext.getBundles();
		//	String symbolicNameSystem = manipulatingContext.getBundle(0).getSymbolicName();

		//Build a set with all the bundles from the system
		Set installedBundles = new HashSet(allBundles.length);
		//		configurator.setPrerequisiteBundles(allBundles);
		for (int i = 0; i < allBundles.length; i++) {
			if (allBundles[i].getBundleId() == 0)
				continue;
			installedBundles.add(allBundles[i]);
		}

		//Remove all the bundles appearing in the final list from the set of installed bundles
		for (int i = 0; i < finalList.length; i++) {
			Bundle[] toAdd = adminService.getBundles(finalList[i].getSymbolicName(), finalList[i].getVersion());
			for (int j = 0; j < toAdd.length; j++) {
				installedBundles.remove(toAdd[j]);
			}
		}

		for (Iterator iter = installedBundles.iterator(); iter.hasNext();) {
			try {
				Bundle bundle = ((Bundle) iter.next());
				bundle.uninstall();
				if (Activator.DEBUG)
					System.out.println("uninstalled Bundle:" + bundle.getSymbolicName() + "(" + bundle.getLocation() + ":" + bundle.getBundleId() + ")");
			} catch (BundleException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return installedBundles;
	}

}
