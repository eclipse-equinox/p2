/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
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
 *     Red Hat, Inc - support for remediation page
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.ui;

import org.eclipse.osgi.util.NLS;

/**
 * Message class for provisioning UI messages.
 *
 * @since 3.4
 */
public class ProvUIMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.ui.messages"; //$NON-NLS-1$
	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, ProvUIMessages.class);
	}

	public static String AcceptLicensesWizardPage_AcceptMultiple;
	public static String AcceptLicensesWizardPage_AcceptSingle;
	public static String AcceptLicensesWizardPage_ItemsLabel;
	public static String AcceptLicensesWizardPage_LicenseTextLabel;
	public static String AcceptLicensesWizardPage_NoLicensesDescription;
	public static String AcceptLicensesWizardPage_RejectMultiple;
	public static String AcceptLicensesWizardPage_RejectSingle;
	public static String AcceptLicensesWizardPage_ReviewExtraLicensesDescription;
	public static String AcceptLicensesWizardPage_ReviewLicensesDescription;
	public static String AcceptLicensesWizardPage_SingleLicenseTextLabel;
	public static String AcceptLicensesWizardPage_Title;
	public static String ApplicationInRestartDialog;
	public static String ApplyProfileChangesDialog_ApplyChanges;
	public static String ApplyProfileChangesDialog_Restart;
	public static String ApplyProfileChangesDialog_NotYet;
	public static String ColocatedRepositoryManipulator_AddSiteOperationLabel;
	public static String ColocatedRepositoryTracker_PromptForSiteLocationEdit;
	public static String ColocatedRepositoryTracker_SiteNotFoundTitle;
	public static String ColocatedRepositoryTracker_SiteNotFound_EditButtonLabel;
	public static String RevertProfilePage_ConfirmDeleteMultipleConfigs;
	public static String RevertProfilePage_ConfirmDeleteSingleConfig;
	public static String RevertProfilePage_Delete;
	public static String RevertProfilePage_CancelButtonLabel;
	public static String RevertProfilePage_DeleteMultipleConfigurationsTitle;
	public static String RevertProfilePage_DeleteSingleConfigurationTitle;
	public static String RevertProfilePage_DeleteTooltip;
	public static String RevertProfilePage_NoProfile;
	public static String RevertProfilePage_RevertLabel;
	public static String RevertProfilePage_RevertTooltip;
	public static String RevertProfilePage_CompareLabel;
	public static String RevertProfilePage_CompareTooltip;
	public static String RevertProfilePage_ProfileTagColumn;
	public static String RevertProfilePage_ProfileTimestampColumn;
	public static String IUCopyrightPropertyPage_NoCopyright;
	public static String IUCopyrightPropertyPage_ViewLinkLabel;
	public static String IUDetailsLabelProvider_KB;
	public static String IUDetailsLabelProvider_Bytes;
	public static String IUDetailsLabelProvider_ComputingSize;
	public static String IUDetailsLabelProvider_Unknown;
	public static String IUGeneralInfoPropertyPage_ContactLabel;
	public static String IUGeneralInfoPropertyPage_CouldNotOpenBrowser;
	public static String IUGeneralInfoPropertyPage_DescriptionLabel;
	public static String IUGeneralInfoPropertyPage_DocumentationLink;
	public static String IUGeneralInfoPropertyPage_IdentifierLabel;
	public static String IUGeneralInfoPropertyPage_NameLabel;
	public static String IUGeneralInfoPropertyPage_ProviderLabel;
	public static String IUGeneralInfoPropertyPage_VersionLabel;
	public static String IULicensePropertyPage_NoLicense;
	public static String IULicensePropertyPage_ViewLicenseLabel;
	public static String PGPPublicKeyViewDialog_Title;
	public static String ProfileModificationAction_InvalidSelections;
	public static String ProfileModificationWizardPage_DetailsLabel;
	public static String ProfileSnapshots_Label;

	// viewer support
	public static String ProvDropAdapter_InvalidDropTarget;
	public static String ProvDropAdapter_NoIUsToDrop;
	public static String ProvDropAdapter_UnsupportedDropOperation;
	public static String ProvElementContentProvider_FetchJobTitle;

	// Provisioning operations
	public static String ProvisioningOperationRunner_CannotApplyChanges;
	public static String ProvisioningOperationWizard_Remediation_Operation;
	public static String ProvisioningOperationWizard_UnexpectedFailureToResolve;
	public static String InstalledSoftwarePage_Filter_Installed_Software;
	public static String InstalledSoftwarePage_NoProfile;
	public static String InstallIUOperationLabel;
	public static String InstallIUOperationTask;
	public static String InstallIUCommandLabel;
	public static String InstallIUCommandTooltip;
	public static String InstallWizardPage_NoCheckboxDescription;
	public static String InstallWizardPage_Title;
	public static String PreselectedIUInstallWizard_Title;
	public static String PreselectedIUInstallWizard_Description;
	public static String UninstallDialog_UninstallMessage;
	public static String UninstallIUOperationLabel;
	public static String UninstallIUOperationTask;
	public static String UninstallIUCommandLabel;
	public static String UninstallIUCommandTooltip;
	public static String UninstallIUProgress;
	public static String UninstallWizardPage_Description;
	public static String UninstallWizardPage_Title;
	public static String UpdateIUOperationLabel;
	public static String UpdateIUOperationTask;
	public static String UpdateIUCommandLabel;
	public static String UpdateIUCommandTooltip;
	public static String UpdateIUProgress;
	public static String RefreshAction_Label;
	public static String RefreshAction_Tooltip;
	public static String RemoveColocatedRepositoryAction_Label;
	public static String RemoveColocatedRepositoryAction_Tooltip;
	public static String RevertIUCommandLabel;
	public static String RevertIUCommandTooltip;

	// Property pages
	public static String IUPropertyPage_NoIUSelected;

	public static String RepositoryDetailsLabelProvider_Disabled;
	public static String RepositoryDetailsLabelProvider_Enabled;
	// Dialog groups
	public static String RepositoryGroup_LocalRepoBrowseButton;
	public static String RepositoryGroup_ArchivedRepoBrowseButton;
	public static String RepositoryGroup_RepositoryFile;
	public static String RepositoryGroup_SelectRepositoryDirectory;
	public static String RepositoryGroup_URLRequired;
	public static String RepositoryManipulationPage_Add;
	public static String RepositoryManipulationPage_ContactingSiteMessage;
	public static String RepositoryManipulationPage_DefaultFilterString;
	public static String RepositoryManipulationPage_Description;
	public static String RepositoryManipulationPage_DisableButton;
	public static String RepositoryManipulationPage_EnableButton;
	public static String RepositoryManipulationPage_EnabledColumnTitle;
	public static String RepositoryManipulationPage_Export;
	public static String RepositoryManipulationPage_Import;
	public static String RepositoryManipulationPage_LocationColumnTitle;
	public static String RepositoryManipulationPage_NameColumnTitle;
	public static String RepositoryManipulationPage_Edit;
	public static String RepositoryManipulationPage_RefreshConnection;
	public static String RepositoryManipulationPage_RefreshOperationCanceled;
	public static String RepositoryManipulationPage_Remove;
	public static String RepositoryManipulationPage_RemoveConfirmMessage;
	public static String RepositoryManipulationPage_RemoveConfirmSingleMessage;
	public static String RepositoryManipulationPage_RemoveConfirmTitle;
	public static String RepositoryManipulationPage_TestConnectionSuccess;
	public static String RepositoryManipulationPage_TestConnectionTitle;
	public static String RepositoryManipulationPage_Title;
	public static String RepositoryManipulationPage_Manage;
	public static String RepositoryManipulatorDropTarget_DragAndDropJobLabel;
	public static String RepositoryManipulatorDropTarget_DragSourceNotValid;
	public static String RepositoryNameAndLocationDialog_Title;

	public static String RepositorySelectionGroup_NameAndLocationSeparator;
	public static String ResolutionWizardPage_Canceled;
	public static String ResolutionWizardPage_ErrorStatus;
	public static String ResolutionWizardPage_NoSelections;
	public static String ResolutionWizardPage_WarningInfoStatus;
	public static String ResolutionWizardPage_RelaxedConstraints;
	public static String ResolutionWizardPage_RelaxedConstraintsTip;
	public static String ResolutionStatusPage_ErrorIULocked;

	// Dialogs
	public static String AddRepositoryDialog_InvalidURL;
	public static String AddRepositoryDialog_LocationLabel;
	public static String AddRepositoryDialog_NameLabel;
	public static String AddRepositoryDialog_Title;
	public static String AddRepositoryDialog_addButtonLabel;
	public static String AvailableIUGroup_LoadingRepository;
	public static String AvailableIUGroup_NoSitesConfiguredDescription;
	public static String AvailableIUGroup_NoSitesConfiguredExplanation;
	public static String ColocatedRepositoryManipulator_NoContentExplanation;
	public static String AvailableIUGroup_NoSitesExplanation;
	public static String AvailableIUsPage_AddButton;
	public static String AvailableIUsPage_AllSites;
	public static String AvailableIUsPage_Description;
	public static String AvailableIUsPage_FilterOnEnvCheckBox;
	public static String AvailableIUsPage_GotoInstallInfo;
	public static String AvailableIUsPage_GotoProperties;
	public static String AvailableIUsPage_GroupByCategory;
	public static String AvailableIUsPage_HideInstalledItems;
	public static String AvailableIUsPage_LocalSites;
	public static String AvailableIUsPage_MultipleSelectionCount;
	public static String AvailableIUsPage_NameWithLocation;
	public static String AvailableIUsPage_NoSites;
	public static String AvailableIUsPage_RepoFilterInstructions;
	public static String AvailableIUsPage_RepoFilterLabel;
	public static String AvailableIUsPage_ResolveAllCheckbox;
	public static String AvailableIUsPage_SelectASite;
	public static String AvailableIUsPage_ShowLatestVersions;
	public static String AvailableIUsPage_SingleSelectionCount;
	public static String AvailableIUsPage_Title;
	public static String AvailableIUsPage_Fetching;
	public static String AvailableIUWrapper_AllAreInstalled;
	public static String IUViewQueryContext_AllAreInstalledDescription;
	public static String Label_Profiles;
	public static String Label_Repositories;
	public static String LaunchUpdateManagerButton;
	public static String LoadMetadataRepositoryJob_ContactSitesProgress;
	public static String LoadMetadataRepositoryJob_SitesMissingError;
	public static String RepositoryElement_NotFound;
	public static String RepositoryTracker_DuplicateLocation;
	public static String MetadataRepositoryElement_RepositoryLoadError;
	public static String UpdateAction_UpdatesAvailableMessage;
	public static String UpdateAction_UpdatesAvailableTitle;
	public static String UpdateActionRemediationJobName;
	public static String UpdateActionRemediationJobTask;
	public static String PlatformUpdateTitle;
	public static String PlatformRestartMessage;
	public static String Policy_RequiresUpdateManagerMessage;
	public static String Policy_RequiresUpdateManagerTitle;
	public static String ProvUI_ErrorDuringApplyConfig;
	public static String ProvUI_InformationTitle;
	public static String ProvUI_InstallDialogError;
	public static String ProvUI_NameColumnTitle;
	public static String ProvUI_IdColumnTitle;
	public static String ProvUI_ProviderColumnTitle;
	public static String ProvUI_VersionColumnTitle;
	public static String ProvUI_WarningTitle;
	public static String ProvUIMessages_NotAccepted_EnterFor_0;
	public static String ProvUIMessages_SavedNotAccepted_EnterFor_0;
	public static String OptionalPlatformRestartMessage;
	public static String IUViewQueryContext_NoCategorizedItemsDescription;
	public static String QueriedElementWrapper_NoCategorizedItemsExplanation;
	public static String QueriedElementWrapper_NoItemsExplanation;
	public static String QueriedElementWrapper_SiteNotFound;
	public static String QueryableMetadataRepositoryManager_LoadRepositoryProgress;
	public static String QueryableProfileRegistry_QueryProfileProgress;
	public static String QueryableUpdates_UpdateListProgress;
	public static String SizeComputingWizardPage_SizeJobTitle;
	public static String RevertDialog_ConfigContentsLabel;
	public static String RevertDialog_ConfigsLabel;
	public static String RevertDialog_ConfirmRestartMessage;
	public static String RevertDialog_RevertOperationLabel;
	public static String RevertDialog_Title;
	public static String RevertDialog_CancelButtonLabel;
	public static String RollbackProfileElement_CurrentInstallation;
	public static String SelectableIUsPage_Select_All;
	public static String SelectableIUsPage_Deselect_All;
	public static String InstallRemediationPage_Title;
	public static String InstallRemediationPage_Description;
	public static String KeySigningInfoFactory_FingerprintItem;
	public static String KeySigningInfoFactory_KeySignersSection;
	public static String KeySigningInfoFactory_PGPSigningType;
	public static String KeySigningInfoFactory_UserIDItem;
	public static String UpdateRemediationPage_Title;
	public static String UpdateRemediationPage_Description;
	public static String RemediationPage_SubDescription;
	public static String RemediationPage_NoSolutionFound;
	public static String RemediationPage_BeingInstalledSection;
	public static String RemediationPage_InstalledSection;
	public static String RemediationPage_BeingInstalledSection_AllowPartialInstall;
	public static String RemediationPage_BeingInstalledSection_AllowDifferentVersion;
	public static String RemediationPage_InstalledSection_AllowInstalledUpdate;
	public static String RemediationPage_InstalledSection_AllowInstalledRemoval;
	public static String RemediationPage_BestSolutionBeingInstalledRelaxed;
	public static String RemediationPage_BestSolutionInstallationRelaxed;
	public static String RemediationPage_BestSolutionBuilt;
	public static String RemediationPage_SolutionDetails;
	public static String RemedyCategoryAdded;
	public static String RemedyCategoryRemoved;
	public static String RemedyCategoryChanged;
	public static String RemedyCategoryNotAdded;
	public static String RemedyElementInstalledVersion;
	public static String RemedyElementRequestedVersion;
	public static String RemedyElementBeingInstalledVersion;
	public static String RemedyElementNotHighestVersion;

	public static String TrustAuthorityDialog_AcceptTrustAllAuthorities;
	public static String TrustAuthorityDialog_AuthoritiesCollapseAllButton;
	public static String TrustAuthorityDialog_AuthoritiesDeselectAllButton;
	public static String TrustAuthorityDialog_AuthoritiesExpandAllButton;
	public static String TrustAuthorityDialog_AuthoritiesSelectAllButton;
	public static String TrustAuthorityDialog_AuthorityColumnTitle;
	public static String TrustAuthorityDialog_AuthorityCopyLinkMenu;
	public static String TrustAuthorityDialog_AuthorityInsecure;
	public static String TrustAuthorityDialog_AuthoritySecure;
	public static String TrustAuthorityDialog_CertificateDetailsButton;
	public static String TrustAuthorityDialog_CertificateExportButton;
	public static String TrustAuthorityDialog_ComputingAuthorityCertficate;
	public static String TrustAuthorityDialog_ExportDialogTitle;
	public static String TrustAuthorityDialog_IUColumnTitle;
	public static String TrustAuthorityDialog_IUDetailDialogDescriptionMessage;
	public static String TrustAuthorityDialog_IUDetailsButton;
	public static String TrustAuthorityDialog_IUDetailsDialogCountMessage;
	public static String TrustAuthorityDialog_IUDetailsDialogTitle;
	public static String TrustAuthorityDialog_IUVersionColumnTitle;
	public static String TrustAuthorityDialog_RejectTrustAllAuthorities;
	public static String TrustAuthorityDialog_RememberSelectedAuthoritiesCheckbox;
	public static String TrustAuthorityDialog_SecuredColumnTitle;
	public static String TrustAuthorityDialog_TrustAllAuthoritiesCheckbox;
	public static String TrustAuthorityDialog_TrustAllAuthoritiesConfirmationDescription;
	public static String TrustAuthorityDialog_TrustAllAuthoritiesConfirmationTitle;
	public static String TrustAuthorityDialog_TrustAuthoritiesTitle;
	public static String TrustAuthorityDialog_TrustAuthorityDescriptionMessage;
	public static String TrustAuthorityDialog_TrustAuthorityMainMessage;
	public static String TrustAuthorityDialog_TrustInsecureAuthorityMessage;
	public static String TrustAuthorityDialog_TrustSelectedCheckbox;
	public static String TrustAuthorityDialog_UnitsColumnTitle;

	public static String TrustCertificateDialog_Details;
	public static String TrustCertificateDialog_Export;
	public static String TrustCertificateDialog_ExportDialogTitle;
	public static String TrustCertificateDialog_Title;
	public static String TrustCertificateDialog_Message;
	public static String TrustCertificateDialog_MessageUnsigned;
	public static String TrustCertificateDialog_MessageNameWarning;
	public static String TrustCertificateDialog_MessagePGP;
	public static String TrustCertificateDialog_MessageRevoked;
	public static String TrustCertificateDialog_AcceptSelectedButtonLabel;
	public static String TrustCertificateDialog_AlwaysTrust;
	public static String TrustCertificateDialog_AlwaysTrustConfirmationMessage;
	public static String TrustCertificateDialog_AlwaysTrustConfirmationTitle;
	public static String TrustCertificateDialog_AlwaysTrustNo;
	public static String TrustCertificateDialog_AlwaysTrustYes;
	public static String TrustCertificateDialog_ArtifactId;
	public static String TrustCertificateDialog_SelectAll;
	public static String TrustCertificateDialog_DeselectAll;
	public static String TrustCertificateDialog_ObjectType;
	public static String TrustCertificateDialog_Id;
	public static String TrustCertificateDialog_Name;
	public static String TrustCertificateDialog_Classifier;
	public static String TrustCertificateDialog_CopyFingerprint;
	public static String TrustCertificateDialog_dates;
	public static String TrustCertificateDialog_NotApplicable;
	public static String TrustCertificateDialog_NotYetValidStartDate;
	public static String TrustCertificateDialog_expiredSince;
	public static String TrustCertificateDialog_validExpires;
	public static String TrustCertificateDialog_valid;
	public static String TrustCertificateDialog_expired;
	public static String TrustCertificateDialog_notYetValid;
	public static String TrustCertificateDialog_RememberSigners;
	public static String TrustCertificateDialog_revoked;
	public static String TrustCertificateDialog_Unknown;
	public static String TrustCertificateDialog_Unsigned;
	public static String TrustCertificateDialog_Version;
	public static String TrustCertificateDialogQuestionTrustRevokedKeyAccept;
	public static String TrustCertificateDialogQuestionTrustRevokedKeyQuestion;
	public static String TrustCertificateDialogQuestionTrustRevokedKeyReject;
	public static String TrustCertificateDialogQuestionTrustRevokedKeyTitle;
	// Operations
	public static String UpdateManagerCompatibility_ExportSitesTitle;
	public static String UpdateManagerCompatibility_ImportSitesTitle;
	public static String UpdateManagerCompatibility_InvalidSiteFileMessage;
	public static String UpdateManagerCompatibility_InvalidSitesTitle;
	public static String UpdateManagerCompatibility_ItemRequiresUpdateManager;
	public static String UpdateManagerCompatibility_UnableToOpenFindAndInstall;
	public static String ServiceUI_LoginDetails;
	public static String ServiceUI_LoginRequired;
	public static String ServiceUI_unsigned_message;
	public static String ServiceUI_warning_title;
	public static String ServiceUI_InstallAnywayAction_Label;
	public static String UpdateOrInstallWizardPage_Size;
	public static String Updates_Label;
	public static String UpdateSingleIUPage_SingleUpdateDescription;
	public static String UpdateWizardPage_Description;
	public static String UpdateWizardPage_Title;
	public static String UserValidationDialog_PasswordLabel;
	public static String UserValidationDialog_SavePasswordButton;
	public static String UserValidationDialog_UsernameLabel;
}
