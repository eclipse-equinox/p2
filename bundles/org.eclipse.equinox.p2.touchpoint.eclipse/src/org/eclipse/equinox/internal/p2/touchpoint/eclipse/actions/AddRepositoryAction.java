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

import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.Profile;
import org.eclipse.equinox.internal.provisional.p2.core.repository.RepositoryEvent;

/**
 * An action that adds a repository to the list of known repositories.
 */
public class AddRepositoryAction extends RepositoryAction {
	public static final String ID = "addRepository"; //$NON-NLS-1$

	public IStatus execute(Map parameters) {
		try {
			final RepositoryEvent event = createEvent(parameters);
			Profile profile = (Profile) parameters.get(ActionConstants.PARM_PROFILE);
			if (profile != null)
				addRepositoryToProfile(profile, event.getRepositoryLocation(), event.getRepositoryNickname(), event.getRepositoryType(), event.isRepositoryEnabled());
			//if provisioning into the current profile, broadcast an event to add this repository directly to the current repository managers.
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

	public IStatus undo(Map parameters) {
		try {
			final RepositoryEvent event = createEvent(parameters);
			Profile profile = (Profile) parameters.get(ActionConstants.PARM_PROFILE);
			if (profile != null)
				removeRepositoryFromProfile(profile, event.getRepositoryLocation(), event.getRepositoryType());
			//if provisioning into the current profile, broadcast an event to add this repository directly to the current repository managers.
			if (isSelfProfile(profile))
				removeFromSelf(event);
			return Status.OK_STATUS;
		} catch (CoreException e) {
			return e.getStatus();
		}
	}
}
