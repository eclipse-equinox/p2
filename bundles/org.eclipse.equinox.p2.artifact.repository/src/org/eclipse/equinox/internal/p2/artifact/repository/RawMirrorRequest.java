/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - transport split
 *     Wind River - continue development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository;

import java.io.OutputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.processors.md5.MD5Verifier;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepHandler;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.osgi.util.NLS;

public class RawMirrorRequest extends MirrorRequest {
	protected IArtifactDescriptor sourceDescriptor, targetDescriptor;

	public RawMirrorRequest(IArtifactDescriptor sourceDescriptor, IArtifactDescriptor targetDescriptor, IArtifactRepository targetRepository, Transport transport) {
		this(sourceDescriptor, targetDescriptor, targetRepository, transport, null);
	}

	public RawMirrorRequest(IArtifactDescriptor sourceDescriptor, IArtifactDescriptor targetDescriptor, IArtifactRepository targetRepository, Transport transport, String statsParameters) {
		super(sourceDescriptor.getArtifactKey(), targetRepository, null, null, transport, statsParameters);
		this.sourceDescriptor = sourceDescriptor;
		this.targetDescriptor = targetDescriptor;
	}

	public void perform(IArtifactRepository sourceRepository, IProgressMonitor monitor) {
		monitor.subTask(NLS.bind(Messages.downloading, getArtifactKey().getId()));
		setSourceRepository(sourceRepository);
		// Do we already have the descriptor in the target?
		if (target.contains(targetDescriptor)) {
			setResult(new Status(IStatus.INFO, Activator.ID, NLS.bind(Messages.mirror_alreadyExists, targetDescriptor, target)));
			return;
		}
		// Does the source actually have the descriptor?
		if (!source.contains(getArtifactDescriptor())) {
			setResult(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.artifact_not_found, getArtifactKey())));
			return;
		}
		IStatus status = transfer(targetDescriptor, sourceDescriptor, monitor);

		// if ok, cancelled or transfer has already been done with the canonical form return with status set 
		if (status.getSeverity() == IStatus.CANCEL) {
			setResult(status);
			return;
		}
		if (monitor.isCanceled()) {
			setResult(Status.CANCEL_STATUS);
			return;
		}
		if (status.isOK()) {
			setResult(status);
			return;
		}

		// failed, first remove possibly erroneously added descriptor
		if (target.contains(targetDescriptor))
			target.removeDescriptor(targetDescriptor);

		setResult(status);
	}

	public IArtifactDescriptor getArtifactDescriptor() {
		return sourceDescriptor;
	}

	// Perform the mirror operation without any processing steps
	protected IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
		ProcessingStepHandler handler = new ProcessingStepHandler();
		if (SimpleArtifactRepository.MD5_CHECK_ENABLED && descriptor.getProperty(IArtifactDescriptor.DOWNLOAD_MD5) != null)
			destination = handler.link(new ProcessingStep[] {new MD5Verifier(descriptor.getProperty(IArtifactDescriptor.DOWNLOAD_MD5))}, destination, monitor);
		return getSourceRepository().getRawArtifact(descriptor, destination, monitor);
	}
}
