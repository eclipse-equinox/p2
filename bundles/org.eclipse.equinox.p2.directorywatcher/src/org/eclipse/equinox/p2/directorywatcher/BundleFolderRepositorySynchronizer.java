/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM Corporation - initial implementation and ideas 
 ******************************************************************************/
package org.eclipse.equinox.p2.directorywatcher;

import java.io.File;
import java.util.*;
import org.eclipse.equinox.internal.p2.directorywatcher.Activator;

public class BundleFolderRepositorySynchronizer {

	private final DirectoryWatcher watcher;
	private final RepositoryListener listener;
	private final List listeners = new ArrayList();
	private long lastModified;

	public BundleFolderRepositorySynchronizer(File directory) {
		this(new File[] {directory});
	}

	public BundleFolderRepositorySynchronizer(File[] directories) {
		watcher = new DirectoryWatcher(directories);
		listener = new RepositoryListener(Activator.getContext(), Integer.toString(directories[0].hashCode()));
		watcher.addListener(listener);

		DirectoryChangeListener updatedListener = new DirectoryChangeListener() {
			public void stopPoll() {
				checkForUpdates();
			}

		};
		watcher.addListener(updatedListener);

	}

	synchronized void checkForUpdates() {
		if (listener.getLastModified() > lastModified) {
			lastModified = listener.getLastModified();
			RepositoryUpdatedEvent event = new RepositoryUpdatedEvent(listener.getMetadataRepository(), listener.getArtifactRepository());
			for (Iterator it = listeners.iterator(); it.hasNext();) {
				RepositoryUpdatedListener listener = (RepositoryUpdatedListener) it.next();
				listener.updated(event);
			}
		}
	}

	public void start(long pollFrequency) {
		watcher.start(pollFrequency);
	}

	public void stop() {
		watcher.stop();
	}

	public void poll() {
		watcher.poll();
	}

	public synchronized void addRepositoryListener(RepositoryUpdatedListener listener) {
		listeners.add(listener);
	}

	public synchronized void removeRepositoryListener(RepositoryUpdatedListener listener) {
		listeners.remove(listener);
	}
}
