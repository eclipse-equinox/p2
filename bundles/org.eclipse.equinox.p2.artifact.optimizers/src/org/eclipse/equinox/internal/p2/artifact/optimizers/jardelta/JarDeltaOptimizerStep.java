/*******************************************************************************
* Copyright (c) 2007 compeople AG and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* 	compeople AG (Stefan Liebig) - initial API and implementation
*  IBM - continuing development
*******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.optimizers.jardelta;

import java.io.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.optimizers.Activator;
import org.eclipse.equinox.internal.p2.artifact.optimizers.jbdiff.AbstractDeltaDiffStep;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.artifact.repository.*;

/**
 * The Pack200Packer expects an input containing normal ".jar" data.   
 */
public class JarDeltaOptimizerStep extends AbstractDeltaDiffStep {
	private static final String JAR_SUFFIX = ".jar"; //$NON-NLS-1$

	private File incoming;
	private OutputStream tempStream;

	protected JarDeltaOptimizerStep(IArtifactRepository repository) {
		super(repository);
	}

	public void write(int b) throws IOException {
		OutputStream stream = getOutputStream();
		stream.write(b);
	}

	private OutputStream getOutputStream() throws IOException {
		if (tempStream != null)
			return tempStream;
		// store input stream in temporary file
		incoming = File.createTempFile("p2.jardelta.optimizer.incoming", JAR_SUFFIX);
		tempStream = new BufferedOutputStream(new FileOutputStream(incoming));
		return tempStream;
	}

	private void performOptimization() throws IOException {
		File predecessor = null;
		File resultFile = null;
		try {
			// get the predecessor and perform the optimization into a temp file
			predecessor = fetchPredecessor(new ArtifactDescriptor(key));
			resultFile = File.createTempFile("p2.jardelta.optimizer.result", JAR_SUFFIX);
			new DeltaComputer(predecessor, incoming, resultFile).run();

			// now write the optimized content to the destination
			if (resultFile.length() > 0) {
				InputStream resultStream = new BufferedInputStream(new FileInputStream(resultFile));
				FileUtils.copyStream(resultStream, true, destination, false);
			} else {
				status = new Status(IStatus.ERROR, Activator.ID, "Empty optimized file: " + resultFile);
			}
		} finally {
			// if we have a predecessor and it is our temp file then clean up the file
			if (predecessor != null && predecessor.getAbsolutePath().indexOf("p2.jardelta.predecessor") > -1)
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
			performOptimization();
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

}