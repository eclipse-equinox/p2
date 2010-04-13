/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM Corporation - initial implementation and ideas
 *     Sonatype, Inc. - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.reconciler.dropins;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.internal.p2.extensionlocation.Constants;
import org.eclipse.equinox.internal.provisional.configurator.Configurator;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.RepositoryListener;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.query.IUProfilePropertyQuery;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.ProfileInclusionRules;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Synchronizes a profile with a set of repositories.
 */
public class ProfileSynchronizer {
	private static final String RECONCILER_APPLICATION_ID = "org.eclipse.equinox.p2.reconciler.application"; //$NON-NLS-1$
	private static final String TIMESTAMPS_FILE_PREFIX = "timestamps"; //$NON-NLS-1$
	private static final String PROFILE_TIMESTAMP = "PROFILE"; //$NON-NLS-1$
	private static final String NO_TIMESTAMP = "-1"; //$NON-NLS-1$
	private static final String PROP_FROM_DROPINS = "org.eclipse.equinox.p2.reconciler.dropins"; //$NON-NLS-1$

	private static final String CACHE_EXTENSIONS = "org.eclipse.equinox.p2.cache.extensions"; //$NON-NLS-1$
	private static final String PIPE = "|"; //$NON-NLS-1$
	private static final String EXPLANATION = "org.eclipse.equinox.p2.director.explain"; //$NON-NLS-1$
	final IProfile profile;

	final Map<String, IMetadataRepository> repositoryMap;
	private Map<String, String> timestamps;
	private final IProvisioningAgent agent;

	static class ReconcilerProfileChangeRequest extends ProfileChangeRequest {
		boolean isMove = false;

		public ReconcilerProfileChangeRequest(IProfile profile, boolean isMove) {
			super(profile);
			this.isMove = isMove;
		}
	}

	/*
	 * Constructor for the class.
	 */
	public ProfileSynchronizer(IProvisioningAgent agent, IProfile profile, Collection<IMetadataRepository> repositories) {
		this.agent = agent;
		this.profile = profile;
		this.repositoryMap = new HashMap<String, IMetadataRepository>();
		for (IMetadataRepository repository : repositories) {
			repositoryMap.put(repository.getLocation().toString(), repository);
		}
	}

	/*
	 * Synchronize the profile with the list of metadata repositories.
	 */
	public IStatus synchronize(IProgressMonitor monitor) {
		readTimestamps();
		if (isUpToDate())
			return Status.OK_STATUS;

		ProvisioningContext context = getContext();
		context.setProperty(EXPLANATION, new Boolean(Tracing.DEBUG_RECONCILER).toString());

		boolean done = false;
		while (!done) {
			// figure out if we really have anything to install/uninstall
			ReconcilerProfileChangeRequest request = createProfileChangeRequest(context);
			String updatedCacheExtensions = synchronizeCacheExtensions();
			if (request == null) {
				if (updatedCacheExtensions != null) {
					IStatus engineResult = setProperty(CACHE_EXTENSIONS, updatedCacheExtensions, context, null);
					if (engineResult.getSeverity() != IStatus.ERROR && engineResult.getSeverity() != IStatus.CANCEL)
						writeTimestamps();
					return engineResult;
				}
				return Status.OK_STATUS;
			}
			if (updatedCacheExtensions != null)
				request.setProfileProperty(CACHE_EXTENSIONS, updatedCacheExtensions);

			SubMonitor sub = SubMonitor.convert(monitor, 100);
			try {
				//create the provisioning plan
				IProvisioningPlan plan = createProvisioningPlan(request, context, sub.newChild(50));
				IStatus status = plan.getStatus();
				if (status.getSeverity() == IStatus.ERROR || status.getSeverity() == IStatus.CANCEL)
					return status;
				debug(request, plan);

				// if there is no work to do then just write out the timestamps and return.
				if (plan.getAdditions().query(QueryUtil.createIUAnyQuery(), null).isEmpty() && plan.getRemovals().query(QueryUtil.createIUAnyQuery(), null).isEmpty()) {
					writeTimestamps();
					return status;
				}

				//invoke the engine to perform installs/uninstalls
				IStatus engineResult = executePlan(plan, context, sub.newChild(50));
				if (engineResult.getSeverity() == IStatus.ERROR || engineResult.getSeverity() == IStatus.CANCEL)
					return engineResult;

			} finally {
				done = !request.isMove;
				sub.done();
			}
		}
		// write out the new timestamps (for caching) and apply the configuration
		writeTimestamps();
		return applyConfiguration(false);
	}

	private void writeTimestamps() {
		timestamps.clear();
		timestamps.put(PROFILE_TIMESTAMP, Long.toString(profile.getTimestamp()));
		for (Entry<String, IMetadataRepository> entry : repositoryMap.entrySet()) {
			IMetadataRepository repository = entry.getValue();
			Map<String, String> props = repository.getProperties();
			String timestamp = null;
			if (props != null)
				timestamp = props.get(IRepository.PROP_TIMESTAMP);

			if (timestamp == null)
				timestamp = NO_TIMESTAMP;

			timestamps.put(entry.getKey(), timestamp);
		}

		try {
			File file = Activator.getContext().getDataFile(TIMESTAMPS_FILE_PREFIX + profile.getProfileId().hashCode());
			OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
			try {
				CollectionUtils.storeProperties(timestamps, os, "Timestamps for " + profile.getProfileId()); //$NON-NLS-1$
			} finally {
				if (os != null)
					os.close();
			}
		} catch (FileNotFoundException e) {
			//Ignore
		} catch (IOException e) {
			//Ignore
		}
	}

	private boolean isUpToDate() {
		// the user might want to force a reconciliation
		if ("true".equals(Activator.getContext().getProperty("osgi.checkConfiguration"))) //$NON-NLS-1$//$NON-NLS-2$
			return false;

		String lastKnownProfileTimeStamp = timestamps.remove(PROFILE_TIMESTAMP);
		if (lastKnownProfileTimeStamp == null)
			return false;
		if (!lastKnownProfileTimeStamp.equals(Long.toString(profile.getTimestamp())))
			return false;

		//When we get here the timestamps map only contains information related to repos
		for (Entry<String, IMetadataRepository> entry : repositoryMap.entrySet()) {
			IMetadataRepository repository = entry.getValue();

			Map<String, String> props = repository.getProperties();
			String currentTimestamp = null;
			if (props != null)
				currentTimestamp = props.get(IRepository.PROP_TIMESTAMP);

			if (currentTimestamp == null)
				currentTimestamp = NO_TIMESTAMP;

			String lastKnownTimestamp = timestamps.remove(entry.getKey());
			//A repo has been added 
			if (lastKnownTimestamp == null)
				return false;
			if (!lastKnownTimestamp.equals(currentTimestamp)) {
				return false;
			}
		}
		//A repo has been removed
		if (timestamps.size() != 0)
			return false;

		return true;
	}

	private void readTimestamps() {
		File file = Activator.getContext().getDataFile(TIMESTAMPS_FILE_PREFIX + profile.getProfileId().hashCode());
		try {
			InputStream is = new BufferedInputStream(new FileInputStream(file));
			try {
				timestamps = CollectionUtils.loadProperties(is);
			} finally {
				if (is != null)
					is.close();
			}
		} catch (FileNotFoundException e) {
			timestamps = new HashMap<String, String>();
			//Ignore
		} catch (IOException e) {
			//Ignore
			timestamps = new HashMap<String, String>();
		}
	}

	private ProvisioningContext getContext() {
		ArrayList<URI> repoURLs = new ArrayList<URI>();
		for (Iterator<String> iterator = repositoryMap.keySet().iterator(); iterator.hasNext();) {
			try {
				repoURLs.add(new URI(iterator.next()));
			} catch (URISyntaxException e) {
				//ignore
			}
		}
		ProvisioningContext result = new ProvisioningContext(agent);
		result.setMetadataRepositories(repoURLs.toArray(new URI[repoURLs.size()]));
		result.setArtifactRepositories(new URI[0]);
		return result;
	}

	private String synchronizeCacheExtensions() {
		List<String> currentExtensions = new ArrayList<String>();
		StringBuffer buffer = new StringBuffer();

		List<String> repositories = new ArrayList<String>(repositoryMap.keySet());
		final String OSGiInstallArea = Activator.getOSGiInstallArea().toExternalForm() + Constants.EXTENSION_LOCATION;
		Collections.sort(repositories, new Comparator<String>() {
			public int compare(String left, String right) {
				if (OSGiInstallArea.equals(left))
					return -1;
				if (OSGiInstallArea.equals(right))
					return 1;
				return left.compareTo(right);
			}
		});
		for (Iterator<String> it = repositories.iterator(); it.hasNext();) {
			String repositoryId = it.next();
			try {
				IArtifactRepository repository = Activator.loadArtifactRepository(new URI(repositoryId), null);

				if (repository instanceof IFileArtifactRepository) {
					currentExtensions.add(escapePipe(repositoryId));
					buffer.append(repositoryId);
					if (it.hasNext())
						buffer.append(PIPE);
				}
			} catch (ProvisionException e) {
				// ignore
			} catch (URISyntaxException e) {
				// unexpected
				e.printStackTrace();
			}
		}
		String currentExtensionsProperty = (buffer.length() == 0) ? null : buffer.toString();

		List<String> previousExtensions = new ArrayList<String>();
		String previousExtensionsProperty = profile.getProperty(CACHE_EXTENSIONS);
		if (previousExtensionsProperty != null) {
			StringTokenizer tokenizer = new StringTokenizer(previousExtensionsProperty, PIPE);
			while (tokenizer.hasMoreTokens()) {
				previousExtensions.add(tokenizer.nextToken());
			}
		}

		if (previousExtensions.size() == currentExtensions.size() && previousExtensions.containsAll(currentExtensions))
			return null;

		return currentExtensionsProperty;
	}

	/**
	 * Escapes the pipe ('|') character in a URI using the standard URI escape sequence.
	 * This is done because the pipe character is used as the delimiter between locations
	 * in the cache extensions profile property.
	 */
	private String escapePipe(String location) {
		String result = location;
		int pipeIndex;
		while ((pipeIndex = result.indexOf(',')) != -1)
			result = result.substring(0, pipeIndex) + "%7C" + result.substring(pipeIndex + 1); //$NON-NLS-1$
		return result;
	}

	/*
	 * Return a map of all the IUs in the profile
	 * Use a map here so we have a copy of the original IU from the profile... we will need it later.
	 */
	private Map<IInstallableUnit, IInstallableUnit> getProfileIUs() {
		IQueryResult<IInstallableUnit> profileQueryResult = profile.query(QueryUtil.createIUAnyQuery(), null);
		Map<IInstallableUnit, IInstallableUnit> result = new HashMap<IInstallableUnit, IInstallableUnit>();
		for (Iterator<IInstallableUnit> it = profileQueryResult.iterator(); it.hasNext();) {
			IInstallableUnit iu = it.next();
			result.put(iu, iu);
		}
		return result;
	}

	/*
	 * Return a map of all the IUs available in the profile. This takes the shared parents into consideration, if applicable.
	 * Use a map here so we have a copy of the original IU from the profile... we will need it later.
	 */
	private Map<IInstallableUnit, IInstallableUnit> getAvailableProfileIUs() {
		IQueryResult<IInstallableUnit> profileQueryResult = profile.available(QueryUtil.createIUAnyQuery(), null);
		Map<IInstallableUnit, IInstallableUnit> result = new HashMap<IInstallableUnit, IInstallableUnit>();
		for (Iterator<IInstallableUnit> it = profileQueryResult.iterator(); it.hasNext();) {
			IInstallableUnit iu = it.next();
			result.put(iu, iu);
		}
		return result;
	}

	/*
	 * Return the profile change requests that we need to execute in order to install everything from the
	 * dropins folder(s). (or uninstall things that have been removed) We use a collection here because if
	 * the user has moved bundles from the dropins to the plugins (for instance) then we need to uninstall
	 * the old bundle and then re-install the new one. This is because the IUs for the moved bundles are
	 * considered the same but they really differ in an IU property. (file location, which is not considered
	 * as part of equality)
	 */
	public ReconcilerProfileChangeRequest createProfileChangeRequest(ProvisioningContext context) {
		ReconcilerProfileChangeRequest request = new ReconcilerProfileChangeRequest(profile, false);

		boolean resolve = Boolean.valueOf(profile.getProperty("org.eclipse.equinox.p2.resolve")).booleanValue(); //$NON-NLS-1$
		if (resolve)
			request.removeProfileProperty("org.eclipse.equinox.p2.resolve"); //$NON-NLS-1$

		List<IInstallableUnit> toAdd = new ArrayList<IInstallableUnit>();
		List<IInstallableUnit> toRemove = new ArrayList<IInstallableUnit>();
		List<IInstallableUnit> toMove = new ArrayList<IInstallableUnit>();

		boolean foundIUsToAdd = false;
		Map<IInstallableUnit, IInstallableUnit> profileIUs = getProfileIUs();

		// we use IProfile.available(...) here so that we also gather any shared IUs
		Map<IInstallableUnit, IInstallableUnit> availableProfileIUs = getAvailableProfileIUs();

		// get all IUs from all our repos (toAdd)
		IQueryResult<IInstallableUnit> allIUs = getAllIUsFromRepos();
		for (Iterator<IInstallableUnit> iter = allIUs.iterator(); iter.hasNext();) {
			final IInstallableUnit iu = iter.next();
			IInstallableUnit existing = profileIUs.get(iu);
			// check to see if it is really the same IU
			if (existing == null) {
				if (QueryUtil.isGroup(iu))
					request.setInstallableUnitProfileProperty(iu, IProfile.PROP_PROFILE_ROOT_IU, Boolean.TRUE.toString());
				// mark all IUs with special property
				request.setInstallableUnitProfileProperty(iu, PROP_FROM_DROPINS, Boolean.TRUE.toString());
				request.setInstallableUnitInclusionRules(iu, ProfileInclusionRules.createOptionalInclusionRule(iu));
				request.setInstallableUnitProfileProperty(iu, IProfile.PROP_PROFILE_LOCKED_IU, Integer.toString(IProfile.LOCK_UNINSTALL));
				toAdd.add(iu);

				// as soon as we find something locally that needs to be installed, then 
				// everything from the parent's dropins must be installed locally as well.
				if (!foundIUsToAdd && availableProfileIUs.get(iu) == null) {
					foundIUsToAdd = true;
				}
			} else {
				// if the IU is already installed in the profile then check to see if it was moved.
				String one = iu.getProperty(RepositoryListener.FILE_NAME);
				String two = existing.getProperty(RepositoryListener.FILE_NAME);
				// if we have an IU which has been moved, keep track of it.
				//
				// cheat here... since we always set the filename property for bundles in the dropins,
				// if the existing IU's filename is null then it isn't from the dropins. a better
				// (and more expensive) way to find this out is to do an IU profile property query.
				if (one != null && two != null && !one.equals(two))
					toMove.add(iu);
			}
		}

		// get all IUs from profile with marked property (existing)
		IQueryResult<IInstallableUnit> dropinIUs = profile.query(new IUProfilePropertyQuery(PROP_FROM_DROPINS, Boolean.TRUE.toString()), null);
		Set<IInstallableUnit> all = allIUs.toUnmodifiableSet();
		for (Iterator<IInstallableUnit> iter = dropinIUs.iterator(); iter.hasNext();) {
			IInstallableUnit iu = iter.next();
			// the STRICT policy is set when we install things via the UI, we use it to differentiate between IUs installed
			// via the dropins and the UI. (dropins are considered optional) If an IU has both properties set it means that
			// it was initially installed via the dropins but then upgraded via the UI. (properties are copied from the old IU
			// to the new IU during an upgrade) In this case we want to remove the "from dropins" property so the upgrade
			// will stick.
			if ("STRICT".equals(profile.getInstallableUnitProperty(iu, "org.eclipse.equinox.p2.internal.inclusion.rules"))) { //$NON-NLS-1$//$NON-NLS-2$
				request.removeInstallableUnitProfileProperty(iu, PROP_FROM_DROPINS);
				request.removeInstallableUnitProfileProperty(iu, IProfile.PROP_PROFILE_LOCKED_IU);
				continue;
			}
			// if the IU from the profile is in the "all available" list, then it is already added
			// otherwise if it isn't in the repo then we have to remove it from the profile.
			if (all.contains(iu))
				toAdd.remove(iu);
			else
				toRemove.add(iu);
		}

		if (!foundIUsToAdd && toRemove.isEmpty() && !resolve && toMove.isEmpty()) {
			if (Tracing.DEBUG_RECONCILER)
				Tracing.debug("[reconciler] Nothing to do."); //$NON-NLS-1$
			return null;
		}

		// if we have just a regular add/remove then set up the change request as per normal
		if (toMove.isEmpty()) {
			context.setExtraInstallableUnits(toAdd);
			request.addAll(toAdd);
			request.removeAll(toRemove);
		} else {
			// if we had some bundles which moved locations then we need to create a move change request
			// and remove the moved bundles first. The caller of this method will take care of calling us again
			// to re-add the bundles at their new location (and other bundles which need adding)
			request = new ReconcilerProfileChangeRequest(profile, true);
			request.removeAll(toMove);
		}

		// force removal of all moved and removed IUs, which will also remove anything which depends on them
		// see: bug 306424#c6 and bug 308934.
		Collection<IRequirement> extraReqs = new ArrayList<IRequirement>();
		for (IInstallableUnit unit : request.getRemovals()) {
			IRequirement negation = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, unit.getId(), //
					new VersionRange(unit.getVersion(), true, unit.getVersion(), true), null, 0, 0, false);
			extraReqs.add(negation);
		}
		request.addExtraRequirements(extraReqs);
		debug(request);
		return request;
	}

	private void debug(ProfileChangeRequest request, IProvisioningPlan plan) {
		if (!Tracing.DEBUG_RECONCILER)
			return;
		final String PREFIX = "[reconciler] [plan] "; //$NON-NLS-1$
		// get the request
		List<IInstallableUnit> toAdd = new ArrayList<IInstallableUnit>(request.getAdditions());
		List<IInstallableUnit> toRemove = new ArrayList<IInstallableUnit>(request.getRemovals());
		// remove from the request everything that is in the plan

		for (Iterator<IInstallableUnit> iterator = plan.getRemovals().query(QueryUtil.createIUAnyQuery(), null).iterator(); iterator.hasNext();) {
			IInstallableUnit iu = iterator.next();
			toRemove.remove(iu);
		}
		for (Iterator<IInstallableUnit> iterator = plan.getAdditions().query(QueryUtil.createIUAnyQuery(), null).iterator(); iterator.hasNext();) {
			IInstallableUnit iu = iterator.next();
			toAdd.remove(iu);
		}

		// if anything is left in the request, then something is wrong with the plan
		if (toAdd.size() == 0 && toRemove.size() == 0)
			Tracing.debug(PREFIX + "Plan matches the request."); //$NON-NLS-1$
		if (toAdd.size() != 0) {
			Tracing.debug(PREFIX + "Some units will not be installed, because they are already installed or there are dependency issues:"); //$NON-NLS-1$
			for (IInstallableUnit unit : toAdd)
				Tracing.debug(PREFIX + unit);
		}
		if (toRemove.size() != 0) {
			Tracing.debug(PREFIX + "Some units will not be uninstalled:"); //$NON-NLS-1$
			for (IInstallableUnit unit : toRemove)
				Tracing.debug(PREFIX + unit);
		}
	}

	/*
	 * If debugging is turned on, then print out the details for the given profile change request.
	 */
	private void debug(ProfileChangeRequest request) {
		if (!Tracing.DEBUG_RECONCILER)
			return;
		final String PREFIX = "[reconciler] "; //$NON-NLS-1$
		Collection<IInstallableUnit> toAdd = request.getAdditions();
		if (toAdd == null || toAdd.size() == 0) {
			Tracing.debug(PREFIX + "No installable units to add."); //$NON-NLS-1$
		} else {
			for (IInstallableUnit add : toAdd) {
				Tracing.debug(PREFIX + "Adding IU: " + add.getId() + ' ' + add.getVersion()); //$NON-NLS-1$
			}
		}
		Map<IInstallableUnit, Map<String, String>> propsToAdd = request.getInstallableUnitProfilePropertiesToAdd();
		if (propsToAdd == null || propsToAdd.isEmpty()) {
			Tracing.debug(PREFIX + "No IU properties to add."); //$NON-NLS-1$
		} else {
			for (Entry<IInstallableUnit, Map<String, String>> entry : propsToAdd.entrySet()) {
				Tracing.debug(PREFIX + "Adding IU property: " + entry.getKey() + "->" + entry.getValue()); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		Collection<IInstallableUnit> toRemove = request.getRemovals();
		if (toRemove == null || toRemove.size() == 0) {
			Tracing.debug(PREFIX + "No installable units to remove."); //$NON-NLS-1$
		} else {
			for (IInstallableUnit remove : toRemove) {
				Tracing.debug(PREFIX + "Removing IU: " + remove.getId() + ' ' + remove.getVersion()); //$NON-NLS-1$
			}
		}
		Map<IInstallableUnit, List<String>> propsToRemove = request.getInstallableUnitProfilePropertiesToRemove();
		if (propsToRemove == null || propsToRemove.isEmpty()) {
			Tracing.debug(PREFIX + "No IU properties to remove."); //$NON-NLS-1$
		} else {
			for (Entry<IInstallableUnit, List<String>> entry : propsToRemove.entrySet()) {
				Tracing.debug(PREFIX + "Removing IU property: " + entry.getKey() + "->" + entry.getValue()); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		Collection<IRequirement> extra = request.getExtraRequirements();
		if (extra == null || extra.isEmpty()) {
			Tracing.debug(PREFIX + "No extra requirements."); //$NON-NLS-1$
		} else {
			for (IRequirement requirement : extra)
				Tracing.debug(PREFIX + "Extra requirement: " + requirement); //$NON-NLS-1$
		}
	}

	/*
	 * Return all of the IUs available in all of our repos. This usually includes the dropins and plugins folders
	 * as well as any sites specified in the platform.xml file.
	 */
	private IQueryResult<IInstallableUnit> getAllIUsFromRepos() {
		// TODO: Should consider using a sequenced iterator here instead of collecting
		Collector<IInstallableUnit> allRepos = new Collector<IInstallableUnit>();
		for (IMetadataRepository repository : repositoryMap.values()) {
			allRepos.addAll(repository.query(QueryUtil.createIUAnyQuery(), null));
		}
		return allRepos;
	}

	private IProvisioningPlan createProvisioningPlan(ProfileChangeRequest request, ProvisioningContext provisioningContext, IProgressMonitor monitor) {
		IPlanner planner = (IPlanner) agent.getService(IPlanner.SERVICE_NAME);
		return planner.getProvisioningPlan(request, provisioningContext, monitor);
	}

	private IStatus setProperty(String key, String value, ProvisioningContext provisioningContext, IProgressMonitor monitor) {
		IEngine engine = (IEngine) agent.getService(IEngine.SERVICE_NAME);
		IProvisioningPlan plan = engine.createPlan(profile, provisioningContext);
		plan.setProfileProperty(key, value);
		IPhaseSet phaseSet = PhaseSetFactory.createPhaseSetIncluding(new String[] {PhaseSetFactory.PHASE_PROPERTY});
		return engine.perform(plan, phaseSet, monitor);
	}

	private IStatus executePlan(IProvisioningPlan plan, ProvisioningContext provisioningContext, IProgressMonitor monitor) {
		IEngine engine = (IEngine) agent.getService(IEngine.SERVICE_NAME);
		IPhaseSet phaseSet = PhaseSetFactory.createDefaultPhaseSetExcluding(new String[] {PhaseSetFactory.PHASE_COLLECT, PhaseSetFactory.PHASE_CHECK_TRUST});

		if (plan.getInstallerPlan() != null) {
			IStatus installerPlanStatus = engine.perform(plan.getInstallerPlan(), phaseSet, monitor);
			if (!installerPlanStatus.isOK())
				return installerPlanStatus;

			applyConfiguration(true);
		}
		return engine.perform(plan, phaseSet, monitor);
	}

	/*
	 * Write out the configuration file.
	 */
	private IStatus applyConfiguration(boolean isInstaller) {
		if (!isInstaller && isReconciliationApplicationRunning())
			return Status.OK_STATUS;
		BundleContext context = Activator.getContext();
		ServiceReference reference = context.getServiceReference(Configurator.class.getName());
		Configurator configurator = (Configurator) context.getService(reference);
		try {
			configurator.applyConfiguration();
		} catch (IOException e) {
			return new Status(IStatus.ERROR, Activator.ID, "Unexpected failure applying configuration", e); //$NON-NLS-1$
		} finally {
			context.ungetService(reference);
		}
		return Status.OK_STATUS;
	}

	static boolean isReconciliationApplicationRunning() {
		EnvironmentInfo info = (EnvironmentInfo) ServiceHelper.getService(Activator.getContext(), EnvironmentInfo.class.getName());
		if (info == null)
			return false;
		String[] args = info.getCommandLineArgs();
		if (args == null)
			return false;
		for (int i = 0; i < args.length; i++) {
			if (args[i] != null && RECONCILER_APPLICATION_ID.equals(args[i].trim()))
				return true;
		}
		return false;
	}
}
