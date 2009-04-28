/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.ui.dialogs;

import java.util.*;
import java.util.List;
import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.ILayoutConstants;
import org.eclipse.equinox.internal.p2.ui.viewers.IUDetailsLabelProvider;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.internal.provisional.p2.engine.Operand;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.ILicense;
import org.eclipse.equinox.internal.provisional.p2.ui.IUPropertyUtils;
import org.eclipse.equinox.internal.provisional.p2.ui.model.IUElementListRoot;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.IUColumnConfig;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.ProvElementContentProvider;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * AcceptLicensesWizardPage shows a list of the IU's that have
 * licenses that have not been approved by the user.
 * 
 * @since 3.4
 */
public class AcceptLicensesWizardPage extends WizardPage {
	private static final String DIALOG_SETTINGS_SECTION = "LicensessPage"; //$NON-NLS-1$
	private static final String LIST_WEIGHT = "ListSashWeight"; //$NON-NLS-1$
	private static final String LICENSE_WEIGHT = "LicenseSashWeight"; //$NON-NLS-1$
	private static final String NAME_COLUMN_WIDTH = "NameColumnWidth"; //$NON-NLS-1$
	private static final String VERSION_COLUMN_WIDTH = "VersionColumnWidth"; //$NON-NLS-1$

	TableViewer iuViewer;
	Text licenseTextBox;
	Button acceptButton;
	Button declineButton;
	SashForm sashForm;
	private IInstallableUnit[] originalIUs;
	private IInstallableUnit[] iusWithUnacceptedLicenses;
	private Policy policy;
	IUColumnConfig nameColumn;
	IUColumnConfig versionColumn;

	public AcceptLicensesWizardPage(Policy policy, IInstallableUnit[] ius, ProvisioningPlan plan) {
		super("AcceptLicenses"); //$NON-NLS-1$
		setTitle(ProvUIMessages.AcceptLicensesWizardPage_Title);
		this.policy = policy;
		update(ius, plan);
	}

	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		if (iusWithUnacceptedLicenses.length == 0) {
			Label label = new Label(parent, SWT.NONE);
			setControl(label);
		} else if (iusWithUnacceptedLicenses.length == 1) {
			createLicenseSection(parent, true);
		} else {
			sashForm = new SashForm(parent, SWT.HORIZONTAL);
			sashForm.setLayout(new GridLayout());
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			sashForm.setLayoutData(gd);

			createIUSection(sashForm);
			createLicenseSection(sashForm, false);
			sashForm.setWeights(getSashWeights());
			setControl(sashForm);

			Object element = iuViewer.getElementAt(0);
			if (element != null)
				iuViewer.setSelection(new StructuredSelection(element));
		}
		Dialog.applyDialogFont(getControl());
	}

	private void createIUSection(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(gd);

		Label label = new Label(composite, SWT.NONE);
		label.setText(ProvUIMessages.AcceptLicensesWizardPage_ItemsLabel);
		iuViewer = new TableViewer(composite, SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		setTableColumns(iuViewer.getTable());
		iuViewer.setContentProvider(new ProvElementContentProvider());
		iuViewer.setLabelProvider(new IUDetailsLabelProvider());
		iuViewer.setComparator(new ViewerComparator());
		iuViewer.setInput(new IUElementListRoot(iusWithUnacceptedLicenses));

		iuViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handleSelectionChanged((IStructuredSelection) event.getSelection());
			}

		});
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_COLUMN_WIDTH + ILayoutConstants.DEFAULT_SMALL_COLUMN_WIDTH);
		gd.heightHint = convertHeightInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_HEIGHT);
		iuViewer.getControl().setLayoutData(gd);
	}

	private void createLicenseAcceptSection(Composite parent, boolean multiple) {
		// Buttons for accepting licenses
		Composite buttonContainer = new Composite(parent, SWT.NULL);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		buttonContainer.setLayout(new GridLayout());
		buttonContainer.setLayoutData(gd);

		acceptButton = new Button(buttonContainer, SWT.RADIO);
		if (multiple)
			acceptButton.setText(ProvUIMessages.AcceptLicensesWizardPage_AcceptMultiple);
		else
			acceptButton.setText(ProvUIMessages.AcceptLicensesWizardPage_AcceptSingle);

		acceptButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setPageComplete(acceptButton.getSelection());
			}
		});
		declineButton = new Button(buttonContainer, SWT.RADIO);
		if (multiple)
			declineButton.setText(ProvUIMessages.AcceptLicensesWizardPage_RejectMultiple);
		else
			declineButton.setText(ProvUIMessages.AcceptLicensesWizardPage_RejectSingle);
		declineButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setPageComplete(acceptButton.getSelection());
			}
		});

		acceptButton.setSelection(false);
		declineButton.setSelection(true);
	}

	private void createLicenseSection(Composite parent, boolean singleLicense) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(gd);

		Label label = new Label(composite, SWT.NONE);
		label.setText(ProvUIMessages.AcceptLicensesWizardPage_LicenseTextLabel);
		licenseTextBox = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.WRAP | SWT.READ_ONLY);
		licenseTextBox.setBackground(licenseTextBox.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		initializeDialogUnits(licenseTextBox);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = convertHeightInCharsToPixels(ILayoutConstants.DEFAULT_TABLE_HEIGHT);
		licenseTextBox.setLayoutData(gd);

		createLicenseAcceptSection(composite, !singleLicense);

		if (singleLicense) {
			licenseTextBox.setText(getLicenseBody(iusWithUnacceptedLicenses[0]));
			setControl(composite);
		}
	}

	void handleSelectionChanged(IStructuredSelection selection) {
		if (!selection.isEmpty()) {
			Object selected = selection.getFirstElement();
			if (selected instanceof IInstallableUnit)
				licenseTextBox.setText(getLicenseBody((IInstallableUnit) selected));
		}
	}

	private void setTableColumns(Table table) {
		table.setHeaderVisible(true);
		nameColumn = new IUColumnConfig(ProvUIMessages.ProvUI_NameColumnTitle, IUColumnConfig.COLUMN_NAME, convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_COLUMN_WIDTH));
		versionColumn = new IUColumnConfig(ProvUIMessages.ProvUI_VersionColumnTitle, IUColumnConfig.COLUMN_VERSION, convertWidthInCharsToPixels(ILayoutConstants.DEFAULT_COLUMN_WIDTH));
		initializeDialogUnits(table);
		getColumnWidthsFromSettings();
		TableColumn tc = new TableColumn(table, SWT.NONE, 0);
		tc.setResizable(true);
		tc.setText(nameColumn.columnTitle);
		tc.setWidth(nameColumn.getWidth());
		tc = new TableColumn(table, SWT.NONE, 1);
		tc.setResizable(true);
		tc.setText(versionColumn.columnTitle);
		tc.setWidth(versionColumn.getWidth());
	}

	public boolean performFinish() {
		rememberAcceptedLicenses();
		return true;
	}

	public boolean hasLicensesToAccept() {
		return iusWithUnacceptedLicenses.length > 0;
	}

	public void update(IInstallableUnit[] theIUs, ProvisioningPlan currentPlan) {
		this.originalIUs = theIUs;
		if (theIUs == null)
			this.iusWithUnacceptedLicenses = new IInstallableUnit[0];
		else
			this.iusWithUnacceptedLicenses = iusWithUnacceptedLicenses(theIUs, currentPlan);
		setDescription();
		setPageComplete(iusWithUnacceptedLicenses.length == 0);
		if (getControl() != null) {
			Composite parent = getControl().getParent();
			getControl().dispose();
			createControl(parent);
			parent.layout(true);
		}
	}

	private String getLicenseBody(IInstallableUnit iu) {
		ILicense license = IUPropertyUtils.getLicense(iu);
		if (license != null && license.getBody() != null)
			return license.getBody();
		// shouldn't happen because we already reduced the list to those
		// that have licenses and bodies are required.
		return ""; //$NON-NLS-1$
	}

	private IInstallableUnit[] iusWithUnacceptedLicenses(IInstallableUnit[] selectedIUs, ProvisioningPlan currentPlan) {
		IInstallableUnit[] iusToCheck;
		if (currentPlan == null)
			iusToCheck = selectedIUs;
		else {
			List allIUs = new ArrayList();
			Operand[] operands = currentPlan.getOperands();
			for (int i = 0; i < operands.length; i++)
				if (operands[i] instanceof InstallableUnitOperand) {
					IInstallableUnit addedIU = ((InstallableUnitOperand) operands[i]).second();
					if (addedIU != null)
						allIUs.add(addedIU);
				}
			iusToCheck = (IInstallableUnit[]) allIUs.toArray(new IInstallableUnit[allIUs.size()]);
		}

		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=218532
		// Current metadata generation can result with a feature group IU and the feature jar IU
		// having the same name and license.  We will weed out duplicates if the license and name are both
		// the same.  
		// Note this algorithm is not generalized...we only save the first iu of any name found and this
		// algorithm would allow duplicates if subsequent licenses found for an iu name did not match the
		// first license yet still duplicated each other. Since this is not likely to happen we keep it
		// simple.  The UI for licenses will soon be reworked.
		HashMap licensesByIUName = new HashMap();//map of String(iu name)->License
		List unaccepted = new ArrayList();
		// We can't be sure that the viewer is created or the right label provider has been installed, so make another one.
		IUDetailsLabelProvider labelProvider = new IUDetailsLabelProvider();
		for (int i = 0; i < iusToCheck.length; i++) {
			IInstallableUnit iu = iusToCheck[i];
			String name = labelProvider.getText(iu);
			ILicense license = IUPropertyUtils.getLicense(iu);
			// It has a license, is it already accepted?
			if (license != null) {
				if (!policy.getLicenseManager().isAccepted(iu)) {
					// Have we already found a license with this IU name?
					ILicense potentialDuplicate = (ILicense) licensesByIUName.get(name);
					// If we have no duplicate or the duplicate license doesn't match, add it
					if (potentialDuplicate == null || !potentialDuplicate.equals(license))
						unaccepted.add(iu);
					// We didn't have a duplicate, need to record this one
					if (potentialDuplicate == null)
						licensesByIUName.put(name, license);
				}
			}
		}
		// Wasn't that fun?
		return (IInstallableUnit[]) unaccepted.toArray(new IInstallableUnit[unaccepted.size()]);
	}

	private void rememberAcceptedLicenses() {
		for (int i = 0; i < iusWithUnacceptedLicenses.length; i++) {
			policy.getLicenseManager().accept(iusWithUnacceptedLicenses[i]);
		}
	}

	private void setDescription() {
		// No licenses but the page is open.  Shouldn't happen, but just in case...
		if (iusWithUnacceptedLicenses.length == 0)
			setDescription(ProvUIMessages.AcceptLicensesWizardPage_NoLicensesDescription);
		// We have licenses.  Use a generic message if we think we aren't showing
		// licenses from required IU's.  This check is not entirely accurate, for example
		// one root IU could have no license and the next one has two different
		// IU's with different licenses.  But this cheaply catches the common cases.
		else if (iusWithUnacceptedLicenses.length <= originalIUs.length)
			setDescription(ProvUIMessages.AcceptLicensesWizardPage_ReviewLicensesDescription);
		else {
			// Without a doubt we know we are showing extra licenses.
			setDescription(ProvUIMessages.AcceptLicensesWizardPage_ReviewExtraLicensesDescription);
		}
	}

	private String getDialogSettingsName() {
		return getWizard().getClass().getName() + "." + DIALOG_SETTINGS_SECTION; //$NON-NLS-1$
	}

	public void saveBoundsRelatedSettings() {
		if (iuViewer == null || iuViewer.getTable().isDisposed())
			return;
		IDialogSettings settings = ProvUIActivator.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(getDialogSettingsName());
		if (section == null) {
			section = settings.addNewSection(getDialogSettingsName());
		}
		section.put(NAME_COLUMN_WIDTH, iuViewer.getTable().getColumn(0).getWidth());
		section.put(VERSION_COLUMN_WIDTH, iuViewer.getTable().getColumn(1).getWidth());

		if (sashForm == null || sashForm.isDisposed())
			return;
		int[] weights = sashForm.getWeights();
		section.put(LIST_WEIGHT, weights[0]);
		section.put(LICENSE_WEIGHT, weights[1]);
	}

	private void getColumnWidthsFromSettings() {
		IDialogSettings settings = ProvUIActivator.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(getDialogSettingsName());
		if (section != null) {
			try {
				if (section.get(NAME_COLUMN_WIDTH) != null)
					nameColumn.columnWidth = section.getInt(NAME_COLUMN_WIDTH);
				if (section.get(VERSION_COLUMN_WIDTH) != null)
					versionColumn.columnWidth = section.getInt(VERSION_COLUMN_WIDTH);
			} catch (NumberFormatException e) {
				// Ignore if there actually was a value that didn't parse.  
			}
		}
	}

	private int[] getSashWeights() {
		IDialogSettings settings = ProvUIActivator.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(getDialogSettingsName());
		if (section != null) {
			try {
				int[] weights = new int[2];
				if (section.get(LIST_WEIGHT) != null) {
					weights[0] = section.getInt(LIST_WEIGHT);
					if (section.get(LICENSE_WEIGHT) != null) {
						weights[1] = section.getInt(LICENSE_WEIGHT);
						return weights;
					}
				}
			} catch (NumberFormatException e) {
				// Ignore if there actually was a value that didn't parse.  
			}
		}
		return new int[] {55, 45};
	}
}
