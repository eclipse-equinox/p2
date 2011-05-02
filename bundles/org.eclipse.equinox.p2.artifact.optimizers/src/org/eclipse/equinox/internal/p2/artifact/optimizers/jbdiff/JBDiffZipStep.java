/*******************************************************************************
 * Copyright (c) 2007, 2010 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  	compeople AG (Stefan Liebig) - initial API and implementation
 * 	IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.optimizers.jbdiff;

import ie.wombat.jbdiff.JBDiff;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.sar.DirectByteArrayOutputStream;
import org.eclipse.equinox.internal.p2.sar.SarUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;

public class JBDiffZipStep extends JBDiffStep {

	public JBDiffZipStep() {
		super();
	}

	// TODO We need a different way of injecting the base artifacts.  This approach forces
	// the target and base to live in the same repo.  Typical but not really required.
	protected JBDiffZipStep(IArtifactRepository repository) {
		super(repository);
	}

	protected void performProcessing() throws IOException {
		DirectByteArrayOutputStream sarredCurrent = new DirectByteArrayOutputStream();
		SarUtil.zipToSar(((DirectByteArrayOutputStream) incomingStream).getInputStream(), sarredCurrent);
		incomingStream = null;
		DirectByteArrayOutputStream predecessor = fetchPredecessorBytes(new ArtifactDescriptor(key));
		byte[] diff = JBDiff.bsdiff(predecessor.getBuffer(), predecessor.getBufferLength(), sarredCurrent.getBuffer(), sarredCurrent.getBufferLength());
		// free up the memory as soon as possible.
		predecessor = null;
		incomingStream = null;
		sarredCurrent = null;

		// copy the result of the optimization to the destination.
		FileUtils.copyStream(new ByteArrayInputStream(diff), true, getDestination(), false);
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
