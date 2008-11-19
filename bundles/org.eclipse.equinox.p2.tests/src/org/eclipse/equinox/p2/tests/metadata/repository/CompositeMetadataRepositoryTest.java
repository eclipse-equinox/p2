/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata.repository;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepositoryFactory;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.osgi.framework.Version;

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
			descriptor.setVersion(new Version("3.2.1"));
			IInstallableUnit iu = MetadataFactory.createInstallableUnit(descriptor);
			compRepo.addInstallableUnits(new IInstallableUnit[] {iu});
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
			compRepo.removeInstallableUnits(InstallableUnitQuery.ANY, null);
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
		assertEquals("Assert correct number of IUs", getNumUnique(repo1.query(InstallableUnitQuery.ANY, new Collector(), null), repo2.query(InstallableUnitQuery.ANY, new Collector(), null)), compRepo.query(InstallableUnitQuery.ANY, new Collector(), null).size());
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

	public void testValidate() {
		//Setup: create an uncompressed repository
		createRepo(false);
		assertEquals("Verifying repository's status is OK", Status.OK_STATUS, (new CompositeMetadataRepositoryFactory()).validate(repoLocation.toURI(), null));
	}

	public void testCompressedPersistence() {
		persistenceTest(true);
	}

	public void testUncompressedPersistence() {
		persistenceTest(false);
	}

	public void testSyntaxErrorWhileParsing() {
		File badCompositeContent = getTestData("1", "/testData/metadataRepo/composite/Bad/syntaxError");

		try {
			getMetadataRepositoryManager().loadRepository(badCompositeContent.toURI(), null);
			//Error while parsing expected
			fail("Expected ProvisionException has not been thrown");
		} catch (ProvisionException e) {
			//expected.
			//TODO more meaningful verification?
		}
	}

	public void testMissingRequireattributeWhileParsing() {
		File badCompositeContent = getTestData("1", "/testData/metadataRepo/composite/Bad/missingRequiredAttribute");
		CompositeMetadataRepository compRepo = null;
		try {
			compRepo = (CompositeMetadataRepository) getMetadataRepositoryManager().loadRepository(badCompositeContent.toURI(), null);
		} catch (ProvisionException e) {
			fail("Error loading repository", e);
		}
		assertEquals("Repository should only have 1 child", 1, compRepo.getChildren().size());
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
		assertEquals("Assert correct number of IUs", getNumUnique(repo1.query(InstallableUnitQuery.ANY, new Collector(), null), repo2.query(InstallableUnitQuery.ANY, new Collector(), null)), compRepo.query(InstallableUnitQuery.ANY, new Collector(), null).size());
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
	private int getNumUnique(Collector c1, Collector c2) {
		Object[] repo1 = c1.toCollection().toArray();
		Object[] repo2 = c2.toCollection().toArray();

		//initialize to the size of both collectors
		int numKeys = repo1.length + repo2.length;

		for (int i = 0; i < repo1.length; i++) {
			for (int j = 0; j < repo2.length; j++) {
				if (isEqual((IInstallableUnit) repo1[i], (IInstallableUnit) repo2[j]))
					numKeys--;
				//identical keys has bee found, therefore the number of unique keys is one less than previously thought
			}
		}
		return numKeys;
	}
}
