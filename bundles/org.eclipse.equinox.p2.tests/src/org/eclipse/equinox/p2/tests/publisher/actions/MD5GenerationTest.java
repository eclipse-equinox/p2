package org.eclipse.equinox.p2.tests.publisher.actions;

import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

public class MD5GenerationTest extends AbstractProvisioningTest {
	public void testGenerationFile() {
		IArtifactDescriptor ad = PublisherHelper.createArtifactDescriptor(new ArtifactKey("classifierTest", "idTest", new Version(1, 0, 0)), getTestData("Artifact to generate from", "testData/artifactRepo/simpleWithMD5/plugins/aaPlugin_1.0.0.jar"));
		assertEquals("50d4ea58b02706ab373a908338877e02", ad.getProperty(IArtifactDescriptor.DOWNLOAD_MD5));
	}

	public void testGenerationFile2() {
		IArtifactDescriptor ad = PublisherHelper.createArtifactDescriptor(new ArtifactKey("classifierTest", "idTest", new Version(1, 0, 0)), getTestData("Artifact to generate from", "testData/artifactRepo/simpleWithMD5/plugins/aaPlugin_1.0.0.jar"));
		assertEquals("50d4ea58b02706ab373a908338877e02", ad.getProperty(IArtifactDescriptor.DOWNLOAD_MD5));
	}

	public void testGenerationFolder() {
		IArtifactDescriptor ad = PublisherHelper.createArtifactDescriptor(new ArtifactKey("classifierTest", "idTest", new Version(1, 0, 0)), getTestData("Artifact to generate from", "testData/artifactRepo/simpleWithMD5/plugins/"));
		assertNull(ad.getProperty(IArtifactDescriptor.DOWNLOAD_MD5));
	}

	public void testGenerationFolder2() {
		IArtifactDescriptor ad = PublisherHelper.createArtifactDescriptor(new ArtifactKey("classifierTest", "idTest", new Version(1, 0, 0)), getTestData("Artifact to generate from", "testData/artifactRepo/simpleWithMD5/plugins/"));
		assertNull(ad.getProperty(IArtifactDescriptor.DOWNLOAD_MD5));
	}

	public void testGenerationNoFolder() {
		IArtifactDescriptor ad = PublisherHelper.createArtifactDescriptor(new ArtifactKey("classifierTest", "idTest", new Version(1, 0, 0)), null);
		assertNull(ad.getProperty(IArtifactDescriptor.DOWNLOAD_MD5));
	}

	public void testGenerationNoFolder2() {
		IArtifactDescriptor ad = PublisherHelper.createArtifactDescriptor(new ArtifactKey("classifierTest", "idTest", new Version(1, 0, 0)), null);
		assertNull(ad.getProperty(IArtifactDescriptor.DOWNLOAD_MD5));
	}
}
