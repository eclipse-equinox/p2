/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
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
package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.equinox.p2.metadata.IUpdateDescriptor;

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

public class UpdateWizardPage extends SizeComputingWizardPage {

	public UpdateWizardPage(ProvisioningUI ui, ProvisioningOperationWizard wizard, IUElementListRoot root, UpdateOperation operation) {
		super(ui, wizard, root, operation);
		setTitle(ProvUIMessages.UpdateWizardPage_Title);
		setDescription(ProvUIMessages.UpdateWizardPage_Description);
	}

	@Override
	protected String getIUDescription(IInstallableUnit iu) {
		if (iu != null) {
			IUpdateDescriptor updateDescriptor = iu.getUpdateDescriptor();
			if (updateDescriptor != null && updateDescriptor.getDescription() != null && updateDescriptor.getDescription().length() > 0)
				return updateDescriptor.getDescription();
		}
		return super.getIUDescription(iu);
	}

	@Override
	protected String getOperationLabel() {
		return ProvUIMessages.UpdateIUOperationLabel;
	}

	@Override
	protected String getOperationTaskName() {
		return ProvUIMessages.UpdateIUOperationTask;
	}
}
