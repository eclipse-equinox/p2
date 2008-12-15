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

import java.util.HashSet;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.ui.dialogs.AvailableIUsPage;
import org.eclipse.equinox.internal.p2.ui.dialogs.ResolutionWizardPage;
import org.eclipse.equinox.internal.p2.ui.model.IIUElement;
import org.eclipse.equinox.internal.p2.ui.viewers.DeferredQueryContentProvider;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.AvailableIUGroup;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.InstallWizard;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IUViewQueryContext;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.equinox.p2.tests.ui.AbstractProvisioningUITest;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * Tests for the install wizard
 */
public class InstallWizardTest extends AbstractProvisioningUITest {

	private static final String AVAILABLE_SOFTWARE_PAGE = "AvailableSoftwarePage";
	private static final String BROKEN_IU = "RCP_Browser_Example.feature.group";

	/**
	 * Tests the wizard
	 */
	public void testWizard() {
		QueryableMetadataRepositoryManager manager = new QueryableMetadataRepositoryManager(Policy.getDefault(), false);
		manager.loadAll(getMonitor());

		Policy policy = Policy.getDefault();
		IUViewQueryContext context = policy.getQueryContext();
		context.setViewType(IUViewQueryContext.AVAILABLE_VIEW_FLAT);
		InstallWizard wizard = new InstallWizard(policy, TESTPROFILE, null, null, manager);
		WizardDialog dialog = new WizardDialog(ProvUI.getDefaultParentShell(), wizard);

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
			// Now manipulate the tree itself.  we are reaching way in.
			DeferredQueryContentProvider provider = (DeferredQueryContentProvider) group.getCheckboxTreeViewer().getContentProvider();
			provider.setSynchronous(true);
			group.getCheckboxTreeViewer().refresh();
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

			ResolutionWizardPage page2 = (ResolutionWizardPage) wizard.getNextPage(page1);
			assertTrue(group.getCheckedLeafIUs().length > 0);
			dialog.showPage(page2);
			assertNotNull(page2.getCurrentPlan());

			// if another operation is scheduled for this profile, we should not be allowed to proceed
			// Note this test is timing dependent, as it relies on a 1 second operation not
			// completing before the assertion.
			Job job = ProvisioningOperationRunner.schedule(getLongTestOperation(), null, StatusManager.LOG);
			assertTrue("Investigate timing problem in test", page1.isPageComplete());

			// causes recalculation of plan and status
			dialog.showPage(page1);
			wizard.getNextPage(page1);
			assertTrue(page1.isPageComplete());
			assertFalse(page2.isPageComplete());
			job.cancel();
			// relies on immediate response from job
			wizard.getNextPage(page1);
			assertTrue(page1.isPageComplete());
			assertTrue(page2.isPageComplete());

			// this doesn't test much, it's just calling group API to flesh out NPE's, etc.
			group.refresh();

			group.getCheckedLeafIUs();
			group.getDefaultFocusControl();
			group.getSelectedIUElements();
			group.getSelectedIUs();

		} finally {
			dialog.close();
		}
	}
}
