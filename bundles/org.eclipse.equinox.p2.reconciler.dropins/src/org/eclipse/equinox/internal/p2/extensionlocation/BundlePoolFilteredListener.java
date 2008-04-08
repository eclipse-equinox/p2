/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.extensionlocation;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.equinox.internal.p2.reconciler.dropins.Activator;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IFileArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.directorywatcher.DirectoryChangeListener;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.osgi.framework.BundleContext;

public class BundlePoolFilteredListener extends DirectoryChangeListener {

	private DirectoryChangeListener delegate;
	private Set bundlePoolFiles = new HashSet();

	public BundlePoolFilteredListener(BundleContext context, DirectoryChangeListener listener) {
		delegate = listener;
		IFileArtifactRepository bundlePool = Util.getBundlePoolRepository(Activator.getCurrentProfile(context));
		IArtifactKey[] keys = bundlePool.getArtifactKeys();
		for (int i = 0; i < keys.length; i++) {
			File artifactFile = bundlePool.getArtifactFile(keys[i]);
			if (artifactFile != null)
				bundlePoolFiles.add(artifactFile);
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
