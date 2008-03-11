/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.provisional.p2.ui.dialogs;

import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;

/**
 * @since 3.4
 *
 */
public class DefaultMetadataURLValidator extends URLValidator {

	protected int repoFlag;

	public DefaultMetadataURLValidator() {
		repoFlag = IMetadataRepositoryManager.REPOSITORIES_ALL;
	}

	public void setKnownRepositoriesFlag(int flag) {
		repoFlag = flag;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.dialogs.URLValidator#validateRepositoryURL(boolean)
	 */
	protected IStatus validateRepositoryURL(URL location, boolean contactRepositories, IProgressMonitor monitor) {
		IStatus duplicateStatus = Status.OK_STATUS;
		URL[] knownRepositories;
		try {
			knownRepositories = ProvisioningUtil.getMetadataRepositories(repoFlag);
		} catch (ProvisionException e) {
			knownRepositories = new URL[0];
		}
		for (int i = 0; i < knownRepositories.length; i++) {
			if (knownRepositories[i].toExternalForm().equalsIgnoreCase(location.toExternalForm())) {
				duplicateStatus = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, LOCAL_VALIDATION_ERROR, ProvUIMessages.AddRepositoryDialog_DuplicateURL, null);
				break;
			}
		}
		if (!duplicateStatus.isOK())
			return duplicateStatus;

		if (contactRepositories)
			return ProvisioningUtil.validateMetadataRepositoryLocation(location, monitor);
		return duplicateStatus;
	}
}
