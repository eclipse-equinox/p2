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

import com.ibm.icu.text.DateFormat;
import java.util.Date;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.rollback.FormerState;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.QueryProvider;

/**
 * Element wrapper class for an IU that represents a profile snapshot
 * from a rollback repository.  It has characteristics of an IU element,
 * in that it is stored as an IU and can be adapted to its IU.  But 
 * conceptually, it is more like a profile, in that its children are the
 * IU's that represent the content of the profile when it was snapshotted.
 * 
 * @since 3.4
 */
public class RollbackProfileElement extends RemoteQueriedElement implements IUElement {

	private IInstallableUnit iu;
	private IProfile snapshot;

	public RollbackProfileElement(Object parent, IInstallableUnit iu) {
		super(parent);
		this.iu = iu;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.model.ProvElement#getImageID(java.lang.Object)
	 */
	protected String getImageId(Object obj) {
		return ProvUIImages.IMG_PROFILE;
	}

	public String getLabel(Object o) {
		return DateFormat.getInstance().format(new Date(Long.decode(iu.getVersion().getQualifier()).longValue()));
	}

	public Object getAdapter(Class adapter) {
		if (adapter == IInstallableUnit.class)
			return iu;
		if (adapter == IProfile.class)
			try {
				return getProfileSnapshot(null);
			} catch (ProvisionException e) {
				handleException(e, ProvUIMessages.RollbackProfileElement_InvalidSnapshot);
			}
		return super.getAdapter(adapter);
	}

	public IInstallableUnit getIU() {
		return iu;
	}

	public long getSize() {
		return SIZE_UNKNOWN;
	}

	public boolean shouldShowSize() {
		return false;
	}

	public boolean shouldShowVersion() {
		return false;
	}

	public void computeSize(IProgressMonitor monitor) {
		// Should never be called, since shouldShowSize() returns false
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.query.QueriedElement#getDefaultQueryType()
	 */
	protected int getDefaultQueryType() {
		return QueryProvider.INSTALLED_IUS;
	}

	/*
	 * overridden to lazily fetch profile
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.query.QueriedElement#getQueryable()
	 */
	public IQueryable getQueryable() {
		if (queryable == null)
			try {
				queryable = getProfileSnapshot(null);
			} catch (ProvisionException e) {
				handleException(e, ProvUIMessages.RollbackProfileElement_InvalidSnapshot);
			}
		return queryable;
	}

	public IProfile getProfileSnapshot(IProgressMonitor monitor) throws ProvisionException {
		if (snapshot == null) {
			IProfile profile = ProvisioningUtil.getProfile(iu.getId());
			snapshot = FormerState.IUToProfile(iu, profile, new ProvisioningContext(), monitor);
			setQueryable(snapshot);
		}
		return snapshot;
	}

	/*
	 * overridden to check whether snapshot IU is specified rather
	 * than loading the profile via getQueryable()
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.query.QueriedElement#knowsQueryable()
	 */
	public boolean knowsQueryable() {
		return iu != null;
	}
}
