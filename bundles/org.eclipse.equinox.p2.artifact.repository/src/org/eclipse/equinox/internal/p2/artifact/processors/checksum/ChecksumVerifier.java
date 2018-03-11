/*******************************************************************************
 * Copyright (c) 2015, 2018 Mykola Nikishov.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mykola Nikishov - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.processors.checksum;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.p2.repository.helpers.ChecksumHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IProcessingStepDescriptor;
import org.eclipse.osgi.util.NLS;

final public class ChecksumVerifier extends MessageDigestProcessingStep {

	private String expectedChecksum;
	private String algorithmName;
	private String algorithmId;

	// public to access from tests
	public ChecksumVerifier(String digestAlgorithm, String algorithmId, String expectedChecksum) {
		this.algorithmName = digestAlgorithm;
		this.algorithmId = algorithmId;
		this.expectedChecksum = expectedChecksum;
		basicInitialize(null);
	}

	@Override
	public final void initialize(IProvisioningAgent agent, IProcessingStepDescriptor descriptor, IArtifactDescriptor context) {
		super.initialize(agent, descriptor, context);
		String data = descriptor.getData();
		if (IArtifactDescriptor.DOWNLOAD_CHECKSUM.concat(".").concat(algorithmId).equals(data)) //$NON-NLS-1$
			expectedChecksum = ChecksumHelper.getChecksums(context, IArtifactDescriptor.DOWNLOAD_CHECKSUM).get(algorithmId);
		else if (IArtifactDescriptor.ARTIFACT_CHECKSUM.concat(".").concat(algorithmId).equals(data)) //$NON-NLS-1$
			expectedChecksum = ChecksumHelper.getChecksums(context, IArtifactDescriptor.ARTIFACT_CHECKSUM).get(algorithmId);
		else
			expectedChecksum = data;

		basicInitialize(descriptor);
	}

	private void basicInitialize(IProcessingStepDescriptor descriptor) {
		int code = (descriptor == null) ? IStatus.ERROR : descriptor.isRequired() ? IStatus.ERROR : IStatus.INFO;
		if (Optional.ofNullable(expectedChecksum).orElse("").isEmpty()) //$NON-NLS-1$
			setStatus(new Status(code, Activator.ID, NLS.bind(Messages.Error_invalid_checksum, algorithmName, expectedChecksum)));
		try {
			messageDigest = MessageDigest.getInstance(algorithmName);
		} catch (NoSuchAlgorithmException e) {
			setStatus(new Status(code, Activator.ID, NLS.bind(Messages.Error_checksum_unavailable, algorithmName), e));
		}
	}

	@Override
	final protected void onClose(String digestString) {
		// if the hashes don't line up set the status to error.
		if (!digestString.equals(expectedChecksum))
			// TODO like ProvisionException.ARTIFACT_MD5_NOT_MATCH but for any checksum
			setStatus(new Status(IStatus.ERROR, Activator.ID, ProvisionException.ARTIFACT_MD5_NOT_MATCH, NLS.bind(Messages.Error_unexpected_checksum, new Object[] {algorithmName, expectedChecksum, digestString}), null));
	}

	public String getExpectedChecksum() {
		return expectedChecksum;
	}

	public String getAlgorithmName() {
		return algorithmName;
	}

	public String getAlgorithmId() {
		return algorithmId;
	}

}
