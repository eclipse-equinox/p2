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
package org.eclipse.equinox.p2.tests.artifact.repository.processing;

import java.io.IOException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IProcessingStepDescriptor;

public class ByteShifter extends ProcessingStep {

	protected int operand;

	public ByteShifter() {
		super();
	}

	public ByteShifter(int shiftLeft) {
		super();
		this.operand = shiftLeft;
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
		if (operand <= 0)
			setStatus(new Status(code, Activator.ID, "ByteShifter operand invalid: " + operand));
	}

	public void initialize(IProvisioningAgent agent, IProcessingStepDescriptor descriptor, IArtifactDescriptor context) {
		super.initialize(agent, descriptor, context);
		try {
			operand = Integer.valueOf(descriptor.getData()).intValue();
		} catch (NumberFormatException e) {
			int code = descriptor.isRequired() ? IStatus.ERROR : IStatus.INFO;
			setStatus(new Status(code, Activator.ID, "ByteShifter operand specification invalid", e));
			return;
		}
		basicInitialize(descriptor);
	}

	public void write(int b) throws IOException {
		getDestination().write(b == -1 ? b : b << operand);
	}

	public IStatus getStatus() {
		return Status.OK_STATUS;
	}
}
