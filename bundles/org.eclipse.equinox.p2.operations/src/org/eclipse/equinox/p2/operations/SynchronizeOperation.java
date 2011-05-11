/*******************************************************************************
 *  Copyright (c) 2011 Sonatype, Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Sonatype, Inc. - initial API and implementation
 *     IBM Corporation - Ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.operations;

import java.util.Collection;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.operations.Messages;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.query.UserVisibleRootQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.ProfileInclusionRules;
import org.eclipse.equinox.p2.query.QueryUtil;

/**
 * A {@link SynchronizeOperation} describes an operation that will modify the installation to 
 * exclusively include the InstallableUnit mentioned. Note that all the Installable Units necessary
 * to satisfy the dependencies of the Installable Units installed will also be installed.  
 * 
 * The following snippet shows how one might use an SynchronizeOperation to perform a synchronous resolution and
 * then kick off an install in the background:
 * 
 * <pre>
 * SynchronizeOperation op = new SynchronizeOperation(session, new IInstallableUnit [] { myIU });
 * IStatus result = op.resolveModal(monitor);
 * if (result.isOK()) {
 *   op.getProvisioningJob(monitor).schedule();
 * }
 * </pre>
 * 
 * @since 2.1
 * @see ProfileChangeOperation
 * @noextend This class is not intended to be subclassed by clients.
 */
public class SynchronizeOperation extends InstallOperation {

	public SynchronizeOperation(ProvisioningSession session, Collection<IInstallableUnit> toInstall) {
		super(session, toInstall);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.p2.operations.ProfileChangeOperation#computeProfileChangeRequest(org.eclipse.core.runtime.MultiStatus, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void computeProfileChangeRequest(MultiStatus status, IProgressMonitor monitor) {
		request = ProfileChangeRequest.createByProfileId(session.getProvisioningAgent(), profileId);
		IProfile profile;
		SubMonitor sub = SubMonitor.convert(monitor, Messages.InstallOperation_ComputeProfileChangeProgress, toInstall.size());
		profile = session.getProfileRegistry().getProfile(profileId);
		request.removeAll(profile.query(new UserVisibleRootQuery(), sub).toUnmodifiableSet());
		request.addAll(toInstall);
		for (IInstallableUnit entryToInstall : toInstall) {
			// If the user is installing a patch, we mark it optional.  This allows the patched IU to be updated later by removing the patch.
			if (QueryUtil.isPatch(entryToInstall))
				request.setInstallableUnitInclusionRules(entryToInstall, ProfileInclusionRules.createOptionalInclusionRule(entryToInstall));
			else
				request.setInstallableUnitProfileProperty(entryToInstall, IProfile.PROP_PROFILE_ROOT_IU, Boolean.toString(true));

			sub.worked(1);
		}
		sub.done();
	}
}