/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.StringTokenizer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.Profile;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Activator;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.repository.*;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.osgi.util.NLS;

/**
 * Helper base class for dealing with repositories associated with profiles. Repositories
 * are associated with a profile by encoding the repository locations in a comma-delimited
 * list in a profile property.
 * @see AddRepositoryAction
 * @see RemoveRepositoryAction
 */
abstract class RepositoryAction extends ProvisioningAction {

	/**
	 * Returns the repository manager of the given type, or <code>null</code>
	 * if not available.
	 */
	private static IRepositoryManager getRepositoryManager(int type) {
		if (type == IRepository.TYPE_METADATA) {
			return (IRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.SERVICE_NAME);
		} else if (type == IRepository.TYPE_ARTIFACT) {
			return (IRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.SERVICE_NAME);
		}
		return null;
	}

	/**
	 * Associates the repository described by the given event with the given profile.
	 * Has no effect if the repository is already associated with this profile.
	 */
	protected void addRepositoryToProfile(Profile profile, URI location, int type) {
		String key = type == IRepository.TYPE_METADATA ? IProfile.PROP_METADATA_REPOSITORIES : IProfile.PROP_ARTIFACT_REPOSITORIES;
		String encodedURI = encodeURI(location);
		String currentRepos = profile.getProperty(key);
		if (currentRepos == null) {
			currentRepos = encodedURI;
		} else {
			//if we already have the repository location, we are done
			StringTokenizer tokens = new StringTokenizer(currentRepos, ","); //$NON-NLS-1$
			while (tokens.hasMoreTokens())
				if (tokens.nextToken().equals(encodedURI))
					return;
			//add to comma-separated list
			currentRepos = currentRepos + ',' + encodedURI;
		}
		profile.setProperty(key, currentRepos);
	}

	/**
	 * Adds the repository corresponding to the given event to the currently running instance.
	 */
	protected void addToSelf(RepositoryEvent event) {
		IRepositoryManager manager = getRepositoryManager(event.getRepositoryType());
		if (manager != null)
			manager.addRepository(event.getRepositoryLocation());
		if (!event.isRepositoryEnabled())
			manager.setEnabled(event.getRepositoryLocation(), false);
	}

	protected RepositoryEvent createEvent(Map parameters) throws CoreException {
		String parm = (String) parameters.get(ActionConstants.PARM_REPOSITORY_LOCATION);
		if (parm == null)
			throw new CoreException(Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_REPOSITORY_LOCATION, getId())));
		URI location = null;
		try {
			location = new URI(parm);
		} catch (URISyntaxException e) {
			throw new CoreException(Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_REPOSITORY_LOCATION, getId()), e));
		}
		parm = (String) parameters.get(ActionConstants.PARM_REPOSITORY_TYPE);
		if (parm == null)
			throw new CoreException(Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_REPOSITORY_TYPE, getId())));
		int type = 0;
		try {
			type = Integer.parseInt(parm);
		} catch (NumberFormatException e) {
			throw new CoreException(Util.createError(NLS.bind(Messages.parameter_not_set, ActionConstants.PARM_REPOSITORY_TYPE, getId()), e));
		}
		//default is to be enabled
		String enablement = (String) parameters.get(ActionConstants.PARM_REPOSITORY_ENABLEMENT);
		boolean enabled = enablement == null ? true : Boolean.valueOf(enablement).booleanValue();
		return new RepositoryEvent(location, type, RepositoryEvent.DISCOVERED, enabled);
	}

	/**
	 * Encodes a URI as a string, in a form suitable for storing in a comma-separated
	 * list of location strings. Any comma character in the local string is encoded.
	 */
	private String encodeURI(URI repositoryLocation) {
		char[] chars = repositoryLocation.toString().toCharArray();
		StringBuffer result = new StringBuffer(chars.length);
		for (int i = 0; i < chars.length; i++) {
			if (chars[i] == ',')
				result.append("${#").append(Integer.toString(chars[i])).append('}'); //$NON-NLS-1$
			else
				result.append(chars[i]);
		}
		return result.toString();
	}

	/**
	 * Returns the id of this action.
	 */
	protected abstract String getId();

	/**
	 * Return <code>true</code> if the given profile is the currently running profile,
	 * and <code>false</code> otherwise.
	 */
	protected boolean isSelfProfile(Profile profile) {
		//if we can't determine the current profile, assume we are running on self
		if (profile == null)
			return true;
		IProfileRegistry registry = (IProfileRegistry) ServiceHelper.getService(Activator.getContext(), IProfileRegistry.class.getName());
		if (registry == null)
			return false;
		final IProfile selfProfile = registry.getProfile(IProfileRegistry.SELF);
		//if we can't determine the self profile, assume we are running on self
		if (selfProfile == null)
			return true;
		return profile.getProfileId().equals(selfProfile.getProfileId());
	}

	/**
	 * Removes the repository corresponding to the given event from the currently running instance.
	 */
	protected void removeFromSelf(RepositoryEvent event) {
		IRepositoryManager manager = getRepositoryManager(event.getRepositoryType());
		if (manager != null)
			manager.removeRepository(event.getRepositoryLocation());
	}

	/**
	 * Removes the association between the repository described by the given event
	 * and the given profile. Has no effect if the location is not already associated with
	 * this profile.
	 */
	protected void removeRepositoryFromProfile(Profile profile, URI location, int type) {
		String key = type == IRepository.TYPE_METADATA ? IProfile.PROP_METADATA_REPOSITORIES : IProfile.PROP_ARTIFACT_REPOSITORIES;
		String encodedURI = encodeURI(location);
		String currentRepos = profile.getProperty(key);
		//if this profile has no associated repositories, we are done
		if (currentRepos == null)
			return;
		//find the matching location, if any
		StringTokenizer tokens = new StringTokenizer(currentRepos, ","); //$NON-NLS-1$
		StringBuffer result = new StringBuffer(currentRepos.length());
		boolean found = false;
		while (tokens.hasMoreTokens()) {
			final String nextLocation = tokens.nextToken();
			if (nextLocation.equals(encodedURI)) {
				found = true;
			} else {
				//add back any location not being removed
				result.append(nextLocation);
				if (tokens.hasMoreTokens())
					result.append(',');
			}
		}
		if (!found)
			return;
		profile.setProperty(key, result.toString());
	}
}
