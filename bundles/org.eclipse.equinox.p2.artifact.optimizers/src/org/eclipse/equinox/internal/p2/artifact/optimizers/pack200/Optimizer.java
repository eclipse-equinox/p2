/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.optimizers.pack200;

import java.io.IOException;
import java.io.OutputStream;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.osgi.util.NLS;

public class Optimizer {
	private static final String PACKED_FORMAT = "packed"; //$NON-NLS-1$
	private IArtifactRepository repository;

	public Optimizer(IArtifactRepository repository) {
		this.repository = repository;
	}

	public void run() {
		IArtifactKey[] keys = repository.getArtifactKeys();
		for (int i = 0; i < keys.length; i++) {
			IArtifactKey key = keys[i];
			if (!key.getClassifier().equals("plugin")) //$NON-NLS-1$
				continue;
			IArtifactDescriptor[] descriptors = repository.getArtifactDescriptors(key);
			IArtifactDescriptor canonical = null;
			boolean optimized = false;
			for (int j = 0; j < descriptors.length; j++) {
				IArtifactDescriptor descriptor = descriptors[j];
				if (isCanonical(descriptor))
					canonical = descriptor;
				optimized |= isOptimized(descriptor);
			}
			if (!optimized)
				optimize(canonical);
		}
	}

	private boolean isCanonical(IArtifactDescriptor descriptor) {
		// TODO length != 0 is not necessarily an indicator for not being complete!
		String format = descriptor.getProperty(IArtifactDescriptor.FORMAT);
		if (format == null)
			return true;
		return false;
	}

	private void optimize(IArtifactDescriptor descriptor) {
		ArtifactDescriptor newDescriptor = new ArtifactDescriptor(descriptor);
		ProcessingStepDescriptor[] steps = new ProcessingStepDescriptor[] {new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Pack200Unpacker", null, true)}; //$NON-NLS-1$
		newDescriptor.setProcessingSteps(steps);
		newDescriptor.setProperty(IArtifactDescriptor.FORMAT, PACKED_FORMAT);
		OutputStream repositoryStream = null;
		try {
			repositoryStream = repository.getOutputStream(newDescriptor);

			// Add in all the processing steps needed to optimize (e.g., pack200, ...)
			ProcessingStepHandler handler = new ProcessingStepHandler();
			OutputStream destination = handler.link(new ProcessingStep[] {new Pack200OptimizerStep()}, repositoryStream, null);

			// Do the actual work by asking the repo to get the artifact and put it in the destination.
			IStatus status = repository.getArtifact(descriptor, destination, new NullProgressMonitor());
			if (!status.isOK()) {
				System.out.println(NLS.bind(Messages.failed_getting_artifact, descriptor.getArtifactKey()));
				System.out.println(status);
			}
		} finally {
			if (repositoryStream != null)
				try {
					repositoryStream.close();
					IStatus status = ProcessingStepHandler.checkStatus(repositoryStream);
					if (!status.isOK()) {
						System.out.println(NLS.bind(Messages.skip_optimization, descriptor.getArtifactKey()));
						System.out.println(status.toString());
					}
				} catch (IOException e) {
					System.out.println(NLS.bind(Messages.skip_optimization, descriptor.getArtifactKey()));
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
		}
	}

	private boolean isOptimized(IArtifactDescriptor descriptor) {
		return PACKED_FORMAT.equals(descriptor.getProperty(IArtifactDescriptor.FORMAT));
	}

}
