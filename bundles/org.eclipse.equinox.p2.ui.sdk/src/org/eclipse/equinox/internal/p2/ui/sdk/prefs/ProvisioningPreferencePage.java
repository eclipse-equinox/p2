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
package org.eclipse.equinox.internal.p2.ui.sdk.prefs;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.equinox.internal.p2.ui.sdk.ProvSDKMessages;
import org.eclipse.equinox.internal.p2.ui.sdk.ProvSDKUIActivator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Preference page for provisioning preferences.
 * 
 * @since 3.4
 */

public class ProvisioningPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private Group browsingGroup, validateGroup;
	// private Button garbageCollectorCheck;
	// private Button deleteImmediatelyRadio, scheduleRadio;
	private Button showLatestRadio, showAllRadio;
	private Button always, never, prompt;

	// private static final int INDENT = 30;

	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		container.setLayout(layout);

		// Group for show all versions vs. show latest
		browsingGroup = new Group(container, SWT.NONE);
		browsingGroup.setText(ProvSDKMessages.ProvisioningPreferencePage_BrowsingPrefsGroup);
		layout = new GridLayout();
		layout.numColumns = 3;
		browsingGroup.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		browsingGroup.setLayoutData(gd);

		showLatestRadio = new Button(browsingGroup, SWT.RADIO);
		showLatestRadio.setText(ProvSDKMessages.ProvisioningPreferencePage_ShowLatestVersions);
		gd = new GridData();
		gd.horizontalSpan = 3;
		showLatestRadio.setLayoutData(gd);

		showAllRadio = new Button(browsingGroup, SWT.RADIO);
		showAllRadio.setText(ProvSDKMessages.ProvisioningPreferencePage_ShowAllVersions);
		gd = new GridData();
		gd.horizontalSpan = 3;
		showAllRadio.setLayoutData(gd);

		//Group for validating a failed plan
		validateGroup = new Group(container, SWT.NONE);
		validateGroup.setText(ProvSDKMessages.ProvisioningPreferencePage_OpenWizardIfInvalid);
		layout = new GridLayout();
		layout.numColumns = 3;
		validateGroup.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		validateGroup.setLayoutData(gd);

		always = new Button(validateGroup, SWT.RADIO);
		always.setText(ProvSDKMessages.ProvisioningPreferencePage_Always);
		gd = new GridData();
		gd.horizontalSpan = 3;
		always.setLayoutData(gd);

		never = new Button(validateGroup, SWT.RADIO);
		never.setText(ProvSDKMessages.ProvisioningPreferencePage_Never);
		gd = new GridData();
		gd.horizontalSpan = 3;
		never.setLayoutData(gd);

		prompt = new Button(validateGroup, SWT.RADIO);
		prompt.setText(ProvSDKMessages.ProvisioningPreferencePage_Prompt);
		gd = new GridData();
		gd.horizontalSpan = 3;
		prompt.setLayoutData(gd);

		/*
		 * Removed until we really implement this
		 *
		gcGroup = new Group(container, SWT.NONE);
		gcGroup.setText(ProvSDKMessages.ProvisioningPreferencePage_gcGroup);
		layout = new GridLayout();
		layout.numColumns = 3;
		gcGroup.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gcGroup.setLayoutData(gd);

		garbageCollectorCheck = new Button(gcGroup, SWT.CHECK);
		garbageCollectorCheck.setText(ProvSDKMessages.ProvisioningPreferencePage_enableGC);
		gd = new GridData();
		gd.horizontalSpan = 3;
		garbageCollectorCheck.setLayoutData(gd);
		garbageCollectorCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});

		deleteImmediatelyRadio = new Button(gcGroup, SWT.RADIO);
		deleteImmediatelyRadio.setText(ProvSDKMessages.ProvisioningPreferencePage_gcImmediately);
		gd = new GridData();
		gd.horizontalSpan = 3;
		gd.horizontalIndent = INDENT;
		deleteImmediatelyRadio.setLayoutData(gd);
		deleteImmediatelyRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});

		scheduleRadio = new Button(gcGroup, SWT.RADIO);
		scheduleRadio.setText(ProvSDKMessages.ProvisioningPreferencePage_gcRetentionTime);
		gd = new GridData();
		gd.horizontalSpan = 3;
		gd.horizontalIndent = INDENT;
		scheduleRadio.setLayoutData(gd);
		scheduleRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});
		*/

		initialize();

		Dialog.applyDialogFont(container);
		return container;

	}

	private void initialize() {
		Preferences pref = ProvSDKUIActivator.getDefault().getPluginPreferences();
		// garbageCollectorCheck.setSelection(pref.getBoolean(PreferenceConstants.PREF_ENABLE_GC));
		// scheduleRadio.setSelection(!pref.getBoolean(PreferenceConstants.PREF_GC_IMMEDIATELY));
		// deleteImmediatelyRadio.setSelection(pref.getBoolean(PreferenceConstants.PREF_GC_IMMEDIATELY));
		showLatestRadio.setSelection(pref.getBoolean(PreferenceConstants.PREF_SHOW_LATEST_VERSION));
		showAllRadio.setSelection(!pref.getBoolean(PreferenceConstants.PREF_SHOW_LATEST_VERSION));
		String openWizard = pref.getString(PreferenceConstants.PREF_OPEN_WIZARD_ON_NONOK_PLAN);
		always.setSelection(openWizard.equals(MessageDialogWithToggle.ALWAYS));
		never.setSelection(openWizard.equals(MessageDialogWithToggle.NEVER));
		prompt.setSelection(openWizard.equals(MessageDialogWithToggle.PROMPT));
	}

	protected void performDefaults() {
		super.performDefaults();
		Preferences pref = ProvSDKUIActivator.getDefault().getPluginPreferences();
		// garbageCollectorCheck.setSelection(pref.getDefaultBoolean(PreferenceConstants.PREF_ENABLE_GC));
		// deleteImmediatelyRadio.setSelection(pref.getDefaultBoolean(PreferenceConstants.PREF_GC_IMMEDIATELY));
		// scheduleRadio.setSelection(!pref.getDefaultBoolean(PreferenceConstants.PREF_GC_IMMEDIATELY));
		showLatestRadio.setSelection(pref.getDefaultBoolean(PreferenceConstants.PREF_SHOW_LATEST_VERSION));
		showAllRadio.setSelection(!pref.getDefaultBoolean(PreferenceConstants.PREF_SHOW_LATEST_VERSION));
		String openWizard = pref.getDefaultString(PreferenceConstants.PREF_OPEN_WIZARD_ON_NONOK_PLAN);
		always.setSelection(openWizard.equals(MessageDialogWithToggle.ALWAYS));
		never.setSelection(openWizard.equals(MessageDialogWithToggle.NEVER));
		prompt.setSelection(openWizard.equals(MessageDialogWithToggle.PROMPT));
	}

	public boolean performOk() {
		Preferences pref = ProvSDKUIActivator.getDefault().getPluginPreferences();
		pref.setValue(PreferenceConstants.PREF_SHOW_LATEST_VERSION, showLatestRadio.getSelection());
		if (always.getSelection())
			pref.setValue(PreferenceConstants.PREF_OPEN_WIZARD_ON_NONOK_PLAN, MessageDialogWithToggle.ALWAYS);
		else if (never.getSelection())
			pref.setValue(PreferenceConstants.PREF_OPEN_WIZARD_ON_NONOK_PLAN, MessageDialogWithToggle.NEVER);
		else
			pref.setValue(PreferenceConstants.PREF_OPEN_WIZARD_ON_NONOK_PLAN, MessageDialogWithToggle.PROMPT);

		// pref.setValue(PreferenceConstants.PREF_GC_IMMEDIATELY, deleteImmediatelyRadio.getSelection());
		// pref.setValue(PreferenceConstants.PREF_ENABLE_GC, garbageCollectorCheck.getSelection());
		ProvSDKUIActivator.getDefault().savePluginPreferences();
		return true;
	}

	/*
	void pageChanged() {
		boolean enabled = garbageCollectorCheck.getSelection();
		scheduleRadio.setEnabled(enabled);
		deleteImmediatelyRadio.setEnabled(enabled);

	}
	*/

	public void init(IWorkbench workbench) {
		// Nothing to do
	}

}
