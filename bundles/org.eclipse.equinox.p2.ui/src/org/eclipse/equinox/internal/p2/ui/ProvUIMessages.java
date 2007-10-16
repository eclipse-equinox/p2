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

	public static String AddColocatedRepositoryDialog_InvalidURL;
	public static String ApplyProfileChangesDialog_ApplyChanges;
	public static String ChooseProfileDialog_DefaultDialogTitle;
	public static String ColocatedRepositoryManipulatorGroup_Add;
	public static String ColocatedRepositoryManipulatorGroup_LocationColumnHeader;
	public static String ColocatedRepositoryManipulatorGroup_NameColumnHeader;
	public static String ColocatedRepositoryManipulatorGroup_Remove;
	public static String InstallDialog_InstallSelectionMessage;
	public static String IUPropertiesGroup_CopyrightProperty;
	public static String IUPropertiesGroup_DescriptionProperty;
	public static String IUPropertiesGroup_LicenseProperty;
	public static String IUDetailsLabelProvider_Unknown;
	public static String IUPropertiesGroup_NameProperty;
	public static String IUPropertiesGroup_ProviderProperty;
	// utility error messages
	public static String ProvisioningUtil_NoRepositoryManager;
	public static String ProvisioningUtil_AddRepositoryFailure;
	public static String ProvisioningUtil_CreateRepositoryFailure;
	public static String ProvisioningUtil_RepoNotWritable;
	public static String ProvisioningUtil_RepositoryNotFound;
	public static String ProvisioningUtil_NoProfileRegistryFound;
	public static String ProvisioningUtil_NoPlannerFound;
	public static String ProvisioningUtil_NoEngineFound;
	public static String ProvisioningUtil_NoIUFound;
	public static String ProvisioningUtil_NoInstallRegistryFound;
	public static String ProvisioningUtil_NoProfileInstallRegistryFound;
	public static String ProvisioningUtil_RepositoriesSearched;

	// viewer support
	public static String ProvDropAdapter_InvalidDropTarget;
	public static String ProvDropAdapter_NoIUsToDrop;
	public static String ProvDropAdapter_UnsupportedDropOperation;

	// Provisioning operations
	public static String ProvisioningOperation_ExecuteErrorTitle;
	public static String ProvisioningOperation_RedoErrorTitle;
	public static String ProvisioningOperation_UndoErrorTitle;
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
	public static String RollbackIUOperationLabel;
	public static String RollbackIUOperationLabelWithMnemonic;
	public static String RollbackIUCommandLabel;
	public static String RollbackIUCommandTooltip;
	public static String RollbackIUProgress;

	// Property pages
	public static String IUPropertyPage_NoIUSelected;
	public static String RepositoryPropertyPage_NoRepoSelected;

	// Dialog groups
	public static String RepositoryGroup_LocalRepoBrowseButton;
	public static String RepositoryGroup_ArchivedRepoBrowseButton;
	public static String RepositoryGroup_RepositoryFile;
	public static String RepositoryGroup_SelectRepositoryDirectory;
	public static String RepositoryGroup_RepositoryNameFieldLabel;
	public static String RepositoryGroup_URLRequired;
	public static String RepositoryGroup_RepositoryURLFieldLabel;

	// Dialogs
	public static String AddRepositoryDialog_DuplicateURL;
	public static String AddRepositoryDialog_Title;
	public static String UpdateAction_UpdateInformationTitle;
	public static String UpdateAction_UpdatesAvailableMessage;
	public static String UpdateAction_UpdatesAvailableTitle;
	public static String PlatformUpdateTitle;
	public static String PlatformRestartMessage;
	public static String ProvUI_NameColumnTitle;
	public static String ProvUI_SizeColumnTitle;
	public static String ProvUI_VersionColumnTitle;
	public static String OptionalPlatformRestartMessage;

	// Operations
	public static String UpdateAndInstallGroup_Properties;
	public static String UpdateAndInstallSelectionDialog_DeselectAllLabel;
	public static String UpdateAndInstallSelectionDialog_SelectAllLabel;
	public static String UpdateAndInstallGroup_Refresh;
	public static String UpdateOperation_NothingToUpdate;

}
