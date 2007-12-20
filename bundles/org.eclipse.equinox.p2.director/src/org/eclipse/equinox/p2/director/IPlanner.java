/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.director;

import java.net.URL;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * Planners are responsible for determining what should be done to a given 
 * profile to reshape it as requested. That is, given the current state of a 
 * profile, a description of the desired end state of that profile and metadata 
 * describing the available IUs, a planner produces a plan that lists the
 * provisioning operands that the engine should perform.  
 */
public interface IPlanner {
	/**
	 * Provides a plan for installing the given units into the given profile.
	 * 
	 * @param toInstall The units to install
	 * @param profile The profile to install into
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 */
	public ProvisioningPlan getInstallPlan(IInstallableUnit[] toInstall, Profile profile, URL[] metadataRepositories, IProgressMonitor monitor);

	/**
	 * Provides a plan for uninstalling the given units from the given profile.
	 * 
	 * @param toUninstall The units to uninstall
	 * @param profile The profile from which to uninstall
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting is not desired
	 */
	public ProvisioningPlan getUninstallPlan(IInstallableUnit[] toUninstall, Profile profile, URL[] metadataRepositories, IProgressMonitor monitor);

	public ProvisioningPlan getBecomePlan(IInstallableUnit target, Profile profile, URL[] metadataRepositories, IProgressMonitor monitor);

	public ProvisioningPlan getReplacePlan(IInstallableUnit[] toUninstall, IInstallableUnit[] toInstall, Profile profile, URL[] metadataRepositories, IProgressMonitor monitor);

	public ProvisioningPlan getRevertPlan(IInstallableUnit previous, Profile profile, URL[] metadataRepositories, IProgressMonitor monitor);

	public IInstallableUnit[] updatesFor(IInstallableUnit toUpdate, URL[] metadataRepositories);

}
