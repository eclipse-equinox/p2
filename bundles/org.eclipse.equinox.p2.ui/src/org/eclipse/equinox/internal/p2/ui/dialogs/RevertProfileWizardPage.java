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
package org.eclipse.equinox.internal.p2.ui.dialogs;

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
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.*;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * @since 3.4
 */
public class RevertProfileWizardPage extends WizardPage {

	private TableViewer configsViewer;
	private TableViewer configContentsViewer;
	String profileId;
	private static final int DEFAULT_COLUMN_WIDTH = 150;

	public RevertProfileWizardPage(String profileId) {
		super("RevertConfiguration"); //$NON-NLS-1$
		setTitle(ProvUIMessages.RevertDialog_PageTitle);
		setDescription(ProvUIMessages.RevertDialog_Description);
		this.profileId = profileId;

	}

	public void createControl(Composite parent) {
		SashForm sashForm = new SashForm(parent, SWT.HORIZONTAL);
		sashForm.setLayout(new GridLayout());
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
		initializeDialogUnits(sashForm);

		createConfigurationsSection(sashForm);
		createContentsSection(sashForm);
		setControl(sashForm);

		// prime the selection
		Object element = configsViewer.getElementAt(0);
		if (element != null)
			configsViewer.setSelection(new StructuredSelection(element));
		Dialog.applyDialogFont(sashForm);
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
		gd = new GridData(GridData.FILL_BOTH);
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
		configContentsViewer = new TableViewer(composite, SWT.SINGLE | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		configContentsViewer.setComparator(new IUComparator(IUComparator.IU_NAME));
		configContentsViewer.setComparer(new ProvElementComparer());
		configContentsViewer.setContentProvider(new DeferredQueryContentProvider());

		// columns before labels or you get a blank table
		setTableColumns(configContentsViewer.getTable());
		configContentsViewer.setLabelProvider(new IUDetailsLabelProvider());

		gd = new GridData(GridData.FILL_BOTH);
		configContentsViewer.getControl().setLayoutData(gd);

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
				setPageComplete(true);
				return;
			}
		}
		configContentsViewer.setInput(null);
		setPageComplete(false);
	}

	private void setTableColumns(Table table) {
		IUColumnConfig[] columns = ProvUI.getIUColumnConfig();

		for (int i = 0; i < columns.length; i++) {
			TableColumn tc = new TableColumn(table, SWT.NONE, i);
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

	public boolean performFinish() {
		Shell shell = getContainer().getShell();
		boolean result = MessageDialog.openQuestion(shell, shell.getText(), ProvUIMessages.RevertDialog_ConfirmRestartMessage);
		if (!result)
			return false;

		boolean finish = revert();
		if (finish) {
			PlatformUI.getWorkbench().restart();
		}
		return finish;
	}

	private boolean revert() {
		final IProfile snapshot = getSelectedSnapshot();
		if (snapshot == null)
			return false;
		final boolean[] reverted = new boolean[1];
		reverted[0] = false;
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				try {
					IProfile currentProfile = ProvisioningUtil.getProfile(profileId);
					ProvisioningPlan plan = ProvisioningUtil.getRevertPlan(currentProfile, snapshot, monitor);
					if (plan != null) {
						if (plan.getStatus().isOK()) {
							ProvisioningOperation op = new ProfileModificationOperation(ProvUIMessages.RevertDialog_RevertOperationLabel, profileId, plan);
							ProvisioningOperationRunner.run(op, StatusManager.SHOW | StatusManager.LOG);
							reverted[0] = true;
						} else if (plan.getStatus().getSeverity() != IStatus.CANCEL) {
							ProvUI.reportStatus(plan.getStatus(), StatusManager.LOG);
							setMessage(ProvUIMessages.ProfileModificationWizardPage_UnexpectedError, IMessageProvider.ERROR);
						}
					}
				} catch (ProvisionException e) {
					ProvUI.handleException(e.getCause(), ProvUIMessages.ProfileModificationWizardPage_UnexpectedError, StatusManager.LOG);
					setMessage(ProvUIMessages.ProfileModificationWizardPage_UnexpectedError, IMessageProvider.ERROR);
				}
			}
		};
		try {
			getContainer().run(true, true, runnable);
		} catch (InterruptedException e) {
			// don't report thread interruption
		} catch (InvocationTargetException e) {
			ProvUI.handleException(e.getCause(), ProvUIMessages.RevertDialog_RevertError, StatusManager.LOG);
			setMessage(ProvUIMessages.RevertDialog_RevertError, IMessageProvider.ERROR);
		}
		return reverted[0];
	}
}
