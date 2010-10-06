/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Cloudsmith - https://bugs.eclipse.org/bugs/show_bug.cgi?id=226401
 *     EclipseSource - ongoing development
 *     Sonatype, Inc. - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.director.app;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.IDirector;
import org.eclipse.equinox.internal.provisional.p2.director.PlanExecutionHelper;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.ServiceReference;

/**
 * This is the original p2 director application created for the p2 1.0 release. There
 * is a replacement application in {@link DirectorApplication} that should be preferred
 * over this implementation where possible. This implementation remains for backwards
 * compatibility purposes.
 * @deprecated
 */
public class Application implements IApplication {
	private static final Integer EXIT_ERROR = new Integer(13);
	static private final String ANT_PROPERTY_PREFIX = "${"; //$NON-NLS-1$
	static private final String FLAVOR_DEFAULT = "tooling"; //$NON-NLS-1$

	public static final int COMMAND_INSTALL = 0;
	public static final int COMMAND_UNINSTALL = 1;
	public static final int COMMAND_LIST = 2;

	public static final String[] COMMAND_NAMES = {"-installIU", "-uninstallIU", "-list"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	private Path destination;

	private URI[] artifactRepositoryLocations;
	private URI[] metadataRepositoryLocations;

	private URI[] metadataReposForRemoval;
	private URI[] artifactReposForRemoval;
	private IArtifactRepositoryManager artifactManager;
	private IMetadataRepositoryManager metadataManager;

	private String root;
	private Version version = null;
	private String flavor;
	private String profileId;
	private String profileProperties; // a comma-separated list of property pairs "tag=value"
	private String bundlePool = null;
	private String nl;
	private String os;
	private String arch;
	private String ws;
	private boolean roamingProfile = false;
	private IPlanner planner;
	private IEngine engine;
	private boolean noProfileId = false;

	private int command = -1;
	protected IProvisioningAgent agent;

	private void ambigousCommand(int cmd1, int cmd2) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.Ambigous_Command, new Object[] {COMMAND_NAMES[cmd1], COMMAND_NAMES[cmd2]})));
	}

	private ProfileChangeRequest buildProvisioningRequest(IProfile profile, IQueryResult<IInstallableUnit> roots, boolean install) {
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		markRoots(request, roots);
		if (install) {
			request.addAll(roots.toUnmodifiableSet());
		} else {
			request.removeAll(roots.toUnmodifiableSet());
		}
		return request;
	}

	/*
	 * Copied from ProvisioningHelper 1.57
	 */
	static IProfile addProfile(IProvisioningAgent agent, String profileId, Map<String, String> properties) throws ProvisionException {
		IProfileRegistry profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
		if (profileRegistry == null)
			return null;
		IProfile profile = profileRegistry.getProfile(profileId);
		if (profile != null)
			return profile;

		Map<String, String> profileProperties = new HashMap<String, String>(properties);
		if (profileProperties.get(IProfile.PROP_ENVIRONMENTS) == null) {
			EnvironmentInfo info = (EnvironmentInfo) ServiceHelper.getService(Activator.getContext(), EnvironmentInfo.class.getName());
			if (info != null)
				profileProperties.put(IProfile.PROP_ENVIRONMENTS, "osgi.os=" + info.getOS() + ",osgi.ws=" + info.getWS() + ",osgi.arch=" + info.getOSArch()); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			else
				profileProperties.put(IProfile.PROP_ENVIRONMENTS, ""); //$NON-NLS-1$
		}
		return profileRegistry.addProfile(profileId, profileProperties);
	}

	/*
	 * Copied from ProvisioningHelper 1.57
	 */
	static IProfile getProfile(IProvisioningAgent agent, String id) {
		IProfileRegistry profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
		if (profileRegistry == null)
			return null;
		return profileRegistry.getProfile(id);
	}

	/*
	 * Copied from ProvisioningHelper 1.57
	 */
	static IMetadataRepository getMetadataRepository(IProvisioningAgent agent, URI location) {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		if (manager == null)
			throw new IllegalStateException("No metadata repository manager found"); //$NON-NLS-1$
		try {
			return manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			return null;
		}
	}

	/*
	 * Copied from ProvisioningHelper 1.57
	 */
	static IQueryResult<IInstallableUnit> getInstallableUnits(IProvisioningAgent agent, URI location, IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
		IQueryable<IInstallableUnit> queryable = null;
		if (location == null) {
			queryable = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		} else {
			queryable = getMetadataRepository(agent, location);
		}
		if (queryable != null)
			return queryable.query(query, monitor);
		return Collector.emptyCollector();
	}

	private String getEnvironmentProperty() {
		Map<String, String> values = new HashMap<String, String>();
		if (os != null)
			values.put("osgi.os", os); //$NON-NLS-1$
		if (nl != null)
			values.put("osgi.nl", nl); //$NON-NLS-1$
		if (ws != null)
			values.put("osgi.ws", ws); //$NON-NLS-1$
		if (arch != null)
			values.put("osgi.arch", arch); //$NON-NLS-1$
		if (values.isEmpty())
			return null;
		return toString(values);
	}

	private IProfile initializeProfile() throws CoreException {
		if (profileId == null) {
			profileId = IProfileRegistry.SELF;
			noProfileId = true;
		}
		IProfile profile = getProfile(agent, profileId);
		if (profile == null) {
			if (destination == null)
				missingArgument("destination"); //$NON-NLS-1$
			if (flavor == null)
				flavor = System.getProperty("eclipse.p2.configurationFlavor", FLAVOR_DEFAULT); //$NON-NLS-1$

			Map<String, String> props = new HashMap<String, String>();
			props.put(IProfile.PROP_INSTALL_FOLDER, destination.toOSString());
			if (bundlePool == null || bundlePool.equals(Messages.destination_commandline))
				props.put(IProfile.PROP_CACHE, destination.toOSString());
			else
				props.put(IProfile.PROP_CACHE, bundlePool);
			if (roamingProfile)
				props.put(IProfile.PROP_ROAMING, Boolean.TRUE.toString());

			String env = getEnvironmentProperty();
			if (env != null)
				props.put(IProfile.PROP_ENVIRONMENTS, env);
			if (profileProperties != null) {
				putProperties(profileProperties, props);
			}
			profile = addProfile(agent, profileId, props);
		}
		return profile;
	}

	private void initializeRepositories(boolean throwException) throws CoreException {
		if (artifactRepositoryLocations == null) {
			if (throwException)
				missingArgument("artifactRepository"); //$NON-NLS-1$
		} else {
			artifactManager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
			if (artifactManager == null) {
				if (throwException)
					throw new ProvisionException(Messages.Application_NoManager);
			} else {
				int removalIdx = 0;
				boolean anyValid = false; // do we have any valid repos or did they all fail to load?
				artifactReposForRemoval = new URI[artifactRepositoryLocations.length];
				for (int i = 0; i < artifactRepositoryLocations.length; i++) {
					try {
						if (!artifactManager.contains(artifactRepositoryLocations[i])) {
							artifactManager.loadRepository(artifactRepositoryLocations[i], null);
							artifactReposForRemoval[removalIdx++] = artifactRepositoryLocations[i];
						}
						anyValid = true;
					} catch (ProvisionException e) {
						//one of the repositories did not load
						LogHelper.log(new Status(IStatus.ERROR, Activator.ID, artifactRepositoryLocations[i].toString() + " failed to load", e)); //$NON-NLS-1$
					}
				}
				if (throwException && !anyValid)
					//all repositories failed to load
					throw new ProvisionException(Messages.Application_NoRepositories);
			}
		}

		if (metadataRepositoryLocations == null) {
			if (throwException)
				missingArgument("metadataRepository"); //$NON-NLS-1$
		} else {
			metadataManager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
			if (metadataManager == null) {
				if (throwException)
					throw new ProvisionException(Messages.Application_NoManager);
			} else {
				int removalIdx = 0;
				boolean anyValid = false; // do we have any valid repos or did they all fail to load?
				metadataReposForRemoval = new URI[metadataRepositoryLocations.length];
				for (int i = 0; i < metadataRepositoryLocations.length; i++) {
					try {
						if (!metadataManager.contains(metadataRepositoryLocations[i])) {
							metadataManager.loadRepository(metadataRepositoryLocations[i], null);
							metadataReposForRemoval[removalIdx++] = metadataRepositoryLocations[i];
						}
						anyValid = true;
					} catch (ProvisionException e) {
						//one of the repositories did not load
						LogHelper.log(new Status(IStatus.ERROR, Activator.ID, metadataRepositoryLocations[i].toString() + " failed to load", e)); //$NON-NLS-1$
					}
				}
				if (throwException && !anyValid)
					//all repositories failed to load
					throw new ProvisionException(Messages.Application_NoRepositories);
			}
		}
	}

	private void initializeServices() throws ProvisionException {
		ServiceReference<IProvisioningAgentProvider> agentProviderRef = Activator.getContext().getServiceReference(IProvisioningAgentProvider.class);
		IProvisioningAgentProvider provider = Activator.getContext().getService(agentProviderRef);
		agent = provider.createAgent(null);

		IDirector director = (IDirector) agent.getService(IDirector.SERVICE_NAME);
		if (director == null)
			throw new RuntimeException(Messages.Missing_director);

		planner = (IPlanner) agent.getService(IPlanner.SERVICE_NAME);
		if (planner == null)
			throw new RuntimeException(Messages.Missing_planner);

		engine = (IEngine) agent.getService(IEngine.SERVICE_NAME);
		if (engine == null)
			throw new RuntimeException(Messages.Missing_Engine);
	}

	private void markRoots(IProfileChangeRequest request, IQueryResult<IInstallableUnit> roots) {
		for (Iterator<IInstallableUnit> iterator = roots.iterator(); iterator.hasNext();) {
			request.setInstallableUnitProfileProperty(iterator.next(), IProfile.PROP_PROFILE_ROOT_IU, Boolean.TRUE.toString());
		}
	}

	private void missingArgument(String argumentName) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.Missing_Required_Argument, argumentName)));
	}

	private IStatus planAndExecute(IProfile profile, ProvisioningContext context, ProfileChangeRequest request) {
		IProvisioningPlan result;
		IStatus operationStatus;
		result = planner.getProvisioningPlan(request, context, new NullProgressMonitor());
		if (!result.getStatus().isOK())
			operationStatus = result.getStatus();
		else {
			operationStatus = PlanExecutionHelper.executePlan(result, engine, context, new NullProgressMonitor());
		}
		return operationStatus;
	}

	private void printRequest(ProfileChangeRequest request) {
		Collection<IInstallableUnit> toAdd = request.getAdditions();
		Collection<IInstallableUnit> toRemove = request.getRemovals();
		for (IInstallableUnit added : toAdd) {
			System.out.println(NLS.bind(Messages.Installing, added.getId(), added.getVersion()));
		}
		for (IInstallableUnit removed : toRemove) {
			System.out.println(NLS.bind(Messages.Uninstalling, removed.getId(), removed.getVersion()));
		}
	}

	public void processArguments(String[] args) throws Exception {
		if (args == null)
			return;
		for (int i = 0; i < args.length; i++) {

			String opt = args[i];
			if (opt.equals("-roaming")) { //$NON-NLS-1$
				roamingProfile = true;
			}

			if (opt.equals(COMMAND_NAMES[COMMAND_LIST])) {
				if (command != -1)
					ambigousCommand(COMMAND_LIST, command);
				command = COMMAND_LIST;
			}

			// check for args without parameters (i.e., a flag arg)

			// check for args with parameters. If we are at the last
			// argument or
			// if the next one
			// has a '-' as the first character, then we can't have an arg
			// with
			// a parm so continue.
			if (i == args.length - 1 || args[i + 1].startsWith("-")) //$NON-NLS-1$
				continue;

			String arg = args[++i];

			if (opt.equalsIgnoreCase("-profile")) //$NON-NLS-1$
				profileId = arg;

			if (opt.equalsIgnoreCase("-profileProperties") || opt.equalsIgnoreCase("-props")) //$NON-NLS-1$ //$NON-NLS-2$
				profileProperties = arg;

			// we create a path object here to handle ../ entries in the middle of paths
			if (opt.equalsIgnoreCase("-destination") || opt.equalsIgnoreCase("-dest")) { //$NON-NLS-1$ //$NON-NLS-2$
				if (arg.startsWith("file:")) //$NON-NLS-1$
					arg = arg.substring(5);
				destination = new Path(arg);
			}

			// we create a path object here to handle ../ entries in the middle of paths
			if (opt.equalsIgnoreCase("-bundlepool") || opt.equalsIgnoreCase("-bp")) { //$NON-NLS-1$ //$NON-NLS-2$
				if (arg.startsWith("file:")) //$NON-NLS-1$
					arg = arg.substring(5);
				bundlePool = new Path(arg).toOSString();
			}

			if (opt.equalsIgnoreCase("-metadataRepository") || opt.equalsIgnoreCase("-metadataRepositories") || opt.equalsIgnoreCase("-mr")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				metadataRepositoryLocations = getURIs(arg);

			if (opt.equalsIgnoreCase("-artifactRepository") || opt.equalsIgnoreCase("-artifactRepositories") || opt.equalsIgnoreCase("-ar")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				artifactRepositoryLocations = getURIs(arg);

			if (opt.equalsIgnoreCase("-flavor")) //$NON-NLS-1$
				flavor = arg;

			if (opt.equalsIgnoreCase(COMMAND_NAMES[COMMAND_INSTALL])) {
				if (command != -1)
					ambigousCommand(COMMAND_INSTALL, command);
				root = arg;
				command = COMMAND_INSTALL;
			}

			if (opt.equalsIgnoreCase("-version")) { //$NON-NLS-1$
				if (arg != null && !arg.startsWith(ANT_PROPERTY_PREFIX))
					version = Version.create(arg);
			}

			if (opt.equalsIgnoreCase(COMMAND_NAMES[COMMAND_UNINSTALL])) {
				if (command != -1)
					ambigousCommand(COMMAND_UNINSTALL, command);
				root = arg;
				command = COMMAND_UNINSTALL;
			}

			if (opt.equalsIgnoreCase("-p2.os")) { //$NON-NLS-1$
				os = arg;
			}
			if (opt.equalsIgnoreCase("-p2.ws")) { //$NON-NLS-1$
				ws = arg;
			}
			if (opt.equalsIgnoreCase("-p2.nl")) { //$NON-NLS-1$
				nl = arg;
			}
			if (opt.equalsIgnoreCase("-p2.arch")) { //$NON-NLS-1$
				arch = arg;
			}
		}

	}

	/**
	 * @param pairs	a comma separated list of tag=value pairs
	 * @param properties the collection into which the pairs are put
	 */
	private void putProperties(String pairs, Map<String, String> properties) {
		StringTokenizer tok = new StringTokenizer(pairs, ",", true); //$NON-NLS-1$
		while (tok.hasMoreTokens()) {
			String next = tok.nextToken().trim();
			int i = next.indexOf('=');
			if (i > 0 && i < next.length() - 1) {
				String tag = next.substring(0, i).trim();
				String value = next.substring(i + 1, next.length()).trim();
				if (tag.length() > 0 && value.length() > 0) {
					properties.put(tag, value);
				}
			}
		}
	}

	public Object run(String[] args) throws Exception {
		System.out.println(Messages.Deprecated_application);
		long time = -System.currentTimeMillis();
		initializeServices();
		processArguments(args);

		IStatus operationStatus = Status.OK_STATUS;
		IQuery<IInstallableUnit> query;
		IQueryResult<IInstallableUnit> roots;
		try {
			initializeRepositories(command == COMMAND_INSTALL);
			switch (command) {
				case COMMAND_INSTALL :
				case COMMAND_UNINSTALL :

					IProfile profile = initializeProfile();
					query = QueryUtil.createIUQuery(root, version == null ? VersionRange.emptyRange : new VersionRange(version, true, version, true));
					roots = collectRootIUs(metadataRepositoryLocations, QueryUtil.createLatestQuery(query));
					if (roots.isEmpty())
						roots = profile.query(query, new NullProgressMonitor());
					if (roots.isEmpty()) {
						System.out.println(NLS.bind(Messages.Missing_IU, root));
						logFailure(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.Missing_IU, root)));
						return EXIT_ERROR;
					}
					// keep this result status in case there is a problem so we can report it to the user
					boolean wasRoaming = Boolean.valueOf(profile.getProperty(IProfile.PROP_ROAMING)).booleanValue();
					try {
						IStatus updateRoamStatus = updateRoamingProperties(profile);
						if (!updateRoamStatus.isOK()) {
							MultiStatus multi = new MultiStatus(Activator.ID, IStatus.ERROR, NLS.bind(Messages.Cant_change_roaming, profile.getProfileId()), null);
							multi.add(updateRoamStatus);
							System.out.println(multi.getMessage());
							System.out.println(updateRoamStatus.getMessage());
							logFailure(multi);
							return EXIT_ERROR;
						}
						ProvisioningContext context = new ProvisioningContext(agent);
						context.setMetadataRepositories(metadataRepositoryLocations);
						context.setArtifactRepositories(artifactRepositoryLocations);
						ProfileChangeRequest request = buildProvisioningRequest(profile, roots, command == COMMAND_INSTALL);
						printRequest(request);
						operationStatus = planAndExecute(profile, context, request);
					} finally {
						// if we were originally were set to be roaming and we changed it, change it back before we return
						if (wasRoaming && !Boolean.valueOf(profile.getProperty(IProfile.PROP_ROAMING)).booleanValue())
							setRoaming(profile);
					}
					break;
				case COMMAND_LIST :
					query = QueryUtil.createIUQuery(null, VersionRange.emptyRange);
					if (metadataRepositoryLocations == null)
						missingArgument("metadataRepository"); //$NON-NLS-1$

					roots = collectRootIUs(metadataRepositoryLocations, query);
					Iterator<IInstallableUnit> unitIterator = roots.iterator();
					while (unitIterator.hasNext()) {
						IInstallableUnit iu = unitIterator.next();
						System.out.println(iu.getId());
					}
					break;
			}
		} finally {
			cleanupRepositories();
		}

		time += System.currentTimeMillis();
		if (operationStatus.isOK())
			System.out.println(NLS.bind(Messages.Operation_complete, new Long(time)));
		else {
			System.out.println(Messages.Operation_failed);
			logFailure(operationStatus);
			return EXIT_ERROR;
		}
		return IApplication.EXIT_OK;
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

	class LocationQueryable implements IQueryable<IInstallableUnit> {
		private URI location;

		public LocationQueryable(URI location) {
			this.location = location;
		}

		public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
			return getInstallableUnits(agent, location, query, monitor);
		}
	}

	private IQueryResult<IInstallableUnit> collectRootIUs(URI[] locations, IQuery<IInstallableUnit> query) {
		IProgressMonitor nullMonitor = new NullProgressMonitor();

		if (locations == null || locations.length == 0)
			return getInstallableUnits(agent, (URI) null, query, nullMonitor);

		List<IQueryable<IInstallableUnit>> locationQueryables = new ArrayList<IQueryable<IInstallableUnit>>(locations.length);
		for (int i = 0; i < locations.length; i++)
			locationQueryables.add(new LocationQueryable(locations[i]));
		return QueryUtil.compoundQueryable(locationQueryables).query(query, nullMonitor);
	}

	public Object start(IApplicationContext context) throws Exception {
		return run((String[]) context.getArguments().get("application.args")); //$NON-NLS-1$
	}

	public void stop() {
		//nothing to do
	}

	private String toString(Map<String, String> context) {
		StringBuffer result = new StringBuffer();
		boolean first = true;
		for (String key : context.keySet()) {
			if (first)
				first = false;
			else
				result.append(',');
			result.append(key);
			result.append('=');
			result.append(context.get(key));
		}
		return result.toString();
	}

	private IStatus updateRoamingProperties(IProfile profile) {
		// if the user didn't specify a destination path on the command-line
		// then we assume they are installing into the currently running
		// instance and we don't have anything to update
		if (destination == null)
			return Status.OK_STATUS;

		// if the user didn't set a profile id on the command-line this is ok if they
		// also didn't set the destination path. (handled in the case above) otherwise throw an error.
		if (noProfileId) // && destination != null
			return new Status(IStatus.ERROR, Activator.ID, Messages.Missing_profileid);

		// make sure that we are set to be roaming before we update the values
		if (!Boolean.valueOf(profile.getProperty(IProfile.PROP_ROAMING)).booleanValue())
			return Status.OK_STATUS;

		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		File destinationFile = destination.toFile();
		if (!destinationFile.equals(new File(profile.getProperty(IProfile.PROP_INSTALL_FOLDER))))
			request.setProfileProperty(IProfile.PROP_INSTALL_FOLDER, destination.toOSString());
		if (!destinationFile.equals(new File(profile.getProperty(IProfile.PROP_CACHE))))
			request.setProfileProperty(IProfile.PROP_CACHE, destination.toOSString());
		if (request.getProfileProperties().size() == 0)
			return Status.OK_STATUS;

		// otherwise we have to make a change so set the profile to be non-roaming so the 
		// values don't get recalculated to the wrong thing if we are flushed from memory - we
		// will set it back later (see bug 269468)
		request.setProfileProperty(IProfile.PROP_ROAMING, "false"); //$NON-NLS-1$

		ProvisioningContext context = new ProvisioningContext(agent);
		context.setMetadataRepositories(new URI[0]);
		context.setArtifactRepositories(new URI[0]);
		IProvisioningPlan result = planner.getProvisioningPlan(request, context, new NullProgressMonitor());
		return PlanExecutionHelper.executePlan(result, engine, context, new NullProgressMonitor());
	}

	/*
	 * Set the roaming property on the given profile.
	 */
	private IStatus setRoaming(IProfile profile) {
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.setProfileProperty(IProfile.PROP_ROAMING, "true"); //$NON-NLS-1$
		ProvisioningContext context = new ProvisioningContext(agent);
		context.setMetadataRepositories(new URI[0]);
		context.setArtifactRepositories(new URI[0]);
		IProvisioningPlan result = planner.getProvisioningPlan(request, context, new NullProgressMonitor());
		return PlanExecutionHelper.executePlan(result, engine, context, new NullProgressMonitor());
	}

	private static URI[] getURIs(String spec) {
		if (spec == null)
			return null;
		String[] urlSpecs = getArrayFromString(spec, ","); //$NON-NLS-1$
		ArrayList<URI> result = new ArrayList<URI>(urlSpecs.length);
		for (int i = 0; i < urlSpecs.length; i++) {
			try {
				result.add(URIUtil.fromString(urlSpecs[i]));
			} catch (URISyntaxException e) {
				LogHelper.log(new Status(IStatus.WARNING, Activator.ID, NLS.bind(Messages.Ignored_repo, urlSpecs[i])));
			}
		}
		if (result.size() == 0)
			return null;
		return result.toArray(new URI[result.size()]);
	}

	/**
	 * Convert a list of tokens into an array. The list separator has to be
	 * specified.
	 */
	public static String[] getArrayFromString(String list, String separator) {
		if (list == null || list.trim().equals("")) //$NON-NLS-1$
			return new String[0];
		List<String> result = new ArrayList<String>();
		for (StringTokenizer tokens = new StringTokenizer(list, separator); tokens.hasMoreTokens();) {
			String token = tokens.nextToken().trim();
			if (!token.equals("")) //$NON-NLS-1$
				result.add(token);
		}
		return result.toArray(new String[result.size()]);
	}

	private void logFailure(IStatus status) {
		FrameworkLog log = (FrameworkLog) ServiceHelper.getService(Activator.getContext(), FrameworkLog.class.getName());
		if (log != null)
			System.err.println("Application failed, log file location: " + log.getFile()); //$NON-NLS-1$
		LogHelper.log(status);
	}
}
