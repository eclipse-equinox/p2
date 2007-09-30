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
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnitConstants;
import org.eclipse.equinox.p2.ui.ProvisioningUtil;

/**
 * An operation that uninstalls the specified IU's from the specified profile
 * 
 * @since 3.4
 */
public class UninstallOperation extends ProfileModificationOperation {

	private boolean installed = true;

	public UninstallOperation(String label, String profileID, IInstallableUnit[] ius) {
		super(label, profileID, ius);
	}

	protected IStatus doExecute(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		for (int i = 0; i < ius.length; i++) {
			if (entryPointName == null) {
				String prop = ius[i].getProperty(IInstallableUnitConstants.ENTRYPOINT_IU_KEY);
				if (prop != null && Boolean.valueOf(prop).booleanValue()) {
					entryPointName = ius[i].getProperty(IInstallableUnitConstants.NAME);
				}
			} else {
				break;
			}
		}
		IStatus status = ProvisioningUtil.uninstall(ius, getProfiles()[0], monitor, uiInfo);
		if (status.isOK()) {
			installed = false;
		}
		return status;
	}

	protected IStatus doUndo(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		IStatus status = ProvisioningUtil.install(ius, entryPointName, getProfiles()[0], monitor, uiInfo);
		if (status.isOK()) {
			installed = true;
		}
		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.operations.AbstractOperation#canExecute()
	 */
	public boolean canExecute() {
		// TODO should make sure it's actually installed in the profile
		return isValid() && installed;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.operations.AbstractOperation#canUndo()
	 */
	public boolean canUndo() {
		return isValid() && !installed;
	}
}
