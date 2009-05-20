/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.dialogs;

import java.util.HashSet;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.metadata.License;
import org.eclipse.equinox.internal.p2.ui.dialogs.*;
import org.eclipse.equinox.internal.p2.ui.model.IIUElement;
import org.eclipse.equinox.internal.p2.ui.viewers.DeferredQueryContentProvider;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.*;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IUViewQueryContext;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
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
	private static final String BROKEN_IU = "RCP_Browser_Example.feature.group";
	private static final String MAIN_IU = "MainIU";

	public void testInstallWizardResolved() throws ProvisionException {
		InstallableUnitDescription iu = new MetadataFactory.InstallableUnitDescription();
		iu.setId(MAIN_IU);
		iu.setVersion(new Version(1, 0, 0));
		iu.setSingleton(true);
		iu.setLicense(new License(null, "There is a license to accept!"));
		iu.setCapabilities(new IProvidedCapability[] {MetadataFactory.createProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, MAIN_IU, iu.getVersion())});
		IInstallableUnit toInstall = MetadataFactory.createInstallableUnit(iu);
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {toInstall});
		PlannerResolutionOperation op = getResolvedOperation(request);
		PreselectedIUInstallWizard wizard = new PreselectedIUInstallWizard(Policy.getDefault(), TESTPROFILE, new IInstallableUnit[] {toInstall}, op, null);
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
			Button finishButton = dialog.testGetButton(IDialogConstants.FINISH_ID);
			assertFalse("1.2", finishButton.isEnabled());
		} finally {
			dialog.getShell().close();
		}
	}

	/**
	 * Tests the wizard
	 */
	public void testInstallWizardUnresolved() {
		Policy policy = Policy.getDefault();
		IUViewQueryContext context = policy.getQueryContext();
		context.setViewType(IUViewQueryContext.AVAILABLE_VIEW_FLAT);
		QueryableMetadataRepositoryManager manager = new QueryableMetadataRepositoryManager(context, false);
		manager.loadAll(getMonitor());
		InstallWizard wizard = new InstallWizard(policy, TESTPROFILE, null, null, manager);
		WizardDialog dialog = new ProvisioningWizardDialog(ProvUI.getDefaultParentShell(), wizard);

		dialog.create();
		dialog.setBlockOnOpen(false);
		dialog.open();

		try {
			AvailableIUsPage page1 = (AvailableIUsPage) wizard.getPage(AVAILABLE_SOFTWARE_PAGE);

			// test initial wizard state
			assertTrue(page1.getSelectedIUs().length == 0);
			assertFalse(page1.isPageComplete());

			// Start reaching in...
			AvailableIUGroup group = page1.testGetAvailableIUGroup();
			group.setRepositoryFilter(AvailableIUGroup.AVAILABLE_ALL, null);
			// Now manipulate the tree itself.  we are reaching way in.
			DeferredQueryContentProvider provider = (DeferredQueryContentProvider) group.getCheckboxTreeViewer().getContentProvider();
			provider.setSynchronous(true);
			group.getCheckboxTreeViewer().refresh();
			group.getCheckboxTreeViewer().expandAll();
			Tree tree = (Tree) group.getCheckboxTreeViewer().getControl();
			TreeItem[] items = tree.getItems();
			HashSet ids = new HashSet();
			ids.add(BROKEN_IU);
			for (int i = 0; i < items.length; i++) {
				Object element = items[i].getData();
				if (element != null && element instanceof IIUElement) {
					IInstallableUnit iu = ((IIUElement) element).getIU();
					if (iu != null && !ids.contains(iu.getId())) {
						ids.add(iu.getId());
						group.getCheckboxTreeViewer().setChecked(element, true);
					}
				}
			}
			// must be done this way to force notification of listeners
			group.setChecked(group.getCheckboxTreeViewer().getCheckedElements());

			IWizardPage page = wizard.getNextPage(page1);
			assertTrue(page instanceof IResolutionErrorReportingPage);
			IResolutionErrorReportingPage page2 = (IResolutionErrorReportingPage) page;
			assertTrue(group.getCheckedLeafIUs().length > 0);
			dialog.showPage(page2);

			// if another operation is scheduled for this profile, we should not be allowed to proceed
			Job job = ProvisioningOperationRunner.schedule(getLongTestOperation(), StatusManager.LOG);
			assertTrue(page1.isPageComplete());

			// causes recalculation of plan and status
			dialog.showPage(page1);
			wizard.getNextPage(page1);
			assertTrue(page1.isPageComplete());
			assertFalse(page2.isPageComplete());
			job.cancel();

			// this doesn't test much, it's just calling group API to flesh out NPE's, etc.
			group.getCheckedLeafIUs();
			group.getDefaultFocusControl();
			group.getSelectedIUElements();
			group.getSelectedIUs();

		} finally {
			dialog.close();
		}
	}
}
