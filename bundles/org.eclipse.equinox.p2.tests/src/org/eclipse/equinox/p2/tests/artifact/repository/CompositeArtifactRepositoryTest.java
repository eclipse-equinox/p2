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

import java.io.*;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.artifact.repository.ArtifactRepositoryManager;
import org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
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

	public void testContainsKey() {
		//Setup: create the repository
		CompositeArtifactRepository compRepo = createRepo(false);
		//add the child
		File child = getTestData("1", "/testData/mirror/mirrorSourceRepo1 with space");
		compRepo.addChild(child.toURI());

		IArtifactRepositoryManager artifactRepositoryManager = getArtifactRepositoryManager();
		IArtifactRepository repo = null;
		try {
			repo = artifactRepositoryManager.loadRepository(child.toURI(), null);
		} catch (ProvisionException e) {
			fail("Unable to load repository for verification", e);
		}

		//get the keys
		IArtifactKey[] keys = repo.getArtifactKeys();
		assertTrue("Error retreaiving artifact keys", keys.length > 0);

		//test for existing key
		assertTrue("Asserting key is in composite repo", compRepo.contains(keys[0]));

		//Create a new key, not found in the composite repo
		IArtifactKey key = PublisherHelper.createBinaryArtifactKey("testKeyId", new Version("1.2.3"));
		//test for a non existing key
		assertFalse("Asserting key is not in composite repo", compRepo.contains(key));
	}

	public void testContainsDescriptor() {
		//Setup: create the repository
		CompositeArtifactRepository compRepo = createRepo(false);
		//add the child
		File child = getTestData("1", "/testData/mirror/mirrorSourceRepo1 with space");
		compRepo.addChild(child.toURI());

		IArtifactRepositoryManager artifactRepositoryManager = getArtifactRepositoryManager();
		IArtifactRepository repo = null;
		try {
			repo = artifactRepositoryManager.loadRepository(child.toURI(), null);
		} catch (ProvisionException e) {
			fail("Unable to load repository for verification", e);
		}

		//get the descriptors
		IArtifactKey[] keys = repo.getArtifactKeys();
		assertTrue("Error retreaiving artifact keys", keys.length > 0);
		IArtifactDescriptor[] descriptors = repo.getArtifactDescriptors(keys[0]);
		assertTrue("Error retreaiving artifact descriptors", descriptors.length > 0);

		//test for existing descriptor
		assertTrue("Asserting key is in composite repo", compRepo.contains(descriptors[0]));

		//Create a new descriptor, not found in the composite repo
		IArtifactKey key = PublisherHelper.createBinaryArtifactKey("testKeyId", new Version("1.2.3"));
		IArtifactDescriptor descriptor = PublisherHelper.createArtifactDescriptor(key, null);
		//test for a non existing descriptor
		assertFalse("Asserting key is not in composite repo", compRepo.contains(descriptor));
	}

	public void testGetArtifactFromDescriptor() {
		//Setup: create the repository
		CompositeArtifactRepository compRepo = createRepo(false);
		//add the child
		File child = getTestData("1", "/testData/mirror/mirrorSourceRepo1 with space");
		compRepo.addChild(child.toURI());

		File destRepoLocation = new File(getTempFolder(), "CompositeArtifactRepositoryTest");
		delete(destRepoLocation);
		IArtifactRepository destinationRepo = null;
		try {
			destinationRepo = getArtifactRepositoryManager().createRepository(destRepoLocation.toURI(), "Test Repo", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		} catch (ProvisionException e1) {
			fail("Error creating destination", e1);
		}

		IArtifactKey[] keys = compRepo.getArtifactKeys();
		assertTrue("Error retreaiving artifact keys", keys.length > 0);
		IArtifactDescriptor[] descriptors = compRepo.getArtifactDescriptors(keys[0]);
		assertTrue("Error retreaiving artifact descriptors", descriptors.length > 0);

		IArtifactDescriptor newDescriptor = new ArtifactDescriptor(keys[0]);
		Map properties = new OrderedProperties();
		properties.putAll(descriptors[0].getProperties());
		properties.remove(IArtifactDescriptor.FORMAT);
		((ArtifactDescriptor) newDescriptor).addProperties(properties);
		try {
			OutputStream repositoryStream = null;
			try {
				//System.out.println("Getting Artifact: " + descriptors[0].getArtifactKey() + " (Descriptor: " + descriptors[0] + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				repositoryStream = destinationRepo.getOutputStream(newDescriptor);
				if (repositoryStream == null)
					fail("Error while obtaining OutputStream");
				compRepo.getArtifact(descriptors[0], repositoryStream, new NullProgressMonitor());
			} finally {
				if (repositoryStream != null)
					repositoryStream.close();
			}
		} catch (ProvisionException e) {
			fail("Error while obtaining OutputStream", e);
		} catch (IOException e) {
			fail("Error while downloading artifact", e);
		}
		//corresponding key should now be in the destination
		assertTrue("Expected Key is not in destination", destinationRepo.contains(keys[0]));

		IArtifactRepository repo = null;
		try {
			repo = getArtifactRepositoryManager().loadRepository(child.toURI(), null);
		} catch (ProvisionException e) {
			fail("Unable to load repository for verification", e);
		}

		IArtifactDescriptor[] srcDescriptors = repo.getArtifactDescriptors(keys[0]);
		if (srcDescriptors == null)
			fail("Error finding descriptors for validation");

		boolean found = false;
		for (int j = 0; j < srcDescriptors.length && !found; j++) {
			//Assumes that since the source repo does not have any packed artifacts that the descriptor will not change during transfer
			if (srcDescriptors[j].equals(descriptors[0])) {
				File srcFile = ((SimpleArtifactRepository) repo).getArtifactFile(srcDescriptors[j]);
				File destFile = ((SimpleArtifactRepository) destinationRepo).getArtifactFile(descriptors[0]);
				if (srcFile == null || destFile == null)
					fail("Unable to retreive files from repositories");
				if (!(srcFile.exists() && destFile.exists()))
					fail("File does not exist on disk");
				assertTrue(srcFile.length() == destFile.length());
				found = true;
			}
		}

		if (!found)
			fail("Matching descriptor was nto found in source");
	}

	public void testGetArtifactsFromRequests() {
		//Setup: create the repository
		CompositeArtifactRepository compRepo = createRepo(false);
		//add the child
		File child1 = getTestData("1", "/testData/mirror/mirrorSourceRepo1 with space");
		compRepo.addChild(child1.toURI());
		File child2 = getTestData("2", "/testData/mirror/mirrorSourceRepo2");
		compRepo.addChild(child2.toURI());

		File destRepoLocation = new File(getTempFolder(), "CompositeArtifactRepositoryTest");
		delete(destRepoLocation);
		IArtifactRepository destinationRepo = null;
		IArtifactRepository repo1 = null;
		IArtifactRepository repo2 = null;
		try {
			repo1 = getArtifactRepositoryManager().loadRepository(child1.toURI(), null);
			repo2 = getArtifactRepositoryManager().loadRepository(child2.toURI(), null);
			destinationRepo = getArtifactRepositoryManager().createRepository(destRepoLocation.toURI(), "Test Repo", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		} catch (ProvisionException e1) {
			fail("Error retreiving repsoitories", e1);
		}

		//create a request for a descriptor from repo1
		IArtifactKey[] keys1 = repo1.getArtifactKeys();
		assertTrue("Error retreaiving artifact keys", keys1.length > 0);
		IArtifactDescriptor[] descriptors1 = repo1.getArtifactDescriptors(keys1[0]);
		assertTrue("Error retreaiving artifact descriptors", descriptors1.length > 0);
		assertTrue("Expected key not in composite repository", compRepo.contains(descriptors1[0]));
		IArtifactDescriptor newDescriptor1 = new ArtifactDescriptor(keys1[0]);
		Map properties1 = new OrderedProperties();
		properties1.putAll(descriptors1[0].getProperties());
		properties1.remove(IArtifactDescriptor.FORMAT);
		((ArtifactDescriptor) newDescriptor1).addProperties(properties1);
		//		IArtifactRequest request1 = ((ArtifactRepositoryManager) getArtifactRepositoryManager()).createMirrorRequest(keys1[0], destinationRepo, (Properties) newDescriptor1.getProperties(), (Properties) destinationRepo.getProperties());
		IArtifactRequest request1 = ((ArtifactRepositoryManager) getArtifactRepositoryManager()).createMirrorRequest(keys1[0], destinationRepo, null, null);

		//create a request for a descriptor from repo2
		IArtifactKey[] keys2 = repo2.getArtifactKeys();
		assertTrue("Error retreaiving artifact keys", keys2.length > 0);
		IArtifactDescriptor[] descriptors2 = repo2.getArtifactDescriptors(keys2[0]);
		assertTrue("Error retreaiving artifact descriptors", descriptors2.length > 0);
		assertTrue("Expected key not in composite repository", compRepo.contains(descriptors2[0]));
		IArtifactDescriptor newDescriptor2 = new ArtifactDescriptor(keys2[0]);
		Map properties2 = new OrderedProperties();
		properties2.putAll(descriptors2[0].getProperties());
		properties2.remove(IArtifactDescriptor.FORMAT);
		((ArtifactDescriptor) newDescriptor2).addProperties(properties2);
		//		IArtifactRequest request2 = ((ArtifactRepositoryManager) getArtifactRepositoryManager()).createMirrorRequest(keys2[0], destinationRepo, (Properties) newDescriptor2.getProperties(), (Properties) destinationRepo.getProperties());
		IArtifactRequest request2 = ((ArtifactRepositoryManager) getArtifactRepositoryManager()).createMirrorRequest(keys2[0], destinationRepo, null, null);

		IArtifactRequest[] requests = new IArtifactRequest[2];
		requests[0] = request1;
		requests[1] = request2;

		try {
			OutputStream repositoryStream = null;
			try {
				compRepo.getArtifacts(requests, new NullProgressMonitor());
			} finally {
				if (repositoryStream != null)
					repositoryStream.close();
			}
		} catch (IOException e) {
			fail("Error while downloading artifacts", e);
		}
		//corresponding keys should now be in the destination
		assertTrue("Expected Key is not in destination", destinationRepo.contains(keys1[0]));
		assertTrue("Expected Key is not in destination", destinationRepo.contains(keys2[0]));

		//verify the file from repo1
		File repo1File = ((SimpleArtifactRepository) repo1).getArtifactFile(descriptors1[0]);
		File destFile1 = ((SimpleArtifactRepository) destinationRepo).getArtifactFile(descriptors1[0]);
		if (repo1File == null || destFile1 == null)
			fail("Unable to retreive files from repositories");
		if (!(repo1File.exists() && destFile1.exists()))
			fail("File does not exist on disk");
		assertTrue(repo1File.length() == destFile1.length());

		//verify the file from repo2
		File repo2File = ((SimpleArtifactRepository) repo2).getArtifactFile(descriptors2[0]);
		File destFile2 = ((SimpleArtifactRepository) destinationRepo).getArtifactFile(descriptors2[0]);
		if (repo2File == null || destFile2 == null)
			fail("Unable to retreive files from repositories");
		if (!(repo2File.exists() && destFile2.exists()))
			fail("File does not exist on disk");
		assertTrue(repo2File.length() == destFile2.length());
	}

	public void testCompressedPersistence() {
		persistenceTest(true);
	}

	public void testUncompressedPersistence() {
		persistenceTest(false);
	}

	public void testSyntaxErrorWhileParsing() {
		File badCompositeArtifacts = getTestData("1", "/testData/artifactRepo/composite/Bad/syntaxError");

		try {
			getArtifactRepositoryManager().loadRepository(badCompositeArtifacts.toURI(), null);
			//Error while parsing expected
			fail("Expected ProvisionException has not been thrown");
		} catch (ProvisionException e) {
			//expected.
			//TODO more meaningful verification?
		}
	}

	public void testMissingRequireattributeWhileParsing() {
		File badCompositeArtifacts = getTestData("1", "/testData/artifactRepo/composite/Bad/missingRequiredAttribute");
		CompositeArtifactRepository compRepo = null;
		try {
			compRepo = (CompositeArtifactRepository) getArtifactRepositoryManager().loadRepository(badCompositeArtifacts.toURI(), null);
		} catch (ProvisionException e) {
			fail("Error loading repository", e);
		}
		assertEquals("Repository should only have 1 child", 1, compRepo.getChildren().size());
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
