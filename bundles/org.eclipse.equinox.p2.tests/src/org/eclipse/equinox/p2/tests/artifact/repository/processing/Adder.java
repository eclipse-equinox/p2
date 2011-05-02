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

public class Adder extends ProcessingStep {

	protected int operand;

	public Adder() {
		// needed
	}

	public Adder(int operand) {
		super();
		this.operand = operand;
	}

	public void initialize(IProvisioningAgent agent, IProcessingStepDescriptor descriptor, IArtifactDescriptor context) {
		super.initialize(agent, descriptor, context);
		try {
			operand = Integer.valueOf(descriptor.getData()).intValue();
		} catch (NumberFormatException e) {
			int code = descriptor.isRequired() ? IStatus.ERROR : IStatus.INFO;
			setStatus(new Status(code, Activator.ID, "Adder operand specification invalid", e));
			return;
		}
	}

	public void write(int b) throws IOException {
		getDestination().write(b + operand);
	}

	public void close() throws IOException {
		super.close();
	}
}
