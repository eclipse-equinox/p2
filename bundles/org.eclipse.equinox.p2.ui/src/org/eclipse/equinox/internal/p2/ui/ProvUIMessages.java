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
	public static String AcceptLicensesWizardPage_ReviewLicensesDescription;
	public static String AcceptLicensesWizardPage_Title;
	public static String ApplyProfileChangesDialog_ApplyChanges;
	public static String CategoryElementCollector_Uncategorized;
	public static String ChooseProfileDialog_DefaultDialogTitle;
	public static String InstallDialog_InstallSelectionMessage;
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
	public static String ProfileElement_InvalidProfile;
	public static String ProfileModificationAction_NullPlan;
	public static String ProfileModificationAction_UnexpectedError;
	public static String ProfileModificationWizardPage_DetailsLabel;
	public static String ProfileModificationWizardPage_NothingSelected;
	public static String ProfileModificationWizardPage_ProfileNotFound;
	public static String ProfileModificationWizardPage_UnexpectedError;
	// utility error messages
	public static String ProvisioningUtil_NoRepositoryManager;
	public static String ProvisioningUtil_AddRepositoryFailure;
	public static String ProvisioningUtil_CreateRepositoryFailure;
	public static String ProvisioningUtil_InstallManyTask;
	public static String ProvisioningUtil_InstallOneTask;
	public static String ProvisioningUtil_LoadRepositoryFailure;
	public static String ProvisioningUtil_RepoNotWritable;
	public static String ProvisioningUtil_RepositoryNotFound;
	public static String ProvisioningUtil_NoProfileRegistryFound;
	public static String ProvisioningUtil_NoPlannerFound;
	public static String ProvisioningUtil_NoDirectorFound;
	public static String ProvisioningUtil_NoEngineFound;
	public static String ProvisioningUtil_NoIUFound;
	public static String ProvisioningUtil_NoInstallRegistryFound;
	public static String ProvisioningUtil_NoProfileInstallRegistryFound;
	public static String ProvisioningUtil_RepositoriesSearched;

	// viewer support
	public static String ProvDropAdapter_InvalidDropTarget;
	public static String ProvDropAdapter_NoIUsToDrop;
	public static String ProvDropAdapter_UnsupportedDropOperation;
	public static String ProvElementQueryResult_CouldNotInstantiateElement;

	// Provisioning operations
	public static String ProvisioningOperation_ExecuteErrorTitle;
	public static String ProvisioningOperation_RedoErrorTitle;
	public static String ProvisioningOperation_UndoErrorTitle;
	public static String ProvisioningOperationRunner_ErrorExecutingOperation;
	public static String InstallIUOperationLabel;
	public static String InstallIUOperationLabelWithMnemonic;
	public static String InstallIUCommandLabel;
	public static String InstallIUCommandTooltip;
	public static String InstallIUProgress;
	public static String UninstallDialog_UninstallMessage;
	public static String UninstallIUOperationLabel;
	public static String UninstallIUOperationLabelWithMnemonic;
	public static String UninstallIUCommandLabel;
	public static String UninstallIUCommandTooltip;
	public static String UninstallIUProgress;
	public static String UpdateIUOperationLabel;
	public static String UpdateIUOperationLabelWithMnemonic;
	public static String UpdateIUCommandLabel;
	public static String UpdateIUCommandTooltip;
	public static String UpdateIUProgress;
	public static String RevertIUOperationLabel;
	public static String RevertIUOperationLabelWithMnemonic;
	public static String RevertIUCommandLabel;
	public static String RevertIUCommandTooltip;
	public static String RevertIUProgress;

	// Property pages
	public static String IUPropertyPage_NoIUSelected;
	public static String RepositoryPropertyPage_DescriptionFieldLabel;
	public static String RepositoryPropertyPage_NameFieldLabel;
	public static String RepositoryPropertyPage_URLFieldLabel;

	public static String RepositoryPropertyPage_NoRepoSelected;

	// Dialog groups
	public static String RepositoryGroup_LocalRepoBrowseButton;
	public static String RepositoryGroup_ArchivedRepoBrowseButton;
	public static String RepositoryGroup_RepositoryFile;
	public static String RepositoryGroup_SelectRepositoryDirectory;
	public static String RepositoryGroup_URLRequired;
	public static String RepositoryManipulatorDropTarget_DragAndDropJobLabel;
	public static String RepositoryManipulatorDropTarget_DragSourceNotValid;

	// Dialogs
	public static String AddRepositoryDialog_DuplicateURL;
	public static String AddRepositoryDialog_InvalidURL;
	public static String AddRepositoryDialog_Title;
	public static String AddRepositoryDialog_URLValidationError;
	public static String AvailableIUContentProvider_FailureRetrievingContents;
	public static String AvailableIUContentProvider_JobName;
	public static String AvailableIUContentProvider_PlaceholderLabel;
	public static String AvailableIUElement_ProfileNotFound;
	public static String AvailableIUGroup_ViewByToolTipText;
	public static String Label_Profiles;
	public static String Label_Repositories;
	public static String MetadataRepositoryElement_RepositoryLoadError;
	public static String UpdateAction_ExceptionDuringUpdateCheck;
	public static String UpdateAction_UpdateInformationTitle;
	public static String UpdateAction_UpdatesAvailableMessage;
	public static String UpdateDialog_AssemblingUpdatesProgress;
	public static String UpdateAction_UpdatesAvailableTitle;
	public static String PlatformUpdateTitle;
	public static String PlatformRestartMessage;
	public static String ProvUI_ErrorDuringApplyConfig;
	public static String ProvUI_InformationTitle;
	public static String ProvUI_NameColumnTitle;
	public static String ProvUI_SizeColumnTitle;
	public static String ProvUI_VersionColumnTitle;
	public static String ProvUI_IDColumnTitle;
	public static String ProvUI_WarningTitle;
	public static String ProvUIActivator_ExceptionDuringProfileChange;
	public static String ProvUILicenseManager_ParsingError;
	public static String OptionalPlatformRestartMessage;
	public static String QueryableArtifactRepositoryManager_RepositoryQueryProgress;
	public static String QueryableMetadataRepositoryManager_RepositoryQueryProgress;
	public static String QueryableProfileRegistry_QueryProfileProgress;
	public static String QueryableUpdates_UpdateListProgress;
	public static String SizingPhaseSet_PhaseSetName;
	public static String RevertDialog_ConfigContentsLabel;
	public static String RevertDialog_ConfigsLabel;
	public static String RevertDialog_ConfirmRestartMessage;
	public static String RevertDialog_Description;
	public static String RevertDialog_PageTitle;
	public static String RevertDialog_RevertError;
	public static String RevertDialog_RevertOperationLabel;
	public static String RevertDialog_SelectMessage;
	public static String RevertDialog_Title;
	public static String RevertProfileWizardPage_ErrorRetrievingHistory;

	// Operations
	public static String UpdateAndInstallSelectionDialog_DeselectAllLabel;
	public static String UpdateAndInstallSelectionDialog_SelectAllLabel;
	public static String URLValidator_UnrecognizedURL;
	public static String UpdateManagerCompatibility_UnableToOpenFindAndInstall;
	public static String UpdateManagerCompatibility_UnableToOpenManageConfiguration;
	public static String UpdateOperation_NothingToUpdate;

}
