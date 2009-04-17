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

package org.eclipse.equinox.internal.p2.ui;

import java.io.File;
import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.repository.helpers.RepositoryHelper;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryLocationValidator;

/**
 * @since 3.4
 *
 */
public class DefaultMetadataURLValidator extends RepositoryLocationValidator {

	protected int repoFlag;

	public DefaultMetadataURLValidator() {
		repoFlag = IRepositoryManager.REPOSITORIES_ALL;
	}

	public void setKnownRepositoriesFlag(int flag) {
		repoFlag = flag;
	}

	protected URI[] getKnownLocations() {
		URI[] knownRepositories;
		try {
			knownRepositories = ProvisioningUtil.getMetadataRepositories(repoFlag);
		} catch (ProvisionException e) {
			knownRepositories = new URI[0];
		}
		return knownRepositories;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.dialogs.URLValidator#validateRepositoryURL(boolean)
	 */
	public IStatus validateRepositoryLocation(URI location, boolean contactRepositories, IProgressMonitor monitor) {

		// First validate syntax issues
		IStatus localValidationStatus = RepositoryHelper.checkRepositoryLocationSyntax(location);
		if (!localValidationStatus.isOK()) {
			// bad syntax, but it could just be non-absolute.
			// In this case, use the helper
			String locationString = URIUtil.toUnencodedString(location);
			if (locationString.length() > 0 && (locationString.charAt(0) == '/' || locationString.charAt(0) == File.separatorChar)) {
				location = RepositoryHelper.localRepoURIHelper(location);
				localValidationStatus = RepositoryHelper.checkRepositoryLocationSyntax(location);
			}
		}

		if (!localValidationStatus.isOK())
			return localValidationStatus;

		// Syntax was ok, now look for duplicates
		URI[] knownRepositories = getKnownLocations();
		for (int i = 0; i < knownRepositories.length; i++) {
			if (knownRepositories[i].equals(location)) {
				localValidationStatus = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, LOCAL_VALIDATION_ERROR, ProvUIMessages.AddRepositoryDialog_DuplicateURL, null);
				break;
			}
		}
		if (!localValidationStatus.isOK())
			return localValidationStatus;

		if (contactRepositories)
			return ProvisioningUtil.validateMetadataRepositoryLocation(location, monitor);

		return localValidationStatus;
	}
}
