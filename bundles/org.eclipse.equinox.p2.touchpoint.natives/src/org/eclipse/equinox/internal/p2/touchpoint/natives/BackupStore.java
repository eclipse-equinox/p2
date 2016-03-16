/*******************************************************************************
 * Copyright (c) 2009, 2016 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *     SAP AG - Ongoing development
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.touchpoint.natives;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.osgi.util.NLS;

/**
 * Stores files by copying them to a uniquely named temporary directory.
 * The BackupStore remembers filenames and can recreate them in their original location.
 * 
 * <h3>Usage</h3>
 * The user of this class should instantiate the BackupStore with some prefix that is 
 * meaningful to a human. Uniqueness is obtained without the prefix - the prefix is used to 
 * be able to differentiate between different backup directories by a human (in case of crashes etc).
 * 
 * If instantiated with a directory this directory will be used to store the backup root directory. If
 * this directory is null, the users home directory is used by default.
 * 
 * Once instantiated, use the {@link #backup(File)} and {@link #backupDirectory(File)} methods
 * to move files to backup instead of deleting them. A file that
 * is backed up should not be deleted - it is simply moved out of the way. 
 * Use {@link #backupCopy(File)} to
 * move the file out of harms way, but keep a copy of it in the original location.
 * The methods {@link #backupAll(File)} and {@link #backupCopyAll(File)} backs up an entire structure.
 * 
 * When backup is finished - the user should either call {@link #restore()} to put all 
 * of the files back, or call {@link #discard()} to remove all of the backed up "copies".
 * 
 * If {@link #restore()} or {@link #discard()} is not called the backup files will never be deleted.
 * 
 * The backup store does not synchronize directories - actions that write new files are
 * responsible for removing them. Overwriting existing files should be done by first backing
 * up the file, and then creating a new file. Modifying a file, should be done by 
 * using {@link #backupCopy(File)} or 
 * first making a copy, then backing up the original, and then renaming the copy.
 *  
 * <h3>Read Only and Permissions</h3>
 * Directories that are read only (to current user) can not be backed up.
 * Backup is performed using {@link File#renameTo(File)} and handling of permissions
 * is operating system dependent. It is expected that a Un*x type system retains the
 * permissions as a file is moved to the backup store and later gets restored.
 * Backup directories are created as they are needed and will (at least on Un*x) inherit the
 * permissions from its parent directory. 
 * 
 * If a rename can not be performed, the backup store will make a copy and delete the original
 * file. This makes it possible to backup and restore across volume boundaries.
 * 
 * When restoring directories they
 * will be created with permissions in a platform specific way (on UN*IX they will inherit the permissions 
 * of the parent directory).
 * 
 * <h3>Checkpointing</h3> 
 * Checkpointing (i.e. to be able to rollback to a particular point) can be implemented by using
 * multiple instances of BackupStore. The client code will need to remember the individual order
 * among the backup stores.
 * 
 * <h3>Restartability</h3>
 * Not implemented - it is possible to obtain the name of the backup directories,
 * so manual restore is possible after a crash. An idea is to add persistence to a file, and
 * be able to read it back in again.
 * 
 * <h3>A note about exceptions</h3>
 * In general {@link IllegalArgumentException} is thrown when attempting an operation
 * that is considered "wrong use", and an {@link IllegalStateException} or subclass thereof is thrown on an overall
 * wrong use of BackupStore (i.e. attempt to backup when store has been restored). Some cases of
 * "wrong use" can not be differentiated from I/O errors (like a "file not found" as this could
 * be caused by an entire disk disappearing - in these case an {@link IOException} is thrown.
 * 
 * <h3>Implementation Note</h3>
 * The backup root directory will contain folders that reflects file system roots. These are encoded using 
 * "_" for the UNI*X root directory, "__" for a Windows network mounted directory, and single "drive letter" folders
 * corresponding to Windows drive letters. Typically, on UN*X there will only be a "_" directory in the backup root,
 * and on windows there will typically be a single directory called "C".
 * 
 *
 */
public class BackupStore implements IBackupStore {

	private static final String BACKUP_FILE_EXTENSION = ".p2bu"; //$NON-NLS-1$

	/**
	 * The name to use for a directory that represents leading separator (i.e. "/" or "\").
	 */
	private static final String ROOTCHAR = "_"; //$NON-NLS-1$

	/**
	 * Map of directory File to backup root (File) - the backup root has 
	 * a directory named {@link #backupName} where the backup is found.
	 */
	//private Map backups = new HashMap();
	private final File backupRoot;

	/**
	 * The name of the backup directory (no path - relative to the backup root).
	 */
	private String backupName;

	/**
	 * The name of a dummy file used to backup empty directories
	 */
	private String dummyName;

	/**
	 * A server socket that is used to obtain a port (a shared resource on this machine)
	 * and thus create a unique number. Used as part of the unique id of backup directories
	 * and probe files.
	 */
	private ServerSocket socket = null;

	/**
	 * Counter of how many files where backed up. Used as a simple check mechanism if
	 * everything was restored (a guard against manual/external tampering with the backup directories).
	 */
	private long backupCounter;

	/**
	 * Counter of how many files where restored. See {@link #backupCounter}.
	 */
	private long restoreCounter;

	/**
	 * Flag indicating if this BackupStore has been restored or canceled.
	 */
	private boolean closed;

	private Map<String, String> renamedInPlace = new HashMap<String, String>();

	/**
	 * Generates a BackupStore with a default prefix of ".p2bu" for backup directory and
	 * probe file. 
	 * The full id of the store is on the format "prefix_hextime_hexIPport" 
	 * - see {@link #genUnique()} for more info.
	 */
	public BackupStore() {
		this(null, BACKUP_FILE_EXTENSION);
	}

	/**
	 * Generates a BackupStore with a specified prefix for backup directories and
	 * probe file.
	 * The full id of the store is on the format "prefix_hextime_hexipport" 
	 * - see {@link #genUnique()} for more info.
	 * 
	 * @param buParentDirectory - name of directory where the backup directory should be created - if null, java.io.tmpdir is used
	 * @param prefix - prefix used for human identification of backup directories
	 */
	public BackupStore(File buParentDirectory, String prefix) {
		if (buParentDirectory == null)
			buParentDirectory = new File(System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
		backupRoot = buParentDirectory;

		// generate a name for the backup store and the dummy file used for empty directories
		String unique = genUnique();
		dummyName = prefix + "d_" + unique; //$NON-NLS-1$
		backupName = prefix + "_" + unique; //$NON-NLS-1$
		backupCounter = 0;
		restoreCounter = 0;
		closed = false;
	}

	/**
	 * Since a socket port is used to create a unique number, the socket
	 * must be closed if this instance is garbage collected and the user
	 * of the instance has not either restored or discarded.
	 */
	protected void finalize() throws Throwable {
		try {
			if (socket != null && !socket.isClosed())
				socket.close();
		} finally {
			super.finalize();
		}
	}

	/**
	 * Returns the unique backup name (this is the name of generated backup directories).
	 * @return the backup name.
	 */
	public String getBackupName() {
		return backupName;
	}

	public File getBackupRoot() {
		return backupRoot;
	}

	/**
	 * Backup the file by moving it to the backup store (for later (optional) restore).
	 * Calling this method with a file that represents a directory is equivalent to calling 
	 * {@link #backupDirectory(File)}.
	 * 
	 * A file (path) can only be backed up once per BackupStore instance.
	 * When the file is backed up, it is moved to a directory under this BackupStore instance's directory 
	 * with a relative path corresponding to the original relative path from the backup root e.g.
	 * the file /A/B/C/foo.txt could be moved to /A/.p2bu_ffffff_ffffff/B/C/foo.txt when /A is the
	 * backup root.
	 * 
	 * If a directory is first backed up, and later replaced by a regular file, and this file
	 * is backed up (or vice versa) - an {@link IllegalArgumentException} is thrown
	 * 
	 * A backup can not be performed on a closed BackupStore. 
	 * 
	 * @param file - the file (or directory) to backup
	 * @return true if the file was backed up, false if this file (path) has already been backed up (the file is not moved to the store).
	 * @throws IOException - if the backup operation fails, or the file does not exist
	 * @throws ClosedBackupStoreException - if the BackupStore has been closed
	 * @throws IllegalArgumentException - on type mismatch (file vs. directory) of earlier backup, or if file does not exist 
	 */
	public boolean backup(File file) throws IOException {
		if (closed)
			throw new ClosedBackupStoreException("Can not perform backup()"); //$NON-NLS-1$
		if (!file.exists())
			throw new IOException(NLS.bind(Messages.BackupStore_file_not_found, file.getAbsolutePath()));
		if (file.isDirectory())
			return backupDirectory(file);
		file = makeParentCanonical(file);
		File buFile = getBackupFile(file);
		// already backed up, but was a directory = wrong usage
		if (buFile.isDirectory())
			throw new IllegalArgumentException(NLS.bind(Messages.BackupStore_directory_file_mismatch, buFile.getAbsolutePath()));
		// has already been backed up - can only be done once with one BackupStore
		if (buFile.exists()) {
			// although backed up, the file can be still on the file system when, for example,
			// two IUs are unzipping their contents to the same location and share a few common file,
			// which have to be removed twice
			if (file.exists() && !file.delete())
				throw new IOException(NLS.bind(Messages.BackupStore_can_not_remove, file.getAbsolutePath()));
			return false;
		}

		moveToBackup(file, buFile);

		return true;
	}

	/**
	 * Move/rename file to a backup file. Callers of the method must have ensured that the source file exists and the
	 * backup file has not been created yet.
	 * 
	 * @param file source file to move; should already exist and must not be directory
	 * @param buFile destination backup file to move to; should not exist and must be a directory
	 * @throws IOException if the backup operation fails
	 */
	protected void moveToBackup(File file, File buFile) throws IOException {
		// make sure all of the directories exist / gets created
		buFile.getParentFile().mkdirs();
		if (buFile.getParentFile().exists() && !buFile.getParentFile().isDirectory())
			throw new IllegalArgumentException(NLS.bind(Messages.BackupStore_file_directory_mismatch, buFile.getParentFile().getAbsolutePath()));
		if (moveToBackupStore(file, buFile)) {
			backupCounter++;
			return;
		}
		// could not move - this can happen because source and target are on different volumes, or
		// that source is locked "in use" on a windows machine. The copy will work across volumes,
		// but the locked file will fail on the subsequent delete.
		//
		// Rename in place
		if (isEclipseExe(file))
			renameInPlace(file);
		else {
			Util.copyStream(new FileInputStream(file), true, new FileOutputStream(buFile), true);
			backupCounter++;
		}
		if (file.exists() && !file.delete())
			throw new IOException(NLS.bind(Messages.BackupStore_can_not_delete_after_copy_0, file));
	}

	private boolean isEclipseExe(File file) {
		String launcher = System.getProperty("eclipse.launcher"); //$NON-NLS-1$
		if (launcher != null) {
			String base = new File(launcher).getName();
			if (file.getName().equalsIgnoreCase(base))
				return true;
		}
		return file.getName().equalsIgnoreCase("eclipse.exe") || file.getName().equalsIgnoreCase("eclipsec.exe"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected boolean moveToBackupStore(File file, File buFile) {
		if (file.renameTo(buFile)) {
			// if the original file still exists, we have a problem.
			if (file.exists()) {
				// If the renamed work, but the file still exists, remove the backup
				// and return false
				if (buFile.exists())
					buFile.delete();
			} else {
				return true;
			}
		}
		return false;
	}

	protected void renameInPlace(File file) {
		String newName = file.getAbsolutePath() + getTimeStamp() + BACKUP_FILE_EXTENSION;
		renamedInPlace.put(file.getAbsolutePath(), newName);
		file.renameTo(new File(newName));
	}

	protected String getTimeStamp() {
		return "-" + new Date().getTime(); //$NON-NLS-1$
	}

	private File getBackupFile(File file) {
		File buRoot = backupRoot;
		File buDir = new File(buRoot, backupName);
		// create the relative path from root and use that in buDir
		File buFile = new File(buDir, makeRelativeFromRoot(file).getPath());
		return buFile;
	}

	/**
	 * Backs up a file, or everything under a directory.
	 * 
	 * @param file - file to backup or directory
	 * @throws IOException if backup operation failed
	 */
	public void backupAll(File file) throws IOException {
		if (!file.exists())
			return;
		file = makeParentCanonical(file);
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null)
				for (int i = 0; i < files.length; i++)
					backupAll(files[i]);
		}
		backup(file);
	}

	/**
	 * Backs up a file, or everything under a directory.
	 * A copy of the backup is left in the original place.
	 * @param file
	 * @throws IOException
	 */
	public void backupCopyAll(File file) throws IOException {
		if (!file.exists())
			return;
		file = makeParentCanonical(file);
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null)
				for (int i = 0; i < files.length; i++)
					backupCopyAll(files[i]);
			// if directory was empty, it needs to be backed up and then recreated
			//
			if (files == null || files.length == 0) {
				backupDirectory(file);
				file.mkdir();
			}
		} else
			backupCopy(file);
	}

	/**
	 * Backup the file by moving it to the backup store (for later (optional) restore) but leaving
	 * a copy of the contents in the original location.
	 * Calling this method with a file that represents a directory throws an {@link IllegalArgumentException}.
	 * 
	 * A file (path) can only be backed up once per BackupStore instance.
	 * When the file is backed up, it is moved to a directory under this BackupStore instance's directory 
	 * with a relative path corresponding to the original relative path from the backup root e.g.
	 * the file /A/B/C/foo.txt could be moved to /A/.p2bu_ffffff_ffffff/B/C/foo.txt when /A is the
	 * backup root.
	 * 
	 * If a directory is first backed up, and later replaced by a regular file, and this file
	 * is backed up (or vice versa) - an {@link IllegalArgumentException} is thrown
	 * 
	 * A backup can not be performed on a closed BackupStore. 
	 * 
	 * @param file - the file (or directory) to backup
	 * @return true if the file was backed up, false if this file (path) has already been backed up (the file is not moved to the store).
	 * @throws IOException - if the backup operation fails, or the file does not exist
	 * @throws ClosedBackupStoreException - if the BackupStore has been closed
	 * @throws IllegalArgumentException - on type mismatch (file vs. directory) of earlier backup, or if file is a Directory
	 */
	public boolean backupCopy(File file) throws IOException {
		if (closed)
			throw new ClosedBackupStoreException(Messages.BackupStore_backupCopy_closed_store);
		if (!file.exists())
			throw new IOException(NLS.bind(Messages.BackupStore_file_not_found, file.getAbsolutePath()));
		if (file.isDirectory())
			throw new IllegalArgumentException(NLS.bind(Messages.BackupStore_can_not_copy_directory, file.getAbsolutePath()));
		file = makeParentCanonical(file);
		//File buRoot = backupRoot;
		// File buRoot = findBackupRoot(file);
		File buDir = new File(backupRoot, backupName);
		// move the file
		// create the relative path from root and use that in buDir
		File buFile = new File(buDir, makeRelativeFromRoot(file).getPath());
		// already backed up, but was a directory = wrong usage
		if (buFile.isDirectory())
			throw new IllegalArgumentException(NLS.bind(Messages.BackupStore_directory_file_mismatch, buFile.getAbsolutePath()));
		// has already been backed up - can only be done once with one BackupStore
		if (buFile.exists())
			return false;

		// make sure all of the directories exist / gets created
		buFile.getParentFile().getCanonicalFile().mkdirs();
		if (buFile.getParentFile().exists() && !buFile.getParentFile().isDirectory())
			throw new IllegalArgumentException(NLS.bind(Messages.BackupStore_file_directory_mismatch, buFile.getParentFile().getAbsolutePath()));

		// just make a copy - one has to be made in one direction anyway
		// A renameTo followed by a copy is preferred as it preserves file permissions on the moved file
		// but it is easier to just copy and keep original.
		Util.copyStream(new FileInputStream(file), true, new FileOutputStream(buFile), true);
		backupCounter++;
		return true;
	}

	/**
	 * Performs backup of an empty directory. The directory must be empty before it can be backed up (i.e.
	 * similar to a delete of a directory). Backup the files of the directory first.
	 * A call to backup a directory is really only needed for empty directories as a restore
	 * of a file will also restore all of its parent directories.
	 * @param file - the (empty) directory to back up
	 * @return true if the directory was moved to backup. false if the directory was already backed up
	 * @throws IllegalArgumentException if file is not a directory, or is not empty.
	 * @throws IOException if directory can not be moved to the backup store, or if the directory is not writeable
	 */
	public boolean backupDirectory(File file) throws IOException {
		if (!file.isDirectory())
			throw new IllegalArgumentException(NLS.bind(Messages.BackupStore_not_a_directory, file.getAbsolutePath()));
		file = makeParentCanonical(file);
		if (file.list().length != 0)
			throw new IllegalArgumentException(NLS.bind(Messages.BackupStore_directory_not_empty, file.getAbsolutePath()));
		// the easiest way is to create a dummy file and back that up (the dummy is simply ignored when restoring).
		File dummy = new File(file, dummyName);
		dummy = makeParentCanonical(dummy);
		File buFile = getBackupFile(dummy);
		boolean backedUp = buFile.exists();
		// backup only if the folder has not been already backed up;
		// this can happen if, for example, two IUs unzip to the same folder and then want to delete it
		if (!backedUp) {
			if (closed)
				throw new ClosedBackupStoreException("Can not perform backup()"); //$NON-NLS-1$
			if (!dummy.createNewFile())
				throw new IOException(NLS.bind(Messages.BackupStore_can_not_create_dummy, dummy.getAbsolutePath()));
			moveToBackup(dummy, buFile);
		}
		// previous checks have verified that the directory exists
		if (!file.delete())
			throw new IOException(NLS.bind(Messages.BackupStore_can_not_remove, dummy.getAbsolutePath()));
		// will return true if the directory was already backed up at the beginning of the operation and false otherwise
		return !backedUp;
	}

	/**
	 * Restores all backup files from backup store.
	 * Note that restore of a (non directory) file deletes an existing file or directory found
	 * in the restore location.
	 * When the backup has been restored this BackupStore instance is closed and can not be
	 * used for further backup or restore.
	 * 
	 * If there are unrestorable items (non writable directories, or general IO exceptions) these items
	 * are written to the log, and the backup copies remain in the file system and can be manually restored
	 * (using a simple zip of the backup directory, and an unzip to the buRoot once the problem has been corrected).
	 * 
	 * @throws IOException if the backup was not fully restored - unrestored items have been logged.
	 * @throws ClosedBackupStoreException if the backup is already closed.
	 */
	public void restore() throws IOException {
		if (closed)
			throw new ClosedBackupStoreException(Messages.BackupStore_restore_closed_store);
		// put back all files 
		// collect things that could not be restored (so final status can be reported)
		Set<File> unrestorable = new HashSet<File>();
		boolean restored = true;
		if (!backupRoot.exists()) {
			logError(NLS.bind(Messages.BackupStore_missing_backup_directory, backupRoot.getAbsolutePath()));
			restored = false;
		} else
			restoreRoots(new File(backupRoot, backupName), unrestorable);

		logUnrestorables(unrestorable);
		if (unrestorable.size() > 0)
			restored = false;
		close(restored);
		closed = true;
	}

	private void logUnrestorables(Set<File> unrestorable) {
		// if there are unrestorable units log them
		//
		if (unrestorable != null && unrestorable.size() > 0) {
			for (File file : unrestorable)
				logError(NLS.bind(Messages.BackupStore_manual_restore_needed, file.getAbsolutePath()));
		}
	}

	/**
	 * Discards and closes this BackupStore. Does nothing if this store is already
	 * restored or discarded.
	 */
	public void discard() {
		if (closed)
			return;
		closeSocket();
		removeBackups();
		closed = true;
	}

	private void close(boolean fullyRestored) throws IOException {
		closeSocket();
		// check external tampering with backup store
		if (backupCounter != restoreCounter) {
			if (!fullyRestored)
				logError(NLS.bind(Messages.BackupStore_0_of_1_items_restored, new Long(restoreCounter), new Long(backupCounter)));
			else {
				logError(NLS.bind(Messages.BackupStore_externally_modified_0_of_1_restored, new Long(restoreCounter), new Long(backupCounter)));
				fullyRestored = false;
			}
		}
		if (!fullyRestored)
			throw new IOException(Messages.BackupStore_errors_while_restoring_see_log);
		// everything has been restored - the backup can now be removed
		removeBackups();
	}

	private void closeSocket() {
		if (socket != null && !socket.isClosed())
			try {
				socket.close();
			} catch (IOException e) { /* ignored */
				logWarning(NLS.bind(Messages.BackupStore_can_not_close_tcp_port, Integer.valueOf(socket.getLocalPort())));
			}
	}

	private void removeBackups() {
		File buRoot = new File(backupRoot, backupName);
		if (!fullyDelete(buRoot))
			logWarning(NLS.bind(Messages.BackupStore_can_not_remove_bu_directory, buRoot.getAbsolutePath()));
	}

	private static void logWarning(String message) {
		LogHelper.log(createWarning(message));
	}

	private static IStatus createWarning(String message) {
		return new Status(IStatus.WARNING, Activator.ID, message);
	}

	private static void logError(String message) {
		LogHelper.log(createError(message));
	}

	private static IStatus createError(String message) {
		return new Status(IStatus.ERROR, Activator.ID, message);
	}

	/**
	 * Deletes a file, or a directory with all of it's children.
	 * @param file the file or directory to fully delete
	 * @return true if, and only if the file is deleted without errors
	 */
	private boolean fullyDelete(File file) {
		if (!file.exists())
			return true;
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children == null)
				return false;
			for (int i = 0; i < children.length; i++)
				if (!fullyDelete(new File(file, children[i].getName())))
					return false;
		}
		return file.delete();
	}

	private void restore(File root, File buRoot, Set<File> unrestorable) {
		File[] children = buRoot.listFiles();
		if (children == null) { // error - can't read the backup directory
			unrestorable.add(buRoot);
			return;
		}
		for (int i = 0; i < children.length; i++) {
			File bu = new File(buRoot, children[i].getName());
			File target = new File(root, bu.getName());
			if (bu.isDirectory()) {
				if (!target.exists() && !target.mkdir()) {
					unrestorable.add(bu);
					continue; // give up on this branch
				} else if (target.exists() && !target.isDirectory()) {
					// ouch, there is a file where we need a directory
					// that must be deleted.
					target.delete();
					if (!target.mkdir()) {
						unrestorable.add(bu);
						continue; // give up on branch
					}
				}
				restore(target, bu, unrestorable);
			} else {
				// do not restore the dummies (as they are used to trigger creation of
				// empty directories and are not wanted in the restored location.
				if (bu.getName().equals(dummyName)) {
					restoreCounter++; // count of the restored directory in this case.
					continue;
				}
				// if the original was overwritten by something and this file was not
				// removed, it needs to be deleted now. If it can't be deleted, the
				// renameTo will fail, and the bu is reported as not restorable.
				// fullyDelete will remove a directory completely - we are restoring a file so it can 
				// not be kept.
				if (target.exists())
					fullyDelete(target);

				// rename if possible, but must copy if not possible to just rename
				if (!bu.renameTo(target)) {
					// did not work to rename, probably because of volume boundaries. Try to copy instead,
					try {
						Util.copyStream(new FileInputStream(bu), true, new FileOutputStream(target), true);
						restoreCounter++; // consider it restored
					} catch (FileNotFoundException e) {
						unrestorable.add(bu);
						continue;
					} catch (IOException e) {
						unrestorable.add(bu);
						continue;
					}
					if (!bu.delete()) { // cleanup
						// could not remove the backup after copy - log, safe to remove manually
						logWarning(NLS.bind(Messages.BackupStore_can_not_delete_tmp_file, bu.getAbsolutePath()));
					}
				} else
					restoreCounter++;
			}
		}
	}

	/**
	 * Restores everything backed up in the buRoot. Responsible for decoding the specially named root
	 * target directories (i.e. _/, __/, C/, etc.) into the real system names.
	 * @param buRoot
	 * @param unrestorable
	 */
	private void restoreRoots(File buRoot, Set<File> unrestorable) {
		File[] children = buRoot.listFiles();
		if (children == null) { // error - can't read the backup directory
			unrestorable.add(buRoot);
			return;
		}
		for (int i = 0; i < children.length; i++) {
			// Names are  root-chars, or drive letters in the root bu directory 
			String name = children[i].getName();
			String rName = name;
			String prefix = ""; //$NON-NLS-1$
			while (rName.startsWith(ROOTCHAR)) {
				prefix += File.separator;
				rName = rName.substring(1);
			}
			if (prefix.length() < 1) {
				// The name is a drive name
				rName = rName + ":" + File.separator; //$NON-NLS-1$
			} else
				rName = prefix + rName;
			// File root = new File(rName);
			File bu = new File(buRoot, name);
			File target = new File(rName);
			if (!bu.isDirectory()) {
				// the roots should all be directories - so this can only happen if someone manually
				// stored files in the backup root - mark them as unrestorable and continue.
				unrestorable.add(bu);
				continue;
			}
			// the backup roots are system roots, and can not be created - but check root is directory and exists.
			// (Network drives could have gone away etc).
			//
			if (!(target.exists() && target.isDirectory())) {
				unrestorable.add(bu);
				continue; // give up on this branch
			}
			// then perform a recursive restore
			restore(target, bu, unrestorable);
		}
		restoreRenamedFiles(unrestorable);
	}

	private void restoreRenamedFiles(Set<File> unrestorable) {
		for (Entry<String, String> entry : renamedInPlace.entrySet()) {
			File bu = new File(entry.getValue());
			if (!bu.renameTo(new File(entry.getKey())))
				unrestorable.add(bu);
		}
	}

	private static long msCounter = 0;

	/**
	 * Generates a unique hex string by taking currentTimeMillis + sequence 
	 * number at the end allowing for 32 numbers to be generated per ms.
	 * This is sufficient uniqueness in the same VM. (And is still just a fallback solution 
	 * if there is no access to a TCP port)
	 * 
	 * To make number unique over multiple VMs - the PID of the process would be enough, but
	 * it is complicated to get hold of - a separate program must be launched and its PPID 
	 * investigated. There is no standard API in Java to get the PID. Instead, a socket port is bound
	 * to ensure local uniqueness.
	 * 
	 * To make number unique across multiple hosts (we may be provisioning over NFS), the
	 * 48 LS bits of the IP address is used (this is more than enough for an IPv4 address). 
	 * (If there is no IP address, the machine is not on a
	 * network) - unfortunately the MAC address can not be used as this requires Java 6 (where 
	 * there also is a UUID that should be used instead of this method).
	 * 
	 * This method needs to be modified when IPv6 addressing is the norm - at that time, the
	 * restriction on Java 1.4 has hopefully been lifted, and it is possible to use the MAC address,
	 * or the UUID provided since java 1.6
	 * 
	 * @return a unique string
	 */
	private String genUnique() {
		// use 5 LSB bits for counter within ms - i.e. 32 instances can be created
		// per millisecond.
		long timePart = (System.currentTimeMillis() << 5) | (msCounter++ & 31);
		// can't use the MAC address - but take IP address if provisioning across NFS
		long ipPart = 0;
		try {
			// the returned address can be 32 bits IPv4, or 128 bits IPv6 (?)
			// In any case use the LSB bits (as many as will fit
			byte[] address = InetAddress.getLocalHost().getAddress();
			for (int i = 0; i < address.length; i++)
				ipPart = ((ipPart << 8) | (address[i] & 0xff));
		} catch (UnknownHostException e) {
			// there is no IP address, and there and hence no concurrency from other machines.
			// use the default ip part 0
		}
		int port = 0;
		try {
			// TODO: this should be replaced by InetAddress.getLoopbackAddress() when 1.7 compatibility is OK
			// on a system where solely IPv6 is available this address resolution will fail:
			socket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")); //$NON-NLS-1$
			port = socket.getLocalPort();
		} catch (IOException e) {
			try {
				if (socket != null)
					socket.close();
			} catch (IOException e1) { // ignore failure to close - 
			}
			// use a random number as port in this case
			port = new Random().nextInt() & 0xffff;
		}
		// port is never > 0xffff
		long aPart = (ipPart << 16) | (port & 0xffff);
		return Long.toHexString(timePart) + "_" + Long.toHexString(aPart); //$NON-NLS-1$

	}

	/**
	 * Turns a file into a "relativized" absolute file.
	 * A leading "root" is transformed to the ROOTCHAR character. On Windows, network mapped drives starts
	 * with two separators - and are encoded as two ROOTCHAR.
	 * e.g.
	 * \\Host\C$\File becomes __\Host\C$\File
	 * /users/test/file becomes _/users/test/file
	 * C:/somewhere/file becomes C/somewhere/file
	 * 
	 * @param file
	 * @return a relativized absolute abstract file
	 */
	private File makeRelativeFromRoot(File file) {
		File absolute = file.getAbsoluteFile();
		String path = absolute.getPath();
		String prefix = ""; //$NON-NLS-1$
		while (path.startsWith(File.separator)) {
			prefix += ROOTCHAR;
			path = path.substring(1);
		}
		if (prefix.length() > 0) {
			path = prefix + File.separator + path;
			return new File(path);
		}
		// it is a windows drive letter first.
		// Transform C:/foo to C/foo
		//
		int idx = path.indexOf(":"); //$NON-NLS-1$
		if (idx < 1)
			throw new InternalError("File is neither absolute nor has a drive name: " + path); //$NON-NLS-1$
		path = path.substring(0, idx) + path.substring(idx + 1);

		return new File(path);
	}

	/** 
	 * The parent path may include ".." as a directory name - this must be made canonical. But if the file itself is
	 * a symbolic link, it should not be resolved. 
	 */
	private File makeParentCanonical(File file) throws IOException {
		return new File(file.getParentFile().getCanonicalFile(), file.getName());
	}
}
