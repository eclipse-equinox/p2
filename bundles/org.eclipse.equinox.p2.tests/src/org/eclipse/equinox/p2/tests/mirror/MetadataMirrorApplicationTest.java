/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.mirror;

import java.io.File;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.tools.MirrorApplication;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/**
 * Test API of the basic mirror application functionality's implementation.
 */
public class MetadataMirrorApplicationTest extends AbstractProvisioningTest {
	protected File destRepoLocation;
	protected File sourceRepoLocation; //helloworldfeature
	protected File sourceRepo2Location; //anotherfeature
	protected File sourceRepo3Location; //helloworldfeature + yetanotherfeature
	protected File sourceRepo4Location; //helloworldfeature v1.0.1

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.tests.AbstractProvisioningTest#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		//load all the repositories
		sourceRepoLocation = getTestData("0.0", "/testData/mirror/mirrorSourceRepo1 with space");
		sourceRepo2Location = getTestData("0.1", "/testData/mirror/mirrorSourceRepo2");
		sourceRepo3Location = getTestData("0.2", "/testData/mirror/mirrorSourceRepo3");
		sourceRepo4Location = getTestData("0.3", "/testData/mirror/mirrorSourceRepo4");

		//create destination location
		destRepoLocation = new File(getTempFolder(), "BasicMirrorApplicationTest");
		AbstractProvisioningTest.delete(destRepoLocation);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.tests.AbstractProvisioningTest#tearDown()
	 */
	protected void tearDown() throws Exception {
		//remove all the repositories
		getMetadataRepositoryManager().removeRepository(destRepoLocation.toURI());
		getMetadataRepositoryManager().removeRepository(sourceRepoLocation.toURI());
		getMetadataRepositoryManager().removeRepository(sourceRepo2Location.toURI());
		getMetadataRepositoryManager().removeRepository(sourceRepo3Location.toURI());
		getMetadataRepositoryManager().removeRepository(sourceRepo4Location.toURI());

		//delete the destination location (no left over files for the next test)
		delete(destRepoLocation);
		super.tearDown();
	}

	/**
	 * runs the mirror application with arguments args
	 */
	private void runMirrorApplication(String message, final String[] args) throws Exception {
		MirrorApplication application = new MirrorApplication();
		Map map = new HashMap();
		map.put("metadataOrArtifacts", "metadata");
		application.setInitializationData(null, null, map);
		application.initializeFromArguments(args);
		application.run(null);
	}

	/**
	 * Runs mirror application with default arguments. source is the source repo, 
	 * destination is the destination repo, append is if the "-writeMode clean" argument should be excluded
	 * 
	 * Note: We use URL here because command line applications traffic in unencoded URLs,
	 * so we can't use java.net.URI which will always use the encoded form
	 */
	private void basicRunMirrorApplication(String message, URL source, URL destination, boolean append) throws Exception {
		//set the default arguments
		String[] args = new String[] {"-source", source.toExternalForm(), "-destination", destination.toExternalForm(), "-writeMode", append ? "" : "clean"};
		//run the mirror application
		runMirrorApplication(message, args);
	}

	/**
	 * just a wrapper method for compatibility
	 */
	private void runMirrorApplication(String message, File source, File destination, boolean append) {
		try {
			basicRunMirrorApplication(message, source.toURL(), destination.toURL(), append);
		} catch (Exception e) {
			fail(message, e);
		}
	}

	/**
	 * Takes 2 QueryResults, compares them, and returns the number of unique keys
	 * Needed to verify that only the appropriate number of files have been transfered by the mirror application
	 */
	private int getNumUnique(IQueryResult c1, IQueryResult c2) {
		Object[] repo1 = c1.toArray(IInstallableUnit.class);
		Object[] repo2 = c2.toArray(IInstallableUnit.class);

		//initialize to the size of both query results
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

	/**
	 * Tests mirroring all metadata in a repository to an empty repository
	 * Source contains A, B
	 * Target contains
	 */
	private void metadataMirrorToEmpty(String message, boolean append) {
		//destination repo is created blank
		runMirrorApplication(message + ".0", sourceRepoLocation, destRepoLocation, append); //do not append
	}

	/**
	 * Tests mirroring all metadata in a repository to a repository populated with non-duplicate entries
	 * Source contains A, B
	 * Target contains C, D
	 */
	private void metadataMirrorToPopulated(String message, boolean append) {
		//Setup: populate destination with non-duplicate metadata
		runMirrorApplication(message + ".0", sourceRepo2Location, destRepoLocation, false); //value of append does not matter

		try {
			//Setup: ensure setup completed successfully
			assertContentEquals(message + ".1", getMetadataRepositoryManager().loadRepository(sourceRepo2Location.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail(message + ".2", e);
		}

		//mirror test data
		runMirrorApplication(message + ".4", sourceRepoLocation, destRepoLocation, append);
	}

	/**
	 * Tests mirroring all metadata in a repository to a repository populated with exact duplicate data
	 * @throws Exception
	 * Source contains A, B
	 * Target contains A, B
	 */
	private void metadataMirrorToFullDuplicate(String message, boolean append) {
		//Setup: populate destination with duplicate metadata
		runMirrorApplication(message + ".0", sourceRepoLocation, destRepoLocation, false); //value of append does not matter

		try {
			//Setup: verify contents
			assertContentEquals(message + ".1", getMetadataRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail(message + ".2", e);
		}

		//mirror test data
		runMirrorApplication(message + ".4", sourceRepoLocation, destRepoLocation, append);
	}

	/**
	 * Tests mirroring all metadata in a repository to a repository populated with partially duplicate data
	 * Source contains A, B, C, D
	 * Target contains  A, B
	 */
	private void metadataMirrorToPartialDuplicate(String message, boolean append) {
		//Setup: populate destination with duplicate metadata
		runMirrorApplication(message + ".0", sourceRepoLocation, destRepoLocation, false); //value of append does not matter

		try {
			//Setup: verify contents
			assertContentEquals(message + ".1", getMetadataRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail(message + ".2", e);
		}

		//mirror test data
		runMirrorApplication(message + ".4", sourceRepo3Location, destRepoLocation, append);
	}

	/**
	 * Tests mirroring all metadata in a repository to a repository populated with both full duplicate and non-duplicate data
	 * Source contains A, B
	 * Target contains A, B, C, D
	 */
	private void metadataMirrorToPopulatedWithFullDuplicate(String message, boolean append) {
		//Setup: populate destination with non-duplicate metadata
		runMirrorApplication(message + ".0", sourceRepo3Location, destRepoLocation, false); //value of append does not matter

		try {
			//Setup: verify
			assertContentEquals(message + ".1", getMetadataRepositoryManager().loadRepository(sourceRepo3Location.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail(message + ".2", e);
		}

		//mirror duplicate data
		runMirrorApplication(message + ".4", sourceRepoLocation, destRepoLocation, append);
	}

	/**
	 * Tests mirroring all metadata in a repository to a repository populated with both partial duplicate and non-duplicate data
	 * Source contains A, B, C, D
	 * Target contains A, B, E, F
	 */
	private void metadataMirrorToPopulatedWithPartialDuplicate(String message, boolean append) {
		//Setup: populate destination with non-duplicate metadata
		runMirrorApplication(message + ".0", sourceRepo2Location, destRepoLocation, false); //value of append does not matter

		try {
			//Setup: verify
			assertContentEquals(message + ".1", getMetadataRepositoryManager().loadRepository(sourceRepo2Location.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail(message + ".2", e);
		}

		//Setup: populate destination with duplicate metadata
		runMirrorApplication(message + ".4", sourceRepoLocation, destRepoLocation, true);

		try {
			//Setup: verify
			assertContains(message + ".5", getMetadataRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
			assertContains(message + ".6", getMetadataRepositoryManager().loadRepository(sourceRepo2Location.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail(message + ".7", e);
		}

		//mirror duplicate data
		runMirrorApplication(message + ".7", sourceRepo3Location, destRepoLocation, append);
	}

	/**
	 * Tests mirroring all artifacts in a repository to an empty repository
	 * Source contains A, B
	 * Target contains
	 */
	private File metadataMirrorEmpty(String message, boolean append) {
		//Setup: Create an empty repository
		File emptyRepository = new File(getTempFolder(), getUniqueString());
		//Setup: remove repository if it exists
		getMetadataRepositoryManager().removeRepository(emptyRepository.toURI());
		//Setup: delete any data that may be in the folder
		AbstractProvisioningTest.delete(emptyRepository);
		try {
			getMetadataRepositoryManager().createRepository(emptyRepository.toURI(), "Empty Repository", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		} catch (ProvisionException e) {
			fail(message + ".1", e);
		}

		runMirrorApplication(message + ".0", emptyRepository, destRepoLocation, append);
		return emptyRepository; //return the repository for use in verification
	}

	/**
	 * Tests mirroring all metadata from an empty repository
	 * Source contains
	 */
	private File metadataMirrorEmptyToPopulated(String message, boolean append) {
		//Setup: Populate the repository
		runMirrorApplication(message + ".0", sourceRepoLocation, destRepoLocation, false);

		return metadataMirrorEmpty(message + ".1", append); //create the empty repository, perform the mirror, pass the result back
	}

	/**
	 * Tests mirroring all metadata in a repository to an empty repository 
	 * Source contains A, B
	 * Target contains
	 * Expected is A, B
	 */
	public void testMetadataMirrorToEmpty() {
		metadataMirrorToEmpty("1.0", true); //run the test with append set to true

		try {
			//verify destination's content
			assertContentEquals("1.1", getMetadataRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("1.2", e);
		}
	}

	/**
	 * Tests mirroring all metadata in a repository to an empty repository with "-writeMode clean"
	 * Source contains A, B
	 * Target contains
	 * Expected is A, B
	 */
	public void testMetadataMirrorToEmptyWithClean() {
		metadataMirrorToEmpty("2.0", false); //run the test with append set to false

		try {
			//verify destination's content
			assertContentEquals("2.1", getMetadataRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("2.2", e);
		}
	}

	/**
	 * Tests mirroring all metadata in a repository to a repository populated with exact duplicate data
	 * Source contains A, B
	 * Target contains A, B
	 * Expected is A, B
	 */
	public void testMetadataMirrorToFullDuplicate() {
		metadataMirrorToFullDuplicate("3.0", true); //run the test with append set to true

		try {
			//verify destination's content
			assertContentEquals("3.1", getMetadataRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("3.2", e);
		}
	}

	/**
	 * Tests mirroring all metadata in a repository to a repository populated with exact duplicate data with "-writeMode clean"
	 * Source contains A, B
	 * Target contains A, B
	 * Expected is A, B
	 */
	public void testMetadataMirrorToFullDuplicateWithClean() {
		metadataMirrorToFullDuplicate("4.0", false); //run the test with append set to false

		try {
			//verify destination's content
			assertContentEquals("4.1", getMetadataRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("4.2", e);
		}
	}

	/**
	 * Tests mirroring all metadata in a repository to a repository populated with non-duplicate entries
	 * Source contains A, B
	 * Target contains C, D
	 * Expected is A, B, C, D
	 */
	public void testMetadataMirrorToPopulated() {
		metadataMirrorToPopulated("5.0", true); //run the test with append set to true

		try {
			//verify destination's content
			assertContains("5.1", getMetadataRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
			assertContains("5.2", getMetadataRepositoryManager().loadRepository(sourceRepo2Location.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
			//checks that the destination has the correct number of keys (no extras)
			assertEquals("5.3", getNumUnique(getMetadataRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null).query(QueryUtil.createIUAnyQuery(), null), getMetadataRepositoryManager().loadRepository(sourceRepo2Location.toURI(), null).query(QueryUtil.createIUAnyQuery(), null)), queryResultSize(getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null).query(QueryUtil.createIUAnyQuery(), null)));
		} catch (ProvisionException e) {
			fail("5.4", e);
		}
	}

	/**
	 * Tests mirroring all metadata in a repository to a repository populated with non-duplicate entries with "-writeMode clean"
	 * Source contains A, B
	 * Target contains C, D
	 * Expected is A, B
	 */
	public void testMetadataMirrorToPopulatedWithClean() {
		metadataMirrorToPopulated("6.0", false); //run the test with append set to false

		try {
			//verify destination's content
			assertContentEquals("6.1", getMetadataRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("6.2", e);
		}
	}

	/**
	 * Tests mirroring all metadata in a repository to a repository populated with partially duplicate data
	 * Source contains A, B, C, D
	 * Target contains  A, B
	 * Expected is A, B, C, D
	 */
	public void testMetadataMirrorToPartialDuplicate() {
		metadataMirrorToPartialDuplicate("7.0", true); //run the test with append set to true

		try {
			//verify destination's content
			assertContentEquals("7.1", getMetadataRepositoryManager().loadRepository(sourceRepo3Location.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("7.2", e);
		}
	}

	/**
	 * Tests mirroring all metadata in a repository to a repository populated with partially duplicate data with "-writeMode clean"
	 * Source contains A, B, C, D
	 * Target contains  A, B
	 * Expected is A, B, C, D
	 */
	public void testMetadataMirrorToPartialDuplicateWithClean() {
		metadataMirrorToPartialDuplicate("8.0", false); //run the test with append set to false

		try {
			//verify destination's content
			assertContentEquals("8.1", getMetadataRepositoryManager().loadRepository(sourceRepo3Location.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("8.2", e);
		}
	}

	/**
	 * Tests mirroring all metadata in a repository to a repository populated with both full duplicate and non-duplicate data
	 * Source contains A, B
	 * Target contains A, B, C, D
	 * Expected is A, B, C, D
	 */
	public void testMetadataMirrorToPopulatedWithFullDuplicate() {
		metadataMirrorToPopulatedWithFullDuplicate("9.0", true); //run the test with append set to true

		try {
			//verify destination's content
			assertContentEquals("9.1", getMetadataRepositoryManager().loadRepository(sourceRepo3Location.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("9.2", e);
		}
	}

	/**
	 * Tests mirroring all metadata in a repository to a repository populated with both full duplicate and non-duplicate data with "-writeMode clean"
	 * Source contains A, B
	 * Target contains A, B, C, D
	 * Expected is A, B
	 */
	public void testMetadataMirrorToPopulatedWithFullDuplicateWithClean() {
		metadataMirrorToPopulatedWithFullDuplicate("10.0", false); //run the test with append set to false

		try {
			//verify destination's content
			assertContentEquals("10.1", getMetadataRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("10.2", e);
		}
	}

	/**
	 * Tests mirroring all metadata in a repository to a repository populated with both partial duplicate and non-duplicate data
	 * Source contains A, B, C, D
	 * Target contains A, B, E, F
	 * Expected is A, B, C, D, E, F
	 */
	public void testMetadataMirrorToPopulatedWithPartialDuplicate() {
		metadataMirrorToPopulatedWithPartialDuplicate("11.0", true); //run the test with append set to true

		try {
			//verify destination's content
			assertContains("11.1", getMetadataRepositoryManager().loadRepository(sourceRepo3Location.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
			assertContains("11.2", getMetadataRepositoryManager().loadRepository(sourceRepo2Location.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
			//checks that the destination has the correct number of keys (no extras)
			assertEquals("11.3", getNumUnique(getMetadataRepositoryManager().loadRepository(sourceRepo2Location.toURI(), null).query(QueryUtil.createIUAnyQuery(), null), getMetadataRepositoryManager().loadRepository(sourceRepo3Location.toURI(), null).query(QueryUtil.createIUAnyQuery(), null)), queryResultSize(getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null).query(QueryUtil.createIUAnyQuery(), null)));
		} catch (ProvisionException e) {
			fail("11.4", e);
		}
	}

	/**
	 * Tests mirroring all metadata in a repository to a repository populated with both partial duplicate and non-duplicate data with "-writeMode clean"
	 * Source contains A, B, C, D
	 * Target contains A, B, E, F
	 * Expected is A, B, C, D
	 */
	public void testMetadataMirrorToPopulatedWithPartialDuplicateWithClean() {
		metadataMirrorToPopulatedWithPartialDuplicate("12.0", false); //run the test with append set to false

		try {
			//verify destination's content
			assertContentEquals("12.1", getMetadataRepositoryManager().loadRepository(sourceRepo3Location.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("12.2", e);
		}
	}

	/**
	 * Tests MirrorApplication's behaviour when given an invalid source repository
	 */
	public void testMetadataMirrorFromInvalid() {
		//get a temp folder
		File invalidRepository = new File(getTempFolder(), getUniqueString());
		//delete any data that may exist in that temp folder
		delete(invalidRepository);

		try {
			basicRunMirrorApplication("13.1", invalidRepository.toURL(), destRepoLocation.toURL(), true);
			//we expect a provisioning exception to be thrown and should never get here
			fail("13.0 ProvisionExpection not thrown");
		} catch (ProvisionException e) {
			return; //correct type of exception has been received
		} catch (Exception e) {
			fail("13.2", e);
		}
	}

	/**
	 * Tests MirrorApplication's behaviour when given an invalid destination repository
	 */
	public void testMetadataMirrorToInvalid() {
		URI invalidDestRepository;
		try {
			invalidDestRepository = new URI("http://eclipse.org/equinox/foobar/abcdefg");
			basicRunMirrorApplication("14.1", sourceRepoLocation.toURL(), invalidDestRepository.toURL(), true);
			//we expect an illegal state exception to be thrown and should never get here
			fail("14.0 IllegalStateExpection not thrown");
		} catch (IllegalStateException e) {
			return; //correct type of exception has been received
		} catch (Exception e) {
			fail("14.1", e);
		}
	}

	/**
	 * Tests MirrorApplication's behaviour when given both an invalid source and an invalid destination repository
	 */
	public void testMetadataMirrorBothInvalid() {
		File invalidRepository = new File(getTempFolder(), getUniqueString());
		delete(invalidRepository);

		try {
			URI invalidDestRepository = new URI("http://eclipse.org/equinox/foobar/abcdefg");
			basicRunMirrorApplication("15.1", invalidRepository.toURL(), invalidDestRepository.toURL(), true);
			//we expect a provisioning exception to be thrown and should never get here
			fail("15.0 ProvisionExpection not thrown");
		} catch (ProvisionException e) {
			return; //correct type of exception has been thrown
		} catch (Exception e) {
			fail("15.2", e);
		}
	}

	/**
	 * Tests mirroring an empty repository to another empty repository
	 * Source contains
	 * Target contains
	 * Expected is
	 */
	public void testMetadataMirrorEmptyToEmpty() {
		File emptyRepository = metadataMirrorEmpty("19.0", false);

		try {
			//verify destination's content
			assertContentEquals("16.1", getMetadataRepositoryManager().loadRepository(emptyRepository.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("16.2", e);
		}

		//remove the empty repository
		getMetadataRepositoryManager().removeRepository(emptyRepository.toURI());
		//delete any leftover data
		delete(emptyRepository);
	}

	/**
	 * Tests mirroring an empty repository to a populated repository
	 * Source contains
	 * Target contains A, B
	 * Expected is A, B
	 */
	public void testArtifactMirrorEmptyToPopulated() {
		File emptyRepository = metadataMirrorEmptyToPopulated("17.0", true);

		try {
			//verify destination's content
			assertContains("17.1", getMetadataRepositoryManager().loadRepository(emptyRepository.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
			assertContentEquals("17.2", getMetadataRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("17.3", e);
		}

		//remove the empty repository
		getMetadataRepositoryManager().removeRepository(emptyRepository.toURI());
		//delete any leftover data
		delete(emptyRepository);
	}

	/**
	 * Tests mirroring an empty repository to a populated repository with "-writeMode clean"
	 * Source contains
	 * Target contains A, B
	 * Expected is
	 */
	public void testArtifactMirrorEmptyToPopulatedWithClean() {
		File emptyRepository = metadataMirrorEmptyToPopulated("18.0", false);

		try {
			//verify destination's content
			assertContentEquals("18.1", getMetadataRepositoryManager().loadRepository(emptyRepository.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("18.2", e);
		}

		//remove the empty repository
		getMetadataRepositoryManager().removeRepository(emptyRepository.toURI());
		//delete any leftover data
		delete(emptyRepository);
	}

	/**
	 * Tests mirroring a repository to itself
	 * Source contains A, B
	 * Target contains A, B
	 * Expected is A, B
	 */
	public void testArtifactMirrorSourceIsDestination() {
		//Setup: Populate the repository
		runMirrorApplication("19.0", sourceRepoLocation, destRepoLocation, false);

		//run the mirror application with the source being the same as the destination
		runMirrorApplication("19.1", destRepoLocation, destRepoLocation, true);

		try {
			//verify destination's content
			assertContentEquals("19.2", getMetadataRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("19.3", e);
		}
	}

	/**
	 * Tests mirroring a repository with a different version of the same package
	 * Source contains A, B (v1.0.1)
	 * Target contains A, B (v1.0.0)
	 * Expected is A, B (v1.0.0) and A, B (v1.0.1)
	 */
	public void testArtifactMirrorDifferentVersions() {
		//Setup: Populate the repository
		runMirrorApplication("20.0", sourceRepoLocation, destRepoLocation, false);

		//start a mirror application where the source contains the same artifacts but with a different version compared to the destination
		runMirrorApplication("20.1", sourceRepo4Location, destRepoLocation, true);

		try {
			//verify destination's content
			assertContains("20.2", getMetadataRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
			assertContains("20.3", getMetadataRepositoryManager().loadRepository(sourceRepo4Location.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
			//checks that the destination has the correct number of keys (no extras)
			assertEquals("20.4", getNumUnique(getMetadataRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null).query(QueryUtil.createIUAnyQuery(), null), getMetadataRepositoryManager().loadRepository(sourceRepo4Location.toURI(), null).query(QueryUtil.createIUAnyQuery(), null)), queryResultSize(getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null).query(QueryUtil.createIUAnyQuery(), null)));
		} catch (ProvisionException e) {
			fail("20.5", e);
		}
	}

	/**
	 * Tests how mirror application handles an unspecified source
	 */
	public void testArtifactMirrorNullSource() {
		String[] args = null;
		//create arguments without a "-source"
		args = new String[] {"-destination", destRepoLocation.toURI().toString()};

		try {
			runMirrorApplication("21.1", args);
			//We expect the IllegalStateException to be thrown
			fail("21.3 IllegalStateException not thrown");
		} catch (IllegalArgumentException e) {
			return; //expected type of exception has been thrown
		} catch (Exception e) {
			fail("21.2", e);
		}
	}

	/**
	 * Tests how mirror application handles an unspecified destination
	 */
	public void testArtifactMirrorNullDestination() {
		String[] args = null;
		//create arguments without a "-destination"
		args = new String[] {"-source", sourceRepoLocation.toURI().toString()};

		try {
			runMirrorApplication("22.1", args);
			//We expect the IllegalStateException to be thrown
			fail("22.3 IllegalStateException not thrown");
		} catch (ProvisionException e) {
			return; //expected type of exception has been thrown
		} catch (Exception e) {
			fail("22.2", e);
		}
	}

	/**
	 * Tests how mirror application handles both an unspecified source and an unspecified destination
	 */
	public void testArtifactMirrorNullBoth() {
		//create arguments with neither "-destination" nor "-source"
		String[] args = new String[] {};

		try {
			runMirrorApplication("23.0", args);
			//We expect the IllegalStateException to be thrown
			fail("23.2 IllegalStateException not thrown");
		} catch (IllegalArgumentException e) {
			return; //expected type of exception has been thrown
		} catch (Exception e) {
			fail("23.1", e);
		}
	}

	/**
	 * Ensures that a repository created by the mirror application is a copy of the source
	 */
	public void testNewArtifactRepoProperties() {
		//run mirror application with source not pre-existing
		metadataMirrorToEmpty("24.0", true);

		try {
			IMetadataRepository sourceRepository = getMetadataRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null);
			IMetadataRepository destinationRepository = getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null);
			assertEquals("24.1", sourceRepository.getName(), destinationRepository.getName());
			assertRepositoryProperties("24.2", sourceRepository.getProperties(), destinationRepository.getProperties());
		} catch (ProvisionException e) {
			fail("24.3", e);
		}
	}

	/**
	 * Ensures that a repository created before the mirror application is run does not have its properties changed
	 */
	public void testExistingArtifactRepoProperties() {
		//Setup: create the destination
		String name = "Destination Name";
		Map properties = null; //default properties
		try {
			//create the repository and get the resulting properties
			properties = getMetadataRepositoryManager().createRepository(destRepoLocation.toURI(), name, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties).getProperties();
		} catch (ProvisionException e) {
			fail("25.0", e);
		}

		//run the mirror application
		metadataMirrorToEmpty("25.2", true);

		try {
			IMetadataRepository repository = getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null);
			assertEquals("25.3", name, repository.getName());
			assertRepositoryProperties("25.4", properties, repository.getProperties());
		} catch (ProvisionException e) {
			fail("25.5", e);
		}
	}

	/**
	 * Ensures that a repository created by the mirror application has specified name
	 * For Bug 256909
	 */
	public void testNewArtifactRepoWithNewName() {
		String name = "Bug 256909 test - new";
		try {
			//set the arguments
			String[] args = new String[] {"-source", sourceRepoLocation.toURL().toExternalForm(), "-destination", destRepoLocation.toURL().toExternalForm(), "-destinationName", name};
			//run the mirror application
			runMirrorApplication("Bug 256909 Test", args);
		} catch (MalformedURLException e) {
			fail("Error creating URLs for Source/Detination", e);
		} catch (Exception e) {
			fail("Error running mirror application", e);
		}

		try {
			assertEquals("Assert name was set correct", name, getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null).getName());
		} catch (ProvisionException e) {
			fail("Cannot obtain destination", e);
		}
	}

	/**
	 * Ensures that an existing destination used by the mirror application is given specified name
	 * For Bug 256909
	 */
	public void testExistingArtifactRepoWithNewName() {
		String oldName = "The original naem for Bug 256909 test - existing";
		String newName = "Bug 256909 test - existing";
		//Setup create the repository
		IMetadataRepository destinationRepo = null;
		try {
			destinationRepo = getMetadataRepositoryManager().createRepository(destRepoLocation.toURI(), oldName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		} catch (ProvisionException e) {
			fail("Error creating repo at destination", e);
		}
		assertEquals("Assert name is set correctly before mirror", oldName, destinationRepo.getName());

		try {
			//set the arguments
			String[] args = new String[] {"-source", sourceRepoLocation.toURL().toExternalForm(), "-destination", destRepoLocation.toURL().toExternalForm(), "-destinationName", newName};
			//run the mirror application
			runMirrorApplication("Bug 256909 Test", args);
		} catch (MalformedURLException e) {
			fail("Error creating URLs for Source/Detination", e);
		} catch (Exception e) {
			fail("Error running mirror application", e);
		}

		try {
			assertEquals("Assert name is set correctly after mirror", newName, getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null).getName());
		} catch (ProvisionException e) {
			fail("Error loading destination", e);
		}
	}

	//for Bug 235683
	public void testMirrorCompressedSource() {
		File compressedSource = getTestData("0", "/testData/mirror/mirrorCompressedRepo");

		//Setup: get the content.jar file
		File compressedMetadataXML = new File(compressedSource.getAbsoluteFile() + "/content.jar");
		//Setup: make sure content.jar exists
		assertTrue("1", compressedMetadataXML.exists());

		try {
			basicRunMirrorApplication("2", compressedSource.toURL(), destRepoLocation.toURL(), false);
		} catch (MalformedURLException e) {
			fail("3", e);
		} catch (Exception e) {
			fail("4", e);
		}

		//get the content.jar file
		File destMetadataXML = new File(destRepoLocation.getAbsolutePath() + "/content.jar");
		//make sure content.jar exists
		assertTrue("5", destMetadataXML.exists());
	}

	//for Bug 235683
	public void testMirrorCompressedSourcetoUncompressedDestination() {
		File compressedSource = getTestData("0", "/testData/mirror/mirrorCompressedRepo");

		//Setup: get the content.jar file
		File compressedMetadataXML = new File(compressedSource.getAbsoluteFile() + "/content.jar");
		//Setup: make sure content.jar exists
		assertTrue("1", compressedMetadataXML.exists());

		//Setup: create the destination
		try {
			String name = "Destination Name " + destRepoLocation;
			getMetadataRepositoryManager().createRepository(destRepoLocation.toURI(), name, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		} catch (ProvisionException e) {
			fail("2", e);
		}

		try {
			basicRunMirrorApplication("3", compressedSource.toURL(), destRepoLocation.toURL(), false);
		} catch (MalformedURLException e) {
			fail("4", e);
		} catch (Exception e) {
			fail("5", e);
		}

		//get the content.jar file
		File destMetadataXML = new File(destRepoLocation.getAbsolutePath() + "/content.jar");
		//make sure content.jar does not exist
		assertFalse("6", destMetadataXML.exists());
		//get the content.xml file
		destMetadataXML = new File(destRepoLocation.getAbsolutePath() + "/content.xml");
		//make sure content.xml exists
		assertTrue("7", destMetadataXML.exists());
	}

	public void testMirrorUncompressedSourceToCompressedDestination() {
		File uncompressedSource = getTestData("0", "/testData/mirror/mirrorSourceRepo3");

		//Setup: get the content.xml file
		File uncompressedContentXML = new File(uncompressedSource.getAbsoluteFile() + "/content.xml");
		//Setup: make sure content.xml exists
		assertTrue("1", uncompressedContentXML.exists());

		//Setup: create the destination
		try {
			String name = "Destination Name " + destRepoLocation;
			Map property = new HashMap();
			property.put(IRepository.PROP_COMPRESSED, "true");
			getMetadataRepositoryManager().createRepository(destRepoLocation.toURI(), name, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, property);
		} catch (ProvisionException e) {
			fail("2", e);
		}

		assertTrue("2.1", new File(destRepoLocation, "content.jar").exists());
		try {
			basicRunMirrorApplication("3", uncompressedSource.toURL(), destRepoLocation.toURL(), false);
		} catch (MalformedURLException e) {
			fail("4", e);
		} catch (Exception e) {
			fail("5", e);
		}

		//get the content.jar file
		File destMetadataXML = new File(destRepoLocation.getAbsolutePath() + "/content.jar");
		//make sure content.jar does exist
		assertTrue("6", destMetadataXML.exists());
		//get the content.xml file
		destMetadataXML = new File(destRepoLocation.getAbsolutePath() + "/content.xml");
		//make sure content.xml exists
		assertFalse("7", destMetadataXML.exists());
	}

	public void testMirrorApplicationWithCompositeSource() {
		//Setup Make composite repository
		File repoLocation = new File(getTempFolder(), "CompositeMetadataMirrorTest");
		AbstractProvisioningTest.delete(repoLocation);
		IMetadataRepository repo = null;
		try {
			repo = getMetadataRepositoryManager().createRepository(repoLocation.toURI(), "metadata name", IMetadataRepositoryManager.TYPE_COMPOSITE_REPOSITORY, null);
		} catch (ProvisionException e) {
			fail("Could not create repository");
		}
		//ensure proper type of repository has been created
		if (!(repo instanceof CompositeMetadataRepository))
			fail("Repository is not a CompositeMetadataRepository");
		//Populate source
		File child1 = getTestData("1", "/testData/mirror/mirrorSourceRepo1 with space");
		File child2 = getTestData("2", "/testData/mirror/mirrorSourceRepo2");
		((CompositeMetadataRepository) repo).addChild(child1.toURI());
		((CompositeMetadataRepository) repo).addChild(child2.toURI());

		runMirrorApplication("Mirroring from Composite Source", repoLocation, destRepoLocation, false);

		try {
			assertContentEquals("Verifying contents", repo, getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));

			//Verify that result is the same as mirroring from the 2 repositories seperately
			assertContains("3", getMetadataRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
			assertContains("4", getMetadataRepositoryManager().loadRepository(sourceRepo2Location.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
			//checks that the destination has the correct number of keys (no extras)
			assertEquals("5", getNumUnique(getMetadataRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null).query(QueryUtil.createIUAnyQuery(), null), getMetadataRepositoryManager().loadRepository(sourceRepo2Location.toURI(), null).query(QueryUtil.createIUAnyQuery(), null)), queryResultSize(getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null).query(QueryUtil.createIUAnyQuery(), null)));
		} catch (ProvisionException e) {
			fail("Could not load destination", e);
		}
	}

	/**
	 * Test how the mirror application handles a repository specified as a local path
	 */
	public void testMetadataMirrorNonURIDest() {
		String[] args = new String[] {"-destination", destRepoLocation.toString(), "-source", sourceRepoLocation.toString()};

		try {
			runMirrorApplication("Mirroring", args);
			assertContentEquals("2.1", getMetadataRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null), getMetadataRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (Exception e) {
			fail("Error mirroring", e);
		}
	}
}
