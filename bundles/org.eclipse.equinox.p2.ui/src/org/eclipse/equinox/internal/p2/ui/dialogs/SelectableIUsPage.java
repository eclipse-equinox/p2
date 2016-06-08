/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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

import java.util.ArrayList;
import org.eclipse.equinox.internal.p2.ui.ProvUI;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.*;
import org.eclipse.equinox.internal.p2.ui.viewers.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.ProfileChangeOperation;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
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
	ProfileChangeOperation resolvedOperation;
	CheckboxTableViewer tableViewer;
	IUDetailsGroup iuDetailsGroup;
	ProvElementContentProvider contentProvider;
	IUDetailsLabelProvider labelProvider;
	protected Display display;
	protected Policy policy;
	SashForm sashForm;

	public SelectableIUsPage(ProvisioningUI ui, ProvisioningOperationWizard wizard, IUElementListRoot root, Object[] initialSelections) {
		super("IUSelectionPage", ui, wizard); //$NON-NLS-1$
		this.root = root;
		if (root == null)
			root = new IUElementListRoot(ui);
		if (initialSelections == null)
			this.initialSelections = new IInstallableUnit[0];
		else
			this.initialSelections = initialSelections;

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
				// If the checkEvent is on a locked update element, uncheck it and select it.
				if (event.getElement() instanceof AvailableUpdateElement) {
					AvailableUpdateElement checkedElement = (AvailableUpdateElement) event.getElement();
					if (checkedElement.isLockedForUpdate()) {
						event.getCheckable().setChecked(checkedElement, false);
						// Select the element so that the locked description is displayed
						CheckboxTableViewer viewer = ((CheckboxTableViewer) event.getSource());
						int itemCount = viewer.getTable().getItemCount();
						for (int i = 0; i < itemCount; i++) {
							if (viewer.getElementAt(i).equals(checkedElement)) {
								viewer.getTable().deselectAll();
								viewer.getTable().select(i);
								setDetailText(resolvedOperation);
								break;
							}
						}
					}
				}
				updateSelection();
			}
		});

		// Filters and sorters before establishing content, so we don't refresh unnecessarily.
		IUComparator comparator = new IUComparator(IUComparator.IU_NAME);
		comparator.useColumnConfig(ProvUI.getIUColumnConfig());
		tableViewer.setComparator(comparator);
		tableViewer.setComparer(new ProvElementComparer());

		contentProvider = new ProvElementContentProvider();
		tableViewer.setContentProvider(contentProvider);
		labelProvider = new IUDetailsLabelProvider(null, ProvUI.getIUColumnConfig(), getShell());
		tableViewer.setLabelProvider(labelProvider);
		tableViewer.setInput(root);
		setInitialCheckState();

		// Select and Deselect All buttons
		createSelectButtons(composite);

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
				updateSelection();
			}
		});

		Button deselectAll = new Button(buttonParent, SWT.PUSH);
		deselectAll.setText(ProvUIMessages.SelectableIUsPage_Deselect_All);
		setButtonLayoutData(deselectAll);
		deselectAll.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				tableViewer.setAllChecked(false);
				updateSelection();
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
			theIUs[i] = ProvUI.getAdapter(elements[i], IInstallableUnit.class);
		}
		return theIUs;
	}

	protected void setInitialCheckState() {
		if (initialSelections == null) {
			return;
		}

		ArrayList<Object> selections = new ArrayList<Object>(initialSelections.length);

		for (int i = 0; i < initialSelections.length; i++) {
			if (initialSelections[i] instanceof AvailableUpdateElement) {
				AvailableUpdateElement element = (AvailableUpdateElement) initialSelections[i];
				if (element.isLockedForUpdate()) {
					continue;
				}
			}
			selections.add(initialSelections[i]);
		}
		tableViewer.setCheckedElements(selections.toArray(new Object[selections.size()]));
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
		java.util.List<IInstallableUnit> units = ElementUtils.elementsToIUs(getSelectedElements());
		if (units.size() == 0)
			return null;
		return units.get(0);
	}

	protected IUDetailsGroup getDetailsGroup() {
		return iuDetailsGroup;
	}

	protected boolean isCreated() {
		return tableViewer != null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.dialogs.ResolutionStatusPage#updateCaches(org.eclipse.equinox.internal.p2.ui.model.IUElementListRoot, org.eclipse.equinox.p2.operations.ProfileChangeOperation)
	 */
	protected void updateCaches(IUElementListRoot newRoot, ProfileChangeOperation op) {
		resolvedOperation = op;
		if (newRoot != null && root != newRoot) {
			root = newRoot;
			if (tableViewer != null)
				tableViewer.setInput(newRoot);
		}

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

	void updateSelection() {
		setPageComplete(tableViewer.getCheckedElements().length > 0);
		getProvisioningWizard().operationSelectionsChanged(this);
	}
}
