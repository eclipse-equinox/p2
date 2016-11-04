/*******************************************************************************
 *  Copyright (c) 2005, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.publisher.actions;

import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

public class MD5GenerationTest extends AbstractProvisioningTest {
	public void testGenerationFile() {
		IArtifactDescriptor ad = PublisherHelper.createArtifactDescriptor(new ArtifactKey("classifierTest", "idTest", Version.createOSGi(1, 0, 0)), getTestData("Artifact to generate from", "testData/artifactRepo/simpleWithMD5/plugins/aaPlugin_1.0.0.jar"));
		assertEquals("50d4ea58b02706ab373a908338877e02", ad.getProperty(IArtifactDescriptor.DOWNLOAD_MD5));
	}

	public void testGenerationFile_emptyPublisherInfo() {
		ArtifactKey key = new ArtifactKey("classifierTest", "idTest", Version.createOSGi(1, 0, 0));
		IPublisherInfo publisherInfo = new PublisherInfo();
		IArtifactDescriptor ad = PublisherHelper.createArtifactDescriptor(publisherInfo, key, getTestData("Artifact to generate from", "testData/artifactRepo/simpleWithMD5/plugins/aaPlugin_1.0.0.jar"));
		assertEquals("50d4ea58b02706ab373a908338877e02", ad.getProperty(IArtifactDescriptor.DOWNLOAD_MD5));
	}

	public void testGenerationFile_noMd5() {
		ArtifactKey key = new ArtifactKey("classifierTest", "idTest", Version.createOSGi(1, 0, 0));
		PublisherInfo publisherInfo = new PublisherInfo();
		publisherInfo.setArtifactOptions(IPublisherInfo.A_NO_MD5);
		IArtifactDescriptor ad = PublisherHelper.createArtifactDescriptor(publisherInfo, key, getTestData("Artifact to generate from", "testData/artifactRepo/simpleWithMD5/plugins/aaPlugin_1.0.0.jar"));
		assertNull(ad.getProperty(IArtifactDescriptor.DOWNLOAD_MD5));
	}

	public void testGenerationFolder() {
		IArtifactDescriptor ad = PublisherHelper.createArtifactDescriptor(new ArtifactKey("classifierTest", "idTest", Version.createOSGi(1, 0, 0)), getTestData("Artifact to generate from", "testData/artifactRepo/simpleWithMD5/plugins/"));
		assertNull(ad.getProperty(IArtifactDescriptor.DOWNLOAD_MD5));
	}

	public void testGenerationNoFolder() {
		IArtifactDescriptor ad = PublisherHelper.createArtifactDescriptor(new ArtifactKey("classifierTest", "idTest", Version.createOSGi(1, 0, 0)), null);
		assertNull(ad.getProperty(IArtifactDescriptor.DOWNLOAD_MD5));
	}
}
