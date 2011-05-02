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

import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;

/**
 * Operation that adds the given profile to the profile registry.
 * 
 * @since 3.6
 */
public class AddProfileJob extends ProvisioningJob {
	private String profileId;
	private Map<String, String> profileProperties;

	public AddProfileJob(String label, ProvisioningSession session, String profileId, Map<String, String> profileProperties) {
		super(label, session);
		this.profileId = profileId;
		this.profileProperties = profileProperties;
	}

	public IStatus runModal(IProgressMonitor monitor) {
		IStatus status = Status.OK_STATUS;
		try {
			ProvAdminUIActivator.getDefault().getProfileRegistry().addProfile(profileId, profileProperties);
		} catch (ProvisionException e) {
			status = getErrorStatus(null, e);
		}
		return status;
	}
}
