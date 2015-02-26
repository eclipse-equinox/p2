/*******************************************************************************
* Copyright (c) 2007, 2015 compeople AG and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* 	compeople AG (Stefan Liebig) - initial API and implementation
*  IBM - continuing development
*  Red Hat Inc. - Bug 460967
*******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.zip.ZipException;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.osgi.signedcontent.*;

/**
 * Processing step validating the signature of the artifact being downloaded  
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

	public void write(byte[] bytes, int off, int len) throws IOException {
		getOutputStream().write(bytes, off, len);
	}

	private OutputStream getOutputStream() throws IOException {
		if (tempStream != null)
			return tempStream;
		// store input stream in temporary file
		inputFile = File.createTempFile("signatureFile", ".jar"); //$NON-NLS-1$ //$NON-NLS-2$
		tempStream = new BufferedOutputStream(new FileOutputStream(inputFile));
		return tempStream;
	}

	private void verify() throws IOException {
		BufferedInputStream resultStream = null;
		try {
			if (tempStream == null)
				// no one wrote to this stream so there is nothing to pass on
				return;
			// Ok, so there is content, close the tempStream
			tempStream.close();
			setStatus(verifyContent());

			// now write the  content to the final destination
			resultStream = new BufferedInputStream(new FileInputStream(inputFile));
			FileUtils.copyStream(resultStream, true, getDestination(), false);
			resultStream = null;
		} finally {
			if (inputFile != null)
				inputFile.delete();
			if (resultStream != null)
				resultStream.close();
		}
	}

	private IStatus verifyContent() throws IOException {
		SignedContentFactory verifierFactory = ServiceHelper.getService(Activator.getContext(), SignedContentFactory.class);
		SignedContent signedContent;
		try {
			signedContent = verifierFactory.getSignedContent(inputFile);
		} catch (GeneralSecurityException e) {
			return new Status(IStatus.ERROR, Activator.ID, MirrorRequest.ARTIFACT_PROCESSING_ERROR, Messages.SignatureVerification_failedRead + inputFile, e);
		} catch (ZipException e) {
			// SignedContentFactory behavior changed to throw a ZipException if the
			// file is not a valid zip file, before it would just return an empty unsigned content object.
			// Here we return OK_STATUS just to keep previous behavior with the assumption that an error
			// will be detected for the invalid artifact later.
			return Status.OK_STATUS;
		}
		ArrayList<IStatus> allStatus = new ArrayList<IStatus>(0);
		SignedContentEntry[] entries = signedContent.getSignedEntries();
		for (int i = 0; i < entries.length; i++)
			try {
				entries[i].verify();
			} catch (InvalidContentException e) {
				allStatus.add(new Status(IStatus.ERROR, Activator.ID, MirrorRequest.ARTIFACT_PROCESSING_ERROR, Messages.SignatureVerification_invalidContent + entries[i].getName(), e));
			} catch (OutOfMemoryError e) {
				allStatus.add(new Status(IStatus.ERROR, Activator.ID, Messages.SignatureVerifier_OutOfMemory, e));
				break;
			}
		if (allStatus.size() > 0)
			return new MultiStatus(Activator.ID, IStatus.ERROR, allStatus.toArray(new IStatus[allStatus.size()]), Messages.SignatureVerification_invalidFileContent + inputFile, null);
		return Status.OK_STATUS;
	}

	public void close() throws IOException {
		// When we go to close we must have seen all the content we are going to see
		// So before closing, verify and write the result to the destination
		verify();
		super.close();
	}

}