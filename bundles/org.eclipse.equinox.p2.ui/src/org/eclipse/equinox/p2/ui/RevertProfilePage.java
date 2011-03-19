/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Iterator;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.dialogs.CopyUtils;
import org.eclipse.equinox.internal.p2.ui.dialogs.InstalledIUGroup;
import org.eclipse.equinox.internal.p2.ui.model.ProfileSnapshots;
import org.eclipse.equinox.internal.p2.ui.model.RollbackProfileElement;
import org.eclipse.equinox.internal.p2.ui.viewers.*;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.operations.*;
import org.eclipse.equinox.p2.planner.IPlanner;
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
 * RevertProfilePage displays a profile's configuration history in
 * an Installation Page.  Clients can use this class as the implementation
 * class for an installationPages extension.
 * 
 * @see InstallationPage
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @since 2.0
 *
 */
public class RevertProfilePage extends InstallationPage implements ICopyable {

	private static final int REVERT_ID = IDialogConstants.CLIENT_ID;
	private static final int DELETE_ID = IDialogConstants.CLIENT_ID + 1;
	TableViewer configsViewer;
	TreeViewer configContentsViewer;
	IUDetailsLabelProvider labelProvider;
	IAction revertAction;
	Button revertButton, deleteButton;
	String profileId;
	AbstractContributionFactory factory;
	Text detailsArea;
	InstalledIUGroup installedIUGroup;
	ProvisioningUI ui;

	private static class TagEditingSuport extends EditingSupport {

		private ProvisioningUI ui;

		public TagEditingSuport(TableViewer viewer, ProvisioningUI ui) {
			super(viewer);
			this.ui = ui;
		}

		protected CellEditor getCellEditor(Object element) {
			return new TextCellEditor(((TableViewer) getViewer()).getTable());
		}

		protected boolean canEdit(Object element) {
			return element instanceof RollbackProfileElement;
		}

		protected Object getValue(Object element) {
			if (element instanceof RollbackProfileElement) {
				return ((RollbackProfileElement) element).getProfileTag() != null ? ((RollbackProfileElement) element).getProfileTag() : ""; //$NON-NLS-1$
			}
			return null;
		}

		protected void setValue(Object element, Object value) {
			if (element instanceof RollbackProfileElement && value instanceof String) {
				RollbackProfileElement ele = ((RollbackProfileElement) element);
				ele.setProfileTag((String) value);
				// save
				IProfileRegistry registry = ProvUI.getProfileRegistry(ui.getSession());
				if (registry != null) {
					IStatus status;
					if (((String) value).length() > 0) {
						status = registry.setProfileStateProperty(ele.getProfileId(), ele.getTimestamp(), IProfile.STATE_PROP_TAG, (String) value);
					} else {
						status = registry.removeProfileStateProperties(ele.getProfileId(), ele.getTimestamp(), Collections.singleton(IProfile.STATE_PROP_TAG));
					}
					if (!status.isOK()) {
						StatusManager.getManager().handle(status);
					}
				}
				getViewer().update(element, null);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.about.InstallationPage#createPageButtons(org.eclipse.swt.widgets.Composite)
	 */
	public void createPageButtons(Composite parent) {
		if (profileId == null)
			return;
		deleteButton = createButton(parent, DELETE_ID, ProvUIMessages.RevertProfilePage_Delete);
		deleteButton.setToolTipText(ProvUIMessages.RevertProfilePage_DeleteTooltip);
		deleteButton.setEnabled(computeDeleteEnablement());
		revertButton = createButton(parent, REVERT_ID, revertAction.getText());
		revertButton.setToolTipText(revertAction.getToolTipText());
		revertButton.setEnabled(revertAction.isEnabled());
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		profileId = getProvisioningUI().getProfileId();
		if (profileId == null) {
			IStatus status = getProvisioningUI().getPolicy().getNoProfileChosenStatus();
			if (status != null)
				ProvUI.reportStatus(status, StatusManager.LOG);
			Text text = new Text(parent, SWT.WRAP | SWT.READ_ONLY);
			text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			text.setText(ProvUIMessages.RevertProfilePage_NoProfile);
			setControl(text);
			return;
		}

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
		configsViewer = new TableViewer(composite, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		ProvElementContentProvider provider = new ProvElementContentProvider() {
			protected void finishedFetchingElements(Object o) {
				Object element = configsViewer.getElementAt(0);
				if (element != null)
					configsViewer.setSelection(new StructuredSelection(element));
			}
		};
		setConfigsColumns(configsViewer);

		// Use deferred fetch because getting snapshots is expensive.
		provider.setFetchInBackground(true);
		configsViewer.setContentProvider(provider);
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

	private void setConfigsColumns(TableViewer tableViewer) {
		tableViewer.getTable().setHeaderVisible(true);
		TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.NONE);
		TableColumn tc = column.getColumn();
		tc.setResizable(true);
		tc.setText(ProvUIMessages.RevertProfilePage_ProfileTimestampColumn);
		tc.setWidth(175);

		column = new TableViewerColumn(tableViewer, SWT.NONE);
		tc = column.getColumn();
		tc.setResizable(true);
		tc.setText(ProvUIMessages.RevertProfilePage_ProfileTagColumn);
		tc.setWidth(200);

		column.setEditingSupport(new TagEditingSuport(tableViewer, ui));
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
		IUComparator comparator = new IUComparator(IUComparator.IU_NAME);
		comparator.useColumnConfig(ProvUI.getIUColumnConfig());
		configContentsViewer.setComparator(comparator);
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
		ProfileSnapshots element = new ProfileSnapshots(profileId, getProvisioningUI().getSession());
		return element;
	}

	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
			case REVERT_ID :
				revertAction.run();
				break;
			case DELETE_ID :
				deleteSelectedSnapshots();
				break;
		}
	}

	protected void handleSelectionChanged(IStructuredSelection selection) {
		if (!selection.isEmpty()) {
			if (selection.size() == 1) {
				final Object selected = selection.getFirstElement();
				if (selected instanceof RollbackProfileElement) {
					Object[] elements = configContentsViewer.getExpandedElements();
					configContentsViewer.getTree().setRedraw(false);
					configContentsViewer.setInput(selected);
					configContentsViewer.setExpandedElements(elements);
					configContentsViewer.getTree().setRedraw(true);
					boolean isNotCurrentProfile = !((RollbackProfileElement) selected).isCurrentProfile();
					revertAction.setEnabled(isNotCurrentProfile);
					if (revertButton != null)
						revertButton.setEnabled(isNotCurrentProfile);
					if (deleteButton != null)
						deleteButton.setEnabled(isNotCurrentProfile);
					return;
				}
			} else {
				// multiple selections, can't revert or look at details
				revertAction.setEnabled(false);
				if (revertButton != null) {
					revertButton.setEnabled(false);
				}
				configContentsViewer.setInput(null);
				deleteButton.setEnabled(computeDeleteEnablement());
				return;
			}
		}
		// Nothing is selected
		configContentsViewer.setInput(null);
		revertAction.setEnabled(false);
		if (revertButton != null)
			revertButton.setEnabled(false);
		if (deleteButton != null)
			deleteButton.setEnabled(computeDeleteEnablement());
	}

	boolean computeDeleteEnablement() {
		// delete is permitted if none of the selected elements are the current profile
		boolean okToDelete = true;
		Iterator<?> iter = ((IStructuredSelection) configsViewer.getSelection()).iterator();
		while (iter.hasNext()) {
			Object selected = iter.next();
			// If it's not a recognized element or if it is the current profile, we can't delete.  Stop iterating.
			if (!(selected instanceof RollbackProfileElement) || ((RollbackProfileElement) selected).isCurrentProfile()) {
				okToDelete = false;
				break;
			}
		}
		return okToDelete;
	}

	private void setTreeColumns(Tree tree) {
		IUColumnConfig[] columns = ProvUI.getIUColumnConfig();
		tree.setHeaderVisible(true);

		for (int i = 0; i < columns.length; i++) {
			TreeColumn tc = new TreeColumn(tree, SWT.NONE, i);
			tc.setResizable(true);
			tc.setText(columns[i].getColumnTitle());
			tc.setWidth(columns[i].getWidthInPixels(tree));
		}
	}

	private IProfile getSelectedSnapshot() {
		Object selected = ((IStructuredSelection) configsViewer.getSelection()).getFirstElement();
		if (selected != null && selected instanceof RollbackProfileElement)
			return ((RollbackProfileElement) selected).getProfileSnapshot(new NullProgressMonitor());
		return null;
	}

	boolean revert() {
		final IProfile snapshot = getSelectedSnapshot();
		if (snapshot == null)
			return false;
		final IProvisioningPlan[] plan = new IProvisioningPlan[1];
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				IProfile currentProfile;
				IProfileRegistry registry = ProvUI.getProfileRegistry(getSession());
				IPlanner planner = (IPlanner) getSession().getProvisioningAgent().getService(IPlanner.SERVICE_NAME);
				currentProfile = registry.getProfile(profileId);
				plan[0] = planner.getDiffPlan(currentProfile, snapshot, monitor);
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
				ProfileModificationJob op = new ProfileModificationJob(ProvUIMessages.RevertDialog_RevertOperationLabel, getSession(), profileId, plan[0], new ProvisioningContext(getSession().getProvisioningAgent()));
				// we want to force a restart (not allow apply changes)
				op.setRestartPolicy(ProvisioningJob.RESTART_ONLY);
				getProvisioningUI().schedule(op, StatusManager.SHOW | StatusManager.LOG);
				reverted = true;
			} else if (plan[0].getStatus().getSeverity() != IStatus.CANCEL) {
				ProvUI.reportStatus(plan[0].getStatus(), StatusManager.LOG | StatusManager.SHOW);
				// This message has no effect in an installation dialog
				// setMessage(ProvUIMessages.ProfileModificationWizardPage_UnexpectedError, IMessageProvider.ERROR);
			}
		}
		return reverted;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.p2.ui.ICopyable#copyToClipboard(org.eclipse.swt.widgets.Control)
	 */
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

	void deleteSelectedSnapshots() {
		IStructuredSelection selection = (IStructuredSelection) configsViewer.getSelection();
		if (selection.isEmpty())
			return;
		String title = selection.size() == 1 ? ProvUIMessages.RevertProfilePage_DeleteSingleConfigurationTitle : ProvUIMessages.RevertProfilePage_DeleteMultipleConfigurationsTitle;
		String confirmMessage = selection.size() == 1 ? ProvUIMessages.RevertProfilePage_ConfirmDeleteSingleConfig : ProvUIMessages.RevertProfilePage_ConfirmDeleteMultipleConfigs;
		if (MessageDialog.openConfirm(configsViewer.getControl().getShell(), title, confirmMessage)) {
			Iterator<?> iter = selection.iterator();
			while (iter.hasNext()) {
				Object selected = iter.next();
				// If it is a recognized element and it is not the current profile, then it can be deleted.
				if (selected instanceof RollbackProfileElement && !((RollbackProfileElement) selected).isCurrentProfile()) {
					RollbackProfileElement snapshot = (RollbackProfileElement) selected;
					IProfileRegistry registry = ProvUI.getProfileRegistry(getSession());
					if (registry != null) {
						try {
							registry.removeProfile(profileId, snapshot.getTimestamp());
						} catch (ProvisionException e) {
							ProvUI.handleException(e, null, StatusManager.SHOW | StatusManager.LOG);
						}
					}
				}
			}
			configsViewer.refresh();
		}
	}

	ProvisioningSession getSession() {
		return getProvisioningUI().getSession();
	}

	ProvisioningUI getProvisioningUI() {
		// if a UI has not been set then assume that the current default UI is the right thing
		if (ui == null)
			return ui = ProvisioningUI.getDefaultUI();
		return ui;
	}

	protected IStructuredSelection getSelection() {
		return (IStructuredSelection) configsViewer.getSelection();
	}

	/**
	 * Set the provisioning UI to use with this page
	 * 
	 * @param value the provisioning ui to use
	 * @since 2.1
	 */
	public void setProvisioningUI(ProvisioningUI value) {
		ui = value;
	}
}
