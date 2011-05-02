/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	public IStatus runModal(IProgressMonitor monitor) {
		for (int i = 0; i < profileIds.length; i++) {
			ProvAdminUIActivator.getDefault().getProfileRegistry().removeProfile(profileIds[i]);
		}
		return Status.OK_STATUS;

	}
}
