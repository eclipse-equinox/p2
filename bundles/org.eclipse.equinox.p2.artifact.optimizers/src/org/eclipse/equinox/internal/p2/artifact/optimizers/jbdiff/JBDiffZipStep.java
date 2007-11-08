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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.artifact.repository.ArtifactDescriptor;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.sar.DirectByteArrayOutputStream;
import org.eclipse.equinox.p2.sar.SarUtil;

public class JBDiffZipStep extends JBDiffStep {

	public JBDiffZipStep(IArtifactRepository repository) {
		super(repository);
	}

	protected void performDiff() throws IOException {
		if (current == null)
			// hmmm, no one wrote to this stream so there is nothing to pass on
			return;
		// Ok, so there is content, close stream
		current.close();

		try {
			DirectByteArrayOutputStream sarredCurrent = new DirectByteArrayOutputStream();
			SarUtil.zipToSar(current.getInputStream(), sarredCurrent);
			current = null;
			fetchPredecessor(new ArtifactDescriptor(key));
			byte[] diff = JBDiff.bsdiff(predecessor.getBuffer(), predecessor.getBufferLength(), sarredCurrent.getBuffer(), sarredCurrent.getBufferLength());
			predecessor = null;
			sarredCurrent = null;
			FileUtils.copyStream(new ByteArrayInputStream(diff), true, destination, false);
		} finally {
			predecessor = null;
			current = null;
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
