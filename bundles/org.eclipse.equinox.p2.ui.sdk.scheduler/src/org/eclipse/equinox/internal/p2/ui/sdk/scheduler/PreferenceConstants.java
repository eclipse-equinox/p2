/*******************************************************************************
 *  Copyright (c) 2008, 2016 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Johannes Michler <orgler@gmail.com> - Bug 321568 -  [ui] Preference for automatic-update-reminder doesn't work in multilanguage-environments
 *     Christian Georgi <christian.georgi@sap.com> - Bug 432887 - Setting to show update wizard w/o notification popup
 *     Mikael Barbero (Eclipse Foundation) - Bug 498116
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.scheduler;

/**
 * @since 3.5
 */
public class PreferenceConstants {
	public static final String PREF_PAGE_AUTO_UPDATES = "org.eclipse.equinox.internal.p2.ui.sdk.scheduler.AutomaticUpdatesPreferencePage"; //$NON-NLS-1$
	public static final String PREF_AUTO_UPDATE_ENABLED = "enabled"; //$NON-NLS-1$
	public static final String PREF_AUTO_UPDATE_SCHEDULE = "schedule"; //$NON-NLS-1$
	public static final String PREF_UPDATE_ON_STARTUP = "on-startup"; //$NON-NLS-1$
	public static final String PREF_UPDATE_ON_SCHEDULE = "on-schedule"; //$NON-NLS-1$  // string value defined in AutomaticUpdateScheduler 
	public static final String PREF_UPDATE_ON_FUZZY_SCHEDULE = "on-fuzzy-schedule"; //$NON-NLS-1$
	public static final String PREF_DOWNLOAD_ONLY = "download"; // value is true or false, default is false //$NON-NLS-1$
	public static final String PREF_SHOW_UPDATE_WIZARD = "showUpdateWizard"; // value is true or false, default is false //$NON-NLS-1$
	public static final String PREF_REMIND_SCHEDULE = "remindOnSchedule"; // value is true or false //$NON-NLS-1$
	public static final String PREF_REMIND_ELAPSED = "remindElapsedTime";//$NON-NLS-1$
	public static final String PREF_REMIND_30Minutes = "30 minutes";//$NON-NLS-1$
	public static final String PREF_REMIND_60Minutes = "Hour";//$NON-NLS-1$
	public static final String PREF_REMIND_240Minutes = "4 Hours";//$NON-NLS-1$
	public static final String PREF_AUTO_UPDATE_INIT = "autoUpdateInit"; //$NON-NLS-1$
	public static final String PREF_MIGRATED_34 = "migrated34Prefs"; //$NON-NLS-1$
	public static final String PREF_GC_ON_STARTUP = "gcOnStartup"; //$NON-NLS-1$
	public static final String PREF_LAST_AUTO_CHECK_FOR_UPDATES = "lastAutoCheckForUpdates"; //$NON-NLS-1$
}
