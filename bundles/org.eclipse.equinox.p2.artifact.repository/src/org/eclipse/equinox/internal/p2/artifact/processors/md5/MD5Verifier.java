/*******************************************************************************
 * Copyright (c) 2007, 2011 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 * 	IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.processors.md5;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStep;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IProcessingStepDescriptor;
import org.eclipse.osgi.util.NLS;

public class MD5Verifier extends ProcessingStep {

	protected String expectedMD5;
	private MessageDigest md5;

	public MD5Verifier() {
		super();
	}

	public MD5Verifier(String expected) {
		super();
		this.expectedMD5 = expected;
		basicInitialize(null);
	}

	//This handle the case where the MD5 verification is initiated by a processing step
	public void initialize(IProvisioningAgent agent, IProcessingStepDescriptor descriptor, IArtifactDescriptor context) {
		super.initialize(agent, descriptor, context);
		String data = descriptor.getData();
		if (IArtifactDescriptor.DOWNLOAD_MD5.equals(data))
			expectedMD5 = context.getProperty(IArtifactDescriptor.DOWNLOAD_MD5);
		else if (IArtifactDescriptor.ARTIFACT_MD5.equals(data))
			expectedMD5 = context.getProperty(IArtifactDescriptor.ARTIFACT_MD5);
		else
			expectedMD5 = data;
		basicInitialize(descriptor);
	}

	private void basicInitialize(IProcessingStepDescriptor descriptor) {
		int code = (descriptor == null) ? IStatus.ERROR : descriptor.isRequired() ? IStatus.ERROR : IStatus.INFO;
		if (expectedMD5 == null || expectedMD5.length() != 32)
			setStatus(new Status(code, Activator.ID, NLS.bind(Messages.Error_invalid_hash, expectedMD5)));
		try {
			md5 = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
		} catch (NoSuchAlgorithmException e) {
			setStatus(new Status(code, Activator.ID, Messages.Error_MD5_unavailable, e));
		}
	}

	public void write(int b) throws IOException {
		md5.update((byte) b);
		getDestination().write(b);
	}

	public void close() throws IOException {
		byte[] digest = md5.digest();
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < digest.length; i++) {
			if ((digest[i] & 0xFF) < 0x10)
				buf.append('0');
			buf.append(Integer.toHexString(digest[i] & 0xFF));
		}

		// if the hashes don't line up set the status to error.
		if (!buf.toString().equals(expectedMD5))
			setStatus(new Status(IStatus.ERROR, Activator.ID, ProvisionException.ARTIFACT_MD5_NOT_MATCH, NLS.bind(Messages.Error_unexpected_hash, expectedMD5, buf), null));
		super.close();
	}
}
