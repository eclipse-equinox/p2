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
package org.eclipse.equinox.internal.p2.touchpoint.natives;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.osgi.util.NLS;

public class BackupFiles {

	private static final String ZIP_SUFFIX = ".zip"; //$NON-NLS-1$
	private static final String PROPERTIES_SUFFIX = ".properties"; //$NON-NLS-1$

	private final File backupDir;
	private boolean doBackup;

	/**
	 * Save or restore backups of files in backupDir
	 */
	public BackupFiles(File backupDir) {
		this.doBackup = true;
		this.backupDir = backupDir;
		this.backupDir.mkdirs();
	}

	//    /**
	//     * If doBackup is set to false, files are deleted on uninstall, instead of restored.
	//     */
	//    public void setDoBackup(boolean doBackup) {
	//        this.doBackup = doBackup;
	//    }

	/**
	 * Restore all backups made in this dir.
	 */
	public void restore(IProgressMonitor monitor) throws IOException {
		// find backup properties files, do in reverse order
		List propsFiles = new LinkedList();
		for (int i = 0;; i += 1) {
			File propsFile = getBackupProperties(i);
			if (!propsFile.exists()) {
				break;
			}
			propsFiles.add(0, propsFile);
		}
		SubMonitor subMonitor = SubMonitor.convert(monitor, propsFiles.size());
		for (Iterator i = propsFiles.iterator(); i.hasNext();) {
			File propsFile = (File) i.next();
			restoreFilesFromBackup(propsFile, subMonitor.newChild(1));
		}
		if (!this.backupDir.delete()) {
			//not empty?  log a warning?
		} else {
			// delete the parent if empty
			this.backupDir.getParentFile().delete();
		}
		monitor.done();
	}

	/**
	 * Find files under outputDir that will be overwritten in unzipURL
	 * and save under backupDir, and delete.
	 * Include properties file to indicate files to delete or restore on rolled back.
	 * The progress monitor is used only to display sub-tasks; we don't update it otherwise.
	 */
	public void backupFilesInZip(String identifier, URL zipURL, File outputDir, IProgressMonitor monitor) throws IOException {
		BackupProperties backupProps = new BackupProperties(identifier, outputDir);
		ZipOutputStream zos = null;
		String prevDir = null;
		try {
			ZipInputStream in = new ZipInputStream(zipURL.openStream());
			ZipEntry ze;
			while ((ze = in.getNextEntry()) != null) {
				String name = ze.getName();
				int i = name.lastIndexOf('/');
				if (i != -1) {
					String dir = name.substring(0, i);
					if (this.doBackup && !dir.equals(prevDir)) {
						monitor.subTask(name.substring(0, i));
						prevDir = dir;
					}
				}
				if (!ze.isDirectory()) {
					File origFile = new File(outputDir, name);
					if (this.doBackup && origFile.exists()) {
						if (zos == null) {
							File zipFile = backupProps.getArchive();
							zos = new ZipOutputStream(new FileOutputStream(zipFile));
						}
						ZipEntry zipEntry = new ZipEntry(name);
						zipEntry.setTime(origFile.lastModified());
						zos.putNextEntry(zipEntry);
						FileUtils.copyStream(new FileInputStream(origFile), true, zos, false);
						zos.closeEntry();
					} else {
						backupProps.addFileToDelete(name);
					}
					origFile.delete();
				}
				in.closeEntry();
			}
			in.close();
		} finally {
			backupProps.store();
			if (zos != null) {
				zos.close();
			}
		}
	}

	private void restoreFilesFromBackup(File propsFile, IProgressMonitor monitor) throws IOException {
		BackupProperties backupProps = new BackupProperties(propsFile);
		monitor.beginTask(NLS.bind(Messages.restoring, propsFile.toString()), 3);
		monitor.subTask(""); //$NON-NLS-1$
		for (Iterator i = backupProps.getFilesToDelete().iterator(); i.hasNext();) {
			String name = (String) i.next();
			File full = new File(backupProps.getRootDir(), name);
			full.delete();
		}
		monitor.worked(1);
		File zipFile = backupProps.getArchive();
		if (zipFile.exists()) { // only exists if files were saved
			SubProgressMonitor sub = new SubProgressMonitor(monitor, 1, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);
			FileUtils.unzipFile(zipFile, backupProps.getRootDir(), "", sub); //$NON-NLS-1$
			zipFile.delete();
		} else {
			monitor.worked(1);
		}
		for (Iterator i = backupProps.getDirsToDelete().iterator(); i.hasNext();) {
			String name = (String) i.next();
			File full = new File(backupProps.getRootDir(), name);
			FileUtils.deleteEmptyDirs(full);
		}
		propsFile.delete();
		monitor.worked(1);
		monitor.done();
	}

	// Backup files are just 0.properties, 1.properties, etc.
	// Get the next unused one.
	File getBackupProperties() {
		for (int i = 0;; i += 1) {
			File result = getBackupProperties(i);
			if (!result.exists())
				return result;
		}
	}

	private File getBackupProperties(int i) {
		return new File(BackupFiles.this.backupDir, Integer.toString(i) + PROPERTIES_SUFFIX);
	}

	private class BackupProperties extends Properties {
		private static final long serialVersionUID = 2268313492348533029L;
		private static final char FILE_KIND = 'f';
		private static final char DIR_KIND = 'd';
		private static final String ROOT_DIR = "rootDir"; //$NON-NLS-1$
		private static final String ARTIFACT_KEY = "artifactKey"; //$NON-NLS-1$
		//		private static final String ARTIFACT_USER = "artifactUser"; //$NON-NLS-1$

		private int n = 0; // number of properties
		private File file; // file to store properties in
		private List keys = new LinkedList(); // keys, in order they were added or read
		private final File rootDir; // root of where files are going
		private Set dirsToCreate = new TreeSet(); // set of dirs we will create

		// create properties based on file we are backing up to
		public BackupProperties(String identifier, File rootDir) {
			this.file = BackupFiles.this.getBackupProperties();
			this.rootDir = rootDir;
			setProperty(ROOT_DIR, rootDir.getPath().replace('\\', '/'));
			setProperty(ARTIFACT_KEY, (identifier != null ? identifier : rootDir.getAbsolutePath()));
			//			setProperty(ARTIFACT_USER, artifact.toUserString());
			// make sure rootDir is deleted if appropriate
			addDir("./"); //$NON-NLS-1$
		}

		// create backup properties from a previously saved BackupProperties
		public BackupProperties(File file) throws IOException {
			this.file = file;
			FileInputStream stream = new FileInputStream(file);
			try {
				load(stream);
			} finally {
				stream.close();
			}
			this.rootDir = new File(getProperty(ROOT_DIR));
		}

		//		public String getArtifactKey() {
		//			return getProperty(ARTIFACT_KEY);
		//		}

		//		public String getArtifactUserString() {
		//			String result = getProperty(ARTIFACT_USER);
		//			if (result != null) {
		//				return result;
		//			} else {
		//				// return something if the key wasn't saved
		//				result = getArtifactKey();
		//				result = result.replaceFirst(",native,", ","); //$NON-NLS-1$ //$NON-NLS-2$
		//				return result.replace(',', ' ').trim();
		//			}
		//		}

		public File getRootDir() {
			return this.rootDir;
		}

		// We are backing up files for this artifact.
		// Create a backup zip based on the artifact key (as a hint).
		public File getArchive() {
			String path = this.file.getPath();
			if (path.endsWith(PROPERTIES_SUFFIX)) {
				path = path.substring(0, path.length() - PROPERTIES_SUFFIX.length());
			}
			return new File(path + ZIP_SUFFIX);
		}

		public List getFilesToDelete() {
			return getMatchingProperties(FILE_KIND);
		}

		public List getDirsToDelete() {
			return getMatchingProperties(DIR_KIND);
		}

		private List getMatchingProperties(char c) {
			List result = new LinkedList();
			for (Enumeration e = propertyNames(); e.hasMoreElements();) {
				String key = (String) e.nextElement();
				if (key.equals(BackupProperties.ROOT_DIR)) {
					continue;
				}
				if (key.charAt(0) == c) {
					result.add(getProperty(key));
				}
			}
			return result;
		}

		public void addFileToDelete(String name) {
			add(FILE_KIND, name);
			addDir(name);
		}

		public void store() throws IOException {
			// add the directories -- at end because we want them all in order
			for (Iterator i = this.dirsToCreate.iterator(); i.hasNext();) {
				String name = (String) i.next();
				add(DIR_KIND, name);
			}
			FileOutputStream stream = new FileOutputStream(this.file);
			try {
				store(stream, /*header*/null);
			} finally {
				stream.close();
			}
		}

		public Object put(Object key, Object value) {
			if (!(key instanceof String))
				throw new AssertionError("expected String: " + key); //$NON-NLS-1$
			if (!(value instanceof String))
				throw new AssertionError("expected String: " + value); //$NON-NLS-1$
			this.keys.add(key);
			return super.put(key, value);
		}

		// return keys in the order they were added
		public synchronized Enumeration keys() {
			final Iterator iterator = this.keys.iterator();
			return new Enumeration() {
				public boolean hasMoreElements() {
					return iterator.hasNext();
				}

				public Object nextElement() {
					return iterator.next();
				}
			};
		}

		private void add(char kind, String name) {
			StringBuffer key = new StringBuffer(4);
			key.append(kind).append(n++);
			setProperty(key.toString(), name.replace('\\', '/'));
		}

		// if we're going to create this dir, remember that so we delete it
		private void addDir(String name) {
			int slash = name.lastIndexOf('/');
			if (slash == -1)
				return; // no dir
			String dirName = name.substring(0, slash);
			if (this.dirsToCreate.contains(dirName))
				return; // already have it
			if (new File(this.rootDir, dirName).exists())
				return; // already exists
			this.dirsToCreate.add(dirName);
		}

	}

}
