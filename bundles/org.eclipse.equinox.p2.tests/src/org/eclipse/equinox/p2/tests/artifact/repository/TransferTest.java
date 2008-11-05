package org.eclipse.equinox.p2.tests.artifact.repository;

import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class TransferTest extends AbstractProvisioningTest {

	//	public void testTransferError() {
	//		File simpleRepo = getTestData("simple repository", "testData/artifactRepo/transferTestRepo");
	//		IArtifactRepository source = null;
	//		IArtifactRepository target = null;
	//		try {
	//			source = getArtifactRepositoryManager().loadRepository(simpleRepo.toURI(), new NullProgressMonitor());
	//			target = createArtifactRepository(new File(getTempFolder(), getName()).toURI(), null);
	//		} catch (ProvisionException e) {
	//			fail("failing setting up the tests", e);
	//		}
	//
	//		IArtifactDescriptor sourceDescriptor = getArtifactKeyFor(source, "osgi.bundle", "missingFromFileSystem", new Version(1, 0, 0))[0];
	//		ArtifactDescriptor targetDescriptor = new ArtifactDescriptor(sourceDescriptor);
	//		targetDescriptor.setRepositoryProperty("artifact.folder", "true");
	//			new MirrorRequest(new ArtifactKey("osgi.bundle", "missingFromFileSystem", new Version(1, 0, 0)), target, null, null).transferSingle(targetDescriptor, sourceDescriptor, new NullProgressMonitor());
	//				System.out.println(s);
	//	}
	//
	//	private IArtifactDescriptor[] getArtifactKeyFor(IArtifactRepository repo, String classifier, String id, Version version) {
	//		return repo.getArtifactDescriptors(new ArtifactKey(classifier, id, version));
	//	}

}
