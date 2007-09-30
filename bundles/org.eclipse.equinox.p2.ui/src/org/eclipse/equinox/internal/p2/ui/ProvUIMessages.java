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
	public static String InstallAction_EntryPointNameRequired;
	public static String InstallAction_InstallConfirmTitle;
	public static String InstallAction_InstallInfoTitle;
	public static String InstallAction_InstallNotPermitted;
	public static String InstallAction_NameEntryPointMessage;
	public static String InstallOperation_CannotInstall;
	public static String IUPropertiesGroup_CopyrightProperty;
	public static String IUPropertiesGroup_DescriptionProperty;
	public static String IUPropertiesGroup_LicenseProperty;
	public static String IUPropertiesGroup_NameProperty;
	public static String IUPropertiesGroup_ProviderProperty;
	public static String ProfileGroup_Cache;
	public static String ProfileGroup_SelectBundlePoolCache;
	// utility error messages
	public static String ProvisioningUtil_NoRepositoryManager;
	public static String ProvisioningUtil_AddRepositoryFailure;
	public static String ProvisioningUtil_RepoNotWritable;
	public static String ProvisioningUtil_RepositoryNotFound;
	public static String ProvisioningUtil_NoProfileRegistryFound;
	public static String ProvisioningUtil_NoDirectorFound;
	public static String ProvisioningUtil_NoOracleFound;
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
	public static String Ops_InstallIUOperationLabel;
	public static String Ops_UninstallIUOperationLabel;
	public static String Ops_UpdateIUOperationLabel;
	public static String Ops_BecomeIUOperationLabel;

	// Property pages
	public static String ProfilePropertyPage_NoProfileSelected;
	public static String IUPropertyPage_NoIUSelected;
	public static String RepositoryGroup_NameColumnLabel;
	public static String RepositoryGroup_PropertiesLabel;
	public static String RepositoryGroup_ValueColumnLabel;
	public static String RepositoryPropertyPage_NoRepoSelected;

	// Dialog groups
	public static String IUGroup_ID;
	public static String IUGroup_IU_ID_Required;
	public static String IUGroup_Namespace;
	public static String IUGroup_ProvidedCapabilities;
	public static String IUGroup_RequiredCapabilities;
	public static String IUGroup_TouchpointData;
	public static String IUGroup_TouchpointType;
	public static String IUGroup_Version;
	public static String RepositoryGroup_Browse;
	public static String RepositoryGroup_RepositoryFile;
	public static String RepositoryGroup_SelectRepositoryDirectory;
	public static String RepositoryGroup_RepositoryNameFieldLabel;
	public static String RepositoryGroup_URLRequired;
	public static String RepositoryGroup_RepositoryURLFieldLabel;
	public static String ProfileGroup_Browse;
	public static String ProfileGroup_Environments;
	public static String ProfileGroup_Flavor;
	public static String ProfileGroup_ID;
	public static String ProfileGroup_InstallFolder;
	public static String ProfileGroup_Name;
	public static String ProfileGroup_NL;
	public static String ProfileGroup_SelectProfileMessage;
	public static String ProfileGroup_Description;
	public static String ProfileGroup_ProfileIDRequired;
	public static String ProfileGroup_ProfileInstallFolderRequired;

	// Dialogs
	public static String AddRepositoryDialog_DuplicateURL;
	public static String AddRepositoryDialog_Title;
	public static String UpdateAction_UpdateInformationTitle;
	public static String UpdateAction_UpdatesAvailableMessage;
	public static String UpdateAction_UpdatesAvailableTitle;
	public static String PlatformUpdateTitle;
	public static String PlatformRestartMessage;
	public static String OptionalPlatformRestartMessage;

	// Operations
	public static String UpdateAndInstallGroup_Install;
	public static String UpdateAndInstallGroup_Properties;
	public static String UpdateAndInstallGroup_Refresh;
	public static String UpdateAndInstallGroup_Uninstall;
	public static String UpdateAndInstallGroup_Update;
	public static String UpdateOperation_NothingToUpdate;

}
