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
package org.eclipse.equinox.internal.provisional.p2.ui.dialogs;

import java.util.ArrayList;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.dialogs.RepositoryManipulatorDropTarget;
import org.eclipse.equinox.internal.p2.ui.model.ElementUtils;
import org.eclipse.equinox.internal.p2.ui.model.MetadataRepositoryElement;
import org.eclipse.equinox.internal.provisional.p2.core.repository.RepositoryEvent;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.*;
import org.eclipse.equinox.internal.provisional.p2.ui.model.MetadataRepositories;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IUViewQueryContext;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.*;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

/**
 * Dialog that allows users to update, add, or remove repositories.
 * 
 * @since 3.4
 */
public class RepositoryManipulationDialog extends TrayDialog {
	private final static int WIDTH_IN_DLUS = 480;
	private final static int HEIGHT_IN_DLUS = 240;
	protected static final String BUTTONACTION = "buttonAction"; //$NON-NLS-1$

	StructuredViewerProvisioningListener listener;
	private CheckboxTableViewer repositoryViewer;
	private Policy policy;
	private RepositoryContentProvider contentProvider;
	private boolean changed = false;

	Button propertiesButton, removeButton;

	/**
	 * Create an instance of this Dialog.
	 * 
	 */
	public RepositoryManipulationDialog(Shell shell, Policy policy) {
		super(shell);
		this.policy = policy;
	}

	protected void configureShell(Shell shell) {
		shell.setText(ProvUIMessages.RepositoryManipulationDialog_Title);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IProvHelpContextIds.REPOSITORY_MANIPULATION_DIALOG);

		super.configureShell(shell);
	}

	protected Control createDialogArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		composite.setLayout(layout);

		Label label = new Label(composite, SWT.NONE);
		label.setText(ProvUIMessages.RepositoryManipulationDialog_Description);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, false);
		data.horizontalSpan = 2;
		data.horizontalIndent = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		label.setLayoutData(data);

		// Table of available repositories
		Table table = new Table(composite, SWT.CHECK | SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		repositoryViewer = new CheckboxTableViewer(table);
		setTableColumns(table);
		contentProvider = new RepositoryContentProvider();
		repositoryViewer.setComparer(new ProvElementComparer());
		repositoryViewer.setComparator(new ViewerComparator());
		repositoryViewer.setContentProvider(contentProvider);
		repositoryViewer.setLabelProvider(new ProvElementLabelProvider());

		// Input last
		repositoryViewer.setInput(getInput());
		setCheckState();

		repositoryViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				if (event.getElement() instanceof MetadataRepositoryElement) {
					((MetadataRepositoryElement) event.getElement()).setEnabled(event.getChecked());
					changed = true;
				}
			}
		});

		DropTarget target = new DropTarget(repositoryViewer.getControl(), DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);
		target.setTransfer(new Transfer[] {URLTransfer.getInstance(), FileTransfer.getInstance()});
		target.addDropListener(new RepositoryManipulatorDropTarget(policy.getRepositoryManipulator(), repositoryViewer.getControl()));

		data = new GridData(GridData.FILL_BOTH);
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		data.widthHint = convertHorizontalDLUsToPixels(WIDTH_IN_DLUS);
		data.heightHint = convertVerticalDLUsToPixels(HEIGHT_IN_DLUS);
		repositoryViewer.getControl().setLayoutData(data);

		// Vertical buttons
		Composite verticalButtonBar = (Composite) createVerticalButtonBar(composite);
		data = new GridData(GridData.FILL_VERTICAL);
		data.verticalIndent = convertVerticalDLUsToPixels(IDialogConstants.BUTTON_BAR_HEIGHT);
		verticalButtonBar.setLayoutData(data);
		listener = getViewerProvisioningListener();

		// Must be done after buttons are created so they
		// get selection notifications first and can update their
		// enablement
		repositoryViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				validateButtons();
			}
		});
		ProvUI.addProvisioningListener(listener);
		composite.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent event) {
				ProvUI.removeProvisioningListener(listener);
			}
		});
		Dialog.applyDialogFont(composite);
		validateButtons();
		return composite;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	protected boolean isResizable() {
		return true;
	}

	private Button createVerticalButton(Composite parent, String label, boolean defaultButton) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(label);

		GridData data = setVerticalButtonLayoutData(button);
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

	private GridData setVerticalButtonLayoutData(Button button) {
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		Point minSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		data.widthHint = Math.max(widthHint, minSize.x);
		button.setLayoutData(data);
		return data;
	}

	private void setTableColumns(Table table) {
		table.setHeaderVisible(true);
		String[] columnHeaders = {ProvUIMessages.RepositoryManipulationDialog_NameColumnTitle, ProvUIMessages.RepositoryManipulationDialog_LocationColumnTitle};
		for (int i = 0; i < columnHeaders.length; i++) {
			TableColumn tc = new TableColumn(table, SWT.NONE, i);
			tc.setResizable(true);
			tc.setText(columnHeaders[i]);
			tc.setWidth(convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH));
		}
	}

	private Control createVerticalButtonBar(Composite parent) {
		// Create composite.
		Composite composite = new Composite(parent, SWT.NULL);
		initializeDialogUnits(composite);

		// create a layout with spacing and margins appropriate for the font
		// size.
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginWidth = 5;
		layout.marginHeight = 0;
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		composite.setLayout(layout);

		createVerticalButtons(composite);
		return composite;
	}

	private void createVerticalButtons(Composite parent) {
		propertiesButton = createVerticalButton(parent, ProvUIMessages.RepositoryManipulationDialog_Properties, false);
		propertiesButton.setData(BUTTONACTION, new PropertyDialogAction(new SameShellProvider(parent.getShell()), repositoryViewer));
		// spacer
		new Label(parent, SWT.NONE);

		Button button = createVerticalButton(parent, ProvUIMessages.RepositoryManipulationDialog_Add, false);
		button.setData(BUTTONACTION, new AddColocatedRepositoryAction(repositoryViewer));
		removeButton = createVerticalButton(parent, ProvUIMessages.RepositoryManipulationDialog_Remove, false);
		removeButton.setData(BUTTONACTION, new RemoveColocatedRepositoryAction(repositoryViewer));

		// spacer
		new Label(parent, SWT.NONE);

		button = createVerticalButton(parent, ProvUIMessages.RepositoryManipulationDialog_Import, false);
		button.setData(BUTTONACTION, new Action() {
			public void run() {
				BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
					public void run() {
						UpdateManagerCompatibility.importSites(getShell());
					}
				});
			}
		});
		button = createVerticalButton(parent, ProvUIMessages.RepositoryManipulationDialog_Export, false);
		button.setData(BUTTONACTION, new Action() {
			public void run() {
				BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
					public void run() {
						UpdateManagerCompatibility.exportSites(getShell(), getElements());
					}
				});
			}
		});
	}

	private MetadataRepositories getInput() {
		MetadataRepositories input = new MetadataRepositories(new IUViewQueryContext(IUViewQueryContext.AVAILABLE_VIEW_BY_REPO), policy, null);
		input.setIncludeDisabledRepositories(true);
		return input;
	}

	protected void okPressed() {
		if (changed)
			ElementUtils.updateRepositoryUsingElements(getElements(), getShell());
		super.okPressed();
	}

	private StructuredViewerProvisioningListener getViewerProvisioningListener() {
		return new StructuredViewerProvisioningListener(repositoryViewer, StructuredViewerProvisioningListener.PROV_EVENT_METADATA_REPOSITORY) {
			protected void repositoryDiscovered(RepositoryEvent e) {
				asyncRefresh();
			}

			protected void repositoryChanged(RepositoryEvent e) {
				asyncRefresh();
			}

			protected void refreshAll() {
				repositoryViewer.setInput(getInput());
				setCheckState();
			}
		};
	}

	private void setCheckState() {
		MetadataRepositoryElement[] elements = getElements();
		for (int i = 0; i < elements.length; i++)
			repositoryViewer.setChecked(elements[i], elements[i].isEnabled());
	}

	private MetadataRepositoryElement[] getElements() {
		TableItem[] items = repositoryViewer.getTable().getItems();
		ArrayList list = new ArrayList(items.length);
		for (int i = 0; i < items.length; i++) {
			if (items[i].getData() instanceof MetadataRepositoryElement)
				list.add(items[i].getData());
		}
		return (MetadataRepositoryElement[]) list.toArray(new MetadataRepositoryElement[list.size()]);
	}

	void validateButtons() {
		IAction action = (IAction) propertiesButton.getData(BUTTONACTION);
		if (action != null)
			propertiesButton.setEnabled(action.isEnabled());
		action = (IAction) removeButton.getData(BUTTONACTION);
		if (action != null)
			removeButton.setEnabled(action.isEnabled());
	}

}
