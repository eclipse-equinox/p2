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
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.AvailableIUElement;
import org.eclipse.equinox.internal.p2.ui.viewers.IUDetailsLabelProvider;
import org.eclipse.equinox.internal.p2.ui.viewers.StaticContentProvider;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.internal.provisional.p2.ui.model.IUElement;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProfileModificationOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.statushandlers.StatusManager;

public abstract class ProfileModificationWizardPage extends WizardPage {
	private static final int DEFAULT_HEIGHT = 20;
	private static final int DEFAULT_WIDTH = 120;
	private static final int DEFAULT_DESCRIPTION_HEIGHT = 4;
	private static final int DEFAULT_COLUMN_WIDTH = 50;
	private static final int DEFAULT_SMALL_COLUMN_WIDTH = 20;
	private static final String NESTING_INDENT = "  "; //$NON-NLS-1$
	private static final IStatus NULL_PLAN_STATUS = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, 0, ProvUIMessages.ProfileModificationWizardPage_UnexpectedError, null);
	private IInstallableUnit[] ius;
	ProvisioningPlan currentPlan;
	IStatus currentStatus;
	private String profileId;
	CheckboxTableViewer listViewer;
	Text detailsArea;
	StaticContentProvider contentProvider;

	protected ProfileModificationWizardPage(String id, IInstallableUnit[] ius, String profileID, ProvisioningPlan initialPlan) {
		super(id);
		this.ius = ius;
		this.profileId = profileID;
		this.currentPlan = initialPlan;
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(data);
		initializeDialogUnits(composite);

		// The viewer allows selection of IU's for browsing the details,
		// and checking to include in the provisioning operation.
		listViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.FULL_SELECTION);
		data = new GridData(GridData.FILL_BOTH);
		data.heightHint = convertHeightInCharsToPixels(DEFAULT_HEIGHT);
		data.widthHint = convertWidthInCharsToPixels(DEFAULT_WIDTH);
		Table table = listViewer.getTable();
		table.setLayoutData(data);
		table.setHeaderVisible(true);
		IUColumnConfig[] columns = getColumnConfig();
		for (int i = 0; i < columns.length; i++) {
			TableColumn tc = new TableColumn(table, SWT.LEFT, i);
			tc.setResizable(true);
			tc.setText(columns[i].columnTitle);
			if (columns[i].columnField == IUColumnConfig.COLUMN_SIZE) {
				tc.setAlignment(SWT.RIGHT);
				tc.setWidth(convertWidthInCharsToPixels(DEFAULT_SMALL_COLUMN_WIDTH));
			} else
				tc.setWidth(convertWidthInCharsToPixels(DEFAULT_COLUMN_WIDTH));
		}
		final List list = new ArrayList(ius.length);
		makeElements(getIUs(), list);

		listViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				checkedIUsChanged();
			}
		});

		listViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateStatus();
			}
		});

		// Filters and sorters before establishing content, so we don't refresh unnecessarily.
		listViewer.setComparator(new IUComparator(IUComparator.IU_ID));
		listViewer.setComparer(new ProvElementComparer());

		contentProvider = new StaticContentProvider(list.toArray());
		listViewer.setContentProvider(contentProvider);
		listViewer.setInput(new Object());
		listViewer.setLabelProvider(new IUDetailsLabelProvider(getColumnConfig(), getShell()));
		setInitialCheckState();
		// If the initial provisioning plan was already calculated,
		// no need to repeat it until the user changes selections
		if (currentPlan == null)
			checkedIUsChanged();
		else
			currentStatus = currentPlan.getStatus();

		// The text area shows a description of the selected IU, or error detail if applicable.
		Group group = new Group(composite, SWT.NONE);
		group.setText(ProvUIMessages.ProfileModificationWizardPage_DetailsLabel);
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_BOTH));
		detailsArea = new Text(group, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY | SWT.WRAP);
		data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.heightHint = convertHeightInCharsToPixels(DEFAULT_DESCRIPTION_HEIGHT);
		data.widthHint = convertWidthInCharsToPixels(DEFAULT_WIDTH);
		detailsArea.setLayoutData(data);

		updateStatus();
		setControl(composite);
		Dialog.applyDialogFont(composite);
	}

	protected void makeElements(IInstallableUnit[] iusToShow, List list) {
		for (int i = 0; i < iusToShow.length; i++) {
			list.add(new AvailableIUElement(iusToShow[i], getProfileId()));
		}
	}

	public boolean performFinish() {
		if (currentPlan != null && currentPlan.getStatus().isOK()) {
			ProfileModificationOperation op = createProfileModificationOperation(currentPlan);
			ProvisioningOperationRunner.schedule(op, getShell());
			return true;
		}
		return false;
	}

	private Object[] getCheckedElements() {
		return listViewer.getCheckedElements();
	}

	protected Object[] getSelectedElements() {
		return ((IStructuredSelection) listViewer.getSelection()).toArray();
	}

	protected IInstallableUnit[] elementsToIUs(Object[] elements) {
		IInstallableUnit[] theIUs = new IInstallableUnit[elements.length];
		for (int i = 0; i < elements.length; i++) {
			theIUs[i] = (IInstallableUnit) ProvUI.getAdapter(elements[i], IInstallableUnit.class);
		}
		return theIUs;
	}

	protected IInstallableUnit getSelectedIU() {
		IInstallableUnit[] units = elementsToIUs(getSelectedElements());
		if (units.length == 0)
			return null;
		return units[0];
	}

	protected IInstallableUnit[] getCheckedIUs() {
		return elementsToIUs(getCheckedElements());
	}

	protected String getProfileId() {
		return profileId;
	}

	protected IInstallableUnit[] getIUs() {
		return ius;
	}

	protected IUColumnConfig[] getColumnConfig() {
		return ProvUI.getIUColumnConfig();
	}

	protected long getSize(IInstallableUnit iu, IProgressMonitor monitor) {
		return IUElement.SIZE_UNKNOWN;
	}

	protected void checkedIUsChanged() {
		try {
			final Object[] selections = getCheckedElements();
			if (selections.length == 0) {
				currentPlan = null;
				currentStatus = new Status(IStatus.WARNING, ProvUIActivator.PLUGIN_ID, ProvUIMessages.ProfileModificationWizardPage_NothingSelected);
			} else
				getContainer().run(true, true, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) {
						try {
							currentPlan = computeProvisioningPlan(selections, monitor);
							if (currentPlan != null)
								currentStatus = currentPlan.getStatus();
							else
								currentStatus = NULL_PLAN_STATUS;
						} catch (ProvisionException e) {
							currentPlan = null;
							currentStatus = ProvUI.handleException(e.getCause(), ProvUIMessages.ProfileModificationWizardPage_UnexpectedError, StatusManager.LOG);
						}
					}
				});
		} catch (InterruptedException e) {
			// Nothing to report if thread was interrupted
		} catch (InvocationTargetException e) {
			currentPlan = null;
			currentStatus = ProvUI.handleException(e.getCause(), ProvUIMessages.ProfileModificationWizardPage_UnexpectedError, StatusManager.LOG);
		}
		updateStatus();
	}

	private ProfileModificationOperation createProfileModificationOperation(ProvisioningPlan plan) {
		return new ProfileModificationOperation(getOperationLabel(), profileId, plan);
	}

	protected abstract ProvisioningPlan computeProvisioningPlan(Object[] checkedElements, IProgressMonitor monitor) throws ProvisionException;

	protected void setInitialCheckState() {
		// The default is to check everything because 
		// in most cases, the user has selected exactly
		// what they want before this page opens.
		listViewer.setAllChecked(true);
	}

	// We currently create an empty provisioning context, but
	// in the future we could consider letting clients supply this.
	protected ProvisioningContext getProvisioningContext() {
		return new ProvisioningContext();
	}

	protected abstract String getOperationLabel();

	void updateStatus() {
		int messageType = IMessageProvider.NONE;
		if (currentStatus != null && !currentStatus.isOK()) {
			messageType = IMessageProvider.INFORMATION;
			int severity = currentStatus.getSeverity();
			if (severity == IStatus.ERROR)
				messageType = IMessageProvider.ERROR;
			else if (severity == IStatus.WARNING)
				messageType = IMessageProvider.WARNING;
			setPageComplete(false);
			ProvUI.reportStatus(currentStatus, StatusManager.LOG);
		} else {
			setPageComplete(true);
		}
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
		String description = iu.getProperty(IInstallableUnit.PROP_DESCRIPTION);
		if (description == null)
			description = ""; //$NON-NLS-1$
		return description;
	}
}