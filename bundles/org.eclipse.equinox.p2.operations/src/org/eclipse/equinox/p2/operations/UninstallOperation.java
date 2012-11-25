/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 ******************************************************************************/
package org.eclipse.equinox.p2.operations;

import java.net.URI;
import java.util.Collection;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.operations.IFailedStatusEvaluator;
import org.eclipse.equinox.internal.p2.operations.Messages;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * An UninstallOperation describes an operation that uninstalls {@link IInstallableUnit}s from
 * a profile.
 * 
 * The following snippet shows how one might use an UninstallOperation to perform a synchronous resolution and
 * then kick off an uninstall in the background:
 * 
 * <pre>
 * UninstallOperation op = new UninstallOperation(session, new IInstallableUnit [] { removeThisIU });
 * IStatus result = op.resolveModal(monitor);
 * if (result.isOK()) {
 *   op.getProvisioningJob(monitor).schedule();
 * }
 * </pre>
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.0
 */
public class UninstallOperation extends ProfileChangeOperation {

	private Collection<IInstallableUnit> toUninstall;

	/**
	 * Create an uninstall operation on the specified provisioning session that uninstalls
	 * the specified IInstallableUnits.  Unless otherwise specified, the operation will
	 * be associated with the currently running profile.
	 * 
	 * @param session the session to use for obtaining provisioning services
	 * @param toUninstall the IInstallableUnits to be installed into the profile.
	 */
	public UninstallOperation(ProvisioningSession session, Collection<IInstallableUnit> toUninstall) {
		super(session);
		this.toUninstall = toUninstall;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.operations.ProfileChangeOperation#computeProfileChangeRequest(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void computeProfileChangeRequest(MultiStatus status, IProgressMonitor monitor) {
		request = ProfileChangeRequest.createByProfileId(session.getProvisioningAgent(), profileId);
		request.removeAll(toUninstall);
		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=255984
		// We ask to remove the the profile root property in addition to removing the IU.  In theory this
		// should be redundant, but there are cases where the planner decides not to uninstall something because
		// it is needed by others.  We still want to remove the root in this case.
		for (IInstallableUnit iuToUninstall : toUninstall) {
			request.removeInstallableUnitProfileProperty(iuToUninstall, IProfile.PROP_PROFILE_ROOT_IU);
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.operations.ProfileChangeOperation#getProvisioningJobName()
	 */
	protected String getProvisioningJobName() {
		return Messages.UninstallOperation_ProvisioningJobName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.operations.ProfileChangeOperation#getResolveJobName()
	 */
	protected String getResolveJobName() {
		return Messages.UninstallOperation_ResolveJobName;
	}

	@Override
	ProvisioningContext getFirstPassProvisioningContext() {
		ProvisioningContext pc = new ProvisioningContext(session.getProvisioningAgent());
		pc.setMetadataRepositories(new URI[0]);
		pc.setArtifactRepositories(new URI[0]);
		return pc;
	}

	@Override
	IFailedStatusEvaluator getSecondPassEvaluator() {
		return new IFailedStatusEvaluator() {
			public ProvisioningContext getSecondPassProvisioningContext(IProvisioningPlan failedPlan) {
				return context;
			}
		};
	}
}
