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
package org.eclipse.equinox.internal.p2.ui.sdk.updates;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.equinox.internal.p2.ui.sdk.ProvSDKUIActivator;
import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceConstants;
import org.eclipse.equinox.p2.updatechecker.IUpdateListener;
import org.eclipse.equinox.p2.updatechecker.UpdateEvent;

/**
 * @since 3.4
 */
public class AutomaticUpdater implements IUpdateListener {

	Preferences prefs;

	public AutomaticUpdater() {
		prefs = ProvSDKUIActivator.getDefault().getPluginPreferences();

	}

	public void updatesAvailable(UpdateEvent event) {
		boolean download = prefs.getBoolean(PreferenceConstants.P_DOWNLOAD);
		if (download) {
			// TODO not implemented
		}
	}

}
