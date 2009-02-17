package org.eclipse.equinox.p2.tests.artifact.repository;

import java.net.URI;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.MirrorRequest;
import org.eclipse.equinox.internal.p2.engine.DownloadManager;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class CorruptedJar extends AbstractProvisioningTest {
	private static final String testDataLocation = "testData/artifactRepo/corruptedJarRepo";
	IArtifactRepository source = null;
	IArtifactRepository target = null;

	@Override
	protected void setUp() throws Exception {
		// TODO Auto-generated method stub
		super.setUp();
		IArtifactRepositoryManager mgr = getArtifactRepositoryManager();
		try {
			source = mgr.loadRepository((getTestData("CorruptedJar repo", testDataLocation).toURI()), null);
		} catch (Exception e) {
			fail("1.0", e);
		}
		try {
			target = mgr.createRepository(getTestFolder("CorruptedJarTarget").toURI(), "CorruptedJar target repo", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		} catch (Exception e) {
			fail("2.0", e);
		}
	}

	public void testDownloadCorruptedJar() {
		ProvisioningContext ctx = new ProvisioningContext();
		ctx.setArtifactRepositories(new URI[] {getTestData("CorruptedJar repo", testDataLocation).toURI()});
		DownloadManager mgr = new DownloadManager(ctx);
		mgr.add(new MirrorRequest(source.getArtifactKeys()[0], target, null, null));
		IStatus s = mgr.start(new NullProgressMonitor());
		assertNotOK(s);
	}
}
