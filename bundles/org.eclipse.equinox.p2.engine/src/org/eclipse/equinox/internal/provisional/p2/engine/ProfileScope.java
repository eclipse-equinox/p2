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
package org.eclipse.equinox.internal.provisional.p2.engine;

import org.eclipse.core.internal.preferences.PreferencesService;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.security.storage.EncodingUtils;

/**
 * A profile scope contains the preferences associated with a particular profile
 * in a provisioned system.
 * @see IProfile
 */
public final class ProfileScope implements IScopeContext {

	/*
	 * String constant (value of <code>"profile"</code>) used for the 
	 * scope name for this preference scope.
	 */
	public static final String SCOPE = "profile"; //$NON-NLS-1$

	private String profileId;

	private IAgentLocation location;

	/**
	 * Create and return a new profile scope for the given profile. The given
	 * profile id must not be null. The provisioning agent of the currently running
	 * system is used.
	 * @deprecated use {@link ProfileScope#ProfileScope(IAgentLocation, String)} instead
	 */
	public ProfileScope(String profileId) {
		this(getDefaultAgent(), profileId);
	}

	/**
	 * Creates and returns a profile scope for the given profile id and agent.
	 * @param agentLocation The location of the provisioning agent to obtain profile preferences for
	 * @param profileId The id of the profile to obtain preferences for
	 */
	public ProfileScope(IAgentLocation agentLocation, String profileId) {
		super();
		Assert.isNotNull(agentLocation);
		Assert.isNotNull(profileId);
		this.profileId = profileId;
		this.location = agentLocation;
	}

	private static IAgentLocation getDefaultAgent() {
		return (IAgentLocation) ServiceHelper.getService(EngineActivator.getContext(), IAgentLocation.SERVICE_NAME);
	}

	public IPath getLocation() {
		// Null returned as the location should only be used when the profile is locked
		return null;
	}

	public String getName() {
		return SCOPE;
	}

	/*
	 * Default path hierarchy for profile nodes is /profile/<profileId>/<qualifier>.
	 * 
	 * @see org.eclipse.core.runtime.preferences.IScopeContext#getNode(java.lang.String)
	 */
	public IEclipsePreferences getNode(String qualifier) {
		if (qualifier == null)
			throw new IllegalArgumentException();
		String locationString = EncodingUtils.encodeSlashes(location.getRootLocation().toString());
		//format is /profile/{agentLocationURI}/{profileId}/qualifier
		return (IEclipsePreferences) PreferencesService.getDefault().getRootNode().node(getName()).node(locationString).node(profileId).node(qualifier);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof ProfileScope))
			return false;
		ProfileScope other = (ProfileScope) obj;
		return profileId.equals(other.profileId);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return super.hashCode() + profileId.hashCode();
	}
}
