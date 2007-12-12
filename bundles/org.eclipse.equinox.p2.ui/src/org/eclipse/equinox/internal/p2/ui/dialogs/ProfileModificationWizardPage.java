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
import org.eclipse.equinox.internal.p2.ui.model.AvailableIUElement;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.p2.ui.model.IUElement;
import org.eclipse.equinox.p2.ui.operations.ProfileModificationOperation;
import org.eclipse.equinox.p2.ui.viewers.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public abstract class ProfileModificationWizardPage extends WizardPage {
	private static final int DEFAULT_HEIGHT = 20;
	private static final int DEFAULT_WIDTH = 120;
	private static final int DEFAULT_COLUMN_WIDTH = 50;
	private static final int DEFAULT_SMALL_COLUMN_WIDTH = 20;
	private IInstallableUnit[] ius;
	private Profile profile;
	CheckboxTableViewer listViewer;
	StaticContentProvider contentProvider;

	protected ProfileModificationWizardPage(String id, IInstallableUnit[] ius, Profile profile) {
		super(id);
		this.ius = ius;
		this.profile = profile;
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
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				makeElements(getIUs(), list, monitor);
			}
		};
		try {
			// We are not open yet so we can't use the local progress control
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);
		} catch (InterruptedException e) {
			// don't report thread interruption
		} catch (InvocationTargetException e) {
			ProvUI.handleException(e.getCause(), null);
		}

		listViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				selectedIUsChanged();
			}
		});
		contentProvider = new StaticContentProvider(list.toArray());
		listViewer.setContentProvider(contentProvider);
		listViewer.setInput(new Object());
		listViewer.setLabelProvider(new IUDetailsLabelProvider(getColumnConfig()));
		setInitialSelections();
		selectedIUsChanged();
		setControl(composite);
		Dialog.applyDialogFont(composite);
	}

	protected void makeElements(IInstallableUnit[] iusToShow, List list, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor);
		sub.setWorkRemaining(iusToShow.length);
		for (int i = 0; i < iusToShow.length; i++) {
			list.add(new AvailableIUElement(iusToShow[i], getSize(iusToShow[i], sub.newChild(1))));
		}
		monitor.done();
	}

	public boolean performFinish() {

		final ProfileModificationOperation[] op = new ProfileModificationOperation[1];
		try {
			final Object[] selections = getSelectedElements();
			getContainer().run(true, true, new IRunnableWithProgress() {
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
		return true;
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

	public IInstallableUnit[] getSelectedIUs() {
		return elementsToIUs(getSelectedElements());
	}

	protected Profile getProfile() {
		return profile;
	}

	protected IInstallableUnit[] getIUs() {
		return ius;
	}

	protected abstract ProfileModificationOperation createProfileModificationOperation(Object[] selectedElements, IProgressMonitor monitor);

	protected IUColumnConfig[] getColumnConfig() {
		return ProvUI.getIUColumnConfig();
	}

	protected long getSize(IInstallableUnit iu, IProgressMonitor monitor) {
		return IUElement.SIZE_UNKNOWN;
	}

	protected void selectedIUsChanged() {
		setPageComplete(getSelectedIUs().length > 0);
	}

	protected void setInitialSelections() {
		// The default is to select everything because 
		// in most cases, the user has selected exactly
		// what they want before this page opens.
		listViewer.setAllChecked(true);
	}
}