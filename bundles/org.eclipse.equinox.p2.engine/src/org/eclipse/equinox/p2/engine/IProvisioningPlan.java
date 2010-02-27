/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.engine;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;

/**
 * A provisioning plan describes a proposed set of changes to a profile. The
 * proposed changes may represent a valid and consistent set of changes, or it
 * may represent a set of changes that would cause errors if executed. In this
 * case the plan contains information about the severity and explanation for the
 * problems.
 * 
 * @since 2.0
 */
public interface IProvisioningPlan {

	/**
	 * Returns the proposed set of installable units to be added to the profile.
	 * 
	 * @return The proposed profile additions
	 */
	public abstract IQueryable<IInstallableUnit> getAdditions();

	/**
	 * Returns the provisioning context in which this plan was created.
	 * 
	 * @return The plan's provisioning context
	 */
	public ProvisioningContext getContext();

	/**
	 * Returns a plan describing the proposed set of changes to the provisioning infrastructure
	 * required by this plan.  The installer changes must be performed before this plan 
	 * can be successfully executed.
	 * 
	 * @return The installer plan.
	 */
	public abstract IProvisioningPlan getInstallerPlan();

	public abstract void setInstallerPlan(IProvisioningPlan installerPlan);

	/**
	 * Returns the profile that this plan will operate on.
	 * 
	 * @return The target profile for this plan
	 */
	public abstract IProfile getProfile();

	/**
	 * Returns the proposed set of installable units to be removed from this profile.
	 * 
	 * @return The proposed profile removals.
	 */
	public abstract IQueryable<IInstallableUnit> getRemovals();

	/**
	 * Returns the overall plan status. The severity of this status indicates
	 * whether the plan can be successfully executed or not:
	 * <ul>
	 * <li>A status of {@link IStatus#OK} indicates that the plan can be executed successfully.</li>
	 * <li>A status of {@link IStatus#INFO} or {@link IStatus#WARNING} indicates
	 * that the plan can be executed but may cause problems.</li>
	 * <li>A status of {@link IStatus#ERROR} indicates that the plan cannot be executed
	 * successfully.</li>
	 * <li>A status of {@link IStatus#CANCEL} indicates that the plan computation was
	 * canceled and is incomplete. A canceled plan cannot be executed.</li>
	 * </ul>
	 * 
	 * @return The overall plan status.
	 */
	public abstract IStatus getStatus();

	public abstract void setStatus(IStatus status);

	public void addInstallableUnit(IInstallableUnit iu);

	public void removeInstallableUnit(IInstallableUnit iu);

	public void updateInstallableUnit(IInstallableUnit from, IInstallableUnit to);

	public void setProfileProperty(String name, String value);

	public void setInstallableUnitProfileProperty(IInstallableUnit iu, String name, String value);
}