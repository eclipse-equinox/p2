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

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.equinox.internal.p2.ui.admin.ProvAdminUIActivator;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Initializes the preferences for the provisioning UI.
 * @since 3.4
 *
 */
public class ProvUIPreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = ProvAdminUIActivator.getDefault().getPreferenceStore();
		store.setDefault(PreferenceConstants.PREF_SHOW_GROUPS_ONLY, true);
		store.setDefault(PreferenceConstants.PREF_SHOW_INSTALL_ROOTS_ONLY, true);
		store.setDefault(PreferenceConstants.PREF_HIDE_SYSTEM_REPOS, true);
		store.setDefault(PreferenceConstants.PREF_COLLAPSE_IU_VERSIONS, true);
		store.setDefault(PreferenceConstants.PREF_USE_CATEGORIES, false);
	}

}
