/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
	public static String AddColocatedRepositoryAction_Label;
	public static String AddColocatedRepositoryAction_ToolTip;
	public static String AddColocatedRepositoryDialog_Title;
	public static String ExternalFileHandler_BundleInstalledOK;
	public static String ExternalFileHandler_ErrorCopyingFile;
	public static String ExternalFileHandler_ErrorExpandingArchive;
	public static String ExternalFileHandler_ErrorLoadingFromZipDirectory;
	public static String ExternalFileHandler_PromptForInstallBundle;
	public static String ExternalFileHandler_RepositoryGeneratedOK;
	public static String ExternalFileHandler_PromptForUnzip;
	public static String ExternalFilesPreferencePage_AddBundleGroup;
	public static String ExternalFilesPreferencePage_ArchiveRepoGenerationGroup;
	public static String ExternalFilesPreferencePage_AutoInstallBundleAlways;
	public static String ExternalFilesPreferencePage_AutoInstallBundleNever;
	public static String ExternalFilesPreferencePage_AutoInstallBundlePrompt;
	public static String ExternalFilesPreferencePage_GenerateArchiveRepoAlways;
	public static String ExternalFilesPreferencePage_GenerateArchiveRepoNever;
	public static String ExternalFilesPreferencePage_GenerateArchiveRepoPrompt;
	public static String RemoveColocatedRepositoryAction_Label;
	public static String RemoveColocatedRepositoryAction_Tooltip;
	public static String RepositoryManipulationDialog_AddButton;
	public static String RepositoryManipulationDialog_LocationColumnHeader;
	public static String RepositoryManipulationDialog_NameColumnHeader;
	public static String RepositoryManipulationDialog_PropertiesButton;
	public static String RepositoryManipulationDialog_RemoveButton;
	public static String RepositoryManipulationDialog_RemoveOperationLabel;
	public static String RepositoryManipulationDialog_UpdateSitesDialogTitle;
	public static String UpdateAndInstallDialog_AddSiteButtonText;
	public static String UpdateAndInstallDialog_AddSiteOperationlabel;
	public static String UpdateAndInstallDialog_AvailableSoftware;
	public static String UpdateAndInstallDialog_InstalledSoftware;
	public static String UpdateAndInstallDialog_ManageSites;
	public static String UpdateAndInstallDialog_PrefLink;
	public static String UpdateAndInstallDialog_RemoveSiteButtonText;
	public static String UpdateAndInstallDialog_RemoveSiteOperationLabel;
	public static String UpdateAndInstallDialog_RevertActionLabel;
	public static String UpdateAndInstallDialog_Title;
	public static String UpdateAndInstallDialog_Properties;
	public static String UpdateAndInstallDialog_Refresh;
	public static String UpdateAndInstallDialog_ViewByCategory;
	public static String UpdateAndInstallDialog_ViewByName;
	public static String UpdateAndInstallDialog_ViewBySite;
	public static String UpdateHandler_CannotLaunchUI;
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
	public static String AutomaticUpdater_ErrorCheckingUpdates;
	public static String AutomaticUpdatesPreferencePage_UpdateSchedule;
	public static String AutomaticUpdatesPreferencePage_findOnStart;
	public static String AutomaticUpdatesPreferencePage_findOnSchedule;
	public static String AutomaticUpdatesPreferencePage_downloadOptions;
	public static String AutomaticUpdatesPreferencePage_searchAndNotify;
	public static String AutomaticUpdatesPreferencePage_downloadAndNotify;
	public static String AutomaticUpdatesPreferencePage_at;
	public static String AutomaticUpdatesPreferencePage_RemindGroup;
	public static String AutomaticUpdatesPreferencePage_RemindSchedule;
	public static String AutomaticUpdatesPreferencePage_RemindOnce;
	public static String AutomaticUpdatesPopup_ClickToReviewDownloaded;
	public static String AutomaticUpdatesPopup_ClickToReviewNotDownloaded;
	public static String ProvisioningPreferencePage_AlwaysOpenWizard;
	public static String ProvisioningPreferencePage_BrowsingPrefsGroup;
	public static String ProvisioningPreferencePage_ShowLatestVersions;
	public static String ProvisioningPreferencePage_ShowAllVersions;
	public static String ProvisioningPreferencePage_NeverOpenWizard;
	public static String ProvisioningPreferencePage_OpenWizardIfInvalid;
	public static String ProvisioningPreferencePage_PromptToOpenWizard;
	public static String ProvSDKQueryProvider_ErrorRetrievingProfile;
	public static String ProvSDKUIActivator_ErrorWritingLicenseRegistry;
	public static String ProvSDKUIActivator_LaunchUpdateManager;
	public static String ProvSDKUIActivator_LicenseManagerReadError;
	public static String ProvSDKUIActivator_NoSelfProfile;
	public static String ProvSDKUIActivator_OpenWizardAnyway;
	public static String ProvSDKUIActivator_Question;
	public static String ProvSDKUIActivator_UnsupportedFeatureMessage;
	public static String ProvSDKUIActivator_UnsupportedFeatureTitle;
}
