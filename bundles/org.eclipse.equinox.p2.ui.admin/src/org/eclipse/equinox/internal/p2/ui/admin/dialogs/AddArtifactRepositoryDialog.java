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
package org.eclipse.equinox.internal.p2.ui.admin.dialogs;

import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.admin.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.AddRepositoryDialog;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.*;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.*;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog that allows an artifact repository to be defined and added.
 * 
 * @since 3.4
 * 
 */
public class AddArtifactRepositoryDialog extends AddRepositoryDialog {

	class ArtifactRepositoryManipulator extends RepositoryManipulator {
		RepositoryLocationValidator validator;

		public AddRepositoryOperation getAddOperation(URI location) {
			return new AddArtifactRepositoryOperation(ProvAdminUIMessages.AddArtifactRepositoryDialog_OperationLabel, location);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#getAddOperationLabel()
		 */
		public String getAddOperationLabel() {
			return ProvAdminUIMessages.AddArtifactRepositoryDialog_OperationLabel;
		}

		public String getManipulatorButtonLabel() {
			// Not used in this dialog
			return null;
		}

		public String getManipulatorLinkLabel() {
			// Not used in this dialog
			return null;
		}

		public RemoveRepositoryOperation getRemoveOperation(URI[] repoLocations) {
			return new RemoveArtifactRepositoryOperation(ProvAdminUIMessages.ArtifactRepositoriesView_RemoveRepositoryOperationLabel, repoLocations);
		}

		public String getRemoveOperationLabel() {
			return ProvAdminUIMessages.ArtifactRepositoriesView_RemoveRepositoryOperationLabel;
		}

		public RepositoryLocationValidator getRepositoryLocationValidator(Shell shell) {
			if (validator == null) {
				validator = new RepositoryLocationValidator() {
					public IStatus validateRepositoryLocation(URI location, boolean contactRepositories, IProgressMonitor monitor) {
						IStatus duplicateStatus = Status.OK_STATUS;
						URI[] knownRepositories = getKnownRepositories();
						for (int i = 0; i < knownRepositories.length; i++) {
							if (knownRepositories[i].equals(location)) {
								duplicateStatus = new Status(IStatus.ERROR, ProvAdminUIActivator.PLUGIN_ID, LOCAL_VALIDATION_ERROR, ProvAdminUIMessages.AddArtifactRepositoryDialog_DuplicateURL, null);
								break;
							}
						}
						return duplicateStatus;
					}
				};
			}
			return validator;
		}

		public boolean manipulateRepositories(Shell shell) {
			// Not used in this dialog
			return false;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#getKnownRepositories()
		 */
		public URI[] getKnownRepositories() {
			try {
				return ProvisioningUtil.getArtifactRepositories(ProvAdminUIActivator.getDefault().getPolicy().getQueryContext().getArtifactRepositoryFlags());
			} catch (ProvisionException e) {
				return new URI[0];
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#getManipulatorInstructionString()
		 */
		public String getManipulatorInstructionString() {
			// We don't have a manipulator
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.equinox.internal.provisional.p2.ui.policy.RepositoryManipulator#getSiteNotFoundCorrectionString()
		 */
		public String getRepositoryNotFoundInstructionString() {
			return ProvAdminUIMessages.AddArtifactRepositoryDialog_ManipulateRepositoryInstruction;
		}

	}

	RepositoryManipulator manipulator;

	public AddArtifactRepositoryDialog(Shell parentShell, Policy policy) {
		super(parentShell, policy);
	}

	protected RepositoryManipulator getRepositoryManipulator() {
		if (manipulator == null) {
			manipulator = new ArtifactRepositoryManipulator();
		}
		return manipulator;
	}

}
