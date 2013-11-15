/*******************************************************************************
 * Copyright (c) 2007, 2008 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 * 	IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.processing;

import java.io.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.osgi.util.NLS;

public abstract class AbstractBufferingStep extends ProcessingStep {
	private static final String WORK_DIR_PREFIX = "work"; //$NON-NLS-1$
	private static final String WORK_DIR_SUFFIX = ""; //$NON-NLS-1$
	protected static final String JAR_SUFFIX = ".jar"; //$NON-NLS-1$
	protected static final String INCOMING_ROOT = "p2.optimizers.incoming"; //$NON-NLS-1$
	protected static final String RESULT_ROOT = "p2.optimizers.result"; //$NON-NLS-1$
	protected static final String PREDECESSOR_ROOT = "p2.optimizers.predecessor"; //$NON-NLS-1$

	protected OutputStream incomingStream;
	private File workDir;

	protected AbstractBufferingStep() {
		super();
	}

	public void write(int b) throws IOException {
		OutputStream stream = getOutputStream();
		stream.write(b);
	}

	protected OutputStream getOutputStream() throws IOException {
		if (incomingStream != null)
			return incomingStream;
		// if buffering, store input stream in temporary file
		incomingStream = createIncomingStream();
		return incomingStream;
	}

	protected abstract OutputStream createIncomingStream() throws IOException;

	public void close() throws IOException {
		// When we go to close we must have seen all the content we are going to see.
		// If no one wrote to the temp stream then we return an error. If there is 
		// content then close the temporary stream and perform the processing.
		// Performing the step should result in the new content being written to 
		// the destination.  Make sure we delete the temporary file if any.
		try {
			if (incomingStream != null) {
				incomingStream.close();
				// if canceled then skip processing
				if (getStatus() != null && getStatus().getSeverity() != IStatus.CANCEL)
					performProcessing();
			} else {
				setStatus(new Status(IStatus.ERROR, Activator.ID, Messages.Empty_stream));
			}
		} finally {
			incomingStream = null;
			cleanupTempFiles();
			cleanupWorkDir();
		}

		super.close();
		// TODO need to get real status here.  sometimes the optimizers do not give 
		// any reasonable return status
		// COMMENT status is initially set to OK!
	}

	protected abstract void performProcessing() throws IOException;

	protected void cleanupTempFiles() {
	}

	private void cleanupWorkDir() {
		if (workDir != null) {
			FileUtils.deleteAll(workDir);
			// TODO try twice since there seems to be some cases where the dir is not 
			// deleted the first time.  At least on Windows...
			FileUtils.deleteAll(workDir);
		}
	}

	protected File getWorkDir() throws IOException {
		if (workDir != null)
			return workDir;
		workDir = File.createTempFile(WORK_DIR_PREFIX, WORK_DIR_SUFFIX);
		if (!workDir.delete())
			throw new IOException(NLS.bind(Messages.Can_not_delete_temp_dir, workDir));
		if (!workDir.mkdirs())
			throw new IOException(NLS.bind(Messages.Can_not_create_temp_dir, workDir));
		return workDir;
	}

}
