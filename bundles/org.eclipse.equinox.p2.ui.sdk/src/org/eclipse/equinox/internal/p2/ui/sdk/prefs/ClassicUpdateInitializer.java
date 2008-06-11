/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.prefs;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.equinox.internal.p2.ui.sdk.ProvSDKUIActivator;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.service.prefs.BackingStoreException;

public class ClassicUpdateInitializer extends AbstractPreferenceInitializer {

	private static final String P_ENABLED = "enabled"; //$NON-NLS-1$
	private static final String UPDATE_PLUGIN_ID = "org.eclipse.update.scheduler"; //$NON-NLS-1$

	public void initializeDefaultPreferences() {
		Preferences prefP2 = ProvSDKUIActivator.getDefault().getPluginPreferences();
		//only migrate auto-update preference from UM once
		boolean autoUpdateInit = prefP2.getBoolean(PreferenceConstants.PREF_AUTO_UPDATE_INIT);
		if (!autoUpdateInit) {
			// get UM automatic update preference
			IPreferencesService preferencesService = Platform.getPreferencesService();
			org.osgi.service.prefs.Preferences instanceScope = preferencesService.getRootNode().node(InstanceScope.SCOPE);
			try {
				boolean updateNodeExists = instanceScope.nodeExists(UPDATE_PLUGIN_ID);
				org.osgi.service.prefs.Preferences prefUM = instanceScope.node(UPDATE_PLUGIN_ID);
				boolean enableUpdate = prefUM.getBoolean(P_ENABLED, false);
				// set p2 automatic update preference to match UM preference
				prefP2.setValue(PreferenceConstants.PREF_AUTO_UPDATE_ENABLED, enableUpdate);
				prefP2.setValue(PreferenceConstants.PREF_AUTO_UPDATE_INIT, true);
				ProvSDKUIActivator.getDefault().savePluginPreferences();
				// turn off UM automatic update preference if it exists
				if (updateNodeExists) {
					prefUM.putBoolean(P_ENABLED, false);
					prefUM.flush();
				}
			} catch (BackingStoreException e) {
				ProvUI.handleException(e, "Error saving classic update preferences", StatusManager.LOG); //$NON-NLS-1$
			}
		}
	}
}
