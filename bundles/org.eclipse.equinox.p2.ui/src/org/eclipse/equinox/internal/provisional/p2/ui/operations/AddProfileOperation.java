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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;

/**
 * Operation that adds the given profile to the profile registry.
 * 
 * @since 3.4
 */
public class AddProfileOperation extends ProvisioningOperation {
	private String profileId;
	private Map profileProperties;

	public AddProfileOperation(String label, String profileId, Map profileProperties) {
		super(label);
		this.profileId = profileId;
		this.profileProperties = profileProperties;
	}

	protected IStatus doExecute(IProgressMonitor monitor) throws ProvisionException {
		ProvisioningUtil.addProfile(profileId, profileProperties, monitor);
		return okStatus();
	}
}
