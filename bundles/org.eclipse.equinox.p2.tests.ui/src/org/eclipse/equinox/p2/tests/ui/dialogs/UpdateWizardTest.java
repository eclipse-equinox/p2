/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.dialogs;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.ui.dialogs.ResolutionResultsWizardPage;
import org.eclipse.equinox.internal.p2.ui.dialogs.SelectableIUsPage;
import org.eclipse.equinox.internal.p2.ui.model.AvailableUpdateElement;
import org.eclipse.equinox.internal.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.UpdateWizard;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Tests for the install wizard
 */
public class UpdateWizardTest extends AbstractProvisioningUITest {

	private static final String SELECTION_PAGE = "IUSelectionPage";

	/**
	 * Tests the wizard
	 */
	public void testWizard() {
		IUElementListRoot root = new IUElementListRoot();
		AvailableUpdateElement element = new AvailableUpdateElement(root, upgrade, top1, TESTPROFILE, true);
		root.setChildren(new Object[] {element});

		UpdateWizard wizard = new UpdateWizard(Policy.getDefault(), TESTPROFILE, root, new Object[] {element}, null, null);
		WizardDialog dialog = new WizardDialog(ProvUI.getDefaultParentShell(), wizard);
		dialog.setBlockOnOpen(false);
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
