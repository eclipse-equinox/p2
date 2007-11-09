/*******************************************************************************
 * Copyright (c) 2007 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.optimizers.jbdiff;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.optimizers.Activator;
import org.eclipse.equinox.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStepDescriptor;
import org.eclipse.equinox.p2.metadata.IArtifactKey;

/**
 * The <code>AbstractDeltaDiffStep</code> is an abstract processing step that
 * retrieves a local artifact repository containing the serialized/encoded
 * artifact key. It assumes that the artifact key is stored within the data property
 * of the processing step descriptor and that is encoded with the <code>ArtifactKeySerializer</code>.
 */
public abstract class AbstractDeltaDiffStep extends ProcessingStep {

	protected IArtifactKey key;
	protected IArtifactRepository repository;

	protected AbstractDeltaDiffStep(IArtifactRepository repository) {
		this.repository = repository;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStep#initialize(org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStepDescriptor, org.eclipse.equinox.p2.artifact.repository.IArtifactDescriptor)
	 */
	public void initialize(ProcessingStepDescriptor descriptor, IArtifactDescriptor context) {
		super.initialize(descriptor, context);

		fetchArtifactKey(descriptor);
	}

	/**
	 * Fetch the artifact key from the given processing step descriptor.
	 * @param descriptor
	 */
	private void fetchArtifactKey(ProcessingStepDescriptor descriptor) {
		try {
			key = ArtifactKeyDeSerializer.deserialize(descriptor.getData());
		} catch (IllegalArgumentException e) {
			status = new Status(IStatus.ERROR, Activator.ID, "Predecessor artifact key for delta could not be deserialized. Serialized key is " + descriptor.getData(), e);
		}
	}

}
