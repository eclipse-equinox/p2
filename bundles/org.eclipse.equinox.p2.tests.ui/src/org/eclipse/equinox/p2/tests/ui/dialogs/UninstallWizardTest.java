/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.dialogs;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.ui.dialogs.ResolutionResultsWizardPage;
import org.eclipse.equinox.internal.p2.ui.dialogs.SelectableIUsPage;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.ProvisioningWizardDialog;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.UninstallWizard;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Tests for the install wizard
 */
public class UninstallWizardTest extends WizardTest {

	private static final String SELECTION_PAGE = "IUSelectionPage";

	/**
	 * Tests the wizard when the uninstall is preresolved.
	 * This is the normal SDK workflow.
	 */
	public void testUninstallWizardResolved() throws ProvisionException {

		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		IInstallableUnit[] iusInvolved = new IInstallableUnit[] {top1, top2};
		req.removeInstallableUnits(iusInvolved);
		PlannerResolutionOperation op = getResolvedOperation(req);
		UninstallWizard wizard = new UninstallWizard(Policy.getDefault(), TESTPROFILE, iusInvolved, op);
		WizardDialog dialog = new ProvisioningWizardDialog(ProvUI.getDefaultParentShell(), wizard);
		dialog.setBlockOnOpen(false);
		dialog.create();
		dialog.open();

		try {
			SelectableIUsPage page1 = (SelectableIUsPage) wizard.getPage(SELECTION_PAGE);
			// We should have a good plan
			assertTrue(page1.isPageComplete());
			ResolutionResultsWizardPage page2 = (ResolutionResultsWizardPage) wizard.getNextPage(page1);
			dialog.showPage(page2);
			assertTrue(page2.isPageComplete());

			// if another operation is scheduled for this profile, we should not be allowed to proceed
			Job job = ProvisioningOperationRunner.schedule(getLongTestOperation(), StatusManager.LOG);
			assertTrue(page1.isPageComplete());
			// causes recalculation of plan and status
			wizard.getNextPage(page1);
			// can't move to next page while op is running
			assertFalse(page1.isPageComplete());
			job.cancel();

		} finally {
			dialog.getShell().close();
		}
	}

	/**
	 * Tests the wizard without the resolution having been done ahead
	 * of time.  This is not the SDK workflow, but should be supported.
	 */
	public void testUninstallWizardUnresolved() {
		// This test is pretty useless right now but at least it opens the wizard
		UninstallWizard wizard = new UninstallWizard(Policy.getDefault(), TESTPROFILE, new IInstallableUnit[] {top1, top2}, null);
		WizardDialog dialog = new ProvisioningWizardDialog(ProvUI.getDefaultParentShell(), wizard);
		dialog.setBlockOnOpen(false);
		dialog.create();
		dialog.open();

		try {
			SelectableIUsPage page1 = (SelectableIUsPage) wizard.getPage(SELECTION_PAGE);
			assertFalse(page1.isPageComplete());
			// Will cause computation of a plan.
			ResolutionResultsWizardPage page2 = (ResolutionResultsWizardPage) wizard.getNextPage(page1);
			dialog.showPage(page2);
			assertTrue(page2.isPageComplete());

			// if another operation is scheduled for this profile, we should not be allowed to proceed
			Job job = ProvisioningOperationRunner.schedule(getLongTestOperation(), StatusManager.LOG);
			assertTrue(page1.isPageComplete());
			// causes recalculation of plan and status
			wizard.getNextPage(page1);
			// can't move to next page while op is running
			assertFalse(page1.isPageComplete());
			job.cancel();

		} finally {
			dialog.getShell().close();
		}
	}
}
