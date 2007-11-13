/*******************************************************************************
 * Copyright (c) 2007 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 * 	IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.processors.verifier;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.processors.Activator;
import org.eclipse.equinox.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStepDescriptor;

public class MD5Verifier extends ProcessingStep {

	protected String md5Test;
	private MessageDigest md5;

	public MD5Verifier() {
		super();
	}

	public MD5Verifier(String md5Test) {
		super();
		this.md5Test = md5Test;
		basicInitialize(null);
	}

	public void initialize(ProcessingStepDescriptor descriptor, IArtifactDescriptor context) {
		super.initialize(descriptor, context);
		String data = descriptor.getData();
		if (data.equals("download"))
			md5Test = context.getProperty(IArtifactDescriptor.DOWNLOAD_MD5);
		else if (data.equals("artifact"))
			md5Test = context.getProperty(IArtifactDescriptor.ARTIFACT_MD5);
		else
			md5Test = data;
		basicInitialize(descriptor);
	}

	private void basicInitialize(ProcessingStepDescriptor descriptor) {
		int code;
		if (descriptor == null)
			code = IStatus.ERROR;
		else
			code = descriptor.isRequired() ? IStatus.ERROR : IStatus.INFO;
		if (md5Test == null || md5Test.length() != 32)
			status = new Status(code, Activator.ID, "MD5 value not available or incorrect size");
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			status = new Status(code, Activator.ID, "Could not create MD5 algorithm", e);
		}
	}

	public void write(int b) throws IOException {
		if (b != -1)
			md5.update((byte) b);
		destination.write(b);
	}

	public void close() throws IOException {
		super.close();

		byte[] digest = md5.digest();
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < digest.length; i++) {
			if ((digest[i] & 0xFF) < 0x10)
				buf.append('0');
			buf.append(Integer.toHexString(digest[i] & 0xFF));
		}

		// if the hashes don't line up set the status to error.
		if (!buf.toString().equals(md5Test)) {
			String message = "Error processing stream. MD5 hash is not as expected.";
			status = new Status(IStatus.ERROR, "plugin id", message);
		} else
			status = Status.OK_STATUS;
	}
}
