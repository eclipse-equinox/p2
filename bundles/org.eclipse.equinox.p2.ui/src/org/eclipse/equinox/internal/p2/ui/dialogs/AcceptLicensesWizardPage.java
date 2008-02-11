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
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.LicenseManager;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.*;
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
	Text license;
	Button acceptButton;
	Button declineButton;
	private IInstallableUnit[] ius;
	private LicenseManager licenseManager;
	private static final int DEFAULT_COLUMN_WIDTH = 100;

	public AcceptLicensesWizardPage(IInstallableUnit[] ius, LicenseManager licenseManager) {
		super("AcceptLicenses"); //$NON-NLS-1$
		setTitle(ProvUIMessages.AcceptLicensesWizardPage_Title);
		this.licenseManager = licenseManager;
		update(ius);
	}

	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		if (ius.length == 0) {
			Label label = new Label(parent, SWT.NONE);
			setControl(label);
		} else if (ius.length == 1) {
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
		iuViewer.setContentProvider(new StaticContentProvider(new Object[0]));
		iuViewer.setInput(ius);
		iuViewer.setLabelProvider(new IUDetailsLabelProvider());
		iuViewer.setComparator(new ViewerComparator(new Comparator() {
			// This comparator sorts in reverse order so that we see the newest configs first
			public int compare(Object o1, Object o2) {
				return ((String) o2).compareTo((String) o1);
			}
		}));
		iuViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handleSelectionChanged((IStructuredSelection) event.getSelection());
			}

		});
		setTableColumns(iuViewer.getTable());
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
		license = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.WRAP | SWT.READ_ONLY);
		license.setBackground(license.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		gd = new GridData(GridData.FILL_BOTH);
		license.setLayoutData(gd);

		createLicenseAcceptSection(composite, !singleLicense);

		if (singleLicense) {
			license.setText(getLicense(ius[0]));
			setControl(composite);
		}
	}

	void handleSelectionChanged(IStructuredSelection selection) {
		if (!selection.isEmpty()) {
			Object selected = selection.getFirstElement();
			if (selected instanceof IInstallableUnit)
				license.setText(getLicense((IInstallableUnit) selected));
		}
	}

	private void setTableColumns(Table table) {
		IUColumnConfig[] columns = ProvUI.getIUColumnConfig();

		for (int i = 0; i < columns.length; i++) {
			TableColumn tc = new TableColumn(table, SWT.NONE, i);
			tc.setResizable(true);
			tc.setText(columns[i].columnTitle);
			tc.setWidth(convertHorizontalDLUsToPixels(DEFAULT_COLUMN_WIDTH));
		}
	}

	public boolean performFinish() {
		rememberAcceptedLicenses();
		return true;
	}

	public boolean hasLicensesToAccept() {
		return ius.length > 0;
	}

	public void update(IInstallableUnit[] theIUs) {
		this.ius = iusWithUnacceptedLicenses(theIUs);
		setDescription();
		setPageComplete(ius.length == 0);
		if (getControl() != null) {
			Composite parent = getControl().getParent();
			getControl().dispose();
			createControl(parent);
			parent.layout(true, true);
		}
	}

	private String getLicense(IInstallableUnit iu) {
		String licenseText = iu.getProperty(IInstallableUnit.PROP_LICENSE);
		if (licenseText != null)
			return licenseText;
		// shouldn't happen because we already reduced the list to those
		// that have licenses
		return ""; //$NON-NLS-1$
	}

	private IInstallableUnit[] iusWithUnacceptedLicenses(IInstallableUnit[] allIUs) {
		List unaccepted = new ArrayList();
		for (int i = 0; i < allIUs.length; i++) {
			IInstallableUnit iu = allIUs[i];
			String licenseText = iu.getProperty(IInstallableUnit.PROP_LICENSE);
			// It has a license, is it already accepted?
			if (licenseText != null && licenseText.length() > 0) {
				if (licenseManager == null || !licenseManager.isAccepted(iu))
					unaccepted.add(iu);
			}
		}
		return (IInstallableUnit[]) unaccepted.toArray(new IInstallableUnit[unaccepted.size()]);
	}

	private void rememberAcceptedLicenses() {
		for (int i = 0; i < ius.length; i++) {
			if (licenseManager != null)
				licenseManager.acceptLicense(ius[i]);
		}
	}

	private void setDescription() {
		if (ius.length == 0)
			setDescription(ProvUIMessages.AcceptLicensesWizardPage_NoLicensesDescription);
		else
			setDescription(ProvUIMessages.AcceptLicensesWizardPage_ReviewLicensesDescription);
	}
}
