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
package org.eclipse.equinox.internal.p2.artifact.repository;

import java.io.IOException;
import java.io.OutputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.p2.artifact.repository.IWritableArtifactRepository;
import org.eclipse.equinox.p2.metadata.IArtifactKey;

public class MirrorRequest extends ArtifactRequest {
	private IWritableArtifactRepository target;

	public MirrorRequest(IArtifactKey key, IWritableArtifactRepository targetRepository) {
		super(key);
		target = targetRepository;
	}

	public void perform(IProgressMonitor monitor) {
		monitor.subTask("Downloading " + getArtifactKey().getId());
		// Do we already have the artifact in the target?
		if (target.contains(getArtifactKey())) {
			setResult(new Status(IStatus.OK, Activator.ID, "The artifact is already available in the repo."));
			return;
		}

		// if the request does not have a descriptor then try to fill one in by getting
		// the list of all and randomly picking the first one.
		if (descriptor == null) {
			IArtifactDescriptor[] descriptors = source.getArtifactDescriptors(getArtifactKey());
			if (descriptors.length > 0)
				descriptor = descriptors[0];
		}

		// if the descriptor is not set now then the repo does not have the requested artifact
		if (descriptor == null) {
			setResult(new Status(IStatus.ERROR, Activator.ID, "Artifact requested can't be found:" + getArtifactKey()));
			return;
		}

		// Get the output stream to store the artifact
		OutputStream destination = target.getOutputStream(getArtifactDescriptor());
		if (destination == null) {
			setResult(new Status(IStatus.ERROR, Activator.ID, "Can't get an output stream to " + target + " to store " + getArtifactKey()));
			return;
		}

		// Do the actual transfer
		try {
			setResult(source.getArtifact(descriptor, destination, monitor));
			return;
		} finally {
			try {
				destination.close();
			} catch (IOException e) {
				setResult(new Status(IStatus.ERROR, Activator.ID, "Error closing the output stream for " + getArtifactKey() + "on repo " + target.getLocation(), e));
			}
		}
	}

	public String toString() {
		return "Mirrroring: " + getArtifactKey() + " into " + target;
	}
}
