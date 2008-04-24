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
package org.eclipse.equinox.internal.p2.ui.sdk;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.equinox.internal.p2.ui.sdk.externalFiles.MetadataGeneratingURLValidator;
import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceConstants;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.*;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.*;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.*;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.*;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IQueryProvider;
import org.eclipse.equinox.internal.provisional.p2.ui.sdk.ProvPolicies;
import org.eclipse.equinox.internal.provisional.p2.ui.sdk.RepositoryManipulationDialog;
import org.eclipse.equinox.internal.provisional.p2.ui.viewers.StructuredViewerProvisioningListener;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Dialog that allows users to update their installed IU's or find new ones.
 * 
 * @since 3.4
 */
public class UpdateAndInstallDialog extends TrayDialog implements IViewMenuProvider {
	private static final String BUTTONACTION = "buttonAction"; //$NON-NLS-1$
	private static final int DEFAULT_HEIGHT = 240;
	private static final int DEFAULT_WIDTH = 300;
	private static final int CHAR_INDENT = 5;
	private static final int SITE_COLUMN_WIDTH_IN_DLUS = 300;
	private static final int OTHER_COLUMN_WIDTH_IN_DLUS = 200;
	private static final int VERTICAL_MARGIN_DLU = 2;
	private static final int DEFAULT_VIEW_TYPE = AvailableIUViewQueryContext.VIEW_BY_REPO;
	private static final int INDEX_INSTALLED = 0;
	private static final int INDEX_AVAILABLE = 1;
	private static final String DIALOG_SETTINGS_SECTION = "UpdateAndInstallDialog"; //$NON-NLS-1$
	private static final String SELECTED_TAB_SETTING = "SelectedTab"; //$NON-NLS-1$
	private static final String AVAILABLE_VIEW_TYPE = "AvailableViewType"; //$NON-NLS-1$
	private static final String SHOW_LATEST_VERSIONS_ONLY = "ShowLatestVersionsOnly"; //$NON-NLS-1$
	private static final String HIDE_INSTALLED_IUS = "HideInstalledContent"; //$NON-NLS-1$

	String profileId;
	Display display;
	AvailableIUViewQueryContext queryContext;
	TabFolder tabFolder;
	AvailableIUGroup availableIUGroup;
	InstalledIUGroup installedIUGroup;
	IRepositoryManipulator repositoryManipulator;
	ChangeViewAction viewByRepo, viewFlat, viewCategory;
	Button installedPropButton, availablePropButton, installButton, uninstallButton, updateButton, revertButton, manipulateRepoButton, addRepoButton, removeRepoButton;
	Button showInstalledCheckbox, showLatestVersionsCheckbox;
	ProgressIndicator progressIndicator;
	Label progressLabel;
	IPropertyChangeListener preferenceListener;
	IJobChangeListener progressListener;
	StructuredViewerProvisioningListener profileListener;

	private class ChangeViewAction extends Action {
		int viewType;

		ChangeViewAction(String text, int viewType) {
			super(text, IAction.AS_RADIO_BUTTON);
			this.viewType = viewType;
			setChecked(this.viewType == queryContext.getViewType());
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.action.IAction#run()
		 */
		public void run() {
			if (this.viewType != queryContext.getViewType()) {
				queryContext.setViewType(viewType);
				updateAvailableViewState();
			}
		}
	}

	/**
	 * Create an instance of this Dialog.
	 * 
	 */
	public UpdateAndInstallDialog(Shell shell, String profileId) {
		super(shell);
		if (shell != null)
			this.display = shell.getDisplay();
		else
			this.display = PlatformUI.getWorkbench().getDisplay();
		this.profileId = profileId;
	}

	protected void configureShell(Shell shell) {
		shell.setText(ProvSDKMessages.UpdateAndInstallDialog_Title);
		super.configureShell(shell);
	}

	protected Control createDialogArea(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		GC gc = new GC(comp);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		comp.setLayout(layout);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		comp.setLayoutData(gd);

		gc.setFont(JFaceResources.getDialogFont());
		gc.dispose();

		createTabFolder(comp);

		IPreferenceStore store = ProvSDKUIActivator.getDefault().getPreferenceStore();
		preferenceListener = new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().equals(PreferenceConstants.PREF_SHOW_LATEST_VERSION))
					availableIUGroup.getStructuredViewer().refresh();
			}
		};
		store.addPropertyChangeListener(preferenceListener);
		comp.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				handleDispose();
			}
		});

		Link updatePrefsLink = new Link(comp, SWT.LEFT | SWT.WRAP);
		updatePrefsLink.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {

				PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(getShell(), PreferenceConstants.PREF_PAGE_AUTO_UPDATES, null, null);
				dialog.open();
			}
		});
		updatePrefsLink.setText(ProvSDKMessages.UpdateAndInstallDialog_PrefLink);
		createProgressArea(comp);
		initializeWidgetState();
		Dialog.applyDialogFont(comp);
		return comp;
	}

	private void initializeWidgetState() {
		// Set widgets according to query context
		showInstalledCheckbox.setSelection(!queryContext.getHideAlreadyInstalled());
		showLatestVersionsCheckbox.setSelection(queryContext.getShowLatestVersionsOnly());
		updateTreeColumns();
	}

	private void createTabFolder(Composite parent) {

		// tab folder
		tabFolder = new TabFolder(parent, SWT.NONE);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = convertVerticalDLUsToPixels(DEFAULT_HEIGHT);
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		tabFolder.setLayoutData(gd);

		// Installed and Available tabs
		TabItem installedTab = new TabItem(tabFolder, SWT.NONE);
		installedTab.setText(ProvSDKMessages.UpdateAndInstallDialog_InstalledSoftware);

		TabItem availableTab = new TabItem(tabFolder, SWT.NONE);
		availableTab.setText(ProvSDKMessages.UpdateAndInstallDialog_AvailableSoftware);

		// Reading the settings will also initialize the query context, so do this before
		// creating the individual pages, which consult the context.
		readDialogSettings();

		// Now create the actual pages.
		installedTab.setControl(createInstalledIUsPage(tabFolder));
		availableTab.setControl(createAvailableIUsPage(tabFolder));

		setDropTarget(tabFolder);
	}

	private void createProgressArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		composite.setLayoutData(gd);
		progressLabel = new Label(composite, SWT.NONE);
		progressIndicator = new ProgressIndicator(composite);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		progressIndicator.setLayoutData(gd);
		progressListener = new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				display.asyncExec(new Runnable() {
					public void run() {
						checkProgressIndicator(null);
					}
				});
			}

			public void scheduled(final IJobChangeEvent event) {
				display.asyncExec(new Runnable() {
					public void run() {
						checkProgressIndicator(event.getJob().getName());
					}
				});
			}
		};
		ProvisioningOperationRunner.addJobChangeListener(progressListener);
		checkProgressIndicator(null);
	}

	// May be called from an async exec, so check that we are still
	// alive.
	void checkProgressIndicator(String jobName) {
		if (progressIndicator.isDisposed())
			return;
		if (ProvisioningOperationRunner.hasScheduledOperations()) {
			progressIndicator.beginAnimatedTask();
			if (jobName == null)
				progressLabel.setText(ProvSDKMessages.UpdateAndInstallDialog_OperationInProgress);
			else
				progressLabel.setText(NLS.bind(ProvSDKMessages.UpdateAndInstallDialog_NamedOperationInProgress, jobName));
		} else {
			progressIndicator.done();
			progressLabel.setText(""); //$NON-NLS-1$
		}
		progressLabel.getParent().layout(true);
		// These may have changed because the status of scheduled provisioning jobs changed
		validateAvailableIUButtons();
		validateInstalledIUButtons();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	protected boolean isResizable() {
		return true;
	}

	private IRepositoryManipulator getRepositoryManipulator() {
		if (repositoryManipulator == null)
			repositoryManipulator = new IRepositoryManipulator() {
				public String getManipulatorLabel() {
					return ProvSDKMessages.UpdateAndInstallDialog_ManageSites;

				}

				public boolean manipulateRepositories(Shell shell) {
					new RepositoryManipulationDialog(shell, this).open();
					return true;
				}

				public ProvisioningOperation getAddOperation(URL repoURL) {
					return new AddColocatedRepositoryOperation(getAddOperationLabel(), repoURL);
				}

				public String getAddOperationLabel() {
					return ProvSDKMessages.UpdateAndInstallDialog_AddSiteOperationlabel;
				}

				public URL[] getKnownRepositories() {
					try {
						return ProvisioningUtil.getMetadataRepositories(IMetadataRepositoryManager.REPOSITORIES_ALL);
					} catch (ProvisionException e) {
						return new URL[0];
					}
				}

				public ProvisioningOperation getRemoveOperation(URL[] reposToRemove) {
					return new RemoveColocatedRepositoryOperation(getRemoveOperationLabel(), reposToRemove);
				}

				public String getRemoveOperationLabel() {
					return ProvSDKMessages.UpdateAndInstallDialog_RemoveSiteOperationLabel;
				}

				public URLValidator getURLValidator(Shell shell) {
					MetadataGeneratingURLValidator validator = new MetadataGeneratingURLValidator();
					validator.setShell(shell);
					return validator;
				}
			};
		return repositoryManipulator;
	}

	private Control createAvailableIUsPage(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = convertHorizontalDLUsToPixels(DEFAULT_WIDTH);
		composite.setLayoutData(gd);
		setDropTarget(composite);

		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = 0;
		layout.marginTop = 0;
		// breathing room underneath last checkboxes
		layout.marginBottom = convertVerticalDLUsToPixels(VERTICAL_MARGIN_DLU);

		composite.setLayout(layout);

		// Now the available group 
		availableIUGroup = new AvailableIUGroup(composite, ProvSDKUIActivator.getDefault().getQueryProvider(), JFaceResources.getDialogFont(), new ProvisioningContext(), queryContext, new AvailableIUPatternFilter(ProvUI.getIUColumnConfig()), ProvUI.getIUColumnConfig(), this, true);

		// Vertical buttons
		Composite vButtonBar = (Composite) createAvailableIUsVerticalButtonBar(composite);
		GridData data = new GridData(GridData.FILL_VERTICAL);
		data.verticalIndent = convertVerticalDLUsToPixels(IDialogConstants.BUTTON_BAR_HEIGHT);
		vButtonBar.setLayoutData(data);

		// Selection listeners must be registered on both the normal selection
		// events and the check mark events.  Must be done after buttons 
		// are created so that the buttons can
		// register and receive their selection notifications before us.
		availableIUGroup.getStructuredViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				validateAvailableIUButtons();
			}
		});

		availableIUGroup.getCheckMappingSelectionProvider().addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				validateAvailableIUButtons();
			}
		});

		// We might need to adjust the content of this viewer according to installation
		// changes.  We want to be very selective about refreshing.
		profileListener = new StructuredViewerProvisioningListener(availableIUGroup.getStructuredViewer(), StructuredViewerProvisioningListener.PROV_EVENT_PROFILE, ProvSDKUIActivator.getDefault().getQueryProvider()) {
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

		availableIUGroup.setUseBoldFontForFilteredItems(queryContext.getViewType() != AvailableIUViewQueryContext.VIEW_FLAT);
		setDropTarget(availableIUGroup.getStructuredViewer().getControl());

		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 2;
		gd.horizontalIndent = convertWidthInCharsToPixels(CHAR_INDENT);
		gd.verticalIndent = convertVerticalDLUsToPixels(VERTICAL_MARGIN_DLU);
		showLatestVersionsCheckbox = new Button(composite, SWT.CHECK);
		showLatestVersionsCheckbox.setText(ProvSDKMessages.UpdateAndInstallDialog_ShowLatestVersionsOnly);
		showLatestVersionsCheckbox.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				updateAvailableViewState();
			}

			public void widgetSelected(SelectionEvent e) {
				updateAvailableViewState();
			}
		});
		showLatestVersionsCheckbox.setLayoutData(gd);

		showInstalledCheckbox = new Button(composite, SWT.CHECK);
		showInstalledCheckbox.setText(ProvSDKMessages.UpdateAndInstallDialog_ShowAlreadyInstalledItems);
		showInstalledCheckbox.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				updateAvailableViewState();
			}

			public void widgetSelected(SelectionEvent e) {
				updateAvailableViewState();
			}
		});
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 2;
		gd.horizontalIndent = convertWidthInCharsToPixels(CHAR_INDENT);
		showInstalledCheckbox.setLayoutData(gd);

		validateAvailableIUButtons();
		return composite;
	}

	public void fillViewMenu(IMenuManager viewMenu) {
		viewByRepo = new ChangeViewAction(ProvSDKMessages.UpdateAndInstallDialog_ViewBySite, AvailableIUViewQueryContext.VIEW_BY_REPO);
		viewMenu.add(viewByRepo);
		viewCategory = new ChangeViewAction(ProvSDKMessages.UpdateAndInstallDialog_ViewByCategory, AvailableIUViewQueryContext.VIEW_BY_CATEGORY);
		viewMenu.add(viewCategory);
		viewFlat = new ChangeViewAction(ProvSDKMessages.UpdateAndInstallDialog_ViewByName, AvailableIUViewQueryContext.VIEW_FLAT);
		viewMenu.add(viewFlat);
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
		installButton = createVerticalButton(composite, ProvUI.INSTALL_COMMAND_LABEL, false);
		IAction installAction = new InstallAction(availableIUGroup.getCheckMappingSelectionProvider(), profileId, null, ProvPolicies.getDefault(), getShell());
		installButton.setData(BUTTONACTION, installAction);
		availablePropButton = createVerticalButton(composite, ProvSDKMessages.UpdateAndInstallDialog_Properties, false);

		// We use the viewer selection for properties, not the check marks
		IAction propertiesAction = new PropertyDialogAction(new SameShellProvider(parent.getShell()), availableIUGroup.getStructuredViewer());
		availablePropButton.setData(BUTTONACTION, propertiesAction);

		// spacer
		new Label(composite, SWT.NONE);

		IAction refreshAction = new RefreshAction(availableIUGroup.getStructuredViewer(), availableIUGroup.getStructuredViewer().getControl()) {
			protected void refresh() {
				availableIUGroup.refresh();
			}
		};
		Button refreshButton = createVerticalButton(composite, refreshAction.getText(), false);
		refreshButton.setData(BUTTONACTION, refreshAction);

		// spacer
		new Label(composite, SWT.NONE);

		manipulateRepoButton = createVerticalButton(composite, ProvSDKMessages.UpdateAndInstallDialog_ManageSites, false);
		manipulateRepoButton.setData(BUTTONACTION, new Action() {
			public void runWithEvent(Event event) {
				getRepositoryManipulator().manipulateRepositories(getShell());
			}
		});
		addRepoButton = createVerticalButton(composite, ProvSDKMessages.UpdateAndInstallDialog_AddSiteButtonText, false);
		addRepoButton.setData(BUTTONACTION, new AddColocatedRepositoryAction(availableIUGroup.getStructuredViewer(), getShell()));
		removeRepoButton = createVerticalButton(composite, ProvSDKMessages.UpdateAndInstallDialog_RemoveSiteButtonText, false);
		removeRepoButton.setData(BUTTONACTION, new RemoveColocatedRepositoryAction(availableIUGroup.getStructuredViewer(), getShell()));

		createMenu(availableIUGroup.getStructuredViewer().getControl(), new IAction[] {installAction, propertiesAction, refreshAction});

		return composite;
	}

	void updateAvailableViewState() {
		if (availableIUGroup.getTree() == null || availableIUGroup.getTree().isDisposed())
			return;
		final Composite parent = availableIUGroup.getComposite().getParent();
		validateAvailableIUButtons();
		availableIUGroup.setUseBoldFontForFilteredItems(queryContext.getViewType() != AvailableIUViewQueryContext.VIEW_FLAT);

		BusyIndicator.showWhile(display, new Runnable() {
			public void run() {
				parent.setRedraw(false);
				updateTreeColumns();
				queryContext.setShowLatestVersionsOnly(showLatestVersionsCheckbox.getSelection());
				if (showInstalledCheckbox.getSelection())
					queryContext.showAlreadyInstalled();
				else
					queryContext.hideAlreadyInstalled(profileId);
				availableIUGroup.setQueryContext(queryContext);
				parent.layout(true);
				parent.setRedraw(true);
			}
		});
	}

	void updateTreeColumns() {
		if (availableIUGroup.getTree() == null || availableIUGroup.getTree().isDisposed())
			return;
		TreeColumn[] columns = availableIUGroup.getTree().getColumns();
		if (columns.length > 0)
			columns[0].setWidth(convertHorizontalDLUsToPixels(queryContext.getViewType() == AvailableIUViewQueryContext.VIEW_BY_REPO ? SITE_COLUMN_WIDTH_IN_DLUS : OTHER_COLUMN_WIDTH_IN_DLUS));

	}

	void validateAvailableIUButtons() {
		// This relies on the actions themselves receiving the selection changed
		// listener before we do, since we use their state to enable the buttons.
		updateEnablement(installButton);
		updateEnablement(availablePropButton);
		updateEnablement(manipulateRepoButton);
		updateEnablement(addRepoButton);
		updateEnablement(removeRepoButton);
		boolean showRepos = queryContext.getViewType() == AvailableIUViewQueryContext.VIEW_BY_REPO;
		addRepoButton.setVisible(showRepos);
		removeRepoButton.setVisible(showRepos);
	}

	private Control createInstalledIUsPage(Composite parent) {

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
		installedIUGroup = new InstalledIUGroup(composite, ProvSDKUIActivator.getDefault().getQueryProvider(), JFaceResources.getDialogFont(), new ProvisioningContext(), profileId);

		// Vertical buttons
		Composite vButtonBar = (Composite) createInstalledIUsVerticalButtonBar(composite, ProvSDKUIActivator.getDefault().getQueryProvider());
		GridData data = new GridData(GridData.FILL_VERTICAL);
		data.verticalIndent = convertVerticalDLUsToPixels(IDialogConstants.BUTTON_BAR_HEIGHT);
		vButtonBar.setLayoutData(data);

		// Must be done after buttons are created so that the buttons can
		// register and receive their selection notifications before us.
		installedIUGroup.getStructuredViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				validateInstalledIUButtons();
			}
		});

		setDropTarget(installedIUGroup.getStructuredViewer().getControl());

		validateInstalledIUButtons();
		return composite;
	}

	private Control createInstalledIUsVerticalButtonBar(Composite parent, IQueryProvider queryProvider) {
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
		updateButton = createVerticalButton(composite, ProvUI.UPDATE_COMMAND_LABEL, false);
		// For update only, we want it to check for all updates if there is nothing selected
		IAction updateAction = new UpdateAction(new ISelectionProvider() {
			public void addSelectionChangedListener(ISelectionChangedListener listener) {
				installedIUGroup.getStructuredViewer().addSelectionChangedListener(listener);
			}

			public ISelection getSelection() {
				StructuredViewer viewer = installedIUGroup.getStructuredViewer();
				ISelection selection = viewer.getSelection();
				if (selection.isEmpty()) {
					final Object[] all = ((IStructuredContentProvider) installedIUGroup.getStructuredViewer().getContentProvider()).getElements(viewer.getInput());
					return new StructuredSelection(all);
				}
				return selection;
			}

			public void removeSelectionChangedListener(ISelectionChangedListener listener) {
				installedIUGroup.getStructuredViewer().removeSelectionChangedListener(listener);
			}

			public void setSelection(ISelection selection) {
				installedIUGroup.getStructuredViewer().setSelection(selection);
			}
		}, profileId, null, ProvPolicies.getDefault(), parent.getShell());
		updateButton.setData(BUTTONACTION, updateAction);

		uninstallButton = createVerticalButton(composite, ProvUI.UNINSTALL_COMMAND_LABEL, false);
		IAction uninstallAction = new UninstallAction(installedIUGroup.getStructuredViewer(), profileId, null, ProvPolicies.getDefault(), parent.getShell());
		uninstallButton.setData(BUTTONACTION, uninstallAction);

		installedPropButton = createVerticalButton(composite, ProvSDKMessages.UpdateAndInstallDialog_Properties, false);
		IAction propertiesAction = new PropertyDialogAction(new SameShellProvider(parent.getShell()), installedIUGroup.getStructuredViewer());
		installedPropButton.setData(BUTTONACTION, propertiesAction);

		// spacer
		new Label(composite, SWT.NONE);

		revertButton = createVerticalButton(composite, ProvSDKMessages.UpdateAndInstallDialog_RevertActionLabel, false);
		revertButton.setData(BUTTONACTION, new Action() {
			public void run() {
				RevertWizard wizard = new RevertWizard(profileId, ProvSDKUIActivator.getDefault().getQueryProvider());
				WizardDialog dialog = new WizardDialog(getShell(), wizard);
				dialog.create();
				dialog.getShell().setSize(600, 500);
				dialog.open();
			}
		});
		createMenu(installedIUGroup.getStructuredViewer().getControl(), new IAction[] {updateAction, uninstallAction, propertiesAction});

		return composite;
	}

	private void createMenu(Control control, IAction[] actions) {
		MenuManager menuManager = new MenuManager();
		for (int i = 0; i < actions.length; i++)
			menuManager.add(actions[i]);
		Menu menu = menuManager.createContextMenu(control);
		control.setMenu(menu);
	}

	void validateInstalledIUButtons() {
		// Note that this relies on the actions getting the selection notification
		// before we do, since we rely on the action enablement to update
		// the buttons.  This should be ok since the buttons
		// hook the listener on create.
		updateEnablement(installedPropButton);
		updateEnablement(uninstallButton);
		updateEnablement(updateButton);
		// special case because revert isn't a standard action
		revertButton.setEnabled(!ProvisioningOperationRunner.hasScheduledOperationsFor(profileId));

	}

	private void updateEnablement(Button button) {
		// At this point the action's enablement is correct for its
		// selection.  Now we want to double check other conditions.
		IAction action = getButtonAction(button);
		if (action != null) {
			if (action instanceof ProvisioningAction)
				((ProvisioningAction) action).checkEnablement();
			button.setEnabled(action.isEnabled());
		}
	}

	private Button createVerticalButton(Composite parent, String label, boolean defaultButton) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(label);

		setButtonLayoutData(button);
		Object data = button.getLayoutData();
		if (data instanceof GridData)
			((GridData) data).horizontalAlignment = GridData.FILL;

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

	private void readDialogSettings() {
		if (tabFolder == null || tabFolder.isDisposed())
			return;

		// Initialize the query context using defaults and pref values
		queryContext = new AvailableIUViewQueryContext(DEFAULT_VIEW_TYPE);
		IPreferenceStore store = ProvSDKUIActivator.getDefault().getPreferenceStore();
		queryContext.setShowLatestVersionsOnly(store.getBoolean(PreferenceConstants.PREF_SHOW_LATEST_VERSION));
		queryContext.hideAlreadyInstalled(profileId);

		// Now refine it based on the dialog settings
		IDialogSettings settings = ProvSDKUIActivator.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(DIALOG_SETTINGS_SECTION);
		if (section != null) {
			// Default selected tab
			int tab = 0;
			if (section.get(SELECTED_TAB_SETTING) != null)
				tab = section.getInt(SELECTED_TAB_SETTING);
			tabFolder.setSelection(tab);

			// View by...
			try {
				if (section.get(AVAILABLE_VIEW_TYPE) != null)
					queryContext.setViewType(section.getInt(AVAILABLE_VIEW_TYPE));
			} catch (NumberFormatException e) {
				// Ignore if there actually was a value that didn't parse.  
				// We'll get a default query context below.
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

	private void saveDialogSettings() {
		if (!tabFolder.isDisposed()) {
			getDialogBoundsSettings().put(SELECTED_TAB_SETTING, tabFolder.getSelectionIndex());
			getDialogBoundsSettings().put(AVAILABLE_VIEW_TYPE, queryContext.getViewType());
			getDialogBoundsSettings().put(SHOW_LATEST_VERSIONS_ONLY, showLatestVersionsCheckbox.getSelection());
			getDialogBoundsSettings().put(HIDE_INSTALLED_IUS, !showInstalledCheckbox.getSelection());
		}
	}

	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings settings = ProvSDKUIActivator.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(DIALOG_SETTINGS_SECTION);
		if (section == null) {
			section = settings.addNewSection(DIALOG_SETTINGS_SECTION);
		}
		return section;
	}

	/**
	 * Overridden to provide a close button.
	 * 
	 * @param parent
	 *            the button bar composite
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, true);
	}

	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.CLOSE_ID == buttonId) {
			saveDialogSettings();
			close();
		}
		super.buttonPressed(buttonId);
	}

	private void setDropTarget(Control control) {
		DropTarget target = new DropTarget(control, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);
		target.setTransfer(new Transfer[] {URLTransfer.getInstance(), FileTransfer.getInstance()});
		target.addDropListener(new RepositoryManipulatorDropTarget(getRepositoryManipulator(), control) {
			protected boolean dropTargetIsValid(DropTargetEvent event) {
				if (URLTransfer.getInstance().isSupportedType(event.currentDataType)) {
					// If we are on available features page or tab, all drops are good.
					if (tabFolder.getSelectionIndex() == INDEX_AVAILABLE)
						return super.dropTargetIsValid(event);
					// This is not working
					// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=222120
					if (tabFolder.getItem(INDEX_AVAILABLE) == event.item)
						return super.dropTargetIsValid(event);
					if (tabFolder.getSelectionIndex() == INDEX_INSTALLED) {
						String path = (String) URLTransfer.getInstance().nativeToJava(event.currentDataType);
						if (path != null) {
							URL url = null;
							try {
								url = new URL(path);
							} catch (MalformedURLException e) {
								return false;
							}
							if (url != null && URLValidator.isFileURL(url))
								return true;
						}
					}
				}
				return super.dropTargetIsValid(event);
			}
		});
	}

	void handleDispose() {
		if (preferenceListener != null) {
			ProvSDKUIActivator.getDefault().getPreferenceStore().removePropertyChangeListener(preferenceListener);
			preferenceListener = null;
		}
		if (progressListener != null) {
			ProvisioningOperationRunner.removeJobChangeListener(progressListener);
			progressListener = null;
		}
		if (profileListener != null) {
			ProvUI.removeProvisioningListener(profileListener);
			profileListener = null;
		}

	}
}
