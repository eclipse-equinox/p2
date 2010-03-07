/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
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

import java.net.URI;
import java.util.Collection;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.repository.IRepository;

/**
 * Element wrapper class for IU's that are available for installation.
 * Used instead of the plain IU when additional information such as sizing
 * info is necessary.
 * 
 * @since 3.4
 */
public class AvailableIUElement extends QueriedElement implements IIUElement {

	IInstallableUnit iu;
	boolean shouldShowChildren;
	boolean isInstalled = false;
	boolean isUpdate = false;

	// Currently this variable is not settable due to the
	// poor performance of sizing, but it is kept here for future improvement.
	// If we reinstate the ability to compute individual sizes we would
	// probably refer to some preference or policy to decide what to do.
	// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=221087
	private static boolean shouldShowSize = false;
	long size = ProvUI.SIZE_UNKNOWN;
	String profileID;

	public AvailableIUElement(Object parent, IInstallableUnit iu, String profileID, boolean showChildren) {
		super(parent);
		this.iu = iu;
		this.profileID = profileID;
		this.shouldShowChildren = showChildren;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.model.ProvElement#getImageID(java.lang.Object)
	 */
	protected String getImageId(Object obj) {
		if (isUpdate)
			return ProvUIImages.IMG_UPDATED_IU;
		else if (isInstalled)
			return ProvUIImages.IMG_DISABLED_IU;
		return ProvUIImages.IMG_IU;
	}

	public String getLabel(Object o) {
		return iu.getId();
	}

	@SuppressWarnings("rawtypes")
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
		SubMonitor mon = SubMonitor.convert(monitor, 100);
		IProvisioningPlan plan = getSizingPlan(mon.newChild(50));
		size = ProvUI.getSize(getEngine(), plan, getProvisioningContext(), mon.newChild(50));
	}

	protected IProfile getProfile() {
		return getProfileRegistry().getProfile(profileID);
	}

	protected IProvisioningPlan getSizingPlan(IProgressMonitor monitor) {
		ProfileChangeRequest request = ProfileChangeRequest.createByProfileId(getProvisioningUI().getSession().getProvisioningAgent(), profileID);
		request.add(getIU());
		return getPlanner().getProvisioningPlan(request, getProvisioningContext(), monitor);
	}

	IEngine getEngine() {
		return ProvUI.getEngine(getProvisioningUI().getSession());
	}

	IPlanner getPlanner() {
		return (IPlanner) getProvisioningUI().getSession().getProvisioningAgent().getService(IPlanner.SERVICE_NAME);
	}

	IProfileRegistry getProfileRegistry() {
		return ProvUI.getProfileRegistry(getProvisioningUI().getSession());
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
	public Collection<IRequirement> getRequirements() {
		return iu.getRequirements();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.model.IIUElement#shouldShowChildren()
	 */
	public boolean shouldShowChildren() {
		return shouldShowChildren;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof AvailableIUElement))
			return false;
		if (iu == null)
			return false;
		return iu.equals(((AvailableIUElement) obj).getIU());
	}

	public int hashCode() {
		if (iu == null)
			return 0;
		return iu.hashCode();
	}

	public String toString() {
		if (iu == null)
			return "NULL"; //$NON-NLS-1$
		return iu.toString();
	}

	public void setIsInstalled(boolean isInstalled) {
		this.isInstalled = isInstalled;
	}

	public boolean isInstalled() {
		return isInstalled;
	}

	public void setIsUpdate(boolean isUpdate) {
		this.isUpdate = isUpdate;
	}

	public boolean isUpdate() {
		return isUpdate;
	}

	private ProvisioningContext getProvisioningContext() {
		if (hasQueryable() && getQueryable() instanceof IRepository<?>)
			return new ProvisioningContext(new URI[] {((IRepository<?>) getQueryable()).getLocation()});
		return new ProvisioningContext();
	}
}
