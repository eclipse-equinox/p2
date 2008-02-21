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

import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.IMutableActivityManager;
import org.eclipse.ui.activities.IWorkbenchActivitySupport;

public class ClassicUpdateInitializer extends AbstractPreferenceInitializer {

	private static final String ACTIVITY_ID = "org.eclipse.equinox.p2.ui.sdk.classicUpdate"; //$NON-NLS-1$

	public void initializeDefaultPreferences() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench == null) {
			return;
		}
		final IWorkbenchActivitySupport activitySupport = workbench.getActivitySupport();
		IMutableActivityManager createWorkingCopy = activitySupport.createWorkingCopy();
		Set activityIds = createWorkingCopy.getEnabledActivityIds();
		final Set enabledActivityIds = new HashSet(activityIds);
		enabledActivityIds.remove(ACTIVITY_ID);
		workbench.getDisplay().asyncExec(new Runnable() {
			public void run() {
				//activity change listeners may touch widgets
				activitySupport.setEnabledActivityIds(enabledActivityIds);
			}
		});
	}
}
