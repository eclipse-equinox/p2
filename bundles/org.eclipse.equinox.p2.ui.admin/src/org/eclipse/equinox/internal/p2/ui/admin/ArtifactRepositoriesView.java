/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.admin;

import org.eclipse.equinox.internal.p2.ui.ProvUIProvisioningListener;
import org.eclipse.equinox.internal.p2.ui.QueryableArtifactRepositoryManager;
import org.eclipse.equinox.internal.p2.ui.admin.dialogs.AddArtifactRepositoryDialog;
import org.eclipse.equinox.internal.p2.ui.model.ArtifactRepositories;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.swt.widgets.Shell;

/**
 * This view allows users to interact with artifact repositories
 * 
 * @since 3.4
 */
public class ArtifactRepositoriesView extends RepositoriesView {

	private RepositoryTracker tracker;

	/**
	 * 
	 */
	public ArtifactRepositoriesView() {
		// constructor
	}

	@Override
	protected Object getInput() {
		return new ArtifactRepositories(getProvisioningUI(), new QueryableArtifactRepositoryManager(getProvisioningUI(), false));
	}

	@Override
	protected String getAddCommandLabel() {
		return ProvAdminUIMessages.ArtifactRepositoriesView_AddRepositoryLabel;
	}

	@Override
	protected String getAddCommandTooltip() {
		return ProvAdminUIMessages.ArtifactRepositoriesView_AddRepositoryTooltip;
	}

	@Override
	protected String getRemoveCommandTooltip() {
		return ProvAdminUIMessages.ArtifactRepositoriesView_RemoveRepositoryTooltip;
	}

	@Override
	protected int openAddRepositoryDialog(Shell shell) {
		return new AddArtifactRepositoryDialog(shell, getProvisioningUI()).open();
	}

	@Override
	protected int getListenerEventTypes() {
		return ProvUIProvisioningListener.PROV_EVENT_ARTIFACT_REPOSITORY;
	}

	@Override
	protected RepositoryTracker getRepositoryTracker() {
		if (tracker == null) {
			tracker = SingleRepositoryTracker.createArtifactRepositoryTracker(getProvisioningUI());
		}
		return tracker;
	}

}
