/*******************************************************************************
 * Copyright (c) 2015, 2019 Mykola Nikishov.
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

import static java.util.Optional.ofNullable;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.p2.repository.helpers.ChecksumProducer;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IProcessingStepDescriptor;
import org.eclipse.osgi.util.NLS;

public final class ChecksumVerifier extends MessageDigestProcessingStep {

	private String expectedChecksum;
	private final String algorithmName;
	private final String providerName;
	private final String algorithmId;
	private final boolean insecureAlgorithm;
	private final int priority;

	@Deprecated
	public ChecksumVerifier(String digestAlgorithm, String providerName, String algorithmId, boolean insecure) {
		this(digestAlgorithm, providerName, algorithmId, insecure, 0);
	}

	// public to access from tests
	public ChecksumVerifier(String digestAlgorithm, String providerName, String algorithmId, boolean insecure,
			int priority) {
		this.algorithmName = digestAlgorithm;
		this.providerName = providerName;
		this.algorithmId = algorithmId;
		this.insecureAlgorithm = insecure;
		this.priority = priority;
		basicInitialize(null);
	}

	@Override
	public void initialize(IProvisioningAgent agent, IProcessingStepDescriptor descriptor, IArtifactDescriptor context) {
		super.initialize(agent, descriptor, context);
		basicInitialize(descriptor);
		if (!getStatus().isOK()) {
			return;
		}

		expectedChecksum = descriptor.getData();
		if (ofNullable(expectedChecksum).orElse("").isEmpty()) { //$NON-NLS-1$
			int code = buildErrorCode(descriptor);
			setStatus(new Status(code, Activator.ID, NLS.bind(Messages.Error_invalid_checksum, algorithmName, expectedChecksum)));
		}
	}

	private void basicInitialize(IProcessingStepDescriptor descriptor) {
		try {
			messageDigest = ChecksumProducer.getMessageDigest(algorithmName, providerName);
			setStatus(Status.OK_STATUS);
		} catch (NoSuchProviderException | NoSuchAlgorithmException e) {
			int code = buildErrorCode(descriptor);
			setStatus(new Status(code, Activator.ID, NLS.bind(Messages.Error_checksum_unavailable, algorithmName), e));
		}
	}

	private int buildErrorCode(IProcessingStepDescriptor descriptor) {
		return (descriptor == null) ? IStatus.ERROR : descriptor.isRequired() ? IStatus.ERROR : IStatus.INFO;
	}

	@Override
	protected void onClose(String digestString) {
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

	public boolean isInsecureAlgorithm() {
		return insecureAlgorithm;
	}

	public int getPriority() {
		return priority;
	}
}
