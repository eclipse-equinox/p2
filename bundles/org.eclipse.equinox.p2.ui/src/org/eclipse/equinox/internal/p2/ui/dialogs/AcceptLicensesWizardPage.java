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

import java.util.*;
import java.util.List;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.viewers.IUDetailsLabelProvider;
import org.eclipse.equinox.internal.p2.ui.viewers.StaticContentProvider;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.internal.provisional.p2.engine.Operand;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.License;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.LicenseManager;
import org.eclipse.equinox.internal.provisional.p2.ui.query.IUPropertyUtils;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.IUColumnConfig;
import org.eclipse.jface.dialogs.Dialog;
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
 * @since 3.4
 */
public class AcceptLicensesWizardPage extends WizardPage {

	TableViewer iuViewer;
	Text licenseTextBox;
	Button acceptButton;
	Button declineButton;
	private IInstallableUnit[] originalIUs;
	private IInstallableUnit[] iusWithUnacceptedLicenses;
	private LicenseManager licenseManager;
	private static final int DEFAULT_COLUMN_WIDTH = 40;

	public AcceptLicensesWizardPage(IInstallableUnit[] ius, LicenseManager licenseManager, ProvisioningPlan plan) {
		super("AcceptLicenses"); //$NON-NLS-1$
		setTitle(ProvUIMessages.AcceptLicensesWizardPage_Title);
		this.licenseManager = licenseManager;
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
			SashForm composite = new SashForm(parent, SWT.HORIZONTAL);
			composite.setLayout(new GridLayout());
			composite.setLayoutData(new GridData(GridData.FILL_BOTH));

			createIUSection(composite);
			createLicenseSection(composite, false);
			setControl(composite);

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
		iuViewer.setContentProvider(new StaticContentProvider(iusWithUnacceptedLicenses));
		iuViewer.setLabelProvider(new IUDetailsLabelProvider());
		iuViewer.setComparator(new ViewerComparator());
		iuViewer.setInput(iusWithUnacceptedLicenses);

		iuViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handleSelectionChanged((IStructuredSelection) event.getSelection());
			}

		});
		gd = new GridData(GridData.FILL_BOTH);
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
		licenseTextBox = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.WRAP | SWT.READ_ONLY);
		licenseTextBox.setBackground(licenseTextBox.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		initializeDialogUnits(licenseTextBox);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = convertWidthInCharsToPixels(80);
		gd.heightHint = convertHeightInCharsToPixels(20);

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
		IUColumnConfig[] columns = ProvUI.getIUColumnConfig();
		initializeDialogUnits(table);
		for (int i = 0; i < columns.length; i++) {
			TableColumn tc = new TableColumn(table, SWT.NONE, i);
			tc.setResizable(true);
			tc.setText(columns[i].columnTitle);
			tc.setWidth(convertWidthInCharsToPixels(DEFAULT_COLUMN_WIDTH));
		}
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
		this.iusWithUnacceptedLicenses = iusWithUnacceptedLicenses(theIUs, currentPlan);
		setDescription();
		setPageComplete(iusWithUnacceptedLicenses.length == 0);
		if (getControl() != null) {
			Composite parent = getControl().getParent();
			getControl().dispose();
			createControl(parent);
			parent.layout(true, true);
		}
	}

	private String getLicenseBody(IInstallableUnit iu) {
		License license = IUPropertyUtils.getLicense(iu);
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
			License license = IUPropertyUtils.getLicense(iu);
			// It has a license, is it already accepted?
			if (license != null) {
				if (licenseManager == null || !licenseManager.isAccepted(iu)) {
					// Have we already found a license with this IU name?
					License potentialDuplicate = (License) licensesByIUName.get(name);
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
			if (licenseManager != null)
				licenseManager.accept(iusWithUnacceptedLicenses[i]);
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
}
