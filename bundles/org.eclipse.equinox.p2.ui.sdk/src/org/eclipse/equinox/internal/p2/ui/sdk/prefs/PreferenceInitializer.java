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
import org.osgi.service.prefs.Preferences;

/**
 * @since 3.4
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		Preferences node = new DefaultScope().getNode("org.eclipse.equinox.p2.ui.sdk"); //$NON-NLS-1$
		node.putBoolean(PreferenceConstants.PREF_AUTO_UPDATE_ENABLED, false);
		node.put(PreferenceConstants.PREF_AUTO_UPDATE_SCHEDULE, PreferenceConstants.PREF_UPDATE_ON_STARTUP);
		node.putBoolean(PreferenceConstants.PREF_DOWNLOAD_ONLY, false);
		node.putBoolean(PreferenceConstants.PREF_REMIND_SCHEDULE, false);
		node.putBoolean(PreferenceConstants.PREF_SHOW_LATEST_VERSION, true);
		node.putBoolean(PreferenceConstants.PREF_ENABLE_GC, false);
		node.putBoolean(PreferenceConstants.PREF_GC_IMMEDIATELY, true);
		node.put(PreferenceConstants.PREF_REMIND_ELAPSED, ProvSDKMessages.AutomaticUpdateScheduler_5Minutes);
	}

}
