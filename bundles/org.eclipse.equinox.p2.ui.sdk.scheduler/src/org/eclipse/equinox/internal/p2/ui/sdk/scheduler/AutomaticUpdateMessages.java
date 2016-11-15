/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Christian Georgi <christian.georgi@sap.com> - Bug 432887 - Setting to show update wizard w/o notification popup
 *     Mikael Barbero (Eclipse Foundation) - Bug 498116
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui.sdk.scheduler;

import org.eclipse.osgi.util.NLS;

/**
 * Message class for provisioning UI messages.  
 * 
 * @since 3.5
 */
public class AutomaticUpdateMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.ui.sdk.scheduler.messages"; //$NON-NLS-1$
	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, AutomaticUpdateMessages.class);
	}
	public static String Pre_neon2_pref_value_everyday;
	public static String SchedulerStartup_OnceADay;
	public static String SchedulerStartup_OnceAWeek;
	public static String SchedulerStartup_OnceAMonth;
	public static String AutomaticUpdatesPopup_PrefLinkOnly;
	public static String AutomaticUpdatesPopup_RemindAndPrefLink;
	public static String AutomaticUpdatesPopup_ReminderJobTitle;
	public static String AutomaticUpdatesPreferencePage_findUpdates;
	public static String AutomaticUpdateScheduler_30Minutes;
	public static String AutomaticUpdateScheduler_60Minutes;
	public static String AutomaticUpdateScheduler_240Minutes;
	public static String AutomaticUpdateScheduler_UpdateNotInitialized;
	public static String AutomaticUpdatesPopup_UpdatesAvailableTitle;
	public static String AutomaticUpdater_AutomaticDownloadOperationName;
	public static String AutomaticUpdater_ClickToReviewUpdates;
	public static String AutomaticUpdater_ClickToReviewUpdatesWithProblems;
	public static String AutomaticUpdatesPreferencePage_UpdateSchedule;
	public static String AutomaticUpdatesPreferencePage_findOnStart;
	public static String AutomaticUpdatesPreferencePage_findOnSchedule;
	public static String AutomaticUpdatesPreferencePage_directlyShowUpdateWizard;
	public static String AutomaticUpdatesPreferencePage_downloadOptions;
	public static String AutomaticUpdatesPreferencePage_searchAndNotify;
	public static String AutomaticUpdatesPreferencePage_downloadAndNotify;
	public static String AutomaticUpdatesPreferencePage_at;
	public static String AutomaticUpdatesPreferencePage_GenericProductName;
	public static String AutomaticUpdatesPreferencePage_never;
	public static String AutomaticUpdatesPreferencePage_RemindGroup;
	public static String AutomaticUpdatesPreferencePage_RemindSchedule;
	public static String AutomaticUpdatesPreferencePage_RemindOnce;
	public static String AutomaticUpdatesPopup_ClickToReviewDownloaded;
	public static String AutomaticUpdatesPopup_ClickToReviewNotDownloaded;
	public static String ErrorSavingPreferences;
	public static String ErrorSavingClassicPreferences;
	public static String ErrorLoadingPreferenceKeys;
}
