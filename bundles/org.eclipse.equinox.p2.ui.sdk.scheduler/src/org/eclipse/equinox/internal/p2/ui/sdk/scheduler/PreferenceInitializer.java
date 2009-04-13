/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.scheduler;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	private static final String P_ENABLED = "enabled"; //$NON-NLS-1$
	private static final String UPDATE_PLUGIN_ID = "org.eclipse.update.scheduler"; //$NON-NLS-1$

	public static void migratePreferences() {
		// Migrate preference values that were stored in alternate locations.
		// 1) migrate from instance scope (during 3.5 development) to profile scope (final 3.5 format)
		// 2) if applicable, migrate from 3.4 prefs kept in a different bundle
		// 3) if applicable, migrate from 3.3 prefs known by Update Manager
		Preferences pref = AutomaticUpdatePlugin.getPreferences();
		try {
			if (pref.keys().length == 0) {
				// migrate preferences from instance scope to profile scope
				org.eclipse.core.runtime.Preferences oldPref = AutomaticUpdatePlugin
						.getDefault().getPluginPreferences();
				String[] keys = oldPref.propertyNames();
				for (int i = 0; i < keys.length; i++)
					pref.put(keys[i], oldPref.getString(keys[i]));

				if (keys.length > 0)
					AutomaticUpdatePlugin.savePreferences();
			}
		} catch (BackingStoreException e) {
			ProvUI.handleException(e,
					AutomaticUpdateMessages.ErrorLoadingPreferenceKeys,
					StatusManager.LOG);
		}

		// Have we initialized the auto update prefs from previous
		// releases?  
		boolean autoUpdateInit = pref.getBoolean(
				PreferenceConstants.PREF_AUTO_UPDATE_INIT, false);
		if (!autoUpdateInit) {
			// first look for the 3.4 automatic update preferences, which were
			// located in a different bundle than now, in the instance scope.
			Preferences node34 = Platform.getPreferencesService().getRootNode()
					.node(InstanceScope.SCOPE).node(
							"org.eclipse.equinox.p2.ui.sdk"); //$NON-NLS-1$
			if (node34 != null) {
				// We only migrate the preferences associated with auto update.
				// Other preferences still remain in that bundle and are handled
				// there.
				pref.putBoolean(PreferenceConstants.PREF_AUTO_UPDATE_ENABLED,
						node34.getBoolean("enabled", false));
				pref.put(PreferenceConstants.PREF_AUTO_UPDATE_SCHEDULE, node34
						.get("schedule",
								PreferenceConstants.PREF_UPDATE_ON_STARTUP));
				pref.putBoolean(PreferenceConstants.PREF_DOWNLOAD_ONLY, node34
						.getBoolean("download", false));
				pref.putBoolean(PreferenceConstants.PREF_REMIND_SCHEDULE,
						node34.getBoolean("remindOnSchedule", false)); //$NON-NLS-1$
				pref
						.put(
								PreferenceConstants.PREF_REMIND_ELAPSED,
								node34
										.get(
												"remindElapsedTime",
												AutomaticUpdateMessages.AutomaticUpdateScheduler_30Minutes));
				// mark the pref that says we've migrated
				pref
						.putBoolean(PreferenceConstants.PREF_AUTO_UPDATE_INIT,
								true);
				AutomaticUpdatePlugin.savePreferences();
			}
			// Look for the 3.3 UM automatic update preferences. We will
			// not migrate them if we already pulled values from 3.4.
			// However, we always want to turn off the UM automatic update
			// checker if it is found to be on.
			Preferences instanceScope = Platform.getPreferencesService()
					.getRootNode().node(InstanceScope.SCOPE);
			try {
				boolean updateNodeExists = instanceScope
						.nodeExists(UPDATE_PLUGIN_ID);
				Preferences prefUM = instanceScope.node(UPDATE_PLUGIN_ID);
				boolean enableUpdate = prefUM.getBoolean(P_ENABLED, false);
				// set p2 automatic update preference to match UM preference,
				// only if we haven't already used 3.4 values
				if (node34 == null) {
					pref.putBoolean(
							PreferenceConstants.PREF_AUTO_UPDATE_ENABLED,
							enableUpdate);
					// mark the pref that says we migrated
					pref.putBoolean(PreferenceConstants.PREF_AUTO_UPDATE_INIT,
							true);
					AutomaticUpdatePlugin.savePreferences();
				}
				// turn off UM automatic update preference if it exists
				if (updateNodeExists) {
					prefUM.putBoolean(P_ENABLED, false);
					prefUM.flush();
				}
			} catch (BackingStoreException e) {
				ProvUI.handleException(e,
						AutomaticUpdateMessages.ErrorSavingClassicPreferences,
						StatusManager.LOG);
			}
		}
	}
	public void initializeDefaultPreferences() {
		// initialize the default scope
		Preferences node = new DefaultScope().getNode(AutomaticUpdatePlugin.PLUGIN_ID);
		node.putBoolean(PreferenceConstants.PREF_AUTO_UPDATE_ENABLED, false);
		node.put(PreferenceConstants.PREF_AUTO_UPDATE_SCHEDULE, PreferenceConstants.PREF_UPDATE_ON_STARTUP);
		node.putBoolean(PreferenceConstants.PREF_DOWNLOAD_ONLY, false);
		node.putBoolean(PreferenceConstants.PREF_REMIND_SCHEDULE, false);
		node.put(PreferenceConstants.PREF_REMIND_ELAPSED, AutomaticUpdateMessages.AutomaticUpdateScheduler_30Minutes);
	}
}
