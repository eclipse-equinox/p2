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

import org.eclipse.equinox.internal.p2.ui.sdk.ProvSDKMessages;
import org.eclipse.equinox.internal.p2.ui.sdk.ProvSDKUIActivator;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Preference page for provisioning preferences.
 * 
 * @since 3.4
 */

public class ExternalFilesPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	public ExternalFilesPreferencePage() {
		super(GRID);
		setPreferenceStore(ProvSDKUIActivator.getDefault().getPreferenceStore());
	}

	protected void createFieldEditors() {
		addField(new RadioGroupFieldEditor(PreferenceConstants.PREF_GENERATE_ARCHIVEREPOFOLDER, ProvSDKMessages.ExternalFilesPreferencePage_ArchiveRepoGenerationGroup, 1, new String[][] { {ProvSDKMessages.ExternalFilesPreferencePage_GenerateArchiveRepoAlways, MessageDialogWithToggle.ALWAYS}, {ProvSDKMessages.ExternalFilesPreferencePage_GenerateArchiveRepoNever, MessageDialogWithToggle.NEVER}, {ProvSDKMessages.ExternalFilesPreferencePage_GenerateArchiveRepoPrompt, MessageDialogWithToggle.PROMPT}}, getFieldEditorParent(), true));
		addField(new RadioGroupFieldEditor(PreferenceConstants.PREF_AUTO_INSTALL_BUNDLES, ProvSDKMessages.ExternalFilesPreferencePage_AddBundleGroup, 1, new String[][] { {ProvSDKMessages.ExternalFilesPreferencePage_AutoInstallBundleAlways, MessageDialogWithToggle.ALWAYS}, {ProvSDKMessages.ExternalFilesPreferencePage_AutoInstallBundleNever, MessageDialogWithToggle.NEVER}, {ProvSDKMessages.ExternalFilesPreferencePage_AutoInstallBundlePrompt, MessageDialogWithToggle.PROMPT}}, getFieldEditorParent(), true));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
		// nothing to do

	}
}
