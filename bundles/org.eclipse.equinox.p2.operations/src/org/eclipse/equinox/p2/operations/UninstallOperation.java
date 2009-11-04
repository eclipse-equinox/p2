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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.operations.Messages;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;

/**
 * @since 2.0
 *
 */
public class UninstallOperation extends ProfileChangeOperation {

	private IInstallableUnit[] toUninstall;

	public UninstallOperation(ProvisioningSession session, String profileId, String rootMarkerKey, ProvisioningContext context, IInstallableUnit[] toInstall) {
		super(session, profileId, rootMarkerKey, context);
		this.toUninstall = toInstall;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.operations.ProfileChangeOperation#computeProfileChangeRequest(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void computeProfileChangeRequest(IProgressMonitor monitor) {
		request = ProfileChangeRequest.createByProfileId(profileId);
		request.removeInstallableUnits(toUninstall);
		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=255984
		// We ask to remove the the profile root property in addition to removing the IU.  In theory this
		// should be redundant, but there are cases where the planner decides not to uninstall something because
		// it is needed by others.  We still want to remove the root in this case.
		if (rootMarkerKey != null)
			for (int i = 0; i < toUninstall.length; i++)
				request.removeInstallableUnitProfileProperty(toUninstall[i], rootMarkerKey);

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

}
