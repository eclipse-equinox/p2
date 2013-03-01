/*******************************************************************************
 *  Copyright (c) 2007, 2013 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat, Inc. - support for remediation page
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

public class InstallWizardPage extends SizeComputingWizardPage {

	public InstallWizardPage(ProvisioningUI ui, ProvisioningOperationWizard wizard, IUElementListRoot root, ProfileChangeOperation operation) {
		super(ui, wizard, root, operation);
		setTitle(ProvUIMessages.InstallWizardPage_Title);
		setDescription(ProvUIMessages.InstallWizardPage_NoCheckboxDescription);
	}

	protected String getOperationLabel() {
		return ProvUIMessages.InstallIUOperationLabel;
	}

	protected String getOperationTaskName() {
		return ProvUIMessages.InstallIUOperationTask;
	}

}
