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
 *  IBM Corporation - ongoing development
*   Mykola Nikishov - continuing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.processors.pack200;

import java.io.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.processing.AbstractBufferingStep;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorRequest;
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
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 * @noreference This class is not intended to be referenced by clients.
 * @deprecated See <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=572043">bug</a> for details.
 */
@Deprecated(forRemoval = true, since = "1.4.100")
public class Pack200ProcessorStep extends AbstractBufferingStep {
	public static final String PACKED_SUFFIX = ".pack.gz"; //$NON-NLS-1$
	private static boolean detailedResult = false;

	private File incoming;

	@Override
	protected OutputStream createIncomingStream() throws IOException {
		incoming = File.createTempFile(INCOMING_ROOT, JAR_SUFFIX + PACKED_SUFFIX);
		return new BufferedOutputStream(new FileOutputStream(incoming));
	}

	@Override
	public void initialize(IProvisioningAgent agent, IProcessingStepDescriptor descriptor, IArtifactDescriptor context) {
		super.initialize(agent, descriptor, context);
		boolean isJava14 = false;
		if (System.getProperty("java.specification.version").compareTo("14") >= 0) { //$NON-NLS-1$ //$NON-NLS-2$
			isJava14 = true;
		}

		if (!isEnabled()) {
			IStatus status = null;
			// Missing pack200 executable is fine on Java 14+
			int statusCode = isJava14 ? IStatus.OK : IStatus.ERROR;
			if (detailedResult) {
				status = new Status(statusCode, Activator.ID, MirrorRequest.ARTIFACT_PROCESSING_ERROR,
						"Unpack facility not configured.", null); //$NON-NLS-1$
				detailedResult = true;
			} else {
				String[] locations = Utils.getPack200Commands("unpack200"); //$NON-NLS-1$
				StringBuilder locationTried = new StringBuilder(100);
				for (String location : locations) {
					locationTried.append(location).append(", "); //$NON-NLS-1$
				}
				status = new Status(statusCode, Activator.ID, MirrorRequest.ARTIFACT_PROCESSING_ERROR,
						"Unpack facility not configured. The locations searched for unpack200 are: " + locationTried, //$NON-NLS-1$
						null);
			}
			setStatus(status);
		}
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
			resultFile = process();
			// now write the processed content to the destination
			if (resultFile.length() > 0) {
				InputStream resultStream = new BufferedInputStream(new FileInputStream(resultFile));
				FileUtils.copyStream(resultStream, true, getDestination(), false);
			} else {
				setStatus(new Status(IStatus.ERROR, Activator.ID, MirrorRequest.ARTIFACT_PROCESSING_ERROR, "Unpacking fails because intermediate file is empty: " + resultFile, null)); //$NON-NLS-1$
			}
		} catch (IOException e) {
			setStatus(new Status(IStatus.ERROR, Activator.ID, MirrorRequest.ARTIFACT_PROCESSING_ERROR, "Unpacking fails", e)); //$NON-NLS-1$
			throw e;
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

	@Override
	public boolean isEnabled() {
		return UnpackStep.canUnpack();
	}
}
