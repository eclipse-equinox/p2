/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.simpleconfigurator;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.*;
import org.eclipse.equinox.internal.simpleconfigurator.utils.*;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

class ConfigApplier {
	private static final String LAST_BUNDLES_INFO = "last.bundles.info"; //$NON-NLS-1$
	private static final String PROP_DEVMODE = "osgi.dev"; //$NON-NLS-1$

	private final BundleContext manipulatingContext;
	private final PackageAdmin packageAdminService;
	private final StartLevel startLevelService;
	private final boolean runningOnEquinox;
	private final boolean inDevMode;

	private final Bundle callingBundle;
	private final URI baseLocation;

	ConfigApplier(BundleContext context, Bundle callingBundle) {
		manipulatingContext = context;
		this.callingBundle = callingBundle;
		runningOnEquinox = "Eclipse".equals(context.getProperty(Constants.FRAMEWORK_VENDOR)); //$NON-NLS-1$
		inDevMode = manipulatingContext.getProperty(PROP_DEVMODE) != null;
		baseLocation = runningOnEquinox ? EquinoxUtils.getInstallLocationURI(context) : null;

		ServiceReference packageAdminRef = manipulatingContext.getServiceReference(PackageAdmin.class.getName());
		if (packageAdminRef == null)
			throw new IllegalStateException("No PackageAdmin service is available."); //$NON-NLS-1$
		packageAdminService = (PackageAdmin) manipulatingContext.getService(packageAdminRef);

		ServiceReference startLevelRef = manipulatingContext.getServiceReference(StartLevel.class.getName());
		if (startLevelRef == null)
			throw new IllegalStateException("No StartLevelService service is available."); //$NON-NLS-1$
		startLevelService = (StartLevel) manipulatingContext.getService(startLevelRef);
	}

	void install(URL url, boolean exclusiveMode) throws IOException {
		List bundleInfoList = SimpleConfiguratorUtils.readConfiguration(url, baseLocation);
		if (Activator.DEBUG)
			System.out.println("applyConfiguration() bundleInfoList.size()=" + bundleInfoList.size());
		if (bundleInfoList.size() == 0)
			return;

		BundleInfo[] expectedState = Utils.getBundleInfosFromList(bundleInfoList);

		// check for an update to the system bundle
		String systemBundleSymbolicName = manipulatingContext.getBundle(0).getSymbolicName();
		Version systemBundleVersion = manipulatingContext.getBundle(0).getVersion();
		if (systemBundleSymbolicName != null) {
			for (int i = 0; i < expectedState.length; i++) {
				String symbolicName = expectedState[i].getSymbolicName();
				if (!systemBundleSymbolicName.equals(symbolicName))
					continue;

				Version version = Version.parseVersion(expectedState[i].getVersion());
				if (!systemBundleVersion.equals(version))
					throw new IllegalStateException("The System Bundle was updated. The framework must be restarted to finalize the configuration change");
			}
		}

		HashSet toUninstall = null;
		if (!exclusiveMode) {
			BundleInfo[] lastInstalledBundles = getLastState();
			if (lastInstalledBundles != null) {
				toUninstall = new HashSet(Arrays.asList(lastInstalledBundles));
				toUninstall.removeAll(Arrays.asList(expectedState));
			}
			saveStateAsLast(url);
		}

		Collection prevouslyResolved = getResolvedBundles();
		Collection toRefresh = new ArrayList();
		Collection toStart = new ArrayList();
		if (exclusiveMode) {
			toRefresh.addAll(installBundles(expectedState, toStart));
			toRefresh.addAll(uninstallBundles(expectedState, packageAdminService));
		} else {
			toRefresh.addAll(installBundles(expectedState, toStart));
			if (toUninstall != null)
				toRefresh.addAll(uninstallBundles(toUninstall));
		}
		refreshPackages((Bundle[]) toRefresh.toArray(new Bundle[toRefresh.size()]), manipulatingContext);
		if (toRefresh.size() > 0)
			try {
				manipulatingContext.getBundle().loadClass("org.eclipse.osgi.service.resolver.PlatformAdmin"); //$NON-NLS-1$
				// now see if there are any currently resolved bundles with option imports which could be resolved or
				// if there are fragments with additional constraints which conflict with an already resolved host
				Bundle[] additionalRefresh = StateResolverUtils.getAdditionalRefresh(prevouslyResolved, manipulatingContext);
				if (additionalRefresh.length > 0)
					refreshPackages(additionalRefresh, manipulatingContext);
			} catch (ClassNotFoundException cnfe) {
				// do nothing; no resolver package available
			}
		startBundles((Bundle[]) toStart.toArray(new Bundle[toStart.size()]));
	}

	private Collection getResolvedBundles() {
		Collection resolved = new HashSet();
		Bundle[] allBundles = manipulatingContext.getBundles();
		for (int i = 0; i < allBundles.length; i++)
			if ((allBundles[i].getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0)
				resolved.add(allBundles[i]);
		return resolved;
	}

	private Collection uninstallBundles(HashSet toUninstall) {
		Collection removedBundles = new ArrayList(toUninstall.size());
		for (Iterator iterator = toUninstall.iterator(); iterator.hasNext();) {
			BundleInfo current = (BundleInfo) iterator.next();
			Bundle[] matchingBundles = packageAdminService.getBundles(current.getSymbolicName(), getVersionRange(current.getVersion()));
			for (int j = 0; matchingBundles != null && j < matchingBundles.length; j++) {
				try {
					removedBundles.add(matchingBundles[j]);
					matchingBundles[j].uninstall();
				} catch (BundleException e) {
					//TODO log in debug mode...
				}
			}
		}
		return removedBundles;
	}

	private void saveStateAsLast(URL url) {
		InputStream sourceStream = null;
		OutputStream destinationStream = null;

		File lastBundlesTxt = getLastBundleInfo();
		try {
			try {
				destinationStream = new FileOutputStream(lastBundlesTxt);
				sourceStream = url.openStream();
				SimpleConfiguratorUtils.transferStreams(sourceStream, destinationStream);
			} finally {
				if (destinationStream != null)
					destinationStream.close();
				if (sourceStream != null)
					sourceStream.close();
			}
		} catch (IOException e) {
			//nothing
		}
	}

	private File getLastBundleInfo() {
		return manipulatingContext.getDataFile(LAST_BUNDLES_INFO);
	}

	private BundleInfo[] getLastState() {
		File lastBundlesInfo = getLastBundleInfo();
		if (!lastBundlesInfo.isFile())
			return null;
		try {
			return (BundleInfo[]) SimpleConfiguratorUtils.readConfiguration(lastBundlesInfo.toURL(), baseLocation).toArray(new BundleInfo[1]);
		} catch (IOException e) {
			return null;
		}
	}

	private ArrayList installBundles(BundleInfo[] finalList, Collection toStart) {
		ArrayList toRefresh = new ArrayList();

		String useReferenceProperty = manipulatingContext.getProperty(SimpleConfiguratorConstants.PROP_KEY_USE_REFERENCE);
		boolean useReference = useReferenceProperty == null ? runningOnEquinox : Boolean.valueOf(useReferenceProperty).booleanValue();

		for (int i = 0; i < finalList.length; i++) {
			if (finalList[i] == null)
				continue;
			//TODO here we do not deal with bundles that don't have a symbolic id
			//TODO Need to handle the case where getBundles return multiple value

			String symbolicName = finalList[i].getSymbolicName();
			String version = finalList[i].getVersion();

			Bundle[] matches = null;
			if (symbolicName != null && version != null)
				matches = packageAdminService.getBundles(symbolicName, getVersionRange(version));

			String bundleLocation = SimpleConfiguratorUtils.getBundleLocation(finalList[i], useReference);

			Bundle current = matches == null ? null : (matches.length == 0 ? null : matches[0]);
			if (current == null) {
				try {
					current = manipulatingContext.installBundle(bundleLocation);
					if (symbolicName != null && version != null) {
						Version v;
						try {
							v = new Version(version);
							if (!symbolicName.equals(current.getSymbolicName()) || !v.equals(current.getVersion())) {
								// can happen if, for example, the new version of the bundle is installed
								// to the same bundle location as the old version
								current.update();
							}
						} catch (IllegalArgumentException e) {
							// invalid version string; should log
							if (Activator.DEBUG)
								e.printStackTrace();
						}
					}

					if (Activator.DEBUG)
						System.out.println("installed bundle:" + finalList[i]); //$NON-NLS-1$
					toRefresh.add(current);
				} catch (BundleException e) {
					if (Activator.DEBUG) {
						System.err.println("Can't install " + symbolicName + '/' + version + " from location " + finalList[i].getLocation()); //$NON-NLS-1$ //$NON-NLS-2$
						e.printStackTrace();
					}
					continue;
				}
			} else if (inDevMode && current.getBundleId() != 0 && current != manipulatingContext.getBundle() && !bundleLocation.equals(current.getLocation()) && !current.getLocation().startsWith("initial@")) {
				// We do not do this for the system bundle (id==0), the manipulating bundle or any bundle installed from the osgi.bundles list (locations starting with "@initial"
				// The bundle exists; but the location is different. Uninstall the current and install the new one (bug 229700)
				try {
					current.uninstall();
					toRefresh.add(current);
				} catch (BundleException e) {
					if (Activator.DEBUG) {
						System.err.println("Can't uninstall " + symbolicName + '/' + version + " from location " + current.getLocation()); //$NON-NLS-1$ //$NON-NLS-2$
						e.printStackTrace();
					}
					continue;
				}
				try {
					current = manipulatingContext.installBundle(bundleLocation);
					if (Activator.DEBUG)
						System.out.println("installed bundle:" + finalList[i]); //$NON-NLS-1$
					toRefresh.add(current);
				} catch (BundleException e) {
					if (Activator.DEBUG) {
						System.err.println("Can't install " + symbolicName + '/' + version + " from location " + finalList[i].getLocation()); //$NON-NLS-1$ //$NON-NLS-2$
						e.printStackTrace();
					}
					continue;
				}
			}

			// Mark Started
			if (finalList[i].isMarkedAsStarted()) {
				toStart.add(current);
			}

			// Set Start Level
			int startLevel = finalList[i].getStartLevel();
			if (startLevel < 1)
				continue;
			if (current.getBundleId() == 0)
				continue;
			if (packageAdminService.getBundleType(current) == PackageAdmin.BUNDLE_TYPE_FRAGMENT)
				continue;
			if (SimpleConfiguratorConstants.TARGET_CONFIGURATOR_NAME.equals(current.getSymbolicName()))
				continue;

			try {
				startLevelService.setBundleStartLevel(current, startLevel);
			} catch (IllegalArgumentException ex) {
				Utils.log(4, null, null, "Failed to set start level of Bundle:" + finalList[i], ex); //$NON-NLS-1$
			}
		}
		return toRefresh;
	}

	private void refreshPackages(Bundle[] bundles, BundleContext context) {
		if (bundles.length == 0 || packageAdminService == null)
			return;

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
		context.addFrameworkListener(listener);
		packageAdminService.refreshPackages(bundles);
		synchronized (flag) {
			while (!flag[0]) {
				try {
					flag.wait();
				} catch (InterruptedException e) {
					//ignore
				}
			}
		}
		//		if (DEBUG) {
		//			for (int i = 0; i < bundles.length; i++) {
		//				System.out.println(SimpleConfiguratorUtils.getBundleStateString(bundles[i]));
		//			}
		//		}
		context.removeFrameworkListener(listener);
	}

	private void startBundles(Bundle[] bundles) {
		for (int i = 0; i < bundles.length; i++) {
			Bundle bundle = bundles[i];
			if (bundle.getState() == Bundle.UNINSTALLED) {
				System.err.println("Could not start: " + bundle.getSymbolicName() + '(' + bundle.getLocation() + ':' + bundle.getBundleId() + ')' + ". It's state is uninstalled.");
				continue;
			}
			if (bundle.getState() == Bundle.STARTING && (bundle == callingBundle || bundle == manipulatingContext.getBundle()))
				continue;
			if (packageAdminService.getBundleType(bundle) == PackageAdmin.BUNDLE_TYPE_FRAGMENT)
				continue;
			if (bundle.getBundleId() == 0)
				continue;

			try {
				bundle.start();
				if (Activator.DEBUG)
					System.out.println("started Bundle:" + bundle.getSymbolicName() + '(' + bundle.getLocation() + ':' + bundle.getBundleId() + ')'); //$NON-NLS-1$
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
	 * @param packageAdmin package admin service.
	 * @return Collection HashSet of bundles finally installed.
	 */
	private Collection uninstallBundles(BundleInfo[] finalList, PackageAdmin packageAdmin) {
		Bundle[] allBundles = manipulatingContext.getBundles();

		//Build a set with all the bundles from the system
		Set removedBundles = new HashSet(allBundles.length);
		//		configurator.setPrerequisiteBundles(allBundles);
		for (int i = 0; i < allBundles.length; i++) {
			if (allBundles[i].getBundleId() == 0)
				continue;
			removedBundles.add(allBundles[i]);
		}

		//Remove all the bundles appearing in the final list from the set of installed bundles
		for (int i = 0; i < finalList.length; i++) {
			if (finalList[i] == null)
				continue;
			Bundle[] toAdd = packageAdmin.getBundles(finalList[i].getSymbolicName(), getVersionRange(finalList[i].getVersion()));
			for (int j = 0; toAdd != null && j < toAdd.length; j++) {
				removedBundles.remove(toAdd[j]);
			}
		}

		for (Iterator iter = removedBundles.iterator(); iter.hasNext();) {
			try {
				Bundle bundle = ((Bundle) iter.next());
				if (bundle.getLocation().startsWith("initial@")) {
					if (Activator.DEBUG)
						System.out.println("Simple configurator thinks a bundle installed by the boot strap should be uninstalled:" + bundle.getSymbolicName() + '(' + bundle.getLocation() + ':' + bundle.getBundleId() + ')'); //$NON-NLS-1$
					// Avoid uninstalling bundles that the boot strap code thinks should be installed (bug 232191)
					iter.remove();
					continue;
				}
				bundle.uninstall();
				if (Activator.DEBUG)
					System.out.println("uninstalled Bundle:" + bundle.getSymbolicName() + '(' + bundle.getLocation() + ':' + bundle.getBundleId() + ')'); //$NON-NLS-1$
			} catch (BundleException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return removedBundles;
	}

	private String getVersionRange(String version) {
		return version == null ? null : new StringBuffer().append('[').append(version).append(',').append(version).append(']').toString();
	}
}
