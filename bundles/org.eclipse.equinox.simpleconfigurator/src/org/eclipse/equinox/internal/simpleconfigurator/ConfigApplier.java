/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *     IBM Corporation - initial API and implementation
 *     Red Hat, Inc (Krzysztof Daniel) - Bug 421935: Extend simpleconfigurator to
 * read .info files from many locations, Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.internal.simpleconfigurator;

import java.io.*;
import java.net.*;
import java.util.*;
import org.eclipse.equinox.internal.simpleconfigurator.utils.*;
import org.osgi.framework.*;
import org.osgi.framework.namespace.*;
import org.osgi.framework.wiring.*;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

class ConfigApplier {
	private static final String LAST_BUNDLES_INFO = "last.bundles.info"; //$NON-NLS-1$
	private static final String PROP_DEVMODE = "osgi.dev"; //$NON-NLS-1$

	private final BundleContext manipulatingContext;
	private final PackageAdmin packageAdminService;
	private final StartLevel startLevelService;
	private final FrameworkWiring frameworkWiring;
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

		ServiceReference<PackageAdmin> packageAdminRef = manipulatingContext.getServiceReference(PackageAdmin.class);
		if (packageAdminRef == null)
			throw new IllegalStateException("No PackageAdmin service is available."); //$NON-NLS-1$
		packageAdminService = manipulatingContext.getService(packageAdminRef);

		ServiceReference<StartLevel> startLevelRef = manipulatingContext.getServiceReference(StartLevel.class);
		if (startLevelRef == null)
			throw new IllegalStateException("No StartLevelService service is available."); //$NON-NLS-1$
		startLevelService = manipulatingContext.getService(startLevelRef);

		frameworkWiring = (FrameworkWiring) manipulatingContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(FrameworkWiring.class);
	}

	void install(URL url, boolean exclusiveMode) throws IOException {
		List<BundleInfo> bundleInfoList = SimpleConfiguratorUtils.readConfiguration(url, baseLocation);
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

		HashSet<BundleInfo> toUninstall = null;
		if (!exclusiveMode) {
			BundleInfo[] lastInstalledBundles = getLastState();
			if (lastInstalledBundles != null) {
				toUninstall = new HashSet<BundleInfo>(Arrays.asList(lastInstalledBundles));
				toUninstall.removeAll(Arrays.asList(expectedState));
			}
			saveStateAsLast(url);
		}

		Set<Bundle> prevouslyResolved = getResolvedBundles();
		Collection<Bundle> toRefresh = new ArrayList<Bundle>();
		Collection<Bundle> toStart = new ArrayList<Bundle>();
		if (exclusiveMode) {
			toRefresh.addAll(installBundles(expectedState, toStart));
			toRefresh.addAll(uninstallBundles(expectedState, packageAdminService));
		} else {
			toRefresh.addAll(installBundles(expectedState, toStart));
			if (toUninstall != null)
				toRefresh.addAll(uninstallBundles(toUninstall));
		}
		refreshPackages((Bundle[]) toRefresh.toArray(new Bundle[toRefresh.size()]), manipulatingContext);
		if (toRefresh.size() > 0) {
			Bundle[] additionalRefresh = getAdditionalRefresh(prevouslyResolved, toRefresh);
			if (additionalRefresh.length > 0)
				refreshPackages(additionalRefresh, manipulatingContext);
		}
		startBundles((Bundle[]) toStart.toArray(new Bundle[toStart.size()]));
	}

	private Bundle[] getAdditionalRefresh(Set<Bundle> previouslyResolved, Collection<Bundle> toRefresh) {
		// This is the luna equinox framework or a non-equinox framework.
		// Use standard OSGi API.
		final Set<Bundle> additionalRefresh = new HashSet<Bundle>();
		final Set<Bundle> originalRefresh = new HashSet<Bundle>(toRefresh);
		for (Iterator<Bundle> iToRefresh = toRefresh.iterator(); iToRefresh.hasNext();) {
			Bundle bundle = (Bundle) iToRefresh.next();
			BundleRevision revision = (BundleRevision) bundle.adapt(BundleRevision.class);
			if (bundle.getState() == Bundle.INSTALLED && revision != null && (revision.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
				// this is an unresolved fragment; look to see if it has additional payload requirements
				boolean foundPayLoadReq = false;
				BundleRequirement hostReq = null;
				Collection<Requirement> requirements = revision.getRequirements(null);
				for (Iterator<Requirement> iReqs = requirements.iterator(); iReqs.hasNext();) {
					BundleRequirement req = (BundleRequirement) iReqs.next();
					if (HostNamespace.HOST_NAMESPACE.equals(req.getNamespace())) {
						hostReq = req;
					}
					if (!HostNamespace.HOST_NAMESPACE.equals(req.getNamespace()) && !ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE.equals(req.getNamespace())) {
						// found a payload requirement
						foundPayLoadReq = true;
					}
				}
				if (foundPayLoadReq) {
					Collection<BundleCapability> candidates = frameworkWiring.findProviders(hostReq);
					for (Iterator<BundleCapability> iCandidates = candidates.iterator(); iCandidates.hasNext();) {
						BundleCapability candidate = iCandidates.next();
						if (!originalRefresh.contains(candidate.getRevision().getBundle())) {
							additionalRefresh.add(candidate.getRevision().getBundle());
						}
					}
				}
			}
		}

		for (Iterator<Bundle> iPreviouslyResolved = previouslyResolved.iterator(); iPreviouslyResolved.hasNext();) {
			Bundle bundle = iPreviouslyResolved.next();
			BundleRevision revision = (BundleRevision) bundle.adapt(BundleRevision.class);
			BundleWiring wiring = revision == null ? null : revision.getWiring();
			if (wiring != null) {
				Collection<BundleRequirement> reqs = revision.getDeclaredRequirements(null);
				Set<BundleRequirement> optionalReqs = new HashSet<BundleRequirement>();
				for (Iterator<BundleRequirement> iReqs = reqs.iterator(); iReqs.hasNext();) {
					BundleRequirement req = (BundleRequirement) iReqs.next();
					String namespace = req.getNamespace();
					// only do this for package and bundle namespaces
					if (PackageNamespace.PACKAGE_NAMESPACE.equals(namespace) || BundleNamespace.BUNDLE_NAMESPACE.equals(namespace)) {
						if (Namespace.RESOLUTION_OPTIONAL.equals(req.getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE))) {
							optionalReqs.add(req);
						}
					}
				}
				if (!optionalReqs.isEmpty()) {
					wiring = getHostWiring(wiring);
					// check that all optional requirements are wired
					Collection<BundleWire> requiredWires = wiring.getRequiredWires(null);
					for (Iterator<BundleWire> iRequiredWires = requiredWires.iterator(); iRequiredWires.hasNext();) {
						BundleWire requiredWire = iRequiredWires.next();
						optionalReqs.remove(requiredWire.getRequirement());
					}
					if (!optionalReqs.isEmpty()) {
						// there are a number of optional requirements not wired
						for (Iterator<BundleRequirement> iOptionalReqs = optionalReqs.iterator(); iOptionalReqs.hasNext();) {
							Collection<BundleCapability> candidates = frameworkWiring.findProviders(iOptionalReqs.next());
							// Filter out candidates that were previously resolved or are currently not resolved.
							// There is no need to refresh the resource if the candidate was previously available.
							for (Iterator<BundleCapability> iCandidates = candidates.iterator(); iCandidates.hasNext();) {
								BundleCapability candidate = iCandidates.next();
								Bundle candidateBundle = candidate.getRevision().getBundle();
								// The candidate is not from the original refresh set, but
								// it could have just became resolved as a result of new bundles.
								if (previouslyResolved.contains(candidateBundle) || candidateBundle.getState() == Bundle.INSTALLED) {
									iCandidates.remove();
								}
							}
							if (!candidates.isEmpty()) {
								additionalRefresh.add(wiring.getBundle());
								break;
							}
						}
					}
				}
			}
		}
		return (Bundle[]) additionalRefresh.toArray(new Bundle[additionalRefresh.size()]);
	}

	private BundleWiring getHostWiring(BundleWiring wiring) {
		if ((wiring.getRevision().getTypes() & BundleRevision.TYPE_FRAGMENT) == 0) {
			// not a fragment
			return wiring;
		}
		Collection<BundleWire> hostWires = wiring.getRequiredWires(HostNamespace.HOST_NAMESPACE);
		// just use the first host wiring
		if (hostWires.isEmpty()) {
			return wiring;
		}
		BundleWire hostWire = (BundleWire) hostWires.iterator().next();
		return hostWire.getProviderWiring();
	}

	private Set<Bundle> getResolvedBundles() {
		Set<Bundle> resolved = new HashSet<Bundle>();
		Bundle[] allBundles = manipulatingContext.getBundles();
		for (int i = 0; i < allBundles.length; i++)
			if ((allBundles[i].getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0)
				resolved.add(allBundles[i]);
		return resolved;
	}

	private Collection<Bundle> uninstallBundles(HashSet<BundleInfo> toUninstall) {
		Collection<Bundle> removedBundles = new ArrayList<Bundle>(toUninstall.size());
		for (Iterator<BundleInfo> iterator = toUninstall.iterator(); iterator.hasNext();) {
			BundleInfo current = iterator.next();
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
				ArrayList<File> sourcesLocation = SimpleConfiguratorUtils.getInfoFiles();
				List<InputStream> sourceStreams = new ArrayList<InputStream>(sourcesLocation.size() + 1);
				sourceStreams.add(url.openStream());
				if (Activator.EXTENDED) {
					for (int i = 0; i < sourcesLocation.size(); i++) {
						sourceStreams.add(new FileInputStream(sourcesLocation.get(i)));
					}
				}
				SimpleConfiguratorUtils.transferStreams(sourceStreams, destinationStream);
			} catch (URISyntaxException e) {
				// nothing, was discovered when starting framework
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

	private ArrayList<Bundle> installBundles(BundleInfo[] finalList, Collection<Bundle> toStart) {
		ArrayList<Bundle> toRefresh = new ArrayList<Bundle>();

		String useReferenceProperty = manipulatingContext.getProperty(SimpleConfiguratorConstants.PROP_KEY_USE_REFERENCE);
		boolean useReference = useReferenceProperty == null ? runningOnEquinox : Boolean.parseBoolean(useReferenceProperty);

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

		// Prior to Luna the Equinox framework would refresh all bundles with the same
		// BSN automatically.  This is no longer the case for Luna or other framework
		// implementations.  Here we want to make sure all existing bundles with the
		// same BSN are refreshed also.
		Set<Bundle> allSameBSNs = new LinkedHashSet<Bundle>(); // maintain order and avoid duplicates
		for (Bundle bundle : bundles) {
			allSameBSNs.add(bundle);
			String bsn = bundle.getLocation();
			if (bsn != null) {
				// look for others with same BSN
				Bundle[] sameBSNs = packageAdminService.getBundles(bsn, null);
				if (sameBSNs != null) {
					// likely contains the bundle we just added above but a set is used
					allSameBSNs.addAll(Arrays.asList(sameBSNs));
				}
			}
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
		context.addFrameworkListener(listener);
		packageAdminService.refreshPackages(allSameBSNs.toArray(new Bundle[0]));
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
	private Collection<Bundle> uninstallBundles(BundleInfo[] finalList, PackageAdmin packageAdmin) {
		Bundle[] allBundles = manipulatingContext.getBundles();

		//Build a set with all the bundles from the system
		Set<Bundle> removedBundles = new HashSet<Bundle>(allBundles.length);
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

		for (Iterator<Bundle> iter = removedBundles.iterator(); iter.hasNext();) {
			try {
				Bundle bundle = iter.next();
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
