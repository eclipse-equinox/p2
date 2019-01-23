/*******************************************************************************
 *  Copyright (c) 2010, 2019 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.examples.rcp.cloud.p2;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.equinox.p2.examples.rcp.cloud.Activator;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.swt.SWT;
import org.osgi.service.prefs.Preferences;

/**
 * @since 3.6
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	public void initializeDefaultPreferences() {
		Preferences node = DefaultScope.INSTANCE.getNode(Activator.PLUGIN_ID); //$NON-NLS-1$
		// default values
		node.putBoolean(PreferenceConstants.REPOSITORIES_VISIBLE, false);
		node.putBoolean(PreferenceConstants.SHOW_LATEST_VERSION_ONLY, true);
		node.putBoolean(PreferenceConstants.AVAILABLE_SHOW_ALL_BUNDLES, false);
		node.putBoolean(PreferenceConstants.INSTALLED_SHOW_ALL_BUNDLES, false);
		node.putBoolean(PreferenceConstants.AVAILABLE_GROUP_BY_CATEGORY, true);
		node.putBoolean(PreferenceConstants.SHOW_DRILLDOWN_REQUIREMENTS, false);
		node.putInt(PreferenceConstants.RESTART_POLICY, Policy.RESTART_POLICY_PROMPT_RESTART_OR_APPLY);
		node.putInt(PreferenceConstants.UPDATE_WIZARD_STYLE, Policy.UPDATE_STYLE_MULTIPLE_IUS);
		node.putBoolean(PreferenceConstants.FILTER_ON_ENV, false);
		node.putInt(PreferenceConstants.UPDATE_DETAILS_HEIGHT, SWT.DEFAULT);
		node.putInt(PreferenceConstants.UPDATE_DETAILS_WIDTH, SWT.DEFAULT);
	}
}
