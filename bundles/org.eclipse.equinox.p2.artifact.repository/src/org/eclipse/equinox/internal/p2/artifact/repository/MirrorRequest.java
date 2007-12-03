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
import java.util.Properties;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.artifact.repository.*;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStepDescriptor;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStepHandler;
import org.eclipse.equinox.p2.metadata.IArtifactKey;

public class MirrorRequest extends ArtifactRequest {
	private static final ProcessingStepDescriptor[] EMPTY_STEPS = new ProcessingStepDescriptor[0];

	private final IArtifactRepository target;

	private final Properties targetDescriptorProperties;

	public MirrorRequest(IArtifactKey key, IArtifactRepository targetRepository, Properties targetDescriptorProperties) {
		super(key);
		target = targetRepository;
		if (targetDescriptorProperties == null || targetDescriptorProperties.isEmpty()) {
			this.targetDescriptorProperties = null;
		} else {
			this.targetDescriptorProperties = new Properties();
			this.targetDescriptorProperties.putAll(targetDescriptorProperties);
		}
	}

	public void perform(IProgressMonitor monitor) {
		monitor.subTask("Downloading " + getArtifactKey().getId());
		// Do we already have the artifact in the target?
		if (target.contains(getArtifactKey())) {
			setResult(new Status(IStatus.OK, Activator.ID, "The artifact is already available in the repo."));
			return;
		}

		// if the request does not have a descriptor then try to fill one in by getting
		// the list of all and randomly picking one that appears to be optimized.
		if (descriptor == null) {
			IArtifactDescriptor[] descriptors = source.getArtifactDescriptors(getArtifactKey());
			if (descriptors.length > 0) {
				IArtifactDescriptor optimized = null;
				IArtifactDescriptor canonical = null;
				for (int i = 0; i < descriptors.length; i++) {
					if (descriptors[i].getProperty(IArtifactDescriptor.FORMAT) == null)
						canonical = descriptors[i];
					else if (ProcessingStepHandler.canProcess(descriptors[i]))
						optimized = descriptors[i];
				}
				boolean chooseCanonical = source.getLocation().getProtocol().equals("file");
				// If the source repo is local then look for a canonical descriptor so we don't waste processing time.
				descriptor = chooseCanonical ? canonical : optimized;
				// if the descriptor is still null then we could not find our first choice of format so switch the logic.
				if (descriptor == null)
					descriptor = !chooseCanonical ? canonical : optimized;
			}
		}

		// if the descriptor is not set now then the repo does not have the requested artifact
		// TODO improve the reporting here.  It may be the case that the repo has the artifact
		// but the client does not have a processor
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
		//		destinationDescriptor.setProperty(IArtifactDescriptor.DOWNLOAD_SIZE, null);

		if (targetDescriptorProperties != null)
			destinationDescriptor.addProperties(targetDescriptorProperties);

		OutputStream destination = target.getOutputStream(destinationDescriptor);
		if (destination == null) {
			setResult(new Status(IStatus.ERROR, Activator.ID, "Can't get an output stream to " + target + " to store " + getArtifactKey()));
			return;
		}

		// Do the actual transfer
		try {
			setResult(descriptor.getRepository().getArtifact(descriptor, destination, monitor));
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
