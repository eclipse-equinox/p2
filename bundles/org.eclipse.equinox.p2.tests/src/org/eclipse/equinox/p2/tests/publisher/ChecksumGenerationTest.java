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

import java.util.Arrays;
import java.util.Collection;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
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
		return Arrays.asList(new Object[][] {{IArtifactDescriptor.DOWNLOAD_MD5, "50d4ea58b02706ab373a908338877e02"},
			{IArtifactDescriptor.DOWNLOAD_CHECKSUM.concat(".md5"), "50d4ea58b02706ab373a908338877e02"},
			{IArtifactDescriptor.DOWNLOAD_CHECKSUM.concat(".sha-256"), "11da2dd636ab76f460513cbcbfe8c56a6e5ad47aa9b38b36c6d04f8ee7722252"},
			// TODO values are from the BC itself, check by external means
			{IArtifactDescriptor.DOWNLOAD_CHECKSUM.concat(".dstu7564-512"), "b776aaeae5c45826515365fe017138eb6ac1e1ad866f7b7bcfba2ca752268afc493e3c19a9217e1ae07733676efb81123e5677dcadaf5c0ca1b530ab9f718b2c"},
			{IArtifactDescriptor.DOWNLOAD_CHECKSUM.concat(".whirlpool"), "b951e497a53600173a0c464d451672086175530deaffb9f376962153e573e99de53131910ea9c3c8243afb73444dadfade5989dc6f8c6e4a96141eaf956daa22"}});
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
		Assert.assertThat(ad.getProperty(checksumProperty), CoreMatchers.not(CoreMatchers.containsString(checksumValue)));
	}
}
