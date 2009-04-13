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

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.equinox.internal.p2.ui.sdk.ProvSDKMessages;
import org.eclipse.equinox.internal.p2.ui.sdk.ProvSDKUIActivator;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * @since 3.4
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	public static void migratePreferences() {
		Preferences pref = ProvSDKUIActivator.getPreferences();
		try {
			if (pref.keys().length == 0) {
				// migrate preferences from instance scope to profile scope
				org.eclipse.core.runtime.Preferences oldPref = ProvSDKUIActivator.getDefault().getPluginPreferences();
				// don't migrate everything.  Some of the preferences moved to
				// another bundle.
				pref.put(PreferenceConstants.PREF_OPEN_WIZARD_ON_ERROR_PLAN, oldPref.getString(PreferenceConstants.PREF_OPEN_WIZARD_ON_ERROR_PLAN));
				pref.putBoolean(PreferenceConstants.PREF_SHOW_LATEST_VERSION, oldPref.getBoolean(PreferenceConstants.PREF_SHOW_LATEST_VERSION));
				ProvSDKUIActivator.savePreferences();
			}
		} catch (BackingStoreException e) {
			ProvUI.handleException(e, ProvSDKMessages.PreferenceInitializer_Error, StatusManager.LOG);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		Preferences node = new DefaultScope().getNode("org.eclipse.equinox.p2.ui.sdk"); //$NON-NLS-1$
		// default values
		node.putBoolean(PreferenceConstants.PREF_SHOW_LATEST_VERSION, true);
		node.put(PreferenceConstants.PREF_OPEN_WIZARD_ON_ERROR_PLAN, MessageDialogWithToggle.PROMPT);
	}
}
