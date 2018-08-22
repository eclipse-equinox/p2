/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.extensionlocation;

import java.io.File;
import java.util.*;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryChangeListener;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.ArtifactKeyQuery;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;

public class BundlePoolFilteredListener extends DirectoryChangeListener {

	private DirectoryChangeListener delegate;
	private Set<File> bundlePoolFiles = new HashSet<>();

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

	@Override
	public boolean added(File file) {
		return delegate.added(file);
	}

	@Override
	public boolean changed(File file) {
		return delegate.changed(file);
	}

	@Override
	public Long getSeenFile(File file) {
		return delegate.getSeenFile(file);
	}

	@Override
	public boolean isInterested(File file) {
		if (bundlePoolFiles.contains(file))
			return false;

		return delegate.isInterested(file);
	}

	@Override
	public boolean removed(File file) {
		return delegate.removed(file);
	}

	@Override
	public void startPoll() {
		delegate.startPoll();
	}

	@Override
	public void stopPoll() {
		delegate.stopPoll();
	}

}
