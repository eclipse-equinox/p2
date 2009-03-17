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
package org.eclipse.equinox.p2.examples.rcp.sdkbundlevisibility.p2;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
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
		node.putBoolean(PreferenceConstants.PREF_SHOW_LATEST_VERSION, true);
		node.put(PreferenceConstants.PREF_OPEN_WIZARD_ON_ERROR_PLAN, MessageDialogWithToggle.PROMPT);
	}

}
