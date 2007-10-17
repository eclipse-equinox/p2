/*******************************************************************************
* Copyright (c) 2007 compeople AG and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* 	compeople AG (Stefan Liebig) - initial API and implementation
*   IBM Corporation - continuing development
*******************************************************************************/
package org.eclipse.equinox.p2.artifact.repository.processing;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.p2.artifact.repository.IArtifactDescriptor;

/**
 * ProcessingSteps process the data written to them and pass the resultant data on
 * to a configured destination stream.  Steps may monitor (e.g., count) the data, compute information 
 * about the data (e.g., checksum or hash) or transform the data (e.g., unpack200).
 */
public abstract class ProcessingStep extends OutputStream {
	protected OutputStream destination;
	protected IProgressMonitor monitor;
	protected IStatus status = Status.OK_STATUS;

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
	public void initialize(ProcessingStepDescriptor descriptor, IArtifactDescriptor context) {
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

	/**
	 * Return the status of this step.  The status will be <code>null</code> if the
	 * step has not yet executed. If the step has executed the returned status
	 * indicates the success or failure of the step.
	 * @param deep whether or not to aggregate the status of any linked steps
	 * @return the requested status 
	 */
	public IStatus getStatus(boolean deep) {
		if (!deep)
			return status;
		ArrayList list = new ArrayList();
		int severity = collectStatus(list);
		if (severity == IStatus.OK)
			return Status.OK_STATUS;
		IStatus[] result = (IStatus[]) list.toArray(new IStatus[list.size()]);
		return new MultiStatus(Activator.ID, severity, result, "Result of processing steps", null);
	}

	private int collectStatus(ArrayList list) {
		list.add(status);
		if (!(destination instanceof ProcessingStep))
			return status.getSeverity();
		int result = ((ProcessingStep) destination).collectStatus(list);
		// TODO greater than test here is a little brittle but it is very unlikely that we will add
		// a new status severity.
		if (status.getSeverity() > result)
			return status.getSeverity();
		return result;

	}
}
