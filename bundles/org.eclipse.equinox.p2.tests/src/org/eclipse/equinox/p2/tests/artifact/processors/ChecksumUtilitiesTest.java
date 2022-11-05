/*******************************************************************************
 *  Copyright (c) 2018, 2019 Mykola Nikishov
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Mykola Nikishov - initial implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.processors;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;

import java.util.Collection;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.processors.checksum.ChecksumUtilities;
import org.eclipse.equinox.internal.p2.artifact.processors.checksum.ChecksumVerifier;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.metadata.OSGiVersion;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ChecksumUtilitiesTest {
	@Parameter(0)
	public String propertyType;
	@Parameter(1)
	public String property;
	@Parameter(2)
	public String value;
	@Parameter(3)
	public String digestAlgorithm;
	@Parameter(4)
	public String algorithmId;

	@Parameters
	public static Collection<Object[]> generateData() {
		return asList(new Object[][] {
				{ IArtifactDescriptor.ARTIFACT_CHECKSUM, IArtifactDescriptor.ARTIFACT_CHECKSUM.concat(".md5"),
						"123456789_123456789_123456789_12", "MD5", "md5" },
				{ IArtifactDescriptor.ARTIFACT_CHECKSUM, IArtifactDescriptor.ARTIFACT_CHECKSUM.concat(".sha-256"),
						"123456789_123456789_123456789_123456789_123456789_123456789_1234", "SHA-256", "sha-256" },
				{ IArtifactDescriptor.DOWNLOAD_CHECKSUM, IArtifactDescriptor.DOWNLOAD_CHECKSUM.concat(".md5"),
						"123456789_123456789_123456789_12", "MD5", "md5" },
				{ IArtifactDescriptor.DOWNLOAD_CHECKSUM, IArtifactDescriptor.DOWNLOAD_CHECKSUM.concat(".sha-256"),
						"123456789_123456789_123456789_123456789_123456789_123456789_1234", "SHA-256", "sha-256" }
		});
	}

	private ArtifactDescriptor artifactDescriptor;

	@Before
	public void buildArtifactDescriptor() {
		artifactDescriptor = new ArtifactDescriptor(new ArtifactKey("", "", new OSGiVersion(1, 1, 1, "")));
		artifactDescriptor.setProperty(property, value);
	}

	@Test
	public void testChecksumProperty() {
		Collection<ChecksumVerifier> checksumVerifiers = ChecksumUtilities.getChecksumVerifiers(artifactDescriptor,
				propertyType, emptySet());

		assertEquals(format("Verifier for property=%s", property), 1, checksumVerifiers.size());
		ChecksumVerifier verifier = checksumVerifiers.iterator().next();
		assertEquals(digestAlgorithm, verifier.getAlgorithmName());
		assertEquals(algorithmId, verifier.getAlgorithmId());
		assertEquals(value, verifier.getExpectedChecksum());
		assertEquals(Status.OK_STATUS, verifier.getStatus());
	}

	@Test
	public void testChecksumsToSkip() {
		Collection<ChecksumVerifier> checksumVerifiers = ChecksumUtilities.getChecksumVerifiers(artifactDescriptor,
				propertyType, singleton(algorithmId));

		assertEquals(emptyList(), checksumVerifiers);
	}
}
