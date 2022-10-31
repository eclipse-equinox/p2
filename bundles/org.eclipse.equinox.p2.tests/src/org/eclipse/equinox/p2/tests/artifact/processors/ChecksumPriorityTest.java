/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.processors;

import static java.util.Collections.emptySet;
import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.stream.Collectors;
import org.eclipse.equinox.internal.p2.artifact.processors.checksum.ChecksumUtilities;
import org.eclipse.equinox.internal.p2.artifact.processors.checksum.ChecksumVerifier;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.metadata.OSGiVersion;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.junit.Test;

public class ChecksumPriorityTest {

	@Test
	public void testChecksumPriorityWithFullSet() {
		ArtifactDescriptor artifactDescriptor = new ArtifactDescriptor(
				new ArtifactKey("", "", new OSGiVersion(1, 1, 1, "")));
		artifactDescriptor.setProperty(IArtifactDescriptor.ARTIFACT_CHECKSUM + ".md5", "abc");
		artifactDescriptor.setProperty(IArtifactDescriptor.ARTIFACT_CHECKSUM + ".sha-1", "abc");
		artifactDescriptor.setProperty(IArtifactDescriptor.ARTIFACT_CHECKSUM + ".sha-256", "abc");
		artifactDescriptor.setProperty(IArtifactDescriptor.ARTIFACT_CHECKSUM + ".sha-512", "abc");
		assertBestChoice(artifactDescriptor, "sha-512");
	}

	@Test
	public void testChecksumPriorityWithStandardSet() {
		ArtifactDescriptor artifactDescriptor = new ArtifactDescriptor(
				new ArtifactKey("", "", new OSGiVersion(1, 1, 1, "")));
		artifactDescriptor.setProperty(IArtifactDescriptor.ARTIFACT_CHECKSUM + ".md5", "abc");
		artifactDescriptor.setProperty(IArtifactDescriptor.ARTIFACT_CHECKSUM + ".sha-256", "abc");
		assertBestChoice(artifactDescriptor, "sha-256");
	}

	@Test
	public void testChecksumPriorityWithDeprecatedSet() {
		ArtifactDescriptor artifactDescriptor = new ArtifactDescriptor(
				new ArtifactKey("", "", new OSGiVersion(1, 1, 1, "")));
		artifactDescriptor.setProperty(IArtifactDescriptor.ARTIFACT_CHECKSUM + ".md5", "abc");
		artifactDescriptor.setProperty(IArtifactDescriptor.ARTIFACT_CHECKSUM + ".sha-1", "abc");
		assertBestChoice(artifactDescriptor, "sha-1");
	}

	private static void assertBestChoice(ArtifactDescriptor artifactDescriptor, String bestAlgorithm) {
		Collection<ChecksumVerifier> checksumVerifiers = ChecksumUtilities.getChecksumVerifiers(artifactDescriptor,
				IArtifactDescriptor.ARTIFACT_CHECKSUM, emptySet());
		// if we have the choice, just get the best!
		assertEquals(
				"more than one algorithm was choosen: " + checksumVerifiers.stream()
						.map(ChecksumVerifier::getAlgorithmId).collect(Collectors.joining(", ")),
				1, checksumVerifiers.size());
		ChecksumVerifier verifier = checksumVerifiers.iterator().next();
		assertEquals(bestAlgorithm, verifier.getAlgorithmId());
	}
}
