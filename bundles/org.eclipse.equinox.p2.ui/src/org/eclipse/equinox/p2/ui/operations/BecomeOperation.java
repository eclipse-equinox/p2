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
import org.eclipse.equinox.p2.ui.ProvisioningUtil;

/**
 * An operation that installs the specified IU's into the specified profile
 * 
 * @since 3.4
 */
public class BecomeOperation extends ProfileModificationOperation {

	private boolean installed = false;

	public BecomeOperation(String label, String profileID, IInstallableUnit toBecome) {
		super(label, profileID, new IInstallableUnit[] {toBecome}, null);
	}

	protected IStatus doExecute(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		IStatus status = ProvisioningUtil.become(ius[0], getProfiles()[0], monitor);
		if (status.isOK()) {
			installed = true;
		}
		return status;
	}

	// TODO We need to see what this means in the light of engine rollback
	protected IStatus doUndo(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		return Status.OK_STATUS;
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
	 * @see org.eclipse.equinox.p2.ui.operations.ProvisioningOperation#computeExecutionStatus(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus computeExecutionStatus(IProgressMonitor monitor) {
		//TODO Need to do the proper thing here
		return Status.OK_STATUS;
	}
}
