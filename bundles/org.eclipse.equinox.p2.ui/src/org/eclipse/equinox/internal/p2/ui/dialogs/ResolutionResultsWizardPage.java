/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.model.ElementUtils;
import org.eclipse.equinox.internal.p2.ui.model.QueriedElement;
import org.eclipse.equinox.internal.p2.ui.viewers.IUDetailsLabelProvider;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.IQueryable;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.internal.provisional.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProfileModificationOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * A wizard page that shows detailed information about a resolved install
 * operation.  It allows drill down into the elements that will be installed.
 * 
 * @since 3.4
 *
 */
public abstract class ResolutionResultsWizardPage extends ResolutionStatusPage {

	private static final String DIALOG_SETTINGS_SECTION = "ResolutionResultsPage"; //$NON-NLS-1$

	protected IUElementListRoot input;
	PlannerResolutionOperation resolvedOperation;
	protected Policy policy;
	TreeViewer treeViewer;
	ProvElementContentProvider contentProvider;
	IUDetailsLabelProvider labelProvider;
	protected Display display;
	private IUDetailsGroup iuDetailsGroup;
	SashForm sashForm;

	protected ResolutionResultsWizardPage(Policy policy, IUElementListRoot input, String profileID, PlannerResolutionOperation resolvedOperation) {
		super("ResolutionPage", profileID); //$NON-NLS-1$
		this.policy = policy;
		Assert.isNotNull(resolvedOperation);
		this.resolvedOperation = resolvedOperation;
		if (input == null)
			this.input = new IUElementListRoot();
		else
			this.input = input;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		display = parent.getDisplay();
		sashForm = new SashForm(parent, SWT.VERTICAL);
		FillLayout layout = new FillLayout();
		sashForm.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		sashForm.setLayoutData(data);
		initializeDialogUnits(sashForm);

		Composite composite = new Composite(sashForm, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		composite.setLayout(gridLayout);

		treeViewer = createTreeViewer(composite);
		data = new GridData(GridData.FILL_BOTH);
		data.heightHint = convertHeightInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_HEIGHT);
		data.widthHint = convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_WIDTH);
		Tree tree = treeViewer.getTree();
		tree.setLayoutData(data);
		tree.setHeaderVisible(true);
		activateCopy(tree);
		IUColumnConfig[] columns = getColumnConfig();
		for (int i = 0; i < columns.length; i++) {
			TreeColumn tc = new TreeColumn(tree, SWT.LEFT, i);
			tc.setResizable(true);
			tc.setText(columns[i].getColumnTitle());
			tc.setWidth(columns[i].getWidthInPixels(tree));
		}

		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				setDetailText(resolvedOperation);
			}
		});

		// Filters and sorters before establishing content, so we don't refresh unnecessarily.
		IUComparator comparator = new IUComparator(IUComparator.IU_NAME);
		comparator.useColumnConfig(getColumnConfig());
		treeViewer.setComparator(comparator);
		treeViewer.setComparer(new ProvElementComparer());

		contentProvider = new ProvElementContentProvider();
		treeViewer.setContentProvider(contentProvider);
		labelProvider = new IUDetailsLabelProvider(null, getColumnConfig(), getShell());
		treeViewer.setLabelProvider(labelProvider);

		setDrilldownElements(input, resolvedOperation.getProvisioningPlan());
		treeViewer.setInput(input);

		// Optional area to show the size
		createSizingInfo(composite);

		// The text area shows a description of the selected IU, or error detail if applicable.
		iuDetailsGroup = new IUDetailsGroup(sashForm, treeViewer, convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_WIDTH), true);

		updateStatus(input, resolvedOperation);
		setControl(sashForm);
		sashForm.setWeights(getSashWeights());
		Dialog.applyDialogFont(sashForm);
	}

	protected void createSizingInfo(Composite parent) {
		// Default is to do nothing
	}

	public boolean performFinish() {
		if (resolvedOperation.getResolutionResult().getSummaryStatus().getSeverity() != IStatus.ERROR) {
			ProfileModificationOperation op = createProfileModificationOperation(resolvedOperation);
			ProvisioningOperationRunner.schedule(op, StatusManager.SHOW | StatusManager.LOG);
			return true;
		}
		return false;
	}

	protected TreeViewer getTreeViewer() {
		return treeViewer;
	}

	public ProvisioningPlan getCurrentPlan() {
		return resolvedOperation.getProvisioningPlan();
	}

	protected Object[] getSelectedElements() {
		return ((IStructuredSelection) treeViewer.getSelection()).toArray();
	}

	protected IInstallableUnit getSelectedIU() {
		IInstallableUnit[] units = ElementUtils.elementsToIUs(getSelectedElements());
		if (units.length == 0)
			return null;
		return units[0];
	}

	protected String getProfileId() {
		return profileId;
	}

	protected IInstallableUnit[] getIUs() {
		return ElementUtils.elementsToIUs(input.getChildren(input));
	}

	void setDrilldownElements(IUElementListRoot root, ProvisioningPlan plan) {
		if (plan == null)
			return;
		Object[] elements = root.getChildren(root);
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] instanceof QueriedElement) {
				((QueriedElement) elements[i]).setQueryable(getQueryable(plan));
			}
		}
	}

	private ProfileModificationOperation createProfileModificationOperation(PlannerResolutionOperation op) {
		ProfileModificationOperation pmo = new ProfileModificationOperation(getOperationLabel(), profileId, op.getProvisioningPlan(), op.getProvisioningContext());
		pmo.setTaskName(getOperationTaskName());
		return pmo;
	}

	protected abstract String getOperationLabel();

	/**
	 * Returns the task name for this operation, or <code>null</code> to display
	 * a generic task name.
	 */
	protected String getOperationTaskName() {
		return null;
	}

	protected TreeViewer createTreeViewer(Composite parent) {
		return new TreeViewer(parent, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
	}

	protected abstract IQueryable getQueryable(ProvisioningPlan plan);

	protected String getClipboardText(Control control) {
		return CopyUtils.getIndentedClipboardText(getSelectedElements(), labelProvider);
	}

	protected IUDetailsGroup getDetailsGroup() {
		return iuDetailsGroup;
	}

	protected boolean isCreated() {
		return treeViewer != null;
	}

	protected void updateCaches(IUElementListRoot newRoot, PlannerResolutionOperation op) {
		resolvedOperation = op;
		setDrilldownElements(newRoot, resolvedOperation.getProvisioningPlan());
		if (treeViewer != null) {
			if (input != newRoot)
				treeViewer.setInput(newRoot);
			else
				treeViewer.refresh();
		}
		input = newRoot;
	}

	protected String getDialogSettingsName() {
		return getWizard().getClass().getName() + "." + DIALOG_SETTINGS_SECTION; //$NON-NLS-1$
	}

	protected int getColumnWidth(int index) {
		return treeViewer.getTree().getColumn(index).getWidth();
	}

	protected SashForm getSashForm() {
		return sashForm;
	}
}