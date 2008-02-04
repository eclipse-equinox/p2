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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.AvailableIUElement;
import org.eclipse.equinox.internal.p2.ui.viewers.IUDetailsLabelProvider;
import org.eclipse.equinox.internal.p2.ui.viewers.StaticContentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.director.ProvisioningContext;
import org.eclipse.equinox.p2.director.ProvisioningPlan;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.p2.ui.model.IUElement;
import org.eclipse.equinox.p2.ui.operations.ProfileModificationOperation;
import org.eclipse.equinox.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.p2.ui.viewers.IUColumnConfig;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public abstract class ProfileModificationWizardPage extends WizardPage {
	private static final int DEFAULT_HEIGHT = 20;
	private static final int DEFAULT_WIDTH = 120;
	private static final int DEFAULT_DESCRIPTION_HEIGHT = 3;
	private static final int DEFAULT_COLUMN_WIDTH = 50;
	private static final int DEFAULT_SMALL_COLUMN_WIDTH = 20;
	private IInstallableUnit[] ius;
	ProvisioningPlan currentPlan;
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
				updateDetailsArea(detailsArea, getSelectedIU());
			}
		});
		contentProvider = new StaticContentProvider(list.toArray());
		listViewer.setContentProvider(contentProvider);
		listViewer.setInput(new Object());
		listViewer.setLabelProvider(new IUDetailsLabelProvider(getColumnConfig(), getShell()));
		setInitialCheckState();
		// If the initial provisioning plan was already calculated,
		// no need to repeat it until the user changes selections
		if (currentPlan == null)
			checkedIUsChanged();

		// The text area shows a description of the selected IU.
		Group group = new Group(composite, SWT.NONE);
		group.setText(ProvUIMessages.ProfileModificationWizardPage_DetailsLabel);
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(GridData.FILL_BOTH));
		detailsArea = new Text(group, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY | SWT.WRAP);
		data = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
		data.heightHint = convertHeightInCharsToPixels(DEFAULT_DESCRIPTION_HEIGHT);
		detailsArea.setLayoutData(data);

		updateDetailsArea(detailsArea, getSelectedIU());
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

	protected IProfile getProfile() {
		try {
			return ProvisioningUtil.getProfile(profileId);
		} catch (ProvisionException e) {
			ProvUI.handleException(e, ProvUIMessages.ProfileModificationWizardPage_ProfileNotFound);
		}
		return null;
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
				setMessage(ProvUIMessages.ProfileModificationWizardPage_NothingSelected, IMessageProvider.WARNING);
			} else
				getContainer().run(true, true, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) {
						currentPlan = computeProvisioningPlan(selections, monitor);
					}
				});
		} catch (InterruptedException e) {
			// Nothing to report if thread was interrupted
		} catch (InvocationTargetException e) {
			currentPlan = null;
			ProvUI.handleException(e.getCause(), null);
			setMessage(ProvUIMessages.ProfileModificationWizardPage_UnexpectedError, IMessageProvider.ERROR);
		}
		if (currentPlan != null)
			if (currentPlan.getStatus().isOK()) {
				setPageComplete(true);
				setMessage(getDescription(), IMessageProvider.NONE);

			} else {
				int messageType = IMessageProvider.INFORMATION;
				int severity = currentPlan.getStatus().getSeverity();
				if (severity == IStatus.ERROR)
					messageType = IMessageProvider.ERROR;
				else if (severity == IStatus.WARNING)
					messageType = IMessageProvider.WARNING;
				setMessage(currentPlan.getStatus().getMessage(), messageType);
				setPageComplete(false);
				ProvUI.reportStatus(currentPlan.getStatus());
			}
	}

	private ProfileModificationOperation createProfileModificationOperation(ProvisioningPlan plan) {
		return new ProfileModificationOperation(getOperationLabel(), profileId, plan);
	}

	protected abstract ProvisioningPlan computeProvisioningPlan(Object[] checkedElements, IProgressMonitor monitor);

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

	protected void updateDetailsArea(Text details, IInstallableUnit iu) {
		String description = null;
		if (iu != null)
			description = iu.getProperty(IInstallableUnit.PROP_DESCRIPTION);
		if (description == null)
			description = ""; //$NON-NLS-1$
		details.setText(description);
	}

	protected abstract String getOperationLabel();

}