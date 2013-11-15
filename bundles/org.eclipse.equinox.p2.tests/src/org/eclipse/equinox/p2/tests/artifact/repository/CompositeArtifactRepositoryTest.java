/*******************************************************************************
 *  Copyright (c) 2008, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import static org.eclipse.equinox.p2.tests.publisher.actions.StatusMatchers.errorStatus;
import static org.eclipse.equinox.p2.tests.publisher.actions.StatusMatchers.statusWithMessageWhich;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.*;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.p2.persistence.CompositeRepositoryState;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.comparator.MD5ArtifactComparator;
import org.eclipse.equinox.p2.internal.repository.tools.ArtifactRepositoryValidator;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.*;
import org.eclipse.equinox.p2.repository.artifact.spi.ArtifactDescriptor;
import org.eclipse.equinox.p2.tests.*;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

public class CompositeArtifactRepositoryTest extends AbstractProvisioningTest {
	private static final String TEST_KEY = "TestKey";
	private static final String TEST_VALUE = "TestValue";
	//artifact repository to remove on tear down
	private File repositoryFile = null;
	private URI repositoryURI = null;

	private int childCount = 0;

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
		IArtifactKey key = PublisherHelper.createBinaryArtifactKey("testKeyId", Version.create("1.2.3"));
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
		IArtifactKey key = PublisherHelper.createBinaryArtifactKey("testKeyId", Version.create("1.2.3"));
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
		IArtifactKey key = PublisherHelper.createBinaryArtifactKey("testKeyId", Version.create("1.2.3"));
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
		IArtifactKey key = PublisherHelper.createBinaryArtifactKey("testKeyId", Version.create("1.2.3"));

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
			fail("Cannot create repository: ", e);
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
		assertEquals("Assert Correct Number of Keys", getArtifactKeyCount(repo1) + getArtifactKeyCount(repo2), getArtifactKeyCount(compRepo));
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
		IQueryResult keys = repo.query(ArtifactKeyQuery.ALL_KEYS, null);
		assertTrue("Error retreaiving artifact keys", !keys.isEmpty());

		//test for existing key
		assertTrue("Asserting key is in composite repo", compRepo.contains((IArtifactKey) keys.iterator().next()));

		//Create a new key, not found in the composite repo
		IArtifactKey key = PublisherHelper.createBinaryArtifactKey("testKeyId", Version.create("1.2.3"));
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
		IQueryResult keys = repo.query(ArtifactKeyQuery.ALL_KEYS, null);
		assertTrue("Error retreaiving artifact keys", !keys.isEmpty());
		IArtifactDescriptor[] descriptors = repo.getArtifactDescriptors((IArtifactKey) keys.iterator().next());
		assertTrue("Error retreaiving artifact descriptors", descriptors.length > 0);

		//test for existing descriptor
		assertTrue("Asserting key is in composite repo", compRepo.contains(descriptors[0]));

		//Create a new descriptor, not found in the composite repo
		IArtifactKey key = PublisherHelper.createBinaryArtifactKey("testKeyId", Version.create("1.2.3"));
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

		IQueryResult keys = compRepo.query(ArtifactKeyQuery.ALL_KEYS, null);
		assertTrue("Error retreaiving artifact keys", !keys.isEmpty());
		IArtifactKey key = (IArtifactKey) keys.iterator().next();
		IArtifactDescriptor[] descriptors = compRepo.getArtifactDescriptors(key);
		assertTrue("Error retreaiving artifact descriptors", descriptors.length > 0);

		IArtifactDescriptor newDescriptor = new ArtifactDescriptor(key);
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
		assertTrue("Expected Key is not in destination", destinationRepo.contains(key));

		IArtifactRepository repo = null;
		try {
			repo = getArtifactRepositoryManager().loadRepository(child.toURI(), null);
		} catch (ProvisionException e) {
			fail("Unable to load repository for verification", e);
		}

		IArtifactDescriptor[] srcDescriptors = repo.getArtifactDescriptors(key);
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
		IQueryResult keys1 = repo1.query(ArtifactKeyQuery.ALL_KEYS, null);
		IArtifactKey key1 = (IArtifactKey) keys1.iterator().next();
		assertTrue("Error retreaiving artifact keys", !keys1.isEmpty());
		IArtifactDescriptor[] descriptors1 = repo1.getArtifactDescriptors(key1);
		assertTrue("Error retreaiving artifact descriptors", descriptors1.length > 0);
		assertTrue("Expected key not in composite repository", compRepo.contains(descriptors1[0]));
		IArtifactDescriptor newDescriptor1 = new ArtifactDescriptor(key1);
		Map properties1 = new OrderedProperties();
		properties1.putAll(descriptors1[0].getProperties());
		properties1.remove(IArtifactDescriptor.FORMAT);
		((ArtifactDescriptor) newDescriptor1).addProperties(properties1);
		//		IArtifactRequest request1 = ((ArtifactRepositoryManager) getArtifactRepositoryManager()).createMirrorRequest(keys1[0], destinationRepo, (Properties) newDescriptor1.getProperties(), (Properties) destinationRepo.getProperties());
		IArtifactRequest request1 = ((ArtifactRepositoryManager) getArtifactRepositoryManager()).createMirrorRequest(key1, destinationRepo, null, null);

		//create a request for a descriptor from repo2
		IQueryResult keys2 = repo2.query(ArtifactKeyQuery.ALL_KEYS, null);
		IArtifactKey key2 = (IArtifactKey) keys2.iterator().next();
		assertTrue("Error retreaiving artifact keys", !keys2.isEmpty());
		IArtifactDescriptor[] descriptors2 = repo2.getArtifactDescriptors(key2);
		assertTrue("Error retreaiving artifact descriptors", descriptors2.length > 0);
		assertTrue("Expected key not in composite repository", compRepo.contains(descriptors2[0]));
		IArtifactDescriptor newDescriptor2 = new ArtifactDescriptor(key2);
		Map properties2 = new OrderedProperties();
		properties2.putAll(descriptors2[0].getProperties());
		properties2.remove(IArtifactDescriptor.FORMAT);
		((ArtifactDescriptor) newDescriptor2).addProperties(properties2);
		//		IArtifactRequest request2 = ((ArtifactRepositoryManager) getArtifactRepositoryManager()).createMirrorRequest(keys2[0], destinationRepo, (Properties) newDescriptor2.getProperties(), (Properties) destinationRepo.getProperties());
		IArtifactRequest request2 = ((ArtifactRepositoryManager) getArtifactRepositoryManager()).createMirrorRequest(key2, destinationRepo, null, null);

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
		assertTrue("Expected Key is not in destination", destinationRepo.contains(key1));
		assertTrue("Expected Key is not in destination", destinationRepo.contains(key2));

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

	public void testGetArtifactsWithErrorInChild() throws Exception {
		repositoryURI = getTestData("1", "/testData/artifactRepo/composite/errorInChild").toURI();
		IArtifactRepository repo = getArtifactRepositoryManager().loadRepository(repositoryURI, null);

		IArtifactRequest[] requests = new IArtifactRequest[] {new ArtifactRequest(new ArtifactKey("osgi.bundle", "plugin", Version.parseVersion("1.0.0")), null) {
			@Override
			public void perform(IArtifactRepository sourceRepository, IProgressMonitor monitor) {
				setResult(sourceRepository.getArtifact(sourceRepository.getArtifactDescriptors(getArtifactKey())[0], new ByteArrayOutputStream(), monitor));
			}
		}};

		IStatus status = repo.getArtifacts(requests, null);

		assertThat(status, is(errorStatus()));
		assertThat(status, is(statusWithMessageWhich(containsString("while reading artifacts from child repositories"))));

		// bug 391400: status should point to repository with problem
		String brokenChildURI = repositoryURI.toString() + "child";
		assertThat(Arrays.asList(status.getChildren()), hasItem(statusWithMessageWhich(containsString(brokenChildURI))));
	}

	public void testLoadingRepositoryRemote() {
		File knownGoodRepoLocation = getTestData("0.1", "/testData/artifactRepo/composite/good.remote");

		CompositeArtifactRepository compRepo = null;
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			compRepo = (CompositeArtifactRepository) getArtifactRepositoryManager().loadRepository(knownGoodRepoLocation.toURI(), null);
		} catch (ProvisionException e) {
			fail("0.99", e);
		} finally {
			System.setOut(out);
		}

		List children = compRepo.getChildren();

		try {
			//ensure children are correct
			URI child1 = URIUtil.fromString("http://www.eclipse.org/foo");
			assertTrue("1.0", children.contains(child1));
			URI child2 = URIUtil.fromString("http://www.eclipse.org/bar");
			assertTrue("1.1", children.contains(child2));
			assertEquals("1.2", 2, children.size());
		} catch (URISyntaxException e) {
			fail("1.99", e);
		}

		//ensure correct properties
		assertEquals("2.0", "artifact name", compRepo.getName());
		Map properties = compRepo.getProperties();
		assertEquals("2.1", 3, properties.size());
		String timestamp = (String) properties.get(IRepository.PROP_TIMESTAMP);
		assertNotNull("2.2", timestamp);
		assertEquals("2.3", "1234", timestamp);
		String compressed = (String) properties.get(IRepository.PROP_COMPRESSED);
		assertNotNull("2.4", compressed);
		assertFalse("2.4", Boolean.parseBoolean(compressed));
	}

	public void testLoadingRepositoryLocal() {
		File knownGoodRepoLocation = getTestData("0.1", "/testData/artifactRepo/composite/good.local");

		CompositeArtifactRepository compRepo = null;
		try {
			compRepo = (CompositeArtifactRepository) getArtifactRepositoryManager().loadRepository(knownGoodRepoLocation.toURI(), null);
		} catch (ProvisionException e) {
			fail("0.99", e);
		}

		List children = compRepo.getChildren();

		//ensure children are correct
		assertTrue("1.0", children.contains(URIUtil.append(compRepo.getLocation(), "one")));
		assertTrue("1.1", children.contains(URIUtil.append(compRepo.getLocation(), "two")));
		assertEquals("1.2", 2, children.size());

		//ensure correct properties
		assertEquals("2.0", "artifact name", compRepo.getName());
		Map properties = compRepo.getProperties();
		assertEquals("2.1", 2, properties.size());
		String timestamp = (String) properties.get(IRepository.PROP_TIMESTAMP);
		assertNotNull("2.2", timestamp);
		assertEquals("2.3", "1234", timestamp);
		String compressed = (String) properties.get(IRepository.PROP_COMPRESSED);
		assertNotNull("2.4", compressed);
		assertFalse("2.5", Boolean.parseBoolean(compressed));
	}

	public void testCompressedPersistence() {
		persistenceTest(true);
	}

	public void testUncompressedPersistence() {
		persistenceTest(false);
	}

	public void testSyntaxErrorWhileParsing() {
		File badCompositeArtifacts = getTestData("1", "/testData/artifactRepo/composite/Bad/syntaxError");
		PrintStream err = System.err;
		StringBuffer buffer = new StringBuffer();
		try {
			System.setErr(new PrintStream(new StringBufferStream(buffer)));
			getArtifactRepositoryManager().loadRepository(badCompositeArtifacts.toURI(), null);
			//Error while parsing expected
			fail("Expected ProvisionException has not been thrown");
		} catch (ProvisionException e) {
			assertTrue(buffer.toString().contains("The element type \"children\" must be terminated by the matching end-tag \"</children>\""));
		} finally {
			System.setErr(err);
		}
	}

	public void testMissingRequireattributeWhileParsing() {
		File badCompositeArtifacts = getTestData("1", "/testData/artifactRepo/composite/Bad/missingRequiredAttribute");
		CompositeArtifactRepository compRepo = null;
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			compRepo = (CompositeArtifactRepository) getArtifactRepositoryManager().loadRepository(badCompositeArtifacts.toURI(), null);
		} catch (ProvisionException e) {
			fail("Error loading repository", e);
		} finally {
			System.setOut(out);
		}

		assertEquals("Repository should only have 1 child", 1, compRepo.getChildren().size());
	}

	public void testValidate() throws Exception {
		//Setup create descriptors with different md5 values
		IArtifactKey dupKey = PublisherHelper.createBinaryArtifactKey("testKeyId", Version.create("1.2.3"));
		File artifact1 = getTestData("0.0", "/testData/mirror/mirrorSourceRepo1 with space/artifacts.xml");
		File artifact2 = getTestData("0.0", "/testData/mirror/mirrorSourceRepo2/artifacts.xml");
		IArtifactDescriptor descriptor1 = PublisherHelper.createArtifactDescriptor(dupKey, artifact1);
		IArtifactDescriptor descriptor2 = PublisherHelper.createArtifactDescriptor(dupKey, artifact2);

		assertEquals("Ensuring Descriptors are the same", descriptor1, descriptor2);
		assertNotSame("Ensuring MD5 values are different", descriptor1.getProperty(IArtifactDescriptor.DOWNLOAD_MD5), descriptor2.getProperty(IArtifactDescriptor.DOWNLOAD_MD5));

		//Setup make repositories
		File repo1Location = getTestFolder(getUniqueString());
		File repo2Location = getTestFolder(getUniqueString());
		File compRepoLocation = getTestFolder(getUniqueString());
		IArtifactRepository repo1 = null;
		IArtifactRepository repo2 = null;
		CompositeArtifactRepository compRepo = null;
		try {
			repo1 = getArtifactRepositoryManager().createRepository(repo1Location.toURI(), "Repo 1", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
			repo1.addDescriptor(descriptor1);
			repo2 = getArtifactRepositoryManager().createRepository(repo2Location.toURI(), "Repo 2", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
			repo2.addDescriptor(descriptor2);
			compRepo = (CompositeArtifactRepository) getArtifactRepositoryManager().createRepository(compRepoLocation.toURI(), "Composite Repo", IArtifactRepositoryManager.TYPE_COMPOSITE_REPOSITORY, null);
			getArtifactRepositoryManager().removeRepository(repo1Location.toURI());
			getArtifactRepositoryManager().removeRepository(repo2Location.toURI());
		} catch (ProvisionException e) {
			fail("Error creating repositories", e);
		}

		//Add the conflicting repositories
		compRepo.addChild(repo1Location.toURI());
		compRepo.addChild(repo2Location.toURI());

		//validate using the MD5 Comparator
		ArtifactRepositoryValidator validator = new ArtifactRepositoryValidator(MD5ArtifactComparator.MD5_COMPARATOR_ID);
		assertFalse("Running verify on invalid repository", validator.validateComposite(compRepo).isOK());
	}

	public void testAddChildWithValidate() throws ProvisionException {
		//Setup create descriptors with different md5 values
		IArtifactKey dupKey = PublisherHelper.createBinaryArtifactKey("testKeyId", Version.create("1.2.3"));
		File artifact1 = getTestData("0.0", "/testData/mirror/mirrorSourceRepo1 with space/artifacts.xml");
		File artifact2 = getTestData("0.0", "/testData/mirror/mirrorSourceRepo2/artifacts.xml");
		IArtifactDescriptor descriptor1 = PublisherHelper.createArtifactDescriptor(dupKey, artifact1);
		IArtifactDescriptor descriptor2 = PublisherHelper.createArtifactDescriptor(dupKey, artifact2);

		assertEquals("Ensuring Descriptors are the same", descriptor1, descriptor2);
		assertNotSame("Ensuring MD5 values are different", descriptor1.getProperty(IArtifactDescriptor.DOWNLOAD_MD5), descriptor2.getProperty(IArtifactDescriptor.DOWNLOAD_MD5));

		//Setup make repositories
		File repo1Location = getTestFolder(getUniqueString());
		File repo2Location = getTestFolder(getUniqueString());
		File compRepoLocation = getTestFolder(getUniqueString());
		IArtifactRepository repo1 = null;
		IArtifactRepository repo2 = null;
		CompositeArtifactRepository compRepo = null;
		try {
			repo1 = getArtifactRepositoryManager().createRepository(repo1Location.toURI(), "Repo 1", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
			repo1.addDescriptor(descriptor1);
			repo2 = getArtifactRepositoryManager().createRepository(repo2Location.toURI(), "Repo 2", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
			repo2.addDescriptor(descriptor2);
			getArtifactRepositoryManager().removeRepository(repo1Location.toURI());
			getArtifactRepositoryManager().removeRepository(repo2Location.toURI());
			compRepo = (CompositeArtifactRepository) getArtifactRepositoryManager().createRepository(compRepoLocation.toURI(), "Composite Repo", IArtifactRepositoryManager.TYPE_COMPOSITE_REPOSITORY, null);
		} catch (ProvisionException e) {
			fail("Error creating repositories", e);
		}

		//Add conflicting repositories
		ArtifactRepositoryValidator validator = new ArtifactRepositoryValidator(MD5ArtifactComparator.MD5_COMPARATOR_ID);
		assertTrue(validator.validateComposite(compRepo, repo1).isOK());
		compRepo.addChild(repo1Location.toURI());
		assertFalse(validator.validateComposite(compRepo, repo2).isOK());
	}

	public void testEnabledAndSystemValues() {
		//Setup make repositories
		File repo1Location = getTestFolder(getUniqueString());
		File repo2Location = getTestFolder(getUniqueString());
		File compRepoLocation = getTestFolder(getUniqueString());
		CompositeArtifactRepository compRepo = null;
		try {
			getArtifactRepositoryManager().createRepository(repo1Location.toURI(), "Repo 1", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
			getArtifactRepositoryManager().createRepository(repo2Location.toURI(), "Repo 2", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
			//Only 1 child should be loaded in the manager
			getArtifactRepositoryManager().removeRepository(repo2Location.toURI());
			compRepo = (CompositeArtifactRepository) getArtifactRepositoryManager().createRepository(compRepoLocation.toURI(), "Composite Repo", IArtifactRepositoryManager.TYPE_COMPOSITE_REPOSITORY, null);
		} catch (ProvisionException e) {
			fail("Error creating repositories", e);
		}

		compRepo.addChild(repo1Location.toURI());
		compRepo.addChild(repo2Location.toURI());

		//force composite repository to load all children
		compRepo.getArtifactDescriptors(new ArtifactKey("", "", Version.emptyVersion));

		assertTrue("Ensuring previously loaded repo is enabled", getArtifactRepositoryManager().isEnabled(repo1Location.toURI()));
		String repo1System = getArtifactRepositoryManager().getRepositoryProperty(repo1Location.toURI(), IRepository.PROP_SYSTEM);
		//if repo1System is null we want to fail
		assertFalse("Ensuring previously loaded repo is not system", repo1System != null ? repo1System.equals(Boolean.toString(true)) : true);
		assertFalse("Ensuring not previously loaded repo is not enabled", getArtifactRepositoryManager().isEnabled(repo2Location.toURI()));
		String repo2System = getArtifactRepositoryManager().getRepositoryProperty(repo2Location.toURI(), IRepository.PROP_SYSTEM);
		//if repo2System is null we want to fail
		assertTrue("Ensuring not previously loaded repo is system", repo2System != null ? repo2System.equals(Boolean.toString(true)) : false);
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
		assertEquals("Assert Correct Number of Keys", getArtifactKeyCount(repo1) + getArtifactKeyCount(repo2), getArtifactKeyCount(compRepo));
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

	/*
	 * Ensure that we can create a non-local composite repository.
	 */
	public void testNonLocalRepo() {
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			URI location = new URI("memory:/in/memory");
			URI childOne = new URI("memory:/in/memory/one");
			URI childTwo = new URI("memory:/in/memory/two");
			URI childThree = new URI("memory:/in/memory/three");
			CompositeArtifactRepository repository = createRepository(location, "in memory test");
			repository.addChild(childOne);
			repository.addChild(childTwo);
			repository.addChild(childThree);
			assertEquals("1.0", 3, repository.getChildren().size());
			repository.removeChild(childTwo);
			assertEquals("1.1", 2, repository.getChildren().size());
			// add a child which already exists... should do nothing
			repository.addChild(childOne);
			assertEquals("1.2", 2, repository.getChildren().size());
			// add the same child but with a relative URI. again it should do nothing
			repository.addChild(new URI("one"));
			assertEquals("1.3", 2, repository.getChildren().size());
		} catch (URISyntaxException e) {
			fail("99.0", e);
		} finally {
			System.setOut(out);
		}
	}

	public void testRelativeChildren() {
		// setup
		File one = getTestData("0.0", "testData/testRepos/simple.1");
		File two = getTestData("0.1", "testData/testRepos/simple.2");
		File temp = getTempFolder();
		copy("0.2", one, new File(temp, "one"));
		copy("0.3", two, new File(temp, "two"));

		// create the composite repository and add the children
		URI location = new File(temp, "comp").toURI();
		CompositeArtifactRepository repository = createRepository(location, "test");
		try {
			repository.addChild(new URI("../one"));
			repository.addChild(new URI("../two"));
		} catch (URISyntaxException e) {
			fail("1.99", e);
		}

		// query the number of artifacts
		List children = repository.getChildren();
		assertEquals("2.0", 2, children.size());
		assertEquals("2.1", 2, getArtifactKeyCount(repository));

		// ensure the child URIs are stored as relative
		CompositeRepositoryState state = repository.toState();
		URI[] childURIs = state.getChildren();
		assertNotNull("3.0", childURIs);
		assertEquals("3.1", 2, childURIs.length);
		assertFalse("3.2", childURIs[0].isAbsolute());
		assertFalse("3.3", childURIs[1].isAbsolute());

		// cleanup
		delete(temp);
	}

	public void testRelativeRemoveChild() {
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			URI location = new URI("memory:/in/memory");
			URI one = new URI("one");
			URI two = new URI("two");
			CompositeArtifactRepository repository = createRepository(location, "in memory test");
			repository.addChild(one);
			repository.addChild(two);
			List children = repository.getChildren();
			assertEquals("1.0", 2, children.size());
			// remove an absolute URI (child one should be first since order is important)
			repository.removeChild((URI) children.iterator().next());
			assertEquals("1.1", 1, repository.getChildren().size());
			// remove a relative URI (child two)
			repository.removeChild(two);
			assertEquals("1.2", 0, repository.getChildren().size());
		} catch (URISyntaxException e) {
			fail("99.0", e);
		} finally {
			System.setOut(out);
		}
	}

	/*
	 * Test a retry request by the composite repository
	 */
	public void testRetryRequest() {
		URI childLocation = getTestData("Loading test data", "testData/artifactRepo/missingArtifact").toURI();
		File destination = null;
		OutputStream out = null;
		try {
			destination = new File(getTempFolder(), getUniqueString());
			out = new FileOutputStream(destination);

			CompositeArtifactRepository repository = createRepository(new URI("memory:/in/memory"), "in memory test");

			IArtifactRepository childOne = getArtifactRepositoryManager().loadRepository(childLocation, null);
			TestArtifactRepository childTwo = new TestArtifactRepository(getAgent(), new URI("memory:/in/memory/two"));
			// Add to repo manager
			assertTrue(childTwo.addToRepositoryManager());

			repository.addChild(childOne.getLocation());
			repository.addChild(childTwo.getLocation());

			IArtifactDescriptor descriptor = new ArtifactDescriptor(new ArtifactKey("osgi.bundle", "missingSize.asdf", Version.create("1.5.1.v200803061910")));

			IStatus status = repository.getArtifact(descriptor, out, new NullProgressMonitor());
			// We should have a failure
			assertFalse(status.isOK());
			// Failure should tell us to retry
			assertEquals(IArtifactRepository.CODE_RETRY, status.getCode());

			status = repository.getArtifact(descriptor, out, new NullProgressMonitor());
			assertFalse(status.isOK());
			assertFalse("Requesting retry with no available children", IArtifactRepository.CODE_RETRY == status.getCode());
		} catch (Exception e) {
			fail("Exception occurred", e);
		} finally {
			getArtifactRepositoryManager().removeRepository(childLocation);
			if (out != null)
				try {
					out.close();
				} catch (IOException e) {
					// Don't care
				}
			if (destination != null)
				delete(destination.getParentFile());
		}
	}

	/*
	 * Test a retry request by a child composite repository 
	 */
	public void testChildRetryRequest() {
		class BadMirrorSite extends TestArtifactRepository {
			int downloadAttempts = 0;

			public BadMirrorSite(URI location) {
				super(getAgent(), location);
				addToRepositoryManager();
			}

			public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream out, IProgressMonitor monitor) {
				if (++downloadAttempts == 1)
					return new MultiStatus(Activator.ID, CODE_RETRY, new IStatus[] {new Status(IStatus.ERROR, "Test", "Test - Download interrupted")}, "Retry another mirror", null);
				return Status.OK_STATUS;
			}

			public boolean contains(IArtifactDescriptor desc) {
				return true;
			}

			public boolean contains(IArtifactKey desc) {
				return true;
			}
		}

		IArtifactRepository destination = null;
		BadMirrorSite child = null;
		CompositeArtifactRepository source = null;
		IArtifactDescriptor descriptor = new ArtifactDescriptor(new ArtifactKey("osgi.bundle", "missingSize.asdf", Version.create("1.5.1.v200803061910")));
		try {
			destination = super.createArtifactRepository(getTempFolder().toURI(), null);
			child = new BadMirrorSite(new URI("memory:/in/memory/child"));
			source = createRepository(new URI("memory:/in/memory/source"), "in memory test");
			source.addChild(child.getLocation());

			// Create mirror request
			MirrorRequest request = new MirrorRequest(descriptor.getArtifactKey(), destination, null, null, (Transport) getAgent().getService(Transport.SERVICE_NAME));
			request.perform(source, new NullProgressMonitor());
			IStatus status = request.getResult();
			// The download should have completed 'successfully'
			assertTrue(status.isOK());
			// There should have been two download attempts at the child
			assertEquals(2, child.downloadAttempts);
		} catch (Exception e) {
			fail("Exception", e);
		} finally {
			if (source != null)
				getArtifactRepositoryManager().removeRepository(source.getLocation());
			if (child != null)
				getArtifactRepositoryManager().removeRepository(child.getLocation());
			if (destination != null) {
				getArtifactRepositoryManager().removeRepository(destination.getLocation());
				delete(new File(destination.getLocation()));
			}
		}
	}

	protected CompositeArtifactRepository createRepository(URI location, String name) {
		CompositeArtifactRepositoryFactory factory = new CompositeArtifactRepositoryFactory();
		factory.setAgent(getAgent());
		return (CompositeArtifactRepository) factory.create(location, name, CompositeArtifactRepository.REPOSITORY_TYPE, null);
	}

	/*
	 * Test a child returning different bytes
	 */
	public void testFailedDownload() {
		final byte[] contents = "Hello".getBytes();
		class BadSite extends TestArtifactRepository {
			File location = new File(getTempFolder(), getUniqueString());

			public BadSite(URI location) {
				super(getAgent(), location);
			}

			public IStatus getArtifact(IArtifactDescriptor descriptor, OutputStream out, IProgressMonitor monitor) {
				super.getArtifact(descriptor, out, monitor);
				return new Status(IStatus.ERROR, "Test", "Test - Download interrupted");
			}

			public void addDescriptor(IArtifactDescriptor descriptor, IProgressMonitor monitor) {
				super.addDescriptor(descriptor, monitor);
				super.addArtifact(descriptor.getArtifactKey(), contents);
			}

			public OutputStream getOutputStream(IArtifactDescriptor descriptor) {
				try {
					return new FileOutputStream(location);
				} catch (Exception e) {
					fail("Failed to open stream", e);
					return null;
				}
			}
		}
		BadSite childOne = null;
		BadSite dest = null;
		CompositeArtifactRepository source = null;
		File destination = null;
		OutputStream out = null;
		try {
			destination = new File(getTempFolder(), getUniqueString());
			out = new FileOutputStream(destination);

			source = createRepository(new URI("memory:/in/memory"), "in memory test");
			IArtifactDescriptor descriptor = new ArtifactDescriptor(new ArtifactKey("osgi.bundle", "missingSize.asdf", Version.create("1.5.1.v200803061910")));

			// Create 'bad' child which returns an error in transfer
			childOne = new BadSite(new URI("memory:/in/memory/one"));
			childOne.addDescriptor(descriptor);
			childOne.addToRepositoryManager();
			source.addChild(childOne.getLocation());

			// Create 'good' child which downloads successfully
			TestArtifactRepository childTwo = new TestArtifactRepository(getAgent(), new URI("memory:/in/memory/two"));
			childTwo.addDescriptor(descriptor);
			childTwo.addArtifact(descriptor.getArtifactKey(), contents);
			childTwo.addToRepositoryManager();
			source.addChild(childTwo.getLocation());

			// Destination repository
			dest = new BadSite(new URI("memory:/in/memory/dest"));

			// Create mirror request
			MirrorRequest request = new MirrorRequest(descriptor.getArtifactKey(), dest, null, null, getTransport());
			request.perform(source, new NullProgressMonitor());
			IStatus status = request.getResult();

			// We should have OK status
			assertTrue(status.isOK());
			// Contents should be equal
			assertEquals(contents.length, dest.location.length());
		} catch (Exception e) {
			fail("Exception occurred", e);
		} finally {
			if (out != null)
				try {
					out.close();
				} catch (IOException e) {
					// Don't care
				}
			if (source != null)
				getArtifactRepositoryManager().removeRepository(source.getLocation());
			if (childOne != null)
				getArtifactRepositoryManager().removeRepository(childOne.getLocation());
			if (dest != null) {
				getArtifactRepositoryManager().removeRepository(dest.getLocation());
				delete(dest.location.getParentFile());
			}
			if (destination != null)
				delete(destination.getParentFile());
		}
	}

	/*
	 * Verify behaviour of contains(IArtifactDescriptor) when a child is marked bad
	 */
	public void testContainsDescriptorBadChild() {
		CompositeArtifactRepository source = null;
		IArtifactRepository childOne = null;
		IArtifactRepository childTwo = null;
		try {
			IArtifactDescriptor desc = new ArtifactDescriptor(new ArtifactKey("osgi", "a", Version.create("1.0.0")));
			source = createRepository(new URI("memory:/in/memory"), "in memory test");
			childOne = createChild();
			source.addChild(childOne.getLocation());

			// Should always contain
			assertTrue("TestSetup failed", source.contains(desc));
			markBad(source, childOne);
			// Should not contain the descriptor of a bad child
			assertFalse("Composite repo contains descriptor despite child marked bad", source.contains(desc));

			// Add a child containing the descriptor
			childTwo = createChild();
			source.addChild(childTwo.getLocation());
			// Should contain the descriptor as the 'good' child has it.
			assertTrue("Composite repo should contain the descriptor", source.contains(desc));
		} catch (Exception e) {
			fail(e.getMessage(), e);
		} finally {
			if (source != null)
				getArtifactRepositoryManager().removeRepository(source.getLocation());
			if (childOne != null)
				getArtifactRepositoryManager().removeRepository(childOne.getLocation());
			if (childTwo != null)
				getArtifactRepositoryManager().removeRepository(childTwo.getLocation());
		}
	}

	/*
	 * Verify behaviour of contains(IArtifactKey) when a child is marked bad
	 */
	public void testContainsKeyBadChild() {
		CompositeArtifactRepository source = null;
		IArtifactRepository childOne = null;
		IArtifactRepository childTwo = null;
		try {
			IArtifactKey desc = new ArtifactKey("osgi", "a", Version.create("1.0.0"));
			source = createRepository(new URI("memory:/in/memory"), "in memory test");
			childOne = createChild();
			source.addChild(childOne.getLocation());

			// Should always contain
			assertTrue("TestSetup failed", source.contains(desc));
			markBad(source, childOne);
			// Should not contain the descriptor of a bad child
			assertFalse("Composite repo contains descriptor despite child marked bad", source.contains(desc));

			// Add a child containing the descriptor
			childTwo = createChild();
			source.addChild(childTwo.getLocation());
			// Should contain the descriptor as the 'good' child has it.
			assertTrue("Composite repo should contain the descriptor", source.contains(desc));
		} catch (Exception e) {
			fail(e.getMessage(), e);
		} finally {
			if (source != null)
				getArtifactRepositoryManager().removeRepository(source.getLocation());
			if (childOne != null)
				getArtifactRepositoryManager().removeRepository(childOne.getLocation());
			if (childTwo != null)
				getArtifactRepositoryManager().removeRepository(childTwo.getLocation());
		}
	}

	/*
	 * Verify the behaviour of getAritfactKeys() when a child is marked bad
	 */
	public void testGetArtifactKeysBadChild() {
		CompositeArtifactRepository source = null;
		IArtifactRepository childOne = null;
		IArtifactRepository childTwo = null;

		try {
			source = createRepository(new URI("memory:/in/memory"), "in memory test");
			IArtifactKey key = new ArtifactKey("classifier", "name", Version.create("1.0.0"));

			childOne = createChild();
			((TestArtifactRepository) childOne).addArtifact(key, new byte[] {});
			source.addChild(childOne.getLocation());

			assertTrue("Composite repo does not contain key", source.contains(key));
			markBad(source, childOne);
			assertFalse("Composite repo contains key but child is marked bad", source.contains(key));

			childTwo = createChild();
			((TestArtifactRepository) childTwo).addArtifact(key, new byte[] {});
			source.addChild(childTwo.getLocation());

			assertTrue("Composite repo does not contain key, but it is available", source.contains(key));
		} catch (Exception e) {
			fail(e.getMessage(), e);
		} finally {
			if (source != null)
				getArtifactRepositoryManager().removeRepository(source.getLocation());
			if (childOne != null)
				getArtifactRepositoryManager().removeRepository(childOne.getLocation());
			if (childTwo != null)
				getArtifactRepositoryManager().removeRepository(childTwo.getLocation());
		}
	}

	/*
	 * Verify the behaviour of getArtifactDescriptors(IArtifactKey) when a child is marked bad
	 */
	public void testGetArtifactDescriptorsBadChild() {
		CompositeArtifactRepository source = null;
		IArtifactRepository childOne = null;
		IArtifactRepository childTwo = null;

		try {
			source = createRepository(new URI("memory:/in/memory"), "in memory test");
			IArtifactKey key = new ArtifactKey("classifier", "name", Version.create("1.0.0"));
			IArtifactDescriptor desc = new ArtifactDescriptor(key);

			childOne = createChild();
			childOne.addDescriptor(desc);
			((TestArtifactRepository) childOne).addArtifact(key, new byte[] {});
			source.addChild(childOne.getLocation());

			assertTrue("Composite repo does not contain descriptor", Arrays.asList(source.getArtifactDescriptors(key)).contains(desc));
			markBad(source, childOne);
			assertFalse("Composite repo contains descriptor but child is marked bad", Arrays.asList(source.getArtifactDescriptors(key)).contains(desc));

			childTwo = createChild();
			childOne.addDescriptor(desc);
			((TestArtifactRepository) childTwo).addArtifact(key, new byte[] {});
			source.addChild(childTwo.getLocation());

			assertTrue("Composite repo does not contain descriptor, but it is available", Arrays.asList(source.getArtifactDescriptors(key)).contains(desc));
		} catch (Exception e) {
			fail(e.getMessage(), e);
		} finally {
			if (source != null)
				getArtifactRepositoryManager().removeRepository(source.getLocation());
			if (childOne != null)
				getArtifactRepositoryManager().removeRepository(childOne.getLocation());
			if (childTwo != null)
				getArtifactRepositoryManager().removeRepository(childTwo.getLocation());
		}
	}

	/*
	 * Mark a child of a Composite repository as bad
	 */
	protected void markBad(CompositeArtifactRepository parent, IArtifactRepository child) {
		try {
			Field field = CompositeArtifactRepository.class.getDeclaredField("loadedRepos");
			field.setAccessible(true);

			Class[] classes = CompositeArtifactRepository.class.getDeclaredClasses();

			Class childInfo = null;
			for (int i = 0; i < classes.length && childInfo == null; i++) {
				if (classes[i].getName().equals("org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository$ChildInfo"))
					childInfo = classes[i];
			}
			assertTrue("Unable to locate inner class ChildInfo", childInfo != null);

			Field repo = childInfo.getDeclaredField("repo");
			repo.setAccessible(true);
			Field good = childInfo.getDeclaredField("good");
			good.setAccessible(true);

			List list = (List) field.get(parent);
			for (Iterator listIter = list.iterator(); listIter.hasNext();) {
				Object obj = listIter.next();
				if (child.equals(repo.get(obj))) {
					good.set(obj, false);
					return;
				}
			}
			fail("Unable to mark as bad:" + child);
		} catch (Exception e) {
			fail("Test setup failed:" + e.getMessage(), e);
		}
	}

	/*
	 * Create a child for a composite repository which always responds true to contains()
	 */
	protected IArtifactRepository createChild() {
		try {
			TestArtifactRepository repo = new TestArtifactRepository(getAgent(), new URI("memory:/in/memory/" + childCount++)) {
				public boolean contains(IArtifactDescriptor desc) {
					return true;
				}

				public boolean contains(IArtifactKey desc) {
					return true;
				}
			};
			repo.addToRepositoryManager();
			return repo;
		} catch (URISyntaxException e) {
			fail("Failed creating child repo", e);
			return null;
		}
	}

	public void testFailingChildFailsCompleteRepository() throws ProvisionException, OperationCanceledException {
		boolean exception = false;
		IArtifactRepository repo = null;
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();

		File repoFile = getTestData("Atomic composite with missing child", "/testData/artifactRepo/composite/missingChild/atomicLoading");
		URI correctChildURI = URIUtil.append(repoFile.toURI(), "one");
		URI repoURI = repoFile.getAbsoluteFile().toURI();

		File alreadyLoadedChildFile = getTestData("Atomic composite with missing child", "/testData/artifactRepo/composite/missingChild/atomicLoading/three");
		IArtifactRepository alreadyLoadedChild = manager.loadRepository(alreadyLoadedChildFile.toURI(), null);
		assertNotNull(alreadyLoadedChild);
		URI previouslyAddedChildURI = URIUtil.append(repoFile.toURI(), "three");

		assertFalse("Child one should not be available in repo manager", manager.contains(correctChildURI));
		try {
			repo = manager.loadRepository(repoFile.toURI(), null);
		} catch (ProvisionException e) {

			assertFalse("Exception message should not contain the location of failing child", e.getMessage().contains(URIUtil.append(repoURI, "two").toString()));
			assertTrue("Exception message should contain the composite repository location " + repoURI + ": " + e.getMessage(), e.getMessage().contains(repoURI.toString()));
			exception = true;
		}
		assertNull(repo);
		assertTrue("an exception should have been reported", exception);
		assertFalse("Successfully loaded child should be removed when composite loading mode is set to atomic", manager.contains(correctChildURI));
		assertTrue("Periously loaded child should remain in repo manager", manager.contains(previouslyAddedChildURI));

	}

	public void testFailingChildLoadsCompleteRepository() {
		boolean exception = false;
		IArtifactRepository repo = null;
		IArtifactRepositoryManager manager = getArtifactRepositoryManager();

		File repoFile = getTestData("Composite with missing child", "/testData/artifactRepo/composite/missingChild/nonAtomicLoading");
		URI correctChildURI = URIUtil.append(repoFile.toURI(), "one");

		assertFalse("Child should not be available in repo manager", manager.contains(correctChildURI));
		try {
			repo = manager.loadRepository(repoFile.toURI(), null);
		} catch (ProvisionException e) {
			exception = true;
		}

		assertNotNull(repo);
		assertFalse("an exception should have been reported", exception);
		assertTrue("Successfully loaded child should be available in repo manager", manager.contains(URIUtil.append(repo.getLocation(), "one")));

	}
}
