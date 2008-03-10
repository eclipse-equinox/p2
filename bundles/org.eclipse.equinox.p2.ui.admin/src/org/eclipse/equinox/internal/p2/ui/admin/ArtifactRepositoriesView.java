/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.admin;

import java.net.URL;
import java.util.ArrayList;
import org.eclipse.equinox.internal.p2.ui.admin.dialogs.AddArtifactRepositoryDialog;
import org.eclipse.equinox.internal.p2.ui.admin.preferences.PreferenceConstants;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.model.*;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.RemoveArtifactRepositoryOperation;
import org.eclipse.swt.widgets.Shell;

/**
 * This view allows users to interact with artifact repositories
 * 
 * @since 3.4
 */
public class ArtifactRepositoriesView extends RepositoriesView {

	/**
	 * 
	 */
	public ArtifactRepositoriesView() {
		// constructor
	}

	protected Object getInput() {
		return new ArtifactRepositories();
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
		return new AddArtifactRepositoryDialog(shell, getRepoFlags()).open();
	}

	protected ProvisioningOperation getRemoveOperation(Object[] elements) {
		ArrayList urls = new ArrayList();
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] instanceof RepositoryElement)
				urls.add(((RepositoryElement) elements[i]).getLocation());
		}
		return new RemoveArtifactRepositoryOperation(ProvAdminUIMessages.ArtifactRepositoriesView_RemoveRepositoryOperationLabel, (URL[]) urls.toArray(new URL[urls.size()]));
	}

	protected boolean isRepository(Object element) {
		return element instanceof ArtifactRepositoryElement;
	}

	protected int getRepoFlags() {
		if (ProvAdminUIActivator.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.PREF_HIDE_SYSTEM_REPOS))
			return IArtifactRepositoryManager.REPOSITORIES_NON_SYSTEM;
		return IArtifactRepositoryManager.REPOSITORIES_ALL;
	}

}
