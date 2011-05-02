/*******************************************************************************
* Copyright (c) 2007, 2010 compeople AG and others.
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
import java.util.ArrayList;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository.ArtifactOutputStream;
import org.eclipse.equinox.internal.provisional.p2.repository.IStateful;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IProcessingStepDescriptor;
import org.eclipse.osgi.util.NLS;

/**
 * Creates processing step instances from extensions and executes them.
 */
public class ProcessingStepHandler {

	private static final String PROCESSING_STEPS_EXTENSION_ID = "org.eclipse.equinox.p2.artifact.repository.processingSteps"; //$NON-NLS-1$

	//TODO This method can go
	public static IStatus checkStatus(OutputStream output) {
		return getStatus(output, true);
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
		IProcessingStepDescriptor[] steps = descriptor.getProcessingSteps();
		for (int i = 0; i < steps.length; i++) {
			if (point.getExtension(steps[i].getProcessorId()) == null)
				return false;
		}
		return true;
	}

	/**
	 * Return the status of this step.  The status will be <code>null</code> if the
	 * step has not yet executed. If the step has executed the returned status
	 * indicates the success or failure of the step.
	 * @param deep whether or not to aggregate the status of any linked steps
	 * @return the requested status 
	 */
	public static IStatus getStatus(OutputStream stream, boolean deep) {
		if (!deep)
			return getStatus(stream);
		ArrayList<IStatus> list = new ArrayList<IStatus>();
		int severity = collectStatus(stream, list);
		if (severity == IStatus.OK)
			return Status.OK_STATUS;
		IStatus[] result = list.toArray(new IStatus[list.size()]);
		return new MultiStatus(Activator.ID, severity, result, Messages.processing_step_results, null);
	}

	/**
	 * Return statuses from this step and any linked step, discarding OK statuses until an error status is received.
	 * @param stream the stream representing the first step
	 * @return the requested status
	 */
	public static IStatus getErrorStatus(OutputStream stream) {
		ArrayList<IStatus> list = new ArrayList<IStatus>();
		int severity = collectErrorStatus(stream, list);
		if (severity == IStatus.OK)
			return Status.OK_STATUS;
		IStatus[] result = list.toArray(new IStatus[list.size()]);
		return new MultiStatus(Activator.ID, 0, result, Messages.processing_step_results, null);
	}

	private static int collectErrorStatus(OutputStream stream, ArrayList<IStatus> list) {
		IStatus status = getStatus(stream);
		if (!status.isOK())
			list.add(status);
		if (status.matches(IStatus.ERROR))
			// Errors past this should be bogus as they rely on output from this step
			return status.getSeverity();

		OutputStream destination = getDestination(stream);
		if (destination == null || !(destination instanceof IStateful))
			return status.getSeverity();
		int result = collectErrorStatus(destination, list);
		// TODO greater than test here is a little brittle but it is very unlikely that we will add
		// a new status severity.
		return status.getSeverity() > result ? status.getSeverity() : result;
	}

	public static IStatus getStatus(OutputStream stream) {
		if (stream instanceof IStateful)
			return ((IStateful) stream).getStatus();
		return Status.OK_STATUS;
	}

	private static int collectStatus(OutputStream stream, ArrayList<IStatus> list) {
		IStatus status = getStatus(stream);
		list.add(status);
		OutputStream destination = getDestination(stream);
		if (destination == null || !(destination instanceof IStateful))
			return status.getSeverity();
		int result = collectStatus(destination, list);
		// TODO greater than test here is a little brittle but it is very unlikely that we will add
		// a new status severity.
		return status.getSeverity() > result ? status.getSeverity() : result;
	}

	private static OutputStream getDestination(OutputStream stream) {
		if (stream instanceof ProcessingStep)
			return ((ProcessingStep) stream).getDestination();
		if (stream instanceof ArtifactOutputStream)
			return ((ArtifactOutputStream) stream).getDestination();
		return null;
	}

	public ProcessingStep[] create(IProvisioningAgent agent, IProcessingStepDescriptor[] descriptors, IArtifactDescriptor context) {
		ProcessingStep[] result = new ProcessingStep[descriptors.length];
		for (int i = 0; i < descriptors.length; i++)
			result[i] = create(agent, descriptors[i], context);
		return result;
	}

	public ProcessingStep create(IProvisioningAgent agent, IProcessingStepDescriptor descriptor, IArtifactDescriptor context) {
		IExtensionRegistry registry = RegistryFactory.getRegistry();
		IExtension extension = registry.getExtension(PROCESSING_STEPS_EXTENSION_ID, descriptor.getProcessorId());
		Exception error;
		if (extension != null) {
			IConfigurationElement[] config = extension.getConfigurationElements();
			try {
				Object object = config[0].createExecutableExtension("class"); //$NON-NLS-1$
				ProcessingStep step = (ProcessingStep) object;
				step.initialize(agent, descriptor, context);
				return step;
			} catch (Exception e) {
				error = e;
			}
		} else
			error = new ProcessingStepHandlerException(NLS.bind(Messages.cannot_get_extension, PROCESSING_STEPS_EXTENSION_ID, descriptor.getProcessorId()));

		int severity = descriptor.isRequired() ? IStatus.ERROR : IStatus.INFO;
		ProcessingStep result = new EmptyProcessingStep();
		result.setStatus(new Status(severity, Activator.ID, Messages.cannot_instantiate_step + descriptor.getProcessorId(), error));
		return result;
	}

	public OutputStream createAndLink(IProvisioningAgent agent, IProcessingStepDescriptor[] descriptors, IArtifactDescriptor context, OutputStream output, IProgressMonitor monitor) {
		if (descriptors == null)
			return output;
		ProcessingStep[] steps = create(agent, descriptors, context);
		return link(steps, output, monitor);
	}

	public OutputStream link(ProcessingStep[] steps, OutputStream output, IProgressMonitor monitor) {
		OutputStream previous = output;
		for (int i = steps.length - 1; i >= 0; i--) {
			ProcessingStep step = steps[i];
			step.link(previous, monitor);
			previous = step;
		}
		if (steps.length == 0)
			return previous;
		// now link the artifact stream to the first stream in the new chain 
		ArtifactOutputStream lastLink = getArtifactStream(previous);
		if (lastLink != null)
			lastLink.setFirstLink(previous);
		return previous;
	}

	// Traverse the chain of processing steps and return the stream served up by
	// the artifact repository or null if one cannot be found.
	private ArtifactOutputStream getArtifactStream(OutputStream stream) {
		OutputStream current = stream;
		while (current instanceof ProcessingStep)
			current = ((ProcessingStep) current).getDestination();
		if (current instanceof ArtifactOutputStream)
			return (ArtifactOutputStream) current;
		return null;
	}

	protected static final class EmptyProcessingStep extends ProcessingStep {
		// Just to hold the status
	}

	protected static final class ProcessingStepHandlerException extends Exception {
		private static final long serialVersionUID = 1L;

		public ProcessingStepHandlerException(String message) {
			super(message);
		}
	}
}
