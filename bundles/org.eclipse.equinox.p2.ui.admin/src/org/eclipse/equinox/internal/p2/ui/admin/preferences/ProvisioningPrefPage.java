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
package org.eclipse.equinox.internal.p2.ui.admin.preferences;

import org.eclipse.equinox.internal.p2.ui.admin.ProvAdminUIActivator;
import org.eclipse.equinox.internal.p2.ui.admin.ProvAdminUIMessages;
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
		addField(new BooleanFieldEditor(PreferenceConstants.PREF_SHOW_GROUPS_ONLY, ProvAdminUIMessages.ProvisioningPrefPage_ShowGroupsOnly, getFieldEditorParent()));
		addField(new BooleanFieldEditor(PreferenceConstants.PREF_SHOW_INSTALL_ROOTS_ONLY, ProvAdminUIMessages.ProvisioningPrefPage_ShowInstallRootsOnly, getFieldEditorParent()));
		addField(new BooleanFieldEditor(PreferenceConstants.PREF_HIDE_SYSTEM_REPOS, ProvAdminUIMessages.ProvisioningPrefPage_HideSystemRepos, getFieldEditorParent()));
		addField(new BooleanFieldEditor(PreferenceConstants.PREF_COLLAPSE_IU_VERSIONS, ProvAdminUIMessages.ProvisioningPrefPage_CollapseIUVersions, getFieldEditorParent()));
		addField(new BooleanFieldEditor(PreferenceConstants.PREF_USE_CATEGORIES, ProvAdminUIMessages.ProvisioningPrefPage_UseCategories, getFieldEditorParent()));
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
