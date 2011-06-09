/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.dialogs;

import java.util.ArrayList;
import org.eclipse.equinox.internal.p2.metadata.License;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.dialogs.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.operations.ProfileModificationJob;
import org.eclipse.equinox.p2.operations.UpdateOperation;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Tests for the install wizard
 */
public class UpdateWizardTest extends WizardTest {

	private static final String SELECTION_PAGE = "IUSelectionPage";
	private static final String RESOLUTION_PAGE = "ResolutionPage";
	private static final String MAIN_IU = "MainIU";
	IInstallableUnit main, mainUpgrade1, mainUpgrade2, mainUpgradeWithLicense;

	protected void setUp() throws Exception {
		super.setUp();
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setId(MAIN_IU);
		iu.setVersion(Version.createOSGi(1, 0, 0));
		iu.setSingleton(true);
		iu.setCapabilities(new IProvidedCapability[] {MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, MAIN_IU, iu.getVersion())});
		main = MetadataFactory.createInstallableUnit(iu);
		install(main, true, false);
		IUpdateDescriptor update = MetadataFactory.createUpdateDescriptor(MAIN_IU, new VersionRange("[1.0.0, 1.0.0]"), 0, "update description");
		mainUpgrade1 = createIU(MAIN_IU, Version.createOSGi(2, 0, 0), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, null, NO_TP_DATA, true, update, NO_REQUIRES);
		update = MetadataFactory.createUpdateDescriptor(MAIN_IU, new VersionRange("[1.0.0, 1.0.0]"), 0, "update description");
		mainUpgrade2 = createIU(MAIN_IU, Version.createOSGi(3, 0, 0), null, NO_REQUIRES, NO_PROVIDES, NO_PROPERTIES, null, NO_TP_DATA, true, update, NO_REQUIRES);
		iu = new MetadataFactory.InstallableUnitDescription();
		iu.setId(MAIN_IU);
		iu.setVersion(Version.createOSGi(4, 0, 0));
		iu.setSingleton(true);
		iu.setUpdateDescriptor(update);
		iu.setLicenses(new ILicense[] {new License(null, "Update Wizard Test License to Accept", null)});
		iu.setCapabilities(new IProvidedCapability[] {MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, MAIN_IU, iu.getVersion())});
		mainUpgradeWithLicense = MetadataFactory.createInstallableUnit(iu);
		createTestMetdataRepository(new IInstallableUnit[] {main, mainUpgrade1, mainUpgrade2, mainUpgradeWithLicense});

	}

	/**
	 * Tests the wizard when a prior resolution has been done.
	 * This is the SDK 
	 */
	public void testUpdateWizardResolved() {
		ArrayList<IInstallableUnit> iusInvolved = new ArrayList<IInstallableUnit>();
		iusInvolved.add(main);
		UpdateOperation op = getProvisioningUI().getUpdateOperation(iusInvolved, null);
		op.resolveModal(getMonitor());
		UpdateWizard wizard = new UpdateWizard(getProvisioningUI(), op, op.getSelectedUpdates(), null);
		WizardDialog dialog = new ProvisioningWizardDialog(ProvUI.getDefaultParentShell(), wizard);
		dialog.setBlockOnOpen(false);
		dialog.open();
		ProfileModificationJob longOp = null;

		try {
			SelectableIUsPage page1 = (SelectableIUsPage) wizard.getPage(SELECTION_PAGE);
			// should already have a plan
			assertTrue(page1.isPageComplete());
			ResolutionResultsWizardPage page2 = (ResolutionResultsWizardPage) wizard.getNextPage(page1);
			dialog.showPage(page2);
			assertTrue(page2.isPageComplete());

			// if another operation is scheduled for this profile, we should not be allowed to proceed
			longOp = getLongTestOperation();
			getProvisioningUI().schedule(longOp, StatusManager.LOG);
			assertTrue(page2.isPageComplete());
			// causes recalculation of plan and status
			wizard.recomputePlan(dialog);
			// can't move to next page while op is running
			assertFalse(page2.isPageComplete());
			longOp.cancel();
		} finally {
			dialog.getShell().close();
			if (longOp != null)
				longOp.cancel();
		}
	}

	public void testUpdateWizardResolvedWithLicense() {
		ArrayList<IInstallableUnit> iusInvolved = new ArrayList<IInstallableUnit>();
		iusInvolved.add(main);
		UpdateOperation op = getProvisioningUI().getUpdateOperation(iusInvolved, null);
		op.resolveModal(getMonitor());
		UpdateWizard wizard = new UpdateWizard(getProvisioningUI(), op, op.getSelectedUpdates(), null);
		ProvisioningWizardDialog dialog = new ProvisioningWizardDialog(ProvUI.getDefaultParentShell(), wizard);
		dialog.setBlockOnOpen(false);
		dialog.open();

		try {
			SelectableIUsPage page1 = (SelectableIUsPage) wizard.getPage(SELECTION_PAGE);
			// should already have a plan
			assertTrue("1.0", page1.isPageComplete());
			// simulate the next button by getting next page and showing
			IWizardPage page = page1.getNextPage();
			dialog.showPage(page);
			// license needs approval
			assertFalse("1.1", wizard.canFinish());
			// finish button should be disabled
			while (dialog.getShell().getDisplay().readAndDispatch()) {
				// run event loop
			}
			Button finishButton = dialog.testGetButton(IDialogConstants.FINISH_ID);
			assertFalse("1.2", finishButton.isEnabled());
		} finally {
			dialog.getShell().close();
		}
	}

	/**
	 * Tests the wizard when a prior resolution has been done, but is in error.
	 */
	public void testUpdateWizardResolvedError() {
		ArrayList<IInstallableUnit> iusInvolved = new ArrayList<IInstallableUnit>();
		iusInvolved.add(main);
		UpdateOperation op = getProvisioningUI().getUpdateOperation(iusInvolved, null);
		op.resolveModal(getMonitor());
		op.setSelectedUpdates(op.getPossibleUpdates());
		UpdateWizard wizard = new UpdateWizard(getProvisioningUI(), op, op.getSelectedUpdates(), null);
		ProvisioningWizardDialog dialog = new ProvisioningWizardDialog(ProvUI.getDefaultParentShell(), wizard);
		dialog.setBlockOnOpen(false);
		dialog.open();

		try {
			assertNotNull("1.0", wizard.getStartingPage());
		} finally {
			dialog.getShell().close();
		}
	}

	/**
	 * Tests the wizard when we have a successful resolution and want to open
	 * directly on the resolution page
	 */
	public void testUpdateWizardResolvedSkipSelections() {
		ArrayList<IInstallableUnit> iusInvolved = new ArrayList<IInstallableUnit>();
		iusInvolved.add(main);
		UpdateOperation op = getProvisioningUI().getUpdateOperation(iusInvolved, null);
		op.resolveModal(getMonitor());
		UpdateWizard wizard = new UpdateWizard(getProvisioningUI(), op, op.getSelectedUpdates(), null);
		wizard.setSkipSelectionsPage(true);
		ProvisioningWizardDialog dialog = new ProvisioningWizardDialog(ProvUI.getDefaultParentShell(), wizard);
		dialog.setBlockOnOpen(false);
		dialog.open();

		try {
			assertNotNull("1.0", wizard.getStartingPage());
			assertEquals("1.1", wizard.getStartingPage(), wizard.getPage(RESOLUTION_PAGE));
		} finally {
			dialog.getShell().close();
		}
	}

	/**
	 * Tests the wizard when multiple versions are available.
	 */
	public void testBug277554MultipleVersions() {
		ArrayList<IInstallableUnit> iusInvolved = new ArrayList<IInstallableUnit>();
		iusInvolved.add(main);
		UpdateOperation op = getProvisioningUI().getUpdateOperation(iusInvolved, null);
		op.resolveModal(getMonitor());
		UpdateWizard wizard = new UpdateWizard(getProvisioningUI(), op, op.getSelectedUpdates(), null);
		ProvisioningWizardDialog dialog = new ProvisioningWizardDialog(ProvUI.getDefaultParentShell(), wizard);
		dialog.setBlockOnOpen(false);
		dialog.open();

		try {
			SelectableIUsPage page1 = (SelectableIUsPage) wizard.getPage(SELECTION_PAGE);
			// should already have a plan
			assertTrue("1.0", page1.isPageComplete());
			assertEquals("1.1", 1, page1.getCheckedIUElements().length);
			ResolutionResultsWizardPage page2 = (ResolutionResultsWizardPage) wizard.getNextPage(page1);
			dialog.showPage(page2);
			// should only have one root item in the resolution page
			assertEquals("1.2", 1, findTree(page2).getItemCount());
		} finally {
			dialog.getShell().close();
		}
	}
}