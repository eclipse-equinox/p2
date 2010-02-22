/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse.actions;

import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;

/**
 * An action that adds a repository to the list of known repositories.
 */
public class RemoveRepositoryAction extends RepositoryAction {
	public static final String ID = "removeRepository"; //$NON-NLS-1$

	public IStatus execute(Map<String, Object> parameters) {
		try {
			IProvisioningAgent agent = getAgent(parameters);
			IProfileRegistry registry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
			IAgentLocation agentLocation = (IAgentLocation) agent.getService(IAgentLocation.SERVICE_NAME);
			RepositoryEvent event = createEvent(parameters);
			IProfile profile = (IProfile) parameters.get(ActionConstants.PARM_PROFILE);
			if (profile != null)
				removeRepositoryFromProfile(agentLocation, profile, event.getRepositoryLocation(), event.getRepositoryType());
			//if we are provisioning into the self profile, update the current set of repositories in this configuration
			if (isSelfProfile(registry, profile))
				removeFromSelf(agent, agentLocation, event);
		} catch (CoreException e) {
			return e.getStatus();
		}
		return Status.OK_STATUS;
	}

	public IStatus undo(Map<String, Object> parameters) {
		try {
			IProvisioningAgent agent = getAgent(parameters);
			IProfileRegistry registry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
			IAgentLocation agentLocation = (IAgentLocation) agent.getService(IAgentLocation.SERVICE_NAME);
			RepositoryEvent event = createEvent(parameters);
			IProfile profile = (IProfile) parameters.get(ActionConstants.PARM_PROFILE);
			if (profile != null)
				addRepositoryToProfile(agentLocation, profile, event.getRepositoryLocation(), event.getRepositoryNickname(), event.getRepositoryType(), event.isRepositoryEnabled());
			//if we are provisioning into the self profile, update the current set of repositories in this configuration
			if (isSelfProfile(registry, profile))
				addToSelf(agent, agentLocation, event);
			return Status.OK_STATUS;
		} catch (CoreException e) {
			return e.getStatus();
		}
	}

	protected String getId() {
		return ID;
	}
}
