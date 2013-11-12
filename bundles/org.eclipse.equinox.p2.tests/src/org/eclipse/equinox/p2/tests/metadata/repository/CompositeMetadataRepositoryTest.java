/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 - ongoing development
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata.repository;

import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepositoryFactory;
import org.eclipse.equinox.internal.p2.persistence.CompositeRepositoryState;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.*;
import org.eclipse.equinox.p2.tests.core.CompoundQueryableTest.CompoundQueryTestProgressMonitor;

/**
 * Test API of the local metadata repository implementation.
 */
public class CompositeMetadataRepositoryTest extends AbstractProvisioningTest {
	private static final String TEST_KEY = "TestKey";
	private static final String TEST_VALUE = "TestValue";
	protected File repoLocation;

	protected void setUp() throws Exception {
		super.setUp();
		repoLocation = new File(getTempFolder(), "CompositeMetadataRepositoryTest");
		AbstractProvisioningTest.delete(repoLocation);
	}

	protected void tearDown() throws Exception {
		getMetadataRepositoryManager().removeRepository(repoLocation.toURI());
		delete(repoLocation);
		super.tearDown();
	}

	public void testCompressedRepositoryCreation() {
		createRepo(true);

		File[] files = repoLocation.listFiles();
		boolean jarFilePresent = false;
		boolean xmlFilePresent = false;
		// one of the files in the repository should be the content.xml.jar
		for (int i = 0; i < files.length; i++) {
			if ("compositeContent.jar".equalsIgnoreCase(files[i].getName())) {
				jarFilePresent = true;
			}
			if ("compositeContent.xml".equalsIgnoreCase(files[i].getName())) {
				xmlFilePresent = true;
			}
		}
		if (!jarFilePresent) {
			fail("Repository did not create JAR for compositeContent.xml");
		}
		if (xmlFilePresent) {
			fail("Repository should not create compositeContent.xml");
		}
	}

	public void testUncompressedRepositoryCreation() {
		createRepo(false);

		File[] files = repoLocation.listFiles();
		boolean jarFilePresent = false;
		// none of the files in the repository should be the content.xml.jar
		for (int i = 0; i < files.length; i++) {
			if ("compositeContent.jar".equalsIgnoreCase(files[i].getName())) {
				jarFilePresent = true;
			}
		}
		if (jarFilePresent) {
			fail("Repository should not create JAR for compositeContent.xml");
		}
	}

	public void testAddInstallableUnits() {
		//create uncommpressed repo
		CompositeMetadataRepository compRepo = createRepo(false);

		//Try to add a new InstallableUnit.
		try {
			InstallableUnitDescription descriptor = new MetadataFactory.InstallableUnitDescription();
			descriptor.setId("testIuId");
			descriptor.setVersion(Version.create("3.2.1"));
			IInstallableUnit iu = MetadataFactory.createInstallableUnit(descriptor);
			compRepo.addInstallableUnits(Arrays.asList(iu));
			fail("Should not be able to insert InstallableUnit");
		} catch (UnsupportedOperationException e) {
			//expected. fall through
		}
	}

	public void testRemoveInstallableUnits() {
		//create uncommpressed repo
		CompositeMetadataRepository compRepo = createRepo(false);

		//Try to remove an InstallableUnit.
		try {
			IQueryResult queryResult = compRepo.query(QueryUtil.createIUAnyQuery(), null);
			compRepo.removeInstallableUnits(queryResult.toSet());
			fail("Should not be able to remove InstallableUnit");
		} catch (UnsupportedOperationException e) {
			//expected. fall through
		}
	}

	public void testRemoveAll() {
		//create uncommpressed repo
		CompositeMetadataRepository compRepo = createRepo(false);

		//Try to removeAll.
		try {
			compRepo.removeAll();
			fail("Should not be able to removeAll()");
		} catch (UnsupportedOperationException e) {
			//expected. fall through
		}
	}

	public void testGetProperties() {
		IMetadataRepositoryManager manager = getMetadataRepositoryManager();
		IMetadataRepository repo = null;
		try {
			repo = manager.createRepository(repoLocation.toURI(), "TestRepo", IMetadataRepositoryManager.TYPE_COMPOSITE_REPOSITORY, null);
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
		IMetadataRepositoryManager manager = getMetadataRepositoryManager();
		IMetadataRepository repo = null;
		try {
			repo = manager.createRepository(repoLocation.toURI(), "TestRepo", IMetadataRepositoryManager.TYPE_COMPOSITE_REPOSITORY, null);
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
			repo = manager.loadRepository(repoLocation.toURI(), null);
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
		CompositeMetadataRepository compRepo = createRepo(false);

		assertEquals("Initial Children size", 0, compRepo.getChildren().size());

		File child = getTestData("1", "/testData/mirror/mirrorSourceRepo1 with space");
		compRepo.addChild(child.toURI());
		assertEquals("Children size with 1 child", 1, compRepo.getChildren().size());

		IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
		IMetadataRepository repo = null;
		try {
			repo = metadataRepositoryManager.loadRepository(child.toURI(), null);
		} catch (ProvisionException e) {
			fail("Unable to load repository for verification", e);
		}

		assertContentEquals("Verifying contents", compRepo, repo);
	}

	public void testRemoveChild() {
		//Setup: create an uncompressed repository
		CompositeMetadataRepository compRepo = createRepo(false);

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
		CompositeMetadataRepository compRepo = createRepo(false);

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
		CompositeMetadataRepository compRepo = createRepo(false);

		assertEquals("Initial Children size", 0, compRepo.getChildren().size());

		File child1 = getTestData("1", "/testData/mirror/mirrorSourceRepo1 with space");
		compRepo.addChild(child1.toURI());
		assertEquals("Children size with 1 child", 1, compRepo.getChildren().size());

		File child2 = getTestData("2", "/testData/mirror/mirrorSourceRepo2");
		compRepo.addChild(child2.toURI());
		assertEquals("Children size with 2 children", 2, compRepo.getChildren().size());

		IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
		IMetadataRepository repo1 = null;
		IMetadataRepository repo2 = null;
		try {
			repo1 = metadataRepositoryManager.loadRepository(child1.toURI(), null);
			repo2 = metadataRepositoryManager.loadRepository(child2.toURI(), null);
		} catch (ProvisionException e) {
			fail("Unable to load repositories for verification", e);
		}

		assertContains("Assert child1's content is in composite repo", repo1, compRepo);
		assertContains("Assert child2's content is in composite repo", repo2, compRepo);
		//checks that the destination has the correct number of keys (no extras)
		assertEquals("Assert correct number of IUs", getNumUnique(repo1.query(QueryUtil.createIUAnyQuery(), null), repo2.query(QueryUtil.createIUAnyQuery(), null)), compRepo.query(QueryUtil.createIUAnyQuery(), null).toUnmodifiableSet().size());
	}

	public void testRemoveNonexistantChild() {
		//Setup: create an uncompressed repository
		CompositeMetadataRepository compRepo = createRepo(false);

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
		CompositeMetadataRepository compRepo = createRepo(false);

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

	public void testLoadingRepositoryRemote() {
		File knownGoodRepoLocation = getTestData("0.1", "/testData/metadataRepo/composite/good.remote");

		CompositeMetadataRepository compRepo = null;

		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			compRepo = (CompositeMetadataRepository) getMetadataRepositoryManager().loadRepository(knownGoodRepoLocation.toURI(), null);
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
		assertEquals("2.0", "metadata name", compRepo.getName());
		Map properties = compRepo.getProperties();
		assertEquals("2.1", 3, properties.size());
		String timestamp = (String) properties.get(IRepository.PROP_TIMESTAMP);
		assertNotNull("2.2", timestamp);
		assertEquals("2.3", "1234", timestamp);
		String compressed = (String) properties.get(IRepository.PROP_COMPRESSED);
		assertNotNull("2.4", compressed);
		assertFalse("2.5", Boolean.parseBoolean(compressed));
	}

	public void testLoadingRepositoryLocal() {
		File testData = getTestData("0.5", "/testData/metadataRepo/composite/good.local");
		copy("0.6", testData, repoLocation);

		CompositeMetadataRepository compRepo = null;
		try {
			compRepo = (CompositeMetadataRepository) getMetadataRepositoryManager().loadRepository(repoLocation.toURI(), null);
		} catch (ProvisionException e) {
			fail("0.9", e);
		}

		List children = compRepo.getChildren();

		//ensure children are correct
		URI child1 = URIUtil.append(compRepo.getLocation(), "one");
		assertTrue("1.0", children.contains(child1));
		URI child2 = URIUtil.append(compRepo.getLocation(), "two");
		assertTrue("1.1", children.contains(child2));
		assertEquals("1.2", 2, children.size());

		//ensure correct properties
		assertEquals("2.0", "metadata name", compRepo.getName());
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
		File badCompositeContent = getTestData("1", "/testData/metadataRepo/composite/Bad/syntaxError");

		StringBuffer buffer = new StringBuffer();
		PrintStream err = System.err;
		try {
			System.setErr(new PrintStream(new StringBufferStream(buffer)));
			getMetadataRepositoryManager().loadRepository(badCompositeContent.toURI(), null);
			//Error while parsing expected
			fail("Expected ProvisionException has not been thrown");
		} catch (ProvisionException e) {
			assertTrue(buffer.toString().contains("The element type \"children\" must be terminated by the matching end-tag \"</children>\"."));
		} finally {
			System.setErr(err);
		}
	}

	public void testMissingRequireattributeWhileParsing() {
		File badCompositeContent = getTestData("0.1", "/testData/metadataRepo/composite/Bad/missingRequiredAttribute");
		copy("0.2", badCompositeContent, repoLocation);

		CompositeMetadataRepository compRepo = null;
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			compRepo = (CompositeMetadataRepository) getMetadataRepositoryManager().loadRepository(repoLocation.toURI(), null);
		} catch (ProvisionException e) {
			fail("1.99", e);
		} finally {
			System.setOut(out);
		}
		assertEquals("2.0", 1, compRepo.getChildren().size());
	}

	public void testEnabledAndSystemValues() {
		//Setup make repositories
		File repo1Location = getTestFolder(getUniqueString());
		File repo2Location = getTestFolder(getUniqueString());
		File compRepoLocation = getTestFolder(getUniqueString());
		CompositeMetadataRepository compRepo = null;
		try {
			getMetadataRepositoryManager().createRepository(repo1Location.toURI(), "Repo 1", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
			getMetadataRepositoryManager().createRepository(repo2Location.toURI(), "Repo 2", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
			//Only 1 child should be loaded in the manager
			getMetadataRepositoryManager().removeRepository(repo2Location.toURI());
			compRepo = (CompositeMetadataRepository) getMetadataRepositoryManager().createRepository(compRepoLocation.toURI(), "Composite Repo", IMetadataRepositoryManager.TYPE_COMPOSITE_REPOSITORY, null);
		} catch (ProvisionException e) {
			fail("Error creating repositories", e);
		}

		compRepo.addChild(repo1Location.toURI());
		compRepo.addChild(repo2Location.toURI());

		//force composite repository to load all children
		compRepo.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor());

		assertTrue("Ensuring previously loaded repo is enabled", getMetadataRepositoryManager().isEnabled(repo1Location.toURI()));
		String repo1System = getMetadataRepositoryManager().getRepositoryProperty(repo1Location.toURI(), IRepository.PROP_SYSTEM);
		//if repo1System is null we want to fail
		assertFalse("Ensuring previously loaded repo is not system", repo1System != null ? repo1System.equals(Boolean.toString(true)) : true);
		assertFalse("Ensuring not previously loaded repo is not enabled", getMetadataRepositoryManager().isEnabled(repo2Location.toURI()));
		String repo2System = getMetadataRepositoryManager().getRepositoryProperty(repo2Location.toURI(), IRepository.PROP_SYSTEM);
		//if repo2System is null we want to fail
		assertTrue("Ensuring not previously loaded repo is system", repo2System != null ? repo2System.equals(Boolean.toString(true)) : false);
	}

	public void testGetLatestIU() {
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		URI location1;
		URI location2;
		try {
			location1 = TestData.getFile("metadataRepo", "multipleversions1").toURI();
			location2 = TestData.getFile("metadataRepo", "multipleversions2").toURI();
		} catch (Exception e) {
			fail("0.99", e);
			return;
		}
		CompositeMetadataRepository compositeRepo = createRepo(false);
		compositeRepo.addChild(location1);
		compositeRepo.addChild(location2);
		IQueryResult queryResult = compositeRepo.query(QueryUtil.createLatestIUQuery(), monitor);
		assertEquals("1.0", 1, queryResultSize(queryResult));
		assertEquals("1.1", Version.createOSGi(3, 0, 0), ((IInstallableUnit) queryResult.iterator().next()).getVersion());
		assertTrue("1.2", monitor.isDone());
		assertTrue("1.3", monitor.isWorkDone());
	}

	public void testGetLatestIULessThan3() {
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		URI location1;
		URI location2;
		try {
			location1 = TestData.getFile("metadataRepo", "multipleversions1").toURI();
			location2 = TestData.getFile("metadataRepo", "multipleversions2").toURI();
		} catch (Exception e) {
			fail("0.99", e);
			return;
		}
		CompositeMetadataRepository compositeRepo = createRepo(false);
		compositeRepo.addChild(location1);
		compositeRepo.addChild(location2);
		IQuery cQuery = QueryUtil.createLatestQuery(new MatchQuery() {
			public boolean isMatch(Object candidate) {
				if (candidate instanceof IInstallableUnit) {
					IInstallableUnit iInstallableUnit = (IInstallableUnit) candidate;
					if (iInstallableUnit.getVersion().compareTo(Version.createOSGi(3, 0, 0)) < 0)
						return true;
				}
				return false;
			}
		});
		IQueryResult queryResult = compositeRepo.query(cQuery, monitor);
		assertEquals("1.0", 1, queryResultSize(queryResult));
		assertEquals("1.1", Version.createOSGi(2, 2, 0), ((IInstallableUnit) queryResult.iterator().next()).getVersion());
		assertTrue("1.2", monitor.isDone());
		assertTrue("1.3", monitor.isWorkDone());
	}

	private void persistenceTest(boolean compressed) {
		//Setup: create an uncompressed repository
		CompositeMetadataRepository compRepo = createRepo(compressed);

		//Add data. forces write to disk.
		File child1 = getTestData("1", "/testData/mirror/mirrorSourceRepo1 with space");
		compRepo.addChild(child1.toURI());
		File child2 = getTestData("2", "/testData/mirror/mirrorSourceRepo2");
		compRepo.addChild(child2.toURI());
		//Assume success (covered by other tests)

		//Remove repo from memory
		IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
		metadataRepositoryManager.removeRepository(repoLocation.toURI());
		compRepo = null;

		//load repository off disk
		IMetadataRepository repo = null;
		try {
			repo = metadataRepositoryManager.loadRepository(repoLocation.toURI(), null);
		} catch (ProvisionException e) {
			fail("Could not load repository after removal", e);
		}
		assertTrue("loaded repository was of type CompositeMetadataRepository", repo instanceof CompositeMetadataRepository);

		compRepo = (CompositeMetadataRepository) repo;

		IMetadataRepository repo1 = null;
		IMetadataRepository repo2 = null;
		try {
			repo1 = metadataRepositoryManager.loadRepository(child1.toURI(), null);
			repo2 = metadataRepositoryManager.loadRepository(child2.toURI(), null);
		} catch (ProvisionException e) {
			fail("Unable to load repositories for verification", e);
		}

		assertContains("Assert child1's content is in composite repo", repo1, compRepo);
		assertContains("Assert child2's content is in composite repo", repo2, compRepo);
		//checks that the destination has the correct number of keys (no extras)
		assertEquals("Assert correct number of IUs", getNumUnique(repo1.query(QueryUtil.createIUAnyQuery(), null), repo2.query(QueryUtil.createIUAnyQuery(), null)), compRepo.query(QueryUtil.createIUAnyQuery(), null).toUnmodifiableSet().size());
	}

	private CompositeMetadataRepository createRepo(boolean compressed) {
		IMetadataRepositoryManager metadataRepositoryManager = getMetadataRepositoryManager();
		Map properties = new HashMap();
		properties.put(IRepository.PROP_COMPRESSED, compressed ? "true" : "false");
		IMetadataRepository repo = null;
		try {
			repo = metadataRepositoryManager.createRepository(repoLocation.toURI(), "metadata name", IMetadataRepositoryManager.TYPE_COMPOSITE_REPOSITORY, properties);
		} catch (ProvisionException e) {
			fail("Could not create repository");
		}

		//esnure proper type of repository has been created
		if (!(repo instanceof CompositeMetadataRepository))
			fail("Repository is not a CompositeMetadataRepository");

		return (CompositeMetadataRepository) repo;
	}

	/**
	 * Takes 2 collectors, compares them, and returns the number of unique keys
	 * Needed to verify that only the appropriate number of files have been transfered by the mirror application
	 */
	private int getNumUnique(IQueryResult<IInstallableUnit> c1, IQueryResult<IInstallableUnit> c2) {
		Set<IInstallableUnit> set1 = c1.toUnmodifiableSet();
		Set<IInstallableUnit> set2 = c2.toUnmodifiableSet();

		//initialize to the size of both collectors
		int numKeys = set1.size() + set2.size();

		for (IInstallableUnit iu1 : set1) {
			for (IInstallableUnit iu2 : set2) {
				if (isEqual(iu1, iu2))
					numKeys--;
				//identical keys has bee found, therefore the number of unique keys is one less than previously thought
			}
		}
		return numKeys;
	}

	/*
	 * Ensure that we can create a non-local composite repository.
	 * Note that we had to change this test method when we changed the 
	 * behaviour of the composite repos to aggressively load the children.
	 */
	public void testNonLocalRepo() {
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			URI location = new URI("http://eclipse.org/equinox/in/memory");
			URI childOne = new URI("http://eclipse.org/equinox/in/memory/one");
			URI childTwo = new URI("http://eclipse.org/equinox/in/memory/two");
			URI childThree = new URI("http://eclipse.org/equinox/in/memory/three");
			CompositeMetadataRepository repository = createRepository(location, "in memory test");
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

	protected CompositeMetadataRepository createRepository(URI location, String name) {
		CompositeMetadataRepositoryFactory factory = new CompositeMetadataRepositoryFactory();
		factory.setAgent(getAgent());
		return (CompositeMetadataRepository) factory.create(location, name, CompositeMetadataRepository.REPOSITORY_TYPE, null);
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
		CompositeMetadataRepository repository = createRepository(location, "test");
		try {
			repository.addChild(new URI("../one"));
			repository.addChild(new URI("../two"));
		} catch (URISyntaxException e) {
			fail("1.99", e);
		}

		// query the number of IUs
		List children = repository.getChildren();
		assertEquals("2.0", 2, children.size());
		IQueryResult queryResult = repository.query(QueryUtil.createIUAnyQuery(), getMonitor());
		assertEquals("2.1", 2, queryResultSize(queryResult));

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
			URI location = new URI("http://eclipse.org/equinox/in/memory");
			URI one = new URI("one");
			URI two = new URI("two");
			CompositeMetadataRepository repository = createRepository(location, "in memory test");
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

	public void testFailingChildFailsCompleteRepository() throws ProvisionException, OperationCanceledException {
		boolean exception = false;
		IMetadataRepository repo = null;
		IMetadataRepositoryManager manager = getMetadataRepositoryManager();

		File repoFile = getTestData("Atomic composite with missing child", "/testData/metadataRepo/composite/missingChild/atomicLoading");
		URI correctChildURI = URIUtil.append(repoFile.toURI(), "one");
		URI repoURI = repoFile.getAbsoluteFile().toURI();

		File alreadyLoadedChildFile = getTestData("Atomic composite with missing child", "/testData/metadataRepo/composite/missingChild/atomicLoading/three");
		IMetadataRepository alreadyLoadedChild = manager.loadRepository(alreadyLoadedChildFile.toURI(), null);
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
		IMetadataRepository repo = null;
		IMetadataRepositoryManager manager = getMetadataRepositoryManager();

		File repoFile = getTestData("Composite with missing child", "/testData/metadataRepo/composite/missingChild/nonAtomicLoading");
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
