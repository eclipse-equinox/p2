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

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.operations.*;
import org.eclipse.equinox.internal.provisional.p2.director.PlannerHelper;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.engine.query.UserVisibleRootQuery;
import org.eclipse.equinox.p2.metadata.query.PatchQuery;

/**
 * @since 2.0
 * @noextend
 */
public class InstallOperation extends ProfileChangeOperation {

	private IInstallableUnit[] toInstall;

	public InstallOperation(ProvisioningSession session, IInstallableUnit[] toInstall) {
		super(session);
		this.toInstall = toInstall;
	}

	protected void computeProfileChangeRequest(MultiStatus status, IProgressMonitor monitor) {
		request = ProfileChangeRequest.createByProfileId(profileId);
		IProfile profile;
		profile = session.getProfile(profileId);
		SubMonitor sub = SubMonitor.convert(monitor, Messages.InstallOperation_ComputeProfileChangeProgress, toInstall.length);
		for (int i = 0; i < toInstall.length; i++) {
			// If the user is installing a patch, we mark it optional.  This allows
			// the patched IU to be updated later by removing the patch.
			if (PatchQuery.isPatch(toInstall[i]))
				request.setInstallableUnitInclusionRules(toInstall[i], PlannerHelper.createOptionalInclusionRule(toInstall[i]));

			// Check to see if it is already installed.  This may alter the request.
			Collector alreadyInstalled = profile.query(new InstallableUnitQuery(toInstall[i].getId()), new Collector(), null);
			// TODO ideally we should only do this check if the iu is a singleton, but in practice many iu's that should
			// be singletons are not, so we don't check this (yet)
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=230878
			if (alreadyInstalled.size() > 0) { //  && installedIU.isSingleton()
				IInstallableUnit installedIU = (IInstallableUnit) alreadyInstalled.iterator().next();
				int compareTo = toInstall[i].getVersion().compareTo(installedIU.getVersion());
				// If the iu is a newer version of something already installed, consider this an
				// update request
				if (compareTo > 0) {
					boolean lockedForUpdate = false;
					String value = profile.getInstallableUnitProperty(installedIU, IProfile.PROP_PROFILE_LOCKED_IU);
					if (value != null)
						lockedForUpdate = (Integer.parseInt(value) & IProfile.LOCK_UPDATE) == IProfile.LOCK_UPDATE;
					if (lockedForUpdate) {
						// Add a status telling the user that this implies an update, but the
						// iu should not be updated
						status.merge(PlanAnalyzer.getStatus(IStatusCodes.ALTERED_IGNORED_IMPLIED_UPDATE, toInstall[i]));
					} else {
						request.addInstallableUnits(new IInstallableUnit[] {toInstall[i]});
						request.removeInstallableUnits(new IInstallableUnit[] {installedIU});
						// Add a status informing the user that the update has been inferred
						status.merge(PlanAnalyzer.getStatus(IStatusCodes.ALTERED_IMPLIED_UPDATE, toInstall[i]));
						// Mark it as a root if it hasn't been already
						if (!UserVisibleRootQuery.isUserVisible(installedIU, profile))
							request.setInstallableUnitProfileProperty(toInstall[i], IProfile.PROP_PROFILE_ROOT_IU, Boolean.toString(true));
					}
				} else if (compareTo < 0) {
					// An implied downgrade.  We will not put this in the plan, add a status informing the user
					status.merge(PlanAnalyzer.getStatus(IStatusCodes.ALTERED_IGNORED_IMPLIED_DOWNGRADE, toInstall[i]));
				} else {
					//					if (rootMarkerKey != null) {
					if (UserVisibleRootQuery.isUserVisible(installedIU, profile))
						// It is already a root, nothing to do. We tell the user it was already installed
						status.merge(PlanAnalyzer.getStatus(IStatusCodes.ALTERED_IGNORED_ALREADY_INSTALLED, toInstall[i]));
					else {
						// It was already installed but not as a root.  Tell the user that parts of it are already installed and mark
						// it as a root. 
						status.merge(PlanAnalyzer.getStatus(IStatusCodes.ALTERED_PARTIAL_INSTALL, toInstall[i]));
						request.setInstallableUnitProfileProperty(toInstall[i], IProfile.PROP_PROFILE_ROOT_IU, Boolean.toString(true));
					}
					//					}
				}
			} else {
				// Install it and mark as a root
				request.addInstallableUnits(new IInstallableUnit[] {toInstall[i]});
				//				if (rootMarkerKey != null)
				request.setInstallableUnitProfileProperty(toInstall[i], IProfile.PROP_PROFILE_ROOT_IU, Boolean.toString(true));
			}
			sub.worked(1);
		}
		sub.done();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.operations.ProfileChangeOperation#getResolveJobName()
	 */
	protected String getResolveJobName() {
		return Messages.InstallOperation_ResolveJobName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.operations.ProfileChangeOperation#getProvisioningJobName()
	 */
	protected String getProvisioningJobName() {
		return Messages.InstallOperation_InstallJobName;

	}
}
