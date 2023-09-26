/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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
 *     Sonatype, Inc. - transport split
 *     Wind River - continue development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.processors.checksum.ChecksumUtilities;
import org.eclipse.equinox.internal.p2.artifact.processors.checksum.ChecksumVerifier;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.repository.Activator;
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

	@Override
	public void perform(IArtifactRepository sourceRepository, IProgressMonitor monitor) {
		SubMonitor subMon = SubMonitor.convert(monitor, NLS.bind(Messages.downloading, getArtifactKey().getId()), 1);
		setSourceRepository(sourceRepository);
		// Do we already have the descriptor in the target?
		if (target.contains(targetDescriptor)) {
			setResult(Status.info(NLS.bind(Messages.mirror_alreadyExists, targetDescriptor, target)));
			return;
		}
		// Does the source actually have the descriptor?
		if (!source.contains(getArtifactDescriptor())) {
			setResult(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.artifact_not_found, getArtifactKey())));
			return;
		}
		IStatus status = transfer(targetDescriptor, sourceDescriptor, subMon.newChild(1));

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
	@Override
	protected IStatus getArtifact(IArtifactDescriptor artifactDescriptor, OutputStream destination, IProgressMonitor monitor) {
		SubMonitor subMon = SubMonitor.convert(monitor, 2);
		if (SimpleArtifactRepository.CHECKSUMS_ENABLED) {
			Collection<ChecksumVerifier> steps = ChecksumUtilities.getChecksumVerifiers(artifactDescriptor,
					IArtifactDescriptor.DOWNLOAD_CHECKSUM, Collections.emptySet());
			if (steps.isEmpty()) {
				LogHelper.log(new Status(IStatus.WARNING, Activator.ID,
						NLS.bind(Messages.noDigestAlgorithmToVerifyDownload, artifactDescriptor.getArtifactKey())));
			}
			ProcessingStep[] stepArray = steps.toArray(new ProcessingStep[steps.size()]);
			// TODO should probably be using createAndLink here
			ProcessingStepHandler handler = new ProcessingStepHandler();
			destination = handler.link(stepArray, destination, subMon.split(1));
		}
		subMon.setWorkRemaining(1);
		return getSourceRepository().getRawArtifact(artifactDescriptor, destination, subMon.split(1));
	}
}
