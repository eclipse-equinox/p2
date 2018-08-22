/*******************************************************************************
 * Copyright (c) 2007, 2018 compeople AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 * 	IBM Corporation - ongoing development
*******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.optimizers.jardelta;

import java.io.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.optimizers.AbstractDeltaStep;
import org.eclipse.equinox.internal.p2.artifact.optimizers.Activator;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;

/**
 * The JAR delta expects an input containing normal ".jar" data.
 */
public class JarDeltaOptimizerStep extends AbstractDeltaStep {

	private File incoming;

	protected JarDeltaOptimizerStep(IArtifactRepository repository) {
		super(repository);
	}

	@Override
	protected OutputStream createIncomingStream() throws IOException {
		incoming = File.createTempFile(INCOMING_ROOT, JAR_SUFFIX);
		return new BufferedOutputStream(new FileOutputStream(incoming));
	}

	@Override
	protected void cleanupTempFiles() {
		super.cleanupTempFiles();
		if (incoming != null)
			incoming.delete();
	}

	@Override
	protected void performProcessing() throws IOException {
		File resultFile = null;
		try {
			resultFile = optimize();
			// now write the optimized content to the destination
			if (resultFile.length() > 0) {
				InputStream resultStream = new BufferedInputStream(new FileInputStream(resultFile));
				FileUtils.copyStream(resultStream, true, getDestination(), false);
			} else {
				setStatus(new Status(IStatus.ERROR, Activator.ID, "Empty optimized file: " + resultFile)); //$NON-NLS-1$
			}
		} finally {
			if (resultFile != null)
				resultFile.delete();
		}
	}

	protected File optimize() throws IOException {
		File predecessor = null;
		try {
			File resultFile = File.createTempFile(RESULT_ROOT, JAR_SUFFIX);
			// get the predecessor and perform the optimization into a temp file
			predecessor = fetchPredecessor(new ArtifactDescriptor(key));
			new DeltaComputer(predecessor, incoming, resultFile).run();
			return resultFile;
		} finally {
			// if we have a predecessor and it is our temp file then clean up the file
			if (predecessor != null && predecessor.getAbsolutePath().indexOf(PREDECESSOR_ROOT) > -1)
				predecessor.delete();
		}
	}
}