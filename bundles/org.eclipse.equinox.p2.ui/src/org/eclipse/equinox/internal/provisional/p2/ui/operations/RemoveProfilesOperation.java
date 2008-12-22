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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;

/**
 * Operation that removes a profile
 * 
 * @since 3.4
 */
public class RemoveProfilesOperation extends ProvisioningOperation {
	String[] profileIds;

	public RemoveProfilesOperation(String label, String[] profileIds) {
		super(label);
		this.profileIds = profileIds;
	}

	protected IStatus doExecute(IProgressMonitor monitor) throws ProvisionException {
		for (int i = 0; i < profileIds.length; i++) {
			ProvisioningUtil.removeProfile(profileIds[i], monitor);
		}
		// assume the best if no exception
		return okStatus();
	}
}
