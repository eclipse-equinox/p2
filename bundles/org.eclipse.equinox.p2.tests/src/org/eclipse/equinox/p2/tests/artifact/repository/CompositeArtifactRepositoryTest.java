/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.osgi.framework.Version;

public class CompositeArtifactRepositoryTest extends AbstractProvisioningTest {
	private static final String TEST_KEY = "TestKey";
	private static final String TEST_VALUE = "TestValue";
	//artifact repository to remove on tear down
	private File repositoryFile = null;
	private URI repositoryURI = null;

	protected void tearDown() throws Exception {
		super.tearDown();
		//repository location is not used by all tests
		if (repositoryURI != null) {
			getArtifactRepositoryManager().removeRepository(repositoryURI);
			repositoryURI = null;
		}
		if (repositoryFile != null) {
			delete(repositoryFile);
			repositoryFile = null;
		}
	}

	public void testCompressedRepositoryCreation() {
		//create a compressed repo
		createRepo(true);

		File files[] = repositoryFile.listFiles();
		boolean jarFilePresent = false;
		boolean artifactFilePresent = false;
		for (int i = 0; i < files.length; i++) {
			if ("compositeArtifacts.jar".equalsIgnoreCase(files[i].getName())) {
				jarFilePresent = true;
			}
			if ("compositeArtifacts.xml".equalsIgnoreCase(files[i].getName())) {
				artifactFilePresent = false;
			}
		}
		if (!jarFilePresent)
			fail("Repository should create JAR for compositeArtifacts.xml");
		if (artifactFilePresent)
			fail("Repository should not create compositeArtifacts.xml");
	}

	public void testVerifyUncompressedRepositoryCreation() {
		//Setup: create an uncompressed repository
		createRepo(false);

		File files[] = repositoryFile.listFiles();
		boolean jarFilePresent = false;
		boolean artifactFilePresent = false;
		for (int i = 0; i < files.length; i++) {
			if ("compositeArtifacts.jar".equalsIgnoreCase(files[i].getName())) {
				jarFilePresent = true;
			}
			if ("compositeArtifacts.xml".equalsIgnoreCase(files[i].getName())) {
				artifactFilePresent = true;
			}
		}
		if (jarFilePresent)
			fail("Repository should not create JAR for compositeArtifacts.xml");
		if (!artifactFilePresent)
			fail("Repository should create compositeArtifacts.xml");
	}

	public void testAddDescriptor() {
		//Setup: create an uncompressed repository
		CompositeArtifactRepository compRepo = createRepo(false);

		//Setup create a descriptor
		IArtifactKey key = PublisherHelper.createBinaryArtifactKey("testKeyId", new Version("1.2.3"));
		IArtifactDescriptor descriptor = PublisherHelper.createArtifactDescriptor(key, null);

		try {
			compRepo.addDescriptor(descriptor);
			fail("Should not be able to add Artifact Descriptor");
		} catch (UnsupportedOperationException e) {
			//expected. fall through
		}
	}

	public void testAddDescriptors() {
		//Setup: create an uncompressed repository
		CompositeArtifactRepository compRepo = createRepo(false);

		//Setup create a descriptor
		IArtifactKey key = PublisherHelper.createBinaryArtifactKey("testKeyId", new Version("1.2.3"));
		IArtifactDescriptor descriptor = PublisherHelper.createArtifactDescriptor(key, null);

		//Setup: create an array of descriptors
		IArtifactDescriptor[] descriptors = new IArtifactDescriptor[1];
		descriptors[0] = descriptor;

		try {
			compRepo.addDescriptors(descriptors);
			fail("Should not be able to add Artifact Descriptors using an array");
		} catch (UnsupportedOperationException e) {
			//expected. fall through
		}
	}

	public void testRemoveDescriptorUsingDescriptor() {
		//Setup: create an uncompressed repository
		CompositeArtifactRepository compRepo = createRepo(false);

		//Setup create a descriptor
		IArtifactKey key = PublisherHelper.createBinaryArtifactKey("testKeyId", new Version("1.2.3"));
		IArtifactDescriptor descriptor = PublisherHelper.createArtifactDescriptor(key, null);

		try {
			compRepo.removeDescriptor(descriptor);
			fail("Should not be able to remove Artifact Descriptor using a Artifact Descriptor");
		} catch (UnsupportedOperationException e) {
			//expected. fall through
		}
	}

	public void testRemoveDescriptorUsingKey() {
		//Setup: create an uncompressed repository
		CompositeArtifactRepository compRepo = createRepo(false);

		//Setup create a key
		IArtifactKey key = PublisherHelper.createBinaryArtifactKey("testKeyId", new Version("1.2.3"));

		try {
			compRepo.removeDescriptor(key);
			fail("Should not be able to remove Artifact Descriptor using an Artifact Key");
		} catch (UnsupportedOperationException e) {
			//expected. fall through
		}
	}

	public void testRemoveAll() {
		//Setup: create an uncompressed repository
		CompositeArtifactRepository compRepo = createRepo(false);

		try {
			compRepo.removeAll();
			fail("Should not be able to Remove All");
		} catch (UnsupportedOperationException e) {
			//expected. fall through
		}
	}

	public void testGetProperties() {
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		repositoryFile = new File(getTempFolder(), "CompositeArtifactRepositoryTest");
		IArtifactRepository repo = null;
		try {
			repo = manager.createRepository(repositoryFile.toURI(), "TestRepo", IArtifactRepositoryManager.TYPE_COMPOSITE_REPOSITORY, null);
		} catch (ProvisionException e) {
			fail("Cannot create repository: ", e);
		}
		Map properties = repo.getProperties();
		//attempting to modify the properties should fail
		try {
			properties.put(TEST_KEY, TEST_VALUE);
			fail("Should not allow setting property");
		} catch (RuntimeException e) {
			//expected
		}
	}

	public void testSetProperty() {
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		repositoryFile = new File(getTempFolder(), "CompositeArtifactRepositoryTest");
		IArtifactRepository repo = null;
		try {
			repo = manager.createRepository(repositoryFile.toURI(), "TestRepo", IArtifactRepositoryManager.TYPE_COMPOSITE_REPOSITORY, null);
		} catch (ProvisionException e) {
			fail("Cannot create repository: ", e);;
		}
		Map properties = repo.getProperties();
		assertTrue("1.0", !properties.containsKey(TEST_KEY));
		repo.setProperty(TEST_KEY, TEST_VALUE);

		//the previously obtained properties should not be affected by subsequent changes
		assertTrue("1.1", !properties.containsKey(TEST_KEY));
		properties = repo.getProperties();
		assertTrue("1.2", properties.containsKey(TEST_KEY));

		//going back to repo manager, should still get the new property
		try {
			repo = manager.loadRepository(repositoryFile.toURI(), null);
		} catch (ProvisionException e) {
			fail("Cannot load repository: ", e);
		}
		properties = repo.getProperties();
		assertTrue("1.3", properties.containsKey(TEST_KEY));

		//setting a null value should remove the key
		repo.setProperty(TEST_KEY, null);
		properties = repo.getProperties();
		assertTrue("1.4", !properties.containsKey(TEST_KEY));
	}

	public void testAddChild() {
		//Setup: create an uncompressed repository
		CompositeArtifactRepository compRepo = createRepo(false);

		assertEquals("Initial Children size", 0, compRepo.getChildren().size());

		File child = getTestData("1", "/testData/mirror/mirrorSourceRepo1 with space");
		compRepo.addChild(child.toURI());
		assertEquals("Children size with 1 child", 1, compRepo.getChildren().size());

		IArtifactRepositoryManager artifactRepositoryManager = getArtifactRepositoryManager();
		IArtifactRepository repo = null;
		try {
			repo = artifactRepositoryManager.loadRepository(child.toURI(), null);
		} catch (ProvisionException e) {
			fail("Unable to load repository for verification", e);
		}

		assertContentEquals("Verifying contents", compRepo, repo);
	}

	public void testRemoveChild() {
		//Setup: create an uncompressed repository
		CompositeArtifactRepository compRepo = createRepo(false);

		assertEquals("Initial Children size", 0, compRepo.getChildren().size());

		//Setup, populate the children
		File child = getTestData("1", "/testData/mirror/mirrorSourceRepo1 with space");
		compRepo.addChild(child.toURI());
		assertEquals("Children size with 1 child", 1, compRepo.getChildren().size());

		compRepo.removeChild(child.toURI());
		assertEquals("Children size after remove", 0, compRepo.getChildren().size());
	}

	public void testAddRepeatChild() {
		//Setup: create an uncompressed repository
		CompositeArtifactRepository compRepo = createRepo(false);

		assertEquals("Initial Children size", 0, compRepo.getChildren().size());

		File child = getTestData("1", "/testData/mirror/mirrorSourceRepo1 with space");
		compRepo.addChild(child.toURI());
		assertEquals("Children size with 1 child", 1, compRepo.getChildren().size());

		//Add the same repo again
		compRepo.addChild(child.toURI());
		//size should not change
		assertEquals("Children size after repeat entry", 1, compRepo.getChildren().size());
	}

	public void testAddMultipleChildren() {
		//Setup: create an uncompressed repository
		CompositeArtifactRepository compRepo = createRepo(false);

		assertEquals("Initial Children size", 0, compRepo.getChildren().size());

		File child1 = getTestData("1", "/testData/mirror/mirrorSourceRepo1 with space");
		compRepo.addChild(child1.toURI());
		assertEquals("Children size with 1 child", 1, compRepo.getChildren().size());

		File child2 = getTestData("2", "/testData/mirror/mirrorSourceRepo2");
		compRepo.addChild(child2.toURI());
		assertEquals("Children size with 2 children", 2, compRepo.getChildren().size());

		IArtifactRepositoryManager artifactRepositoryManager = getArtifactRepositoryManager();
		IArtifactRepository repo1 = null;
		IArtifactRepository repo2 = null;
		try {
			repo1 = artifactRepositoryManager.loadRepository(child1.toURI(), null);
			repo2 = artifactRepositoryManager.loadRepository(child2.toURI(), null);
		} catch (ProvisionException e) {
			fail("Unable to load repositories for verification", e);
		}

		assertContains("Assert child1's content is in composite repo", repo1, compRepo);
		assertContains("Assert child2's content is in composite repo", repo2, compRepo);
		//checks that the destination has the correct number of keys (no extras)
		//FIXME will this work?
		assertEquals("Assert Correct Number of Keys", repo1.getArtifactKeys().length + repo2.getArtifactKeys().length, compRepo.getArtifactKeys().length);
	}

	public void testRemoveNonexistantChild() {
		//Setup: create an uncompressed repository
		CompositeArtifactRepository compRepo = createRepo(false);

		assertEquals("Initial Children size", 0, compRepo.getChildren().size());

		//Setup, populate the children
		File child = getTestData("1", "/testData/mirror/mirrorSourceRepo1 with space");
		compRepo.addChild(child.toURI());
		assertEquals("Children size with 1 child", 1, compRepo.getChildren().size());

		File invalidChild = getTestData("2", "/testData/mirror/mirrorSourceRepo2");
		compRepo.removeChild(invalidChild.toURI());
		//Should not affect the size of children
		assertEquals("Children size after remove", 1, compRepo.getChildren().size());
	}

	public void testRemoveAllChildren() {
		//Setup: create an uncompressed repository
		CompositeArtifactRepository compRepo = createRepo(false);

		assertEquals("Initial Children size", 0, compRepo.getChildren().size());

		File child1 = getTestData("1", "/testData/mirror/mirrorSourceRepo1 with space");
		compRepo.addChild(child1.toURI());
		assertEquals("Children size with 1 child", 1, compRepo.getChildren().size());

		File child2 = getTestData("2", "/testData/mirror/mirrorSourceRepo2");
		compRepo.addChild(child2.toURI());
		assertEquals("Children size with 2 children", 2, compRepo.getChildren().size());

		compRepo.removeAllChildren();
		assertEquals("Children size after removeAllChildren", 0, compRepo.getChildren().size());
	}

	public void testCompressedPersistence() {
		persistenceTest(true);
	}

	public void testUncompressedPersistence() {
		persistenceTest(false);
	}

	private void persistenceTest(boolean compressed) {
		//Setup: create an uncompressed repository
		CompositeArtifactRepository compRepo = createRepo(compressed);

		//Add data. forces write to disk.
		File child1 = getTestData("1", "/testData/mirror/mirrorSourceRepo1 with space");
		compRepo.addChild(child1.toURI());
		File child2 = getTestData("2", "/testData/mirror/mirrorSourceRepo2");
		compRepo.addChild(child2.toURI());
		//Assume success (covered by other tests)

		//Remove repo from memory
		IArtifactRepositoryManager artifactRepositoryManager = getArtifactRepositoryManager();
		artifactRepositoryManager.removeRepository(repositoryURI);
		compRepo = null;

		//load repository off disk
		IArtifactRepository repo = null;
		try {
			repo = artifactRepositoryManager.loadRepository(repositoryURI, null);
		} catch (ProvisionException e) {
			fail("Could not load repository after removal", e);
		}
		assertTrue("loaded repository was of type CompositeArtifactRepository", repo instanceof CompositeArtifactRepository);

		compRepo = (CompositeArtifactRepository) repo;

		IArtifactRepository repo1 = null;
		IArtifactRepository repo2 = null;
		try {
			repo1 = artifactRepositoryManager.loadRepository(child1.toURI(), null);
			repo2 = artifactRepositoryManager.loadRepository(child2.toURI(), null);
		} catch (ProvisionException e) {
			fail("Unable to load repositories for verification", e);
		}

		assertContains("Assert child1's content is in composite repo", repo1, compRepo);
		assertContains("Assert child2's content is in composite repo", repo2, compRepo);
		//checks that the destination has the correct number of keys (no extras)
		//FIXME will this work?
		assertEquals("Assert Correct Number of Keys", repo1.getArtifactKeys().length + repo2.getArtifactKeys().length, compRepo.getArtifactKeys().length);
	}

	private CompositeArtifactRepository createRepo(boolean compressed) {
		IArtifactRepositoryManager artifactRepositoryManager = getArtifactRepositoryManager();
		repositoryFile = new File(getTempFolder(), "CompositeArtifactRepositoryTest");
		delete(repositoryFile);
		repositoryURI = repositoryFile.toURI();
		Map properties = new HashMap();
		properties.put(IRepository.PROP_COMPRESSED, compressed ? "true" : "false");
		IArtifactRepository repo = null;
		try {
			repo = artifactRepositoryManager.createRepository(repositoryURI, "artifact name", IArtifactRepositoryManager.TYPE_COMPOSITE_REPOSITORY, properties);
		} catch (ProvisionException e) {
			fail("Could not create repository");
		}

		//ensure proper type of repository has been created
		if (!(repo instanceof CompositeArtifactRepository))
			fail("Repository is not a CompositeArtifactRepository");

		return (CompositeArtifactRepository) repo;
	}
}
