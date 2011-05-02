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
package org.eclipse.equinox.internal.p2.artifact.processors.jbdiff;

import ie.wombat.jbdiff.JBPatch;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.eclipse.equinox.internal.p2.sar.DirectByteArrayOutputStream;
import org.eclipse.equinox.internal.p2.sar.SarUtil;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;

/**
 * The <code>JBPatchZipStep</code> patches a JBDiff based diff of zips/jars.   
 */
public class JBPatchZipStep extends JBPatchStep {

	public JBPatchZipStep() {
		super();
	}

	protected void performProcessing() throws IOException {
		DirectByteArrayOutputStream predecessor = fetchPredecessorBytes(new ArtifactDescriptor(key));
		DirectByteArrayOutputStream current = (DirectByteArrayOutputStream) incomingStream;
		byte[] result = JBPatch.bspatch(predecessor.getBuffer(), predecessor.getBufferLength(), current.getBuffer(), current.getBufferLength());
		// free up the memory as soon as possible.
		predecessor = null;
		current = null;
		incomingStream = null;

		// copy the result of the optimization to the destination.
		SarUtil.sarToZip(new ByteArrayInputStream(result), true, getDestination(), false);
	}

	private DirectByteArrayOutputStream fetchPredecessorBytes(ArtifactDescriptor artifactDescriptor) throws IOException {
		DirectByteArrayOutputStream zippedPredecessor = new DirectByteArrayOutputStream();
		setStatus(repository.getArtifact(artifactDescriptor, zippedPredecessor, getProgressMonitor()));
		if (!getStatus().isOK())
			throw (IOException) new IOException(getStatus().getMessage()).initCause(getStatus().getException());

		DirectByteArrayOutputStream result = new DirectByteArrayOutputStream();
		SarUtil.zipToSar(zippedPredecessor.getInputStream(), result);
		return result;
	}

}