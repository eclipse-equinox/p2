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
*  IBM - continuing development
*  Mykola Nikishov - continuing development
*******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing;

import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository.ArtifactOutputStream;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
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
	 * Check to see that we have processors for all required steps in the given descriptor
	 * @param descriptor the descriptor to check
	 * @return whether or not processors for all the descriptor's steps are installed
	 * @see IProcessingStepDescriptor#isRequired()
	 */
	public static boolean canProcess(IArtifactDescriptor descriptor) {
		IExtensionPoint point = ofNullable(RegistryFactory.getRegistry())
			.map(r -> r.getExtensionPoint(PROCESSING_STEPS_EXTENSION_ID))
			.orElse(null);
		if (point == null)
			return false;

		List<String> processorIds = stream(descriptor.getProcessingSteps())
			// ignore steps that are not required
			.filter(IProcessingStepDescriptor::isRequired)
			.map(IProcessingStepDescriptor::getProcessorId)
			.collect(toList());
		try {
			for (String processorId : processorIds) {
				IExtension requiredExtension = point.getExtension(processorId);
				if (requiredExtension == null)
					return false;

				List<IConfigurationElement> stepConfigs = stream(requiredExtension.getConfigurationElements())
					// skip anything but step elements
					.filter(config -> "step".equals(config.getName())) //$NON-NLS-1$
					.collect(toList());
				if (stepConfigs.size() != 1)
					// do not tolerate no or multiple step elements
					return false;

				IConfigurationElement stepConfig = stepConfigs.get(0);
				ProcessingStep stepToCheck = (ProcessingStep) stepConfig.createExecutableExtension("class"); //$NON-NLS-1$
				if (!stepToCheck.isEnabled())
					return false;
			}
		} catch (InvalidRegistryObjectException e) {
			// extension is no longer valid, log and ignore
			LogHelper.log(new Status(IStatus.WARNING, Activator.ID, "", e)); //$NON-NLS-1$
			return false;
		} catch (CoreException e) {
			// unable to instantiate an extension, already logged, ignore
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
		ArrayList<IStatus> list = new ArrayList<>();
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
		ArrayList<IStatus> list = new ArrayList<>();
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
