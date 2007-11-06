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
package org.eclipse.equinox.internal.p2.artifact.optimizers.pack200;

import java.io.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.jarprocessor.JarProcessorExecutor;
import org.eclipse.equinox.p2.jarprocessor.JarProcessorExecutor.Options;

/**
 * The Pack200Packer expects an input containing normal ".jar" data.   
 */
public class Pack200Step extends ProcessingStep {
	private static final String PACKED_SUFFIX = ".pack.gz"; //$NON-NLS-1$
	private static final String JAR_SUFFIX = ".jar"; //$NON-NLS-1$

	private File source;
	private OutputStream tempStream;

	public void write(int b) throws IOException {
		OutputStream stream = getOutputStream();
		stream.write(b);
	}

	private OutputStream getOutputStream() throws IOException {
		if (tempStream != null)
			return tempStream;
		// store input stream in temporary file
		source = File.createTempFile("p2.pack200", JAR_SUFFIX);
		tempStream = new BufferedOutputStream(new FileOutputStream(source));
		return tempStream;
	}

	private void performPack() throws IOException {
		BufferedInputStream resultStream = null;
		File resultFile = null;
		File workDir = null;
		try {
			if (tempStream == null)
				// hmmm, no one wrote to this stream so there is nothing to pass on
				return;
			// Ok, so there is content, close the tempStream
			tempStream.close();
			// now create a temporary directory for the JarProcessor to work in
			// TODO How to create a unique, temporary directory atomically?
			workDir = File.createTempFile("p2.pack200.", "");
			if (!workDir.delete())
				throw new IOException("Could not delete file for creating temporary working dir.");
			if (!workDir.mkdirs())
				throw new IOException("Could not create temporary working dir.");

			// unpack
			Options options = new Options();
			options.pack = true;
			// TODO use false here assuming that all content is conditioned.  Need to revise this
			options.processAll = false;
			options.input = source;
			options.outputDir = workDir.getPath();
			options.verbose = true;
			new JarProcessorExecutor().runJarProcessor(options);

			// now write the packed content to our destination
			resultFile = new File(workDir, source.getName() + PACKED_SUFFIX);
			if (resultFile.length() > 0) {
				resultStream = new BufferedInputStream(new FileInputStream(resultFile));
				FileUtils.copyStream(resultStream, true, destination, false);
			} else {
				status = new Status(IStatus.ERROR, Activator.ID, "Empty file packed: " + resultFile);
			}
		} finally {
			if (source != null)
				source.delete();
			if (resultFile != null)
				resultFile.delete();
			if (workDir != null) {
				FileUtils.deleteAll(workDir);
				// TODO try twice since there seems to be some cases where the dir is not 
				// deleted the first time.  At least on Windows...
				FileUtils.deleteAll(workDir);
			}
		}
	}

	public void close() throws IOException {
		// When we go to close we must have seen all the content we are going to see
		// So before closing, run unpack and write the unpacked result to the destination
		performPack();
		super.close();
		// TODO need to get real status here but curently the JAR processor does not give
		// any reasonable return status
		if (status == null)
			status = Status.OK_STATUS;
	}

}