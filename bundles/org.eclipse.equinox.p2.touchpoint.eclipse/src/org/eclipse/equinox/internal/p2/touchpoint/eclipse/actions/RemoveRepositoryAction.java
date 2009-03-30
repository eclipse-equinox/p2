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

import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;

import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.Profile;

/**
 * An action that adds a repository to the list of known repositories.
 */
public class RemoveRepositoryAction extends RepositoryAction {
	public static final String ID = "removeRepository"; //$NON-NLS-1$

	public IStatus execute(Map parameters) {
		try {
			RepositoryEvent event = createEvent(parameters);
			Profile profile = (Profile) parameters.get(ActionConstants.PARM_PROFILE);
			if (profile != null)
				removeRepositoryFromProfile(profile, event.getRepositoryLocation(), event.getRepositoryType());
			//if we are provisioning into the self profile, update the current set of repositories in this configuration
			if (isSelfProfile(profile))
				removeFromSelf(event);
		} catch (CoreException e) {
			return e.getStatus();
		}
		return Status.OK_STATUS;
	}

	public IStatus undo(Map parameters) {
		try {
			RepositoryEvent event = createEvent(parameters);
			Profile profile = (Profile) parameters.get(ActionConstants.PARM_PROFILE);
			if (profile != null)
				addRepositoryToProfile(profile, event.getRepositoryLocation(), event.getRepositoryNickname(), event.getRepositoryType(), event.isRepositoryEnabled());
			//if we are provisioning into the self profile, update the current set of repositories in this configuration
			if (isSelfProfile(profile))
				addToSelf(event);
			return Status.OK_STATUS;
		} catch (CoreException e) {
			return e.getStatus();
		}
	}

	protected String getId() {
		return ID;
	}
}
