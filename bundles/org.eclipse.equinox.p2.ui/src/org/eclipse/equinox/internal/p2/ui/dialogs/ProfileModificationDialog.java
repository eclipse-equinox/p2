/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.AvailableIUElement;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.p2.ui.model.IUElement;
import org.eclipse.equinox.p2.ui.operations.ProfileModificationOperation;
import org.eclipse.equinox.p2.ui.viewers.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public abstract class ProfileModificationDialog extends TrayDialog {
	private static final int DEFAULT_HEIGHT = 20;
	private static final int DEFAULT_WIDTH = 120;
	private static final int DEFAULT_COLUMN_WIDTH = 50;
	private static final int DEFAULT_SMALL_COLUMN_WIDTH = 20;
	private String title;
	private String message;
	private IInstallableUnit[] ius;
	private Profile profile;
	CheckboxTableViewer listViewer;
	StaticContentProvider contentProvider;

	protected ProfileModificationDialog(Shell parentShell, IInstallableUnit[] ius, Profile profile, String title, String message) {
		super(parentShell);
		this.title = title;
		this.message = message;
		this.ius = ius;
		this.profile = profile;
	}

	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		initializeDialogUnits(composite);

		// Create message area;
		Label label = new Label(composite, SWT.NONE);
		if (message != null) {
			label.setText(message);
		}
		listViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.FULL_SELECTION);
		GridData data = new GridData(GridData.FILL_BOTH);
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
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				makeElements(getIUs(), list, monitor);
			}
		};
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);
		} catch (InterruptedException e) {
			// don't report thread interruption
		} catch (InvocationTargetException e) {
			ProvUI.handleException(e.getCause(), null);
		}
		contentProvider = new StaticContentProvider(list.toArray());
		listViewer.setContentProvider(contentProvider);
		listViewer.setInput(new Object());
		listViewer.setLabelProvider(new IUDetailsLabelProvider(getColumnConfig()));
		listViewer.setAllChecked(true);

		addSelectionButtons(composite);
		Dialog.applyDialogFont(composite);
		return composite;
	}

	protected void makeElements(IInstallableUnit[] iusToShow, List list, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor);
		sub.setWorkRemaining(iusToShow.length);
		for (int i = 0; i < iusToShow.length; i++) {
			list.add(new AvailableIUElement(iusToShow[i], getSize(iusToShow[i], sub.newChild(1))));
		}
		monitor.done();
	}

	/*
	 * (non-Javadoc) Method declared in Window.
	 */
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		if (title != null) {
			shell.setText(title);
		}
	}

	/*
	 * (non-Javadoc) Method declared on Dialog.
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, getOkButtonString(), true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Add the selection and deselection buttons to the dialog.
	 * @param composite org.eclipse.swt.widgets.Composite
	 */
	private void addSelectionButtons(Composite composite) {
		Composite buttonComposite = new Composite(composite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		buttonComposite.setLayout(layout);
		buttonComposite.setLayoutData(new GridData(SWT.END, SWT.TOP, true, false));

		Button selectButton = createButton(buttonComposite, IDialogConstants.SELECT_ALL_ID, ProvUIMessages.UpdateAndInstallSelectionDialog_SelectAllLabel, false);

		SelectionListener listener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				listViewer.setAllChecked(true);
			}
		};
		selectButton.addSelectionListener(listener);

		Button deselectButton = createButton(buttonComposite, IDialogConstants.DESELECT_ALL_ID, ProvUIMessages.UpdateAndInstallSelectionDialog_DeselectAllLabel, false);

		listener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				listViewer.setAllChecked(false);
			}
		};
		deselectButton.addSelectionListener(listener);
	}

	protected void okPressed() {

		final ProfileModificationOperation[] op = new ProfileModificationOperation[1];
		try {
			final Object[] selections = getSelectedElements();
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					op[0] = createProfileModificationOperation(selections, monitor);
				}
			});
			ProvisioningOperationRunner.schedule(op[0], getShell());
		} catch (InterruptedException e) {
			// don't report thread interruption
		} catch (InvocationTargetException e) {
			ProvUI.handleException(e.getCause(), null);
		}
		super.okPressed();
	}

	protected Object[] getSelectedElements() {
		return listViewer.getCheckedElements();
	}

	protected IInstallableUnit[] elementsToIUs(Object[] elements) {
		IInstallableUnit[] theIUs = new IInstallableUnit[elements.length];
		for (int i = 0; i < elements.length; i++) {
			theIUs[i] = (IInstallableUnit) ProvUI.getAdapter(elements[i], IInstallableUnit.class);
		}
		return theIUs;
	}

	protected Profile getProfile() {
		return profile;
	}

	protected IInstallableUnit[] getIUs() {
		return ius;
	}

	protected abstract ProfileModificationOperation createProfileModificationOperation(Object[] selectedElements, IProgressMonitor monitor);

	protected abstract String getOkButtonString();

	protected IUColumnConfig[] getColumnConfig() {
		return ProvUI.getIUColumnConfig();
	}

	protected long getSize(IInstallableUnit iu, IProgressMonitor monitor) {
		return IUElement.SIZE_UNKNOWN;
	}
}