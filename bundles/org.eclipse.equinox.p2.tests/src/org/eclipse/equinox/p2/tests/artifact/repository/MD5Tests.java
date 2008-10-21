package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.ByteArrayOutputStream;
import java.io.File;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class MD5Tests extends AbstractProvisioningTest {
	File testRepo = null;
	IArtifactRepository repo = null;

	protected void setUp() throws Exception {
		super.setUp();
		testRepo = getTestData("Repository with MD5", "testData/artifactRepo/simpleWithMD5");
		repo = getArtifactRepositoryManager().loadRepository(testRepo.toURI(), new NullProgressMonitor());
		assertNotNull("1.0", repo);
	}

	public void testCheckMD5() {
		IArtifactKey[] keys = repo.getArtifactKeys();
		for (int i = 0; i < keys.length; i++) {
			IArtifactDescriptor[] desc = repo.getArtifactDescriptors(keys[i]);
			for (int j = 0; j < desc.length; j++) {
				IStatus status = repo.getArtifact(desc[j], new ByteArrayOutputStream(500), new NullProgressMonitor());
				if (desc[j].getArtifactKey().getId().startsWith("bogus")) {
					assertNotOK(status);
					continue;
				}
				assertOK("2.1 " + desc[j], status);
			}
		}
	}

	protected void tearDown() throws Exception {
		getArtifactRepositoryManager().removeRepository(testRepo.toURI());
		super.tearDown();
	}
}
