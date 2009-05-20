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
package org.eclipse.equinox.internal.p2.ui.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;

/**
 * Element wrapper class for IU's that are available for installation.
 * Used instead of the plain IU when additional information such as sizing
 * info is necessary.
 * 
 * @since 3.4
 */
public class AvailableUpdateElement extends AvailableIUElement {

	IInstallableUnit iuToBeUpdated;

	public AvailableUpdateElement(Object parent, IInstallableUnit iu, IInstallableUnit iuToBeUpdated, String profileID, boolean shouldShowChildren) {
		super(parent, iu, profileID, shouldShowChildren);
		setIsInstalled(false);
		this.iuToBeUpdated = iuToBeUpdated;
	}

	public IInstallableUnit getIUToBeUpdated() {
		return iuToBeUpdated;
	}

	protected ProvisioningPlan getSizingPlan(IProgressMonitor monitor) throws ProvisionException {
		ProfileChangeRequest request = ProfileChangeRequest.createByProfileId(profileID);
		if (iuToBeUpdated.getId().equals(getIU().getId()))
			request.removeInstallableUnits(new IInstallableUnit[] {iuToBeUpdated});
		request.addInstallableUnits(new IInstallableUnit[] {getIU()});
		return ProvisioningUtil.getProvisioningPlan(request, new ProvisioningContext(), monitor);
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
}
