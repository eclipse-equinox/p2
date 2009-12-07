/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.p2.operations;

import org.eclipse.equinox.p2.engine.IProfile;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.operations.*;
import org.eclipse.equinox.internal.provisional.p2.director.PlannerHelper;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.metadata.query.PatchQuery;

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

	private IInstallableUnit[] iusToUpdate;
	private HashMap possibleUpdatesByIU = new HashMap();
	private List defaultUpdates;

	/**
	 * Create an update operation on the specified provisioning session that updates
	 * the specified IInstallableUnits.  Unless otherwise specified, the operation will
	 * be associated with the currently running profile.
	 * 
	 * @param session the session to use for obtaining provisioning services
	 * @param toBeUpdated the IInstallableUnits to be updated.
	 */
	public UpdateOperation(ProvisioningSession session, IInstallableUnit[] toBeUpdated) {
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
		this.defaultUpdates = Arrays.asList(defaultUpdates);
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
		return (Update[]) defaultUpdates.toArray(new Update[defaultUpdates.size()]);
	}

	/**
	 * Get the list of all possible updates.  This list may include multiple versions
	 * of updates for the same IInstallableUnit, as well as patches to the IInstallableUnit.
	 * 
	 * @return an array of all possible updates
	 */
	public Update[] getPossibleUpdates() {
		ArrayList all = new ArrayList();
		Iterator iter = possibleUpdatesByIU.values().iterator();
		while (iter.hasNext()) {
			all.addAll((List) iter.next());
		}
		return (Update[]) all.toArray(new Update[all.size()]);
	}

	private Update[] updatesFor(IInstallableUnit iu, IProfile profile, IProgressMonitor monitor) {
		List updates;
		if (possibleUpdatesByIU.containsKey(iu)) {
			// We've already looked them up in the planner, use the cache
			updates = (List) possibleUpdatesByIU.get(iu);
		} else {
			// We must consult the planner
			IInstallableUnit[] replacements = session.getPlanner().updatesFor(iu, context, monitor);
			updates = new ArrayList(replacements.length);
			for (int i = 0; i < replacements.length; i++) {
				// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=273967
				// In the case of patches, it's possible that a patch is returned as an available update
				// even though it is already installed, because we are querying each IU for updates individually.
				// For now, we ignore any proposed update that is already installed.
				Collector alreadyInstalled = profile.query(new InstallableUnitQuery(replacements[i]), new Collector(), null);
				if (alreadyInstalled.isEmpty()) {
					Update update = new Update(iu, replacements[i]);
					updates.add(update);
				}
			}
			possibleUpdatesByIU.put(iu, updates);
		}
		return (Update[]) updates.toArray(new Update[updates.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.operations.ProfileChangeOperation#computeProfileChangeRequest(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void computeProfileChangeRequest(MultiStatus status, IProgressMonitor monitor) {
		// Here we create a profile change request by finding the latest version available for any replacement, unless
		// otherwise specified in the selections.
		// We have to consider the scenario where the only updates available are patches, in which case the original
		// IU should not be removed as part of the update.
		Set toBeUpdated = new HashSet();
		HashSet elementsToPlan = new HashSet();
		boolean selectionSpecified = false;
		IProfile profile = session.getProfileRegistry().getProfile(profileId);
		if (profile == null)
			return;

		SubMonitor sub = SubMonitor.convert(monitor, Messages.UpdateOperation_ProfileChangeRequestProgress, 100 * iusToUpdate.length);
		for (int i = 0; i < iusToUpdate.length; i++) {
			SubMonitor iuMon = sub.newChild(100);
			Update[] updates = updatesFor(iusToUpdate[i], profile, iuMon);
			for (int j = 0; j < updates.length; j++) {
				toBeUpdated.add(iusToUpdate[i]);
				if (defaultUpdates != null && defaultUpdates.contains(updates[j])) {
					elementsToPlan.add(updates[j]);
					selectionSpecified = true;
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
				HashMap latestVersions = new HashMap();
				boolean foundUpdate = false;
				boolean foundPatch = false;
				for (int j = 0; j < updates.length; j++) {
					String key;
					if (PatchQuery.isPatch(updates[j].replacement)) {
						foundPatch = true;
						key = updates[j].replacement.getId();
					} else {
						foundUpdate = true;
						key = updates[j].toUpdate.getId();
					}
					Update latestUpdate = (Update) latestVersions.get(key);
					IInstallableUnit latestIU = latestUpdate == null ? null : latestUpdate.replacement;
					if (latestIU == null || updates[j].replacement.getVersion().compareTo(latestIU.getVersion()) > 0)
						latestVersions.put(key, updates[j]);
				}
				// If there is a true update available, ignore any patches found
				// Patches are keyed by their own id
				if (foundPatch && foundUpdate) {
					Set keys = new HashSet();
					keys.addAll(latestVersions.keySet());
					Iterator keyIter = keys.iterator();
					while (keyIter.hasNext()) {
						String id = (String) keyIter.next();
						// Get rid of things keyed by a different id.  We've already made sure
						// that updates with a different id are keyed under the original id
						if (!id.equals(iusToUpdate[i].getId())) {
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

		request = ProfileChangeRequest.createByProfileId(profileId);
		Iterator iter = elementsToPlan.iterator();
		while (iter.hasNext()) {
			Update update = (Update) iter.next();
			IInstallableUnit theUpdate = update.replacement;
			if (defaultUpdates == null) {
				defaultUpdates = new ArrayList();
				defaultUpdates.add(update);
			} else {
				if (!defaultUpdates.contains(update))
					defaultUpdates.add(update);
			}
			request.addInstallableUnits(new IInstallableUnit[] {theUpdate});
			//			if (rootMarkerKey != null)
			request.setInstallableUnitProfileProperty(theUpdate, IProfile.PROP_PROFILE_ROOT_IU, Boolean.toString(true));
			if (PatchQuery.isPatch(theUpdate)) {
				request.setInstallableUnitInclusionRules(theUpdate, PlannerHelper.createOptionalInclusionRule(theUpdate));
			} else {
				request.removeInstallableUnits(new IInstallableUnit[] {update.toUpdate});
			}

		}
		sub.done();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.operations.ProfileChangeOperation#getProvisioningJobName()
	 */
	protected String getProvisioningJobName() {
		return Messages.UpdateOperation_UpdateJobName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.operations.ProfileChangeOperation#getResolveJobName()
	 */
	protected String getResolveJobName() {
		return Messages.UpdateOperation_ResolveJobName;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.p2.operations.ProfileChangeOperation#prepareToResolve()
	 */
	protected void prepareToResolve() {
		super.prepareToResolve();
		if (iusToUpdate == null) {
			iusToUpdate = session.getInstalledIUs(profileId, false);
		}
	}

}
