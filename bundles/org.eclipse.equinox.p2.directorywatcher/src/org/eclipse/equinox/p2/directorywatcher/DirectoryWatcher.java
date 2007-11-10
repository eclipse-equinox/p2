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
import java.io.IOException;
import java.util.*;
import org.osgi.framework.BundleContext;

public class DirectoryWatcher extends Thread {
	public final static String POLL = "eclipse.p2.directory.watcher.poll";
	public final static String DIR = "eclipse.p2.directory.watcher.dir";
	public final static String DEBUG = "eclipse.p2.directory.watcher.debug";
	private static long debug;

	public static void log(String string, Throwable e) {
		System.err.println(string + ": " + e);
		if (debug > 0)
			e.printStackTrace();
	}

	File targetDirectory;
	boolean done = false;
	long poll = 2000;
	Map processedBundles = new HashMap();
	Map processedConfigs = new HashMap();
	private Set listeners = new HashSet();
	private HashSet scannedFiles = new HashSet();
	private HashSet removals;
	private Set pendingDeletions;
	private Object runningLock = new Object();
	private boolean running = false;

	public DirectoryWatcher(Dictionary properties, BundleContext context) {
		super("Directory Watcher");
		poll = getLong(context.getProperty(POLL), poll);
		debug = getLong(context.getProperty(DEBUG), -1);

		String dir = (String) properties.get(DIR);
		if (dir == null)
			dir = "./load";

		try {
			targetDirectory = new File(dir).getCanonicalFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		targetDirectory.mkdirs();
	}

	public void addListener(IDirectoryChangeListener listener) {
		listeners.add(listener);
	}

	public void start() {
		super.start();
		synchronized (runningLock) {
			while (!running)
				try {
					runningLock.wait();
				} catch (InterruptedException e) {
					// reset interrupted state
					Thread.currentThread().interrupt();
				}
		}
	}

	public synchronized void close() {
		done = true;
		notify();
	}

	private long getLong(String value, long defaultValue) {
		if (value != null)
			try {
				return Long.parseLong(value);
			} catch (Exception e) {
				System.out.println(value + " is not a long");
			}
		return defaultValue;
	}

	public File getTargetDirectory() {
		return targetDirectory;
	}

	private boolean isInterested(IDirectoryChangeListener listener, File file) {
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
			IDirectoryChangeListener listener = (IDirectoryChangeListener) i.next();
			for (Iterator j = removed.iterator(); j.hasNext();) {
				File file = (File) j.next();
				if (isInterested(listener, file))
					listener.removed(file);
			}
		}
	}

	private void processFile(File file, IDirectoryChangeListener listener) {
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

	public void run() {
		if (debug > 0) {
			System.out.println(POLL + "(ms) " + poll);
			System.out.println(DIR + "  " + targetDirectory.getAbsolutePath());
			System.out.println(DEBUG + " " + debug);
		}
		synchronized (this) {
			signalRunning();
			do {
				try {
					startPoll();
					scanDirectory();
					stopPoll();
					notify();
					wait(poll);
				} catch (InterruptedException e) {
					// ignore
				} catch (Throwable e) {
					e.printStackTrace();
					log("In main loop, we have serious trouble", e);
					done = true;
				}
			} while (!done);
		}
	}

	private void signalRunning() {
		synchronized (runningLock) {
			running = true;
			runningLock.notify();
		}
	}

	private void scanDirectory() {
		File list[] = targetDirectory.listFiles();
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
					IDirectoryChangeListener listener = (IDirectoryChangeListener) iterator.next();
					if (isInterested(listener, file))
						processFile(file, listener);
				}
			}
		}
	}

	private void startPoll() {
		removals = scannedFiles;
		scannedFiles = new HashSet();
		pendingDeletions = new HashSet();
		for (Iterator i = listeners.iterator(); i.hasNext();)
			((IDirectoryChangeListener) i.next()).startPoll();
	}

	private void stopPoll() {
		notifyRemovals();
		removals = scannedFiles;
		for (Iterator i = listeners.iterator(); i.hasNext();)
			((IDirectoryChangeListener) i.next()).stopPoll();
		processPendingDeletions();
	}

	public synchronized void poll() {
		notify();
		try {
			wait();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
