/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.operations;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.engine.*;

/**
 * A job that modifies a profile according to a specified provisioning plan.  
 * 
 * @since 2.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ProfileModificationJob extends ProvisioningJob implements IProfileChangeJob {

	IProvisioningPlan plan;
	String profileId;
	IPhaseSet phaseSet = PhaseSetFactory.createDefaultPhaseSet();
	ProvisioningContext provisioningContext;
	int restartPolicy = ProvisioningJob.RESTART_OR_APPLY;
	private String taskName;

	/**
	 * Create a job that will update a profile according to the specified provisioning plan.
	 * 
	 * @param name the name of the job
	 * @param session the provisioning session to use to obtain provisioning services
	 * @param profileId the id of the profile to be altered
	 * @param plan the provisioning plan describing how the profile is to be altered
	 * @param context the provisioning context describing how the operation is to be performed
	 */
	public ProfileModificationJob(String name, ProvisioningSession session, String profileId, IProvisioningPlan plan, ProvisioningContext context) {
		super(name, session);
		this.plan = plan;
		this.profileId = profileId;
		this.provisioningContext = context;
	}

	/**
	 * Set the phase set to be used when running the provisioning plan.  This method need only
	 * be used when the default phase set is not sufficient.  For example, clients could 
	 * use this method to perform a sizing or to download artifacts without provisioning them.
	 * 
	 * @param phaseSet the provisioning phases to be run during provisioning.
	 */
	public void setPhaseSet(IPhaseSet phaseSet) {
		this.phaseSet = phaseSet;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.p2.operations.IProfileChangeJob#getProfileId()
	 */
	public String getProfileId() {
		return profileId;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.p2.operations.ProvisioningJob#runModal(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus runModal(IProgressMonitor monitor) {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		String task = taskName;
		IStatus status = Status.OK_STATUS;
		if (task == null)
			task = getName();
		monitor.beginTask(task, 1000);
		try {
			status = getSession().performProvisioningPlan(plan, phaseSet, provisioningContext, new SubProgressMonitor(monitor, 1000));
		} finally {
			monitor.done();
		}
		return status;
	}

	/**
	 * Sets the top level task name for progress when running this operation.
	 * 
	 * @param label the label to be used for the task name
	 */
	public void setTaskName(String label) {
		this.taskName = label;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.p2.operations.ProvisioningJob#getRestartPolicy()
	 */
	public int getRestartPolicy() {
		//if we are installing into self we must always use the restart policy
		if (IProfileRegistry.SELF.equals(profileId))
			return restartPolicy;
		//otherwise only apply restart policy if the profile being modified is the one that is currently running
		IProfile profile = getSession().getProfileRegistry().getProfile(IProfileRegistry.SELF);
		String id = profile == null ? null : profile.getProfileId();
		if (id != null && profileId.equals(id)) {
			return restartPolicy;
		}
		return ProvisioningJob.RESTART_NONE;
	}

	/**
	 * Set the restart policy that describes whether restart is needed after
	 * performing this job.  This policy will be consulted when the 
	 * profile being changed is the profile of the running system.
	 * 
	 * @param policy an integer describing the restart policy
	 * @see ProvisioningJob#RESTART_NONE
	 * @see ProvisioningJob#RESTART_ONLY
	 * @see ProvisioningJob#RESTART_OR_APPLY
	 */
	public void setRestartPolicy(int policy) {
		restartPolicy = policy;
	}
}
