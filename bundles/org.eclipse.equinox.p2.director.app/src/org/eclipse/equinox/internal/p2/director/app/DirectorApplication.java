/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith - https://bugs.eclipse.org/bugs/show_bug.cgi?id=226401
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.director.app;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.console.ProvisioningHelper;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.*;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.LatestIUVersionQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.*;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;

public class DirectorApplication implements IApplication {
	class LocationQueryable implements IQueryable {
		private URI location;

		public LocationQueryable(URI location) {
			this.location = location;
		}

		public Collector query(Query query, Collector collector, IProgressMonitor monitor) {
			return ProvisioningHelper.getInstallableUnits(location, query, collector, monitor);
		}
	}

	static private final String BUNDLE_CORE = "org.eclipse.equinox.p2.core"; //$NON-NLS-1$
	static private final String BUNDLE_ENGINE = "org.eclipse.equinox.p2.engine"; //$NON-NLS-1$
	static private final String BUNDLE_EXEMPLARY_SETUP = "org.eclipse.equinox.p2.exemplarysetup"; //$NON-NLS-1$
	static private final String BUNDLE_FRAMEWORKADMIN_EQUINOX = "org.eclipse.equinox.frameworkadmin.equinox"; //$NON-NLS-1$
	static private final String BUNDLE_SIMPLE_CONFIGURATOR_MANIPULATOR = "org.eclipse.equinox.simpleconfigurator.manipulator"; //$NON-NLS-1$
	private static final Integer EXIT_ERROR = new Integer(13);
	static private final String FLAVOR_DEFAULT = "tooling"; //$NON-NLS-1$
	static private final String PROP_P2_DATA_AREA = "eclipse.p2.data.area"; //$NON-NLS-1$

	static private final String PROP_P2_PROFILE = "eclipse.p2.profile"; //$NON-NLS-1$

	private static void getURIs(List uris, String spec) throws CoreException {
		if (spec == null)
			return;
		String[] urlSpecs = StringHelper.getArrayFromString(spec, ',');
		for (int i = 0; i < urlSpecs.length; i++) {
			try {
				uris.add(URIUtil.fromString(urlSpecs[i]));
			} catch (URISyntaxException e) {
				throw new ProvisionException(NLS.bind(Messages.unable_to_parse_0_to_uri_1, urlSpecs[i], e.getMessage()));
			}
		}
	}

	private static void optionRequiresArgument(String opt) throws CoreException {
		throw new ProvisionException(NLS.bind(Messages.option_0_requires_an_argument, opt));
	}

	private static void parseIUsArgument(List vnames, String arg) {
		String[] roots = StringHelper.getArrayFromString(arg, ',');
		for (int i = 0; i < roots.length; ++i)
			vnames.add(VersionedName.parse(roots[i]));
	}

	private static String processFileArgument(String arg) {
		if (arg.startsWith("file:")) //$NON-NLS-1$
			arg = arg.substring(5);

		// we create a path object here to handle ../ entries in the middle of paths
		return Path.fromOSString(arg).toOSString();
	}

	private IArtifactRepositoryManager artifactManager;
	private IMetadataRepositoryManager metadataManager;

	private URI[] artifactReposForRemoval;
	private URI[] metadataReposForRemoval;

	private final List artifactRepositoryLocations = new ArrayList();
	private final List metadataRepositoryLocations = new ArrayList();
	private final List rootsToInstall = new ArrayList();
	private final List rootsToUninstall = new ArrayList();

	private String bundlePool = null;
	private String destination;
	private String sharedLocation;
	private String flavor;
	private boolean printHelpInfo = false;
	private boolean printIUList = false;
	private boolean verifyOnly = false;
	private boolean roamingProfile = false;
	private boolean stackTrace = false;
	private String profileId;
	private String profileProperties; // a comma-separated list of property pairs "tag=value"
	private String ws;
	private String os;
	private String arch;
	private String nl;

	private IEngine engine;
	private boolean noProfileId = false;
	private PackageAdmin packageAdmin;
	private ServiceReference packageAdminRef;
	private IPlanner planner;

	private String preservedDataArea;
	private String preservedProfile;
	private boolean restartCore;
	private boolean restartEngine;
	private boolean restartExemplarySetup;

	private ProfileChangeRequest buildProvisioningRequest(IProfile profile, IInstallableUnit[] installs, IInstallableUnit[] uninstalls) {
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		markRoots(request, installs);
		markRoots(request, uninstalls);
		request.addInstallableUnits(installs);
		request.removeInstallableUnits(uninstalls);
		return request;
	}

	private void cleanupRepositories() {
		if (artifactReposForRemoval != null && artifactManager != null) {
			for (int i = 0; i < artifactReposForRemoval.length && artifactReposForRemoval[i] != null; i++) {
				artifactManager.removeRepository(artifactReposForRemoval[i]);
			}
		}
		if (metadataReposForRemoval != null && metadataManager != null) {
			for (int i = 0; i < metadataReposForRemoval.length && metadataReposForRemoval[i] != null; i++) {
				metadataManager.removeRepository(metadataReposForRemoval[i]);
			}
		}
	}

	private Collector collectRootIUs(Query query, Collector collector) {
		IProgressMonitor nullMonitor = new NullProgressMonitor();

		int top = metadataRepositoryLocations.size();
		if (top == 0)
			return ProvisioningHelper.getInstallableUnits(null, query, collector, nullMonitor);

		Collector result = collector != null ? collector : new Collector();
		IQueryable[] locationQueryables = new IQueryable[top];
		for (int i = 0; i < top; i++)
			locationQueryables[i] = new LocationQueryable((URI) metadataRepositoryLocations.get(i));
		return new CompoundQueryable(locationQueryables).query(query, result, nullMonitor);
	}

	private IInstallableUnit[] collectRoots(IProfile profile, List rootNames) throws CoreException {
		ArrayList allRoots = new ArrayList();
		int top = rootNames.size();
		for (int i = 0; i < top; ++i) {
			VersionedName rootName = (VersionedName) rootNames.get(i);
			Version v = rootName.getVersion();
			Query query = new InstallableUnitQuery(rootName.getId(), Version.emptyVersion.equals(v) ? VersionRange.emptyRange : new VersionRange(v, true, v, true));
			Collector roots = collectRootIUs(new CompositeQuery(new Query[] {query, new LatestIUVersionQuery()}), new Collector());
			if (roots.size() <= 0)
				roots = profile.query(query, roots, new NullProgressMonitor());
			if (roots.size() <= 0)
				throw new CoreException(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.Missing_IU, rootName)));
			allRoots.addAll(roots.toCollection());
		}
		return (IInstallableUnit[]) allRoots.toArray(new IInstallableUnit[allRoots.size()]);

	}

	synchronized Bundle getBundle(String symbolicName) {
		if (packageAdmin == null)
			return null;

		Bundle[] bundles = packageAdmin.getBundles(symbolicName, null);
		if (bundles == null)
			return null;
		//Return the first bundle that is not installed or uninstalled
		for (int i = 0; i < bundles.length; i++) {
			if ((bundles[i].getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
				return bundles[i];
			}
		}
		return null;
	}

	private String getEnvironmentProperty() {
		HashMap values = new HashMap();
		if (os != null)
			values.put("osgi.os", os); //$NON-NLS-1$
		if (nl != null)
			values.put("osgi.nl", nl); //$NON-NLS-1$
		if (ws != null)
			values.put("osgi.ws", ws); //$NON-NLS-1$
		if (arch != null)
			values.put("osgi.arch", arch); //$NON-NLS-1$
		return values.isEmpty() ? null : toString(values);
	}

	private IProfile initializeProfile() throws CoreException {
		if (profileId == null) {
			profileId = IProfileRegistry.SELF;
			noProfileId = true;
		}
		IProfile profile = ProvisioningHelper.getProfile(profileId);
		if (profile == null) {
			if (destination == null)
				missingArgument("destination"); //$NON-NLS-1$
			if (flavor == null)
				flavor = System.getProperty("eclipse.p2.configurationFlavor", FLAVOR_DEFAULT); //$NON-NLS-1$

			Properties props = new Properties();
			props.setProperty(IProfile.PROP_INSTALL_FOLDER, destination);
			props.setProperty(IProfile.PROP_FLAVOR, flavor);
			if (bundlePool == null)
				props.setProperty(IProfile.PROP_CACHE, sharedLocation == null ? destination : sharedLocation);
			else
				props.setProperty(IProfile.PROP_CACHE, bundlePool);
			if (roamingProfile)
				props.setProperty(IProfile.PROP_ROAMING, Boolean.TRUE.toString());

			String env = getEnvironmentProperty();
			if (env != null)
				props.setProperty(IProfile.PROP_ENVIRONMENTS, env);
			if (profileProperties != null)
				putProperties(profileProperties, props);
			profile = ProvisioningHelper.addProfile(profileId, props);
			String currentFlavor = profile.getProperty(IProfile.PROP_FLAVOR);
			if (currentFlavor != null && !currentFlavor.endsWith(flavor))
				throw new RuntimeException(NLS.bind(Messages.flavor_0_inconsistent_with_flavor_1, flavor, currentFlavor));
		}
		return profile;
	}

	private void initializeRepositories() throws CoreException {
		if (rootsToInstall.isEmpty())
			// Not much point initializing repositories if we have nothing to install
			return;

		if (artifactRepositoryLocations == null)
			missingArgument("-artifactRepository"); //$NON-NLS-1$

		artifactManager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
		if (artifactManager == null)
			throw new ProvisionException(Messages.Application_NoManager);

		int removalIdx = 0;
		boolean anyValid = false; // do we have any valid repos or did they all fail to load?
		artifactReposForRemoval = new URI[artifactRepositoryLocations.size()];
		for (int i = 0; i < artifactRepositoryLocations.size(); i++) {
			URI location = (URI) artifactRepositoryLocations.get(i);
			try {
				if (!artifactManager.contains(location)) {
					artifactManager.loadRepository(location, null);
					artifactReposForRemoval[removalIdx++] = location;
				}
				anyValid = true;
			} catch (ProvisionException e) {
				//one of the repositories did not load
				LogHelper.log(e.getStatus());
			}
		}
		if (!anyValid)
			//all repositories failed to load
			throw new ProvisionException(Messages.Application_NoRepositories);

		if (metadataRepositoryLocations == null)
			missingArgument("metadataRepository"); //$NON-NLS-1$

		metadataManager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.class.getName());
		if (metadataManager == null)
			throw new ProvisionException(Messages.Application_NoManager);

		removalIdx = 0;
		anyValid = false; // do we have any valid repos or did they all fail to load?
		int top = metadataRepositoryLocations.size();
		metadataReposForRemoval = new URI[top];
		for (int i = 0; i < top; i++) {
			URI location = (URI) metadataRepositoryLocations.get(i);
			try {
				if (!metadataManager.contains(location)) {
					metadataManager.loadRepository(location, null);
					metadataReposForRemoval[removalIdx++] = location;
				}
				anyValid = true;
			} catch (ProvisionException e) {
				//one of the repositories did not load
				LogHelper.log(e.getStatus());
			}
		}
		if (!anyValid)
			//all repositories failed to load
			throw new ProvisionException(Messages.Application_NoRepositories);
	}

	private void initializeServices() throws CoreException {
		BundleContext context = Activator.getContext();
		packageAdminRef = context.getServiceReference(PackageAdmin.class.getName());
		packageAdmin = (PackageAdmin) context.getService(packageAdminRef);
		if (destination != null || sharedLocation != null) {
			restartExemplarySetup = stopTransient(BUNDLE_EXEMPLARY_SETUP);
			restartEngine = stopTransient(BUNDLE_ENGINE); // Since it uses the agent data location
			restartCore = stopTransient(BUNDLE_CORE); // Since it manages the agent data location	

			// Set/Clear properties that we don't want to inherit from the caller
			String p2DataArea = sharedLocation == null ? new File(destination, "p2").getAbsolutePath() : sharedLocation; //$NON-NLS-1$
			preservedDataArea = System.setProperty(PROP_P2_DATA_AREA, p2DataArea);
			if (profileId == null) {
				if (destination != null) {
					File configIni = new File(destination, "configuration/config.ini"); //$NON-NLS-1$
					InputStream in = null;
					try {
						Properties ciProps = new Properties();
						in = new BufferedInputStream(new FileInputStream(configIni));
						ciProps.load(in);
						profileId = ciProps.getProperty(PROP_P2_PROFILE);
					} catch (IOException e) {
						// Ignore
					} finally {
						if (in != null)
							try {
								in.close();
							} catch (IOException e) {
								// Ignore;
							}
					}
					if (profileId == null)
						profileId = destination;
				}
			}
			if (profileId == null)
				preservedProfile = System.setProperty(PROP_P2_PROFILE, profileId);
			else
				System.getProperties().remove(PROP_P2_PROFILE);
		}

		if (!startTransient(BUNDLE_SIMPLE_CONFIGURATOR_MANIPULATOR))
			throw new ProvisionException(NLS.bind(Messages.Missing_bundle, BUNDLE_SIMPLE_CONFIGURATOR_MANIPULATOR));
		if (!startTransient(BUNDLE_FRAMEWORKADMIN_EQUINOX))
			throw new ProvisionException(NLS.bind(Messages.Missing_bundle, BUNDLE_FRAMEWORKADMIN_EQUINOX));
		if (!startTransient(BUNDLE_CORE))
			throw new ProvisionException(NLS.bind(Messages.Missing_bundle, BUNDLE_CORE));
		if (!startTransient(BUNDLE_ENGINE))
			throw new ProvisionException(NLS.bind(Messages.Missing_bundle, BUNDLE_ENGINE));
		if (!startTransient(BUNDLE_EXEMPLARY_SETUP))
			throw new ProvisionException(NLS.bind(Messages.Missing_bundle, BUNDLE_EXEMPLARY_SETUP));

		IDirector director = (IDirector) ServiceHelper.getService(context, IDirector.class.getName());
		if (director == null)
			throw new ProvisionException(Messages.Missing_director);

		planner = (IPlanner) ServiceHelper.getService(context, IPlanner.class.getName());
		if (planner == null)
			throw new ProvisionException(Messages.Missing_planner);

		engine = (IEngine) ServiceHelper.getService(context, IEngine.SERVICE_NAME);
		if (engine == null)
			throw new ProvisionException(Messages.Missing_Engine);
	}

	private void logFailure(IStatus status) {
		FrameworkLog log = (FrameworkLog) ServiceHelper.getService(Activator.getContext(), FrameworkLog.class.getName());
		if (log != null)
			System.err.println("Application failed, log file location: " + log.getFile()); //$NON-NLS-1$
		LogHelper.log(status);
	}

	private void markRoots(ProfileChangeRequest request, IInstallableUnit[] roots) {
		for (int idx = 0; idx < roots.length; ++idx)
			request.setInstallableUnitProfileProperty(roots[idx], IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.TRUE.toString());
	}

	private void missingArgument(String argumentName) throws CoreException {
		throw new ProvisionException(NLS.bind(Messages.Missing_Required_Argument, argumentName));
	}

	private void performList() throws CoreException {
		Query query = new InstallableUnitQuery(null, VersionRange.emptyRange);
		if (metadataRepositoryLocations.isEmpty())
			missingArgument("metadataRepository"); //$NON-NLS-1$

		Collector roots = collectRootIUs(query, null);
		Iterator unitIterator = roots.iterator();
		while (unitIterator.hasNext()) {
			IInstallableUnit iu = (IInstallableUnit) unitIterator.next();
			System.out.println(iu.getId());
		}
	}

	private void performProvisioningActions() throws CoreException {
		IProfile profile = initializeProfile();
		IInstallableUnit[] installs = collectRoots(profile, rootsToInstall);
		IInstallableUnit[] uninstalls = collectRoots(profile, rootsToUninstall);

		// keep this result status in case there is a problem so we can report it to the user
		boolean wasRoaming = Boolean.valueOf(profile.getProperty(IProfile.PROP_ROAMING)).booleanValue();
		try {
			updateRoamingProperties(profile);
			ProvisioningContext context = new ProvisioningContext((URI[]) metadataRepositoryLocations.toArray(new URI[metadataRepositoryLocations.size()]));
			context.setArtifactRepositories((URI[]) artifactRepositoryLocations.toArray(new URI[artifactRepositoryLocations.size()]));
			ProfileChangeRequest request = buildProvisioningRequest(profile, installs, uninstalls);
			printRequest(request);
			planAndExecute(profile, context, request);
		} finally {
			// if we were originally were set to be roaming and we changed it, change it back before we return
			if (wasRoaming && !Boolean.valueOf(profile.getProperty(IProfile.PROP_ROAMING)).booleanValue())
				setRoaming(profile);
		}
	}

	private void planAndExecute(IProfile profile, ProvisioningContext context, ProfileChangeRequest request) throws CoreException {
		ProvisioningPlan result = planner.getProvisioningPlan(request, context, new NullProgressMonitor());
		IStatus operationStatus = result.getStatus();
		if (!operationStatus.isOK())
			throw new CoreException(operationStatus);
		if (!verifyOnly) {
			operationStatus = PlanExecutionHelper.executePlan(result, engine, context, new NullProgressMonitor());
			if (!operationStatus.isOK())
				throw new CoreException(operationStatus);
		}
	}

	private void printRequest(ProfileChangeRequest request) {
		IInstallableUnit[] toAdd = request.getAddedInstallableUnits();
		for (int i = 0; i < toAdd.length; i++) {
			System.out.println(NLS.bind(Messages.Installing, toAdd[i].getId(), toAdd[i].getVersion()));
		}
		IInstallableUnit[] toRemove = request.getRemovedInstallableUnits();
		for (int i = 0; i < toRemove.length; i++) {
			System.out.println(NLS.bind(Messages.Uninstalling, toRemove[i].getId(), toRemove[i].getVersion()));
		}
	}

	public void processArguments(String[] args) throws CoreException {
		if (args == null) {
			printHelpInfo = true;
			return;
		}

		for (int i = 0; i < args.length; i++) {
			// check for args without parameters (i.e., a flag arg)
			String opt = args[i];
			if ("-list".equalsIgnoreCase(opt) || "-l".equals(opt)) { //$NON-NLS-1$ //$NON-NLS-2$
				printIUList = true;
				continue;
			}

			if ("-help".equalsIgnoreCase(opt) || "-h".equals(opt) || "-?".equals(opt)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				printHelpInfo = true;
				continue;
			}

			if ("-installIU".equalsIgnoreCase(opt) || "-installIUs".equalsIgnoreCase(opt) || "-i".equals(opt)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				if (++i >= args.length)
					optionRequiresArgument(opt);
				parseIUsArgument(rootsToInstall, args[i]);
				continue;
			}

			if ("-uninstallIU".equalsIgnoreCase(opt) || "-uninstallIUs".equalsIgnoreCase(opt) || "-u".equals(opt)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				if (++i >= args.length)
					optionRequiresArgument(opt);
				parseIUsArgument(rootsToUninstall, args[i]);
				continue;
			}

			if ("-profile".equalsIgnoreCase(opt) || "-p".equals(opt)) { //$NON-NLS-1$ //$NON-NLS-2$
				if (++i >= args.length)
					optionRequiresArgument(opt);
				profileId = args[i];
				continue;
			}

			if ("-flavor".equalsIgnoreCase(opt) || "-f".equals(opt)) { //$NON-NLS-1$ //$NON-NLS-2$
				if (++i >= args.length)
					optionRequiresArgument(opt);
				flavor = args[i];
				continue;
			}

			if ("-shared".equalsIgnoreCase(opt) || "-s".equals(opt)) { //$NON-NLS-1$ //$NON-NLS-2$
				if (++i < args.length) {
					String nxt = args[i];
					if (nxt.startsWith("-")) //$NON-NLS-1$
						--i; // Oops, that's the next option, not an argument
					else
						sharedLocation = processFileArgument(nxt);
				}
				if (sharedLocation == null)
					// -shared without an argument means "Use default shared area"
					sharedLocation = Path.fromOSString(System.getProperty("user.home")).append(".p2/").toOSString(); //$NON-NLS-1$ //$NON-NLS-2$
				continue;
			}

			if ("-destination".equalsIgnoreCase(opt) || "-d".equals(opt)) { //$NON-NLS-1$ //$NON-NLS-2$
				if (++i >= args.length)
					optionRequiresArgument(opt);
				destination = processFileArgument(args[i]);
				continue;
			}

			if ("-bundlepool".equalsIgnoreCase(opt) || "-b".equals(opt)) { //$NON-NLS-1$ //$NON-NLS-2$
				if (++i >= args.length)
					optionRequiresArgument(opt);
				bundlePool = processFileArgument(args[i]);
				continue;
			}

			if ("-metadataRepository".equalsIgnoreCase(opt) || "-metadataRepositories".equalsIgnoreCase(opt) || "-m".equalsIgnoreCase(opt)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				if (++i >= args.length)
					optionRequiresArgument(opt);
				getURIs(metadataRepositoryLocations, args[i]);
				continue;
			}

			if ("-artifactRepository".equalsIgnoreCase(opt) || "-artifactRepositories".equalsIgnoreCase(opt) || "-a".equalsIgnoreCase(opt)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				if (++i >= args.length)
					optionRequiresArgument(opt);
				getURIs(artifactRepositoryLocations, args[i]);
				continue;
			}

			if ("-repository".equalsIgnoreCase(opt) || "-repositories".equalsIgnoreCase(opt) || "-r".equalsIgnoreCase(opt)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				if (++i >= args.length)
					optionRequiresArgument(opt);
				getURIs(metadataRepositoryLocations, args[i]);
				getURIs(artifactRepositoryLocations, args[i]);
				continue;
			}

			if ("-profileProperties".equalsIgnoreCase(opt)) { //$NON-NLS-1$
				if (++i >= args.length)
					optionRequiresArgument(opt);
				profileProperties = args[i];
				continue;
			}

			if ("-roaming".equalsIgnoreCase(opt)) { //$NON-NLS-1$
				roamingProfile = true;
				continue;
			}

			if ("-verifyOnly".equalsIgnoreCase(opt)) { //$NON-NLS-1$
				verifyOnly = true;
				continue;
			}

			if ("-p2.os".equalsIgnoreCase(opt)) { //$NON-NLS-1$
				if (++i >= args.length)
					optionRequiresArgument(opt);
				os = args[i];
				continue;
			}

			if ("-p2.ws".equalsIgnoreCase(opt)) { //$NON-NLS-1$
				if (++i >= args.length)
					optionRequiresArgument(opt);
				ws = args[i];
				continue;
			}

			if ("-p2.nl".equalsIgnoreCase(opt)) { //$NON-NLS-1$
				if (++i >= args.length)
					optionRequiresArgument(opt);
				nl = args[i];
				continue;
			}

			if ("-p2.arch".equalsIgnoreCase(opt)) { //$NON-NLS-1$
				if (++i >= args.length)
					optionRequiresArgument(opt);
				arch = args[i];
				continue;
			}
			throw new ProvisionException(NLS.bind(Messages.unknown_option_0, opt));
		}

		if (!printHelpInfo && !printIUList && rootsToInstall.isEmpty() && rootsToUninstall.isEmpty())
			// Nothing to do, default to print the help info
			printHelpInfo = true;
	}

	/**
	 * @param pairs	a comma separated list of tag=value pairs
	 * @param properties the collection into which the pairs are put
	 */
	private void putProperties(String pairs, Properties properties) {
		String[] propPairs = StringHelper.getArrayFromString(pairs, ',');
		for (int i = 0; i < propPairs.length; ++i) {
			String propPair = propPairs[i];
			int eqIdx = propPair.indexOf('=');
			if (eqIdx < 0)
				continue;
			String tag = propPair.substring(0, eqIdx).trim();
			if (tag.length() == 0)
				continue;
			String value = propPair.substring(eqIdx + 1).trim();
			if (value.length() > 0)
				properties.put(tag, value);
		}
	}

	private void restoreServices() throws CoreException {
		BundleContext context = Activator.getContext();
		try {
			if (destination == null && sharedLocation == null)
				return;

			stopTransient(BUNDLE_EXEMPLARY_SETUP);
			stopTransient(BUNDLE_CORE); // Since it manages the agent data location

			if (preservedDataArea != null)
				System.setProperty(PROP_P2_DATA_AREA, preservedDataArea);
			else
				System.getProperties().remove(PROP_P2_DATA_AREA);

			if (preservedProfile != null)
				System.setProperty(PROP_P2_PROFILE, preservedProfile);
			else
				System.getProperties().remove(PROP_P2_PROFILE);

			if (restartCore)
				startTransient(BUNDLE_CORE);
			if (restartEngine)
				startTransient(BUNDLE_ENGINE);
			if (restartExemplarySetup)
				startTransient(BUNDLE_EXEMPLARY_SETUP);
		} finally {
			context.ungetService(packageAdminRef);
		}
	}

	public Object run(String[] args) throws CoreException {
		long time = System.currentTimeMillis();

		try {
			processArguments(args);
			initializeServices();
			initializeRepositories();
			if (!(rootsToInstall.isEmpty() && rootsToUninstall.isEmpty()))
				performProvisioningActions();
			if (printIUList)
				performList();
			if (printHelpInfo)
				performHelpInfo();
			System.out.println(NLS.bind(Messages.Operation_complete, new Long(System.currentTimeMillis() - time)));
			return IApplication.EXIT_OK;
		} catch (CoreException e) {
			deeplyPrint(e.getStatus(), System.err, 0);
			logFailure(e.getStatus());
			return EXIT_ERROR;
		} finally {
			cleanupRepositories();
			restoreServices();
		}
	}

	private static void appendLevelPrefix(PrintStream strm, int level) {
		for (int idx = 0; idx < level; ++idx)
			strm.print(' ');
	}

	private void deeplyPrint(CoreException ce, PrintStream strm, int level) {
		appendLevelPrefix(strm, level);
		if (stackTrace)
			ce.printStackTrace(strm);
		deeplyPrint(ce.getStatus(), strm, level);
	}

	private void deeplyPrint(IStatus status, PrintStream strm, int level) {
		appendLevelPrefix(strm, level);
		String msg = status.getMessage();
		strm.println(msg);
		Throwable cause = status.getException();
		if (cause != null) {
			strm.print("Caused by: "); //$NON-NLS-1$
			if (stackTrace || !(msg.equals(cause.getMessage()) || msg.equals(cause.toString())))
				deeplyPrint(cause, strm, level);
		}

		if (status.isMultiStatus()) {
			IStatus[] children = status.getChildren();
			for (int i = 0; i < children.length; i++)
				deeplyPrint(children[i], strm, level + 1);
		}
	}

	private void deeplyPrint(Throwable t, PrintStream strm, int level) {
		if (t instanceof CoreException)
			deeplyPrint((CoreException) t, strm, level);
		else {
			appendLevelPrefix(strm, level);
			if (stackTrace)
				t.printStackTrace(strm);
			else {
				strm.println(t.toString());
				Throwable cause = t.getCause();
				if (cause != null) {
					strm.print("Caused by: "); //$NON-NLS-1$
					deeplyPrint(cause, strm, level);
				}
			}
		}
	}

	private void performHelpInfo() {
		// TODO: Emit help

	}

	/*
	 * Set the roaming property on the given profile.
	 */
	private IStatus setRoaming(IProfile profile) {
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.setProfileProperty(IProfile.PROP_ROAMING, "true"); //$NON-NLS-1$
		ProvisioningContext context = new ProvisioningContext(new URI[0]);
		context.setArtifactRepositories(new URI[0]);
		ProvisioningPlan result = planner.getProvisioningPlan(request, context, new NullProgressMonitor());
		return PlanExecutionHelper.executePlan(result, engine, context, new NullProgressMonitor());
	}

	public Object start(IApplicationContext context) throws Exception {
		return run((String[]) context.getArguments().get("application.args")); //$NON-NLS-1$
	}

	private boolean startTransient(String bundleName) throws CoreException {
		Bundle bundle = getBundle(bundleName);
		if (bundle == null)
			return false;
		try {
			bundle.start(Bundle.START_TRANSIENT);
		} catch (BundleException e) {
			throw new ProvisionException(NLS.bind(Messages.unable_to_start_bundle_0, bundleName));
		}
		return true;
	}

	public void stop() {
		// Nothing left to do here
	}

	private boolean stopTransient(String bundleName) throws CoreException {
		Bundle bundle = getBundle(bundleName);
		boolean wasActive = false;
		if (bundle != null) {
			wasActive = (bundle.getState() & (Bundle.ACTIVE | Bundle.STARTING)) != 0;
			if (wasActive)
				try {
					bundle.stop(Bundle.STOP_TRANSIENT);
				} catch (BundleException e) {
					throw new ProvisionException(NLS.bind(Messages.unable_to_stop_bundle_0, bundleName));
				}
		}
		return wasActive;
	}

	private String toString(Map context) {
		StringBuffer result = new StringBuffer();
		Iterator entries = context.entrySet().iterator();
		while (entries.hasNext()) {
			Map.Entry entry = (Map.Entry) entries.next();
			if (result.length() > 0)
				result.append(',');
			result.append((String) entry.getKey());
			result.append('=');
			result.append((String) entry.getValue());
		}
		return result.toString();
	}

	private void updateRoamingProperties(IProfile profile) throws CoreException {
		// if the user didn't specify a destination path on the command-line
		// then we assume they are installing into the currently running
		// instance and we don't have anything to update
		if (destination == null)
			return;

		// if the user didn't set a profile id on the command-line this is ok if they
		// also didn't set the destination path. (handled in the case above) otherwise throw an error.
		if (noProfileId) // && destination != null
			throw new ProvisionException(Messages.Missing_profileid);

		// make sure that we are set to be roaming before we update the values
		if (!Boolean.valueOf(profile.getProperty(IProfile.PROP_ROAMING)).booleanValue())
			return;

		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		File destinationFile = new File(destination);
		if (!destinationFile.equals(new File(profile.getProperty(IProfile.PROP_INSTALL_FOLDER))))
			request.setProfileProperty(IProfile.PROP_INSTALL_FOLDER, destination);
		if (!destinationFile.equals(new File(profile.getProperty(IProfile.PROP_CACHE))))
			request.setProfileProperty(IProfile.PROP_CACHE, destination);
		if (request.getProfileProperties().size() == 0)
			return;

		// otherwise we have to make a change so set the profile to be non-roaming so the 
		// values don't get recalculated to the wrong thing if we are flushed from memory - we
		// will set it back later (see bug 269468)
		request.setProfileProperty(IProfile.PROP_ROAMING, "false"); //$NON-NLS-1$

		ProvisioningContext context = new ProvisioningContext(new URI[0]);
		context.setArtifactRepositories(new URI[0]);
		ProvisioningPlan result = planner.getProvisioningPlan(request, context, new NullProgressMonitor());
		IStatus status = PlanExecutionHelper.executePlan(result, engine, context, new NullProgressMonitor());
		if (!status.isOK())
			throw new CoreException(new MultiStatus(Activator.ID, IStatus.ERROR, new IStatus[] {status}, NLS.bind(Messages.Cant_change_roaming, profile.getProfileId()), null));
	}
}