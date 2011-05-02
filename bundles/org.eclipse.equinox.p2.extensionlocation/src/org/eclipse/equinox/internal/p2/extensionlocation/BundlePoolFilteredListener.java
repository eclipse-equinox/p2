/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.extensionlocation;

import org.eclipse.equinox.p2.query.IQueryResult;

import java.io.File;
import java.util.*;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryChangeListener;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.ArtifactKeyQuery;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;

public class BundlePoolFilteredListener extends DirectoryChangeListener {

	private DirectoryChangeListener delegate;
	private Set<File> bundlePoolFiles = new HashSet<File>();

	public BundlePoolFilteredListener(DirectoryChangeListener listener) {
		delegate = listener;
		IFileArtifactRepository bundlePool = Activator.getBundlePoolRepository();
		if (bundlePool != null) {
			IQueryResult<IArtifactKey> keys = bundlePool.query(ArtifactKeyQuery.ALL_KEYS, null);
			for (Iterator<IArtifactKey> iterator = keys.iterator(); iterator.hasNext();) {
				IArtifactKey key = iterator.next();
				File artifactFile = bundlePool.getArtifactFile(key);
				if (artifactFile != null)
					bundlePoolFiles.add(artifactFile);
			}
		}
	}

	public boolean added(File file) {
		return delegate.added(file);
	}

	public boolean changed(File file) {
		return delegate.changed(file);
	}

	public Long getSeenFile(File file) {
		return delegate.getSeenFile(file);
	}

	public boolean isInterested(File file) {
		if (bundlePoolFiles.contains(file))
			return false;

		return delegate.isInterested(file);
	}

	public boolean removed(File file) {
		return delegate.removed(file);
	}

	public void startPoll() {
		delegate.startPoll();
	}

	public void stopPoll() {
		delegate.stopPoll();
	}

}
