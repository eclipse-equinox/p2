/*******************************************************************************
 * Copyright (c) 2007 aQute, IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * aQute - initial implementation and ideas 
 * IBM Corporation - initial adaptation to Equinox provisioning use
 ******************************************************************************/
package org.eclipse.equinox.p2.directorywatcher;

import java.io.File;
import java.util.*;
import org.osgi.framework.BundleContext;

public class DirectoryWatcher {
	public class WatcherThread extends Thread {

		private final long pollFrequency;
		private boolean done = false;

		public WatcherThread(long pollFrequency) {
			super("Directory Watcher");
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
					e.printStackTrace();
					log("In main loop, we have serious trouble", e);
					done = true;
				}
			} while (!done);
		}

		public synchronized void done() {
			done = true;
			notify();
		}
	}

	public final static String POLL = "eclipse.p2.directory.watcher.poll";
	public final static String DIR = "eclipse.p2.directory.watcher.dir";
	private static final long DEFAULT_POLL_FREQUENCY = 2000;

	public static void log(String string, Throwable e) {
		System.err.println(string + ": " + e);
	}

	final File directory;

	long poll = 2000;
	private Set listeners = new HashSet();
	private HashSet scannedFiles = new HashSet();
	private HashSet removals;
	private Set pendingDeletions;
	private WatcherThread watcher;

	public DirectoryWatcher(Dictionary properties, BundleContext context) {
		String dir = (String) properties.get(DIR);
		if (dir == null)
			dir = "./load";

		File targetDirectory = new File(dir);
		targetDirectory.mkdirs();
		directory = targetDirectory;
	}

	public DirectoryWatcher(File directory) {
		if (directory == null)
			throw new IllegalArgumentException("Folder must not be null");

		this.directory = directory;
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
			throw new IllegalStateException("Already Started");

		watcher = new WatcherThread(pollFrequency);
		watcher.start();
	}

	public synchronized void stop() {
		if (watcher == null)
			throw new IllegalStateException("Not Started");

		watcher.done();
		watcher = null;
	}

	public File getDirectory() {
		return directory;
	}

	private void startPoll() {
		removals = scannedFiles;
		scannedFiles = new HashSet();
		pendingDeletions = new HashSet();
		for (Iterator i = listeners.iterator(); i.hasNext();)
			((DirectoryChangeListener) i.next()).startPoll();
	}

	private void scanDirectories() {
		File list[] = directory.listFiles();
		if (list == null)
			return;
		for (int i = 0; i < list.length; i++) {
			File file = list[i];
			// if this is a deletion marker then add to the list of pending deletions.
			if (list[i].getPath().endsWith(".del")) {
				File target = new File(file.getPath().substring(0, file.getPath().length() - 4));
				removals.add(target);
				pendingDeletions.add(target);
			} else {
				// else remember that we saw the file and remove it from this list of files to be 
				// removed at the end.  Then notify all the listeners as needed.
				scannedFiles.add(file);
				removals.remove(file);
				for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
					DirectoryChangeListener listener = (DirectoryChangeListener) iterator.next();
					if (isInterested(listener, file))
						processFile(file, listener);
				}
			}
		}
	}

	private void stopPoll() {
		notifyRemovals();
		removals = scannedFiles;
		for (Iterator i = listeners.iterator(); i.hasNext();)
			((DirectoryChangeListener) i.next()).stopPoll();
		processPendingDeletions();
	}

	private boolean isInterested(DirectoryChangeListener listener, File file) {
		String[] extensions = listener.getExtensions();
		for (int i = 0; i < extensions.length; i++)
			if (file.getPath().endsWith(extensions[i]))
				return true;
		return false;
	}

	/**
	 * Notify the listeners of the files that have been deleted or marked for deletion.
	 */
	private void notifyRemovals() {
		Set removed = removals;
		for (Iterator i = listeners.iterator(); i.hasNext();) {
			DirectoryChangeListener listener = (DirectoryChangeListener) i.next();
			for (Iterator j = removed.iterator(); j.hasNext();) {
				File file = (File) j.next();
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
			log("Processing : " + listener, e);
		}
	}

	/**
	 * Try to remove the files that have been marked for deletion.
	 */
	private void processPendingDeletions() {
		for (Iterator iterator = pendingDeletions.iterator(); iterator.hasNext();) {
			File file = (File) iterator.next();
			if (!file.exists() || file.delete())
				iterator.remove();
			new File(file.getPath() + ".del").delete();
		}
	}

}
