/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.ui;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import org.eclipse.compare.*;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.dialogs.CopyUtils;
import org.eclipse.equinox.internal.p2.ui.dialogs.InstalledIUGroup;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.internal.p2.ui.viewers.*;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
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
import org.eclipse.swt.graphics.Image;
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
	private static final int COMPARE_ID = IDialogConstants.CLIENT_ID + 2;
	TableViewer configsViewer;
	TreeViewer configContentsViewer;
	IUDetailsLabelProvider labelProvider;
	IAction revertAction;
	Button revertButton, deleteButton, compareButton;
	String profileId;
	AbstractContributionFactory factory;
	Text detailsArea;
	InstalledIUGroup installedIUGroup;
	ProvisioningUI ui;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.about.InstallationPage#createPageButtons(org.eclipse.swt.widgets.Composite)
	 */
	public void createPageButtons(Composite parent) {
		if (profileId == null)
			return;
		compareButton = createButton(parent, COMPARE_ID, ProvUIMessages.RevertProfilePage_CompareLabel);
		compareButton.setToolTipText(ProvUIMessages.RevertProfilePage_CompareTooltip);
		compareButton.setEnabled(computeCompareEnablement());
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
		ui = ProvisioningUI.getDefaultUI();
		profileId = ui.getProfileId();
		if (profileId == null) {
			IStatus status = ui.getPolicy().getNoProfileChosenStatus();
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
		configsViewer = new TableViewer(composite, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		ProvElementContentProvider provider = new ProvElementContentProvider() {
			protected void finishedFetchingElements(Object o) {
				Object element = configsViewer.getElementAt(0);
				if (element != null)
					configsViewer.setSelection(new StructuredSelection(element));
			}
		};
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
		ProfileSnapshots element = new ProfileSnapshots(profileId);
		return element;
	}

	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
			case REVERT_ID :
				revertAction.run();
				break;
			case COMPARE_ID :
				compare();
				break;
			case DELETE_ID :
				deleteSelectedSnapshots();
				break;
		}
	}

	void handleSelectionChanged(IStructuredSelection selection) {
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
					if (compareButton != null)
						compareButton.setEnabled(false);
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
				compareButton.setEnabled(computeCompareEnablement());
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

	boolean computeCompareEnablement() {
		// compare is enabled if there are two elements selected
		Object[] selection = ((IStructuredSelection) configsViewer.getSelection()).toArray();
		if (selection.length == 2) {
			for (int i = 0; i < selection.length; i++) {
				if (!(selection[i] instanceof RollbackProfileElement))
					return false;
			}
			return true;
		}
		return false;
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

	private RollbackProfileElement[] getRollbackProfileElementsToCompare() {
		// expecting two items selected
		RollbackProfileElement[] result = new RollbackProfileElement[2];
		IStructuredSelection selection = ((IStructuredSelection) configsViewer.getSelection());
		int i = 0;
		for (Object selected : selection.toList()) {
			if (selected != null && selected instanceof RollbackProfileElement) {
				result[i++] = (RollbackProfileElement) selected;
			}
			if (i == 2)
				break;
		}
		return result;
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
				ProfileModificationJob op = new ProfileModificationJob(ProvUIMessages.RevertDialog_RevertOperationLabel, getSession(), profileId, plan[0], new ProvisioningContext());
				// we want to force a restart (not allow apply changes)
				op.setRestartPolicy(ProvisioningJob.RESTART_ONLY);
				ui.schedule(op, StatusManager.SHOW | StatusManager.LOG);
				reverted = true;
			} else if (plan[0].getStatus().getSeverity() != IStatus.CANCEL) {
				ProvUI.reportStatus(plan[0].getStatus(), StatusManager.LOG | StatusManager.SHOW);
				// This message has no effect in an installation dialog
				// setMessage(ProvUIMessages.ProfileModificationWizardPage_UnexpectedError, IMessageProvider.ERROR);
			}
		}
		return reverted;
	}

	void compare() {
		final RollbackProfileElement[] rpe = getRollbackProfileElementsToCompare();
		CompareUI.openCompareDialog(new ProfileCompareEditorInput(rpe));
	}

	private class ProfileCompareEditorInput extends CompareEditorInput {
		private Object root;
		private ProvElementNode l;
		private ProvElementNode r;

		public ProfileCompareEditorInput(RollbackProfileElement[] rpe) {
			super(new CompareConfiguration());
			Assert.isTrue(rpe.length == 2);
			l = new ProvElementNode(rpe[0]);
			r = new ProvElementNode(rpe[1]);
		}

		protected Object prepareInput(IProgressMonitor monitor) {
			initLabels();
			Differencer d = new Differencer();
			root = d.findDifferences(false, monitor, null, null, l, r);
			return root;
		}

		private void initLabels() {
			CompareConfiguration cc = getCompareConfiguration();
			cc.setLeftEditable(false);
			cc.setRightEditable(false);
			cc.setLeftLabel(l.getName());
			cc.setLeftImage(l.getImage());
			cc.setRightLabel(r.getName());
			cc.setRightImage(r.getImage());
		}

		public String getOKButtonLabel() {
			return IDialogConstants.OK_LABEL;
		}
	}

	private class ProvElementNode implements IStructureComparator, ITypedElement, IStreamContentAccessor {
		private ProvElement pe;
		private IInstallableUnit iu;
		final static String BLANK = ""; //$NON-NLS-1$
		private String id = BLANK;

		public ProvElementNode(Object input) {
			pe = (ProvElement) input;
			iu = (IInstallableUnit) ProvUI.getAdapter(pe, IInstallableUnit.class);
			if (iu != null) {
				id = iu.getId();
			}
		}

		public Object[] getChildren() {
			Set<ProvElementNode> children = new HashSet<ProvElementNode>();
			if (pe instanceof RollbackProfileElement) {
				Object[] c = ((RollbackProfileElement) pe).getChildren(null);
				for (int i = 0; i < c.length; i++) {
					children.add(new ProvElementNode(c[i]));
				}
			} else if (pe instanceof InstalledIUElement) {
				Object[] c = ((InstalledIUElement) pe).getChildren(null);
				for (int i = 0; i < c.length; i++) {
					children.add(new ProvElementNode(c[i]));
				}
			}
			return children.toArray();
		}

		/**
		 * Implementation based on <code>id</code>.
		 * @param other the object to compare this <code>ProvElementNode</code> against.
		 * @return <code>true</code> if the <code>ProvElementNodes</code>are equal; <code>false</code> otherwise.
		 */
		public boolean equals(Object other) {
			if (other instanceof ProvElementNode)
				return id.equals(((ProvElementNode) other).id);
			return super.equals(other);
		}

		/**
		 * Implementation based on <code>id</code>.
		 * @return a hash code for this object.
		 */
		public int hashCode() {
			return id.hashCode();
		}

		public Image getImage() {
			return pe.getImage(null);
		}

		public String getName() {
			if (iu != null) {
				return iu.getProperty(IInstallableUnit.PROP_NAME, null);
			}
			return pe.getLabel(null);
		}

		public String getType() {
			return ITypedElement.UNKNOWN_TYPE;
		}

		public InputStream getContents() {
			String contents = BLANK;
			if (iu != null) {
				contents = iu.getVersion().toString();
			}
			return new ByteArrayInputStream(contents.getBytes());
		}
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
							configsViewer.refresh();
						} catch (ProvisionException e) {
							ProvUI.handleException(e, null, StatusManager.SHOW | StatusManager.LOG);
						}
					}
				}
			}
		}
	}

	ProvisioningSession getSession() {
		return getProvisioningUI().getSession();
	}

	ProvisioningUI getProvisioningUI() {
		return ProvisioningUI.getDefaultUI();
	}
}
