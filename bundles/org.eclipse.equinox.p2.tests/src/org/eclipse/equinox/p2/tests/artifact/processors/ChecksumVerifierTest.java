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
 * 	Mykola Nikishov - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.processors;

import static org.easymock.EasyMock.*;

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
			{"MD5", "md5", IArtifactDescriptor.DOWNLOAD_CHECKSUM.concat(".md5"), IArtifactDescriptor.ARTIFACT_CHECKSUM.concat(".md5"), "123456789_123456789_123456789_12"},
			{"SHA-256", "sha-256", IArtifactDescriptor.DOWNLOAD_CHECKSUM.concat(".sha-256"), IArtifactDescriptor.ARTIFACT_CHECKSUM.concat(".sha-256"), "123456789_123456789_123456789_123456789_123456789_123456789_1234"}});
	}

	@Parameter(0)
	public String digestAlgorithm;
	@Parameter(1)
	public String algorithmId;
	@Parameter(2)
	public String downloadProperty;
	@Parameter(3)
	public String artifactProperty;
	@Parameter(4)
	public String checksum;

	@Test
	public void testInitialize() throws IOException, IllegalArgumentException, SecurityException {
		IProcessingStepDescriptor processingStepDescriptor = createMock(IProcessingStepDescriptor.class);
		expect(processingStepDescriptor.getData()).andReturn(checksum);
		replay(processingStepDescriptor);

		ChecksumVerifier verifier = new ChecksumVerifier(digestAlgorithm, algorithmId);

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

		ChecksumVerifier verifier = new ChecksumVerifier(digestAlgorithm, algorithmId);

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

		ChecksumVerifier verifier = new ChecksumVerifier(digestAlgorithm, algorithmId);

		verifier.initialize(null, processingStepDescriptor, artifactDescriptor);

		Assert.assertEquals(Status.OK_STATUS, verifier.getStatus());

		verifier.close();
		verify(processingStepDescriptor);
	}
}
