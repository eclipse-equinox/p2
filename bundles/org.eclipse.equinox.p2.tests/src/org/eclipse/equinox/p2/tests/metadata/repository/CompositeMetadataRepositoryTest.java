/*******************************************************************************
 * Copyright (c) 2008, 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 - ongoing development
 *     EclipseSource - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata.repository;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepositoryFactory;
import org.eclipse.equinox.internal.p2.persistence.CompositeRepositoryState;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.MatchQuery;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.StringBufferStream;
import org.eclipse.equinox.p2.tests.TestData;
import org.eclipse.equinox.p2.tests.core.CompoundQueryableTest.CompoundQueryTestProgressMonitor;

/**
 * Test API of the local metadata repository implementation.
 */
@SuppressWarnings("deprecation") // MatchQuery
public class CompositeMetadataRepositoryTest extends AbstractProvisioningTest {
	private static final String TEST_KEY = "TestKey";
	private static final String TEST_VALUE = "TestValue";
	protected File repoLocation;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		repoLocation = new File(getTempFolder(), "CompositeMetadataRepositoryTest");
		AbstractProvisioningTest.delete(repoLocation);
	}

	@Override
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
		for (File file : files) {
			if ("compositeContent.jar".equalsIgnoreCase(file.getName())) {
				jarFilePresent = true;
			}
			if ("compositeContent.xml".equalsIgnoreCase(file.getName())) {
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
		for (File file : files) {
			if ("compositeContent.jar".equalsIgnoreCase(file.getName())) {
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
			IQueryResult<IInstallableUnit> queryResult = compRepo.query(QueryUtil.createIUAnyQuery(), null);
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
		Map<String, String> properties = repo.getProperties();
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
		Map<String, String> properties = repo.getProperties();
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

	public void testAddChild() throws IOException {
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

	public void testRemoveChild() throws IOException {
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

	public void testAddRepeatChild() throws IOException {
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

	public void testAddMultipleChildren() throws IOException {
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

	public void testRemoveNonexistantChild() throws IOException {
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

	public void testRemoveAllChildren() throws IOException {
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

	public void testLoadingRepositoryRemote()
			throws IOException, ProvisionException, OperationCanceledException, URISyntaxException {
		File knownGoodRepoLocation = getTestData("0.1", "/testData/metadataRepo/composite/good.remote");

		CompositeMetadataRepository compRepo = null;

		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			compRepo = (CompositeMetadataRepository) getMetadataRepositoryManager().loadRepository(knownGoodRepoLocation.toURI(), null);
		} finally {
			System.setOut(out);
		}

		List<URI> children = compRepo.getChildren();

		// ensure children are correct
		URI child1 = URIUtil.fromString("http://www.eclipse.org/foo");
		assertTrue(children.contains(child1));
		URI child2 = URIUtil.fromString("http://www.eclipse.org/bar");
		assertTrue(children.contains(child2));
		assertEquals(2, children.size());

		//ensure correct properties
		assertEquals("metadata name", compRepo.getName());
		Map<String, String> properties = compRepo.getProperties();
		assertEquals(3, properties.size());
		String timestamp = properties.get(IRepository.PROP_TIMESTAMP);
		assertNotNull(timestamp);
		assertEquals("1234", timestamp);
		String compressed = properties.get(IRepository.PROP_COMPRESSED);
		assertNotNull(compressed);
		assertFalse(Boolean.parseBoolean(compressed));
	}

	public void testLoadingRepositoryLocal() throws IOException, ProvisionException, OperationCanceledException {
		File testData = getTestData("0.5", "/testData/metadataRepo/composite/good.local");
		copy(testData, repoLocation);

		CompositeMetadataRepository compRepo = (CompositeMetadataRepository) getMetadataRepositoryManager()
				.loadRepository(repoLocation.toURI(), null);

		List<URI> children = compRepo.getChildren();

		//ensure children are correct
		URI child1 = URIUtil.append(compRepo.getLocation(), "one");
		assertTrue(children.contains(child1));
		URI child2 = URIUtil.append(compRepo.getLocation(), "two");
		assertTrue(children.contains(child2));
		assertEquals(2, children.size());

		//ensure correct properties
		assertEquals("metadata name", compRepo.getName());
		Map<String, String> properties = compRepo.getProperties();
		assertEquals(2, properties.size());
		String timestamp = properties.get(IRepository.PROP_TIMESTAMP);
		assertNotNull(timestamp);
		assertEquals("1234", timestamp);
		String compressed = properties.get(IRepository.PROP_COMPRESSED);
		assertNotNull(compressed);
		assertFalse(Boolean.parseBoolean(compressed));
	}

	public void testCompressedPersistence() throws IOException {
		persistenceTest(true);
	}

	public void testUncompressedPersistence() throws IOException {
		persistenceTest(false);
	}

	public void testSyntaxErrorWhileParsing() throws IOException {
		File badCompositeContent = getTestData("1", "/testData/metadataRepo/composite/Bad/syntaxError");

		StringBuilder buffer = new StringBuilder();
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

	public void testMissingRequireattributeWhileParsing()
			throws IOException, ProvisionException, OperationCanceledException {
		File badCompositeContent = getTestData("0.1", "/testData/metadataRepo/composite/Bad/missingRequiredAttribute");
		copy(badCompositeContent, repoLocation);

		CompositeMetadataRepository compRepo = null;
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			compRepo = (CompositeMetadataRepository) getMetadataRepositoryManager().loadRepository(repoLocation.toURI(), null);
		} finally {
			System.setOut(out);
		}
		assertEquals(1, compRepo.getChildren().size());
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

	public void testGetLatestIU() throws IOException {
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		URI location1 = TestData.getFile("metadataRepo", "multipleversions1").toURI();
		URI location2 = TestData.getFile("metadataRepo", "multipleversions2").toURI();
		CompositeMetadataRepository compositeRepo = createRepo(false);
		compositeRepo.addChild(location1);
		compositeRepo.addChild(location2);
		IQueryResult<IInstallableUnit> queryResult = compositeRepo.query(QueryUtil.createLatestIUQuery(), monitor);
		assertEquals(1, queryResultSize(queryResult));
		assertEquals(Version.createOSGi(3, 0, 0), queryResult.iterator().next().getVersion());
		assertTrue(monitor.isDone());
		assertTrue(monitor.isWorkDone());
	}

	public void testGetLatestIULessThan3() throws IOException {
		CompoundQueryTestProgressMonitor monitor = new CompoundQueryTestProgressMonitor();
		URI location1 = TestData.getFile("metadataRepo", "multipleversions1").toURI();
		URI location2 = TestData.getFile("metadataRepo", "multipleversions2").toURI();
		CompositeMetadataRepository compositeRepo = createRepo(false);
		compositeRepo.addChild(location1);
		compositeRepo.addChild(location2);
		IQuery<IInstallableUnit> cQuery = QueryUtil.createLatestQuery(new MatchQuery<IInstallableUnit>() {
			@Override
			public boolean isMatch(IInstallableUnit candidate) {
				if (candidate.getVersion().compareTo(Version.createOSGi(3, 0, 0)) < 0) {
					return true;
				}
				return false;
			}
		});
		IQueryResult<IInstallableUnit> queryResult = compositeRepo.query(cQuery, monitor);
		assertEquals(1, queryResultSize(queryResult));
		assertEquals(Version.createOSGi(2, 2, 0), queryResult.iterator().next().getVersion());
		assertTrue(monitor.isDone());
		assertTrue(monitor.isWorkDone());
	}

	private void persistenceTest(boolean compressed) throws IOException {
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
		Map<String, String> properties = new HashMap<>();
		properties.put(IRepository.PROP_COMPRESSED, compressed ? "true" : "false");
		IMetadataRepository repo = null;
		try {
			repo = metadataRepositoryManager.createRepository(repoLocation.toURI(), "metadata name", IMetadataRepositoryManager.TYPE_COMPOSITE_REPOSITORY, properties);
		} catch (ProvisionException e) {
			fail("Could not create repository");
		}

		//esnure proper type of repository has been created
		if (!(repo instanceof CompositeMetadataRepository)) {
			fail("Repository is not a CompositeMetadataRepository");
		}

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
				if (isEqual(iu1, iu2)) {
					numKeys--;
				//identical keys has bee found, therefore the number of unique keys is one less than previously thought
				}
			}
		}
		return numKeys;
	}

	/*
	 * Ensure that we can create a non-local composite repository.
	 * Note that we had to change this test method when we changed the
	 * behaviour of the composite repos to aggressively load the children.
	 */
	public void testNonLocalRepo() throws URISyntaxException {
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
			assertEquals(3, repository.getChildren().size());
			repository.removeChild(childTwo);
			assertEquals(2, repository.getChildren().size());
			// add a child which already exists... should do nothing
			repository.addChild(childOne);
			assertEquals(2, repository.getChildren().size());
			// add the same child but with a relative URI. again it should do nothing
			repository.addChild(new URI("one"));
			assertEquals(2, repository.getChildren().size());
		} finally {
			System.setOut(out);
		}
	}

	protected CompositeMetadataRepository createRepository(URI location, String name) {
		CompositeMetadataRepositoryFactory factory = new CompositeMetadataRepositoryFactory();
		factory.setAgent(getAgent());
		return (CompositeMetadataRepository) factory.create(location, name, CompositeMetadataRepository.REPOSITORY_TYPE, null);
	}

	public void testRelativeChildren() throws IOException, URISyntaxException {
		// setup
		File one = getTestData("0.0", "testData/testRepos/simple.1");
		File two = getTestData("0.1", "testData/testRepos/simple.2");
		File temp = getTempFolder();
		copy(one, new File(temp, "one"));
		copy(two, new File(temp, "two"));

		// create the composite repository and add the children
		URI location = new File(temp, "comp").toURI();
		CompositeMetadataRepository repository = createRepository(location, "test");
		repository.addChild(new URI("../one"));
		repository.addChild(new URI("../two"));

		// query the number of IUs
		List<URI> children = repository.getChildren();
		assertEquals(2, children.size());
		IQueryResult<IInstallableUnit> queryResult = repository.query(QueryUtil.createIUAnyQuery(), getMonitor());
		assertEquals(2, queryResultSize(queryResult));

		// ensure the child URIs are stored as relative
		CompositeRepositoryState state = repository.toState();
		URI[] childURIs = state.getChildren();
		assertNotNull(childURIs);
		assertEquals(2, childURIs.length);
		assertFalse(childURIs[0].isAbsolute());
		assertFalse(childURIs[1].isAbsolute());

		// cleanup
		delete(temp);
	}

	public void testRelativeRemoveChild() throws URISyntaxException {
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			URI location = new URI("http://eclipse.org/equinox/in/memory");
			URI one = new URI("one");
			URI two = new URI("two");
			CompositeMetadataRepository repository = createRepository(location, "in memory test");
			repository.addChild(one);
			repository.addChild(two);
			List<URI> children = repository.getChildren();
			assertEquals(2, children.size());
			// remove an absolute URI (child one should be first since order is important)
			repository.removeChild(children.iterator().next());
			assertEquals(1, repository.getChildren().size());
			// remove a relative URI (child two)
			repository.removeChild(two);
			assertEquals(0, repository.getChildren().size());
		} finally {
			System.setOut(out);
		}
	}

	public void testFailingChildFailsCompleteRepository()
			throws ProvisionException, OperationCanceledException, IOException {
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

	public void testFailingChildLoadsCompleteRepository() throws IOException {
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
