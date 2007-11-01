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

package org.eclipse.equinox.internal.p2.ui.sdk;

import org.eclipse.osgi.util.NLS;

/**
 * Message class for provisioning UI messages.  
 * 
 * @since 3.4
 */
public class ProvSDKMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.ui.sdk.messages"; //$NON-NLS-1$
	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, ProvSDKMessages.class);
	}
	public static String RepositoryManipulationDialog_UpdateSitesDialogTitle;
	public static String UpdateAndInstallDialog_AvailableFeatures;
	public static String UpdateAndInstallDialog_InstalledFeatures;
	public static String UpdateAndInstallDialog_ManageSites;
	public static String UpdateAndInstallDialog_AlertCheckbox;
	public static String UpdateAndInstallDialog_PrefLink;
	public static String UpdateAndInstallDialog_Title;
	public static String UpdateHandler_NoProfilesDefined;
	public static String UpdateHandler_NoProfileInstanceDefined;
	public static String UpdateHandler_SDKUpdateUIMessageTitle;
	public static String SchedulerStartup_day;
	public static String SchedulerStartup_Monday;
	public static String SchedulerStartup_Tuesday;
	public static String SchedulerStartup_Wednesday;
	public static String SchedulerStartup_Thursday;
	public static String SchedulerStartup_Friday;
	public static String SchedulerStartup_Saturday;
	public static String SchedulerStartup_Sunday;
	public static String SchedulerStartup_1AM;
	public static String SchedulerStartup_2AM;
	public static String SchedulerStartup_3AM;
	public static String SchedulerStartup_4AM;
	public static String SchedulerStartup_5AM;
	public static String SchedulerStartup_6AM;
	public static String SchedulerStartup_7AM;
	public static String SchedulerStartup_8AM;
	public static String SchedulerStartup_9AM;
	public static String SchedulerStartup_10AM;
	public static String SchedulerStartup_11AM;
	public static String SchedulerStartup_12PM;
	public static String SchedulerStartup_1PM;
	public static String SchedulerStartup_2PM;
	public static String SchedulerStartup_3PM;
	public static String SchedulerStartup_4PM;
	public static String SchedulerStartup_5PM;
	public static String SchedulerStartup_6PM;
	public static String SchedulerStartup_7PM;
	public static String SchedulerStartup_8PM;
	public static String SchedulerStartup_9PM;
	public static String SchedulerStartup_10PM;
	public static String SchedulerStartup_11PM;
	public static String SchedulerStartup_12AM;
	public static String AutomaticUpdatesJob_AutomaticUpdateSearch;
	public static String AutomaticUpdatesPreferencePage_findUpdates;
	public static String AutomaticUpdateScheduler_UpdateNotInitialized;
	public static String AutomaticUpdatesDialog_DownloadedNotification;
	public static String AutomaticUpdatesDialog_UpdatesAvailableTitle;
	public static String AutomaticUpdater_AutomaticDownloadOperationName;
	public static String AutomaticUpdatesDialog_UpdatesFoundNotification;
	public static String AutomaticUpdatesPreferencePage_UpdateSchedule;
	public static String AutomaticUpdatesPreferencePage_findOnStart;
	public static String AutomaticUpdatesPreferencePage_findOnSchedule;
	public static String AutomaticUpdatesPreferencePage_downloadOptions;
	public static String AutomaticUpdatesPreferencePage_searchAndNotify;
	public static String AutomaticUpdatesPreferencePage_downloadAndNotify;
	public static String AutomaticUpdatesPreferencePage_at;
	public static String AutomaticUpdatesJob_Updates;
	public static String AutomaticUpdatesDialog_ClickToReviewDownloaded;
	public static String AutomaticUpdatesDialog_ClickToReviewNotDownloaded;
	public static String ProvSDKUIActivator_NoSelfProfile;
}
