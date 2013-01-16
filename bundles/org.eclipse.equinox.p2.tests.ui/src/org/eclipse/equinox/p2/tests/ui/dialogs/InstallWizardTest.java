/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ericsson AB (Hamdan Msheik) - Bypass install license wizard page via plugin_customization
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.dialogs;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.equinox.internal.p2.metadata.License;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.dialogs.*;
import org.eclipse.equinox.internal.p2.ui.model.IIUElement;
import org.eclipse.equinox.internal.p2.ui.viewers.DeferredQueryContentProvider;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.operations.*;
import org.eclipse.equinox.p2.ui.AcceptLicensesWizardPage;
import org.eclipse.equinox.p2.ui.LoadMetadataRepositoryJob;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Tests for the install wizard
 */
public class InstallWizardTest extends WizardTest {

	private static final String SELECTION_PAGE = "IUSelectionPage";
	private static final String AVAILABLE_SOFTWARE_PAGE = "AvailableSoftwarePage";
	private static final String MAIN_IU = "MainIU";

	IInstallableUnit toInstall;

	protected void setUp() throws Exception {
		super.setUp();
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setId(MAIN_IU);
		iu.setProperty(InstallableUnitDescription.PROP_TYPE_GROUP, "true");
		iu.setVersion(Version.createOSGi(1, 0, 0));
		iu.setSingleton(true);
		iu.setLicenses(new ILicense[] {new License(null, "There is a license to accept!", null)});
		iu.setCapabilities(new IProvidedCapability[] {MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, MAIN_IU, iu.getVersion())});
		toInstall = MetadataFactory.createInstallableUnit(iu);
		createTestMetdataRepository(new IInstallableUnit[] {toInstall});
	}

	public void testInstallWizardResolved() {
		ArrayList<IInstallableUnit> iusInvolved = new ArrayList<IInstallableUnit>();
		iusInvolved.add(toInstall);
		InstallOperation op = new InstallOperation(getSession(), iusInvolved);
		op.setProfileId(TESTPROFILE);
		PreselectedIUInstallWizard wizard = new PreselectedIUInstallWizard(getProvisioningUI(), op, iusInvolved, null);
		ProvisioningWizardDialog dialog = new ProvisioningWizardDialog(ProvUI.getDefaultParentShell(), wizard);
		dialog.setBlockOnOpen(false);
		dialog.open();
		ProfileModificationJob longOp = null;

		try {
			SelectableIUsPage page1 = (SelectableIUsPage) wizard.getPage(SELECTION_PAGE);
			// should already have a plan
			assertTrue("1.0", page1.isPageComplete());
			// simulate the next button by getting next page and showing
			IWizardPage page = page1.getNextPage();
			dialog.showPage(page);
			// we should be ok
			assertTrue("1.1", page.isPageComplete());

			// if another operation is scheduled for this profile, we should not be allowed to proceed
			longOp = getLongTestOperation();
			getProvisioningUI().schedule(longOp, StatusManager.LOG);
			// causes recalculation of plan and status
			wizard.recomputePlan(dialog);
			// can't move to next page while op is running
			assertFalse("1.2", page.isPageComplete());
			longOp.cancel();

			// op is no longer running, recompute plan
			wizard.recomputePlan(dialog);

			// license needs approval
			assertFalse("1.4", wizard.canFinish());
			// finish button should be disabled
			dialog.updateButtons();
			Button finishButton = dialog.testGetButton(IDialogConstants.FINISH_ID);
			assertFalse("1.5", finishButton.isEnabled());
		} finally {
			dialog.getShell().close();
			if (longOp != null)
				longOp.cancel();
		}
	}

	public void testInstallWizard() throws Exception {
		ArrayList<IInstallableUnit> iusInvolved = new ArrayList<IInstallableUnit>();
		iusInvolved.add(toInstall);
		InstallOperation op = new MyNewInstallOperation(getSession(), iusInvolved);
		op.setProfileId(TESTPROFILE);
		PreselectedIUInstallWizard wizard = new PreselectedIUInstallWizard(getProvisioningUI(), op, iusInvolved, null);
		ProvisioningWizardDialog dialog = new ProvisioningWizardDialog(ProvUI.getDefaultParentShell(), wizard);
		dialog.setBlockOnOpen(false);
		dialog.open();
		ProfileModificationJob longOp = null;

		try {
			SelectableIUsPage page1 = (SelectableIUsPage) wizard.getPage(SELECTION_PAGE);
			// should already have a plan
			assertTrue("1.0", page1.isPageComplete());
			// simulate the next button by getting next page and showing
			InstallWizardPage page = (InstallWizardPage) page1.getNextPage();

			// get the operation
			Field opField = ResolutionResultsWizardPage.class.getDeclaredField("resolvedOperation");
			opField.setAccessible(true);
			assertTrue("Expected instance of MyNewInstallOperation", opField.get(page) instanceof MyNewInstallOperation);
		} finally {
			dialog.getShell().close();
			if (longOp != null)
				longOp.cancel();
		}
	}

	public void testInstallWizardWithoutLicenceBypass() throws Exception {
		ArrayList<IInstallableUnit> iusInvolved = new ArrayList<IInstallableUnit>();
		iusInvolved.add(toInstall);
		InstallOperation op = new MyNewInstallOperation(getSession(), iusInvolved);
		op.setProfileId(TESTPROFILE);
		PreselectedIUInstallWizard wizard = new PreselectedIUInstallWizard(getProvisioningUI(), op, iusInvolved, null);
		wizard.setBypassLicencePage(false);
		ProvisioningWizardDialog dialog = new ProvisioningWizardDialog(ProvUI.getDefaultParentShell(), wizard);
		dialog.setBlockOnOpen(false);
		dialog.open();
		ProfileModificationJob longOp = null;

		try {
			SelectableIUsPage selectableIUsPage = (SelectableIUsPage) wizard.getPage(SELECTION_PAGE);
			// should already have a plan
			assertTrue("1.0", selectableIUsPage.isPageComplete());
			// simulate the next button by getting next page and showing
			InstallWizardPage installWizardPage = (InstallWizardPage) selectableIUsPage.getNextPage();

			assertFalse("Licence page bypass flag must be false", wizard.isBypassLicencePage());
			IWizardPage licensePage = installWizardPage.getNextPage();
			assertTrue("Expected instance of AcceptLicensesWizardPage", licensePage instanceof AcceptLicensesWizardPage);

		} finally {
			dialog.getShell().close();
			if (longOp != null)
				longOp.cancel();
		}
	}

	public void testInstallWizardWithLicenceBypass() throws Exception {
		ArrayList<IInstallableUnit> iusInvolved = new ArrayList<IInstallableUnit>();
		iusInvolved.add(toInstall);
		InstallOperation op = new MyNewInstallOperation(getSession(), iusInvolved);
		op.setProfileId(TESTPROFILE);
		PreselectedIUInstallWizard wizard = new PreselectedIUInstallWizard(getProvisioningUI(), op, iusInvolved, null);
		wizard.setBypassLicencePage(true);
		ProvisioningWizardDialog dialog = new ProvisioningWizardDialog(ProvUI.getDefaultParentShell(), wizard);
		dialog.setBlockOnOpen(false);
		dialog.open();
		ProfileModificationJob longOp = null;

		try {
			SelectableIUsPage selectableIUsPage = (SelectableIUsPage) wizard.getPage(SELECTION_PAGE);
			// should already have a plan
			assertTrue("1.0", selectableIUsPage.isPageComplete());
			// simulate the next button by getting next page and showing
			InstallWizardPage installWizardPage = (InstallWizardPage) selectableIUsPage.getNextPage();

			assertTrue("Licence page bypass flag must be true", wizard.isBypassLicencePage());
			IWizardPage licensePage = installWizardPage.getNextPage();
			assertNull("Expected instance of AcceptLicensesWizardPage must be null", licensePage);

		} finally {
			dialog.getShell().close();
			if (longOp != null)
				longOp.cancel();
		}
	}

	private static class MyNewInstallOperation extends InstallOperation {
		public MyNewInstallOperation(ProvisioningSession session, Collection<IInstallableUnit> toInstall) {
			super(session, toInstall);
		}
	}

	/**
	 * Tests the wizard
	 */
	public void testInstallWizardUnresolved() {
		LoadMetadataRepositoryJob job = new LoadMetadataRepositoryJob(getProvisioningUI());
		getPolicy().setGroupByCategory(false);
		job.runModal(getMonitor());
		InstallWizard wizard = new InstallWizard(getProvisioningUI(), null, null, job);
		WizardDialog dialog = new ProvisioningWizardDialog(ProvUI.getDefaultParentShell(), wizard);

		dialog.create();
		dialog.setBlockOnOpen(false);
		dialog.open();

		ProfileModificationJob longOp = null;

		try {
			AvailableIUsPage page1 = (AvailableIUsPage) wizard.getPage(AVAILABLE_SOFTWARE_PAGE);

			// test initial wizard state
			assertTrue("1.0", page1.getSelectedIUs().size() == 0);
			assertFalse("1.1", page1.isPageComplete());

			// Start reaching in...
			AvailableIUGroup group = page1.testGetAvailableIUGroup();
			group.setRepositoryFilter(AvailableIUGroup.AVAILABLE_ALL, null);
			// Now manipulate the tree itself.  we are reaching way in.
			// We are trying to select everything in the repo apart from the IU we know is broken
			DeferredQueryContentProvider provider = (DeferredQueryContentProvider) group.getCheckboxTreeViewer().getContentProvider();
			provider.setSynchronous(true);
			group.getCheckboxTreeViewer().refresh();
			group.getCheckboxTreeViewer().expandAll();
			Tree tree = (Tree) group.getCheckboxTreeViewer().getControl();
			TreeItem[] items = tree.getItems();
			for (int i = 0; i < items.length; i++) {
				Object element = items[i].getData();
				if (element != null && element instanceof IIUElement) {
					IInstallableUnit iu = ((IIUElement) element).getIU();
					if (iu != null && iu.getId().equals(MAIN_IU)) {
						group.getCheckboxTreeViewer().setChecked(element, true);
					}
				}
			}
			// must be done this way to force notification of listeners
			group.setChecked(group.getCheckboxTreeViewer().getCheckedElements());
			assertTrue("2.0", group.getCheckedLeafIUs().length > 0);
			assertTrue("2.1", page1.isPageComplete());

			// simulate the user clicking next
			IWizardPage page = wizard.getNextPage(page1);
			dialog.showPage(page);
			assertTrue("3.0", page.isPageComplete());

			// if another operation is scheduled for this profile, we should not be allowed to proceed
			longOp = getLongTestOperation();
			getProvisioningUI().schedule(longOp, StatusManager.LOG);
			// causes recalculation of plan and status
			wizard.recomputePlan(dialog);
			// can't move to next page while op is running
			assertFalse("3.1", page.isPageComplete());
			longOp.cancel();

			// this doesn't test much, it's just calling group API to flesh out NPE's, etc.
			group.getCheckedLeafIUs();
			group.getDefaultFocusControl();
			group.getSelectedIUElements();
			group.getSelectedIUs();

		} finally {
			dialog.close();
			if (longOp != null)
				longOp.cancel();
		}
	}
}
