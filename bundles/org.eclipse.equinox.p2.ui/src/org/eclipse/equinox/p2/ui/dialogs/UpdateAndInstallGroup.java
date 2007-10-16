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

import org.eclipse.core.runtime.Assert;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.ui.*;
import org.eclipse.equinox.p2.ui.actions.*;
import org.eclipse.equinox.p2.ui.model.*;
import org.eclipse.equinox.p2.ui.viewers.*;
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
public class UpdateAndInstallGroup {

	private static final String BUTTONACTION = "buttonAction"; //$NON-NLS-1$
	private static final int DEFAULT_HEIGHT = 240;
	private static final int DEFAULT_WIDTH = 450;
	private static final int DEFAULT_COLUMN_WIDTH = 150;
	TabFolder tabFolder;
	TableViewer installedIUViewer;
	TableViewer availableIUViewer;
	Profile profile;
	IRepositoryManipulator repositoryManipulator;
	IProfileChooser profileChooser;
	private FontMetrics fm;
	Button installedPropButton, availablePropButton, installButton, uninstallButton, updateButton;

	/**
	 * Create an instance of this group.
	 * 
	 */
	// TODO we currently specify the filtering for what IU's we want to see with
	// a viewer filter. More likely we would pass in the required
	// capability/version info
	// and use the iterator API rather than get all IU's and then filter them
	public UpdateAndInstallGroup(Composite parent, Profile profile, ViewerFilter[] installedIUFilters, ViewerFilter[] availableIUFilters, String installedString, String availableString, IRepositoryManipulator repositoryManipulator, IProfileChooser profileChooser, FontMetrics fm) {

		this.profile = profile;
		this.repositoryManipulator = repositoryManipulator;
		this.profileChooser = profileChooser;

		// tab folder
		tabFolder = new TabFolder(parent, SWT.NONE);

		Assert.isNotNull(fm);
		this.fm = fm;
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = convertVerticalDLUsToPixels(DEFAULT_HEIGHT);
		tabFolder.setLayoutData(gd);

		// Installed IU's
		TabItem installedTab = new TabItem(tabFolder, SWT.NONE);
		installedTab.setText(installedString);
		installedTab.setControl(createInstalledIUsPage(tabFolder, installedIUFilters));

		// Find IU's
		TabItem availableTab = new TabItem(tabFolder, SWT.NONE);
		availableTab.setText(availableString);
		availableTab.setControl(createAvailableIUsPage(tabFolder, availableIUFilters));
	}

	public Control getControl() {
		return tabFolder;
	}

	private Control createAvailableIUsPage(Composite parent, ViewerFilter[] iuFilters) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = convertHorizontalDLUsToPixels(DEFAULT_WIDTH);
		composite.setLayoutData(gd);

		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);

		// Table of available IU's
		availableIUViewer = new TableViewer(composite, SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		setTableColumns(availableIUViewer.getTable());
		availableIUViewer.setContentProvider(new AvailableIUContentProvider());
		availableIUViewer.setInput(new AllMetadataRepositories());
		availableIUViewer.setLabelProvider(new IUDetailsLabelProvider());
		if (iuFilters != null) {
			availableIUViewer.setFilters(iuFilters);
		}
		availableIUViewer.setComparator(new ViewerComparator());
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		availableIUViewer.getControl().setLayoutData(data);

		// Vertical buttons
		Composite buttonBar = (Composite) createAvailableIUsVerticalButtonBar(composite);
		data = new GridData(GridData.FILL_VERTICAL);
		buttonBar.setLayoutData(data);

		// Must be done after buttons are created so that the buttons can
		// register and receive their selection notifications before us.
		availableIUViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				validateAvailableIUButtons(event.getSelection());
			}
		});

		final StructuredViewerProvisioningListener listener = new StructuredViewerProvisioningListener(availableIUViewer, StructuredViewerProvisioningListener.PROV_EVENT_REPOSITORY);
		ProvUIActivator.getDefault().addProvisioningListener(listener);
		availableIUViewer.getControl().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				ProvUIActivator.getDefault().removeProvisioningListener(listener);
			}
		});

		validateAvailableIUButtons(installedIUViewer.getSelection());
		return composite;
	}

	private Control createAvailableIUsVerticalButtonBar(Composite parent) {
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
		availablePropButton = createVerticalButton(composite, ProvUIMessages.UpdateAndInstallGroup_Properties, false);
		availablePropButton.setData(BUTTONACTION, new PropertyDialogAction(new SameShellProvider(parent.getShell()), availableIUViewer));
		installButton = createVerticalButton(composite, ProvUIMessages.InstallIUCommandLabel, false);
		installButton.setData(BUTTONACTION, new InstallAction(availableIUViewer, profile, null, parent.getShell()));
		Button refreshButton = createVerticalButton(composite, ProvUIMessages.UpdateAndInstallGroup_Refresh, false);
		refreshButton.setData(BUTTONACTION, new Action() {
			public void runWithEvent(Event event) {
				availableIUViewer.setInput(new AllMetadataRepositories());
			}
		});

		if (repositoryManipulator != null) {
			Button repoButton = createVerticalButton(composite, repositoryManipulator.getLabel(), false);
			repoButton.setData(BUTTONACTION, new Action() {
				public void runWithEvent(Event event) {
					if (repositoryManipulator.manipulateRepositories(getControl().getShell())) {
						availableIUViewer.setInput(new AllMetadataRepositories());
					}
				}
			});

		}
		return composite;
	}

	void validateAvailableIUButtons(ISelection selection) {
		// This relies on the actions themselves receiving the selection changed
		// listener before we do, since we use their state to enable the buttons
		updateEnablement(availablePropButton);
		updateEnablement(installButton);
	}

	private Control createInstalledIUsPage(Composite parent, ViewerFilter[] iuFilters) {

		Composite composite = new Composite(parent, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = convertHorizontalDLUsToPixels(DEFAULT_WIDTH);
		composite.setLayoutData(gd);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);

		// Table of installed IU's
		installedIUViewer = new TableViewer(composite, SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		setTableColumns(installedIUViewer.getTable());
		installedIUViewer.setContentProvider(new ProfileContentProvider());
		installedIUViewer.setInput(profile);
		installedIUViewer.setLabelProvider(new IUDetailsLabelProvider());

		if (iuFilters != null) {
			installedIUViewer.setFilters(iuFilters);
		}
		installedIUViewer.setComparator(new ViewerComparator());

		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		installedIUViewer.getControl().setLayoutData(data);

		// Vertical buttons
		Composite buttonBar = (Composite) createInstalledIUsVerticalButtonBar(composite);
		data = new GridData(GridData.FILL_VERTICAL);
		buttonBar.setLayoutData(data);

		// Must be done after buttons are created so that the buttons can
		// register and receive their selection notifications before us.
		installedIUViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				validateInstalledIUButtons(event.getSelection());
			}
		});

		final StructuredViewerProvisioningListener listener = new StructuredViewerProvisioningListener(installedIUViewer, StructuredViewerProvisioningListener.PROV_EVENT_IU | StructuredViewerProvisioningListener.PROV_EVENT_PROFILE);
		ProvUIActivator.getDefault().addProvisioningListener(listener);
		installedIUViewer.getControl().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				ProvUIActivator.getDefault().removeProvisioningListener(listener);
			}
		});
		validateInstalledIUButtons(installedIUViewer.getSelection());
		return composite;
	}

	private Control createInstalledIUsVerticalButtonBar(Composite parent) {
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
		installedPropButton = createVerticalButton(composite, ProvUIMessages.UpdateAndInstallGroup_Properties, false);
		installedPropButton.setData(BUTTONACTION, new PropertyDialogAction(new SameShellProvider(parent.getShell()), installedIUViewer));
		uninstallButton = createVerticalButton(composite, ProvUIMessages.UninstallIUCommandLabel, false);
		uninstallButton.setData(BUTTONACTION, new UninstallAction(installedIUViewer, profile, null, parent.getShell()));
		updateButton = createVerticalButton(composite, ProvUIMessages.UpdateIUCommandLabel, false);
		updateButton.setData(BUTTONACTION, new UpdateAction(installedIUViewer, profile, null, parent.getShell()));
		if (profileChooser != null) {
			Button profileButton = createVerticalButton(composite, profileChooser.getLabel(), false);
			profileButton.setData(BUTTONACTION, new Action() {
				public void runWithEvent(Event event) {
					Profile chosenProfile = profileChooser.getProfile();
					if (chosenProfile != null) {
						profile = chosenProfile;
						installedIUViewer.setInput(profile);
					}
				}
			});
		}

		return composite;
	}

	void validateInstalledIUButtons(ISelection selection) {
		// Note that this relies on the actions getting the selection notification
		// before we do, since we rely on the action enablement to update
		// the buttons.  This should be ok since the buttons
		// hook the listener on create.
		updateEnablement(installedPropButton);
		updateEnablement(uninstallButton);
		updateEnablement(updateButton);
	}

	private void updateEnablement(Button button) {
		IAction action = getButtonAction(button);
		if (action != null) {
			button.setEnabled(action.isEnabled());
		}
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
		IAction action = getButtonAction(event.widget);
		if (action != null) {
			action.runWithEvent(event);
		}
	}

	private IAction getButtonAction(Widget widget) {
		Object data = widget.getData(BUTTONACTION);
		if (data == null || !(data instanceof IAction)) {
			return null;
		}
		return (IAction) data;
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
		// TODO will we ever let callers set the column config?
		IUColumnConfig[] columns = ProvUI.getIUColumnConfig();
		table.setHeaderVisible(true);

		for (int i = 0; i < columns.length; i++) {
			TableColumn tc = new TableColumn(table, SWT.NONE, i);
			tc.setResizable(true);
			tc.setText(columns[i].columnTitle);
			tc.setWidth(convertHorizontalDLUsToPixels(DEFAULT_COLUMN_WIDTH));
		}
	}

}
