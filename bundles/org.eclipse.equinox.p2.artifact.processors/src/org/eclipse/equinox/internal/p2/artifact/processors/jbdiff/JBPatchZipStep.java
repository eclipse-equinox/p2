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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.eclipse.equinox.p2.artifact.repository.ArtifactDescriptor;
import org.eclipse.equinox.p2.sar.DirectByteArrayOutputStream;
import org.eclipse.equinox.p2.sar.SarUtil;

/**
 * The <code>JBPatchZipStep</code> patches a JBDiff based diff of zips/jars.   
 */
public class JBPatchZipStep extends JBPatchStep {

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
			SarUtil.sarToZip(new ByteArrayInputStream(result), true, destination, false);
		} finally {
			diff = null;
			predecessor = null;
		}
	}

	private void fetchPredecessor(ArtifactDescriptor artifactDescriptor) throws IOException {
		DirectByteArrayOutputStream zippedPredecessor = new DirectByteArrayOutputStream();

		status = repository.getArtifact(artifactDescriptor, zippedPredecessor, monitor);
		if (!status.isOK())
			throw (IOException) new IOException(status.getMessage()).initCause(status.getException());

		predecessor = new DirectByteArrayOutputStream();
		SarUtil.zipToSar(zippedPredecessor.getInputStream(), predecessor);
	}

}