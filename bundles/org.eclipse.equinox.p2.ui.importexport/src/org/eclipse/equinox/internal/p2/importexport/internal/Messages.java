/*******************************************************************************
 * Copyright (c) 2011 WindRiver Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     WindRiver Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.importexport.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	public static String Column_Id;
	public static String Column_Name;
	public static String Column_Version;
	public static String ExportPage_Title;
	public static String ExportPage_TryAgainQuestion;
	public static String ExportPage_Description;
	public static String ExportPage_Label;
	public static String ExportPage_DEST_ERRORMESSAGE;
	public static String ExportPage_ERROR_CONFIG;
	public static String ExportPage_Fail;
	public static String ExportPage_FILEDIALOG_TITLE;
	public static String ExportPage_FixSuggestion;
	public static String ExportPage_EntriesNotInRepo;
	public static String EXTENSION_ALL;
	public static String EXTENSION_ALL_NAME;
	public static String EXTENSION_p2F_NAME;
	public static String EXTENSION_p2F;
	public static String ExportPage_LABEL_EXPORTFILE;
	public static String ExportPage_SuccessWithProblems;
	public static String ExportWizard_ConfirmDialogTitle;
	public static String ExportWizard_OverwriteConfirm;
	public static String ExportWizard_WizardTitle;
	public static String ImportPage_DESCRIPTION;
	public static String ImportPage_DEST_ERROR;
	public static String ImportPage_DESTINATION_LABEL;
	public static String ImportPage_FILEDIALOG_TITLE;
	public static String ImportPage_FILENOTFOUND;
	public static String AbstractImportPage_HigherVersionInstalled;
	public static String ImportPage_InstallLatestVersion;
	public static String ImportPage_QueryFeaturesJob;
	public static String AbstractImportPage_SameVersionInstalled;
	public static String AbstractPage_ButtonDeselectAll;
	public static String AbstractPage_ButtonSelectAll;
	public static String ImportPage_TITLE;
	public static String ImportWizard_CannotQuerySelection;
	public static String ImportWizard_WINDOWTITLE;
	public static String Page_BUTTON_BROWSER;
	public static String PAGE_NOINSTALLTION_ERROR;
	public static String ImportFromInstallationPage_DESTINATION_LABEL;
	public static String ImportFromInstallationPage_DIALOG_TITLE;
	public static String ImportFromInstallationPage_INVALID_DESTINATION;
	public static String ImportFromInstallationPage_DIALOG_DESCRIPTION;
	public static String ImportFromInstallationPage_SELECT_COMPONENT;

	public static String io_IncompatibleVersion;
	public static String io_parseError;
	public static String Replicator_ExportJobName;
	public static String Replicator_InstallFromLocal;
	public static String Replicator_NotFoundInRepository;
	public static String Replicator_SaveJobName;

	static {
		NLS.initializeMessages("org.eclipse.equinox.internal.p2.importexport.internal.messages", Messages.class); //$NON-NLS-1$
	}
}
