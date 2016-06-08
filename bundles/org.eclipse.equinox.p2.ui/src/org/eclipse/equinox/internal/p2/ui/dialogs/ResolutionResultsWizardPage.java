/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource - ongoing development
 *     Sonatype, Inc. - ongoing development
 *     Red Hat, Inc. - support for remediation page
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import java.util.Collection;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.internal.p2.ui.viewers.*;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
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
		this.resolvedOperation = operation;
		if (input == null)
			this.input = new IUElementListRoot(ui);
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
		TreeViewerColumn nameColumn = new TreeViewerColumn(treeViewer, SWT.LEFT);
		nameColumn.getColumn().setText(ProvUIMessages.ProvUI_NameColumnTitle);
		nameColumn.getColumn().setWidth(400);
		nameColumn.getColumn().setMoveable(true);
		nameColumn.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				IInstallableUnit iu = ProvUI.getAdapter(element, IInstallableUnit.class);
				String label = iu.getProperty(IInstallableUnit.PROP_NAME, null);
				if (label == null)
					label = iu.getId();
				return label;
			}

			public Image getImage(Object element) {
				if (element instanceof ProvElement)
					return ((ProvElement) element).getImage(element);
				if (ProvUI.getAdapter(element, IInstallableUnit.class) != null)
					return ProvUIImages.getImage(ProvUIImages.IMG_IU);
				return null;
			}

			public String getToolTipText(Object element) {
				if (element instanceof AvailableIUElement && ((AvailableIUElement) element).getImageOverlayId(null) == ProvUIImages.IMG_INFO)
					return ProvUIMessages.RemedyElementNotHighestVersion;
				return super.getToolTipText(element);
			}
		});
		TreeViewerColumn versionColumn = new TreeViewerColumn(treeViewer, SWT.LEFT);
		versionColumn.getColumn().setText(ProvUIMessages.ProvUI_VersionColumnTitle);
		versionColumn.getColumn().setWidth(200);
		versionColumn.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				IInstallableUnit iu = ProvUI.getAdapter(element, IInstallableUnit.class);
				if (element instanceof IIUElement) {
					if (((IIUElement) element).shouldShowVersion())
						return iu.getVersion().toString();
					return ""; //$NON-NLS-1$
				}
				return iu.getVersion().toString();
			}
		});
		TreeViewerColumn idColumn = new TreeViewerColumn(treeViewer, SWT.LEFT);
		idColumn.getColumn().setText(ProvUIMessages.ProvUI_IdColumnTitle);
		idColumn.getColumn().setWidth(200);

		idColumn.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				IInstallableUnit iu = ProvUI.getAdapter(element, IInstallableUnit.class);
				return iu.getId();
			}
		});

		// Filters and sorters before establishing content, so we don't refresh unnecessarily.
		IUComparator comparator = new IUComparator(IUComparator.IU_NAME);
		comparator.useColumnConfig(getColumnConfig());
		treeViewer.setComparator(comparator);
		treeViewer.setComparer(new ProvElementComparer());
		ColumnViewerToolTipSupport.enableFor(treeViewer);
		contentProvider = new ProvElementContentProvider();
		treeViewer.setContentProvider(contentProvider);
		//		labelProvider = new IUDetailsLabelProvider(null, getColumnConfig(), getShell());
		//		treeViewer.setLabelProvider(labelProvider);

		// Optional area to show the size
		createSizingInfo(composite);

		// The text area shows a description of the selected IU, or error detail if applicable.
		iuDetailsGroup = new IUDetailsGroup(sashForm, treeViewer, convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_WIDTH), true);

		setControl(sashForm);
		sashForm.setWeights(getSashWeights());
		Dialog.applyDialogFont(sashForm);

		// Controls for filtering/presentation/site selection
		Composite controlsComposite = new Composite(composite, SWT.NONE);
		gridLayout = new GridLayout();
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.numColumns = 2;
		gridLayout.makeColumnsEqualWidth = true;
		gridLayout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		controlsComposite.setLayout(layout);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		controlsComposite.setLayoutData(gd);

		final Runnable runnable = new Runnable() {
			public void run() {
				treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
					public void selectionChanged(SelectionChangedEvent event) {
						setDetailText(resolvedOperation);
					}
				});
				setDrilldownElements(input, resolvedOperation);
				treeViewer.setInput(input);
			}
		};

		if (resolvedOperation != null && !resolvedOperation.hasResolved()) {
			try {
				getContainer().run(true, false, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) {
						resolvedOperation.resolveModal(monitor);
						display.asyncExec(runnable);
					}
				});
			} catch (Exception e) {
				StatusManager.getManager().handle(new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, e.getMessage(), e));
			}
		} else {
			runnable.run();
		}
	}

	@Override
	public void updateStatus(IUElementListRoot newRoot, ProfileChangeOperation op) {
		super.updateStatus(newRoot, op);
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