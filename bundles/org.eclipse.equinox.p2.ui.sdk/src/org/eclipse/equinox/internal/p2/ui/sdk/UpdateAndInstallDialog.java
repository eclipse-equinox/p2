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
package org.eclipse.equinox.internal.p2.ui.sdk;

import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceConstants;
import org.eclipse.equinox.p2.ui.IProfileChooser;
import org.eclipse.equinox.p2.ui.IRepositoryManipulator;
import org.eclipse.equinox.p2.ui.dialogs.RevertWizard;
import org.eclipse.equinox.p2.ui.dialogs.UpdateAndInstallGroup;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.FontMetrics;
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
public class UpdateAndInstallDialog extends TrayDialog {

	private static final String DIALOG_SETTINGS_SECTION = "UpdateAndInstallDialog"; //$NON-NLS-1$
	private static final String SELECTED_TAB_SETTING = "SelectedTab"; //$NON-NLS-1$
	String profileId;
	UpdateAndInstallGroup group;

	/**
	 * Create an instance of this Dialog.
	 * 
	 */
	public UpdateAndInstallDialog(Shell shell, String profileId) {
		super(shell);
		this.profileId = profileId;
		setShellStyle(SWT.DIALOG_TRIM | SWT.MODELESS | SWT.MAX | SWT.RESIZE | getDefaultOrientation());
		setBlockOnOpen(false);
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
		FontMetrics fontMetrics = gc.getFontMetrics();
		gc.dispose();

		group = new UpdateAndInstallGroup(comp, profileId, ProvSDKMessages.UpdateAndInstallDialog_InstalledFeatures, ProvSDKMessages.UpdateAndInstallDialog_AvailableFeatures, getRepositoryManipulator(), getProfileChooser(), ProvSDKUIActivator.getDefault().getQueryProvider(), ProvSDKUIActivator.getDefault().getLicenseManager(), fontMetrics);
		final Button checkBox = new Button(comp, SWT.CHECK);
		final IPreferenceStore store = ProvSDKUIActivator.getDefault().getPreferenceStore();
		checkBox.setText(ProvSDKMessages.UpdateAndInstallDialog_AlertCheckbox);
		checkBox.setSelection(store.getBoolean(PreferenceConstants.PREF_AUTO_UPDATE_ENABLED));
		checkBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				store.setValue(PreferenceConstants.PREF_AUTO_UPDATE_ENABLED, checkBox.getSelection());
			}
		});

		final IPropertyChangeListener preferenceListener = new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().equals(PreferenceConstants.PREF_AUTO_UPDATE_ENABLED))
					checkBox.setSelection(store.getBoolean(PreferenceConstants.PREF_AUTO_UPDATE_ENABLED));
				if (event.getProperty().equals(PreferenceConstants.PREF_SHOW_LATEST_VERSION))
					group.getAvailableIUViewer().refresh();
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
		readDialogSettings();
		Dialog.applyDialogFont(comp);
		return comp;
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
		return new IRepositoryManipulator() {
			public String getLabel() {
				return ProvSDKMessages.UpdateAndInstallDialog_ManageSites;

			}

			public boolean manipulateRepositories(Shell shell) {
				new RepositoryManipulationDialog(shell).open();
				return true;
			}

		};
	}

	private IProfileChooser getProfileChooser() {
		return new IProfileChooser() {
			public String getLabel() {
				return ProvSDKMessages.UpdateAndInstallDialog_RevertActionLabel;

			}

			public String getProfileId(Shell shell) {
				RevertWizard wizard = new RevertWizard(profileId, ProvSDKUIActivator.getDefault().getQueryProvider());
				WizardDialog dialog = new WizardDialog(shell, wizard);
				dialog.create();
				dialog.getShell().setSize(600, 500);
				if (dialog.open() == Window.OK)
					return profileId;
				return null;
			}
		};
	}

	private void readDialogSettings() {
		IDialogSettings settings = ProvSDKUIActivator.getDefault().getDialogSettings();
		IDialogSettings section = settings.getSection(DIALOG_SETTINGS_SECTION);
		if (section != null) {
			if (group != null && !group.getTabFolder().isDisposed()) {
				int tab = 0;
				if (section.get(SELECTED_TAB_SETTING) != null)
					tab = section.getInt(SELECTED_TAB_SETTING);
				group.getTabFolder().setSelection(tab);
			}
		}
	}

	private void saveDialogSettings() {
		if (!group.getTabFolder().isDisposed()) {
			getDialogBoundsSettings().put(SELECTED_TAB_SETTING, group.getTabFolder().getSelectionIndex());
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

}
