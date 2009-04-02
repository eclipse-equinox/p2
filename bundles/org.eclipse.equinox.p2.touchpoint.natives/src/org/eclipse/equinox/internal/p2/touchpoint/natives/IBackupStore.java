/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.touchpoint.natives;

import java.io.File;
import java.io.IOException;

public interface IBackupStore {

	/**
	 * Backup the file.
	 * Calling this method with a file that represents a directory is equivalent to calling 
	 * {@link #backupDirectory(File)}.
	 * 
	 * A file (path) can only be backed up once per IBackupStore instance.
	 * 
	 * If a directory is first backed up, and later replaced by a regular file, and this file
	 * is also backed up (or vice versa) - an {@link IllegalArgumentException} is thrown
	 * 
	 * A backup can not be performed on a closed IBackupStore. 
	 * 
	 * @param file - the file (or directory) to backup
	 * @return true if the file was backed up, false if this file (path) has already been backed up (the file is not moved to the store).
	 * @throws IOException - if the backup operation fails, or the file does not exist
	 * @throws IllegalStateException - if the IBackupStore has been closed
	 * @throws IllegalArgumentException - on type mismatch (file vs. directory) of earlier backup, or if file does not exist 
	 */
	public boolean backup(File file) throws IOException;

	/**
	 * Same as {@link #backup(File)} except that a copy is kept in the original location.
	 * Can not be used to copy directories.
	 * @param file to backup and copy
	 * @return true if the file was backed up, false if this file (path) has already been backed up (the file is not moved to the store).
	 * @throws IOException - if the backup operation fails, if the file does not exist, or if the copy can not be created.
	 * @throws IllegalStateException - if the IBackupStore has been closed
	 * @throws IllegalArgumentException - on type mismatch (file vs. directory) of earlier backup, or if file is a directory. 
	 */
	public boolean backupCopy(File file) throws IOException;

	/**
	 * Performs backup of an empty directory. The directory must be empty before it can be backed up (i.e.
	 * similar to a delete of a directory). Backup the files of the directory first.
	 * A call to backup a directory is really only needed for empty directories as a restore
	 * of a file will also restore all of its parent directories.
	 * @param file - the (empty) directory to back up
	 * @return true if the directory was moved to backup. false if the directory was already backed up and remains.
	 * @throws IllegalArgumentException if file is not a directory, or is not empty.
	 * @throws IOException if directory can not be moved to the backup store, or if the directory is not writeable
	 */
	public boolean backupDirectory(File file) throws IOException;

	/**
	 * Discards and closes this BackupStore. Does nothing if this store is already
	 * restored or discarded.
	 */
	public void discard();

	/**
	 * Restores all backup files from backup store.
	 * Note that restore of a (non directory) file deletes an existing file or directory found
	 * in the restore location.
	 * When the backup has been restored it can not be
	 * used for further backup or restore.
	 * 
	 * If there are unrestorable items (non writeable directories, or general IO exceptions) these items
	 * should be written to the log, and the backup copies should be retained
	 * for manual restore.
	 * 
	 * @throws IOException if the backup was not fully restored - unrestored items should have been logged.
	 * @throws IllegalStateException if the backup is already closed.
	 */
	public void restore() throws IOException;

	/**
	 * Returns the unique backup name (this is the name of generated backup directories).
	 * @return the backup name.
	 */
	public String getBackupName();

	/**
	 * Backs up a file, or everything under a directory.
	 * 
	 * @param file - file to backup or directory
	 * @throws IOException if backup operation failed
	 */
	public void backupAll(File file) throws IOException;

	/**
	 * Backs up a file, or everything under a directory.
	 * A copy of the backup is left in the original place.
	 * @param file
	 * @throws IOException
	 */
	public void backupCopyAll(File file) throws IOException;
}