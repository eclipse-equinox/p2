/*******************************************************************************
 *  Copyright (c) 2015, 2018 Mykola Nikishov.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Mykola Nikishov - multiple artifact checksums
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.Collection;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ChecksumGenerationTest extends AbstractProvisioningTest {
	@Parameter(0)
	public String checksumProperty;
	@Parameter(1)
	public String checksumValue;

	@Parameters
	public static Collection<Object[]> generateChecksums() {
		return Arrays.asList(new Object[][] { { IArtifactDescriptor.DOWNLOAD_CHECKSUM.concat(".sha-256"),
				"11da2dd636ab76f460513cbcbfe8c56a6e5ad47aa9b38b36c6d04f8ee7722252" }, });
	}

	@Test
	public void testGenerationFile() {
		IArtifactDescriptor ad = PublisherHelper.createArtifactDescriptor(new ArtifactKey("classifierTest", "idTest", Version.createOSGi(1, 0, 0)), getTestData("Artifact to generate from", "testData/artifactRepo/simpleWithMD5/plugins/aaPlugin_1.0.0.jar"));
		assertEquals(String.format("%s checksum property", checksumProperty), checksumValue, ad.getProperty(checksumProperty));
	}

	@Test
	public void testGenerationFolder() {
		IArtifactDescriptor ad = PublisherHelper.createArtifactDescriptor(new ArtifactKey("classifierTest", "idTest", Version.createOSGi(1, 0, 0)), getTestData("Artifact to generate from", "testData/artifactRepo/simpleWithMD5/plugins/"));
		assertEquals(String.format("%s checksum property", checksumProperty), null, ad.getProperty(checksumProperty));
	}

	@Test
	public void testGenerationNoFolder() {
		IArtifactDescriptor ad = PublisherHelper.createArtifactDescriptor(new ArtifactKey("classifierTest", "idTest", Version.createOSGi(1, 0, 0)), null);
		assertThat(ad.getProperty(checksumProperty), CoreMatchers.not(CoreMatchers.containsString(checksumValue)));
	}
}
