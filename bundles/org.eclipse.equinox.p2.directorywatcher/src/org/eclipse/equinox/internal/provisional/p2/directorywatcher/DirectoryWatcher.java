/*******************************************************************************
 *  Copyright (c) 2007, 2010 aQute and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *  aQute - initial implementation and ideas 
 *  IBM Corporation - initial adaptation to Equinox provisioning use
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.directorywatcher;

import java.io.File;
import java.util.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;

public class DirectoryWatcher {
	private static final String DEL_EXT = ".del"; //$NON-NLS-1$

	public class WatcherThread extends Thread {

		private final long pollFrequency;
		private boolean done = false;

		public WatcherThread(long pollFrequency) {
			super("Directory Watcher"); //$NON-NLS-1$
			this.pollFrequency = pollFrequency;
		}

		public void run() {
			do {
				try {
					poll();
					synchronized (this) {
						wait(pollFrequency);
					}
				} catch (InterruptedException e) {
					// ignore
				} catch (Throwable e) {
					log(Messages.error_main_loop, e);
					done = true;
				}
			} while (!done);
		}

		public synchronized void done() {
			done = true;
			notify();
		}
	}

	public final static String POLL = "eclipse.p2.directory.watcher.poll"; //$NON-NLS-1$
	public final static String DIR = "eclipse.p2.directory.watcher.dir"; //$NON-NLS-1$
	private static final long DEFAULT_POLL_FREQUENCY = 2000;

	public static void log(String string, Throwable e) {
		System.err.println(string + ": " + e); //$NON-NLS-1$
	}

	final File[] directories;

	long poll = 2000;
	private Set<DirectoryChangeListener> listeners = new HashSet<DirectoryChangeListener>();
	private HashSet<File> scannedFiles = new HashSet<File>();
	private HashSet<File> removals;
	private Set<File> pendingDeletions;
	private WatcherThread watcher;

	public DirectoryWatcher(Map<String, String> properties, BundleContext context) {
		String dir = properties.get(DIR);
		if (dir == null)
			dir = "./load"; //$NON-NLS-1$

		File targetDirectory = new File(dir);
		targetDirectory.mkdirs();
		directories = new File[] {targetDirectory};
	}

	public DirectoryWatcher(File directory) {
		if (directory == null)
			throw new IllegalArgumentException(Messages.null_folder);

		this.directories = new File[] {directory};
	}

	public DirectoryWatcher(File[] directories) {
		if (directories == null)
			throw new IllegalArgumentException(Messages.null_folder);
		this.directories = directories;
	}

	public synchronized void addListener(DirectoryChangeListener listener) {
		listeners.add(listener);
	}

	public synchronized void removeListener(DirectoryChangeListener listener) {
		listeners.remove(listener);
	}

	public void start() {
		start(DEFAULT_POLL_FREQUENCY);
	}

	public synchronized void poll() {
		startPoll();
		scanDirectories();
		stopPoll();
	}

	public synchronized void start(final long pollFrequency) {
		if (watcher != null)
			throw new IllegalStateException(Messages.thread_started);

		watcher = new WatcherThread(pollFrequency);
		watcher.start();
	}

	public synchronized void stop() {
		if (watcher == null)
			throw new IllegalStateException(Messages.thread_not_started);

		watcher.done();
		watcher = null;
	}

	public File[] getDirectories() {
		return directories;
	}

	private void startPoll() {
		removals = scannedFiles;
		scannedFiles = new HashSet<File>();
		pendingDeletions = new HashSet<File>();
		for (DirectoryChangeListener listener : listeners)
			listener.startPoll();
	}

	private void scanDirectories() {
		for (int index = 0; index < directories.length; index++) {
			File directory = directories[index];
			File list[] = directory.listFiles();
			if (list == null)
				continue;
			for (int i = 0; i < list.length; i++) {
				File file = list[i];
				// if this is a deletion marker then add to the list of pending deletions.
				if (list[i].getPath().endsWith(DEL_EXT)) {
					File target = new File(file.getPath().substring(0, file.getPath().length() - 4));
					removals.add(target);
					pendingDeletions.add(target);
				} else {
					// else remember that we saw the file and remove it from this list of files to be 
					// removed at the end.  Then notify all the listeners as needed.
					scannedFiles.add(file);
					removals.remove(file);
					for (DirectoryChangeListener listener : listeners) {
						if (isInterested(listener, file))
							processFile(file, listener);
					}
				}
			}
		}
	}

	private void stopPoll() {
		notifyRemovals();
		removals = scannedFiles;
		for (DirectoryChangeListener listener : listeners)
			listener.stopPoll();
		processPendingDeletions();
	}

	private boolean isInterested(DirectoryChangeListener listener, File file) {
		return listener.isInterested(file);
	}

	/**
	 * Notify the listeners of the files that have been deleted or marked for deletion.
	 */
	private void notifyRemovals() {
		Set<File> removed = removals;
		for (DirectoryChangeListener listener : listeners) {
			for (File file : removed) {
				if (isInterested(listener, file))
					listener.removed(file);
			}
		}
	}

	private void processFile(File file, DirectoryChangeListener listener) {
		try {
			Long oldTimestamp = listener.getSeenFile(file);
			if (oldTimestamp == null) {
				// The file is new
				listener.added(file);
			} else {
				// The file is not new but may have changed
				long lastModified = file.lastModified();
				if (oldTimestamp.longValue() != lastModified)
					listener.changed(file);
			}
		} catch (Exception e) {
			log(NLS.bind(Messages.error_processing, listener), e);
		}
	}

	/**
	 * Try to remove the files that have been marked for deletion.
	 */
	private void processPendingDeletions() {
		for (Iterator<File> iterator = pendingDeletions.iterator(); iterator.hasNext();) {
			File file = iterator.next();
			if (!file.exists() || file.delete())
				iterator.remove();
			new File(file.getPath() + DEL_EXT).delete();
		}
	}

}
