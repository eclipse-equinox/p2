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

import com.ibm.icu.text.DateFormat;
import java.util.Date;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IQueryable;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUIImages;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.QueryProvider;

/**
 * Element class for a profile snapshot
 * 
 * @since 3.4
 */
public class RollbackProfileElement extends RemoteQueriedElement {

	private String profileId;
	private long timestamp;
	private IProfile snapshot;
	private boolean isCurrent = false;

	public RollbackProfileElement(Object parent, String profileId, long timestamp) {
		super(parent);
		this.timestamp = timestamp;
		this.profileId = profileId;
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
		if (isCurrent)
			return ProvUIMessages.RollbackProfileElement_CurrentInstallation;
		return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG).format(new Date(timestamp));
	}

	public Object getAdapter(Class adapter) {
		if (adapter == IProfile.class)
			try {
				return getProfileSnapshot(new NullProgressMonitor());
			} catch (ProvisionException e) {
				handleException(e, ProvUIMessages.RollbackProfileElement_InvalidSnapshot);
			}
		return super.getAdapter(adapter);
	}

	public IProfile getProfileSnapshot(IProgressMonitor monitor) throws ProvisionException {
		if (snapshot == null) {
			snapshot = ProvisioningUtil.getProfile(profileId, timestamp);
			setQueryable(snapshot);
		}
		return snapshot;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setIsCurrentProfile(boolean current) {
		this.isCurrent = current;
	}

	public boolean isCurrentProfile() {
		return isCurrent;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.model.QueriedElement#getDefaultQueryType()
	 */
	protected int getDefaultQueryType() {
		return QueryProvider.INSTALLED_IUS;
	}

	/*
	 * The queryable is the profile snapshot
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.model.QueriedElement#getQueryable()
	 */
	public IQueryable getQueryable() {
		try {
			return getProfileSnapshot(new NullProgressMonitor());
		} catch (ProvisionException e) {
			handleException(e, null);
			return null;
		}
	}
}
