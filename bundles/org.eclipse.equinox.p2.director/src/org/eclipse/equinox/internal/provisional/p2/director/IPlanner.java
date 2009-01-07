/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.director;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;

/**
 * Planners are responsible for determining what should be done to a given 
 * profile to reshape it as requested. That is, given the current state of a 
 * profile, a description of the desired end state of that profile and metadata 
 * describing the available IUs, a planner produces a plan that lists the
 * provisioning operands that the engine should perform.  
 */
public interface IPlanner {
	public ProvisioningPlan getProvisioningPlan(ProfileChangeRequest profileChangeRequest, ProvisioningContext context, IProgressMonitor monitor);

	public ProvisioningPlan getRevertPlan(IInstallableUnit profileSnapshot, ProvisioningContext context, IProgressMonitor monitor);

	public IInstallableUnit[] updatesFor(IInstallableUnit toUpdate, ProvisioningContext context, IProgressMonitor monitor);

	public ProvisioningPlan getRevertPlan(IProfile currentProfile, IProfile revertProfile, ProvisioningContext context, IProgressMonitor monitor);

}
