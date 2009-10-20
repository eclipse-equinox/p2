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
package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IQueryable;
import org.eclipse.equinox.internal.provisional.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;

public class UninstallWizardPage extends ResolutionResultsWizardPage {

	public UninstallWizardPage(Policy policy, IUElementListRoot root, String profileId, PlannerResolutionOperation initialResolution) {
		super(policy, root, profileId, initialResolution);
		setTitle(ProvUIMessages.UninstallWizardPage_Title);
		setDescription(ProvUIMessages.UninstallWizardPage_Description);
	}

	protected String getOperationLabel() {
		return ProvUIMessages.UninstallIUOperationLabel;
	}

	protected String getOperationTaskName() {
		return ProvUIMessages.UninstallIUOperationTask;
	}

	protected IQueryable getQueryable(ProvisioningPlan plan) {
		return plan.getRemovals();
	}
}
