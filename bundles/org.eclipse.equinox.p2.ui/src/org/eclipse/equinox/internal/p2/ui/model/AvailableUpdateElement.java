/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.ui.ProvUIImages;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.Update;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;

/**
 * Element wrapper class for IU's that are available for installation.
 * Used instead of the plain IU when additional information such as sizing
 * info is necessary.
 * 
 * @since 3.4
 */
public class AvailableUpdateElement extends AvailableIUElement {

	IInstallableUnit iuToBeUpdated;
	boolean isLockedForUpdate = false;

	public AvailableUpdateElement(Object parent, IInstallableUnit iu, IInstallableUnit iuToBeUpdated, String profileID, boolean shouldShowChildren) {
		super(parent, iu, profileID, shouldShowChildren);
		setIsInstalled(false);
		this.iuToBeUpdated = iuToBeUpdated;
		init();

	}

	private void init() {
		IProfileRegistry profileRegistry = (IProfileRegistry) getProvisioningUI().getSession().getProvisioningAgent().getService(IProfileRegistry.SERVICE_NAME);
		IProfile profile = profileRegistry.getProfile(profileID);
		String property = profile.getInstallableUnitProperty(iuToBeUpdated, IProfile.PROP_PROFILE_LOCKED_IU);
		try {
			isLockedForUpdate = property == null ? false : (Integer.parseInt(property) & IProfile.LOCK_UPDATE) > 0;
		} catch (NumberFormatException e) {
			isLockedForUpdate = false;
		}
	}

	public boolean isLockedForUpdate() {
		return isLockedForUpdate;
	}

	public IInstallableUnit getIUToBeUpdated() {
		return iuToBeUpdated;
	}

	protected IProvisioningPlan getSizingPlan(IProgressMonitor monitor) {
		IPlanner planner = getPlanner();
		IProfileChangeRequest request = ProfileChangeRequest.createByProfileId(getProvisioningUI().getSession().getProvisioningAgent(), profileID);
		if (iuToBeUpdated.getId().equals(getIU().getId()))
			request.remove(iuToBeUpdated);
		request.add(getIU());
		return planner.getProvisioningPlan(request, new ProvisioningContext(getProvisioningUI().getSession().getProvisioningAgent()), monitor);
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof AvailableUpdateElement))
			return false;
		if (iu == null)
			return false;
		if (iuToBeUpdated == null)
			return false;
		AvailableUpdateElement other = (AvailableUpdateElement) obj;
		return iu.equals(other.getIU()) && iuToBeUpdated.equals(other.getIUToBeUpdated());
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((iu == null) ? 0 : iu.hashCode());
		result = prime * result + ((iuToBeUpdated == null) ? 0 : iuToBeUpdated.hashCode());
		return result;
	}

	public Update getUpdate() {
		return new Update(iuToBeUpdated, getIU());
	}

	protected String getImageId(Object obj) {
		String imageId = super.getImageId(obj);
		if (ProvUIImages.IMG_IU.equals(imageId) && isLockedForUpdate())
			return ProvUIImages.IMG_DISABLED_IU;
		return imageId;
	}
}
