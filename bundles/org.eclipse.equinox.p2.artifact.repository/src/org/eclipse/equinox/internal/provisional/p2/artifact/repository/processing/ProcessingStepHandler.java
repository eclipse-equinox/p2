/*******************************************************************************
* Copyright (c) 2007, 2008 compeople AG and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* 	compeople AG (Stefan Liebig) - initial API and implementation
*  IBM - continuing development
*******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing;

import java.io.OutputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;

/**
 * Creates processing step instances from extensions and executes them.
 */
public class ProcessingStepHandler {

	private static final String PROCESSING_STEPS_EXTENSION_ID = "org.eclipse.equinox.p2.artifact.repository.processingSteps"; //$NON-NLS-1$

	public static IStatus checkStatus(OutputStream output) {
		if (!(output instanceof ProcessingStep))
			return Status.OK_STATUS;
		return ((ProcessingStep) output).getStatus(true);
	}

	/**
	 * Check to see that we have processors for all the steps in the given descriptor
	 * @param descriptor the descriptor to check
	 * @return whether or not processors for all the descriptor's steps are installed
	 */
	public static boolean canProcess(IArtifactDescriptor descriptor) {
		IExtensionRegistry registry = RegistryFactory.getRegistry();
		IExtensionPoint point = registry.getExtensionPoint(PROCESSING_STEPS_EXTENSION_ID);
		if (point == null)
			return false;
		ProcessingStepDescriptor[] steps = descriptor.getProcessingSteps();
		for (int i = 0; i < steps.length; i++) {
			if (point.getExtension(steps[i].getProcessorId()) == null)
				return false;
		}
		return true;
	}

	public ProcessingStep[] create(ProcessingStepDescriptor[] descriptors, IArtifactDescriptor context) {
		ProcessingStep[] result = new ProcessingStep[descriptors.length];
		for (int i = 0; i < descriptors.length; i++)
			result[i] = create(descriptors[i], context);
		return result;
	}

	public ProcessingStep create(ProcessingStepDescriptor descriptor, IArtifactDescriptor context) {
		IExtensionRegistry registry = RegistryFactory.getRegistry();
		IExtension extension = registry.getExtension(PROCESSING_STEPS_EXTENSION_ID, descriptor.getProcessorId());
		Exception error;
		if (extension != null) {
			IConfigurationElement[] config = extension.getConfigurationElements();
			try {
				Object object = config[0].createExecutableExtension("class"); //$NON-NLS-1$
				ProcessingStep step = (ProcessingStep) object;
				step.initialize(descriptor, context);
				return step;
			} catch (Exception e) {
				error = e;
			}
		} else
			error = new ProcessingStepHandlerException("Could not get extension " + PROCESSING_STEPS_EXTENSION_ID + " for desriptor id " + descriptor.getProcessorId());

		int severity = descriptor.isRequired() ? IStatus.ERROR : IStatus.INFO;
		ProcessingStep result = new EmptyProcessingStep();
		result.setStatus(new Status(severity, Activator.ID, "Could not instantiate step:" + descriptor.getProcessorId(), error));
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

	protected static final class EmptyProcessingStep extends ProcessingStep {
		// Just to hold the status
	}

	protected static final class ProcessingStepHandlerException extends Exception {
		public ProcessingStepHandlerException(String message) {
			super(message);
		}
	}
}
