/*******************************************************************************
 * Copyright (c) 2007, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rapicorp, Inc - prompt to install debian package
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.p2.touchpoint.natives.messages"; //$NON-NLS-1$

	static {
		// load message values from bundle file and assign to fields below
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	public static String BackupStore_0_of_1_items_restored;
	public static String BackupStore_backupCopy_closed_store;
	public static String BackupStore_can_not_close_tcp_port;
	public static String BackupStore_can_not_copy_directory;
	public static String BackupStore_can_not_create_dummy;
	public static String BackupStore_can_not_delete_after_copy_0;
	public static String BackupStore_can_not_delete_tmp_file;
	public static String BackupStore_can_not_remove;
	public static String BackupStore_can_not_remove_bu_directory;
	public static String BackupStore_directory_file_mismatch;
	public static String BackupStore_directory_not_empty;
	public static String BackupStore_errors_while_restoring_see_log;
	public static String BackupStore_externally_modified_0_of_1_restored;
	public static String BackupStore_file_directory_mismatch;
	public static String BackupStore_file_not_found;
	public static String BackupStore_manual_restore_needed;
	public static String BackupStore_missing_backup_directory;
	public static String BackupStore_not_a_directory;
	public static String BackupStore_restore_closed_store;
	public static String BlockMacUpdate_0;
	public static String BlockMacUpdate_1;

	public static String action_0_failed_file_1_doesNotExist;
	public static String artifact_not_available;
	public static String artifact_repo_not_found;
	public static String could_not_obtain_download_cache;
	public static String download_cache_not_writeable;
	public static String unzipping;
	public static String param_not_set;
	public static String chmod_param_cant_be_set_together;
	public static String copy_failed;
	public static String failed_backup_restore;
	public static String backup_file_failed;
	public static String Cannot_Check_Package;
	public static String Cannot_Find_status;
	public static String Error_list_children_0;
	public static String Incorrect_Command;
	public static String link_failed;
	public static String mkdir_failed;
	public static String NativePackageExtractionApplication_FolderNotFound;
	public static String NativePackageExtractionApplication_MissingParameters;
	public static String NativePackageExtractionApplication_PersistencePb;
	public static String NativePackageExtractionApplication_MissingValue;
	public static String PromptForNative_InstallText;
	public static String PromptForNative_IntroText;
	public static String PromptForNative_DialogTitle;
	public static String rmdir_failed;
	public static String Util_Invalid_Zip_File_Format;
	public static String Util_Error_Unzipping;

}
