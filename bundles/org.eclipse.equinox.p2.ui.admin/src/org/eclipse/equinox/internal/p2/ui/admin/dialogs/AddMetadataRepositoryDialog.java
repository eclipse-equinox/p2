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
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog that allows a metadata repository to be defined and added.
 * 
 * @since 3.4
 * 
 */
public class AddMetadataRepositoryDialog extends AddRepositoryDialog {

	class MetadataRepositoryTracker extends RepositoryTracker {

		public AddRepositoryJob getAddOperation(URI location, ProvisioningSession session) {
			return new AddMetadataRepositoryOperation(ProvAdminUIMessages.AddArtifactRepositoryDialog_OperationLabel, session, location);
		}

		public RemoveRepositoryJob getRemoveOperation(URI[] repoLocations, ProvisioningSession session) {
			return new RemoveMetadataRepositoryOperation(ProvAdminUIMessages.ArtifactRepositoriesView_RemoveRepositoryOperationLabel, session, repoLocations);
		}

		public URI[] getKnownRepositories(ProvisioningSession session) {
			return session.getMetadataRepositoryManager().getKnownRepositories(getArtifactRepositoryFlags());
		}

		protected IStatus validateRepositoryLocationWithManager(ProvisioningSession session, URI location, IProgressMonitor monitor) {
			return Status.OK_STATUS;
		}
	}

	RepositoryTracker tracker;

	public AddMetadataRepositoryDialog(Shell parentShell, ProvisioningUI ui) {
		super(parentShell, ui);
	}

	protected RepositoryTracker getRepositoryTracker() {
		if (tracker == null) {
			tracker = new MetadataRepositoryTracker();
		}
		return tracker;
	}
}
