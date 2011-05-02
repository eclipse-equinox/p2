/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
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

	protected Object getInput() {
		return new ArtifactRepositories(getProvisioningUI(), new QueryableArtifactRepositoryManager(getProvisioningUI(), false));
	}

	protected String getAddCommandLabel() {
		return ProvAdminUIMessages.ArtifactRepositoriesView_AddRepositoryLabel;
	}

	protected String getAddCommandTooltip() {
		return ProvAdminUIMessages.ArtifactRepositoriesView_AddRepositoryTooltip;
	}

	protected String getRemoveCommandTooltip() {
		return ProvAdminUIMessages.ArtifactRepositoriesView_RemoveRepositoryTooltip;
	}

	protected int openAddRepositoryDialog(Shell shell) {
		return new AddArtifactRepositoryDialog(shell, getProvisioningUI()).open();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.admin.RepositoriesView#getListenerEventTypes()
	 */
	protected int getListenerEventTypes() {
		return ProvUIProvisioningListener.PROV_EVENT_ARTIFACT_REPOSITORY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.admin.RepositoriesView#getRepositoryTracker()
	 */
	protected RepositoryTracker getRepositoryTracker() {
		if (tracker == null)
			tracker = new ArtifactRepositoryTracker(getProvisioningUI());
		return tracker;
	}

}
