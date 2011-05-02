/*******************************************************************************
 * Copyright (c) 2007, 2010 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 * 	IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.optimizers.jbdiff;

import ie.wombat.jbdiff.JBDiff;
import java.io.*;
import org.eclipse.equinox.internal.p2.artifact.optimizers.AbstractDeltaStep;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.sar.DirectByteArrayOutputStream;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;

/**
 *
 */
public class JBDiffStep extends AbstractDeltaStep {

	public JBDiffStep() {
		super();
	}

	// TODO We need a different way of injecting the base artifacts.  This approach forces
	// the target and base to live in the same repo.  Typical but not really required.
	protected JBDiffStep(IArtifactRepository repository) {
		super(repository);
	}

	protected OutputStream createIncomingStream() throws IOException {
		return new DirectByteArrayOutputStream();
	}

	protected void performProcessing() throws IOException {
		DirectByteArrayOutputStream predecessor = fetchPredecessorBytes(new ArtifactDescriptor(key));
		DirectByteArrayOutputStream current = (DirectByteArrayOutputStream) incomingStream;
		byte[] diff = JBDiff.bsdiff(predecessor.getBuffer(), predecessor.getBufferLength(), current.getBuffer(), current.getBufferLength());
		// free up the memory as soon as possible.
		predecessor = null;
		current = null;
		incomingStream = null;

		// copy the result of the optimization to the destination.
		FileUtils.copyStream(new ByteArrayInputStream(diff), true, getDestination(), false);
	}

	private DirectByteArrayOutputStream fetchPredecessorBytes(ArtifactDescriptor artifactDescriptor) throws IOException {
		DirectByteArrayOutputStream result = new DirectByteArrayOutputStream();
		setStatus(repository.getArtifact(artifactDescriptor, result, getProgressMonitor()));
		if (!getStatus().isOK())
			throw (IOException) new IOException(getStatus().getMessage()).initCause(getStatus().getException());
		return result;
	}
}
