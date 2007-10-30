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

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceConstants;
import org.eclipse.equinox.p2.engine.Profile;
import org.eclipse.equinox.p2.ui.IRepositoryManipulator;
import org.eclipse.equinox.p2.ui.dialogs.UpdateAndInstallGroup;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
	private Profile profile;
	private UpdateAndInstallGroup group;

	/**
	 * Create an instance of this Dialog.
	 * 
	 */
	public UpdateAndInstallDialog(Shell shell, Profile profile) {
		super(shell);
		this.profile = profile;
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

		gc.setFont(JFaceResources.getDialogFont());
		FontMetrics fontMetrics = gc.getFontMetrics();
		gc.dispose();

		group = new UpdateAndInstallGroup(comp, profile, new ViewerFilter[] {ProvSDKPolicies.getInstalledIUFilter()}, new ViewerFilter[] {ProvSDKPolicies.getAvailableIUFilter()}, ProvSDKMessages.UpdateAndInstallDialog_InstalledFeatures, ProvSDKMessages.UpdateAndInstallDialog_AvailableFeatures, getRepositoryManipulator(), null, fontMetrics);
		final Button checkBox = new Button(comp, SWT.CHECK);
		final Preferences pref = ProvSDKUIActivator.getDefault().getPluginPreferences();
		checkBox.setText(ProvSDKMessages.UpdateAndInstallDialog_AlertCheckbox);
		checkBox.setSelection(pref.getBoolean(PreferenceConstants.P_ENABLED));
		checkBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pref.setValue(PreferenceConstants.P_ENABLED, checkBox.getSelection());
			}
		});

		pref.addPropertyChangeListener(new IPropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().equals(PreferenceConstants.P_ENABLED))
					checkBox.setSelection(pref.getBoolean(PreferenceConstants.P_ENABLED));
			}

		});

		Link updatePrefsLink = new Link(comp, SWT.LEFT | SWT.WRAP);
		GridData gd = new GridData();
		gd.horizontalIndent = convertHorizontalDLUsToPixels(IDialogConstants.SMALL_INDENT);
		updatePrefsLink.setLayoutData(gd);
		updatePrefsLink.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {

				PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(getShell(), PreferenceConstants.AUTO_UPDATES_PAGE, null, null);
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

	protected void okPressed() {
		saveDialogSettings();
		super.okPressed();
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
}
