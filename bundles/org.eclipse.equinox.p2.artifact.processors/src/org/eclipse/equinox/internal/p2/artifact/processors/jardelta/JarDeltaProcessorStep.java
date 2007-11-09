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
package org.eclipse.equinox.internal.p2.artifact.processors.jardelta;

import java.io.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.processors.Activator;
import org.eclipse.equinox.internal.p2.artifact.processors.jbdiff.AbstractDeltaPatchStep;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.artifact.repository.ArtifactDescriptor;
import org.eclipse.equinox.p2.artifact.repository.IFileArtifactRepository;

/**
 * Processor that takes a JAR delta and applies it.
 */
public class JarDeltaProcessorStep extends AbstractDeltaPatchStep {
	private static final String JAR_SUFFIX = ".jar"; //$NON-NLS-1$

	private File incoming;
	private OutputStream tempStream;

	public void write(int b) throws IOException {
		OutputStream stream = getOutputStream();
		stream.write(b);
	}

	private OutputStream getOutputStream() throws IOException {
		if (tempStream != null)
			return tempStream;
		// store input stream in temporary file
		incoming = File.createTempFile("p2.jardelta.processor.incoming", JAR_SUFFIX);
		tempStream = new BufferedOutputStream(new FileOutputStream(incoming));
		return tempStream;
	}

	public void close() throws IOException {
		// When we go to close we must have seen all the content we are going to see.
		// If no one wrote to the temp stream then there is nothing to do.  Be sure to delete the 
		// the temporary file if any.
		if (tempStream == null) {
			if (incoming != null)
				incoming.delete();
			return;
		}

		// So there is content.  Close the temporary stream and perform the optimization.
		// Performing the optimization should result in the new content being written to 
		// the destination.  Make sure we delete the temporary file if any.
		try {
			tempStream.close();
			performPatch();
		} finally {
			if (incoming != null)
				incoming.delete();
		}

		super.close();
		// TODO need to get real status here.  sometimes the optimizers do not give 
		// any reasonable return status
		if (status == null)
			status = Status.OK_STATUS;
	}

	private void performPatch() throws IOException {
		File predecessor = null;
		File resultFile = null;
		try {
			// get the predecessor and perform the optimization into a temp file
			predecessor = fetchPredecessor(new ArtifactDescriptor(key));
			resultFile = File.createTempFile("p2.jardelta.processor.result", JAR_SUFFIX);
			new DeltaApplier(predecessor, incoming, resultFile).run();

			// now write the optimized content to the destination
			if (resultFile.length() > 0) {
				InputStream resultStream = new BufferedInputStream(new FileInputStream(resultFile));
				FileUtils.copyStream(resultStream, true, destination, false);
			} else {
				status = new Status(IStatus.ERROR, Activator.ID, "Empty optimized file: " + resultFile);
			}
		} finally {
			if (predecessor != null)
				predecessor.delete();
			if (resultFile != null)
				resultFile.delete();
		}
	}

	private File fetchPredecessor(ArtifactDescriptor descriptor) {
		if (repository instanceof IFileArtifactRepository)
			return ((IFileArtifactRepository) repository).getArtifactFile(descriptor);
		File result = null;
		OutputStream resultStream = null;
		try {
			try {
				result = File.createTempFile("p2.jardelta.predecessor", JAR_SUFFIX);
				resultStream = new BufferedOutputStream(new FileOutputStream(result));
				status = repository.getArtifact(descriptor, resultStream, monitor);
				return result;
			} finally {
				if (resultStream != null)
					resultStream.close();
			}
		} catch (IOException e) {
		}
		return null;
	}
}