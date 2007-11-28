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
package org.eclipse.equinox.internal.p2.ui.sdk.prefs;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.equinox.internal.p2.ui.sdk.ProvSDKMessages;
import org.eclipse.equinox.internal.p2.ui.sdk.ProvSDKUIActivator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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

	private Group gcGroup;
	private Button garbageCollectorCheck;
	private Button deleteImmediatelyRadio, scheduleRadio;
	private static final int INDENT = 30;

	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		container.setLayout(layout);

		gcGroup = new Group(container, SWT.NONE);
		gcGroup.setText(ProvSDKMessages.ProvisioningPreferencePage_gcGroup);
		layout = new GridLayout();
		layout.numColumns = 3;
		gcGroup.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
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

		initialize();

		Dialog.applyDialogFont(container);
		return container;

	}

	private void initialize() {
		Preferences pref = ProvSDKUIActivator.getDefault().getPluginPreferences();
		garbageCollectorCheck.setSelection(pref.getBoolean(PreferenceConstants.PREF_ENABLE_GC));
		scheduleRadio.setSelection(!pref.getBoolean(PreferenceConstants.PREF_GC_IMMEDIATELY));
		deleteImmediatelyRadio.setSelection(pref.getBoolean(PreferenceConstants.PREF_GC_IMMEDIATELY));
		pageChanged();
	}

	void pageChanged() {
		boolean enabled = garbageCollectorCheck.getSelection();
		scheduleRadio.setEnabled(enabled);
		deleteImmediatelyRadio.setEnabled(enabled);
	}

	protected void performDefaults() {
		super.performDefaults();
		Preferences pref = ProvSDKUIActivator.getDefault().getPluginPreferences();
		garbageCollectorCheck.setSelection(pref.getDefaultBoolean(PreferenceConstants.PREF_ENABLE_GC));
		pageChanged();
	}

	public boolean performOk() {
		Preferences pref = ProvSDKUIActivator.getDefault().getPluginPreferences();
		pref.setValue(PreferenceConstants.PREF_DOWNLOAD_ONLY, garbageCollectorCheck.getSelection());

		ProvSDKUIActivator.getDefault().savePluginPreferences();
		return true;
	}

	public void init(IWorkbench workbench) {
		// Nothing to do
	}

}