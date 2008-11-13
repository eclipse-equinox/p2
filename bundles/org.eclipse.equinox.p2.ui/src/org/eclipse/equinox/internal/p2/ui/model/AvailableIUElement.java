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
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.RequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.QueryProvider;

/**
 * Element wrapper class for IU's that are available for installation.
 * Used instead of the plain IU when additional information such as sizing
 * info is necessary.
 * 
 * @since 3.4
 */
public class AvailableIUElement extends QueriedElement implements IIUElement {

	IInstallableUnit iu;
	// Currently this variable is not settable due to the
	// poor performance of sizing, but it is kept here for future improvement.
	// If we reinstate the ability to compute individual sizes we would
	// probably refer to some preference or policy to decide what to do.
	// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=221087
	private static boolean shouldShowSize = false;
	long size = IIUElement.SIZE_UNKNOWN;
	String profileID;

	public AvailableIUElement(Object parent, IInstallableUnit iu, String profileID) {
		super(parent);
		this.iu = iu;
		this.profileID = profileID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.model.ProvElement#getImageID(java.lang.Object)
	 */
	protected String getImageId(Object obj) {
		return ProvUIImages.IMG_UNINSTALLED_IU;
	}

	public String getLabel(Object o) {
		return iu.getId();
	}

	public Object getAdapter(Class adapter) {
		if (adapter == IInstallableUnit.class)
			return iu;
		return super.getAdapter(adapter);
	}

	public long getSize() {
		return size;
	}

	public void computeSize(IProgressMonitor monitor) {
		if (profileID == null)
			return;
		try {
			SubMonitor mon = SubMonitor.convert(monitor, 100);
			ProvisioningPlan plan = getSizingPlan(mon.newChild(50));
			size = ProvisioningUtil.getSize(plan, profileID, mon.newChild(50));
		} catch (ProvisionException e) {
			handleException(e, ProvUIMessages.AvailableIUElement_ProfileNotFound);
			size = IIUElement.SIZE_UNAVAILABLE;
		}
	}

	protected IProfile getProfile() throws ProvisionException {
		return ProvisioningUtil.getProfile(profileID);
	}

	protected ProvisioningPlan getSizingPlan(IProgressMonitor monitor) throws ProvisionException {
		ProfileChangeRequest request = ProfileChangeRequest.createByProfileId(profileID);
		request.addInstallableUnits(new IInstallableUnit[] {getIU()});
		return ProvisioningUtil.getProvisioningPlan(request, new ProvisioningContext(), monitor);
	}

	public IInstallableUnit getIU() {
		return iu;
	}

	public boolean shouldShowSize() {
		return shouldShowSize;
	}

	public boolean shouldShowVersion() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.model.QueriedElement#getDefaultQueryType()
	 */
	protected int getDefaultQueryType() {
		return QueryProvider.AVAILABLE_IUS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.model.IUElement#getRequirements()
	 */
	public RequiredCapability[] getRequirements() {
		return iu.getRequiredCapabilities();
	}
}
