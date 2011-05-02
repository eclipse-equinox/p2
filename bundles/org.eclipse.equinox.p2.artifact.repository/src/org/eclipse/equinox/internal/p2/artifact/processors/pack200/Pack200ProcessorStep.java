/*******************************************************************************
 * Copyright (c) 2007, 2010 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *  IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.processors.pack200;

import java.io.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.processing.AbstractBufferingStep;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.jarprocessor.UnpackStep;
import org.eclipse.equinox.internal.p2.jarprocessor.Utils;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IProcessingStepDescriptor;
import org.eclipse.internal.provisional.equinox.p2.jarprocessor.JarProcessorExecutor;
import org.eclipse.internal.provisional.equinox.p2.jarprocessor.JarProcessorExecutor.Options;

/**
 * The Pack200Unpacker expects an input containing ".jar.pack.gz" data.   
 */
public class Pack200ProcessorStep extends AbstractBufferingStep {
	public static final String PACKED_SUFFIX = ".pack.gz"; //$NON-NLS-1$
	private static boolean detailedResult = false;

	private File incoming;

	protected OutputStream createIncomingStream() throws IOException {
		incoming = File.createTempFile(INCOMING_ROOT, JAR_SUFFIX + PACKED_SUFFIX);
		return new BufferedOutputStream(new FileOutputStream(incoming));
	}

	public void initialize(IProvisioningAgent agent, IProcessingStepDescriptor descriptor, IArtifactDescriptor context) {
		super.initialize(agent, descriptor, context);
		if (!UnpackStep.canUnpack()) {
			IStatus status = null;
			if (detailedResult) {
				status = new Status(IStatus.ERROR, Activator.ID, "Unpack facility not configured."); //$NON-NLS-1$
				detailedResult = true;
			} else {
				String[] locations = Utils.getPack200Commands("unpack200"); //$NON-NLS-1$
				StringBuffer locationTried = new StringBuffer(100);
				for (int i = 0; i < locations.length; i++) {
					locationTried.append(locations[i]).append(", "); //$NON-NLS-1$
				}
				status = new Status(IStatus.ERROR, Activator.ID, "Unpack facility not configured. The locations searched for unpack200 are: " + locationTried); //$NON-NLS-1$
			}
			setStatus(status);
		}
	}

	protected void cleanupTempFiles() {
		super.cleanupTempFiles();
		if (incoming != null)
			incoming.delete();
	}

	protected void performProcessing() throws IOException {
		File resultFile = null;
		try {
			resultFile = process();
			// now write the processed content to the destination
			if (resultFile.length() > 0) {
				InputStream resultStream = new BufferedInputStream(new FileInputStream(resultFile));
				FileUtils.copyStream(resultStream, true, getDestination(), false);
			} else {
				setStatus(new Status(IStatus.ERROR, Activator.ID, "Unpacking fails because intermediate file is empty: " + resultFile)); //$NON-NLS-1$
			}
		} finally {
			if (resultFile != null)
				resultFile.delete();
		}
	}

	protected File process() throws IOException {
		Options options = new Options();
		options.unpack = true;
		// TODO use false here assuming that all content is conditioned.  Need to revise this
		options.processAll = false;
		options.input = incoming;
		options.outputDir = getWorkDir().getPath();
		options.verbose = false;
		new JarProcessorExecutor().runJarProcessor(options);
		return new File(getWorkDir(), incoming.getName().substring(0, incoming.getName().length() - PACKED_SUFFIX.length()));
	}
}
