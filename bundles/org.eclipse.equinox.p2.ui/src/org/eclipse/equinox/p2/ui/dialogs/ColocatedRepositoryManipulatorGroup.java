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
package org.eclipse.equinox.p2.ui.dialogs;

import java.util.List;
import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.ui.ProvUIActivator;
import org.eclipse.equinox.p2.ui.ProvisioningOperationRunner;
import org.eclipse.equinox.p2.ui.model.AllMetadataRepositories;
import org.eclipse.equinox.p2.ui.model.MetadataRepositoryContentProvider;
import org.eclipse.equinox.p2.ui.operations.RemoveColocatedRepositoryOperation;
import org.eclipse.equinox.p2.ui.viewers.ColocatedRepositoryLabelProvider;
import org.eclipse.equinox.p2.ui.viewers.StructuredViewerProvisioningListener;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PropertyDialogAction;

/**
 * Dialog group that shows installed IU's and allows user to update or search
 * for new ones.
 * 
 * @since 3.4
 */
public class ColocatedRepositoryManipulatorGroup {

	private static final String BUTTONACTION = "buttonAction"; //$NON-NLS-1$
	TableViewer repositoryViewer;
	private FontMetrics fm;
	Display display;
	StructuredViewerProvisioningListener listener;

	/**
	 * Create an instance of this group.
	 * 
	 */
	public ColocatedRepositoryManipulatorGroup(Composite parent, ViewerFilter[] filters, int widthInDUs, int heightInDUs, FontMetrics fm) {

		Assert.isNotNull(fm);
		this.fm = fm;
		this.display = parent.getDisplay();

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = convertHorizontalDLUsToPixels(2);
		layout.marginHeight = convertVerticalDLUsToPixels(2);

		composite.setLayout(layout);

		// Table of available repositories
		repositoryViewer = new TableViewer(composite, SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		setTableColumns(repositoryViewer.getTable());
		repositoryViewer.setContentProvider(new MetadataRepositoryContentProvider());
		repositoryViewer.setInput(new AllMetadataRepositories());
		repositoryViewer.setLabelProvider(new ColocatedRepositoryLabelProvider());
		if (filters != null) {
			repositoryViewer.setFilters(filters);
		}
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		data.widthHint = convertHorizontalDLUsToPixels(widthInDUs);
		data.heightHint = convertVerticalDLUsToPixels(heightInDUs);
		repositoryViewer.getControl().setLayoutData(data);

		// Vertical buttons
		Composite buttonBar = (Composite) createVerticalButtonBar(composite);
		data = new GridData(GridData.FILL_VERTICAL);
		buttonBar.setLayoutData(data);
		listener = new StructuredViewerProvisioningListener(repositoryViewer, StructuredViewerProvisioningListener.PROV_EVENT_REPOSITORY);
		ProvUIActivator.getDefault().addProvisioningListener(listener);
		composite.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent event) {
				ProvUIActivator.getDefault().removeProvisioningListener(listener);
			}
		});
	}

	public Control getControl() {
		return repositoryViewer.getControl().getParent();
	}

	private Control createVerticalButtonBar(Composite parent) {
		// Create composite.
		Composite composite = new Composite(parent, SWT.NULL);

		// create a layout with spacing and margins appropriate for the font
		// size.
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginWidth = 5;
		layout.marginHeight = 0;
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		composite.setLayout(layout);

		// Add the buttons to the button bar.
		Button button = createVerticalButton(composite, ProvUIMessages.UpdateAndInstallGroup_Properties, false);
		button.setData(BUTTONACTION, new PropertyDialogAction(new SameShellProvider(parent.getShell()), repositoryViewer));
		button = createVerticalButton(composite, ProvUIMessages.ColocatedRepositoryManipulatorGroup_Add, false);
		button.setData(BUTTONACTION, new Action() {
			public void runWithEvent(Event event) {
				new AddColocatedRepositoryDialog(getControl().getShell(), (IMetadataRepository[]) ((IStructuredContentProvider) repositoryViewer.getContentProvider()).getElements(null)).open();
			}
		});
		button = createVerticalButton(composite, ProvUIMessages.ColocatedRepositoryManipulatorGroup_Remove, false);
		button.setData(BUTTONACTION, new Action() {
			public void runWithEvent(Event event) {
				List selection = ((IStructuredSelection) repositoryViewer.getSelection()).toList();
				IMetadataRepository[] repos = new IMetadataRepository[selection.size()];
				for (int i = 0; i < repos.length; i++) {
					repos[i] = (IMetadataRepository) selection.get(i);
				}
				RemoveColocatedRepositoryOperation op = new RemoveColocatedRepositoryOperation(ProvUIMessages.ColocatedRepositoryManipulatorGroup_Remove, repos);
				ProvisioningOperationRunner.execute(op, getControl().getShell());
			}
		});
		return composite;
	}

	private Button createVerticalButton(Composite parent, String label, boolean defaultButton) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(label);

		GridData data = setButtonLayoutData(button);
		data.horizontalAlignment = GridData.FILL;

		button.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				verticalButtonPressed(event);
			}
		});
		button.setToolTipText(label);
		if (defaultButton) {
			Shell shell = parent.getShell();
			if (shell != null) {
				shell.setDefaultButton(button);
			}
		}
		return button;
	}

	void verticalButtonPressed(Event event) {
		Object data = event.widget.getData(BUTTONACTION);
		if (data == null || !(data instanceof IAction)) {
			return;
		}
		IAction action = (IAction) data;
		action.runWithEvent(event);
	}

	private GridData setButtonLayoutData(Button button) {
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		Point minSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		data.widthHint = Math.max(widthHint, minSize.x);
		button.setLayoutData(data);
		return data;
	}

	private int convertHorizontalDLUsToPixels(int dlus) {
		// shouldn't happen
		if (fm == null) {
			return 0;
		}
		return Dialog.convertHorizontalDLUsToPixels(fm, dlus);
	}

	private int convertVerticalDLUsToPixels(int dlus) {
		// shouldn't happen
		if (fm == null) {
			return 0;
		}
		return Dialog.convertVerticalDLUsToPixels(fm, dlus);
	}

	private void setTableColumns(Table table) {
		table.setHeaderVisible(true);
		String[] columnHeaders = {ProvUIMessages.ColocatedRepositoryManipulatorGroup_NameColumnHeader, ProvUIMessages.ColocatedRepositoryManipulatorGroup_LocationColumnHeader};
		for (int i = 0; i < columnHeaders.length; i++) {
			TableColumn tc = new TableColumn(table, SWT.NONE, i);
			tc.setResizable(true);
			tc.setText(columnHeaders[i]);
			tc.setWidth(convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH));
		}
	}

}
