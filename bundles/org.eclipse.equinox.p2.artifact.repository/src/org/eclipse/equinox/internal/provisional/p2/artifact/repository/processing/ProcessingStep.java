/*******************************************************************************
* Copyright (c) 2007, 2010 compeople AG and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* 	compeople AG (Stefan Liebig) - initial API and implementation
*   IBM Corporation - continuing development
*******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing;

import java.io.IOException;
import java.io.OutputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.repository.IStateful;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IProcessingStepDescriptor;

/**
 * ProcessingSteps process the data written to them and pass the resultant data on
 * to a configured destination stream.  Steps may monitor (e.g., count) the data, compute information 
 * about the data (e.g., checksum or hash) or transform the data (e.g., unpack200).
 */
public abstract class ProcessingStep extends OutputStream implements IStateful {

	private OutputStream destination;
	private IProgressMonitor monitor;
	private IStatus status = Status.OK_STATUS;

	protected ProcessingStep() {
		super();
	}

	/**
	 * Initialize this processing step according to the information in the given 
	 * descriptor and context.  After initialization, this step is ready for linking 
	 * with other steps or output streams
	 * @param descriptor description of the step
	 * @param context the context in which the step is being used
	 */
	public void initialize(IProvisioningAgent agent, IProcessingStepDescriptor descriptor, IArtifactDescriptor context) {
		// nothing to do here!
	}

	/**
	 * Link this step with the given output stream and configure the step to use the given
	 * progress monitor.  After linking the step is ready to have data written to it.
	 * @param destination the stream into which to write the processed data
	 * @param monitor the progress monitor to use for reporting activity
	 */
	public void link(OutputStream destination, IProgressMonitor monitor) {
		this.destination = destination;
		this.monitor = monitor;
	}

	/**
	 * Process the given byte and pass the result on to the configured destination stream
	 * @param b the byte being written
	 */
	public void write(int b) throws IOException {
		// nothing to do here!
	}

	/** 
	 * Flush any unwritten data from this stream.
	 */
	public void flush() throws IOException {
		super.flush();
		if (destination != null)
			destination.flush();
	}

	/**
	 * Close this stream and, if the configured destination is a ProcessingStep, 
	 * close it as well.  Typically a chain of steps terminates in a conventional 
	 * output stream.  Implementors of this method should ensure they set the 
	 * status of the step.
	 */
	public void close() throws IOException {
		super.close();
		if (destination instanceof ProcessingStep)
			destination.close();
		monitor = null;
	}

	public IStatus getStatus() {
		return status;
	}

	public void setStatus(IStatus value) {
		if (value == null)
			value = Status.OK_STATUS;
		if (status != null && status.getSeverity() >= value.getSeverity())
			return;
		status = value;
	}

	/**
	 * Get the progress monitor. 
	 * @return the progress monitor; may be null
	 */
	protected IProgressMonitor getProgressMonitor() {
		return monitor;
	}

	/**
	 * Get the stream to write the processed data into.
	 * 
	 * @return output stream for processed data
	 */
	protected OutputStream getDestination() {
		return destination;
	}

	/**
	 * Return the status of this step.  The status will be <code>null</code> if the
	 * step has not yet executed. If the step has executed the returned status
	 * indicates the success or failure of the step.
	 * @param deep whether or not to aggregate the status of any linked steps
	 * @return the requested status 
	 */
	public IStatus getStatus(boolean deep) {
		return ProcessingStepHandler.getStatus(this, deep);
	}
}
