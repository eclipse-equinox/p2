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
import org.eclipse.equinox.p2.director.ProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.ProvisioningUtil;

/**
 * An operation that installs the specified IU's into the specified profile
 * 
 * @since 3.4
 */
public class InstallOperation extends ProfileModificationOperation {

	IInstallableUnit[] installRoots;

	public InstallOperation(String label, String profileID, ProvisioningPlan plan, IInstallableUnit[] ius) {
		super(label, profileID, plan);
		this.installRoots = ius;
	}

	protected IStatus doExecute(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		return ProvisioningUtil.performInstall(plan, getProfile(), installRoots, monitor);
	}
}
