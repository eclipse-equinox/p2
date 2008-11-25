/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.dialogs;

import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.ElementUtils;
import org.eclipse.equinox.internal.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.p2.ui.viewers.IUDetailsLabelProvider;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.IUPropertyUtils;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

/**
 * A wizard page that presents a check box list of IUs and allows the user
 * to select and deselect them.  Typically the first page in a provisioning
 * operation wizard.
 * 
 * @since 3.5
 *
 */
public class SelectableIUsPage extends WizardPage implements ISelectableIUsPage {

	IUElementListRoot root;
	Object[] initialSelections;
	CheckboxTableViewer tableViewer;
	Text detailsArea;
	ProvElementContentProvider contentProvider;
	protected Display display;
	protected Policy policy;
	String profileId;

	public SelectableIUsPage(Policy policy, String id, IUElementListRoot root, Object[] initialSelections, String profileId) {
		super(id);
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

		tableViewer = createTableViewer(composite);
		data = new GridData(GridData.FILL_BOTH);
		data.heightHint = convertHeightInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_HEIGHT);
		data.widthHint = convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_WIDTH);
		Table table = tableViewer.getTable();
		table.setLayoutData(data);
		table.setHeaderVisible(true);
		IUColumnConfig[] columns = ProvUI.getIUColumnConfig();
		for (int i = 0; i < columns.length; i++) {
			TableColumn tc = new TableColumn(table, SWT.LEFT, i);
			tc.setResizable(true);
			tc.setText(columns[i].columnTitle);
			if (columns[i].columnField == IUColumnConfig.COLUMN_SIZE) {
				tc.setAlignment(SWT.RIGHT);
				tc.setWidth(convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_SMALL_COLUMN_WIDTH));
			} else
				tc.setWidth(convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_COLUMN_WIDTH));
		}

		tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateDetails();
			}
		});

		tableViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				setPageComplete(tableViewer.getCheckedElements().length > 0);
			}

		});

		// Filters and sorters before establishing content, so we don't refresh unnecessarily.
		tableViewer.setComparator(new IUComparator(IUComparator.IU_NAME));
		tableViewer.setComparer(new ProvElementComparer());

		contentProvider = new ProvElementContentProvider();
		tableViewer.setContentProvider(contentProvider);
		tableViewer.setInput(root);
		tableViewer.setLabelProvider(new IUDetailsLabelProvider(null, ProvUI.getIUColumnConfig(), getShell()));
		setInitialCheckState();

		// The text area shows a description of the selected IU, or error detail if applicable.
		Group group = new Group(sashForm, SWT.NONE);
		group.setText(ProvUIMessages.ProfileModificationWizardPage_DetailsLabel);
		group.setLayout(new GridLayout());

		createDetailsArea(group);

		setControl(sashForm);
		sashForm.setWeights(new int[] {80, 20});
		Dialog.applyDialogFont(sashForm);
	}

	protected CheckboxTableViewer createTableViewer(Composite parent) {
		// The viewer allows selection of IU's for browsing the details,
		// and checking to include in the provisioning operation.
		CheckboxTableViewer v = CheckboxTableViewer.newCheckList(parent, SWT.BORDER | SWT.FULL_SELECTION);
		return v;
	}

	protected void createDetailsArea(Composite parent) {
		detailsArea = new Text(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY | SWT.WRAP);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.heightHint = convertHeightInCharsToPixels(ILayoutConstants.DEFAULT_DESCRIPTION_HEIGHT);
		data.widthHint = convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_WIDTH);
		detailsArea.setLayoutData(data);
	}

	protected void updateDetails() {
		detailsArea.setText(getDetailText());
	}

	String getDetailText() {
		IInstallableUnit[] ius = ElementUtils.elementsToIUs(getSelectedIUElements());
		String description = null;
		if (ius.length > 0) {
			description = IUPropertyUtils.getIUProperty(ius[0], IInstallableUnit.PROP_DESCRIPTION);
		}
		if (description == null)
			description = ""; //$NON-NLS-1$
		return description;
	}

	public Object[] getCheckedIUElements() {
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
}
