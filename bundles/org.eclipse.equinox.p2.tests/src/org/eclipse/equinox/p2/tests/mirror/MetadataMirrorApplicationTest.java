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
package org.eclipse.equinox.p2.tests.mirror;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.mirror.MirrorApplication;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.Bundle;

/**
 * Test API of the basic mirror application functionality's implementation.
 */
public class MetadataMirrorApplicationTest extends AbstractProvisioningTest {
	protected File destRepoLocation;
	protected File sourceRepoLocation; //helloworldfeature
	protected File sourceRepo2Location; //anotherfeature
	protected File sourceRepo3Location; //helloworldfeature + yetanotherfeature
	protected File sourceRepo4Location; //helloworldfeature v1.0.1

	private IMetadataRepositoryManager getManager() {
		return (IMetadataRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IMetadataRepositoryManager.class.getName());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.tests.AbstractProvisioningTest#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		//load all the repositories
		sourceRepoLocation = getTestData("0.0", "/testData/mirror/mirrorSourceRepo1");
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
		getManager().removeRepository(destRepoLocation.toURL());
		getManager().removeRepository(sourceRepoLocation.toURL());
		getManager().removeRepository(sourceRepo2Location.toURL());
		getManager().removeRepository(sourceRepo3Location.toURL());
		getManager().removeRepository(sourceRepo4Location.toURL());

		//delete the destination location (no left over files for the next test)
		delete(destRepoLocation);
		super.tearDown();
	}

	/**
	 * runs the mirror application with arguments args
	 */
	private void runMirrorApplication(String message, final String[] args) throws Exception {
		MirrorApplication application = new MirrorApplication();
		application.start(new IApplicationContext() {

			public void applicationRunning() {
			}

			public Map getArguments() {
				Map arguments = new HashMap();

				arguments.put(IApplicationContext.APPLICATION_ARGS, args);

				return arguments;
			}

			public String getBrandingApplication() {
				return null;
			}

			public Bundle getBrandingBundle() {
				return null;
			}

			public String getBrandingDescription() {
				return null;
			}

			public String getBrandingId() {
				return null;
			}

			public String getBrandingName() {
				return null;
			}

			public String getBrandingProperty(String key) {
				return null;
			}
		});
	}

	/**
	 * runs mirror application with default arguments. source is the source repo, destination is the destination repo, append is if the "-append" argument is needed
	 */
	private void basicRunMirrorApplication(String message, URL source, URL destination, boolean append) throws Exception {
		//set the default arguments
		String[] args = new String[] {"-source", source.toExternalForm(), "-destination", destination.toExternalForm(), append ? "-append" : ""};
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
			assertContentEquals(message + ".1", getManager().loadRepository(sourceRepo2Location.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail(message + ".2", e);
		} catch (MalformedURLException e) {
			fail(message + ".3", e);
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
			assertContentEquals(message + ".1", getManager().loadRepository(sourceRepoLocation.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail(message + ".2", e);
		} catch (MalformedURLException e) {
			fail(message + ".3", e);
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
			assertContentEquals(message + ".1", getManager().loadRepository(sourceRepoLocation.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail(message + ".2", e);
		} catch (MalformedURLException e) {
			fail(message + ".3", e);
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
			assertContentEquals(message + ".1", getManager().loadRepository(sourceRepo3Location.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail(message + ".2", e);
		} catch (MalformedURLException e) {
			fail(message + ".3", e);
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
			assertContentEquals(message + ".1", getManager().loadRepository(sourceRepo2Location.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail(message + ".2", e);
		} catch (MalformedURLException e) {
			fail(message + ".3", e);
		}

		//Setup: populate destination with duplicate metadata
		runMirrorApplication(message + ".4", sourceRepoLocation, destRepoLocation, true);

		try {
			//Setup: verify
			assertContains(message + ".5", getManager().loadRepository(sourceRepoLocation.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
			assertContains(message + ".6", getManager().loadRepository(sourceRepo2Location.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail(message + ".7", e);
		} catch (MalformedURLException e) {
			fail(message + ".8", e);
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
		try {
			//Setup: remove repository if it exists
			getManager().removeRepository(emptyRepository.toURL());
		} catch (MalformedURLException e1) {
			fail(message + ".0", e1);
		}
		//Setup: delete any data that may be in the folder
		AbstractProvisioningTest.delete(emptyRepository);
		try {
			getManager().createRepository(emptyRepository.toURL(), "Empty Repository", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		} catch (ProvisionException e) {
			fail(message + ".1", e);
		} catch (MalformedURLException e) {
			fail(message + ".2", e);
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
	 * Tests mirroring all metadata in a repository to an empty repository with "-append" 
	 * Source contains A, B
	 * Target contains
	 * Expected is A, B
	 */
	public void testMetadataMirrorToEmptyWithAppend() {
		metadataMirrorToEmpty("1.0", true); //run the test with append set to true

		try {
			//verify destination's content
			assertContentEquals("1.1", getManager().loadRepository(sourceRepoLocation.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail("1.2", e);
		} catch (MalformedURLException e) {
			fail("1.3", e);
		}
	}

	/**
	 * Tests mirroring all metadata in a repository to an empty repository without "-append"
	 * Source contains A, B
	 * Target contains
	 * Expected is A, B
	 */
	public void testMetadataMirrorToEmptyWithoutAppend() {
		metadataMirrorToEmpty("2.0", false); //run the test with append set to false

		try {
			//verify destination's content
			assertContentEquals("2.1", getManager().loadRepository(sourceRepoLocation.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail("2.2", e);
		} catch (MalformedURLException e) {
			fail("2.3", e);
		}
	}

	/**
	 * Tests mirroring all metadata in a repository to a repository populated with exact duplicate data with "-append"
	 * Source contains A, B
	 * Target contains A, B
	 * Expected is A, B
	 */
	public void testMetadataMirrorToFullDuplicateWithAppend() {
		metadataMirrorToFullDuplicate("3.0", true); //run the test with append set to true

		try {
			//verify destination's content
			assertContentEquals("3.1", getManager().loadRepository(sourceRepoLocation.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail("3.2", e);
		} catch (MalformedURLException e) {
			fail("3.3", e);
		}
	}

	/**
	 * Tests mirroring all metadata in a repository to a repository populated with exact duplicate data wihtout "-append"
	 * Source contains A, B
	 * Target contains A, B
	 * Expected is A, B
	 */
	public void testMetadataMirrorToFullDuplicateWithoutAppend() {
		metadataMirrorToFullDuplicate("4.0", false); //run the test with append set to false

		try {
			//verify destination's content
			assertContentEquals("4.1", getManager().loadRepository(sourceRepoLocation.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail("4.2", e);
		} catch (MalformedURLException e) {
			fail("4.3", e);
		}
	}

	/**
	 * Tests mirroring all metadata in a repository to a repository populated with non-duplicate entries with "-append"
	 * Source contains A, B
	 * Target contains C, D
	 * Expected is A, B, C, D
	 */
	public void testMetadataMirrorToPopulatedWithAppend() {
		metadataMirrorToPopulated("5.0", true); //run the test with append set to true

		try {
			//verify destination's content
			assertContains("5.1", getManager().loadRepository(sourceRepoLocation.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
			assertContains("5.2", getManager().loadRepository(sourceRepo2Location.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
			//checks that the destination has the correct number of keys (no extras)
			assertEquals("5.3", getNumUnique(getManager().loadRepository(sourceRepoLocation.toURL(), null).query(InstallableUnitQuery.ANY, new Collector(), null), getManager().loadRepository(sourceRepo2Location.toURL(), null).query(InstallableUnitQuery.ANY, new Collector(), null)), getManager().loadRepository(destRepoLocation.toURL(), null).query(InstallableUnitQuery.ANY, new Collector(), null).size());
		} catch (ProvisionException e) {
			fail("5.4", e);
		} catch (MalformedURLException e) {
			fail("5.5", e);
		}
	}

	/**
	 * Tests mirroring all metadata in a repository to a repository populated with non-duplicate entries without "-append"
	 * Source contains A, B
	 * Target contains C, D
	 * Expected is A, B
	 */
	public void testMetadataMirrorToPopulatedWithoutAppend() {
		metadataMirrorToPopulated("6.0", false); //run the test with append set to false

		try {
			//verify destination's content
			assertContentEquals("6.1", getManager().loadRepository(sourceRepoLocation.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail("6.2", e);
		} catch (MalformedURLException e) {
			fail("6.3", e);
		}
	}

	/**
	 * Tests mirroring all metadata in a repository to a repository populated with partially duplicate data
	 * Source contains A, B, C, D
	 * Target contains  A, B
	 * Expected is A, B, C, D
	 */
	public void testMetadataMirrorToPartialDuplicateWithAppend() {
		metadataMirrorToPartialDuplicate("7.0", true); //run the test with append set to true

		try {
			//verify destination's content
			assertContentEquals("7.1", getManager().loadRepository(sourceRepo3Location.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail("7.2", e);
		} catch (MalformedURLException e) {
			fail("7.3", e);
		}
	}

	/**
	 * Tests mirroring all metadata in a repository to a repository populated with partially duplicate data
	 * Source contains A, B, C, D
	 * Target contains  A, B
	 * Expected is A, B, C, D
	 */
	public void testMetadataMirrorToPartialDuplicateWithoutAppend() {
		metadataMirrorToPartialDuplicate("8.0", false); //run the test with append set to false

		try {
			//verify destination's content
			assertContentEquals("8.1", getManager().loadRepository(sourceRepo3Location.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail("8.2", e);
		} catch (MalformedURLException e) {
			fail("8.3", e);
		}
	}

	/**
	 * Tests mirroring all metadata in a repository to a repository populated with both full duplicate and non-duplicate data with "-append"
	 * Source contains A, B
	 * Target contains A, B, C, D
	 * Expected is A, B, C, D
	 */
	public void testMetadataMirrorToPopulatedWithFullDuplicateWithAppend() {
		metadataMirrorToPopulatedWithFullDuplicate("9.0", true); //run the test with append set to true

		try {
			//verify destination's content
			assertContentEquals("9.1", getManager().loadRepository(sourceRepo3Location.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail("9.2", e);
		} catch (MalformedURLException e) {
			fail("9.3", e);
		}
	}

	/**
	 * Tests mirroring all metadata in a repository to a repository populated with both full duplicate and non-duplicate data without "-append"
	 * Source contains A, B
	 * Target contains A, B, C, D
	 * Expected is A, B
	 */
	public void testMetadataMirrorToPopulatedWithFullDuplicateWithoutAppend() {
		metadataMirrorToPopulatedWithFullDuplicate("10.0", false); //run the test with append set to false

		try {
			//verify destination's content
			assertContentEquals("10.1", getManager().loadRepository(sourceRepoLocation.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail("10.2", e);
		} catch (MalformedURLException e) {
			fail("10.3", e);
		}
	}

	/**
	 * Tests mirroring all metadata in a repository to a repository populated with both partial duplicate and non-duplicate data with "-append"
	 * Source contains A, B, C, D
	 * Target contains A, B, E, F
	 * Expected is A, B, C, D, E, F
	 */
	public void testMetadataMirrorToPopulatedWithPartialDuplicateWithAppend() {
		metadataMirrorToPopulatedWithPartialDuplicate("11.0", true); //run the test with append set to true

		try {
			//verify destination's content
			assertContains("11.1", getManager().loadRepository(sourceRepo3Location.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
			assertContains("11.2", getManager().loadRepository(sourceRepo2Location.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
			//checks that the destination has the correct number of keys (no extras)
			assertEquals("11.3", getNumUnique(getManager().loadRepository(sourceRepo2Location.toURL(), null).query(InstallableUnitQuery.ANY, new Collector(), null), getManager().loadRepository(sourceRepo3Location.toURL(), null).query(InstallableUnitQuery.ANY, new Collector(), null)), getManager().loadRepository(destRepoLocation.toURL(), null).query(InstallableUnitQuery.ANY, new Collector(), null).size());
		} catch (ProvisionException e) {
			fail("11.4", e);
		} catch (MalformedURLException e) {
			fail("11.5", e);
		}
	}

	/**
	 * Tests mirroring all metadata in a repository to a repository populated with both partial duplicate and non-duplicate data without "-append"
	 * Source contains A, B, C, D
	 * Target contains A, B, E, F
	 * Expected is A, B, C, D
	 */
	public void testMetadataMirrorToPopulatedWithPartialDuplicateWithoutAppend() {
		metadataMirrorToPopulatedWithPartialDuplicate("12.0", false); //run the test with append set to false

		try {
			//verify destination's content
			assertContentEquals("12.1", getManager().loadRepository(sourceRepo3Location.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail("12.2", e);
		} catch (MalformedURLException e) {
			fail("12.3", e);
		}
	}

	/**
	 * Tests MirrorApplication's behaviour when given an invalid source repository with "-append"
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
	 * Tests MirrorApplication's behaviour when given an invalid destination repository with "-append"
	 */
	public void testMetadataMirrorToInvalid() {
		URL invalidDestRepository;
		try {
			invalidDestRepository = new URL("http://foobar.com/abcdefg");
			basicRunMirrorApplication("14.1", sourceRepoLocation.toURL(), invalidDestRepository, true);
			//we expect an illegal state exception to be thrown and should never get here
			fail("14.0 IllegalStateExpection not thrown");
		} catch (IllegalStateException e) {
			return; //correct type of exception has been received
		} catch (Exception e) {
			fail("14.1", e);
		}
	}

	/**
	 * Tests MirrorApplication's behaviour when given both an invalid source and an invalid destination repository with "-append"
	 */
	public void testMetadataMirrorBothInvalid() {
		File invalidRepository = new File(getTempFolder(), getUniqueString());
		delete(invalidRepository);

		try {
			URL invalidDestRepository = new URL("http://foobar.com/abcdefg");
			basicRunMirrorApplication("15.1", invalidRepository.toURL(), invalidDestRepository, true);
			//we expect a provisioning exception to be thrown and should never get here
			fail("15.0 ProvisionExpection not thrown");
		} catch (ProvisionException e) {
			return; //correct type of exception has been thrown
		} catch (Exception e) {
			fail("15.2", e);
		}
	}

	/**
	 * Tests mirroring an empty repository to another empty repository with "-append"
	 * Source contains
	 * Target contains
	 * Expected is
	 */
	public void testMetadataMirrorEmptyToEmpty() {
		File emptyRepository = metadataMirrorEmpty("19.0", false);

		try {
			//verify destination's content
			assertContentEquals("16.1", getManager().loadRepository(emptyRepository.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail("16.2", e);
		} catch (MalformedURLException e) {
			fail("16.3", e);
		}

		//Cleanup
		try {
			//remove the empty repository
			getManager().removeRepository(emptyRepository.toURL());
		} catch (MalformedURLException e) {
			//delete any leftover data
			delete(emptyRepository);
			fail("16.4", e);
		}
		//delete any leftover data
		delete(emptyRepository);
	}

	/**
	 * Tests mirroring an empty repository to a populated repository with "-append"
	 * Source contains
	 * Target contains A, B
	 * Expected is A, B
	 */
	public void testArtifactMirrorEmptyToPopulatedWithAppend() {
		File emptyRepository = metadataMirrorEmptyToPopulated("17.0", true);

		try {
			//verify destination's content
			assertContains("17.1", getManager().loadRepository(emptyRepository.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
			assertContentEquals("17.2", getManager().loadRepository(sourceRepoLocation.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail("17.3", e);
		} catch (MalformedURLException e) {
			fail("17.4", e);
		}

		//Cleanup
		try {
			//remove the empty repository
			getManager().removeRepository(emptyRepository.toURL());
		} catch (MalformedURLException e1) {
			//delete any leftover data
			delete(emptyRepository);
			fail("17.5", e1);
		}
		//delete any leftover data
		delete(emptyRepository);
	}

	/**
	 * Tests mirroring an empty repository to a populated repository without "-append"
	 * Source contains
	 * Target contains A, B
	 * Expected is
	 */
	public void testArtifactMirrorEmptyToPopulatedWithoutAppend() {
		File emptyRepository = metadataMirrorEmptyToPopulated("18.0", false);

		try {
			//verify destination's content
			assertContentEquals("18.1", getManager().loadRepository(emptyRepository.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail("18.2", e);
		} catch (MalformedURLException e) {
			fail("18.3", e);
		}

		//Cleanup
		try {
			//remove the empty repository
			getManager().removeRepository(emptyRepository.toURL());
		} catch (MalformedURLException e1) {
			//delete any leftover data
			delete(emptyRepository);
			fail("18.4", e1);
		}
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
			assertContentEquals("19.2", getManager().loadRepository(sourceRepoLocation.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail("19.3", e);
		} catch (MalformedURLException e) {
			fail("19.4", e);
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
			assertContains("20.2", getManager().loadRepository(sourceRepoLocation.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
			assertContains("20.3", getManager().loadRepository(sourceRepo4Location.toURL(), null), getManager().loadRepository(destRepoLocation.toURL(), null));
			//checks that the destination has the correct number of keys (no extras)
			assertEquals("20.4", getNumUnique(getManager().loadRepository(sourceRepoLocation.toURL(), null).query(InstallableUnitQuery.ANY, new Collector(), null), getManager().loadRepository(sourceRepo4Location.toURL(), null).query(InstallableUnitQuery.ANY, new Collector(), null)), getManager().loadRepository(destRepoLocation.toURL(), null).query(InstallableUnitQuery.ANY, new Collector(), null).size());
		} catch (ProvisionException e) {
			fail("20.5", e);
		} catch (MalformedURLException e) {
			fail("20.6", e);
		}
	}

	/**
	 * Tests how mirror application handles an unspecified source
	 */
	public void testArtifactMirrorNullSource() {
		String[] args = null;
		try {
			//create arguments without a "-source"
			args = new String[] {"-destination", destRepoLocation.toURL().toExternalForm()};
		} catch (MalformedURLException e) {
			fail("21.0", e);
		}

		try {
			runMirrorApplication("21.1", args);
			//We expect the IllegalStateException to be thrown
			fail("21.3 IllegalStateException not thrown");
		} catch (IllegalStateException e) {
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
		try {
			//create arguments without a "-destination"
			args = new String[] {"-source", sourceRepoLocation.toURL().toExternalForm()};
		} catch (MalformedURLException e) {
			fail("22.0", e);
		}

		try {
			runMirrorApplication("22.1", args);
			//We expect the IllegalStateException to be thrown
			fail("22.3 IllegalStateException not thrown");
		} catch (IllegalStateException e) {
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
		} catch (IllegalStateException e) {
			return; //expected type of exception has been thrown
		} catch (Exception e) {
			fail("23.1", e);
		}
	}

	/**
	 * Ensures that a repository created by the mirror application is a copy of the source
	 */
	public void testNewArtifactRepoProperties() {
		//run mirror application with source not preexisting
		metadataMirrorToEmpty("24.0", true);

		try {
			IMetadataRepository sourceRepository = getManager().loadRepository(sourceRepoLocation.toURL(), null);
			IMetadataRepository destinationRepository = getManager().loadRepository(destRepoLocation.toURL(), null);
			assertEquals("24.1", sourceRepository.getName(), destinationRepository.getName());
			assertRepositoryProperties("24.2", sourceRepository.getProperties(), destinationRepository.getProperties());
		} catch (ProvisionException e) {
			fail("24.3", e);
		} catch (MalformedURLException e) {
			fail("24.4", e);
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
			properties = getManager().createRepository(destRepoLocation.toURL(), name, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties).getProperties();
		} catch (ProvisionException e) {
			fail("25.0", e);
		} catch (MalformedURLException e) {
			fail("25.1", e);
		}

		//run the mirror application
		metadataMirrorToEmpty("25.2", true);

		try {
			IMetadataRepository repository = getManager().loadRepository(destRepoLocation.toURL(), null);
			assertEquals("25.3", name, repository.getName());
			assertRepositoryProperties("25.4", properties, repository.getProperties());
		} catch (ProvisionException e) {
			fail("25.5", e);
		} catch (MalformedURLException e) {
			fail("25.6", e);
		}
	}
}
