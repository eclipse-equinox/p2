/*******************************************************************************
* Copyright (c) 2007 IBM Corporation and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* 	IBM Corporation - initial API and implementation
*******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.processor.jardelta;

import java.io.IOException;
import org.eclipse.equinox.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStepDescriptor;

/**
 * Processor that taks a JAR delta and applies it.
 */
public class JarDeltaStep extends ProcessingStep {
	public static final String ID = "org.eclipse.equinox.p2.artifact.processor.jardelta"; //$NON-NLS-1$

	public void close() throws IOException {
	}

	public void initialize(ProcessingStepDescriptor descriptor, IArtifactDescriptor context) {
		super.initialize(descriptor, context);
	}

	public void write(int b) throws IOException {
		//		OutputStream stream = getOutputStream();
		//		stream.write(b);
	}

}