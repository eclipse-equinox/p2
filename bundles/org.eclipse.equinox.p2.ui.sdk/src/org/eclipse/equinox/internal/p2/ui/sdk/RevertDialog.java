/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk;

import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.director.ProvisioningPlan;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.p2.ui.model.RollbackProfileElement;
import org.eclipse.equinox.p2.ui.model.RollbackRepositoryElement;
import org.eclipse.equinox.p2.ui.operations.*;
import org.eclipse.equinox.p2.ui.viewers.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

/**
 * Dialog that allows users to update their installed IU's or find new ones.
 * 
 * @since 3.4
 */
public class RevertDialog extends TrayDialog {

	private static final String DIALOG_SETTINGS_SECTION = "RevertDialog"; //$NON-NLS-1$
	private static final int DEFAULT_COLUMN_WIDTH = 150;

	Profile profile;
	TableViewer configsViewer;
	TableViewer configContentsViewer;

	/**
	 * Create an instance of this Dialog.
	 * 
	 */
	public RevertDialog(Shell shell, Profile profile) {
		super(shell);
		this.profile = profile;
		setShellStyle(SWT.DIALOG_TRIM | SWT.MODELESS | SWT.MAX | SWT.RESIZE | getDefaultOrientation());
		setBlockOnOpen(false);
	}

	protected void configureShell(Shell shell) {
		shell.setText(ProvSDKMessages.RevertDialog_Title);
		super.configureShell(shell);
	}

	protected Control createDialogArea(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		comp.setLayout(layout);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		comp.setLayoutData(gd);
		Label label = new Label(comp, SWT.WRAP);
		label.setText(ProvSDKMessages.RevertDialog_SelectMessage);
		gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		configsViewer = new TableViewer(comp, SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		configsViewer.setContentProvider(new RepositoryContentProvider(ProvSDKUIActivator.getDefault().getQueryProvider()));
		configsViewer.setInput(getInput());
		configsViewer.setLabelProvider(new ProvElementLabelProvider());
		configsViewer.setComparator(new ViewerComparator(new Comparator() {
			// This comparator sorts in reverse order so that we see the newest configs first
			public int compare(Object o1, Object o2) {
				return ((String) o2).compareTo((String) o1);
			}
		}));
		configsViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				RevertDialog.this.selectionChanged((IStructuredSelection) event.getSelection());
			}

		});
		gd = new GridData(GridData.FILL_BOTH);
		configsViewer.getControl().setLayoutData(gd);

		configContentsViewer = new TableViewer(comp, SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		configContentsViewer.setContentProvider(new StaticContentProvider(new Object[0]));
		configContentsViewer.setInput(new Object[0]);
		configContentsViewer.setLabelProvider(new ProvElementLabelProvider());
		configContentsViewer.setComparator(new ViewerComparator());
		setTableColumns(configContentsViewer.getTable());
		gd = new GridData(GridData.FILL_BOTH);
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		configContentsViewer.getControl().setLayoutData(gd);

		selectionChanged((IStructuredSelection) configsViewer.getSelection());
		Dialog.applyDialogFont(comp);
		return comp;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	protected boolean isResizable() {
		return true;
	}

	protected void okPressed() {
		revert();
		super.okPressed();
	}

	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings settings = ProvSDKUIActivator.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(DIALOG_SETTINGS_SECTION);
		if (section == null) {
			section = settings.addNewSection(DIALOG_SETTINGS_SECTION);
		}
		return section;
	}

	private void revert() {
		final IInstallableUnit iu = getSelectedIU();
		if (iu == null)
			return;
		final ProvisioningPlan[] plan = new ProvisioningPlan[1];
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				try {
					plan[0] = ProvisioningUtil.getBecomePlan(iu, profile, monitor);
				} catch (ProvisionException e) {
					ProvUI.handleException(e, ProvSDKMessages.RevertDialog_RevertError);
				}
			}
		};
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);
			ProvisioningOperation op = new ProfileModificationOperation(ProvSDKMessages.RevertDialog_RevertOperationLabel, profile.getProfileId(), plan[0]);
			Job job = ProvisioningOperationRunner.schedule(op, getShell());
			job.join();
		} catch (InterruptedException e) {
			// don't report thread interruption
		} catch (InvocationTargetException e) {
			ProvUI.handleException(e.getCause(), ProvSDKMessages.RevertDialog_RevertError);
		}

	}

	private Object getInput() {
		try {
			RollbackRepositoryElement element = new RollbackRepositoryElement(ProvisioningUtil.getRollbackRepository(null), profile);
			element.setQueryProvider(ProvSDKUIActivator.getDefault().getQueryProvider());
			return element;
		} catch (ProvisionException e) {
			ProvUI.handleException(e, null);
			return null;
		}
	}

	void selectionChanged(IStructuredSelection selection) {
		if (selection.isEmpty()) {
			setEnabled(false);
		} else {
			setEnabled(true);
			Object selected = selection.getFirstElement();
			if (selected instanceof RollbackProfileElement)
				configContentsViewer.setInput(((RollbackProfileElement) selected).getChildren(null));
		}
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

	private IInstallableUnit getSelectedIU() {
		Object selected = ((IStructuredSelection) configsViewer.getSelection()).getFirstElement();
		if (selected != null && selected instanceof RollbackProfileElement)
			return ((RollbackProfileElement) selected).getIU();
		return null;
	}

	private void setEnabled(boolean enabled) {
		Button okButton = getButton(IDialogConstants.OK_ID);
		if (okButton != null && !okButton.isDisposed()) {
			okButton.setEnabled(enabled);
		}
	}
}
