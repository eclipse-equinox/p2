/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.admin;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;

/**
 * Job that removes one or more profiles.
 *
 * @since 3.6
 */
public class RemoveProfilesJob extends ProvisioningJob {
	String[] profileIds;

	public RemoveProfilesJob(String label, ProvisioningSession session, String[] profileIds) {
		super(label, session);
		this.profileIds = profileIds;
	}

	@Override
	public IStatus runModal(IProgressMonitor monitor) {
		for (String profileId : profileIds) {
			ProvAdminUIActivator.getDefault().getProfileRegistry().removeProfile(profileId);
		}
		return Status.OK_STATUS;

	}
}
