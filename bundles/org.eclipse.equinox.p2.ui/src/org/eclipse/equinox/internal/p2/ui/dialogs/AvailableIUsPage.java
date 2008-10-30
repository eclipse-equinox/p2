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

import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.*;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.AvailableIUGroup;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.InstallWizard;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IUViewQueryContext;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.StructuredViewerProvisioningListener;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public class AvailableIUsPage extends WizardPage {
	private static final String DIALOG_SETTINGS_SECTION = "AvailableIUsPage"; //$NON-NLS-1$
	private static final String AVAILABLE_VIEW_TYPE = "AvailableViewType"; //$NON-NLS-1$
	private static final String SHOW_LATEST_VERSIONS_ONLY = "ShowLatestVersionsOnly"; //$NON-NLS-1$
	private static final String HIDE_INSTALLED_IUS = "HideInstalledContent"; //$NON-NLS-1$
	private static final String BUTTONACTION = "buttonAction"; //$NON-NLS-1$
	private static final int DEFAULT_WIDTH = 300;

	InstallWizard wizard;
	String profileId;
	Policy policy;
	QueryableMetadataRepositoryManager manager;
	IUViewQueryContext queryContext;
	AvailableIUGroup availableIUGroup;
	Composite availableIUButtonBar;
	Button availablePropButton, manipulateRepoButton, addRepoButton, installationInfoButton;
	Button showInstalledCheckbox, showLatestVersionsCheckbox;
	Text detailsArea;
	StructuredViewerProvisioningListener profileListener;
	Display display;

	public AvailableIUsPage(Policy policy, String profileId, InstallWizard wizard, QueryableMetadataRepositoryManager manager) {
		super("AvailableSoftwarePage"); //$NON-NLS-1$
		this.policy = policy;
		this.profileId = profileId;
		this.wizard = wizard;
		this.manager = manager;
		makeQueryContext();
		setTitle(ProvUIMessages.AvailableIUsPage_Title);
		setDescription(ProvUIMessages.AvailableIUsPage_Description);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		this.display = parent.getDisplay();

		Composite composite = new Composite(parent, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = convertHorizontalDLUsToPixels(DEFAULT_WIDTH);
		composite.setLayoutData(gd);
		setDropTarget(composite);

		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 0;

		composite.setLayout(layout);

		// Now the available group 
		availableIUGroup = new AvailableIUGroup(policy, composite, JFaceResources.getDialogFont(), manager, queryContext, ProvUI.getIUColumnConfig());

		// Vertical buttons
		Composite vButtonBar = (Composite) createVerticalButtonBar(composite);
		GridData data = new GridData(GridData.FILL_VERTICAL);
		vButtonBar.setLayoutData(data);

		// Selection listeners must be registered on both the normal selection
		// events and the check mark events.  Must be done after buttons 
		// are created so that the buttons can register and receive their selection notifications before us.
		availableIUGroup.getStructuredViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateDetails();
				validateAvailableIUButtons();
			}
		});

		availableIUGroup.getCheckboxTreeViewer().addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				validateNextButton();
			}
		});

		// We might need to adjust the content of this viewer according to installation
		// changes.  We want to be very selective about refreshing.
		profileListener = new StructuredViewerProvisioningListener(availableIUGroup.getStructuredViewer(), StructuredViewerProvisioningListener.PROV_EVENT_PROFILE) {
			protected void profileAdded(String id) {
				// do nothing
			}

			protected void profileRemoved(String id) {
				// do nothing
			}

			protected void profileChanged(String id) {
				if (id.equals(profileId)) {
					display.asyncExec(new Runnable() {
						public void run() {
							if (isClosing())
								return;
							if (!showInstalledCheckbox.getSelection())
								refreshAll();
						}
					});
				}
			}
		};
		ProvUI.addProvisioningListener(profileListener);

		availableIUGroup.setUseBoldFontForFilteredItems(queryContext.getViewType() != IUViewQueryContext.AVAILABLE_VIEW_FLAT);
		setDropTarget(availableIUGroup.getStructuredViewer().getControl());

		Group group = new Group(composite, SWT.NONE);
		group.setText(ProvUIMessages.ProfileModificationWizardPage_DetailsLabel);
		layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		group.setLayout(layout);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 2;
		group.setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 2;
		gd.verticalIndent = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		gd.heightHint = convertHeightInCharsToPixels(ILayoutConstants.DEFAULT_DESCRIPTION_HEIGHT);
		gd.widthHint = convertHorizontalDLUsToPixels(DEFAULT_WIDTH);

		detailsArea = new Text(group, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY | SWT.WRAP);
		detailsArea.setLayoutData(gd);

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 2;
		gd.verticalIndent = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);

		showLatestVersionsCheckbox = new Button(composite, SWT.CHECK);
		showLatestVersionsCheckbox.setText(ProvUIMessages.AvailableIUsPage_ShowLatestVersions);
		showLatestVersionsCheckbox.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				updateQueryContext();
				availableIUGroup.updateAvailableViewState();
			}

			public void widgetSelected(SelectionEvent e) {
				updateQueryContext();
				availableIUGroup.updateAvailableViewState();
			}
		});
		showLatestVersionsCheckbox.setLayoutData(gd);

		showInstalledCheckbox = new Button(composite, SWT.CHECK);
		showInstalledCheckbox.setText(ProvUIMessages.AvailableIUsPage_IncludeInstalledItems);
		showInstalledCheckbox.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				updateQueryContext();
				availableIUGroup.updateAvailableViewState();
			}

			public void widgetSelected(SelectionEvent e) {
				updateQueryContext();
				availableIUGroup.updateAvailableViewState();
			}
		});
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 2;
		showInstalledCheckbox.setLayoutData(gd);

		validateAvailableIUButtons();
		validateNextButton();
		initializeWidgetState();
		setControl(composite);
	}

	private Control createVerticalButtonBar(Composite parent) {
		// Create composite.
		// Cached so we can line things up later.
		availableIUButtonBar = new Composite(parent, SWT.NULL);

		// create a layout with spacing and margins appropriate for the font
		// size.
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginWidth = 5;
		layout.marginHeight = 0;
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
		availableIUButtonBar.setLayout(layout);

		// Add the buttons to the button bar.

		IAction propertiesAction = new PropertyDialogAction(new SameShellProvider(parent.getShell()), availableIUGroup.getStructuredViewer());
		availablePropButton = createVerticalButton(availableIUButtonBar, propertiesAction, false);

		// spacer
		new Label(availableIUButtonBar, SWT.NONE);

		IAction addSites = new AddColocatedRepositoryAction(availableIUGroup.getStructuredViewer());
		// Change the text so it's clear this is adding sites, not just "add".  Since items in the list are not all sites.
		addSites.setText(ProvUIMessages.AvailableIUsPage_AddSite);
		addRepoButton = createVerticalButton(availableIUButtonBar, addSites, false);

		IAction manipulateRepos = new Action() {
			public void runWithEvent(Event event) {
				policy.getRepositoryManipulator().manipulateRepositories(getShell());
			}
		};
		manipulateRepos.setText(ProvUIMessages.AvailableIUsPage_ManageSites);
		manipulateRepos.setToolTipText(ProvUIMessages.AvailableIUsPage_ManageSitesTooltip);
		manipulateRepoButton = createVerticalButton(availableIUButtonBar, manipulateRepos, false);

		// spacer
		new Label(availableIUButtonBar, SWT.NONE);

		IAction openInstallDialog = new Action() {
			public void runWithEvent(Event event) {
				ProvUI.openInstallationDialog(event);
			}
		};
		openInstallDialog.setText(ProvUIMessages.AvailableIUsPage_InstallInfo);
		openInstallDialog.setToolTipText(ProvUIMessages.AvailableIUsPage_InstallInfoTooltip);
		installationInfoButton = createVerticalButton(availableIUButtonBar, openInstallDialog, false);

		IAction refreshAction = new RefreshAction(availableIUGroup.getStructuredViewer(), availableIUGroup.getStructuredViewer().getControl()) {
			protected void refresh() {
				availableIUGroup.refresh();
			}
		};
		refreshAction.setToolTipText(ProvUIMessages.AvailableIUsPage_RefreshTooltip);
		createVerticalButton(availableIUButtonBar, refreshAction, false);

		createMenu(availableIUGroup.getStructuredViewer().getControl(), new IAction[] {propertiesAction, refreshAction});

		return availableIUButtonBar;
	}

	void validateNextButton() {
		setPageComplete(availableIUGroup.getCheckedLeafIUs().length > 0);
	}

	void validateAvailableIUButtons() {

		// This relies on the actions themselves receiving the selection changed
		// listener before we do, since we use their state to enable the buttons.
		updateEnablement(availablePropButton);
		updateEnablement(manipulateRepoButton);
		updateEnablement(addRepoButton);
	}

	private void createMenu(Control control, IAction[] actions) {
		MenuManager menuManager = new MenuManager();
		for (int i = 0; i < actions.length; i++)
			menuManager.add(actions[i]);
		Menu menu = menuManager.createContextMenu(control);
		control.setMenu(menu);
	}

	void updateQueryContext() {
		queryContext.setShowLatestVersionsOnly(showLatestVersionsCheckbox.getSelection());
		if (showInstalledCheckbox.getSelection())
			queryContext.showAlreadyInstalled();
		else
			queryContext.hideAlreadyInstalled(profileId);
	}

	void updateEnablement(Button button) {
		// At this point the action's enablement is correct for its
		// selection.  Now we want to double check other conditions.
		IAction action = getButtonAction(button);
		if (action != null) {
			if (action instanceof ProvisioningAction)
				((ProvisioningAction) action).checkEnablement();
			button.setEnabled(action.isEnabled());
		}
	}

	private Button createVerticalButton(Composite parent, IAction action, boolean defaultButton) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(action.getText());

		setButtonLayoutData(button);
		Object data = button.getLayoutData();
		if (data instanceof GridData)
			((GridData) data).horizontalAlignment = GridData.FILL;

		button.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				verticalButtonPressed(event);
			}
		});
		button.setToolTipText(action.getToolTipText());
		if (defaultButton) {
			Shell shell = parent.getShell();
			if (shell != null) {
				shell.setDefaultButton(button);
			}
		}
		button.setData(BUTTONACTION, action);
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

	private void setDropTarget(Control control) {
		DropTarget target = new DropTarget(control, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);
		target.setTransfer(new Transfer[] {URLTransfer.getInstance(), FileTransfer.getInstance()});
		target.addDropListener(new RepositoryManipulatorDropTarget(policy.getRepositoryManipulator(), control));
	}

	private void initializeWidgetState() {
		// Set widgets according to query context
		showInstalledCheckbox.setSelection(!queryContext.getHideAlreadyInstalled());
		showLatestVersionsCheckbox.setSelection(queryContext.getShowLatestVersionsOnly());
		availableIUGroup.updateTreeColumns();
		Control focusControl = null;
		focusControl = availableIUGroup.getDefaultFocusControl();
		if (focusControl != null)
			focusControl.setFocus();
		updateDetails();
	}

	public boolean performFinish() {
		savePageSettings();
		return true;
	}

	private void makeQueryContext() {
		// Make a local query context that is based on the default.
		IUViewQueryContext defaultQueryContext = policy.getQueryContext();
		queryContext = new IUViewQueryContext(defaultQueryContext.getViewType());
		queryContext.setArtifactRepositoryFlags(defaultQueryContext.getArtifactRepositoryFlags());
		queryContext.setMetadataRepositoryFlags(defaultQueryContext.getMetadataRepositoryFlags());
		if (defaultQueryContext.getHideAlreadyInstalled()) {
			queryContext.hideAlreadyInstalled(profileId);
		}
		queryContext.setShowLatestVersionsOnly(defaultQueryContext.getShowLatestVersionsOnly());
		queryContext.setVisibleAvailableIUProperty(defaultQueryContext.getVisibleAvailableIUProperty());
		queryContext.setVisibleInstalledIUProperty(defaultQueryContext.getVisibleInstalledIUProperty());
		// Now check for saved away dialog settings
		IDialogSettings settings = ProvUIActivator.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(DIALOG_SETTINGS_SECTION);
		if (section != null) {
			// View by...
			try {
				if (section.get(AVAILABLE_VIEW_TYPE) != null)
					queryContext.setViewType(section.getInt(AVAILABLE_VIEW_TYPE));
			} catch (NumberFormatException e) {
				// Ignore if there actually was a value that didn't parse.  
			}

			// Show latest versions
			if (section.get(SHOW_LATEST_VERSIONS_ONLY) != null)
				queryContext.setShowLatestVersionsOnly(section.getBoolean(SHOW_LATEST_VERSIONS_ONLY));

			// Hide installed content
			boolean hideContent = section.getBoolean(HIDE_INSTALLED_IUS);
			if (hideContent)
				queryContext.hideAlreadyInstalled(profileId);
			else
				queryContext.showAlreadyInstalled();
		}
	}

	private void savePageSettings() {
		if (getShell().isDisposed())
			return;
		IDialogSettings settings = ProvUIActivator.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(DIALOG_SETTINGS_SECTION);
		if (section == null) {
			section = settings.addNewSection(DIALOG_SETTINGS_SECTION);
		}
		section.put(AVAILABLE_VIEW_TYPE, queryContext.getViewType());
		section.put(SHOW_LATEST_VERSIONS_ONLY, showLatestVersionsCheckbox.getSelection());
		section.put(HIDE_INSTALLED_IUS, !showInstalledCheckbox.getSelection());
	}

	void updateDetails() {
		IInstallableUnit[] selected = getSelectedIUs();
		if (selected.length == 1) {
			String description = IUPropertyUtils.getIUProperty(selected[0], IInstallableUnit.PROP_DESCRIPTION);
			if (description != null) {
				detailsArea.setText(description);
				return;
			}
		}
		detailsArea.setText(""); //$NON-NLS-1$
	}

	public IInstallableUnit[] getSelectedIUs() {
		return availableIUGroup.getSelectedIUs();
	}

	/*
	 * This method is provided only for automated testing.
	 */
	public AvailableIUGroup testGetAvailableIUGroup() {
		return availableIUGroup;
	}

	public IInstallableUnit[] getCheckedIUs() {
		return availableIUGroup.getCheckedLeafIUs();
	}

	/*
	 * Overridden so that we don't call getNextPage().
	 * We use getNextPage() to start resolving the install so
	 * we only want to do that when the next button is pressed.
	 * 
	 * (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardPage#canFlipToNextPage()
	 */
	public boolean canFlipToNextPage() {
		return isPageComplete();
	}
}
