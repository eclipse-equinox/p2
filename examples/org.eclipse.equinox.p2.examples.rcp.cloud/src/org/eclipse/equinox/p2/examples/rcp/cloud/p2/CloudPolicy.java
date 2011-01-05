/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.examples.rcp.cloud.p2;

import org.eclipse.equinox.p2.engine.query.UserVisibleRootQuery;
import org.eclipse.equinox.p2.examples.rcp.cloud.Activator;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Point;

/**
 * CloudPolicy defines the RCP Cloud Example policies for the p2 UI. The policy
 * is registered as an OSGi service when the example bundle starts.
 * 
 * @since 3.5
 */
public class CloudPolicy extends Policy {

	public void updateForPreferences() {
		IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
		setRepositoriesVisible(prefs
				.getBoolean(PreferenceConstants.REPOSITORIES_VISIBLE));
		setRestartPolicy(prefs.getInt(PreferenceConstants.RESTART_POLICY));
		setShowLatestVersionsOnly(prefs
				.getBoolean(PreferenceConstants.SHOW_LATEST_VERSION_ONLY));
		setGroupByCategory(prefs
				.getBoolean(PreferenceConstants.AVAILABLE_GROUP_BY_CATEGORY));
		setShowDrilldownRequirements(prefs
				.getBoolean(PreferenceConstants.SHOW_DRILLDOWN_REQUIREMENTS));
		setFilterOnEnv(prefs.getBoolean(PreferenceConstants.FILTER_ON_ENV));
		setUpdateWizardStyle(prefs.getInt(PreferenceConstants.UPDATE_WIZARD_STYLE));
		int preferredWidth = prefs.getInt(PreferenceConstants.UPDATE_DETAILS_WIDTH);
		int preferredHeight = prefs.getInt(PreferenceConstants.UPDATE_DETAILS_HEIGHT);
		setUpdateDetailsPreferredSize(new Point(preferredWidth, preferredHeight));		
		
		if (prefs.getBoolean(PreferenceConstants.AVAILABLE_SHOW_ALL_BUNDLES))
			setVisibleAvailableIUQuery(QueryUtil.ALL_UNITS);
		else
			setVisibleAvailableIUQuery(QueryUtil.createIUGroupQuery());
		if (prefs.getBoolean(PreferenceConstants.INSTALLED_SHOW_ALL_BUNDLES))
			setVisibleAvailableIUQuery(QueryUtil.ALL_UNITS);
		else
			setVisibleAvailableIUQuery(new UserVisibleRootQuery());

	}
}
