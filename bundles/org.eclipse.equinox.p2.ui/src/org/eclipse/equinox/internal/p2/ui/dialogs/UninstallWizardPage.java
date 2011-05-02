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
package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.UninstallOperation;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

public class UninstallWizardPage extends ResolutionResultsWizardPage {

	public UninstallWizardPage(ProvisioningUI ui, ProvisioningOperationWizard wizard, IUElementListRoot root, UninstallOperation initialResolution) {
		super(ui, wizard, root, initialResolution);
		setTitle(ProvUIMessages.UninstallWizardPage_Title);
		setDescription(ProvUIMessages.UninstallWizardPage_Description);
	}

	protected String getOperationLabel() {
		return ProvUIMessages.UninstallIUOperationLabel;
	}

	protected String getOperationTaskName() {
		return ProvUIMessages.UninstallIUOperationTask;
	}

	protected IQueryable<IInstallableUnit> getQueryable(IProvisioningPlan plan) {
		return plan.getRemovals();
	}
}
