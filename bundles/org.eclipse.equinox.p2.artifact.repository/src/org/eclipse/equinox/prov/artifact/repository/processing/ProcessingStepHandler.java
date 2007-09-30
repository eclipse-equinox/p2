/*******************************************************************************
* Copyright (c) 2007 compeople AG and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* 	compeople AG (Stefan Liebig) - initial API and implementation
*  IBM - continuing development
*******************************************************************************/
package org.eclipse.equinox.prov.artifact.repository.processing;

import java.io.OutputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.prov.artifact.repository.Activator;
import org.eclipse.equinox.prov.artifact.repository.IArtifactDescriptor;

/**
 * Creates processing step instances from extensions and executes them.
 */
public class ProcessingStepHandler {

	public ProcessingStep[] create(ProcessingStepDescriptor[] descriptors, IArtifactDescriptor context) {
		ProcessingStep[] result = new ProcessingStep[descriptors.length];
		for (int i = 0; i < descriptors.length; i++)
			result[i] = create(descriptors[i], context);
		return result;
	}

	public ProcessingStep create(ProcessingStepDescriptor descriptor, IArtifactDescriptor context) {
		IExtensionRegistry registry = RegistryFactory.getRegistry();
		IExtension extension = registry.getExtension("org.eclipse.equinox.prov.artifact.repository.processingSteps", descriptor.getProcessorId()); //$NON-NLS-1$
		IConfigurationElement[] config = extension.getConfigurationElements();
		Exception error = null;
		try {
			Object object = config[0].createExecutableExtension("class"); //$NON-NLS-1$
			if (object instanceof ProcessingStep) {
				ProcessingStep step = (ProcessingStep) object;
				step.initialize(descriptor, context);
				return step;
			}
		} catch (CoreException e) {
			error = e;
		}
		int severity = descriptor.isRequired() ? IStatus.ERROR : IStatus.INFO;
		ProcessingStep result = new ProcessingStep() {};
		result.status = new Status(severity, Activator.ID, "Could not instantiate step:" + descriptor.getProcessorId(), error);
		return result;
	}

	public OutputStream createAndLink(ProcessingStepDescriptor[] descriptors, IArtifactDescriptor context, OutputStream output, IProgressMonitor monitor) {
		if (descriptors == null)
			return output;
		ProcessingStep[] steps = create(descriptors, context);
		return link(steps, output, monitor);
	}

	public OutputStream link(ProcessingStep[] steps, OutputStream output, IProgressMonitor monitor) {
		OutputStream previous = output;
		for (int i = steps.length - 1; i >= 0; i--) {
			ProcessingStep step = steps[i];
			step.link(previous, monitor);
			previous = step;
		}
		return previous;
	}

	public IStatus validateSteps(OutputStream output) {
		if (!(output instanceof ProcessingStep))
			return Status.OK_STATUS;
		return ((ProcessingStep) output).getStatus(true);
	}
}
