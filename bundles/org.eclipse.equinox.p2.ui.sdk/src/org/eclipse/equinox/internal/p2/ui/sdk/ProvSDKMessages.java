/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
	public static String Handler_CannotLaunchUI;
	public static String Handler_SDKUpdateUIMessageTitle;
	public static String InstallNewSoftwareHandler_ProgressTaskName;
	public static String PreferenceInitializer_Error;
	public static String ProvisioningPreferencePage_AlwaysOpenWizard;
	public static String ProvisioningPreferencePage_BrowsingPrefsGroup;
	public static String ProvisioningPreferencePage_ShowLatestVersions;
	public static String ProvisioningPreferencePage_ShowAllVersions;
	public static String ProvisioningPreferencePage_NeverOpenWizard;
	public static String ProvisioningPreferencePage_OpenWizardIfInvalid;
	public static String ProvisioningPreferencePage_PromptToOpenWizard;
	public static String ProvisioningPreferencePage_UninstallUpdateLink;
	public static String ProvisioningPreferencePage_checkCompatibleWithCurrentJRE;
	public static String ProvSDKUIActivator_ErrorSavingPrefs;
	public static String ProvSDKUIActivator_NoSelfProfile;
	public static String ProvSDKUIActivator_OpenWizardAnyway;
	public static String ProvSDKUIActivator_Question;
	public static String SDKPolicy_PrefPageName;
	public static String UpdateHandler_NoSitesMessage;
	public static String UpdateHandler_NoSitesTitle;
	public static String UpdateHandler_ProgressTaskName;
	public static String RemediationOperation_ResolveJobName;
	public static String RemediationOperation_ResolveJobTask;
	public static String TrustPreferencePage_title;
	public static String TrustPreferencePage_export;
	public static String TrustPreferencePage_fileExportTitle;
	public static String TrustPreferencePage_pgpIntro;
	public static String TrustPreferencePage_fileImportTitle;
	public static String TrustPreferencePage_addPGPKeyButtonLabel;
	public static String TrustPreferencePage_Contributor;
	public static String TrustPreferencePage_CopyFingerprint;
	public static String TrustPreferencePage_DataValidExpires;
	public static String TrustPreferencePage_DateExpired;
	public static String TrustPreferencePage_DateExpiredSince;
	public static String TrustPreferencePage_DateNotYetvalid;
	public static String TrustPreferencePage_DateNotYetValid;
	public static String TrustPreferencePage_DateValid;
	public static String TrustPreferencePage_Details;
	public static String TrustPreferencePage_Export;
	public static String TrustPreferencePage_FingerprintIdColumn;
	public static String TrustPreferencePage_NameColumn;
	public static String TrustPreferencePage_PreferenceContributor;
	public static String TrustPreferencePage_removePGPKeyButtonLabel;
	public static String TrustPreferencePage_RevokedPGPKey;
	public static String TrustPreferencePage_TrustAll;
	public static String TrustPreferencePage_TrustAllConfirmationDescription;
	public static String TrustPreferencePage_TrustAllConfirmationTitle;
	public static String TrustPreferencePage_TrustAllNo;
	public static String TrustPreferencePage_TrustAllYes;
	public static String TrustPreferencePage_TypeColumn;
	public static String TrustPreferencePage_ValidityColumn;

}
