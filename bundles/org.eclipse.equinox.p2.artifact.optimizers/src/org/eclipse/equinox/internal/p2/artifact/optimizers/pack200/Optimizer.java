/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
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
import java.util.Iterator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepHandler;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ProcessingStepDescriptor;
import org.eclipse.osgi.util.NLS;

public class Optimizer {
	private IArtifactRepository repository;

	public Optimizer(IArtifactRepository repository) {
		this.repository = repository;
	}

	public void run() {
		IQueryResult<IArtifactKey> keys = repository.query(ArtifactKeyQuery.ALL_KEYS, null);
		for (Iterator<IArtifactKey> iterator = keys.iterator(); iterator.hasNext();) {
			IArtifactKey key = iterator.next();
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
		IProcessingStepDescriptor[] steps = new IProcessingStepDescriptor[] {new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Pack200Unpacker", null, true)}; //$NON-NLS-1$
		newDescriptor.setProcessingSteps(steps);
		newDescriptor.setProperty(IArtifactDescriptor.FORMAT, IArtifactDescriptor.FORMAT_PACKED);
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
		} catch (ProvisionException e) {
			System.out.println(NLS.bind(Messages.skip_optimization, descriptor.getArtifactKey()));
			System.out.println(e.getMessage());
			e.printStackTrace();
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
		return IArtifactDescriptor.FORMAT_PACKED.equals(descriptor.getProperty(IArtifactDescriptor.FORMAT));
	}

}
