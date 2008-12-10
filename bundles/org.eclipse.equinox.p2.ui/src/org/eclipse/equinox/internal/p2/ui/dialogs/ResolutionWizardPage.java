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
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.internal.p2.ui.viewers.IUDetailsLabelProvider;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProfileModificationOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.statushandlers.StatusManager;

public abstract class ResolutionWizardPage extends WizardPage {
	private static final String NESTING_INDENT = "  "; //$NON-NLS-1$
	static final IStatus NULL_PLAN_STATUS = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, 0, ProvUIMessages.ProfileModificationWizardPage_UnexpectedError, null);
	protected IUElementListRoot input;
	ProvisioningPlan currentPlan;
	IStatus currentStatus;
	private String profileId;
	protected Policy policy;
	TreeViewer treeViewer;
	Text detailsArea;
	ProvElementContentProvider contentProvider;
	protected Display display;

	protected ResolutionWizardPage(Policy policy, String id, IUElementListRoot input, String profileID, ProvisioningPlan initialPlan) {
		super(id);
		this.policy = policy;
		currentPlan = initialPlan;
		if (input == null)
			this.input = new IUElementListRoot();
		else
			this.input = input;
		this.profileId = profileID;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		display = parent.getDisplay();
		SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
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
		IUColumnConfig[] columns = getColumnConfig();
		for (int i = 0; i < columns.length; i++) {
			TreeColumn tc = new TreeColumn(tree, SWT.LEFT, i);
			tc.setResizable(true);
			tc.setText(columns[i].columnTitle);
			tc.setWidth(convertWidthInCharsToPixels(columns[i].defaultColumnWidth));
		}

		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateStatus();
			}
		});

		// Filters and sorters before establishing content, so we don't refresh unnecessarily.
		treeViewer.setComparator(new IUComparator(IUComparator.IU_NAME));
		treeViewer.setComparer(new ProvElementComparer());

		contentProvider = new ProvElementContentProvider();
		treeViewer.setContentProvider(contentProvider);
		treeViewer.setLabelProvider(new IUDetailsLabelProvider(null, getColumnConfig(), getShell()));

		if (currentPlan == null)
			// this will also set the input on the viewer
			recomputePlan(input);
		else {
			treeViewer.setInput(input);
			currentStatus = PlanStatusHelper.computeStatus(currentPlan, ElementUtils.elementsToIUs(input.getChildren(input)));
		}

		// Optional area to show the size
		createSizingInfo(composite);

		// The text area shows a description of the selected IU, or error detail if applicable.
		Group group = new Group(sashForm, SWT.NONE);
		group.setText(ProvUIMessages.ProfileModificationWizardPage_DetailsLabel);
		group.setLayout(new GridLayout());

		createDetailsArea(group);

		updateStatus();
		setControl(sashForm);
		sashForm.setWeights(new int[] {80, 20});
		Dialog.applyDialogFont(sashForm);
	}

	protected void createSizingInfo(Composite parent) {
		// Default is to do nothing
	}

	protected void createDetailsArea(Composite parent) {
		detailsArea = new Text(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY | SWT.WRAP);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.heightHint = convertHeightInCharsToPixels(ILayoutConstants.DEFAULT_DESCRIPTION_HEIGHT);
		data.widthHint = convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_WIDTH);
		detailsArea.setLayoutData(data);
	}

	public boolean performFinish() {
		if (currentStatus != null && currentStatus.getSeverity() != IStatus.ERROR) {
			ProfileModificationOperation op = createProfileModificationOperation(currentPlan);
			ProvisioningOperationRunner.schedule(op, getShell(), StatusManager.SHOW | StatusManager.LOG);
			return true;
		}
		return false;
	}

	protected TreeViewer getTreeViewer() {
		return treeViewer;
	}

	public ProvisioningPlan getCurrentPlan() {
		return currentPlan;
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

	protected IUColumnConfig[] getColumnConfig() {
		// TODO we could consider making this settable via API, but for now we rely on
		// a standard column config.  We intentionally use the IU's id as one of the columns, because
		// resolution errors are reported by ID.
		return new IUColumnConfig[] {new IUColumnConfig(ProvUIMessages.ProvUI_NameColumnTitle, IUColumnConfig.COLUMN_NAME, ILayoutConstants.DEFAULT_COLUMN_WIDTH), new IUColumnConfig(ProvUIMessages.ProvUI_VersionColumnTitle, IUColumnConfig.COLUMN_VERSION, ILayoutConstants.DEFAULT_SMALL_COLUMN_WIDTH), new IUColumnConfig(ProvUIMessages.ProvUI_IdColumnTitle, IUColumnConfig.COLUMN_ID, ILayoutConstants.DEFAULT_COLUMN_WIDTH)};

	}

	public void recomputePlan(IUElementListRoot root) {
		this.input = root;
		final Object[] elements = root.getChildren(root);
		final IInstallableUnit[] ius = ElementUtils.elementsToIUs(elements);
		try {
			if (elements.length == 0) {
				currentPlan = null;
				currentStatus = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, IStatusCodes.EXPECTED_NOTHING_TO_DO, ProvUIMessages.ProfileModificationWizardPage_NothingSelected, null);
			} else
				getContainer().run(true, true, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) {
						currentPlan = null;
						currentStatus = NULL_PLAN_STATUS;
						MultiStatus status = PlanStatusHelper.getProfileChangeAlteredStatus();
						ProfileChangeRequest request = computeProfileChangeRequest(elements, status, monitor);
						if (request != null) {
							PlannerResolutionOperation op = new PlannerResolutionOperation(ProvUIMessages.ProfileModificationWizardPage_ResolutionOperationLabel, ius, getProfileId(), request, status, false);
							try {
								op.execute(monitor, ProvUI.getUIInfoAdapter(getShell()));
							} catch (ExecutionException e) {
								currentStatus = ProvUI.handleException(e.getCause(), ProvUIMessages.ProfileModificationWizardPage_UnexpectedError, StatusManager.LOG);
							}
							currentPlan = op.getProvisioningPlan();
							if (currentPlan != null) {
								for (int i = 0; i < elements.length; i++) {
									if (elements[i] instanceof QueriedElement) {
										((QueriedElement) elements[i]).setQueryable(getQueryable(currentPlan));
									}
								}
								currentStatus = currentPlan.getStatus();
							}
						}
					}
				});
		} catch (InterruptedException e) {
			// Nothing to report if thread was interrupted
		} catch (InvocationTargetException e) {
			currentPlan = null;
			currentStatus = ProvUI.handleException(e.getCause(), ProvUIMessages.ProfileModificationWizardPage_UnexpectedError, StatusManager.LOG);
		}
		treeViewer.setInput(input);
		updateStatus();
	}

	private ProfileModificationOperation createProfileModificationOperation(ProvisioningPlan plan) {
		return new ProfileModificationOperation(getOperationLabel(), profileId, plan);
	}

	protected abstract ProfileChangeRequest computeProfileChangeRequest(Object[] checkedElements, MultiStatus additionalStatus, IProgressMonitor monitor);

	// We currently create an empty provisioning context, but
	// in the future we could consider letting clients supply this.
	protected ProvisioningContext getProvisioningContext() {
		return new ProvisioningContext();
	}

	protected abstract String getOperationLabel();

	void updateStatus() {
		if (detailsArea == null || detailsArea.isDisposed())
			return;
		int messageType = IMessageProvider.NONE;
		boolean pageComplete = true;
		if (currentStatus != null && !currentStatus.isOK()) {
			messageType = IMessageProvider.INFORMATION;
			int severity = currentStatus.getSeverity();
			if (severity == IStatus.ERROR) {
				messageType = IMessageProvider.ERROR;
				pageComplete = false;
				// Log errors for later support, but not if these are 
				// simple UI validation errors.
				if (currentStatus.getCode() != IStatusCodes.EXPECTED_NOTHING_TO_DO)
					ProvUI.reportStatus(currentStatus, StatusManager.LOG);
			} else if (severity == IStatus.WARNING) {
				messageType = IMessageProvider.WARNING;
				// Log warnings for later support
				ProvUI.reportStatus(currentStatus, StatusManager.LOG);
			}
		} else {
			// Check to see if another operation is in progress
			if (ProvisioningOperationRunner.hasScheduledOperationsFor(profileId)) {
				messageType = IMessageProvider.ERROR;
				currentStatus = PlanStatusHelper.getStatus(IStatusCodes.OPERATION_ALREADY_IN_PROGRESS, null);
				pageComplete = false;
			}
		}
		setPageComplete(pageComplete);
		setMessage(getMessageText(), messageType);
		detailsArea.setText(getDetailText());
	}

	String getDetailText() {
		String detail = ""; //$NON-NLS-1$
		if (currentStatus == null || currentStatus.isOK()) {
			IInstallableUnit iu = getSelectedIU();
			if (iu != null)
				detail = getIUDescription(iu);
		} else {
			// current status is not OK.  See if there are embedded exceptions or status to report
			StringBuffer buffer = new StringBuffer();
			appendDetailText(currentStatus, buffer, -1, false);
			detail = buffer.toString();
		}
		return detail;
	}

	void appendDetailText(IStatus status, StringBuffer buffer, int indent, boolean includeTopLevel) {
		for (int i = 0; i < indent; i++)
			buffer.append(NESTING_INDENT);
		if (includeTopLevel && status.getMessage() != null)
			buffer.append(status.getMessage());
		Throwable t = status.getException();
		if (t != null) {
			// A provision (or core) exception occurred.  Get its status message or if none, its top level message.
			if (t instanceof CoreException) {
				IStatus exceptionStatus = ((CoreException) t).getStatus();
				if (exceptionStatus != null && exceptionStatus.getMessage() != null)
					buffer.append(exceptionStatus.getMessage());
				else {
					String details = t.getLocalizedMessage();
					if (details != null)
						buffer.append(details);
				}
			} else {
				String details = t.getLocalizedMessage();
				if (details != null)
					buffer.append(details);
			}
		} else {
			// This is the most important case.  No exception occurred, we have a non-OK status after trying
			// to get a provisioning plan.  It's important not to lose the multi status information.  The top level status
			// message has already been reported 
			IStatus[] children = status.getChildren();
			for (int i = 0; i < children.length; i++) {
				appendDetailText(children[i], buffer, indent + 1, true);
				buffer.append('\n');
			}
		}
	}

	String getMessageText() {
		if (currentStatus == null || currentStatus.isOK())
			return getDescription();
		return currentStatus.getMessage();
	}

	protected String getIUDescription(IInstallableUnit iu) {
		// Get the iu description in the default locale
		String description = IUPropertyUtils.getIUProperty(iu, IInstallableUnit.PROP_DESCRIPTION);
		if (description == null)
			description = ""; //$NON-NLS-1$
		return description;
	}

	protected TreeViewer createTreeViewer(Composite parent) {
		return new TreeViewer(parent, SWT.BORDER | SWT.FULL_SELECTION);
	}

	protected abstract IQueryable getQueryable(ProvisioningPlan plan);
}