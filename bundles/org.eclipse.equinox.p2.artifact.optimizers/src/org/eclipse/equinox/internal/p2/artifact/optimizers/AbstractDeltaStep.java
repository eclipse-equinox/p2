/*******************************************************************************
 * Copyright (c) 2007, 2018 compeople AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *   IBM - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.optimizers;

import java.io.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.processing.AbstractBufferingStep;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;

/**
 * The <code>AbstractDeltaDiffStep</code> is an abstract processing step that
 * retrieves a local artifact repository containing the serialized/encoded
 * artifact key. It assumes that the artifact key is stored within the data
 * property of the processing step descriptor and that is encoded with the
 * <code>ArtifactKeySerializer</code>.
 */
public abstract class AbstractDeltaStep extends AbstractBufferingStep {

	protected IArtifactKey key;
	protected IArtifactRepository repository;

	public AbstractDeltaStep() {
		this(null);
	}

	protected AbstractDeltaStep(IArtifactRepository repository) {
		super();
		this.repository = repository;
	}

	@Override
	public void initialize(IProvisioningAgent agent, IProcessingStepDescriptor descriptor,
			IArtifactDescriptor context) {
		super.initialize(agent, descriptor, context);
		readArtifactKey(descriptor);
	}

	protected void readArtifactKey(IProcessingStepDescriptor descriptor) {
		try {
			key = ArtifactKey.parse(descriptor.getData());
		} catch (IllegalArgumentException e) {
			setStatus(new Status(IStatus.ERROR, Activator.ID,
					"Predecessor artifact key for delta could not be deserialized. Serialized key is "
							+ descriptor.getData(),
					e));
		}
	}

	protected File fetchPredecessor(ArtifactDescriptor descriptor) {
		if (repository instanceof IFileArtifactRepository)
			return ((IFileArtifactRepository) repository).getArtifactFile(descriptor);
		File result = null;
		try {
			result = File.createTempFile(PREDECESSOR_ROOT, JAR_SUFFIX);
			try (OutputStream resultStream = new BufferedOutputStream(new FileOutputStream(result));) {
				setStatus(repository.getArtifact(descriptor, resultStream, getProgressMonitor()));
				return result;
			}
		} catch (IOException e) {
		}
		return null;
	}
}