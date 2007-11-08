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
package org.eclipse.equinox.internal.p2.ui.admin;

import java.util.ArrayList;
import org.eclipse.equinox.internal.p2.ui.admin.dialogs.AddArtifactRepositoryDialog;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.ui.model.*;
import org.eclipse.equinox.p2.ui.operations.ProvisioningOperation;
import org.eclipse.equinox.p2.ui.operations.RemoveArtifactRepositoryOperation;
import org.eclipse.jface.viewers.IContentProvider;
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

	protected IContentProvider getContentProvider() {
		return new ArtifactRepositoryContentProvider();
	}

	protected Object getInput() {
		return new AllArtifactRepositories();
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

	protected int openAddRepositoryDialog(Shell shell, Object[] elements) {
		return new AddArtifactRepositoryDialog(shell, (IArtifactRepository[]) elements).open();
	}

	protected ProvisioningOperation getRemoveOperation(Object[] elements) {
		ArrayList repos = new ArrayList();
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] instanceof IArtifactRepository) {
				repos.add(elements[i]);
			}
		}
		return new RemoveArtifactRepositoryOperation(ProvAdminUIMessages.ArtifactRepositoriesView_RemoveRepositoryOperationLabel, (IArtifactRepository[]) repos.toArray(new IArtifactRepository[repos.size()]));
	}

	protected boolean isRepository(Object element) {
		return element instanceof IArtifactRepository || element instanceof ArtifactRepositoryElement;
	}

}