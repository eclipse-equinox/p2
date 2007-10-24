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
import org.eclipse.equinox.p2.artifact.repository.*;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStepDescriptor;
import org.eclipse.equinox.p2.metadata.IArtifactKey;

public class MirrorRequest extends ArtifactRequest {
	private static final ProcessingStepDescriptor[] EMPTY_STEPS = new ProcessingStepDescriptor[0];

	private IArtifactRepository target;

	public MirrorRequest(IArtifactKey key, IArtifactRepository targetRepository) {
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
			if (descriptors.length > 0) {
				boolean optimizedFound = false;
				for (int i = 0; i < descriptors.length; i++) {
					if (descriptors[i].getProcessingSteps().length == 0) {
						if (!optimizedFound)
							descriptor = descriptors[i];
					} else {
						optimizedFound = true;
						descriptor = descriptors[i];
					}
				}
			}
		}

		// if the descriptor is not set now then the repo does not have the requested artifact
		if (descriptor == null) {
			setResult(new Status(IStatus.ERROR, Activator.ID, "Artifact requested can't be found:" + getArtifactKey()));
			return;
		}

		// Get the output stream to store the artifact
		// Since we are mirroring, ensure we clear out data from the original descriptor that may
		// not apply in the new repo location.
		// TODO this is brittle.  perhaps the repo itself should do this?  there are cases wehre
		// we really do need to give the repo the actual descriptor to use however...
		ArtifactDescriptor destinationDescriptor = new ArtifactDescriptor(getArtifactDescriptor());
		destinationDescriptor.setProcessingSteps(EMPTY_STEPS);
		destinationDescriptor.setProperty(IArtifactDescriptor.DOWNLOAD_MD5, null);
		//		clonedDescriptor.setProperty(IArtifactDescriptor.DOWNLOAD_SIZE, null);

		OutputStream destination = target.getOutputStream(destinationDescriptor);
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
