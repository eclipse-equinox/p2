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
 * IBM Corporation - initial implementation and ideas
 *     Sonatype, Inc. - ongoing development
 *     RedHat, Inc. - Bug 397216, Bug 460967
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.reconciler.dropins;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.*;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.extensionlocation.Constants;
import org.eclipse.equinox.internal.provisional.configurator.Configurator;
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
import org.eclipse.osgi.util.NLS;
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
	private static final String INCLUSION_RULES = "org.eclipse.equinox.p2.internal.inclusion.rules"; //$NON-NLS-1$
	private static final String INCLUSION_OPTIONAL = "OPTIONAL"; //$NON-NLS-1$
	private static final String INCLUSION_STRICT = "STRICT"; //$NON-NLS-1$

	private static final String CACHE_EXTENSIONS = "org.eclipse.equinox.p2.cache.extensions"; //$NON-NLS-1$
	private static final String PIPE = "|"; //$NON-NLS-1$
	private static final String EXPLANATION = "org.eclipse.equinox.p2.director.explain"; //$NON-NLS-1$

	static final String PROP_IGNORE_USER_CONFIGURATION = "eclipse.ignoreUserConfiguration"; //$NON-NLS-1$

	final IProfile profile;

	final Map<String, IMetadataRepository> repositoryMap;
	private Map<String, String> timestamps;
	private final IProvisioningAgent agent;

	/*
	 * Specialized profile change request so we can keep track of IUs which have moved
	 * locations on disk.
	 */
	static class ReconcilerProfileChangeRequest extends ProfileChangeRequest {
		List<IInstallableUnit> toMove = new ArrayList<>();

		public ReconcilerProfileChangeRequest(IProfile profile) {
			super(profile);
		}

		void moveAll(Collection<IInstallableUnit> list) {
			toMove.addAll(list);
		}

		Collection<IInstallableUnit> getMoves() {
			return toMove;
		}
	}

	/*
	 * Constructor for the class.
	 */
	public ProfileSynchronizer(IProvisioningAgent agent, IProfile profile, Collection<IMetadataRepository> repositories) {
		this.agent = agent;
		this.profile = profile;
		this.repositoryMap = new HashMap<>();
		for (IMetadataRepository repository : repositories) {
			repositoryMap.put(repository.getLocation().toString(), repository);
		}
	}

	/*
	 * Synchronize the profile with the list of metadata repositories.
	 * TODO fix progress monitoring (although in practice the user doesn't see it or have a chance to cancel)
	 */
	public IStatus synchronize(IProgressMonitor monitor) {
		readTimestamps();
		if (isUpToDate())
			return Status.OK_STATUS;

		ProvisioningContext context = getContext();
		context.setProperty(EXPLANATION, Boolean.toString(Tracing.DEBUG_RECONCILER));

		String updatedCacheExtensions = synchronizeCacheExtensions();

		// figure out if we really have anything to install/uninstall.
		ReconcilerProfileChangeRequest request = createProfileChangeRequest(context);
		if (request == null) {
			if (updatedCacheExtensions == null)
				return Status.OK_STATUS;
			IStatus engineResult = setProperty(CACHE_EXTENSIONS, updatedCacheExtensions, context, null);
			if (engineResult.getSeverity() != IStatus.ERROR && engineResult.getSeverity() != IStatus.CANCEL)
				writeTimestamps();
			return engineResult;
		}
		if (updatedCacheExtensions != null)
			request.setProfileProperty(CACHE_EXTENSIONS, updatedCacheExtensions);

		// if some of the IUs move locations then construct a special plan and execute that first
		IStatus moveResult = performRemoveForMovedIUs(request, context, monitor);
		if (moveResult.getSeverity() == IStatus.ERROR || moveResult.getSeverity() == IStatus.CANCEL)
			return moveResult;

		if (!request.getRemovals().isEmpty()) {
			Collection<IRequirement> requirements = new ArrayList<>();
			for (IInstallableUnit unit : request.getRemovals()) {
				IRequirement req = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, unit.getId(), new VersionRange(unit.getVersion(), true, unit.getVersion(), true), null, 0, 0, false);
				requirements.add(req);
			}
			request.addExtraRequirements(requirements);
		}

		// now create a plan for the rest of the work and execute it
		IStatus addRemoveResult = performAddRemove(request, context, monitor);
		if (addRemoveResult.getSeverity() == IStatus.ERROR || addRemoveResult.getSeverity() == IStatus.CANCEL)
			return addRemoveResult;

		// write out the new timestamps (for caching) and apply the configuration
		writeTimestamps();
		IStatus applyResult = applyConfiguration(false);

		// Mark the state update as hidden so it does not appear in the Installation History UI list
		// TODO We need to determine if it is ok to use this copy of the profile.
		// See https://bugs.eclipse.org/334670
		IProfileRegistry profileRegistry = agent.getService(IProfileRegistry.class);
		if (profileRegistry != null) {
			IStatus result = profileRegistry.setProfileStateProperty(profile.getProfileId(), profile.getTimestamp(), IProfile.STATE_PROP_HIDDEN, Boolean.TRUE.toString());
			if (!result.isOK()) {
				// we don't get here but if we do, we will ignore the problem and continue. We
				// still want the install operation to succeed. The consequence of this failure is the
				// profile state appears in the UI in the Install History page, which isn't horrible.
				LogHelper.log(result);
			}
		}

		return applyResult;
	}

	/*
	 * Return a list of the roots in the profile.
	 */
	private IQueryResult<IInstallableUnit> getStrictRoots() {
		return profile.query(new IUProfilePropertyQuery(INCLUSION_RULES, INCLUSION_STRICT), null);
	}

	/*
	 * Convert the profile change request into operands and have the engine execute them. There
	 * is fancy logic here in case we are trying to remove IUs which are depended on by something
	 * which is installed via the UI. Since the bundle has been removed from the file-system it is a forced
	 * removal so we have to uninstall the UI-installed IU.
	 */
	private IStatus performAddRemove(ReconcilerProfileChangeRequest request, ProvisioningContext context, IProgressMonitor monitor) {
		// if we have moves then we have previously removed them.
		// now we need to add them back (at the new location)
		for (IInstallableUnit iu : request.getMoves()) {
			request.add(iu);
			request.setInstallableUnitProfileProperty(iu, PROP_FROM_DROPINS, Boolean.TRUE.toString());
			request.setInstallableUnitInclusionRules(iu, ProfileInclusionRules.createOptionalInclusionRule(iu));
			request.setInstallableUnitProfileProperty(iu, IProfile.PROP_PROFILE_LOCKED_IU, Integer.toString(IProfile.LOCK_UNINSTALL));
		}

		Collection<IInstallableUnit> additions = request.getAdditions();
		Collection<IInstallableUnit> removals = request.getRemovals();
		// see if there is any work to do
		if (additions.isEmpty() && removals.isEmpty())
			return Status.OK_STATUS;

		// TODO See bug 270195. Eventually we will attempt to remove strictly installed IUs if their
		// dependent bundles have been deleted.
		boolean removeStrictRoots = false;
		if (removeStrictRoots)
			return performStrictRootRemoval(request, context, monitor);
		IProvisioningPlan plan = createProvisioningPlan(request, context, monitor);
		debug(request, plan);
		return executePlan(plan, context, monitor);
	}

	// TODO re-enable after resolving bug 270195.
	private IStatus performStrictRootRemoval(ReconcilerProfileChangeRequest request, ProvisioningContext context, IProgressMonitor monitor) {
		Collection<IInstallableUnit> removals = request.getRemovals();
		// if we don't have any removals then we don't have to worry about potentially
		// invalidating things we already have installed, removal of roots, etc so just
		// create a regular plan.
		if (removals.isEmpty()) {
			IProvisioningPlan plan = createProvisioningPlan(request, context, monitor);
			debug(request, plan);
			return executePlan(plan, context, monitor);
		}

		// We are now creating a backup of the original request that will be used to create the final plan (where no optional magic is used)
		ProfileChangeRequest finalRequest = request.clone();

		// otherwise collect the roots, pretend they are optional, and see
		// if the resulting plan affects them
		Set<IInstallableUnit> strictRoots = getStrictRoots().toUnmodifiableSet();
		Collection<IRequirement> forceNegation = new ArrayList<>(removals.size());
		for (IInstallableUnit iu : removals)
			forceNegation.add(createNegation(iu));
		request.addExtraRequirements(forceNegation);

		// set all the profile roots to be optional to see how they would be effected by the plan
		for (IInstallableUnit iu : strictRoots)
			request.setInstallableUnitProfileProperty(iu, INCLUSION_RULES, INCLUSION_OPTIONAL);

		// get the tentative plan back from the planner
		IProvisioningPlan plan = createProvisioningPlan(request, context, monitor);
		debug(request, plan);
		if (!plan.getStatus().isOK())
			return plan.getStatus();

		// Analyze the plan to see if any of the strict roots are being uninstalled.
		int removedRoots = 0;
		for (IInstallableUnit initialRoot : strictRoots) {
			// if the root wasn't uninstalled, then continue
			if (plan.getRemovals().query(QueryUtil.createIUQuery(initialRoot), null).isEmpty())
				continue;
			// otherwise add its removal to the change request, along with a negation and
			// change of strict to optional for their inclusion rule.
			finalRequest.remove(initialRoot);
			finalRequest.setInstallableUnitProfileProperty(initialRoot, INCLUSION_RULES, INCLUSION_OPTIONAL);
			IRequirement negation = createNegation(initialRoot);
			Collection<IRequirement> extra = new ArrayList<>();
			extra.add(negation);
			request.addExtraRequirements(extra);
			LogHelper.log(new Status(IStatus.INFO, Activator.ID, NLS.bind(Messages.remove_root, initialRoot.getId(), initialRoot.getVersion())));
			removedRoots++;
		}

		// Check for the case where all the strict roots are being removed.
		if (removedRoots == strictRoots.size())
			return new Status(IStatus.ERROR, Activator.ID, Messages.remove_all_roots);
		plan = createProvisioningPlan(finalRequest, context, monitor);
		if (!plan.getStatus().isOK()) {
			System.out.println("original request"); //$NON-NLS-1$
			System.out.println(request);
			System.out.println("final request"); //$NON-NLS-1$
			System.out.println(finalRequest);
			throw new IllegalStateException("The second plan is not resolvable."); //$NON-NLS-1$
		}

		// execute the plan and return the status
		return executePlan(plan, context, monitor);
	}

	/*
	 * If the request contains IUs to be moved then create and execute a plan which
	 * removes them. Otherwise just return.
	 */
	private IStatus performRemoveForMovedIUs(ReconcilerProfileChangeRequest request, ProvisioningContext context, IProgressMonitor monitor) {
		Collection<IInstallableUnit> moves = request.getMoves();
		if (moves.isEmpty())
			return Status.OK_STATUS;
		IEngine engine = agent.getService(IEngine.class);
		IProvisioningPlan plan = engine.createPlan(profile, context);
		for (IInstallableUnit unit : moves)
			plan.removeInstallableUnit(unit);
		return executePlan(plan, context, monitor);
	}

	/*
	 * Write out the timestamps of various repositories and folders/file to help
	 * us cache and detect cases where we don't have to perform a reconciliation.
	 */
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
			Activator.trace("Writing timestamp file to : " + file.getAbsolutePath()); //$NON-NLS-1$
			try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
				CollectionUtils.storeProperties(timestamps, os, "Timestamps for " + profile.getProfileId()); //$NON-NLS-1$
				if (Tracing.DEBUG_RECONCILER) {
					for (String key : timestamps.keySet()) {
						Object value = timestamps.get(key);
						Activator.trace(key + '=' + value);
					}
				}
			}
		} catch (FileNotFoundException e) {
			//Ignore
		} catch (IOException e) {
			//Ignore
		}
	}

	/*
	 * Check timestamps and return true if the profile is considered to be up-to-date or
	 * false if we should perform a reconciliation.
	 */
	private boolean isUpToDate() {
		// the user might want to force a reconciliation
		if ("true".equals(Activator.getContext().getProperty("osgi.checkConfiguration"))) { //$NON-NLS-1$//$NON-NLS-2$
			Activator.trace("User requested forced reconciliation via \"osgi.checkConfiguration=true\" System property."); //$NON-NLS-1$
			Activator.trace("Performing reconciliation."); //$NON-NLS-1$
			return false;
		}

		String lastKnownProfileTimeStamp = timestamps.remove(PROFILE_TIMESTAMP);
		if (lastKnownProfileTimeStamp == null) {
			Activator.trace("Profile timestamp not found in cache."); //$NON-NLS-1$
			Activator.trace("Performing reconciliation."); //$NON-NLS-1$
			return false;
		}
		String currentProfileTimestamp = Long.toString(profile.getTimestamp());
		if (!lastKnownProfileTimeStamp.equals(currentProfileTimestamp)) {
			Activator.trace("Profile timestamps not equal, expected: " + lastKnownProfileTimeStamp + ", actual=" + currentProfileTimestamp); //$NON-NLS-1$ //$NON-NLS-2$
			Activator.trace("Performing reconciliation."); //$NON-NLS-1$
			return false;
		}

		//When we get here the timestamps map only contains information related to repos
		for (Entry<String, IMetadataRepository> entry : repositoryMap.entrySet()) {
			IMetadataRepository repository = entry.getValue();

			Map<String, String> props = repository.getProperties();
			String currentTimestamp = null;
			if (props != null)
				currentTimestamp = props.get(IRepository.PROP_TIMESTAMP);

			if (currentTimestamp == null)
				currentTimestamp = NO_TIMESTAMP;

			String key = entry.getKey();
			String lastKnownTimestamp = timestamps.remove(key);
			//A repo has been added
			if (lastKnownTimestamp == null) {
				Activator.trace("No cached timestamp found for: " + key); //$NON-NLS-1$
				Activator.trace("Performing reconciliation."); //$NON-NLS-1$
				return false;
			}
			if (!lastKnownTimestamp.equals(currentTimestamp)) {
				Activator.trace("Timestamps not equal for file: " + key + ", expected: " + lastKnownTimestamp + ", actual: " + currentTimestamp); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				Activator.trace("Performing reconciliation."); //$NON-NLS-1$
				return false;
			}
		}
		if (timestamps.size() == 0) {
			Activator.trace("Timestamps valid."); //$NON-NLS-1$
			Activator.trace("Skipping reconciliation."); //$NON-NLS-1$
			return true;
		}

		//A repo has been removed
		if (Tracing.DEBUG_RECONCILER) {
			Activator.trace("Extra values in timestamp file:"); //$NON-NLS-1$
			for (String string : timestamps.keySet())
				Activator.trace(string);
			Activator.trace("Performing reconciliation."); //$NON-NLS-1$
		}
		return false;
	}

	/*
	 * Read the values of the stored timestamps that we use for caching.
	 */
	private void readTimestamps() {
		if (Boolean.TRUE.toString().equalsIgnoreCase(System.getProperty(PROP_IGNORE_USER_CONFIGURATION))) {
			timestamps = new HashMap<>();
			Activator.trace("Master profile changed."); //$NON-NLS-1$
			Activator.trace("Performing reconciliation."); //$NON-NLS-1$
			return;
		}
		File file = Activator.getContext().getDataFile(TIMESTAMPS_FILE_PREFIX + profile.getProfileId().hashCode());
		try {
			try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
				timestamps = CollectionUtils.loadProperties(is);
			}
		} catch (FileNotFoundException e) {
			//Ignore
			timestamps = new HashMap<>();
			Activator.trace("Timestamp file does not exist."); //$NON-NLS-1$
			Activator.trace("Performing reconciliation."); //$NON-NLS-1$
		} catch (IOException e) {
			//Ignore
			timestamps = new HashMap<>();
			Activator.trace("Exception loading timestamp file: " + e.getMessage()); //$NON-NLS-1$
			Activator.trace("Performing reconciliation."); //$NON-NLS-1$
		}
	}

	private ProvisioningContext getContext() {
		ArrayList<URI> repoURLs = new ArrayList<>();
		for (String string : repositoryMap.keySet()) {
			try {
				repoURLs.add(new URI(string));
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
		List<String> currentExtensions = new ArrayList<>();
		StringBuilder buffer = new StringBuilder();

		List<String> repositories = new ArrayList<>(repositoryMap.keySet());
		URL installArea = Activator.getOSGiInstallArea();
		final String OSGiInstallArea;
		try {
			// The OSGi install area is an unencoded URL and repository locations are encoded URIs
			// so make them the same so we can compare them.
			// See https://bugs.eclipse.org/346565.
			OSGiInstallArea = URIUtil.toURI(installArea).toString() + Constants.EXTENSION_LOCATION;
			// Sort the repositories so the extension location at the OSGi install folder is first.
			// See https://bugs.eclipse.org/246310.
			repositories.sort((left, right) -> {
				if (OSGiInstallArea.equals(left))
					return -1;
				if (OSGiInstallArea.equals(right))
					return 1;
				return left.compareTo(right);
			});
		} catch (URISyntaxException e) {
			// This shouldn't happen but if it does we will log the error and continue
			// with the repositories in the default order.
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Unable to convert OSGi install area: " + installArea + " into URI.", e)); //$NON-NLS-1$ //$NON-NLS-2$
		}
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

		List<String> previousExtensions = new ArrayList<>();
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
		Map<IInstallableUnit, IInstallableUnit> result = new HashMap<>();
		for (IInstallableUnit iu : profileQueryResult) {
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
		Map<IInstallableUnit, IInstallableUnit> result = new HashMap<>();
		for (IInstallableUnit iu : profileQueryResult) {
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
		ReconcilerProfileChangeRequest request = new ReconcilerProfileChangeRequest(profile);

		boolean resolve = Boolean.parseBoolean(profile.getProperty("org.eclipse.equinox.p2.resolve")); //$NON-NLS-1$
		if (resolve)
			request.removeProfileProperty("org.eclipse.equinox.p2.resolve"); //$NON-NLS-1$

		List<IInstallableUnit> toRemove = new ArrayList<>();
		List<IInstallableUnit> toMove = new ArrayList<>();

		boolean foundIUsToAdd = false;
		Map<IInstallableUnit, IInstallableUnit> profileIUs = getProfileIUs();

		// we use IProfile.available(...) here so that we also gather any shared IUs
		Map<IInstallableUnit, IInstallableUnit> availableProfileIUs = getAvailableProfileIUs();

		// get all IUs from all our repos
		IQueryResult<IInstallableUnit> allIUs = getAllIUsFromRepos();
		for (Iterator<IInstallableUnit> iter = allIUs.iterator(); iter.hasNext();) {
			final IInstallableUnit iu = iter.next();
			IInstallableUnit existing = profileIUs.get(iu);
			// check to see if this IU has moved locations
			if (existing != null) {
				// if the IU is already installed in the profile then check to see if it was moved.
				String one = iu.getProperty(RepositoryListener.FILE_NAME);
				String two = existing.getProperty(RepositoryListener.FILE_NAME);
				// cheat here... since we always set the filename property for bundles in the dropins,
				// if the existing IU's filename is null then it isn't from the dropins. a better
				// (and more expensive) way to find this out is to do an IU profile property query.
				if (two == null) {
					// the IU is already installed so don't mark it as a dropin now - see bug 404619.
					iter.remove();
					continue;
				}
				// if we have an IU which has been moved, keep track of it.
				if (one != null && !one.equals(two)) {
					toMove.add(iu);
					continue;
				}
			}
			// even though we are adding all IUs below, we need to explicitly set the properties for
			// them as well. Do that here.
			if (QueryUtil.isGroup(iu))
				request.setInstallableUnitProfileProperty(iu, IProfile.PROP_PROFILE_ROOT_IU, Boolean.TRUE.toString());
			// mark all IUs with special property
			request.setInstallableUnitProfileProperty(iu, PROP_FROM_DROPINS, Boolean.TRUE.toString());
			request.setInstallableUnitInclusionRules(iu, ProfileInclusionRules.createOptionalInclusionRule(iu));
			request.setInstallableUnitProfileProperty(iu, IProfile.PROP_PROFILE_LOCKED_IU, Integer.toString(IProfile.LOCK_UNINSTALL));

			// as soon as we find something locally that needs to be installed, then
			// everything from the parent's dropins must be installed locally as well.
			if (!foundIUsToAdd && availableProfileIUs.get(iu) == null) {
				foundIUsToAdd = true;
			}
		}

		// get all IUs from profile with marked property (existing)
		IQueryResult<IInstallableUnit> dropinIUs = profile.query(new IUProfilePropertyQuery(PROP_FROM_DROPINS, Boolean.TRUE.toString()), null);
		Set<IInstallableUnit> all = allIUs.toUnmodifiableSet();
		for (IInstallableUnit iu : dropinIUs) {
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
			if (!all.contains(iu))
				toRemove.add(iu);
		}

		if (!foundIUsToAdd && toRemove.isEmpty() && !resolve && toMove.isEmpty()) {
			if (Tracing.DEBUG_RECONCILER)
				Tracing.debug("[reconciler] Nothing to do."); //$NON-NLS-1$
			return null;
		}

		// everything from the drop-ins must be considered for addition/removal everytime so add all here
		request.addAll(all);
		request.removeAll(toRemove);
		request.moveAll(toMove);

		debug(request);
		return request;
	}

	/*
	 * Create and return a negated requirement saying that the given IU must not exist in the profile.
	 */
	private IRequirement createNegation(IInstallableUnit unit) {
		return MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, unit.getId(), //
				new VersionRange(unit.getVersion(), true, unit.getVersion(), true), null, 0, 0, false);
	}

	/*
	 * If in debug mode, print out information which tells us whether or not the given
	 * provisioning plan matches the request.
	 */
	private void debug(ReconcilerProfileChangeRequest request, IProvisioningPlan plan) {
		if (!Tracing.DEBUG_RECONCILER)
			return;
		final String PREFIX = "[reconciler] [plan] "; //$NON-NLS-1$
		// get the request
		List<IInstallableUnit> toAdd = new ArrayList<>(request.getAdditions());
		List<IInstallableUnit> toRemove = new ArrayList<>(request.getRemovals());
		List<IInstallableUnit> toMove = new ArrayList<>(request.getMoves());

		// remove from the request everything that is in the plan
		for (IInstallableUnit iu : plan.getRemovals().query(QueryUtil.createIUAnyQuery(), null)) {
			if (!toRemove.remove(iu)) {
				Tracing.debug(PREFIX + iu + " will be removed"); //$NON-NLS-1$
			}
		}
		for (IInstallableUnit iu : plan.getAdditions().query(QueryUtil.createIUAnyQuery(), null)) {
			if (!toAdd.remove(iu)) {
				Tracing.debug(PREFIX + iu + " will be added"); //$NON-NLS-1$
			}
		}
		// Move operations are treated as doing a remove/add. The removes have already happened
		// and at this point we are adding the moved IUs back at their new location. Remove the moved
		// IUs from the added list because this will just confuse the user.
		toAdd.removeAll(toMove);

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
	private void debug(ReconcilerProfileChangeRequest request) {
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

		Collection<IInstallableUnit> toMove = request.getMoves();
		if (toMove == null || toMove.isEmpty()) {
			Tracing.debug(PREFIX + "No installable units to move."); //$NON-NLS-1$
		} else {
			for (IInstallableUnit move : toMove)
				Tracing.debug(PREFIX + "Moving IU: " + move.getId() + ' ' + move.getVersion()); //$NON-NLS-1$
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
		Collector<IInstallableUnit> allRepos = new Collector<>();
		for (IMetadataRepository repository : repositoryMap.values()) {
			allRepos.addAll(repository.query(QueryUtil.createIUAnyQuery(), null));
		}
		return allRepos;
	}

	/*
	 * Create and return a provisioning plan for the given change request.
	 */
	private IProvisioningPlan createProvisioningPlan(ProfileChangeRequest request, ProvisioningContext provisioningContext, IProgressMonitor monitor) {
		IPlanner planner = agent.getService(IPlanner.class);
		return planner.getProvisioningPlan(request, provisioningContext, monitor);
	}

	/*
	 * Call the engine to set the given property on the profile.
	 */
	private IStatus setProperty(String key, String value, ProvisioningContext provisioningContext, IProgressMonitor monitor) {
		IEngine engine = agent.getService(IEngine.class);
		IProvisioningPlan plan = engine.createPlan(profile, provisioningContext);
		plan.setProfileProperty(key, value);
		IPhaseSet phaseSet = PhaseSetFactory.createPhaseSetIncluding(new String[] {PhaseSetFactory.PHASE_PROPERTY});
		return engine.perform(plan, phaseSet, monitor);
	}

	/*
	 * Execute the given plan.
	 */
	private IStatus executePlan(IProvisioningPlan plan, ProvisioningContext provisioningContext, IProgressMonitor monitor) {
		IEngine engine = agent.getService(IEngine.class);
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
		ServiceReference<Configurator> reference = context.getServiceReference(Configurator.class);
		Configurator configurator = context.getService(reference);
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
		EnvironmentInfo info = ServiceHelper.getService(Activator.getContext(), EnvironmentInfo.class);
		if (info == null)
			return false;
		String[] args = info.getCommandLineArgs();
		if (args == null)
			return false;
		for (String arg : args) {
			if (arg != null && RECONCILER_APPLICATION_ID.equals(arg.trim()))
				return true;
		}
		return false;
	}
}
