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
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.viewers.StaticContentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.director.ProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.p2.ui.model.RollbackProfileElement;
import org.eclipse.equinox.p2.ui.model.RollbackRepositoryElement;
import org.eclipse.equinox.p2.ui.operations.*;
import org.eclipse.equinox.p2.ui.query.IQueryProvider;
import org.eclipse.equinox.p2.ui.viewers.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

/**
 * @since 3.4
 */
public class RevertProfileWizardPage extends WizardPage {

	private TableViewer configsViewer;
	private TableViewer configContentsViewer;
	String profileId;
	IQueryProvider queryProvider;
	private static final int DEFAULT_COLUMN_WIDTH = 150;

	public RevertProfileWizardPage(String profileId, IQueryProvider queryProvider) {
		super("RevertConfiguration"); //$NON-NLS-1$
		setTitle(ProvUIMessages.RevertDialog_PageTitle);
		setDescription(ProvUIMessages.RevertDialog_Description);
		this.profileId = profileId;
		this.queryProvider = queryProvider;

	}

	public void createControl(Composite parent) {
		SashForm sashForm = new SashForm(parent, SWT.HORIZONTAL);
		sashForm.setLayout(new GridLayout());
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
		initializeDialogUnits(sashForm);

		createConfigurationsSection(sashForm);
		createContentsSection(sashForm);
		setControl(sashForm);

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
		configsViewer.setContentProvider(new RepositoryContentProvider(queryProvider));
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
		configContentsViewer = new TableViewer(composite, SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		configContentsViewer.setContentProvider(new StaticContentProvider(new Object[0]));
		configContentsViewer.setInput(new Object[0]);
		configContentsViewer.setLabelProvider(new ProvElementLabelProvider());
		configContentsViewer.setComparator(new ViewerComparator());
		setTableColumns(configContentsViewer.getTable());
		gd = new GridData(GridData.FILL_BOTH);
		configContentsViewer.getControl().setLayoutData(gd);

	}

	private Object getInput() {
		try {
			RollbackRepositoryElement element = new RollbackRepositoryElement(ProvisioningUtil.getRollbackRepositoryURL(), profileId);
			element.setQueryProvider(queryProvider);
			return element;
		} catch (ProvisionException e) {
			ProvUI.handleException(e, null);
			return null;
		}
	}

	void handleSelectionChanged(IStructuredSelection selection) {
		if (!selection.isEmpty()) {
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
		final IInstallableUnit iu = getSelectedIU();
		if (iu == null)
			return false;
		final ProvisioningPlan[] plan = new ProvisioningPlan[1];
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				try {
					plan[0] = ProvisioningUtil.getRevertPlan(iu, profileId, monitor);
				} catch (ProvisionException e) {
					ProvUI.handleException(e, ProvUIMessages.RevertDialog_RevertError);
				}
			}
		};
		try {
			getContainer().run(true, true, runnable);
			ProvisioningOperation op = new ProfileModificationOperation(ProvUIMessages.RevertDialog_RevertOperationLabel, profileId, plan[0]);
			Job job = ProvisioningOperationRunner.schedule(op, getShell());
			job.join();
		} catch (InterruptedException e) {
			// don't report thread interruption
		} catch (InvocationTargetException e) {
			ProvUI.handleException(e.getCause(), ProvUIMessages.RevertDialog_RevertError);
		}
		return true;
	}

	public boolean isPageComplete() {
		return getSelectedIU() != null;
	}
}
