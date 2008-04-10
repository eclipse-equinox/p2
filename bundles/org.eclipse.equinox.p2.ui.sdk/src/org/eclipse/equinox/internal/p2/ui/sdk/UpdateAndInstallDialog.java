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
import org.eclipse.equinox.internal.p2.ui.sdk.externalFiles.MetadataGeneratingURLValidator;
import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceConstants;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.ui.IRepositoryManipulator;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.*;
import org.eclipse.equinox.internal.provisional.p2.ui.dialogs.*;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.*;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.IQueryProvider;
import org.eclipse.equinox.internal.provisional.p2.ui.sdk.ProvPolicies;
import org.eclipse.equinox.internal.provisional.p2.ui.sdk.RepositoryManipulationDialog;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
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
	private static final int INDEX_INSTALLED = 0;
	private static final int INDEX_AVAILABLE = 1;
	private static final String DIALOG_SETTINGS_SECTION = "UpdateAndInstallDialog"; //$NON-NLS-1$
	private static final String SELECTED_TAB_SETTING = "SelectedTab"; //$NON-NLS-1$
	private static final String AVAILABLE_VIEW_TYPE = "AvailableViewType"; //$NON-NLS-1$

	String profileId;
	AvailableIUViewQueryContext queryContext;
	TabFolder tabFolder;
	AvailableIUGroup availableIUGroup;
	InstalledIUGroup installedIUGroup;
	IRepositoryManipulator repositoryManipulator;
	ChangeViewAction viewByRepo, viewFlat, viewCategory;
	Button installedPropButton, availablePropButton, installButton, uninstallButton, updateButton, manipulateRepoButton, addRepoButton, removeRepoButton;

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

		final IPreferenceStore store = ProvSDKUIActivator.getDefault().getPreferenceStore();
		final IPropertyChangeListener preferenceListener = new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().equals(PreferenceConstants.PREF_SHOW_LATEST_VERSION))
					availableIUGroup.getStructuredViewer().refresh();
			}
		};
		store.addPropertyChangeListener(preferenceListener);
		comp.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				store.removePropertyChangeListener(preferenceListener);
			}
		});

		Link updatePrefsLink = new Link(comp, SWT.LEFT | SWT.WRAP);
		gd = new GridData();
		gd.horizontalIndent = convertHorizontalDLUsToPixels(IDialogConstants.SMALL_INDENT);
		updatePrefsLink.setLayoutData(gd);
		updatePrefsLink.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {

				PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(getShell(), PreferenceConstants.PREF_PAGE_AUTO_UPDATES, null, null);
				dialog.open();
			}
		});
		updatePrefsLink.setText(ProvSDKMessages.UpdateAndInstallDialog_PrefLink);
		Dialog.applyDialogFont(comp);
		return comp;
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
		layout.marginHeight = 0;
		composite.setLayout(layout);

		// Now the available group 
		availableIUGroup = new AvailableIUGroup(composite, ProvSDKUIActivator.getDefault().getQueryProvider(), JFaceResources.getDialogFont(), new ProvisioningContext(), queryContext, new AvailableIUPatternFilter(ProvUI.getIUColumnConfig()), ProvUI.getIUColumnConfig(), this);

		// Vertical buttons
		Composite vButtonBar = (Composite) createAvailableIUsVerticalButtonBar(composite);
		GridData data = new GridData(GridData.FILL_VERTICAL);
		vButtonBar.setLayoutData(data);

		// Must be done after buttons are created so that the buttons can
		// register and receive their selection notifications before us.
		availableIUGroup.getStructuredViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				validateAvailableIUButtons();
			}
		});
		availableIUGroup.setUseBoldFontForFilteredItems(queryContext.getViewType() != AvailableIUViewQueryContext.VIEW_FLAT);
		setDropTarget(availableIUGroup.getStructuredViewer().getControl());

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
		IAction installAction = new InstallAction(availableIUGroup.getStructuredViewer(), profileId, null, ProvPolicies.getDefault(), getShell());
		installButton.setData(BUTTONACTION, installAction);
		availablePropButton = createVerticalButton(composite, ProvSDKMessages.UpdateAndInstallDialog_Properties, false);

		IAction propertiesAction = new PropertyDialogAction(new SameShellProvider(parent.getShell()), availableIUGroup.getStructuredViewer());
		availablePropButton.setData(BUTTONACTION, propertiesAction);

		IAction refreshAction = new RefreshAction(availableIUGroup.getStructuredViewer(), availableIUGroup.getStructuredViewer().getControl()) {
			protected void refresh() {
				availableIUGroup.refresh();
			}
		};
		Button refreshButton = createVerticalButton(composite, refreshAction.getText(), false);
		refreshButton.setData(BUTTONACTION, refreshAction);

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
		Composite parent = availableIUGroup.getComposite().getParent();
		parent.setRedraw(false);
		validateAvailableIUButtons();
		availableIUGroup.setUseBoldFontForFilteredItems(queryContext.getViewType() != AvailableIUViewQueryContext.VIEW_FLAT);
		// This triggers the viewer refresh
		availableIUGroup.setQueryContext(queryContext);

		parent.layout(true);
		parent.setRedraw(true);
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
		manipulateRepoButton.setVisible(!showRepos);
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

		// temporarily disabled.
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=224180
		if (false) {
			Button revert = createVerticalButton(composite, ProvSDKMessages.UpdateAndInstallDialog_RevertActionLabel, false);
			revert.setData(BUTTONACTION, new Action() {
				public void runWithEvent(Event event) {
					RevertWizard wizard = new RevertWizard(profileId, ProvSDKUIActivator.getDefault().getQueryProvider());
					WizardDialog dialog = new WizardDialog(getShell(), wizard);
					dialog.create();
					dialog.getShell().setSize(600, 500);
				}
			});
		}
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
		IDialogSettings settings = ProvSDKUIActivator.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(DIALOG_SETTINGS_SECTION);
		if (section != null) {
			if (tabFolder != null && !tabFolder.isDisposed()) {
				int tab = 0;
				if (section.get(SELECTED_TAB_SETTING) != null)
					tab = section.getInt(SELECTED_TAB_SETTING);
				tabFolder.setSelection(tab);

				int viewType = AvailableIUViewQueryContext.VIEW_BY_CATEGORY;
				if (section.get(AVAILABLE_VIEW_TYPE) != null)
					viewType = section.getInt(AVAILABLE_VIEW_TYPE);
				queryContext = new AvailableIUViewQueryContext(viewType);
			}
		}
		// If we did not find a setting for the query context, use a default
		if (queryContext == null) {
			queryContext = new AvailableIUViewQueryContext(AvailableIUViewQueryContext.VIEW_BY_REPO);
		}
	}

	private void saveDialogSettings() {
		if (!tabFolder.isDisposed()) {
			getDialogBoundsSettings().put(SELECTED_TAB_SETTING, tabFolder.getSelectionIndex());
			getDialogBoundsSettings().put(AVAILABLE_VIEW_TYPE, queryContext.getViewType());
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
}
