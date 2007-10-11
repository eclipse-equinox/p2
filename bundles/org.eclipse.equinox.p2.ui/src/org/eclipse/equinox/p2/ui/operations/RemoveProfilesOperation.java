/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.ui.operations;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.Profile;

/**
 * Operation that removes a profile
 * 
 * @since 3.4
 */
public class RemoveProfilesOperation extends ProfileOperation {
	private boolean removed = false;

	public RemoveProfilesOperation(String label, Profile[] profiles) {
		super(label, profiles);
	}

	protected IStatus doExecute(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		for (int i = 0; i < profileIds.length; i++) {
			ProvisioningUtil.removeProfile(profileIds[i], monitor);
		}
		// assume the best if no exception
		removed = true;
		return okStatus();
	}

	protected IStatus doUndo(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		for (int i = 0; i < cachedProfiles.length; i++) {
			ProvisioningUtil.addProfile(cachedProfiles[i], monitor);
		}
		removed = false;
		return okStatus();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.operations.AbstractOperation#canExecute()
	 */
	public boolean canExecute() {
		return profileIds != null && !removed;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.operations.AbstractOperation#canUndo()
	 */
	public boolean canUndo() {
		return cachedProfiles != null && removed;
	}
}
