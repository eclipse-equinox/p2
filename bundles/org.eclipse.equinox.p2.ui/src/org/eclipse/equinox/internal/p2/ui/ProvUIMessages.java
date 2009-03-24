/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
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
	public static String AcceptLicensesWizardPage_Title;
	public static String ApplicationInRestartDialog;
	public static String ApplyProfileChangesDialog_ApplyChanges;
	public static String CategoryElementCollector_Uncategorized;
	public static String ColocatedRepositoryManipulator_AddSiteOperationLabel;
	public static String ColocatedRepositoryManipulator_GotoPrefs;
	public static String ColocatedRepositoryManipulator_ManageSites;
	public static String ColocatedRepositoryManipulator_RemoveSiteOperationLabel;
	public static String RevertProfilePage_RevertLabel;
	public static String RevertProfilePage_RevertTooltip;
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
	public static String ProfileChangeRequestBuildingRequest;
	public static String ProfileElement_InvalidProfile;
	public static String ProfileModificationAction_NoChangeRequestProvided;
	public static String ProfileModificationAction_NoExplanationProvided;
	public static String ProfileModificationAction_ResolutionOperationLabel;
	public static String ProfileModificationWizardPage_DetailsLabel;
	public static String ProfileModificationWizardPage_NothingSelected;
	public static String ProfileModificationWizardPage_ResolutionOperationLabel;
	public static String ProfileModificationWizardPage_UnexpectedError;
	public static String ProfileSnapshots_Label;
	// utility error messages
	public static String ProvisioningUtil_NoRepositoryManager;
	public static String ProvisioningUtil_LoadRepositoryFailure;
	public static String ProvisioningUtil_NoProfileRegistryFound;
	public static String ProvisioningUtil_NoPlannerFound;
	public static String ProvisioningUtil_NoDirectorFound;
	public static String ProvisioningUtil_NoEngineFound;

	// viewer support
	public static String ProvDropAdapter_InvalidDropTarget;
	public static String ProvDropAdapter_NoIUsToDrop;
	public static String ProvDropAdapter_UnsupportedDropOperation;

	// Provisioning operations
	public static String ProvisioningOperation_ExecuteErrorTitle;
	public static String ProvisioningOperation_RedoErrorTitle;
	public static String ProvisioningOperation_UndoErrorTitle;
	public static String ProvisioningOperationRunner_ErrorExecutingOperation;
	public static String InstallIUOperationLabel;
	public static String InstallIUCommandLabel;
	public static String InstallIUCommandTooltip;
	public static String InstallIUProgress;
	public static String InstallWizardPage_NoCheckboxDescription;
	public static String InstallWizardPage_Title;
	public static String UninstallDialog_UninstallMessage;
	public static String UninstallIUOperationLabel;
	public static String UninstallIUCommandLabel;
	public static String UninstallIUCommandTooltip;
	public static String UninstallIUProgress;
	public static String UninstallWizardPage_Description;
	public static String UninstallWizardPage_Title;
	public static String UpdateIUOperationLabel;
	public static String UpdateIUCommandLabel;
	public static String UpdateIUCommandTooltip;
	public static String UpdateIUProgress;
	public static String RefreshAction_Label;
	public static String RefreshAction_Tooltip;
	public static String RemoveColocatedRepositoryAction_Label;
	public static String RemoveColocatedRepositoryAction_OperationLabel;
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
	public static String RepositoryManipulationPage_RefreshConnection;
	public static String RepositoryManipulationPage_RefreshOperationLabel;
	public static String RepositoryManipulationPage_Remove;
	public static String RepositoryManipulationPage_RemoveConfirmMessage;
	public static String RepositoryManipulationPage_RemoveConfirmSingleMessage;
	public static String RepositoryManipulationPage_RemoveConfirmTitle;
	public static String RepositoryManipulationPage_Title;
	public static String RepositoryManipulatorDropTarget_DragAndDropJobLabel;
	public static String RepositoryManipulatorDropTarget_DragSourceNotValid;
	public static String ResolutionReport_SummaryStatus;
	public static String ResolutionWizardPage_Canceled;
	public static String ResolutionWizardPage_ErrorStatus;
	public static String ResolutionWizardPage_NoSelections;
	public static String ResolutionWizardPage_WarningInfoStatus;

	public static String AddColocatedRepositoryAction_Label;
	public static String AddColocatedRepositoryAction_Tooltip;
	public static String AddColocatedRepositoryDialog_AddSiteTitle;
	// Dialogs
	public static String AddRepositoryDialog_DuplicateURL;
	public static String AddRepositoryDialog_InvalidURL;
	public static String AddRepositoryDialog_LocationLabel;
	public static String AddRepositoryDialog_NameLabel;
	public static String AddRepositoryDialog_Title;
	public static String AvailableIUElement_ProfileNotFound;
	public static String AvailableIUGroup_LoadingRepository;
	public static String AvailableIUGroup_NoSitesConfiguredDescription;
	public static String AvailableIUGroup_NoSitesConfiguredExplanation;
	public static String ColocatedRepositoryManipulator_NoContentExplanation;
	public static String AvailableIUGroup_NoSitesExplanation;
	public static String AvailableIUsPage_AddButton;
	public static String AvailableIUsPage_AllSites;
	public static String AvailableIUsPage_Description;
	public static String AvailableIUsPage_GotoInstallInfo;
	public static String AvailableIUsPage_GotoProperties;
	public static String AvailableIUsPage_GroupByCategory;
	public static String AvailableIUsPage_HideInstalledItems;
	public static String AvailableIUsPage_LocalSites;
	public static String AvailableIUsPage_NameWithLocation;
	public static String AvailableIUsPage_NoSites;
	public static String AvailableIUsPage_RepoFilterInstructions;
	public static String AvailableIUsPage_RepoFilterLabel;
	public static String AvailableIUsPage_ResolveAllCheckbox;
	public static String AvailableIUsPage_SelectASite;
	public static String AvailableIUsPage_ShowLatestVersions;
	public static String AvailableIUsPage_Title;
	public static String AvailableIUWrapper_AllAreInstalled;
	public static String IUViewQueryContext_AllAreInstalledDescription;
	public static String DefaultQueryProvider_ErrorRetrievingProfile;
	public static String DeferredFetchFilteredTree_RetrievingList;
	public static String ElementUtils_UpdateJobTitle;
	public static String Label_Profiles;
	public static String Label_Repositories;
	public static String MetadataRepositoryElement_NotFound;
	public static String MetadataRepositoryElement_RepositoryLoadError;
	public static String UpdateAction_UpdatesAvailableMessage;
	public static String UpdateAction_UpdatesAvailableTitle;
	public static String PlanAnalyzer_IgnoringInstall;
	public static String PlanAnalyzer_IgnoringUninstall;
	public static String PlanAnalyzer_LockedImpliedUpdate0;
	public static String PlanAnalyzer_PartialInstall;
	public static String PlanAnalyzer_PartialUninstall;
	public static String PlanAnalyzer_SideEffectInstall;
	public static String PlanAnalyzer_SideEffectUninstall;
	public static String PlannerResolutionOperation_UnexpectedError;
	public static String PlanStatusHelper_IgnoringImpliedDowngrade;
	public static String PlanStatusHelper_ImpliedUpdate;
	public static String PlanStatusHelper_Items;
	public static String PlanStatusHelper_NothingToDo;
	public static String PlanStatusHelper_AlreadyInstalled;
	public static String PlanStatusHelper_AnotherOperationInProgress;
	public static String PlanStatusHelper_Launch;
	public static String PlanStatusHelper_RequestAltered;
	public static String PlanStatusHelper_RequiresUpdateManager;
	public static String PlanStatusHelper_UnexpectedError;
	public static String PlanStatusHelper_UpdateManagerPromptTitle;
	public static String PlanStatusHelper_PromptForUpdateManagerUI;
	public static String PlatformUpdateTitle;
	public static String PlatformRestartMessage;
	public static String Policy_MultiplePolicyRegistrationsWarning;
	public static String ProvUI_ErrorDuringApplyConfig;
	public static String ProvUI_InformationTitle;
	public static String ProvUI_InstallDialogError;
	public static String ProvUI_NameColumnTitle;
	public static String ProvUI_IdColumnTitle;
	public static String ProvUI_VersionColumnTitle;
	public static String ProvUI_WarningTitle;
	public static String ProvUIActivator_ExceptionDuringProfileChange;
	public static String ProvUILicenseManager_ParsingError;
	public static String OptionalPlatformRestartMessage;
	public static String IUViewQueryContext_NoCategorizedItemsDescription;
	public static String QueriedElementWrapper_NoCategorizedItemsExplanation;
	public static String QueriedElementWrapper_NoItemsExplanation;
	public static String QueriedElementWrapper_SiteNotFound;
	public static String ColocatedRepositoryManipulator_SiteNotFoundDescription;
	public static String QueryableArtifactRepositoryManager_RepositoryQueryProgress;
	public static String QueryableMetadataRepositoryManager_LoadRepositoryProgress;
	public static String QueryableMetadataRepositoryManager_MultipleRepositoriesNotFound;
	public static String QueryableMetadataRepositoryManager_RepositoryQueryProgress;
	public static String QueryableProfileRegistry_QueryProfileProgress;
	public static String QueryableUpdates_UpdateListProgress;
	public static String SizeComputingWizardPage_SizeJobTitle;
	public static String SizingPhaseSet_PhaseSetName;
	public static String RevertDialog_ConfigContentsLabel;
	public static String RevertDialog_ConfigsLabel;
	public static String RevertDialog_ConfirmRestartMessage;
	public static String RevertDialog_Description;
	public static String RevertDialog_PageTitle;
	public static String RevertDialog_RevertError;
	public static String RevertDialog_RevertOperationLabel;
	public static String RevertDialog_Title;
	public static String RevertProfileWizardPage_ErrorRetrievingHistory;
	public static String RollbackProfileElement_InvalidSnapshot;

	public static String TrustCertificateDialog_Details;
	public static String TrustCertificateDialog_Title;
	// Operations
	public static String URLValidator_UnrecognizedURL;
	public static String UpdateManagerCompatibility_ExportSitesTitle;
	public static String UpdateManagerCompatibility_ImportSitesTitle;
	public static String UpdateManagerCompatibility_InvalidSiteFileMessage;
	public static String UpdateManagerCompatibility_InvalidSitesTitle;
	public static String UpdateManagerCompatibility_UnableToOpenFindAndInstall;
	public static String UpdateManagerCompatibility_UnableToOpenManageConfiguration;
	public static String UpdateOperation_NothingToUpdate;
	public static String ServiceUI_Cancel;
	public static String ServiceUI_LoginDetails;
	public static String ServiceUI_LoginRequired;
	public static String ServiceUI_OK;
	public static String UpdateOrInstallWizardPage_Size;
	public static String Updates_Label;
	public static String UpdateWizardPage_Description;
	public static String UpdateWizardPage_Title;
	public static String UserValidationDialog_PasswordLabel;
	public static String UserValidationDialog_SavePasswordButton;
	public static String UserValidationDialog_UsernameLabel;

}
