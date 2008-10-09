/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui;

import java.net.URI;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.RepositoryManipulationDialog;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.*;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.*;
import org.eclipse.swt.widgets.Shell;

/**
 * Provides a repository manipulator that interprets URLs as colocated
 * artifact and metadata repositories.
 * 
 * @since 3.5
 */

public class ColocatedRepositoryManipulator extends RepositoryManipulator {

	Policy policy;

	public ColocatedRepositoryManipulator(Policy policy) {
		this.policy = policy;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#getManipulatorLabel()
	 */
	public String getManipulatorLabel() {
		return ProvUIMessages.ColocatedRepositoryManipulator_ManageSites;

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#manipulateRepositories(org.eclipse.swt.widgets.Shell)
	 */
	public boolean manipulateRepositories(Shell shell) {
		new RepositoryManipulationDialog(shell, policy).open();
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#getAddOperation(java.net.URL)
	 */
	public ProvisioningOperation getAddOperation(URI repoLocation) {
		return new AddColocatedRepositoryOperation(getAddOperationLabel(), repoLocation);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#getAddOperationLabel()
	 */
	public String getAddOperationLabel() {
		return ProvUIMessages.ColocatedRepositoryManipulator_AddSiteOperationLabel;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#getKnownRepositories()
	 */
	public URI[] getKnownRepositories() {
		try {
			return ProvisioningUtil.getMetadataRepositories(policy.getQueryContext().getMetadataRepositoryFlags());
		} catch (ProvisionException e) {
			return new URI[0];
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#getRemoveOperation(java.net.URL[])
	 */
	public ProvisioningOperation getRemoveOperation(URI[] reposToRemove) {
		return new RemoveColocatedRepositoryOperation(getRemoveOperationLabel(), reposToRemove);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#getRemoveOperationLabel()
	 */
	public String getRemoveOperationLabel() {
		return ProvUIMessages.ColocatedRepositoryManipulator_RemoveSiteOperationLabel;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#getURLValidator(org.eclipse.swt.widgets.Shell)
	 */
	public RepositoryLocationValidator getRepositoryLocationValidator(Shell shell) {
		DefaultMetadataURLValidator validator = new DefaultMetadataURLValidator();
		validator.setKnownRepositoriesFlag(policy.getQueryContext().getMetadataRepositoryFlags());
		return validator;
	}

}
