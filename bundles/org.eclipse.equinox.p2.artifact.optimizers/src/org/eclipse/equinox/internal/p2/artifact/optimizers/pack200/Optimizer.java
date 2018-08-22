/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.optimizers.pack200;

import java.io.IOException;
import java.io.OutputStream;
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
	private final IArtifactRepository repository;

	public Optimizer(IArtifactRepository repository) {
		this.repository = repository;
	}

	public void run() {
		IQueryResult<IArtifactKey> keys = repository.query(ArtifactKeyQuery.ALL_KEYS, null);
		for (IArtifactKey key : keys) {
			if (!key.getClassifier().equals("plugin")) //$NON-NLS-1$
				continue;
			IArtifactDescriptor[] descriptors = repository.getArtifactDescriptors(key);
			IArtifactDescriptor canonical = null;
			boolean optimized = false;
			for (IArtifactDescriptor descriptor : descriptors) {
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
		IProcessingStepDescriptor[] steps = new IProcessingStepDescriptor[] {
				new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Pack200Unpacker", null, true) }; //$NON-NLS-1$
		newDescriptor.setProcessingSteps(steps);
		newDescriptor.setProperty(IArtifactDescriptor.FORMAT, IArtifactDescriptor.FORMAT_PACKED);
		try (OutputStream repositoryStream = repository.getOutputStream(newDescriptor)) {

			// Add in all the processing steps needed to optimize (e.g., pack200, ...)
			ProcessingStepHandler handler = new ProcessingStepHandler();
			OutputStream destination = handler.link(new ProcessingStep[] { new Pack200OptimizerStep() },
					repositoryStream, null);

			// Do the actual work by asking the repo to get the artifact and put it in the
			// destination.
			IStatus status = repository.getArtifact(descriptor, destination, new NullProgressMonitor());
			if (!status.isOK()) {
				System.out.println(NLS.bind(Messages.failed_getting_artifact, descriptor.getArtifactKey()));
				System.out.println(status);
			}
			repositoryStream.close();
			status = ProcessingStepHandler.checkStatus(repositoryStream);
			if (!status.isOK()) {
				System.out.println(NLS.bind(Messages.skip_optimization, descriptor.getArtifactKey()));
				System.out.println(status.toString());
			}
		} catch (ProvisionException e) {
			System.out.println(NLS.bind(Messages.skip_optimization, descriptor.getArtifactKey()));
			System.out.println(e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println(NLS.bind(Messages.skip_optimization, descriptor.getArtifactKey()));
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	private boolean isOptimized(IArtifactDescriptor descriptor) {
		return IArtifactDescriptor.FORMAT_PACKED.equals(descriptor.getProperty(IArtifactDescriptor.FORMAT));
	}

}
