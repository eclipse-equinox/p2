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

package org.eclipse.equinox.internal.p2.ui.admin;

import org.eclipse.osgi.util.NLS;

/**
 * Message class for provisioning UI messages.  
 * 
 * @since 3.4
 */
public class ProvAdminUIMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.p2.ui.admin.internal.messages"; //$NON-NLS-1$
	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, ProvAdminUIMessages.class);
	}
	public static String AddArtifactRepositoryDialog_OperationLabel;
	public static String AddProfileDialog_Title;
	public static String AddRepositoryDialog_InvalidURL;
	public static String AddMetadataRepositoryDialog_OperationLabel;
	public static String ProfilesView_AlwaysConfirmSelectionInstallOps;
	public static String MetadataRepositoriesView_AddRepositoryTooltip;
	public static String MetadataRepositoriesView_AddRepositoryLabel;
	public static String MetadataRepositoriesView_ChooseProfileDialogTitle;
	public static String MetadataRepositoriesView_RemoveRepositoryLabel;
	public static String MetadataRepositoriesView_RemoveRepositoryTooltip;
	public static String MetadataRepositoriesView_RemoveRepositoryOperationLabel;
	public static String ArtifactRepositoriesView_AddRepositoryTooltip;
	public static String ArtifactRepositoriesView_AddRepositoryLabel;
	public static String ArtifactRepositoriesView_RemoveRepositoryLabel;
	public static String ArtifactRepositoriesView_RemoveRepositoryTooltip;
	public static String ArtifactRepositoriesView_RemoveRepositoryOperationLabel;
	public static String ProfilesView_AddProfileTooltip;
	public static String ProfilesView_AddProfileLabel;
	public static String ProfilesView_ConfirmUninstallMessage;
	public static String ProfilesView_RemoveProfileLabel;
	public static String ProfilesView_RemoveProfileTooltip;
	// Preferences
	public static String ProvisioningPrefPage_ConfirmSelectionInstallOps;
	public static String ProvisioningPrefPage_HideInternalRepos;
	public static String ProvisioningPrefPage_ShowGroupsOnly;
	public static String ProvisioningPrefPage_Description;

	public static String Ops_RemoveProfileOperationLabel;
	public static String Ops_ConfirmIUInstall;
	public static String AddProfileDialog_OperationLabel;
	public static String AddProfileDialog_DuplicateProfileID;
	public static String UpdateAndInstallDialog_AvailableIUsPageLabel;
	public static String UpdateAndInstallDialog_InstalledIUsPageLabel;

	public static String Ops_InstallIUOperationLabel;
	public static String Ops_UninstallIUOperationLabel;
	public static String Ops_UpdateIUOperationLabel;

	public static String InstallIUCommandLabel;
	public static String BecomeIUCommandLabel;
	public static String InstallIUCommandTooltip;
	public static String UninstallIUCommandLabel;
	public static String UninstallIUCommandTooltip;
	public static String UpdateIUCommandLabel;
	public static String UpdateIUCommandTooltip;
	public static String ProvView_RefreshCommandLabel;
	public static String ProvView_RefreshCommandTooltip;
}
