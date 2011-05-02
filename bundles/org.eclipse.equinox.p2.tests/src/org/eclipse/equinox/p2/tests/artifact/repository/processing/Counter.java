/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *	IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository.processing;

import java.io.IOException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IProcessingStepDescriptor;

public class Counter extends ProcessingStep {

	protected long size = -1;
	long total = 0;

	public Counter() {
		// needed
	}

	public Counter(long size) {
		super();
		this.size = size;
		basicInitialize(null);
	}

	private void basicInitialize(IProcessingStepDescriptor descriptor) {
		// if the status is already set to something that not ok, we've already found a problem.
		if (!getStatus().isOK())
			return;

		int code;
		// if there is a descriptor, decide if the "bad case" is an error or info.  If no 
		// descriptor then default to error.
		if (descriptor != null)
			code = descriptor.isRequired() ? IStatus.ERROR : IStatus.INFO;
		else
			code = IStatus.ERROR;

		// finally, check the actual setup and set the status.
		if (size != -1)
			setStatus(new Status(code, Activator.ID, "Counter size not set"));
	}

	public void initialize(IProvisioningAgent agent, IProcessingStepDescriptor descriptor, IArtifactDescriptor context) {
		super.initialize(agent, descriptor, context);
		String data = descriptor.getData();
		if (data == null)
			return;
		try {
			if (data.equals("download"))
				size = Long.parseLong(context.getProperty(IArtifactDescriptor.DOWNLOAD_SIZE));
			else if (data.equals("artifact"))
				size = Long.parseLong(context.getProperty(IArtifactDescriptor.ARTIFACT_SIZE));
			else
				size = Long.parseLong(data);
		} catch (NumberFormatException e) {
			int code = descriptor.isRequired() ? IStatus.ERROR : IStatus.INFO;
			setStatus(new Status(code, Activator.ID, "Counter size specification invalid", e));
			return;
		}
		basicInitialize(descriptor);
	}

	public void write(int b) throws IOException {
		total++;
		getDestination().write(b);
	}

	public void close() throws IOException {
		super.close();
		if (total != size)
			setStatus(new Status(IStatus.WARNING, "plugin id", "Size mismatch.  Was " + total + " should have been " + size));
	}
}
