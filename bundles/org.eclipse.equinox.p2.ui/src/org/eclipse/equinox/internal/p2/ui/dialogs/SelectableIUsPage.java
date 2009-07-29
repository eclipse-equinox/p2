/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.ui.*;
import org.eclipse.equinox.internal.p2.ui.model.ElementUtils;
import org.eclipse.equinox.internal.p2.ui.viewers.IUDetailsLabelProvider;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

/**
 * A wizard page that presents a check box list of IUs and allows the user
 * to select and deselect them.  Typically the first page in a provisioning
 * operation wizard, and usually it is the page used to report resolution errors
 * before advancing to resolution detail.
 * 
 * @since 3.5
 *
 */
public class SelectableIUsPage extends ResolutionStatusPage implements IResolutionErrorReportingPage {

	private static final String DIALOG_SETTINGS_SECTION = "SelectableIUsPage"; //$NON-NLS-1$

	IUElementListRoot root;
	Object[] initialSelections;
	PlannerResolutionOperation resolvedOperation;
	CheckboxTableViewer tableViewer;
	IUDetailsGroup iuDetailsGroup;
	ProvElementContentProvider contentProvider;
	IUDetailsLabelProvider labelProvider;
	protected Display display;
	protected Policy policy;
	SashForm sashForm;

	public SelectableIUsPage(Policy policy, IUElementListRoot root, Object[] initialSelections, String profileId) {
		super("IUSelectionPage", profileId); //$NON-NLS-1$
		this.root = root;
		this.policy = policy;
		this.initialSelections = initialSelections;
		this.profileId = profileId;
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

		tableViewer = createTableViewer(composite);
		data = new GridData(GridData.FILL_BOTH);
		data.heightHint = convertHeightInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_HEIGHT);
		data.widthHint = convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_WIDTH);
		Table table = tableViewer.getTable();
		table.setLayoutData(data);
		table.setHeaderVisible(true);
		activateCopy(table);
		IUColumnConfig[] columns = getColumnConfig();
		for (int i = 0; i < columns.length; i++) {
			TableColumn tc = new TableColumn(table, SWT.LEFT, i);
			tc.setResizable(true);
			tc.setText(columns[i].getColumnTitle());
			tc.setWidth(columns[i].getWidthInPixels(table));
		}

		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				setDetailText(resolvedOperation);
			}
		});

		tableViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				setPageComplete(tableViewer.getCheckedElements().length > 0);
			}
		});

		// Filters and sorters before establishing content, so we don't refresh unnecessarily.
		IUComparator comparator = new IUComparator(IUComparator.IU_NAME);
		comparator.useColumnConfig(ProvUI.getIUColumnConfig());
		tableViewer.setComparator(comparator);
		tableViewer.setComparer(new ProvElementComparer());

		contentProvider = new ProvElementContentProvider();
		tableViewer.setContentProvider(contentProvider);
		tableViewer.setInput(root);
		labelProvider = new IUDetailsLabelProvider(null, ProvUI.getIUColumnConfig(), getShell());
		tableViewer.setLabelProvider(labelProvider);
		setInitialCheckState();

		// Select and Deselect All buttons
		createSelectButtons(composite);

		// The text area shows a description of the selected IU, or error detail if applicable.
		iuDetailsGroup = new IUDetailsGroup(sashForm, tableViewer, convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_WIDTH), true);

		updateStatus(root, resolvedOperation);
		setControl(sashForm);
		sashForm.setWeights(getSashWeights());
		Dialog.applyDialogFont(sashForm);
	}

	private void createSelectButtons(Composite parent) {
		Composite buttonParent = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		gridLayout.marginWidth = 0;
		gridLayout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		buttonParent.setLayout(gridLayout);
		GridData data = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
		buttonParent.setLayoutData(data);

		Button selectAll = new Button(buttonParent, SWT.PUSH);
		selectAll.setText(ProvUIMessages.SelectableIUsPage_Select_All);
		setButtonLayoutData(selectAll);
		selectAll.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				tableViewer.setAllChecked(true);
				setPageComplete(tableViewer.getCheckedElements().length > 0);
			}
		});

		Button deselectAll = new Button(buttonParent, SWT.PUSH);
		deselectAll.setText(ProvUIMessages.SelectableIUsPage_Deselect_All);
		setButtonLayoutData(deselectAll);
		deselectAll.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				tableViewer.setAllChecked(false);
				setPageComplete(tableViewer.getCheckedElements().length > 0);
			}
		});

		// dummy to take extra space
		Label dummy = new Label(buttonParent, SWT.NONE);
		data = new GridData(SWT.FILL, SWT.FILL, true, true);
		dummy.setLayoutData(data);

		// separator underneath
		Label sep = new Label(buttonParent, SWT.HORIZONTAL | SWT.SEPARATOR);
		data = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
		data.horizontalSpan = 3;
		sep.setLayoutData(data);
	}

	protected CheckboxTableViewer createTableViewer(Composite parent) {
		// The viewer allows selection of IU's for browsing the details,
		// and checking to include in the provisioning operation.
		CheckboxTableViewer v = CheckboxTableViewer.newCheckList(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		return v;
	}

	public Object[] getCheckedIUElements() {
		if (tableViewer == null)
			return initialSelections;
		return tableViewer.getCheckedElements();
	}

	public Object[] getSelectedIUElements() {
		return ((IStructuredSelection) tableViewer.getSelection()).toArray();
	}

	protected Object[] getSelectedElements() {
		return ((IStructuredSelection) tableViewer.getSelection()).toArray();
	}

	protected IInstallableUnit[] elementsToIUs(Object[] elements) {
		IInstallableUnit[] theIUs = new IInstallableUnit[elements.length];
		for (int i = 0; i < elements.length; i++) {
			theIUs[i] = (IInstallableUnit) ProvUI.getAdapter(elements[i], IInstallableUnit.class);
		}
		return theIUs;
	}

	protected void setInitialCheckState() {
		tableViewer.setCheckedElements(initialSelections);
	}

	/*
	 * Overridden so that we don't call getNextPage().
	 * We use getNextPage() to start resolving the operation so
	 * we only want to do that when the next button is pressed.
	 * 
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardPage#canFlipToNextPage()
	 */
	public boolean canFlipToNextPage() {
		return isPageComplete();
	}

	/*
	 * Overridden to null out any cached page so that the wizard
	 * is always consulted.  This allows wizards to do things like
	 * synchronize previous page selections with this page.
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardPage#getPreviousPage()
	 */
	public IWizardPage getPreviousPage() {
		setPreviousPage(null);
		return super.getPreviousPage();
	}

	protected String getClipboardText(Control control) {
		StringBuffer buffer = new StringBuffer();
		Object[] elements = getSelectedElements();
		for (int i = 0; i < elements.length; i++) {
			if (i > 0)
				buffer.append(CopyUtils.NEWLINE);
			buffer.append(labelProvider.getClipboardText(elements[i], CopyUtils.DELIMITER));
		}
		return buffer.toString();
	}

	protected IInstallableUnit getSelectedIU() {
		IInstallableUnit[] units = ElementUtils.elementsToIUs(getSelectedElements());
		if (units.length == 0)
			return null;
		return units[0];
	}

	/*
	 * Overridden to handle conditions where continuing with the operation should not be allowed.
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.dialogs.ResolutionStatusPage#updateStatus(org.eclipse.equinox.internal.p2.ui.model.IUElementListRoot, org.eclipse.equinox.internal.provisional.p2.ui.operations.PlannerResolutionOperation)
	 */
	public void updateStatus(IUElementListRoot newRoot, PlannerResolutionOperation op) {
		IStatus specialStatus = null;
		if (ProvisioningOperationRunner.hasScheduledOperationsFor(profileId)) {
			specialStatus = PlanAnalyzer.getStatus(IStatusCodes.OPERATION_ALREADY_IN_PROGRESS, null);
		} else if (op == null) {
			specialStatus = new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, 0, ProvUIMessages.ProfileModificationWizardPage_UnexpectedError, null);
		}
		if (specialStatus == null) {
			super.updateStatus(newRoot, op);
		} else {
			updateCaches(newRoot, op);
			setPageComplete(false);
			if (!isCreated())
				return;
			getDetailsGroup().setDetailText(specialStatus.getMessage());
			setMessage(getMessageText(specialStatus), IMessageProvider.ERROR);

		}
	}

	protected IUDetailsGroup getDetailsGroup() {
		return iuDetailsGroup;
	}

	protected boolean isCreated() {
		return tableViewer != null;
	}

	protected void updateCaches(IUElementListRoot newRoot, PlannerResolutionOperation op) {
		resolvedOperation = op;
		if (root != newRoot && tableViewer != null)
			tableViewer.setInput(newRoot);
		root = newRoot;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.dialogs.ISelectableIUsPage#setCheckedElements(java.lang.Object[])
	 */
	public void setCheckedElements(Object[] elements) {
		if (tableViewer == null)
			initialSelections = elements;
		else
			tableViewer.setCheckedElements(elements);
	}

	protected SashForm getSashForm() {
		return sashForm;
	}

	protected String getDialogSettingsName() {
		return getWizard().getClass().getName() + "." + DIALOG_SETTINGS_SECTION; //$NON-NLS-1$
	}

	protected int getColumnWidth(int index) {
		return tableViewer.getTable().getColumn(index).getWidth();
	}
}
