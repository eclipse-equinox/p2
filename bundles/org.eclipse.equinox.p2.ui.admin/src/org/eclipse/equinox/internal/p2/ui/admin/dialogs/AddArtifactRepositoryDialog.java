/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.admin.dialogs;

import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.admin.*;
import org.eclipse.equinox.internal.p2.ui.dialogs.AddRepositoryDialog;
import org.eclipse.equinox.p2.operations.*;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.equinox.p2.ui.RepositoryManipulator;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog that allows an artifact repository to be defined and added.
 * 
 * @since 3.4
 * 
 */
public class AddArtifactRepositoryDialog extends AddRepositoryDialog {

	class ArtifactRepositoryManipulator extends RepositoryManipulator {

		public AddRepositoryJob getAddOperation(URI location, ProvisioningUI ui) {
			return new AddArtifactRepositoryOperation(ProvAdminUIMessages.AddArtifactRepositoryDialog_OperationLabel, ui.getSession(), location);
		}

		public RemoveRepositoryJob getRemoveOperation(URI[] repoLocations, ProvisioningUI ui) {
			return new RemoveArtifactRepositoryOperation(ProvAdminUIMessages.ArtifactRepositoriesView_RemoveRepositoryOperationLabel, ui.getSession(), repoLocations);
		}

		public boolean manipulateRepositories(Shell shell, ProvisioningUI ui) {
			// Not used in this dialog
			return false;
		}

		public URI[] getKnownRepositories(ProvisioningSession session) {
			return session.getArtifactRepositories(getArtifactRepositoryFlags());
		}

		protected IStatus validateRepositoryLocationWithManager(ProvisioningSession session, URI location, IProgressMonitor monitor) {
			return Status.OK_STATUS;
		}
	}

	RepositoryManipulator manipulator;

	public AddArtifactRepositoryDialog(Shell parentShell, ProvisioningUI ui) {
		super(parentShell, ui);
	}

	protected RepositoryManipulator getRepositoryManipulator() {
		if (manipulator == null) {
			manipulator = new ArtifactRepositoryManipulator();
		}
		return manipulator;
	}

}
