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
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.osgi.internal.provisional.verifier.CertificateVerifier;
import org.eclipse.osgi.internal.provisional.verifier.CertificateVerifierFactory;

/**
 * The Pack200Unpacker expects an input containing ".jar.pack.gz" data.   
 */
public class SignatureVerifier extends ProcessingStep {
	private File inputFile;
	private OutputStream tempStream;

	public boolean areRequirementsSatisfied() {
		return true;
	}

	public void write(int b) throws IOException {
		getOutputStream().write(b);
	}

	private OutputStream getOutputStream() throws IOException {
		if (tempStream != null)
			return tempStream;
		// store input stream in temporary file
		inputFile = File.createTempFile("signatureFile", ".jar");
		tempStream = new BufferedOutputStream(new FileOutputStream(inputFile));
		return tempStream;
	}

	private void verify() throws IOException {
		BufferedInputStream resultStream = null;
		try {
			if (tempStream == null)
				// hmmm, no one wrote to this stream so there is nothing to pass on
				return;
			// Ok, so there is content, close the tempStream
			tempStream.close();

			CertificateVerifierFactory verifierFactory = (CertificateVerifierFactory) ServiceHelper.getService(Activator.getContext(), CertificateVerifierFactory.class.getName());
			CertificateVerifier verifier = verifierFactory.getVerifier(inputFile);
			if (verifier.verifyContent().length > 0)
				status = new Status(IStatus.ERROR, "plugin id", "signature verification failure");
			else
				status = Status.OK_STATUS;
			// now write the  content to the final destination
			resultStream = new BufferedInputStream(new FileInputStream(inputFile));
			FileUtils.copyStream(resultStream, true, destination, false);
			resultStream = null;
		} finally {
			if (inputFile != null)
				inputFile.delete();
			if (resultStream != null)
				resultStream.close();
		}
	}

	public void close() throws IOException {
		// When we go to close we must have seen all the content we are going to see
		// So before closing, verify and write the result to the destination
		verify();
		super.close();
	}

}