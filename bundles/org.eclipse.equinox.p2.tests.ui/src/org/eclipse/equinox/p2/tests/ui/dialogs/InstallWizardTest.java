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

import org.eclipse.equinox.internal.p2.ui.dialogs.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.AvailableIUGroup;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.InstallWizard;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;
import org.eclipse.jface.wizard.WizardDialog;

/**
 * Tests for the install wizard
 */
public class InstallWizardTest extends AbstractProvisioningUITest {

	private static final String AVAILABLE_SOFTWARE_PAGE = "AvailableSoftwarePage";
	private static final String RESOLUTION_PAGE = "InstallWizardPage";
	private static final String LICENSE_PAGE = "AcceptLicenses";

	/**
	 * Tests the wizard
	 */
	public void testWizard() {
		InstallWizard wizard = new InstallWizard(Policy.getDefault(), IProfileRegistry.SELF, null, null, null);
		WizardDialog dialog = new WizardDialog(ProvUI.getDefaultParentShell(), wizard);

		dialog.create();
		dialog.setBlockOnOpen(false);
		dialog.open();

		try {
			AvailableIUsPage page1 = (AvailableIUsPage) wizard.getPage(AVAILABLE_SOFTWARE_PAGE);
			InstallWizardPage page2 = (InstallWizardPage) wizard.getPage(RESOLUTION_PAGE);
			AcceptLicensesWizardPage page3 = (AcceptLicensesWizardPage) wizard.getPage(LICENSE_PAGE);

			// test initial wizard state
			assertTrue(page1.getSelectedIUs().length == 0);
			assertFalse(page1.isPageComplete());

			// resolution page not created
			assertNull(page2);

			// no license page created yet
			assertNull(page3);

			// Test the API.  Note we aren't testing much about correctness since
			// there is not a lot of API to test the state of the UI.  However
			// this does let us get better code coverage and find exceptions/breakage.
			AvailableIUGroup group = page1.testGetAvailableIUGroup();
			group.refresh();

			// Now manipulate the tree itself.  we are reaching way in.

		} finally {
			dialog.close();
		}
	}
}
