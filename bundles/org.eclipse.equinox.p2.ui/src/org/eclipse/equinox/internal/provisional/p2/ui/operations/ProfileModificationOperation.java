/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.ui.operations;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.*;

/**
 * Class representing a provisioning profile plan
 * 
 * @since 3.4
 */
public class ProfileModificationOperation extends ProvisioningOperation {

	ProvisioningPlan plan;
	String profileId;
	PhaseSet phaseSet;
	boolean isUser = true;
	ProvisioningContext provisioningContext;
	private String taskName;

	public ProfileModificationOperation(String label, String profileId, ProvisioningPlan plan, ProvisioningContext context) {
		this(label, profileId, plan, context, null, true);
	}

	public ProfileModificationOperation(String label, String profileId, ProvisioningPlan plan, ProvisioningContext context, PhaseSet set, boolean isUser) {
		super(label);
		this.plan = plan;
		this.profileId = profileId;
		this.provisioningContext = context;
		this.isUser = isUser;
		if (set == null)
			phaseSet = new DefaultPhaseSet();
		else
			phaseSet = set;
	}

	public String getProfileId() {
		return profileId;
	}

	protected IProfile getProfile() {
		try {
			return ProvisioningUtil.getProfile(profileId);
		} catch (ProvisionException e) {
			return null;
		}
	}

	protected IStatus doExecute(IProgressMonitor monitor) throws ProvisionException {
		String task = taskName;
		if (task == null)
			task = ""; //$NON-NLS-1$
		monitor.beginTask(task, 1000);
		try {
			return ProvisioningUtil.performProvisioningPlan(plan, phaseSet, provisioningContext, new SubProgressMonitor(monitor, 1000));
		} finally {
			monitor.done();
		}
	}

	public boolean runInBackground() {
		return true;
	}

	/**
	 * Sets the top level task name for progress when running this operation.
	 * @param label
	 */
	public void setTaskName(String label) {
		this.taskName = label;
	}

	public boolean isUser() {
		return isUser;
	}
}
