/*******************************************************************************
 * Copyright (c) 2009, 2018 Cloudsmith Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *     SAP AG - Ongoing development
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.touchpoint.natives;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.stream.Collectors.joining;
import static org.eclipse.equinox.internal.p2.touchpoint.natives.Util.logError;
import static org.eclipse.equinox.internal.p2.touchpoint.natives.Util.logWarning;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Stream;
import org.eclipse.osgi.util.NLS;

/**
 * Stores files by moving them to a uniquely named temporary directory.
 * 
 * TheBackupStore remembers filenames and can recreate them in their original
 * location.
 *
 * <h3>Usage</h3> The user of this class should instantiate the BackupStore with
 * some prefix that is meaningful to a human. Uniqueness is obtained without the
 * prefix - the prefix is used to be able to differentiate between different
 * backup directories by a human (in case of crashes etc).
 *
 * If instantiated with a directory this directory will be used to store the
 * backup root directory. If this directory is null, the users home directory is
 * used by default.
 *
 * Once instantiated, use the {@link #backup(File)} and
 * {@link #backupDirectory(File)} methods to move files to backup instead of
 * deleting them. A file that is backed up should not be deleted - it is simply
 * moved out of the way. Use {@link #backupCopy(File)} to move the file out of
 * harms way, but keep a copy of it in the original location. The methods
 * {@link #backupAll(File)} and {@link #backupCopyAll(File)} backs up an entire
 * structure.
 *
 * When backup is finished - the user should either call {@link #restore()} to
 * put all of the files back, or call {@link #discard()} to remove all of the
 * backed up "copies".
 *
 * If {@link #restore()} or {@link #discard()} is not called the backup files
 * will never be deleted.
 *
 * The backup store does not synchronize directories - actions that write new
 * files are responsible for removing them. Overwriting existing files should be
 * done by first backing up the file, and then creating a new file. Modifying a
 * file, should be done by using {@link #backupCopy(File)} or first making a
 * copy, then backing up the original, and then renaming the copy.
 *
 * <h3>Read Only and Permissions</h3> Directories that are read only (to current
 * user) can not be backed up. Backup is performed using
 * {@link File#renameTo(File)} and handling of permissions is operating system
 * dependent. It is expected that a Un*x type system retains the permissions as
 * a file is moved to the backup store and later gets restored. Backup
 * directories are created as they are needed and will (at least on Un*x)
 * inherit the permissions from its parent directory.
 *
 * If a rename can not be performed, the backup store will make a copy and
 * delete the original file. This makes it possible to backup and restore across
 * volume boundaries.
 *
 * When restoring directories they will be created with permissions in a
 * platform specific way (on UN*IX they will inherit the permissions of the
 * parent directory).
 *
 * <h3>Checkpointing</h3> Checkpointing (i.e. to be able to rollback to a
 * particular point) can be implemented by using multiple instances of
 * BackupStore. The client code will need to remember the individual order among
 * the backup stores.
 *
 * <h3>Restartability</h3> Not implemented - it is possible to obtain the name
 * of the backup directories, so manual restore is possible after a crash. An
 * idea is to add persistence to a file, and be able to read it back in again.
 *
 * <h3>A note about exceptions</h3> In general {@link IllegalArgumentException}
 * is thrown when attempting an operation that is considered "wrong use", and an
 * {@link IllegalStateException} or subclass thereof is thrown on an overall
 * wrong use of BackupStore (i.e. attempt to backup when store has been
 * restored). Some cases of "wrong use" can not be differentiated from I/O
 * errors (like a "file not found" as this could be caused by an entire disk
 * disappearing - in these case an {@link IOException} is thrown.
 *
 * <h3>Implementation Note</h3> The backup root directory will contain folders
 * that reflects file system roots. These are encoded using "_" for the UNI*X
 * root directory, "__" for a Windows network mounted directory, and single
 * "drive letter" folders corresponding to Windows drive letters. Typically, on
 * UN*X there will only be a "_" directory in the backup root, and on windows
 * there will typically be a single directory called "C".
 */
public class SimpleBackupStore implements IBackupStore {
	public static final String BACKUP_FILE_EXTENSION = "p2bu"; //$NON-NLS-1$

	public static final String DIR_PLACEHOLDER = "emptydir"; //$NON-NLS-1$

	/**
	 * The name to use for a directory that represents leading separator (i.e. "/"
	 * or "\").
	 */
	private static final String ROOTCHAR = "_"; //$NON-NLS-1$

	/**
	 * Map of directory File to backup root (File) - the backup root has a directory
	 * named {@link #buStoreName} where the backup is found.
	 */
	private final Path buStoreRoot;

	private String buInPlaceSuffix;

	/**
	 * Backup files that sit next to the original rather than in the backup store.
	 */
	private List<Path> buInPlace;

	/**
	 * Counter of how many files where backed up. Used as a simple check mechanism
	 * if everything was restored (a guard against manual/external tampering with
	 * the backup directories).
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

	/**
	 * Generates a BackupStore with a default prefix of ".p2bu" for backup directory
	 * and probe file.
	 */
	public SimpleBackupStore() {
		this(null, "." + BACKUP_FILE_EXTENSION); //$NON-NLS-1$
	}

	/**
	 * Generates a BackupStore with a specified prefix for backup directories and
	 * probe file.
	 *
	 * @param buStoreParent Parent under which the backup store will be created. If
	 *                      null, java.io.tmpdir is used
	 * @param prefix        Prefix used for human identification of backup stores.
	 */
	public SimpleBackupStore(File buStoreParent, String prefix) {
		String unique = UUID.randomUUID().toString();

		String buStoreName = prefix + "_" + unique; //$NON-NLS-1$
		this.buStoreRoot = (buStoreParent != null) ? buStoreParent.toPath().resolve(buStoreName)
				: Paths.get(System.getProperty("java.io.tmpdir")).resolve(buStoreName); //$NON-NLS-1$

		this.buInPlaceSuffix = String.format("-%s.%s", unique, BACKUP_FILE_EXTENSION); //$NON-NLS-1$
		this.buInPlace = new ArrayList<>();
	}

	/**
	 * Returns the unique backup name (this is the name of generated backup
	 * directories).
	 *
	 * @return the backup name.
	 */
	@Override
	public String getBackupName() {
		return buStoreRoot.getFileName().toString();
	}

	/**
	 * @return the parent dire under which backups are created
	 */
	public File getBackupRoot() {
		return buStoreRoot.toFile();
	}

	/**
	 * Backup the file by moving it to the backup store (for later (optional)
	 * restore). Calling this method with a file that represents a directory is
	 * equivalent to calling {@link #backupDirectory(File)}.
	 *
	 * A file (path) can only be backed up once per BackupStore instance. When the
	 * file is backed up, it is moved to a directory under this BackupStore
	 * instance's directory with a relative path corresponding to the original
	 * relative path from the backup root e.g. the file /A/B/C/foo.txt could be
	 * moved to /A/.p2bu_ffffff_ffffff/B/C/foo.txt when /A is the backup root.
	 *
	 * If a directory is first backed up, and later replaced by a regular file, and
	 * this file is backed up (or vice versa) - an {@link IllegalArgumentException}
	 * is thrown
	 *
	 * A backup can not be performed on a closed BackupStore.
	 *
	 * @param file - the file (or directory) to backup
	 * 
	 * @return true if the file was backed up, false if this file (path) has already
	 *         been backed up (the file is not moved to the store).
	 * 
	 * @throws IOException                - if the backup operation fails, or the
	 *                                    file does not exist
	 * @throws ClosedBackupStoreException - if the BackupStore has been closed
	 * @throws IllegalArgumentException   - on type mismatch (file vs. directory) of
	 *                                    earlier backup, or if file does not exist
	 */
	@Override
	public boolean backup(File file) throws IOException {
		assertOpen();

		Path path = file.toPath();

		if (Files.isDirectory(path)) {
			return backupDirectory(path.toFile());
		}

		if (!Files.exists(path)) {
			throw new IOException(NLS.bind(Messages.BackupStore_file_not_found, path.toAbsolutePath()));
		}

		Path buPath = toBackupPath(path);

		// Already backed up, but was a directory - wrong usage
		if (Files.isDirectory(buPath)) {
			throw new IllegalArgumentException(
					NLS.bind(Messages.BackupStore_directory_file_mismatch, buPath.toAbsolutePath()));
		}

		return moveToBackup(path, buPath);
	}

	/**
	 * Performs backup of an empty directory.
	 *
	 * The directory must be empty before it can be backed up (i.e. similar to a
	 * delete of a directory). The called must backup the files of the directory
	 * first. A call to backup a directory is really only needed for empty
	 * directories as a restore of a file will also restore all of its parent
	 * directories.
	 *
	 * @param file - the (empty) directory to back up
	 * 
	 * @return true if the directory was moved to backup. false if the directory was
	 *         already backed up.
	 *
	 * @throws IllegalArgumentException if file is not a directory, or is not empty.
	 * @throws IOException              if directory can not be moved to the backup
	 *                                  store, or if the directory is not writeable
	 */
	@Override
	public boolean backupDirectory(File file) throws IOException {
		assertOpen();

		Path path = file.toPath();

		if (!Files.isDirectory(path)) {
			throw new IllegalArgumentException(NLS.bind(Messages.BackupStore_not_a_directory, file.getAbsolutePath()));
		}

		try (Stream<Path> s = Files.list(path)) {
			if (s.findAny().isPresent()) {
				throw new IllegalArgumentException(
						NLS.bind(Messages.BackupStore_directory_not_empty, file.getAbsolutePath()));
			}
		}

		return moveDirToBackup(path);
	}

	/**
	 * Backs up a file, or everything under a directory.
	 *
	 * @param file - file to backup or directory
	 * 
	 * @throws IOException if backup operation failed
	 */
	@Override
	public void backupAll(File file) throws IOException {
		assertOpen();

		Path path = file.toPath().normalize();

		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path f, BasicFileAttributes attrs) throws IOException {
				backup(f.toFile());
				return CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (exc != null) {
					throw exc;
				}
				moveDirToBackup(dir);
				return CONTINUE;
			}
		});
	}

	/**
	 * Backup the file by leaving a copy of the contents in the original location.
	 * 
	 * Calling this method with a file that represents a directory throws an
	 * {@link IllegalArgumentException}.
	 *
	 * A file (path) can only be backed up once per BackupStore instance. When the
	 * file is backed up, it is moved to a directory under this BackupStore
	 * instance's directory with a relative path corresponding to the original
	 * relative path from the backup root e.g. the file /A/B/C/foo.txt could be
	 * moved to /A/.p2bu_ffffff_ffffff/B/C/foo.txt when /A is the backup root.
	 *
	 * If a directory is first backed up, and later replaced by a regular file, and
	 * this file is backed up (or vice versa) - an {@link IllegalArgumentException}
	 * is thrown
	 *
	 * A backup can not be performed on a closed BackupStore.
	 *
	 * @param file - the file (or directory) to backup
	 * 
	 * @return true if the file was backed up, false if this file (path) has already
	 *         been backed up (the file is not moved to the store).
	 * 
	 * @throws IOException                if the backup operation fails, or the file
	 *                                    does not exist
	 * @throws ClosedBackupStoreException if the BackupStore has been closed
	 * @throws IllegalArgumentException   on type mismatch (file vs. directory) of
	 *                                    earlier backup, or if file is a Directory
	 */
	@Override
	public boolean backupCopy(File file) throws IOException {
		assertOpen();

		Path path = file.toPath();

		if (!Files.exists(path)) {
			throw new IOException(NLS.bind(Messages.BackupStore_file_not_found, file.getAbsolutePath()));
		}

		if (Files.isDirectory(path)) {
			throw new IllegalArgumentException(
					NLS.bind(Messages.BackupStore_can_not_copy_directory, file.getAbsolutePath()));
		}

		Path buPath = toBackupPath(path);

		// Already backed up, but was a directory = wrong usage
		if (Files.isDirectory(buPath)) {
			throw new IllegalArgumentException(
					NLS.bind(Messages.BackupStore_directory_file_mismatch, buPath.toAbsolutePath()));
		}

		// Already backed up, can only be done once with one BackupStore
		if (Files.exists(buPath)) {
			return false;
		}

		Files.createDirectories(buPath.getParent());
		Files.copy(path, buPath, REPLACE_EXISTING);

		backupCounter++;
		return true;
	}

	/**
	 * Backs up a file, or everything under a directory.
	 * 
	 * A copy of the backup is left in the original place.
	 *
	 * @param file
	 * 
	 * @throws IOException
	 */
	@Override
	public void backupCopyAll(File file) throws IOException {
		assertOpen();

		Path path = file.toPath();
		if (!Files.exists(path)) {
			return;
		}

		path = path.normalize();

		if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
			backupCopy(file);
		} else if (Files.isDirectory(path)) {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path f, BasicFileAttributes attrs) throws IOException {
					backupCopy(f.toFile());
					return CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					if (exc != null) {
						throw exc;
					}
					copyDirToBackup(dir);
					return CONTINUE;
				}
			});
		}
	}

	/**
	 * Restores all backup files from backup store. Note that restore of a (non
	 * directory) file deletes an existing file or directory found in the restore
	 * location. When the backup has been restored this BackupStore instance is
	 * closed and can not be used for further backup or restore.
	 *
	 * If there are unrestorable items (non writable directories, or general IO
	 * exceptions) these items are written to the log, and the backup copies remain
	 * in the file system and can be manually restored (using a simple zip of the
	 * backup directory, and an unzip to the buRoot once the problem has been
	 * corrected).
	 *
	 * @throws IOException                if the backup was not fully restored -
	 *                                    unrestored items have been logged.
	 * @throws ClosedBackupStoreException if the backup is already closed.
	 */
	@Override
	public void restore() throws IOException {
		assertOpen();
		closed = true;

		// Put back all files.
		// Collect things that could not be restored
		Map<Path, Throwable> unrestorable = new HashMap<>();

		restoreBackups(unrestorable);
		restoreInPlaceBackups(unrestorable);

		boolean restored = true;

		// Checked failed attempts to restore
		if (!unrestorable.isEmpty()) {
			restored = false;

			unrestorable.forEach((p, err) -> {
				logError(NLS.bind(Messages.BackupStore_manual_restore_needed, err, p.toAbsolutePath()));
			});
		}

		// Check external tampering with backup store
		if (backupCounter != restoreCounter) {
			restored = false;

			if (!unrestorable.isEmpty()) {
				logError(NLS.bind(Messages.BackupStore_0_of_1_items_restored, restoreCounter, backupCounter));
			} else {
				logError(NLS.bind(Messages.BackupStore_externally_modified_0_of_1_restored, restoreCounter,
						backupCounter));
			}
		}

		if (!restored) {
			throw new IOException(Messages.BackupStore_errors_while_restoring_see_log);
		}
	}

	/**
	 * Discards and closes this BackupStore. Does nothing if this store is already
	 * restored or discarded.
	 */
	@Override
	public void discard() {
		if (closed) {
			return;
		}
		closed = true;

		try {
			deleteAll(buStoreRoot);
		} catch (IOException e) {
			logWarning(NLS.bind(Messages.BackupStore_can_not_remove_bu_directory, buStoreRoot.toAbsolutePath()));
		}

		for (Path buFile : buInPlace) {
			try {
				deleteAll(buFile);
			} catch (IOException e) {
				logWarning(NLS.bind(Messages.BackupStore_can_not_remove_bu_file, buFile.toAbsolutePath()));
			}
		}
	}

	private void assertOpen() {
		if (closed) {
			throw new ClosedBackupStoreException(Messages.BackupStore_closed_store);
		}
	}

	/**
	 * Makes sure a directory exists in the backup store without touching the
	 * original directory content
	 * 
	 * @param path
	 * 
	 * @return false if the directory is already created in the backup store, false
	 *         if a placeholder had to be created and backed up.
	 * 
	 * @throws IOException
	 */
	private boolean copyDirToBackup(Path path) throws IOException {
		Path buPath = toBackupPath(path);

		if (Files.exists(buPath)) {
			return false;
		}

		Path placeholderPath = path.resolve(DIR_PLACEHOLDER);
		try {
			Files.createFile(placeholderPath);
		} catch (IOException e) {
			throw new IOException(
					NLS.bind(Messages.BackupStore_can_not_create_placeholder, placeholderPath.toAbsolutePath()), e);
		}

		Path buPlaceholderPath = buPath.resolve(DIR_PLACEHOLDER);
		moveToBackup(placeholderPath, buPlaceholderPath);
		return true;
	}

	private boolean moveDirToBackup(Path dir) throws IOException {
		boolean copied = copyDirToBackup(dir);

		try {
			Files.delete(dir);
		} catch (IOException e) {
			throw new IOException(NLS.bind(Messages.BackupStore_can_not_remove, dir.toAbsolutePath()));
		}

		return copied;
	}

	/**
	 * Move/rename file to a backup file.
	 *
	 * Exposed for testing purposes.
	 *
	 * Callers of the method must have ensured that the source file exists and the
	 * backup file does not exist.
	 *
	 * @param file   source file to move; should already exist and must not be
	 *               directory
	 * @param buFile destination backup file to move to; should not exist and must
	 *               be a directory
	 *
	 * @throws IOException if the backup operation fails
	 */
	private boolean moveToBackup(Path path, Path buPath) throws IOException {
		// Already backed up. Can only be done once with one BackupStore.
		if (Files.exists(buPath)) {
			/*
			 * Although backed up, the file can be still on the file system. For example,
			 * two IUs may be unzipping their contents to the same location and share a few
			 * common files, which have to be removed twice.
			 */
			try {
				Files.delete(path);
			} catch (IOException e) {
				throw new IOException(NLS.bind(Messages.BackupStore_can_not_remove, path.toAbsolutePath()), e);
			}

			return false;
		}

		// make sure all of the directories exist / gets created
		Path buPathDir = buPath.getParent();
		try {
			Files.createDirectories(buPathDir);
		} catch (IOException e) {
			throw new IllegalArgumentException(
					NLS.bind(Messages.BackupStore_file_directory_mismatch, buPathDir.toAbsolutePath()), e);
		}

		move(path, buPath);
		if (isEclipseExe(path) && Files.isRegularFile(path)) {
			// The original is the launcher executable and it still exists at the original
			// location although the move succeeded.
			// This happens when it is the Windows executable that is locked because it's
			// running and we are attempting to move it to a different drive.
			// In this case the target will exist as a copy, so we should delete it.
			// Then backup in place which will necessarily be on the same drive.
			Files.delete(buPath);
			Path inPlaceBuPath = toInPlaceBackupPath(path);
			move(path, inPlaceBuPath);
			buInPlace.add(inPlaceBuPath);
		}

		backupCounter++;
		return true;
	}

	/**
	 * Restores everything stored in the backup root
	 *
	 * Responsible for converting the root prefix of the path from backup format
	 * back to the original real OS names. I.e. "_/" to "//", "__/" to "///", "C/"
	 * to "C:", etc.
	 *
	 * @param unrestorable accumulate unrestorable paths (including the entire
	 *                     backup store).
	 * 
	 * @throws IOException
	 */
	private void restoreBackups(Map<Path, Throwable> unrestorable) throws IOException {
		if (!Files.exists(buStoreRoot)) {
			unrestorable.put(buStoreRoot, new IOException(
					NLS.bind(Messages.BackupStore_missing_backup_directory, buStoreRoot.toAbsolutePath())));
			return;
		}

		Files.walkFileTree(buStoreRoot, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path buDir, BasicFileAttributes attrs) {
				try {
					if (Files.isSameFile(buStoreRoot, buDir)) {
						return CONTINUE;
					}

					Path dir = toSourcePath(buDir);

					// There is a file where we the original directory used to be - delete it
					if (Files.isRegularFile(dir)) {
						Files.delete(dir);
					}

					// Make the original directory if needed
					Files.createDirectories(dir);
				} catch (IOException e) {
					unrestorable.put(buDir, e);
				}
				return CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path buFile, BasicFileAttributes attrs) {
				Path file = toSourcePath(buFile);
				try {
					// The first level children of buStoreRoot are always directories since they
					// model file system roots
					if (Files.isSameFile(buFile.getParent(), buStoreRoot)) {
						unrestorable.put(buFile, new IOException("Not a directory")); //$NON-NLS-1$
					} else {
						/*
						 * Do not restore the place-holders as they are used to trigger creation of
						 * empty directories and are not wanted in the restored location.
						 * 
						 * They are counted as restored non the less.
						 */
						if (!DIR_PLACEHOLDER.equals(buFile.getFileName().toString())) {
							// Clean up the site where the original used to be.
							// It may be that a file or a directory now occupies it.
							deleteAll(file);

							// Move the backup to the original location
							move(buFile, file);
						} else {
							Files.delete(buFile);
						}

						restoreCounter++;
					}
				} catch (IOException e) {
					unrestorable.put(buFile, e);
				}
				return CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				unrestorable.put(file, exc);
				throw exc;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path buDir, IOException exc) throws IOException {
				if (exc != null) {
					unrestorable.put(buDir, exc);
					throw exc;
				}
				try {
					Files.delete(buDir);
				} catch (DirectoryNotEmptyException e) {
					String children = Files.list(buDir).map(p -> p.relativize(buDir)).map(Path::toString)
							.collect(joining(",")); //$NON-NLS-1$
					unrestorable.put(buDir,
							new IOException(String.format("Directory %s not empty: %s", buDir, children, e))); //$NON-NLS-1$
				} catch (IOException e) {
					unrestorable.put(buDir, e);
				}
				return CONTINUE;
			}
		});
	}

	private void restoreInPlaceBackups(Map<Path, Throwable> unrestorable) {
		for (Path buPath : buInPlace) {
			Path path = toInPlaceSourcePath(buPath);

			try {
				move(buPath, path);
				restoreCounter++;
			} catch (IOException e) {
				unrestorable.put(buPath, e);
			}
		}
	}

	/**
	 * Converts a source path to a backup path.
	 * 
	 * Exposed for testing purposes.
	 *
	 * A leading "root" is transformed to the ROOTCHAR character. On Windows,
	 * network mapped drives starts with two separators - and are encoded as two
	 * ROOTCHAR.
	 *
	 * E.g. \\Host\C$\file becomes __\Host\C$\file /users/test/file becomes
	 * _/users/test/file C:/file becomes C/file
	 *
	 * @param file a source file that needs to be backed up
	 * 
	 * @return a file to which the original content can be backed up
	 * 
	 * @throws IOException
	 */
	protected Path toBackupPath(Path path) throws IOException {
		Path pathNormal = path.normalize();

		String buPath = pathNormal.toAbsolutePath().toString();

		String buPrefix = ""; //$NON-NLS-1$
		while (buPath.startsWith(File.separator)) {
			buPrefix += ROOTCHAR;
			buPath = buPath.substring(1);
		}

		// Linux or Windows net mount
		if (!buPrefix.isEmpty()) {
			buPath = Paths.get(buPrefix, buPath).toString();
		}
		// Windows
		else {
			// It is a windows drive letter first.
			// Transform C:/foo to C/foo
			int idx = buPath.indexOf(":"); //$NON-NLS-1$
			if (idx < 1) {
				throw new IllegalArgumentException("File is neither absolute nor has a drive name: " + buPath); //$NON-NLS-1$
			}
			buPath = buPath.substring(0, idx) + buPath.substring(idx + 1);
		}

		Path buFile = buStoreRoot.resolve(buPath);
		return buFile;
	}

	/**
	 * Converts a backup file to the original source file.
	 * 
	 * ///x/y/z -> ___x/y/z \\x\y\z c:\x\y\z -> c\x\y\z
	 * 
	 * @param buPath an absolute file under {@link #buStoreRoot} to which some
	 *               content is backed up.
	 * 
	 * @return the original source file to which the content can be restored.
	 */
	protected Path toSourcePath(Path buPath) {
		Path buPathRel = buStoreRoot.relativize(buPath);

		String pathName = buPathRel.toString();

		String prefix = ""; //$NON-NLS-1$
		while (pathName.startsWith(ROOTCHAR)) {
			prefix += File.separator;
			pathName = pathName.substring(1);
		}

		if (prefix.isEmpty()) {
			// The first char is a windows drive name
			pathName = pathName.charAt(0) + ":" + pathName.substring(1); //$NON-NLS-1$
		} else {
			pathName = prefix + pathName;
		}

		return Paths.get(pathName);
	}

	/**
	 * Converts a path to an in-place backup path.
	 * 
	 * Exposed for testing purposes.
	 * 
	 * @param path
	 * 
	 * @return a path next to the original where the original will be moved, rather
	 *         than will be moved
	 */
	protected Path toInPlaceBackupPath(Path path) {
		String buPathName = path.getFileName() + buInPlaceSuffix;
		Path buPath = path.toAbsolutePath().resolveSibling(buPathName);
		return buPath;
	}

	/**
	 * Converts a in-place backup path to the original source path.
	 * 
	 * Exposed for testing purposes.
	 * 
	 * @param path
	 * 
	 * @return a source path
	 */
	protected Path toInPlaceSourcePath(Path buPath) {
		String buPathName = buPath.getFileName().toString();

		int suffixIdx = buPathName.indexOf(buInPlaceSuffix);
		if (suffixIdx <= 0) {
			throw new IllegalArgumentException();
		}

		String pathName = buPathName.substring(0, suffixIdx);
		Path path = buPath.resolveSibling(pathName);
		return path;
	}

	/**
	 * A generic file operation that attempts to move a file.
	 *
	 * Exposed in a separate method for testing purposes.
	 */
	protected void move(Path source, Path target) throws IOException {
		Files.move(source, target, REPLACE_EXISTING);
	}

	private static boolean isEclipseExe(Path file) {
		String name = file.getFileName().toString();

		String launcher = System.getProperty("eclipse.launcher"); //$NON-NLS-1$
		if (launcher != null) {
			String launcherName = Paths.get(launcher).getFileName().toString();
			if (name.equalsIgnoreCase(launcherName)) {
				return true;
			}
		}

		return name.equalsIgnoreCase("eclipse.exe") || name.equalsIgnoreCase("eclipsec.exe"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Deletes a file, or a directory with all of it's children.
	 * 
	 * @param path
	 * 
	 * @throws IOException
	 */
	private static void deleteAll(Path path) throws IOException {
		if (!Files.exists(path)) {
			return;
		}

		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (exc != null) {
					throw exc;
				}
				Files.delete(dir);
				return CONTINUE;
			}
		});
	}
}
