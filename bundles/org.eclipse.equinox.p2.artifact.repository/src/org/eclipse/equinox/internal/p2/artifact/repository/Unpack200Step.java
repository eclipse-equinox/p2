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
package org.eclipse.equinox.internal.p2.artifact.repository;

import java.io.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.jarprocessor.UnpackStep;
import org.eclipse.equinox.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStepDescriptor;
import org.eclipse.equinox.p2.jarprocessor.JarProcessorExecutor;
import org.eclipse.equinox.p2.jarprocessor.JarProcessorExecutor.Options;

/**
 * The Pack200Unpacker expects an input containing ".jar.pack.gz" data.   
 */
public class Unpack200Step extends ProcessingStep {
	public static final String JAR_SUFFIX = ".jar"; //$NON-NLS-1$
	public static final String PACKED_SUFFIX = ".pack.gz"; //$NON-NLS-1$
	private final static String PACKED_EXT = JAR_SUFFIX + PACKED_SUFFIX;

	private File packed;
	private OutputStream tempStream;

	public void close() throws IOException {
		// When we go to close we must have seen all the content we are going to see
		// So before closing, run unpack and write the unpacked result to the destination
		performUnpack();
		super.close();
		if (status == null)
			status = Status.OK_STATUS;
	}

	private OutputStream getOutputStream() throws IOException {
		if (tempStream != null)
			return tempStream;
		// store input stream in temporary file
		packed = File.createTempFile("p2.pack200", PACKED_EXT);
		tempStream = new BufferedOutputStream(new FileOutputStream(packed));
		return tempStream;
	}

	public void initialize(ProcessingStepDescriptor descriptor, IArtifactDescriptor context) {
		super.initialize(descriptor, context);
		if (!UnpackStep.canUnpack())
			status = new Status(IStatus.ERROR, Activator.ID, "Unpack facility not configured");
	}

	private void performUnpack() throws IOException {
		BufferedInputStream unpackedStream = null;
		File unpacked = null;
		File workDir = null;
		try {
			if (tempStream == null)
				// hmmm, no one wrote to this stream so there is nothing to pass on
				return;
			// Ok, so there is content, close the tempStream
			tempStream.close();
			// now create a temporary directory for the JarProcessor to work in
			// TODO How to create a unique, temporary directory atomically?
			workDir = File.createTempFile("p2.unpack.", "");
			if (!workDir.delete())
				throw new IOException("Could not delete file for creating temporary working dir.");
			if (!workDir.mkdirs())
				throw new IOException("Could not create temporary working dir.");

			// unpack
			Options options = new Options();
			options.unpack = true;
			options.processAll = true;
			options.input = packed;
			options.outputDir = workDir.getPath();
			new JarProcessorExecutor().runJarProcessor(options);

			// now write the unpacked content to our destination
			String packedFileName = packed.getName();
			unpacked = new File(workDir, packedFileName.substring(0, packedFileName.length() - PACKED_SUFFIX.length()));
			if (unpacked.length() == 0)
				System.out.println("Empty file unpacked:  " + unpacked);
			unpackedStream = new BufferedInputStream(new FileInputStream(unpacked));
			FileUtils.copyStream(unpackedStream, true, destination, false);
		} finally {
			// note that unpackedStream will be closed by copyStream()
			if (packed != null)
				packed.delete();
			if (unpacked != null)
				unpacked.delete();
			if (workDir != null)
				FileUtils.deleteAll(workDir);
		}
	}

	public void write(int b) throws IOException {
		OutputStream stream = getOutputStream();
		stream.write(b);
	}

}