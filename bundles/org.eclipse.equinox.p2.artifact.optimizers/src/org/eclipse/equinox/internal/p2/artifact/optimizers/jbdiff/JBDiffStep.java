/*******************************************************************************
* Copyright (c) 2007 compeople AG and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*  compeople AG (Stefan Liebig) - initial API and implementation
*******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.optimizers.jbdiff;

import ie.wombat.jbdiff.JBDiff;
import java.io.*;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.artifact.repository.ArtifactDescriptor;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.sar.DirectByteArrayOutputStream;

/**
 *
 */
public class JBDiffStep extends AbstractDeltaDiffStep {

	protected DirectByteArrayOutputStream current;
	protected DirectByteArrayOutputStream predecessor;

	public JBDiffStep(IArtifactRepository repository) {
		super(repository);
	}

	public void write(int b) throws IOException {
		OutputStream stream = getOutputStream();
		stream.write(b);
	}

	private OutputStream getOutputStream() {
		if (current != null)
			return current;

		current = new DirectByteArrayOutputStream();
		return current;
	}

	protected void performDiff() throws IOException {
		if (current == null)
			// hmmm, no one wrote to this stream so there is nothing to pass on
			return;
		// Ok, so there is content, close stream
		current.close();

		try {
			fetchPredecessor(new ArtifactDescriptor(key));
			byte[] diff = JBDiff.bsdiff(predecessor.getBuffer(), predecessor.getBufferLength(), current.getBuffer(), current.getBufferLength());
			predecessor = null;
			current = null;
			FileUtils.copyStream(new ByteArrayInputStream(diff), true, destination, false);
		} finally {
			predecessor = null;
			current = null;
		}
	}

	public void close() throws IOException {
		// When we go to close we must have seen all the content we are going to see
		// So before closing, run unpack and write the unpacked result to the destination
		performDiff();
		super.close();
		status = Status.OK_STATUS;
	}

	private void fetchPredecessor(ArtifactDescriptor artifactDescriptor) throws IOException {
		predecessor = new DirectByteArrayOutputStream();
		status = repository.getArtifact(artifactDescriptor, predecessor, monitor);
		if (!status.isOK())
			throw (IOException) new IOException(status.getMessage()).initCause(status.getException());
	}

}
