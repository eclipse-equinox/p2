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
package org.eclipse.equinox.prov.ui.admin.internal.preferences;

import org.eclipse.equinox.prov.ui.admin.ProvAdminUIActivator;
import org.eclipse.equinox.prov.ui.admin.internal.ProvAdminUIMessages;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Preference page for provisioning preferences.
 * 
 * @since 3.4
 */

public class ProvisioningPrefPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public ProvisioningPrefPage() {
		super(GRID);
		setPreferenceStore(ProvAdminUIActivator.getDefault().getPreferenceStore());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
	 */
	public void createFieldEditors() {
		addField(new BooleanFieldEditor(PreferenceConstants.PREF_CONFIRM_SELECTION_INSTALL, ProvAdminUIMessages.ProvisioningPrefPage_ConfirmSelectionInstallOps, getFieldEditorParent()));
		addField(new BooleanFieldEditor(PreferenceConstants.PREF_SHOW_GROUPS_ONLY, ProvAdminUIMessages.ProvisioningPrefPage_ShowGroupsOnly, getFieldEditorParent()));
		addField(new BooleanFieldEditor(PreferenceConstants.PREF_HIDE_IMPLEMENTATION_REPOS, ProvAdminUIMessages.ProvisioningPrefPage_HideInternalRepos, getFieldEditorParent()));
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