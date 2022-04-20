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
import org.eclipse.equinox.internal.p2.artifact.processing.AbstractBufferingStep;
import org.eclipse.equinox.internal.p2.jarprocessor.UnpackStep;
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

	private File incoming;

	@Override
	protected OutputStream createIncomingStream() throws IOException {
		incoming = File.createTempFile(INCOMING_ROOT, JAR_SUFFIX + PACKED_SUFFIX);
		return new BufferedOutputStream(new FileOutputStream(incoming));
	}

	@Override
	protected void performProcessing() {
		// NO-OP see https://github.com/eclipse-equinox/p2/issues/40
	}

	protected File process() throws IOException {
		Options options = new Options();
		options.unpack = false;
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
