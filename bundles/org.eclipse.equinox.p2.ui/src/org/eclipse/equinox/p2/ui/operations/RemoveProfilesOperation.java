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
import org.eclipse.equinox.p2.ui.ProvisioningUtil;

/**
 * Operation that removes a profile
 * 
 * @since 3.4
 */
public class RemoveProfilesOperation extends ProfileOperation {
	public RemoveProfilesOperation(String label, Profile[] profiles) {
		super(label, profiles);
	}

	protected IStatus doExecute(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		for (int i = 0; i < profileIds.length; i++) {
			ProvisioningUtil.removeProfile(profileIds[i], monitor, uiInfo);
		}
		// assume the best if no exception
		return okStatus();
	}
}
