/*******************************************************************************
 * Copyright (c) 2007, 2020 IBM Corporation and others.
 * All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.equinox.internal.simpleconfigurator.utils.*;
import org.eclipse.osgi.report.resolution.ResolutionReport.Entry.Type;
import org.osgi.framework.*;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.namespace.*;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.*;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.resolver.ResolutionException;

class ConfigApplier {

	private static final String LAST_BUNDLES_INFO = "last.bundles.info"; //$NON-NLS-1$
	private static final String PROP_DEVMODE = "osgi.dev"; //$NON-NLS-1$

	private final BundleContext manipulatingContext;
	private final PackageAdmin packageAdminService;
	private final FrameworkWiring frameworkWiring;
	private final boolean runningOnEquinox;
	private final boolean inDevMode;

	private final Bundle callingBundle;
	private final URI baseLocation;
	private boolean deepRefresh;
	private int maxRefreshTry;

	ConfigApplier(BundleContext context, Bundle callingBundle) {
		deepRefresh = Boolean.parseBoolean(context.getProperty("equinox.simpleconfigurator.deeprefresh"));
		String maxRefreshValue = context.getProperty("equinox.simpleconfigurator.maxrefresh");
		if (maxRefreshValue != null) {
			maxRefreshTry = Integer.parseInt(maxRefreshValue);
		} else {
			maxRefreshTry = 10;
		}
		manipulatingContext = context;
		this.callingBundle = callingBundle;
		runningOnEquinox = "Eclipse".equals(context.getProperty(Constants.FRAMEWORK_VENDOR)); //$NON-NLS-1$
		inDevMode = manipulatingContext.getProperty(PROP_DEVMODE) != null;
		baseLocation = runningOnEquinox ? EquinoxUtils.getInstallLocationURI(context) : null;

		ServiceReference<PackageAdmin> packageAdminRef = manipulatingContext.getServiceReference(PackageAdmin.class);
		if (packageAdminRef == null)
			throw new IllegalStateException("No PackageAdmin service is available."); //$NON-NLS-1$
		packageAdminService = manipulatingContext.getService(packageAdminRef);

		frameworkWiring = manipulatingContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(FrameworkWiring.class);
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
			for (BundleInfo element : expectedState) {
				String symbolicName = element.getSymbolicName();
				if (!systemBundleSymbolicName.equals(symbolicName))
					continue;

				Version version = Version.parseVersion(element.getVersion());
				if (!systemBundleVersion.equals(version))
					throw new IllegalStateException("The System Bundle was updated. The framework must be restarted to finalize the configuration change");
			}
		}

		HashSet<BundleInfo> toUninstall = null;
		if (!exclusiveMode) {
			BundleInfo[] lastInstalledBundles = getLastState();
			if (lastInstalledBundles != null) {
				toUninstall = new HashSet<>(Arrays.asList(lastInstalledBundles));
				toUninstall.removeAll(Arrays.asList(expectedState));
			}
			saveStateAsLast(url);
		}

		Set<Bundle> prevouslyResolved = getResolvedBundles();
		Collection<Bundle> toRefresh = new LinkedHashSet<>();
		Collection<Bundle> toStart = new ArrayList<>();
		if (exclusiveMode) {
			toRefresh.addAll(installBundles(expectedState, toStart));
			toRefresh.addAll(uninstallBundles(expectedState, packageAdminService));
		} else {
			toRefresh.addAll(installBundles(expectedState, toStart));
			if (toUninstall != null)
				toRefresh.addAll(uninstallBundles(toUninstall));
		}
		if (!toRefresh.isEmpty()) {
			if (manipulatingContext.getBundle().getState() == Bundle.STARTING) {
				// This is the startup of simple configurator.
				// Do the full refresh of all bundles to force re-resolve
				refreshAllBundles();
			} else {
				// In this case the platform is up, we should try to do an incremental resolve
				// TODO consider removing this case because it can cause inconsistent results.
				Map<String, List<Bundle>> bundlesByBsn = Arrays.stream(manipulatingContext.getBundles())
						.filter(bundle -> bundle.getSymbolicName() != null)
						.collect(Collectors.groupingBy(Bundle::getSymbolicName));
				// Prior to Luna the Equinox framework would refresh all bundles with the same
				// BSN automatically. This is no longer the case for Luna or other framework
				// implementations. Here we want to make sure all existing bundles with the
				// same BSN are refreshed also.
				Set<Bundle> allSameBSNs = new LinkedHashSet<>(); // maintain order and avoid duplicates
				for (Bundle bundle : toRefresh) {
					allSameBSNs.addAll(bundlesByBsn.getOrDefault(bundle.getSymbolicName(), List.of(bundle)));
				}
				refreshPackages(allSameBSNs);
				refreshPackages(getAdditionalRefresh(prevouslyResolved, toRefresh));
			}
			if (deepRefresh) {
				// when refreshing large sets of bundles the resolver sometimes take a choice
				// where a requirement can't be bound even though it is there and resolvable
				// this code collects all those requirements and refresh the smaller set of
				// bundles that provide these until a max retry is reached, all bundles are
				// resolved, or all providers have been tried to refresh already
				Set<Bundle> bundlesTried = new HashSet<>();
				Collection<Bundle> provider;
				Set<Bundle> doNotRefresh = getDoNotRefresh();
				int maxtry = maxRefreshTry;
				do {
					provider = getUnresolvedRequirementsProvider(doNotRefresh);
					if (provider.isEmpty() || !bundlesTried.addAll(provider)) {
						// nothing more we can do...
						break;
					}
					if (Activator.DEBUG) {
						System.out.println("There are " + provider.size() + " bundles to refresh...");
						for (Bundle bundle : provider) {
							System.out.println("\t" + bundle.getSymbolicName() + " " + bundle.getVersion());
						}
					}
					CountDownLatch latch = new CountDownLatch(1);
					frameworkWiring.refreshBundles(provider, event -> {
						if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
							latch.countDown();
						}
					});
					try {
						latch.await();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				} while (maxtry-- > 0);
			}
		}
		startBundles(toStart.toArray(new Bundle[toStart.size()]));
	}

	/**
	 * This method performs a resolve on all unresolved bundles and captures
	 * <ol>
	 * <li>Missing capabilities</li>
	 * <li>Use constraint violations</li>
	 * </ol>
	 * 
	 * after that each unique requirement is looked up in the wiring if there is any
	 * provider for it, if that is the case, the requirements is not really missing
	 * or part of a conflicting chain
	 * 
	 * @param doNotRefresh
	 * 
	 * @return a collection of bundles that potentially can provide a missing
	 *         requirement
	 */
	private Collection<Bundle> getUnresolvedRequirementsProvider(Set<Bundle> doNotRefresh) {
		ResolutionReportListener reportListener = new ResolutionReportListener();
		ServiceRegistration<ResolverHookFactory> hookReg = manipulatingContext
				.registerService(ResolverHookFactory.class,
				reportListener, null);
		try {
			if (frameworkWiring.resolveBundles(null)) {
				return List.of();
			}
			if (Activator.DEBUG) {
				System.out.println("There are unresolved bundles...");
			}
		} finally {
			hookReg.unregister();
		}
		return reportListener.getReports().stream()
				.flatMap(rr -> rr.getEntries().values().stream().flatMap(Collection::stream)).flatMap(error -> {
					if (error.getType() == Type.MISSING_CAPABILITY) {
						Requirement requirement = (Requirement) error.getData();
						if (!"optional".equals(requirement.getDirectives().get("resolution"))) {
							return Stream.of(requirement);
						}
					}
					if (error.getType() == Type.USES_CONSTRAINT_VIOLATION) {
						ResolutionException ex = (ResolutionException) error.getData();
						return ex.getUnresolvedRequirements().stream();
					}
					return Stream.empty();
				}).distinct().flatMap(req -> frameworkWiring.findProviders(req).stream())
				.map(bc -> bc.getResource().getBundle())
				.distinct().filter(bundle -> !doNotRefresh.contains(bundle)).toList();
	}

	private Collection<Bundle> getAdditionalRefresh(Set<Bundle> previouslyResolved, Collection<Bundle> toRefresh) {
		// This is the luna equinox framework or a non-equinox framework.
		// Use standard OSGi API.
		final Set<Bundle> additionalRefresh = new HashSet<>();
		for (Bundle bundle : toRefresh) {
			BundleRevision revision = bundle.adapt(BundleRevision.class);
			if (bundle.getState() == Bundle.INSTALLED && revision != null && (revision.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
				// this is an unresolved fragment; look to see if it has additional payload requirements
				boolean foundPayLoadReq = false;
				BundleRequirement hostReq = null;
				Collection<Requirement> requirements = revision.getRequirements(null);
				for (Requirement requirement : requirements) {
					BundleRequirement req = (BundleRequirement) requirement;
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
					for (BundleCapability candidate : candidates) {
						if (!toRefresh.contains(candidate.getRevision().getBundle())) {
							additionalRefresh.add(candidate.getRevision().getBundle());
						}
					}
				}
			}
		}

		for (Bundle bundle : previouslyResolved) {
			BundleRevision revision = bundle.adapt(BundleRevision.class);
			BundleWiring wiring = revision == null ? null : revision.getWiring();
			if (wiring != null) {
				Collection<BundleRequirement> reqs = revision.getDeclaredRequirements(null);
				Set<BundleRequirement> optionalReqs = new HashSet<>();
				for (BundleRequirement req : reqs) {
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
					for (BundleWire requiredWire : requiredWires) {
						optionalReqs.remove(requiredWire.getRequirement());
					}
					if (!optionalReqs.isEmpty()) {
						// there are a number of optional requirements not wired
						for (BundleRequirement bundleRequirement : optionalReqs) {
							Collection<BundleCapability> candidates = frameworkWiring.findProviders(bundleRequirement);
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
		return additionalRefresh;
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
		BundleWire hostWire = hostWires.iterator().next();
		return hostWire.getProviderWiring();
	}

	private Set<Bundle> getResolvedBundles() {
		Set<Bundle> resolved = new HashSet<>();
		Bundle[] allBundles = manipulatingContext.getBundles();
		for (Bundle bundle : allBundles)
			if ((bundle.getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0)
				resolved.add(bundle);
		return resolved;
	}

	private void refreshAllBundles() {
		Set<Bundle> toRefresh = new HashSet<>(frameworkWiring.getRemovalPendingBundles());
		Set<Bundle> doNotRefresh = getDoNotRefresh();
		for (Bundle bundle : manipulatingContext.getBundles()) {
			if (!doNotRefresh.contains(bundle)) {
				toRefresh.add(bundle);
			}
		}
		refreshPackages(toRefresh);
	}

	private Set<Bundle> getDoNotRefresh() {
		Set<Bundle> doNotRefresh = new HashSet<>();
		Bundle thisBundle = manipulatingContext.getBundle();
		if (thisBundle != null) {
			doNotRefresh.add(thisBundle);
			addDoNotRefreshFragments(thisBundle, doNotRefresh);
		}
		Bundle systemBundle = manipulatingContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
		doNotRefresh.add(systemBundle);
		addDoNotRefreshFragments(systemBundle, doNotRefresh);
		return doNotRefresh;
	}

	private void addDoNotRefreshFragments(Bundle bundle, Set<Bundle> doNotRefresh) {
		BundleWiring systemWiring = bundle.adapt(BundleWiring.class);
		if (systemWiring != null) {
			for (BundleWire hostWire : systemWiring.getProvidedWires(HostNamespace.HOST_NAMESPACE)) {
				Bundle systemFragment = hostWire.getRequirer().getBundle();
				if (systemFragment.getState() != Bundle.UNINSTALLED
						&& systemFragment.adapt(BundleWiring.class) != null) {
					doNotRefresh.add(systemFragment);
				}
			}
		}
	}

	private Collection<Bundle> uninstallBundles(HashSet<BundleInfo> toUninstall) {
		Collection<Bundle> removedBundles = new ArrayList<>(toUninstall.size());
		for (BundleInfo current : toUninstall) {
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

		File lastBundlesTxt = getLastBundleInfo();
		try (OutputStream destinationStream = new FileOutputStream(lastBundlesTxt)) {
			ArrayList<File> sourcesLocation = SimpleConfiguratorUtils.getInfoFiles();
			List<InputStream> sourceStreams = new ArrayList<>(sourcesLocation.size() + 1);
			sourceStreams.add(url.openStream());
			if (Activator.EXTENDED) {
				for (File source : sourcesLocation) {
					sourceStreams.add(new FileInputStream(source));
				}
			}
			SimpleConfiguratorUtils.transferStreams(sourceStreams, destinationStream);
		} catch (URISyntaxException e) {
			// nothing, was discovered when starting framework
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
			return SimpleConfiguratorUtils.readConfiguration(lastBundlesInfo.toURL(), baseLocation).toArray(new BundleInfo[1]);
		} catch (IOException e) {
			return null;
		}
	}

	private ArrayList<Bundle> installBundles(BundleInfo[] finalList, Collection<Bundle> toStart) {
		ArrayList<Bundle> toRefresh = new ArrayList<>();

		String useReferenceProperty = manipulatingContext.getProperty(SimpleConfiguratorConstants.PROP_KEY_USE_REFERENCE);
		boolean useReference = useReferenceProperty == null ? runningOnEquinox : Boolean.parseBoolean(useReferenceProperty);

		for (BundleInfo element : finalList) {
			if (element == null)
				continue;
			//TODO here we do not deal with bundles that don't have a symbolic id
			//TODO Need to handle the case where getBundles return multiple value

			String symbolicName = element.getSymbolicName();
			String version = element.getVersion();

			Bundle[] matches = null;
			if (symbolicName != null && version != null)
				matches = packageAdminService.getBundles(symbolicName, getVersionRange(version));

			String bundleLocation = SimpleConfiguratorUtils.getBundleLocation(element, useReference);

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
						System.out.println("installed bundle:" + element); //$NON-NLS-1$
					toRefresh.add(current);
				} catch (BundleException e) {
					if (Activator.DEBUG) {
						System.err.println("Can't install " + symbolicName + '/' + version + " from location " + element.getLocation()); //$NON-NLS-1$ //$NON-NLS-2$
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
						System.out.println("installed bundle:" + element); //$NON-NLS-1$
					toRefresh.add(current);
				} catch (BundleException e) {
					if (Activator.DEBUG) {
						System.err.println("Can't install " + symbolicName + '/' + version + " from location " + element.getLocation()); //$NON-NLS-1$ //$NON-NLS-2$
						e.printStackTrace();
					}
					continue;
				}
			}

			// Mark Started
			if (element.isMarkedAsStarted()) {
				toStart.add(current);
			}

			// Set Start Level
			int startLevel = element.getStartLevel();
			if (startLevel < 1)
				continue;
			if (current.getBundleId() == 0)
				continue;
			if (isFragment(current))
				continue;
			if (SimpleConfiguratorConstants.TARGET_CONFIGURATOR_NAME.equals(current.getSymbolicName()))
				continue;

			try {
				current.adapt(BundleStartLevel.class).setStartLevel(startLevel);
			} catch (IllegalArgumentException ex) {
				Utils.log(4, null, null, "Failed to set start level of Bundle:" + element, ex); //$NON-NLS-1$
			}
		}
		return toRefresh;
	}

	private boolean isFragment(Bundle current) {
		BundleRevision revision = current.adapt(BundleRevision.class);
		return (revision != null) && ((revision.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0);
	}

	private void refreshPackages(Collection<Bundle> bundles) {
		if (bundles.isEmpty()) {
			return;
		}

		CountDownLatch latch = new CountDownLatch(1);
		FrameworkListener listener = event -> {
			if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
				latch.countDown();
			}
		};
		frameworkWiring.refreshBundles(bundles, listener);
		try {
			latch.await();
		} catch (InterruptedException e) {
			// ignore
		}
	}

	private void startBundles(Bundle[] bundles) {
		for (Bundle bundle : bundles) {
			if (bundle.getState() == Bundle.UNINSTALLED) {
				System.err.println("Could not start: " + bundle.getSymbolicName() + '(' + bundle.getLocation() + ':' + bundle.getBundleId() + ')' + ". It's state is uninstalled.");
				continue;
			}
			if (bundle.getState() == Bundle.STARTING && (bundle == callingBundle || bundle == manipulatingContext.getBundle()))
				continue;
			if (isFragment(bundle))
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
		Set<Bundle> removedBundles = new HashSet<>(allBundles.length);
		//		configurator.setPrerequisiteBundles(allBundles);
		for (Bundle allBundle : allBundles) {
			if (allBundle.getBundleId() == 0) {
				continue;
			}
			removedBundles.add(allBundle);
		}

		//Remove all the bundles appearing in the final list from the set of installed bundles
		for (BundleInfo element : finalList) {
			if (element == null)
				continue;
			Bundle[] toAdd = packageAdmin.getBundles(element.getSymbolicName(), getVersionRange(element.getVersion()));
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
		return version == null ? null : new StringBuilder().append('[').append(version).append(',').append(version).append(']').toString();
	}
}
