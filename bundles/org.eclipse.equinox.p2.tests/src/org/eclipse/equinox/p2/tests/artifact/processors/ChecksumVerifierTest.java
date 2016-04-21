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
 * 	Mykola Nikishov - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.processors;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.not;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.processors.checksum.ChecksumVerifier;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IProcessingStepDescriptor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ChecksumVerifierTest {
	@Parameters
	public static Collection<Object[]> generateData() {
		return Arrays.asList(new Object[][] {
			// legacy MD5 checksum location
			{"MD5", null, "md5", IArtifactDescriptor.DOWNLOAD_MD5, IArtifactDescriptor.ARTIFACT_MD5, "123456789_123456789_123456789_12"},
			// new checksum location
			{"MD5", null, "md5", IArtifactDescriptor.DOWNLOAD_CHECKSUM.concat(".md5"), IArtifactDescriptor.ARTIFACT_CHECKSUM.concat(".md5"), "123456789_123456789_123456789_12"},
			{"SHA-256", null, "sha-256", IArtifactDescriptor.DOWNLOAD_CHECKSUM.concat(".sha-256"), IArtifactDescriptor.ARTIFACT_CHECKSUM.concat(".sha-256"), "123456789_123456789_123456789_123456789_123456789_123456789_1234"},
			{"Whirlpool", "BC", "whirlpool", IArtifactDescriptor.DOWNLOAD_CHECKSUM.concat(".whirlpool"), IArtifactDescriptor.ARTIFACT_CHECKSUM.concat(".whirlpool"), "f3073bf4b0867c7456850fbe317b322c03b00198e15fe40b9a455abde6e1c77e31d6ed6963a6755564a1adec0ed9bb8aac71d4a457256a85e9fc55a964ede598"},
			{"DSTU7564-512", "BC", "dstu7564-512", IArtifactDescriptor.DOWNLOAD_CHECKSUM.concat(".dstu7564-512"), IArtifactDescriptor.ARTIFACT_CHECKSUM.concat(".dstu7564-512"), "b776aaeae5c45826515365fe017138eb6ac1e1ad866f7b7bcfba2ca752268afc493e3c19a9217e1ae07733676efb81123e5677dcadaf5c0ca1b530ab9f718b2c"}});
	}

	@Parameter(0)
	public String digestAlgorithm;
	@Parameter(1)
	public String providerName;
	@Parameter(2)
	public String algorithmId;
	@Parameter(3)
	public String downloadProperty;
	@Parameter(4)
	public String artifactProperty;
	@Parameter(5)
	public String checksum;

	@Test
	public void testInitialize() throws IOException, IllegalArgumentException, SecurityException {
		IProcessingStepDescriptor processingStepDescriptor = createMock(IProcessingStepDescriptor.class);
		expect(processingStepDescriptor.getData()).andReturn(checksum);
		replay(processingStepDescriptor);

		ChecksumVerifier verifier = new ChecksumVerifier(digestAlgorithm, providerName, algorithmId);

		verifier.initialize(null, processingStepDescriptor, null);

		Assert.assertEquals(Status.OK_STATUS, verifier.getStatus());

		verifier.close();
		verify(processingStepDescriptor);
	}

	@Test
	public void testInitialize_DownloadChecksum() throws IOException, IllegalArgumentException, SecurityException {
		IProcessingStepDescriptor processingStepDescriptor = createMock(IProcessingStepDescriptor.class);
		expect(processingStepDescriptor.getData()).andReturn(downloadProperty);
		IArtifactDescriptor artifactDescriptor = createMock(IArtifactDescriptor.class);
		replay(processingStepDescriptor);
		expect(artifactDescriptor.getProperty(eq(downloadProperty))).andReturn(checksum);
		expect(artifactDescriptor.getProperty(not(eq(downloadProperty)))).andReturn(null).times(1, 2);
		HashMap<String, String> properties = new HashMap<>();
		properties.put(downloadProperty, checksum);
		expect(artifactDescriptor.getProperties()).andReturn(properties);
		replay(artifactDescriptor);

		ChecksumVerifier verifier = new ChecksumVerifier(digestAlgorithm, providerName, algorithmId);

		verifier.initialize(null, processingStepDescriptor, artifactDescriptor);

		Assert.assertEquals(Status.OK_STATUS, verifier.getStatus());

		verifier.close();
		verify(processingStepDescriptor);
	}

	@Test
	public void testInitialize_ArtifactChecksum() throws IOException, IllegalArgumentException, SecurityException {
		IProcessingStepDescriptor processingStepDescriptor = createMock(IProcessingStepDescriptor.class);
		expect(processingStepDescriptor.getData()).andReturn(artifactProperty);
		IArtifactDescriptor artifactDescriptor = createMock(IArtifactDescriptor.class);
		replay(processingStepDescriptor);
		expect(artifactDescriptor.getProperty(eq(artifactProperty))).andReturn(checksum);
		HashMap<String, String> properties = new HashMap<>();
		properties.put(artifactProperty, checksum);
		expect(artifactDescriptor.getProperties()).andReturn(properties);
		expect(artifactDescriptor.getProperty(not(eq(artifactProperty)))).andReturn(null).times(1, 2);
		replay(artifactDescriptor);

		ChecksumVerifier verifier = new ChecksumVerifier(digestAlgorithm, providerName, algorithmId);

		verifier.initialize(null, processingStepDescriptor, artifactDescriptor);

		Assert.assertEquals(Status.OK_STATUS, verifier.getStatus());

		verifier.close();
		verify(processingStepDescriptor);
	}
}
