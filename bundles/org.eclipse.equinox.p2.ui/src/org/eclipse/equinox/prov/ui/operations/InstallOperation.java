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
package org.eclipse.equinox.prov.ui.operations;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.prov.core.ProvisionException;
import org.eclipse.equinox.prov.metadata.IInstallableUnit;
import org.eclipse.equinox.prov.ui.ProvUIActivator;
import org.eclipse.equinox.prov.ui.ProvisioningUtil;
import org.eclipse.equinox.prov.ui.internal.ProvUIMessages;

/**
 * An operation that installs the specified IU's into the specified profile
 * 
 * @since 3.4
 */
public class InstallOperation extends ProfileModificationOperation {

	private boolean installed = false;

	public InstallOperation(String label, String profileID, IInstallableUnit[] ius, String entryPointName) {
		super(label, profileID, ius, entryPointName);
	}

	protected IStatus doExecute(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		IStatus status = ProvisioningUtil.install(ius, entryPointName, getProfiles()[0], monitor, uiInfo);
		if (status.isOK()) {
			installed = true;
		}
		return status;
	}

	// TODO undo is more likely a rollback than an uninstall?  Need to clarify
	protected IStatus doUndo(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		IStatus status = ProvisioningUtil.uninstall(ius, getProfiles()[0], monitor, uiInfo);
		if (status.isOK()) {
			installed = false;
		}
		return status;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.operations.AbstractOperation#canExecute()
	 */
	public boolean canExecute() {
		return isValid() && !installed;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.operations.AbstractOperation#canUndo()
	 */
	public boolean canUndo() {
		return isValid() && installed;
	}

	/*
	 * (non-Javadoc)
	 * Overridden to use the Oracle to compute the validity of an install.
	 * @see org.eclipse.equinox.prov.ui.operations.ProvisioningOperation#computeExecutionStatus(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus computeExecutionStatus(IProgressMonitor monitor) {
		try {
			if (ProvisioningUtil.canInstall(ius, getProfile(), monitor, null))
				return okStatus();
			return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, ProvUIMessages.InstallOperation_CannotInstall, null);
		} catch (ProvisionException e) {
			return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, e.getLocalizedMessage(), e);
		}
	}
}
