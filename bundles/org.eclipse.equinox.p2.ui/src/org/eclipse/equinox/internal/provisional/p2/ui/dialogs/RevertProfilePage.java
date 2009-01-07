/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.ui.dialogs;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.ProfileSnapshots;
import org.eclipse.equinox.internal.p2.ui.model.RollbackProfileElement;
import org.eclipse.equinox.internal.p2.ui.viewers.DeferredQueryContentProvider;
import org.eclipse.equinox.internal.p2.ui.viewers.IUDetailsLabelProvider;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.*;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.about.IInstallationPageContainer;
import org.eclipse.ui.about.InstallationPage;
import org.eclipse.ui.menus.*;
import org.eclipse.ui.services.IServiceLocator;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * @since 3.4
 */
public class RevertProfilePage extends InstallationPage {

	private static final int DEFAULT_COLUMN_WIDTH = 150;
	TableViewer configsViewer;
	TreeViewer configContentsViewer;
	IAction revertAction;
	String profileId;
	IMenuService menuService;
	IInstallationPageContainer pageContainer;
	AbstractContributionFactory factory;
	Text detailsArea;
	InstalledIUGroup installedIUGroup;

	/* (non-Javadoc)
	 * @see org.eclipse.ui.about.InstallationPage#init(org.eclipse.ui.services.IServiceLocator)
	 */
	public void init(IServiceLocator locator) {
		pageContainer = (IInstallationPageContainer) locator.getService(IInstallationPageContainer.class);
		menuService = (IMenuService) locator.getService(IMenuService.class);

		// this assumes that the control is created before init
		contributeButtonActions();

	}

	private void contributeButtonActions() {
		if (pageContainer == null || menuService == null)
			return;
		factory = new AbstractContributionFactory(pageContainer.getButtonBarURI(), null) {

			public void createContributionItems(IServiceLocator serviceLocator, IContributionRoot additions) {
				additions.addContributionItem(new ActionContributionItem(revertAction), null);
			}
		};
		menuService.addContributionFactory(factory);
	}

	public void createControl(Composite parent) {
		profileId = Policy.getDefault().getProfileChooser().getProfileId(ProvUI.getDefaultParentShell());

		initializeDialogUnits(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IProvHelpContextIds.REVERT_CONFIGURATION_WIZARD);

		SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
		sashForm.setLayout(new GridLayout());
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		sashForm.setLayoutData(gd);

		createConfigurationsSection(sashForm);
		createContentsSection(sashForm);
		setControl(sashForm);

		// prime the selection.  The selection accesses the
		// revert action, so create it also.
		createRevertAction();
		Object element = configsViewer.getElementAt(0);
		if (element != null)
			configsViewer.setSelection(new StructuredSelection(element));
	}

	private void createConfigurationsSection(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(gd);

		Label label = new Label(composite, SWT.NONE);
		label.setText(ProvUIMessages.RevertDialog_ConfigsLabel);
		configsViewer = new TableViewer(composite, SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		configsViewer.setContentProvider(new RepositoryContentProvider());
		configsViewer.setLabelProvider(new ProvElementLabelProvider());
		configsViewer.setComparator(new ViewerComparator() {
			// We override the ViewerComparator so that we don't get the labels of the elements
			// for comparison, but rather get the timestamps and compare them.
			// Reverse sorting is used so that newest is first.
			public int compare(Viewer viewer, Object o1, Object o2) {
				if (o1 instanceof RollbackProfileElement && o2 instanceof RollbackProfileElement) {
					long timestamp1 = ((RollbackProfileElement) o1).getTimestamp();
					long timestamp2 = ((RollbackProfileElement) o2).getTimestamp();
					if (timestamp1 > timestamp2)
						return -1;
					return 1;
				}
				// this is naive (doesn't consult the label provider), but shouldn't happen
				return o2.toString().compareTo(o1.toString());
			}
		});
		configsViewer.setInput(getInput());

		configsViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handleSelectionChanged((IStructuredSelection) event.getSelection());
			}

		});
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		configsViewer.getControl().setLayoutData(gd);
	}

	private void createContentsSection(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(gd);

		Label label = new Label(composite, SWT.NONE);
		label.setText(ProvUIMessages.RevertDialog_ConfigContentsLabel);
		configContentsViewer = new TreeViewer(composite, SWT.SINGLE | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		configContentsViewer.setComparator(new IUComparator(IUComparator.IU_NAME));
		configContentsViewer.setComparer(new ProvElementComparer());
		configContentsViewer.setContentProvider(new DeferredQueryContentProvider());

		// columns before labels or you get a blank table
		setTreeColumns(configContentsViewer.getTree());
		configContentsViewer.setLabelProvider(new IUDetailsLabelProvider());

		gd = new GridData(GridData.FILL_BOTH);
		configContentsViewer.getControl().setLayoutData(gd);

	}

	private void createRevertAction() {
		revertAction = new Action() {
			public void run() {
				boolean result = MessageDialog.openQuestion(getShell(), ProvUIMessages.RevertDialog_Title, ProvUIMessages.RevertDialog_ConfirmRestartMessage);
				if (!result)
					return;
				boolean finish = revert();
				if (finish) {
					pageContainer.close();
				}
			}
		};
		revertAction.setText(ProvUIMessages.InstalledSoftwarePage_RevertLabel);
		revertAction.setToolTipText(ProvUIMessages.InstalledSoftwarePage_RevertTooltip);
	}

	private Object getInput() {
		ProfileSnapshots element = new ProfileSnapshots(profileId);
		return element;
	}

	void handleSelectionChanged(IStructuredSelection selection) {
		if (!selection.isEmpty()) {
			final Object selected = selection.getFirstElement();
			if (selected instanceof RollbackProfileElement) {
				configContentsViewer.setInput(selected);
				revertAction.setEnabled(true);
				return;
			}
		}
		configContentsViewer.setInput(null);
		revertAction.setEnabled(false);
	}

	private void setTreeColumns(Tree tree) {
		IUColumnConfig[] columns = ProvUI.getIUColumnConfig();

		for (int i = 0; i < columns.length; i++) {
			TreeColumn tc = new TreeColumn(tree, SWT.NONE, i);
			tc.setResizable(true);
			tc.setText(columns[i].columnTitle);
			tc.setWidth(convertHorizontalDLUsToPixels(DEFAULT_COLUMN_WIDTH));
		}
	}

	private IProfile getSelectedSnapshot() {
		Object selected = ((IStructuredSelection) configsViewer.getSelection()).getFirstElement();
		if (selected != null && selected instanceof RollbackProfileElement)
			try {
				return ((RollbackProfileElement) selected).getProfileSnapshot(new NullProgressMonitor());
			} catch (ProvisionException e) {
				ProvUI.handleException(e, null, StatusManager.LOG);
			}
		return null;
	}

	boolean revert() {
		final IProfile snapshot = getSelectedSnapshot();
		if (snapshot == null)
			return false;
		final ProvisioningPlan[] plan = new ProvisioningPlan[1];
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				IProfile currentProfile;
				try {
					currentProfile = ProvisioningUtil.getProfile(profileId);
					plan[0] = ProvisioningUtil.getRevertPlan(currentProfile, snapshot, monitor);
				} catch (ProvisionException e) {
					ProvUI.handleException(e, null, StatusManager.SHOW | StatusManager.LOG);
				}
			}
		};
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
		try {
			dialog.run(true, true, runnable);
		} catch (InvocationTargetException e) {
			ProvUI.handleException(e.getCause(), null, StatusManager.SHOW | StatusManager.LOG);
		} catch (InterruptedException e) {
			// nothing to report
		}
		boolean reverted = false;
		if (plan[0] != null) {
			if (plan[0].getStatus().isOK()) {
				ProvisioningOperation op = new ProfileModificationOperation(ProvUIMessages.RevertDialog_RevertOperationLabel, profileId, plan[0]);
				ProvisioningOperationRunner.schedule(op, StatusManager.SHOW | StatusManager.LOG);
				ProvisioningOperationRunner.requestRestart(true);
				reverted = true;
			} else if (plan[0].getStatus().getSeverity() != IStatus.CANCEL) {
				ProvUI.reportStatus(plan[0].getStatus(), StatusManager.LOG);
				setMessage(ProvUIMessages.ProfileModificationWizardPage_UnexpectedError, IMessageProvider.ERROR);
			}
		}
		return reverted;
	}

	public void dispose() {
		super.dispose();
		menuService.removeContributionFactory(factory);
	}
}
