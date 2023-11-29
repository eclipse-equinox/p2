/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.operations;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.operations.*;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.query.UserVisibleRootQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.ProfileInclusionRules;
import org.eclipse.equinox.p2.query.*;

/**
 * An UpdateOperation describes an operation that updates {@link IInstallableUnit}s in
 * a profile.
 *
 * The following snippet shows how one might use an UpdateOperation to check for updates
 * to the profile and then install them in the background.
 *
 * <pre>
 * UpdateOperation op = new UpdateOperation(session);
 * IStatus result = op.resolveModal(monitor);
 * if (result.isOK()) {
 *   op.getProvisioningJob(monitor).schedule();
 * }
 * </pre>
 *
 * The life cycle of an UpdateOperation is different than that of the other
 * operations.  Since assembling the list of possible updates may be costly,
 * clients should not have to create a new update operation if the desired updates
 * to be applied need to change.  In this case, the client can set a new set of
 * chosen updates on the update operation and resolve again.
 *
 * <pre>
 * UpdateOperation op = new UpdateOperation(session);
 * IStatus result = op.resolveModal(monitor);
 * if (result.isOK()) {
 *   op.getProvisioningJob(monitor).schedule();
 * } else if (result.getSeverity() == IStatus.ERROR) {
 *   Update [] chosenUpdates = letUserPickFrom(op.getPossibleUpdates());
 *   op.setSelectedUpdates(chosenUpdates);
 *   IStatus result = op.resolveModal(monitor);
 * }
 * </pre>
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.0
 */
public class UpdateOperation extends ProfileChangeOperation {

	/**
	 * A status code used to indicate that there were no updates found when
	 * looking for updates.
	 */
	public static final int STATUS_NOTHING_TO_UPDATE = IStatusCodes.NOTHING_TO_UPDATE;

	private Collection<IInstallableUnit> iusToUpdate;
	private HashMap<IInstallableUnit, List<Update>> possibleUpdatesByIU = new HashMap<>();
	private List<Update> defaultUpdates;

	/**
	 * Create an update operation on the specified provisioning session that updates
	 * the specified IInstallableUnits.  Unless otherwise specified, the operation will
	 * be associated with the currently running profile.
	 *
	 * @param session the session to use for obtaining provisioning services
	 * @param toBeUpdated the IInstallableUnits to be updated.
	 */
	public UpdateOperation(ProvisioningSession session, Collection<IInstallableUnit> toBeUpdated) {
		super(session);
		this.iusToUpdate = toBeUpdated;
	}

	/**
	 * Create an update operation that will update all of the user-visible installable
	 * units in the profile (the profile roots).
	 *
	 * @param session the session providing the provisioning services
	 */
	public UpdateOperation(ProvisioningSession session) {
		this(session, null);
	}

	/**
	 * Set the updates that should be selected from the set of available updates.
	 * If the selected updates are not specified, then the latest available update
	 * for each IInstallableUnit with updates will be chosen.
	 *
	 * @param defaultUpdates the updates that should be chosen from all of the available
	 * updates.
	 */
	public void setSelectedUpdates(Update[] defaultUpdates) {
		this.defaultUpdates = new ArrayList<>(Arrays.asList(defaultUpdates));
	}

	/**
	 * Get the updates that have been selected from the set of available updates.
	 * If none have been specified by the client, then the latest available update
	 * for each IInstallableUnit with updates will be chosen.
	 *
	 * @return the updates that should be chosen from all of the available updates
	 */
	public Update[] getSelectedUpdates() {
		if (defaultUpdates == null)
			return new Update[0];
		return defaultUpdates.toArray(new Update[defaultUpdates.size()]);
	}

	/**
	 * Get the list of all possible updates.  This list may include multiple versions
	 * of updates for the same IInstallableUnit, as well as patches to the IInstallableUnit.
	 *
	 * @return an array of all possible updates
	 */
	public Update[] getPossibleUpdates() {
		ArrayList<Update> all = new ArrayList<>();
		for (List<Update> updates : possibleUpdatesByIU.values())
			all.addAll(updates);
		return all.toArray(new Update[all.size()]);
	}

	private Update[] updatesFor(IInstallableUnit iu, IProfile profile, IProgressMonitor monitor) {
		List<Update> updates;
		if (possibleUpdatesByIU.containsKey(iu)) {
			// We've already looked them up in the planner, use the cache
			updates = possibleUpdatesByIU.get(iu);
		} else {
			// We must consult the planner
			IQueryResult<IInstallableUnit> replacements = session.getPlanner().updatesFor(iu, context, monitor);
			updates = new ArrayList<>();
			for (IInstallableUnit replacementIU : replacements) {
				// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=273967
				// In the case of patches, it's possible that a patch is returned as an
				// available update
				// even though it is already installed, because we are querying each IU for
				// updates individually.
				// For now, we ignore any proposed update that is already installed.
				IQueryResult<IInstallableUnit> alreadyInstalled = profile.query(QueryUtil.createIUQuery(replacementIU), null);
				if (alreadyInstalled.isEmpty()) {
					Update update = new Update(iu, replacementIU);
					updates.add(update);
				}
			}
			possibleUpdatesByIU.put(iu, updates);
		}
		return updates.toArray(new Update[updates.size()]);
	}

	@Override
	protected void computeProfileChangeRequest(MultiStatus status, IProgressMonitor monitor) {
		// Here we create a profile change request by finding the latest version available for any replacement, unless
		// otherwise specified in the selections.
		// We have to consider the scenario where the only updates available are patches, in which case the original
		// IU should not be removed as part of the update.
		Set<IInstallableUnit> toBeUpdated = new HashSet<>();
		HashSet<Update> elementsToPlan = new HashSet<>();
		boolean selectionSpecified = defaultUpdates != null;
		IProfile profile = session.getProfileRegistry().getProfile(profileId);
		if (profile == null)
			return;

		SubMonitor sub = SubMonitor.convert(monitor, Messages.UpdateOperation_ProfileChangeRequestProgress, 100 * iusToUpdate.size());
		for (IInstallableUnit iuToUpdate : iusToUpdate) {
			SubMonitor iuMon = sub.newChild(100);
			Update[] updates = updatesFor(iuToUpdate, profile, iuMon);
			for (Update update : updates) {
				toBeUpdated.add(iuToUpdate);
				if (defaultUpdates != null && defaultUpdates.contains(update)) {
					elementsToPlan.add(update);
				}
			}
			if (!selectionSpecified) {
				// If no selection was specified, we must figure out the latest version to apply.
				// The rules are that a true update will always win over a patch, but if only
				// patches are available, they should all be selected.
				// We first gather the latest versions of everything proposed.
				// Patches are keyed by their id because they are unique and should not be compared to
				// each other.  Updates are keyed by the IU they are updating so we can compare the
				// versions and select the latest one
				HashMap<String, Update> latestVersions = new HashMap<>();
				boolean foundUpdate = false;
				boolean foundPatch = false;
				for (Update update : updates) {
					String key;
					if (QueryUtil.isPatch(update.replacement)) {
						foundPatch = true;
						key = update.replacement.getId();
					} else {
						foundUpdate = true;
						key = update.toUpdate.getId();
					}
					Update latestUpdate = latestVersions.get(key);
					IInstallableUnit latestIU = latestUpdate == null ? null : latestUpdate.replacement;
					if (latestIU == null || update.replacement.getVersion().compareTo(latestIU.getVersion()) > 0) {
						latestVersions.put(key, update);
					}
				}
				// If there is a true update available, ignore any patches found
				// Patches are keyed by their own id
				if (foundPatch && foundUpdate) {
					Set<String> keys = new HashSet<>();
					keys.addAll(latestVersions.keySet());
					for (String id : keys) {
						// Get rid of things keyed by a different id.  We've already made sure
						// that updates with a different id are keyed under the original id
						if (!id.equals(iuToUpdate.getId())) {
							latestVersions.remove(id);
						}
					}
				}
				elementsToPlan.addAll(latestVersions.values());
			}
			sub.worked(100);
		}

		if (toBeUpdated.size() <= 0 || elementsToPlan.isEmpty()) {
			sub.done();
			status.add(PlanAnalyzer.getStatus(IStatusCodes.NOTHING_TO_UPDATE, null));
			return;
		}

		request = ProfileChangeRequest.createByProfileId(session.getProvisioningAgent(), profileId);
		for (Update update : elementsToPlan) {
			IInstallableUnit theUpdate = update.replacement;
			if (defaultUpdates == null) {
				defaultUpdates = new ArrayList<>();
				defaultUpdates.add(update);
			} else {
				if (!defaultUpdates.contains(update))
					defaultUpdates.add(update);
			}
			request.add(theUpdate);
			request.setInstallableUnitProfileProperty(theUpdate, IProfile.PROP_PROFILE_ROOT_IU, Boolean.toString(true));
			if (QueryUtil.isPatch(theUpdate)) {
				request.setInstallableUnitInclusionRules(theUpdate, ProfileInclusionRules.createOptionalInclusionRule(theUpdate));
			} else {
				request.remove(update.toUpdate);
			}

		}
		sub.done();
	}

	@Override
	protected String getProvisioningJobName() {
		return Messages.UpdateOperation_UpdateJobName;
	}

	@Override
	protected String getResolveJobName() {
		return Messages.UpdateOperation_ResolveJobName;
	}

	@Override
	protected void prepareToResolve() {
		super.prepareToResolve();
		if (iusToUpdate == null) {
			iusToUpdate = getInstalledIUs();
		}
	}

	/*
	 * Get the IInstallable units for the specified profile
	 *
	 * @param profileId the profile in question
	 * @param all <code>true</code> if all IInstallableUnits in the profile should
	 * be returned, <code>false</code> only those IInstallableUnits marked as (user visible) roots
	 * should be returned.
	 *
	 * @return an array of IInstallableUnits installed in the profile.
	 */
	private Collection<IInstallableUnit> getInstalledIUs() {
		IProfile profile = session.getProfileRegistry().getProfile(profileId);
		if (profile == null)
			return Collections.emptyList();
		IQuery<IInstallableUnit> query = new UserVisibleRootQuery();
		IQueryResult<IInstallableUnit> queryResult = profile.query(query, null);
		return queryResult.toUnmodifiableSet();
	}

	/*
	 * Overridden to delay computation of the profile change request until the resolution
	 * occurs.  This is done because computing the request is expensive (it involves searching
	 * for updates).
	 */
	@Override
	void makeResolveJob(IProgressMonitor monitor) {
		// throw away any previous requests
		request = null;
		noChangeRequest = PlanAnalyzer.getProfileChangeAlteredStatus();
		// the requestHolder is a hack to work around the fact that there is no public API
		// for the resolution job to get the request from the operation after it has been
		// computed.
		final ProfileChangeRequest[] requestHolder = new ProfileChangeRequest[1];
		job = new SearchForUpdatesResolutionJob(getResolveJobName(), session, profileId, request, getFirstPassProvisioningContext(), getSecondPassEvaluator(), noChangeRequest, mon -> {
			// We only check for other jobs running if this job is *not* scheduled
			if (job.getState() == Job.NONE && session.hasScheduledOperationsFor(profileId)) {
				noChangeRequest.add(PlanAnalyzer.getStatus(IStatusCodes.OPERATION_ALREADY_IN_PROGRESS, null));
			} else {
				computeProfileChangeRequest(noChangeRequest, mon);
				requestHolder[0] = UpdateOperation.this.request;
			}
		}, requestHolder, this);
	}

	/*
	 * Overridden because our resolution job life cycle is different.  We have a job
	 * before we've computed the profile change request, so we must ensure that we
	 * have already computed the profile change request.
	 */
	@Override
	public boolean hasResolved() {
		return request != null && super.hasResolved();
	}
}
