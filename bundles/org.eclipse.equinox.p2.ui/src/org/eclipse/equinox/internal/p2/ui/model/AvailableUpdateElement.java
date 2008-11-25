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
		request.removeInstallableUnits(new IInstallableUnit[] {iuToBeUpdated});
		request.addInstallableUnits(new IInstallableUnit[] {getIU()});
		return ProvisioningUtil.getProvisioningPlan(request, new ProvisioningContext(), monitor);
	}
}
