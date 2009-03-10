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
import org.eclipse.equinox.internal.p2.ui.dialogs.CopyUtils;
import org.eclipse.equinox.internal.p2.ui.dialogs.ICopyable;
import org.eclipse.equinox.internal.p2.ui.model.ProfileSnapshots;
import org.eclipse.equinox.internal.p2.ui.model.RollbackProfileElement;
import org.eclipse.equinox.internal.p2.ui.viewers.DeferredQueryContentProvider;
import org.eclipse.equinox.internal.p2.ui.viewers.IUDetailsLabelProvider;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.*;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.*;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.about.InstallationPage;
import org.eclipse.ui.menus.AbstractContributionFactory;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * @since 3.4
 */
public class RevertProfilePage extends InstallationPage implements ICopyable {

	private static final int REVERT_ID = IDialogConstants.CLIENT_ID;
	private static final int DEFAULT_COLUMN_WIDTH = 150;
	TableViewer configsViewer;
	TreeViewer configContentsViewer;
	IUDetailsLabelProvider labelProvider;
	IAction revertAction;
	String profileId;
	AbstractContributionFactory factory;
	Text detailsArea;
	InstalledIUGroup installedIUGroup;

	public void createPageButtons(Composite parent) {
		createButton(parent, REVERT_ID, revertAction.getText());
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
		CopyUtils.activateCopy(this, configsViewer.getControl());
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
		configContentsViewer = new TreeViewer(composite, SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		configContentsViewer.setComparator(new IUComparator(IUComparator.IU_NAME));
		configContentsViewer.setComparer(new ProvElementComparer());
		configContentsViewer.setContentProvider(new DeferredQueryContentProvider());

		// columns before labels or you get a blank table
		setTreeColumns(configContentsViewer.getTree());
		labelProvider = new IUDetailsLabelProvider();
		configContentsViewer.setLabelProvider(labelProvider);

		gd = new GridData(GridData.FILL_BOTH);
		configContentsViewer.getControl().setLayoutData(gd);
		CopyUtils.activateCopy(this, configContentsViewer.getControl());
	}

	private void createRevertAction() {
		revertAction = new Action() {
			public void run() {
				boolean result = MessageDialog.openQuestion(getShell(), ProvUIMessages.RevertDialog_Title, ProvUIMessages.RevertDialog_ConfirmRestartMessage);
				if (!result)
					return;
				boolean finish = revert();
				if (finish) {
					getPageContainer().closeModalContainers();
				}
			}
		};
		revertAction.setText(ProvUIMessages.RevertProfilePage_RevertLabel);
		revertAction.setToolTipText(ProvUIMessages.RevertProfilePage_RevertTooltip);
	}

	private Object getInput() {
		ProfileSnapshots element = new ProfileSnapshots(profileId);
		return element;
	}

	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
			case REVERT_ID :
				revertAction.run();
				break;
		}
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
		tree.setHeaderVisible(true);

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
		// the dialog does not throw OperationCanceledException so we have to
		// check the monitor
		if (dialog.getProgressMonitor().isCanceled())
			return false;

		boolean reverted = false;
		if (plan[0] != null) {
			if (plan[0].getStatus().isOK()) {
				// We use a default provisioning context (all repos) because we have no other
				// way currently to figure out which sites the user wants to contact
				ProvisioningOperation op = new ProfileModificationOperation(ProvUIMessages.RevertDialog_RevertOperationLabel, profileId, plan[0], new ProvisioningContext(), new DefaultPhaseSet(), true);
				ProvisioningOperationRunner.schedule(op, StatusManager.SHOW | StatusManager.LOG);
				ProvisioningOperationRunner.requestRestart(true);
				reverted = true;
			} else if (plan[0].getStatus().getSeverity() != IStatus.CANCEL) {
				ProvUI.reportStatus(plan[0].getStatus(), StatusManager.LOG | StatusManager.SHOW);
				// This message has no effect in an installation dialog
				// setMessage(ProvUIMessages.ProfileModificationWizardPage_UnexpectedError, IMessageProvider.ERROR);
			}
		}
		return reverted;
	}

	public void copyToClipboard(Control activeControl) {
		String text = ""; //$NON-NLS-1$
		if (activeControl == configContentsViewer.getControl()) {
			text = CopyUtils.getIndentedClipboardText(((IStructuredSelection) configContentsViewer.getSelection()).toArray(), labelProvider);
		} else if (activeControl == configsViewer.getControl()) {
			Object[] elements = ((IStructuredSelection) configsViewer.getSelection()).toArray();
			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < elements.length; i++) {
				if (elements[i] instanceof RollbackProfileElement) {
					if (i > 0)
						buffer.append(CopyUtils.NEWLINE);
					buffer.append(((RollbackProfileElement) elements[i]).getLabel(elements[i]));
				}
			}
			text = buffer.toString();
		} else
			return;
		if (text.length() == 0)
			return;
		Clipboard clipboard = new Clipboard(PlatformUI.getWorkbench().getDisplay());
		clipboard.setContents(new Object[] {text}, new Transfer[] {TextTransfer.getInstance()});
		clipboard.dispose();
	}
}
