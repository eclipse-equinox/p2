/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.ui.operations;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.director.ProvisioningPlan;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.ui.ProvisioningUtil;

/**
 * Class representing a provisioning profile plan
 * 
 * @since 3.4
 */
public class ProfileModificationOperation extends ProvisioningOperation {

	ProvisioningPlan plan;
	String profileId;

	public ProfileModificationOperation(String label, String id, ProvisioningPlan plan) {
		super(label);
		this.plan = plan;
		this.profileId = id;
	}

	public String getProfileId() {
		return profileId;
	}

	public Profile getProfile() {
		try {
			return ProvisioningUtil.getProfile(profileId);
		} catch (ProvisionException e) {
			return null;
		}
	}

	protected IStatus doExecute(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		return ProvisioningUtil.performProvisioningPlan(plan, getProfile(), monitor);
	}
}
