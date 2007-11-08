/*******************************************************************************
 * Copyright (c) 2007 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.processors.jbdiff;

import ie.wombat.jbdiff.JBPatch;
import java.io.*;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.artifact.repository.ArtifactDescriptor;
import org.eclipse.equinox.p2.sar.DirectByteArrayOutputStream;

/**
 * The JBPatchStep patches a JBDiff based data.   
 */
public class JBPatchStep extends AbstractDeltaPatchStep {

	protected DirectByteArrayOutputStream diff;
	protected DirectByteArrayOutputStream predecessor;

	public void write(int b) throws IOException {
		OutputStream stream = getOutputStream();
		stream.write(b);
	}

	private OutputStream getOutputStream() {
		if (diff != null)
			return diff;
		diff = new DirectByteArrayOutputStream();
		return diff;
	}

	public void close() throws IOException {
		// When we go to close we must have seen all the content we are going to see
		// So before closing, run patch and write the patched result to the destination
		performPatch();
		super.close();
		status = Status.OK_STATUS;
	}

	protected void performPatch() throws IOException {
		if (diff == null)
			// hmmm, no one wrote to this stream so there is nothing to pass on
			return;
		// Ok, so there is content, close the diff
		diff.close();

		try {
			fetchPredecessor(new ArtifactDescriptor(key));
			byte[] result = JBPatch.bspatch(predecessor.getBuffer(), predecessor.getBufferLength(), diff.getBuffer(), diff.getBufferLength());
			diff = null;
			predecessor = null;
			FileUtils.copyStream(new ByteArrayInputStream(result), true, destination, false);
		} finally {
			diff = null;
			predecessor = null;
		}
	}

	private void fetchPredecessor(ArtifactDescriptor artifactDescriptor) throws IOException {
		predecessor = new DirectByteArrayOutputStream();
		status = repository.getArtifact(artifactDescriptor, predecessor, monitor);
		if (!status.isOK())
			throw (IOException) new IOException(status.getMessage()).initCause(status.getException());
	}

}