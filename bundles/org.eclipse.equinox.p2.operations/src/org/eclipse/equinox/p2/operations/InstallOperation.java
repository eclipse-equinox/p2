/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 *     Rapicorp, Inc (Pascal Rapicault) - Bug 394156 - Add support for updates from one namespace to another
 ******************************************************************************/
package org.eclipse.equinox.p2.operations;

import java.util.Collection;
import java.util.Iterator;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.director.Explanation;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.operations.*;
import org.eclipse.equinox.internal.provisional.p2.director.PlannerStatus;
import org.eclipse.equinox.internal.provisional.p2.director.RequestStatus;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.query.UserVisibleRootQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.ProfileInclusionRules;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;

/**
 * An InstallOperation describes an operation that installs IInstallableUnits into
 * a profile.
 * 
 * The following snippet shows how one might use an InstallOperation to perform a synchronous resolution and
 * then kick off an install in the background:
 * 
 * <pre>
 * InstallOperation op = new InstallOperation(session, new IInstallableUnit [] { myIU });
 * IStatus result = op.resolveModal(monitor);
 * if (result.isOK()) {
 *   op.getProvisioningJob(monitor).schedule();
 * }
 * </pre>
 * 
 * @since 2.0
 * @see ProfileChangeOperation
 * @noextend This class is not intended to be subclassed by clients.
 */
public class InstallOperation extends ProfileChangeOperation {

	/**
	 * @since 2.1
	 */
	protected Collection<IInstallableUnit> toInstall;

	/**
	 * Create an install operation on the specified provisioning session that installs
	 * the supplied IInstallableUnits.  Unless otherwise specified, the operation will
	 * be associated with the currently running profile.
	 * 
	 * @param session the session to use for obtaining provisioning services
	 * @param toInstall the IInstallableUnits to be installed into the profile.
	 */
	public InstallOperation(ProvisioningSession session, Collection<IInstallableUnit> toInstall) {
		super(session);
		this.toInstall = toInstall;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.p2.operations.ProfileChangeOperation#computeProfileChangeRequest(org.eclipse.core.runtime.MultiStatus, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void computeProfileChangeRequest(MultiStatus status, IProgressMonitor monitor) {
		request = ProfileChangeRequest.createByProfileId(session.getProvisioningAgent(), profileId);
		IProfile profile = session.getProfileRegistry().getProfile(profileId);
		SubMonitor sub = SubMonitor.convert(monitor, Messages.InstallOperation_ComputeProfileChangeProgress, toInstall.size());
		for (IInstallableUnit entryToInstall : toInstall) {
			// If the user is installing a patch, we mark it optional.  This allows
			// the patched IU to be updated later by removing the patch.
			if (QueryUtil.isPatch(entryToInstall))
				request.setInstallableUnitInclusionRules(entryToInstall, ProfileInclusionRules.createOptionalInclusionRule(entryToInstall));

			// Check to see if it is already installed.  This may alter the request.
			IQueryResult<IInstallableUnit> alreadyInstalled = profile.query(QueryUtil.createIUQuery(entryToInstall.getId()), null);
			// TODO ideally we should only do this check if the iu is a singleton, but in practice many iu's that should
			// be singletons are not, so we don't check this (yet)
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=230878
			if (!alreadyInstalled.isEmpty()) { //  && installedIU.isSingleton()
				IInstallableUnit installedIU = alreadyInstalled.iterator().next();
				int compareTo = entryToInstall.getVersion().compareTo(installedIU.getVersion());
				// If the iu is a newer version of something already installed, consider this an update request
				if (compareTo > 0) {
					boolean lockedForUpdate = false;
					String value = profile.getInstallableUnitProperty(installedIU, IProfile.PROP_PROFILE_LOCKED_IU);
					if (value != null)
						lockedForUpdate = (Integer.parseInt(value) & IProfile.LOCK_UPDATE) == IProfile.LOCK_UPDATE;
					if (lockedForUpdate) {
						// Add a status telling the user that this implies an update, but the iu should not be updated
						status.merge(PlanAnalyzer.getStatus(IStatusCodes.ALTERED_IGNORED_IMPLIED_UPDATE, entryToInstall));
					} else {
						dealWithUpdates(status, profile, entryToInstall, installedIU);
					}
				} else if (compareTo < 0) {
					// An implied downgrade.  We will not put this in the plan, add a status informing the user
					status.merge(PlanAnalyzer.getStatus(IStatusCodes.ALTERED_IGNORED_IMPLIED_DOWNGRADE, entryToInstall));
				} else {
					if (UserVisibleRootQuery.isUserVisible(installedIU, profile))
						// It is already a root, nothing to do. We tell the user it was already installed
						status.merge(PlanAnalyzer.getStatus(IStatusCodes.ALTERED_IGNORED_ALREADY_INSTALLED, entryToInstall));
					else {
						// It was already installed but not as a root.  Tell the user that parts of it are already installed and mark
						// it as a root. 
						status.merge(PlanAnalyzer.getStatus(IStatusCodes.ALTERED_PARTIAL_INSTALL, entryToInstall));
						request.setInstallableUnitProfileProperty(entryToInstall, IProfile.PROP_PROFILE_ROOT_IU, Boolean.toString(true));
					}
				}
			} else {
				//Deal with the case of updates with renames
				boolean handled = false;
				Iterator<IInstallableUnit> allIUsIterator = profile.query(QueryUtil.ALL_UNITS, null).iterator();
				while (allIUsIterator.hasNext()) {
					IInstallableUnit iuAlreadyInstalled = allIUsIterator.next();
					if (entryToInstall.getUpdateDescriptor() != null && entryToInstall.getUpdateDescriptor().isUpdateOf(iuAlreadyInstalled)) {
						dealWithUpdates(status, profile, entryToInstall, iuAlreadyInstalled);
						handled = true;
						break;
					}
				}
				if (!handled)
					// Install it and mark as a root
					request.add(entryToInstall);
				request.setInstallableUnitProfileProperty(entryToInstall, IProfile.PROP_PROFILE_ROOT_IU, Boolean.toString(true));
			}
			sub.worked(1);
		}
		sub.done();
	}

	private void dealWithUpdates(MultiStatus status, IProfile profile, IInstallableUnit entryToInstall, IInstallableUnit installedIU) {
		request.add(entryToInstall);
		request.remove(installedIU);
		// Add a status informing the user that the update has been inferred
		status.merge(PlanAnalyzer.getStatus(IStatusCodes.ALTERED_IMPLIED_UPDATE, entryToInstall));
		// Mark it as a root if it hasn't been already
		if (!UserVisibleRootQuery.isUserVisible(installedIU, profile))
			request.setInstallableUnitProfileProperty(entryToInstall, IProfile.PROP_PROFILE_ROOT_IU, Boolean.toString(true));
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

	@Override
	ProvisioningContext getFirstPassProvisioningContext() {
		// Set it back to no referencing for first pass in case we reuse this context.
		context.setProperty(ProvisioningContext.FOLLOW_REPOSITORY_REFERENCES, null);
		return context;
	}

	@Override
	IFailedStatusEvaluator getSecondPassEvaluator() {
		return new IFailedStatusEvaluator() {
			public ProvisioningContext getSecondPassProvisioningContext(IProvisioningPlan failedPlan) {
				// Follow metadata repository references if the first try fails
				// There should be real API for this!
				if (missingRequirement(failedPlan))
					context.setProperty(ProvisioningContext.FOLLOW_REPOSITORY_REFERENCES, Boolean.toString(true));
				return context;
			}
		};
	}

	// this is very reachy
	boolean missingRequirement(IProvisioningPlan failedPlan) {
		IStatus status = failedPlan.getStatus();
		RequestStatus requestStatus = null;
		if (status instanceof PlannerStatus)
			requestStatus = ((PlannerStatus) status).getRequestStatus();
		return requestStatus != null && requestStatus.getShortExplanation() == Explanation.MISSING_REQUIREMENT;
	}
}
