/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.examples.rcp.cloud.p2;

import org.eclipse.equinox.p2.operations.PreloadMetadataRepositoryJob;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.ui.RepositoryManipulator;

/**
 * UpdateHandler invokes the check for updates UI
 */
public class UpdateHandler extends PreloadingRepositoryHandler {

	protected void doExecute(PreloadMetadataRepositoryJob job) {
		UpdateOperation operation = getProvisioningUI().getUpdateOperation(null, null);
		// check for updates
		operation.resolveModal(null);
		if (getProvisioningUI().getPolicy().continueWorkingWithOperation(operation, getShell())) {
			getProvisioningUI().openUpdateWizard(getShell(), true, operation, job);
		}
	}

	protected boolean preloadRepositories() {
		RepositoryManipulator repoMan = getProvisioningUI().getPolicy().getRepositoryManipulator();
		if (repoMan.getKnownRepositories(getProvisioningUI().getSession()).length == 0) {
			return false;
		}
		return super.preloadRepositories();
	}
}
