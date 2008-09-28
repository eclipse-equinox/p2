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
package org.eclipse.equinox.internal.provisional.p2.ui.operations;

import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;

/**
 * Operation that adds the given profile to the profile registry.
 * 
 * @since 3.4
 */
public class AddProfileOperation extends ProfileOperation {
	private boolean added = false;
	private String profileId;
	private Map profileProperties;

	public AddProfileOperation(String label, IProfile profile) {
		super(label, new IProfile[0]);
		this.profileId = profile.getProfileId();
		this.profileProperties = profile.getProperties();
	}

	public AddProfileOperation(String label, String profileId, Map profileProperties) {
		super(label, new IProfile[0]);
		this.profileId = profileId;
		this.profileProperties = profileProperties;

	}

	protected IStatus doExecute(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		ProvisioningUtil.addProfile(profileId, profileProperties, monitor);
		added = true;
		return okStatus();
	}

	protected IStatus doUndo(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		ProvisioningUtil.removeProfile(profileIds[0], monitor);
		added = false;
		return okStatus();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.operations.AbstractOperation#canExecute()
	 */
	public boolean canExecute() {
		return super.canExecute() && !added;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.operations.AbstractOperation#canUndo()
	 */
	public boolean canUndo() {
		return super.canUndo() && added;
	}
}
