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
package org.eclipse.equinox.prov.tests.artifact.repository.processing;

import java.io.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.jarprocessor.UnpackStep;
import org.eclipse.equinox.internal.p2.jarprocessor.Utils;
import org.eclipse.equinox.internal.prov.artifact.repository.Activator;
import org.eclipse.equinox.p2.jarprocessor.JarProcessorExecutor;
import org.eclipse.equinox.p2.jarprocessor.JarProcessorExecutor.Options;
import org.eclipse.equinox.prov.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.prov.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.prov.artifact.repository.processing.ProcessingStepDescriptor;
import org.eclipse.equinox.prov.core.helpers.FileUtils;

/**
 * The Pack200Unpacker expects an input containing ".jar.pack.gz" data.   
 */
public class Pack200Unpacker extends ProcessingStep {
	private final static String PACKED_EXT = Utils.JAR_SUFFIX + Utils.PACKED_SUFFIX;

	private File packed;
	private OutputStream tempStream;

	public void initialize(ProcessingStepDescriptor descriptor, IArtifactDescriptor context) {
		super.initialize(descriptor, context);
		if (!UnpackStep.canUnpack())
			status = new Status(IStatus.ERROR, Activator.ID, "Unpack facility not configured");
	}

	public void write(int b) throws IOException {
		OutputStream stream = getOutputStream();
		stream.write(b);
	}

	private OutputStream getOutputStream() throws IOException {
		if (tempStream != null)
			return tempStream;
		// store input stream in temporary file
		packed = File.createTempFile("pack200", PACKED_EXT);
		tempStream = new BufferedOutputStream(new FileOutputStream(packed));
		return tempStream;
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
			workDir = File.createTempFile("work", "");
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
			unpacked = new File(workDir, packedFileName.substring(0, packedFileName.length() - Utils.PACKED_SUFFIX.length()));
			unpackedStream = new BufferedInputStream(new FileInputStream(unpacked));
			FileUtils.copyStream(unpackedStream, true, destination, false);
			unpackedStream = null;
		} finally {
			if (packed != null)
				packed.delete();
			if (unpackedStream != null)
				unpackedStream.close();
			if (unpacked != null)
				unpacked.delete();
			if (workDir != null)
				workDir.delete();
		}
	}

	public void close() throws IOException {
		// When we go to close we must have seen all the content we are going to see
		// So before closing, run unpack and write the unpacked result to the destination
		performUnpack();
		super.close();
		status = Status.OK_STATUS;
	}
}