/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Fix compiler problems from generified IAdaptable#getAdapter
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.model;

import java.text.DateFormat;
import java.util.Date;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.query.IQueryable;

/**
 * Element class for a profile snapshot
 *
 * @since 3.4
 */
public class RollbackProfileElement extends RemoteQueriedElement {

	private final String profileId;
	private final long timestamp;
	private IProfile snapshot;
	private boolean isCurrent = false;
	private String profileTag;

	public RollbackProfileElement(Object parent, String profileId, long timestamp) {
		this(parent, profileId, timestamp, null);
	}

	public RollbackProfileElement(Object parent, String profileId, long timestamp, String profileTag) {
		super(parent);
		this.timestamp = timestamp;
		this.profileId = profileId;
		this.profileTag = profileTag;
	}

	@Override
	protected String getImageId(Object obj) {
		return ProvUIImages.IMG_PROFILE;
	}

	@Override
	public String getLabel(Object o) {
		if (isCurrent) {
			return ProvUIMessages.RollbackProfileElement_CurrentInstallation;
		}
		return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG).format(new Date(timestamp));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IProfile.class) {
			return (T) getProfileSnapshot(new NullProgressMonitor());
		}
		return super.getAdapter(adapter);
	}

	public IProfile getProfileSnapshot(IProgressMonitor monitor) {
		if (snapshot == null) {
			snapshot = ProvUI.getProfileRegistry(getProvisioningUI().getSession()).getProfile(profileId, timestamp);
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

	public String getProfileId() {
		return profileId;
	}

	public String getProfileTag() {
		return profileTag;
	}

	public void setProfileTag(String profileTag) {
		this.profileTag = profileTag;
	}

	@Override
	protected int getDefaultQueryType() {
		return QueryProvider.INSTALLED_IUS;
	}

	@Override
	public IQueryable<?> getQueryable() {
		return getProfileSnapshot(new NullProgressMonitor());
	}
}
