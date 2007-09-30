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

import org.eclipse.core.commands.ExecutionException;
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
public class UpdateOperation extends ProfileModificationOperation {

	private IInstallableUnit[] replacementIUs;

	public UpdateOperation(String label, String profileID, IInstallableUnit[] toUpdate, IInstallableUnit[] replacements) {
		super(label, profileID, toUpdate);
		replacementIUs = replacements;
	}

	protected IStatus doExecute(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		return ProvisioningUtil.update(ius, replacementIUs, getProfiles()[0], monitor, uiInfo);
	}

	protected IStatus doUndo(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		// won't get called because canUndo() is currently false;
		return Status.OK_STATUS;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.operations.AbstractOperation#canExecute()
	 */
	public boolean canExecute() {
		return isValid();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.commands.operations.AbstractOperation#canUndo()
	 */
	// TODO this should be implemented as a rollback 
	public boolean canUndo() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.commands.operations.IAdvancedUndoableOperation2#computeExecutionStatus(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus computeExecutionStatus(IProgressMonitor monitor) throws ExecutionException {
		if (replacementIUs == null) {
			try {
				replacementIUs = ProvisioningUtil.updatesFor(ius, getProfile(), monitor, null);
			} catch (ProvisionException e) {
				return new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, e.getLocalizedMessage(), e);
			}
		}
		if (replacementIUs.length > 0) {
			return okStatus();
		}
		return new Status(IStatus.INFO, ProvUIActivator.PLUGIN_ID, ProvUIMessages.UpdateOperation_NothingToUpdate);
	}
}
