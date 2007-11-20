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

import org.eclipse.equinox.internal.p2.ui.sdk.ProvSDKMessages;
import org.eclipse.equinox.internal.p2.ui.sdk.ProvSDKUIActivator;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Preference page for provisioning preferences.
 * 
 * @since 3.4
 */

public class ProvisioningPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public ProvisioningPreferencePage() {
		super(GRID);
		setPreferenceStore(ProvSDKUIActivator.getDefault().getPreferenceStore());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
	 */
	public void createFieldEditors() {
		addField(new BooleanFieldEditor(PreferenceConstants.PREF_SHOW_LATEST_VERSION, ProvSDKMessages.ProvisioningPreferencePage_ShowLatestVersions, getFieldEditorParent()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
		// nothing to do
	}

}