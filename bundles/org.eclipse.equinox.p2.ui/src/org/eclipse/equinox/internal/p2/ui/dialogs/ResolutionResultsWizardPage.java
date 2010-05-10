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
 *     Sonatype, Inc. - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.util.Collection;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.internal.p2.ui.viewers.*;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
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
	ProfileChangeOperation resolvedOperation;
	TreeViewer treeViewer;
	ProvElementContentProvider contentProvider;
	IUDetailsLabelProvider labelProvider;
	protected Display display;
	private IUDetailsGroup iuDetailsGroup;
	SashForm sashForm;

	protected ResolutionResultsWizardPage(ProvisioningUI ui, ProvisioningOperationWizard wizard, IUElementListRoot input, ProfileChangeOperation operation) {
		super("ResolutionPage", ui, wizard); //$NON-NLS-1$
		// We can exist as an empty page, but if there is an operation, we need to know that it's resolved.
		if (operation != null && !operation.hasResolved()) {
			operation.resolveModal(null);
		}
		this.resolvedOperation = operation;
		if (input == null)
			this.input = new IUElementListRoot();
		else
			this.input = input;
		updateStatus(input, resolvedOperation);
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

		setDrilldownElements(input, resolvedOperation);
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
		if (resolvedOperation.getResolutionResult().getSeverity() != IStatus.ERROR) {
			getProvisioningUI().schedule(resolvedOperation.getProvisioningJob(null), StatusManager.SHOW | StatusManager.LOG);
			return true;
		}
		return false;
	}

	protected TreeViewer getTreeViewer() {
		return treeViewer;
	}

	public IProvisioningPlan getCurrentPlan() {
		if (resolvedOperation != null)
			return resolvedOperation.getProvisioningPlan();
		return null;
	}

	protected Object[] getSelectedElements() {
		return ((IStructuredSelection) treeViewer.getSelection()).toArray();
	}

	protected IInstallableUnit getSelectedIU() {
		java.util.List<IInstallableUnit> units = ElementUtils.elementsToIUs(getSelectedElements());
		if (units.size() == 0)
			return null;
		return units.get(0);
	}

	protected boolean shouldCompleteOnCancel() {
		return false;
	}

	protected Collection<IInstallableUnit> getIUs() {
		return ElementUtils.elementsToIUs(input.getChildren(input));
	}

	void setDrilldownElements(IUElementListRoot root, ProfileChangeOperation operation) {
		if (operation == null || operation.getProvisioningPlan() == null)
			return;
		Object[] elements = root.getChildren(root);
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] instanceof QueriedElement) {
				((QueriedElement) elements[i]).setQueryable(getQueryable(operation.getProvisioningPlan()));
			}
		}
	}

	protected abstract String getOperationLabel();

	/**
	 * Returns the restart policy for this operation.
	 * 
	 * @return an integer constant describing whether the running profile
	 * needs to be restarted. 
	 * 
	 * @see ProvisioningJob#RESTART_NONE
	 * @see ProvisioningJob#RESTART_ONLY
	 * @see ProvisioningJob#RESTART_OR_APPLY
	 *
	 */
	protected int getRestartPolicy() {
		return ProvisioningJob.RESTART_OR_APPLY;
	}

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

	protected abstract IQueryable<IInstallableUnit> getQueryable(IProvisioningPlan plan);

	protected String getClipboardText(Control control) {
		return CopyUtils.getIndentedClipboardText(getSelectedElements(), labelProvider);
	}

	protected IUDetailsGroup getDetailsGroup() {
		return iuDetailsGroup;
	}

	protected boolean isCreated() {
		return treeViewer != null;
	}

	protected void updateCaches(IUElementListRoot newRoot, ProfileChangeOperation op) {
		resolvedOperation = op;
		if (newRoot != null) {
			setDrilldownElements(newRoot, resolvedOperation);
			if (treeViewer != null) {
				if (input != newRoot)
					treeViewer.setInput(newRoot);
				else
					treeViewer.refresh();
			}
			input = newRoot;
		}
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