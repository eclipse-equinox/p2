/*******************************************************************************
 * Copyright (c) 2015, 2021 Mykola Nikishov and others.
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

import static org.junit.Assert.assertEquals;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.processors.checksum.ChecksumVerifier;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IProcessingStepDescriptor;
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
				// new checksum location
				{ "MD5", null, "md5", IArtifactDescriptor.DOWNLOAD_CHECKSUM.concat(".md5"),
						IArtifactDescriptor.ARTIFACT_CHECKSUM.concat(".md5"), "123456789_123456789_123456789_12" },
				{ "SHA-256", null, "sha-256", IArtifactDescriptor.DOWNLOAD_CHECKSUM.concat(".sha-256"),
						IArtifactDescriptor.ARTIFACT_CHECKSUM.concat(".sha-256"),
						"123456789_123456789_123456789_123456789_123456789_123456789_1234" } });
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
		IProcessingStepDescriptor processingStepDescriptor = mock(IProcessingStepDescriptor.class);
		when(processingStepDescriptor.getData()).thenReturn(checksum);

		try (ChecksumVerifier verifier = new ChecksumVerifier(digestAlgorithm, providerName, algorithmId, false, 0)) {
			verifier.initialize(null, processingStepDescriptor, null);
			assertEquals(Status.OK_STATUS, verifier.getStatus());
		}
	}

	@Test
	public void testInitialize_DownloadChecksum() throws IOException, IllegalArgumentException, SecurityException {
		IProcessingStepDescriptor processingStepDescriptor = mock(IProcessingStepDescriptor.class);
		when(processingStepDescriptor.getData()).thenReturn(downloadProperty);
		IArtifactDescriptor artifactDescriptor = mock(IArtifactDescriptor.class);
		when(artifactDescriptor.getProperty(downloadProperty)).thenReturn(checksum);
		when(artifactDescriptor.getProperty(not(eq(downloadProperty)))).thenReturn(null);
		HashMap<String, String> properties = new HashMap<>();
		properties.put(downloadProperty, checksum);
		when(artifactDescriptor.getProperties()).thenReturn(properties);

		try (ChecksumVerifier verifier = new ChecksumVerifier(digestAlgorithm, providerName, algorithmId, false, 0)) {
			verifier.initialize(null, processingStepDescriptor, artifactDescriptor);
			assertEquals(Status.OK_STATUS, verifier.getStatus());
		}
	}

	@Test
	public void testInitialize_ArtifactChecksum() throws IOException, IllegalArgumentException, SecurityException {
		IProcessingStepDescriptor processingStepDescriptor = mock(IProcessingStepDescriptor.class);
		when(processingStepDescriptor.getData()).thenReturn(artifactProperty);
		IArtifactDescriptor artifactDescriptor = mock(IArtifactDescriptor.class);
		when(artifactDescriptor.getProperty(artifactProperty)).thenReturn(checksum);
		HashMap<String, String> properties = new HashMap<>();
		properties.put(artifactProperty, checksum);
		when(artifactDescriptor.getProperties()).thenReturn(properties);
		when(artifactDescriptor.getProperty(not(eq(artifactProperty)))).thenReturn(null);

		try (ChecksumVerifier verifier = new ChecksumVerifier(digestAlgorithm, providerName, algorithmId, false, 0)) {
			verifier.initialize(null, processingStepDescriptor, artifactDescriptor);
			assertEquals(Status.OK_STATUS, verifier.getStatus());
		}
	}
}
