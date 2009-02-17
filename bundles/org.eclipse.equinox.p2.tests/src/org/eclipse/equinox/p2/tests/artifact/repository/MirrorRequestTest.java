package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorRequest;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class MirrorRequestTest extends AbstractProvisioningTest {
	private static final String testDataLocation = "testData/artifactRepo/emptyJarRepo";
	File targetLocation;
	IArtifactRepository targetRepository, sourceRepository;

	public void setUp() throws Exception {
		super.setUp();
		targetLocation = File.createTempFile("target", ".repo");
		targetLocation.delete();
		targetLocation.mkdirs();
		targetRepository = new SimpleArtifactRepository("TargetRepo", targetLocation.toURI(), null);

		IArtifactRepositoryManager mgr = getArtifactRepositoryManager();
		sourceRepository = mgr.loadRepository((getTestData("EmptyJar repo", testDataLocation).toURI()), null);

	}

	protected void tearDown() throws Exception {
		AbstractProvisioningTest.delete(targetLocation);
		super.tearDown();
	}

	public void testInvalidZipFileInTheSource() {
		IArtifactKey key = new ArtifactKey("org.eclipse.update.feature", "HelloWorldFeature", new Version(1, 0, 0));
		Properties targetProperties = new Properties();
		targetProperties.put("artifact.folder", "true");
		MirrorRequest request = new MirrorRequest(key, targetRepository, null, targetProperties);
		request.setSourceRepository(sourceRepository);

		request.perform(new NullProgressMonitor());

		assertTrue(request.getResult().matches(IStatus.ERROR));
		assertTrue(request.getResult().getException() instanceof IOException);
	}

	public void testMissingArtifact() {
		IArtifactKey key = new ArtifactKey("org.eclipse.update.feature", "Missing", new Version(1, 0, 0));
		Properties targetProperties = new Properties();
		targetProperties.put("artifact.folder", "true");
		MirrorRequest request = new MirrorRequest(key, targetRepository, null, targetProperties);
		request.setSourceRepository(sourceRepository);

		request.perform(new NullProgressMonitor());

		assertTrue(request.getResult().matches(IStatus.ERROR));
	}
}
