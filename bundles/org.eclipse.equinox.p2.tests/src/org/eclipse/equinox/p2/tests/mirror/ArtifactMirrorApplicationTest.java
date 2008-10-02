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
import org.eclipse.equinox.internal.p2.artifact.mirror.MirrorApplication;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.Bundle;

/**
 * Test API of the basic mirror application functionality's implementation.
 */
public class ArtifactMirrorApplicationTest extends AbstractProvisioningTest {
	protected File destRepoLocation;
	protected File sourceRepoLocation; //helloworldfeature
	protected File sourceRepo2Location; //anotherfeature
	protected File sourceRepo3Location; //helloworldfeature + yetanotherfeature
	protected File sourceRepo4Location; //helloworldfeature v1.0.1

	private IArtifactRepositoryManager getArtifactRepositoryManager() {
		return (IArtifactRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IArtifactRepositoryManager.class.getName());
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
		delete(destRepoLocation);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.tests.AbstractProvisioningTest#tearDown()
	 */
	protected void tearDown() throws Exception {
		//remove all the repositories
		getArtifactRepositoryManager().removeRepository(destRepoLocation.toURL());
		getArtifactRepositoryManager().removeRepository(sourceRepoLocation.toURL());
		getArtifactRepositoryManager().removeRepository(sourceRepo2Location.toURL());
		getArtifactRepositoryManager().removeRepository(sourceRepo3Location.toURL());
		getArtifactRepositoryManager().removeRepository(sourceRepo4Location.toURL());

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
	 * Tests mirroring all artifacts in a repository to an empty repository
	 * Source contains A, B
	 * Target contains
	 */
	private void artifactMirrorToEmpty(String message, boolean append) {
		//destination repo is created blank
		runMirrorApplication(message, sourceRepoLocation, destRepoLocation, append);
	}

	/**
	 * Tests mirroring all artifacts in a repository to a repository populated with non-duplicate entries
	 * Source contains A, B
	 * Target contains C, D
	 */
	private void artifactMirrorToPopulated(String message, boolean append) {
		//Setup: populate destination with non-duplicate artifacts
		runMirrorApplication(message + ".0", sourceRepo2Location, destRepoLocation, false); //value of append should not matter

		try {
			//Setup ensure setup completes successfully
			assertContentEquals(message + ".1", getArtifactRepositoryManager().loadRepository(sourceRepo2Location.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail(message + ".2", e);
		} catch (MalformedURLException e) {
			fail(message + ".3", e);
		}

		//mirror test data
		runMirrorApplication(message + ".4", sourceRepoLocation, destRepoLocation, append);
	}

	/**
	 * Tests mirroring all artifacts in a repository to a repository populated with exact duplicate data
	 * Source contains A, B
	 * Target contains A, B
	 */
	private void artifactMirrorToFullDuplicate(String message, boolean append) {
		//Setup: populate destination with duplicate artifacts
		runMirrorApplication(message + ".0", sourceRepoLocation, destRepoLocation, false); //value of append should not matter

		try {
			//Setup: verify contents
			assertContentEquals(message + ".1", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail(message + ".2", e);
		} catch (MalformedURLException e) {
			fail(message + ".3", e);
		}

		//mirror test data
		runMirrorApplication(message + ".4", sourceRepoLocation, destRepoLocation, append);
	}

	/**
	 * Tests mirroring all artifacts in a repository to a repository populated with partially duplicate data
	 * Source contains A, B, C, D
	 * Target contains  A, B
	 */
	private void artifactMirrorToPartialDuplicate(String message, boolean append) {
		//Setup: populate destination with duplicate artifacts
		runMirrorApplication(message + ".0", sourceRepoLocation, destRepoLocation, false);

		try {
			//Setup: verify contents
			assertContentEquals(message + ".1", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail(message + ".2", e);
		} catch (MalformedURLException e) {
			fail(message + ".3", e);
		}

		//mirror test data
		runMirrorApplication(message + ".4", sourceRepo3Location, destRepoLocation, append);
	}

	/**
	 * Tests mirroring all artifacts in a repository to a repository populated with both full duplicate and non-duplicate data
	 * Source contains A, B
	 * Target contains A, B, C, D
	 */
	private void artifactMirrorToPopulatedWithFullDuplicate(String message, boolean append) {
		//Setup: populate destination with non-duplicate artifacts
		runMirrorApplication(message + ".0", sourceRepo3Location, destRepoLocation, false); //value of append should not matter

		try {
			//Setup: verify
			assertContentEquals(message + ".1", getArtifactRepositoryManager().loadRepository(sourceRepo3Location.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail(message + ".2", e);
		} catch (MalformedURLException e) {
			fail(message + ".3", e);
		}

		//mirror duplicate data
		runMirrorApplication(message + ".4", sourceRepoLocation, destRepoLocation, append);
	}

	/**
	 * Tests mirroring all artifacts in a repository to a repository populated with both partial duplicate and non-duplicate data
	 * Source contains A, B, C, D
	 * Target contains A, B, E, F
	 */
	private void artifactMirrorToPopulatedWithPartialDuplicate(String message, boolean append) {
		//Setup: populate destination with non-duplicate artifacts
		runMirrorApplication(message + ".0", sourceRepo2Location, destRepoLocation, false); //value of append should not matter

		try {
			//Setup: verify
			assertContentEquals(message + ".1", getArtifactRepositoryManager().loadRepository(sourceRepo2Location.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail(message + ".2", e);
		} catch (MalformedURLException e) {
			fail(message + ".3", e);
		}

		//Setup: populate destination with duplicate artifacts
		runMirrorApplication(message + ".4", sourceRepoLocation, destRepoLocation, true);

		try {
			//Setup: verify
			assertContains(message + ".5", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
			assertContains(message + ".6", getArtifactRepositoryManager().loadRepository(sourceRepo2Location.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail(message + ".7", e);
		} catch (MalformedURLException e) {
			fail(message + ".8", e);
		}

		//mirror duplicate data
		runMirrorApplication(message + ".9", sourceRepo3Location, destRepoLocation, append);
	}

	/**
	 * Tests mirroring all artifacts from an empty repository
	 * Source contains
	 */
	private File artifactMirrorEmpty(String message, boolean append) {
		//Setup: Create an empty repository
		File emptyRepository = new File(getTempFolder(), getUniqueString());
		try {
			//Setup: remove repository if it exists
			getArtifactRepositoryManager().removeRepository(emptyRepository.toURL());
		} catch (MalformedURLException e1) {
			fail(message + ".0", e1);
		}
		//Setup: delete any data that may be in the folder
		delete(emptyRepository);
		try {
			getArtifactRepositoryManager().createRepository(emptyRepository.toURL(), "Empty Repository", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		} catch (ProvisionException e) {
			fail(message + ".1", e);
		} catch (MalformedURLException e) {
			fail(message + ".2", e);
		}

		runMirrorApplication(message + ".0", emptyRepository, destRepoLocation, append);
		return emptyRepository; //return the repository for use in verification
	}

	/**
	 * Tests mirroring all artifacts from an empty repository
	 * Source contains
	 */
	private File artifactMirrorEmptyToPopulated(String message, boolean append) {
		//Setup: Populate the repository
		runMirrorApplication(message + ".0", sourceRepoLocation, destRepoLocation, false);

		return artifactMirrorEmpty(message + ".1", append); //create the empty repository, perform the mirror, pass the result back
	}

	/**
	 * Tests mirroring all artifacts in a repository to an empty repository with "-append"
	 * Source contains A, B
	 * Target contains
	 * Expected is A, B
	 */
	public void testArtifactMirrorToEmptyWithAppend() {
		artifactMirrorToEmpty("1.0", true); // run the test with append set to true

		try {
			//verify destination's content
			assertContentEquals("1.1", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail("1.2", e);
		} catch (MalformedURLException e) {
			fail("1.3", e);
		}
	}

	/**
	 * Tests mirroring all artifacts in a repository to an empty repository without "-append"
	 * Source contains A, B
	 * Target contains
	 * Expected is A, B
	 */
	public void testArtifactMirrorToEmptyWithoutAppend() {
		artifactMirrorToEmpty("2.0", false);

		try {
			//verify destination's content
			assertContentEquals("2.1", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail("2.2", e);
		} catch (MalformedURLException e) {
			fail("2.3", e);
		}
	}

	/**
	 * Tests mirroring all artifacts in a repository to a repository populated with exact duplicate data with "-append"
	 * Source contains A, B
	 * Target contains A, B
	 * Expected is A, B
	 */
	public void testArtifactMirrorToFullDuplicateWithAppend() {
		artifactMirrorToFullDuplicate("3.0", true); //run the test with append set to true

		try {
			//verify destination's content
			assertContentEquals("3.1", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail("3.2", e);
		} catch (MalformedURLException e) {
			fail("3.3", e);
		}
	}

	/**
	 * Tests mirroring all artifacts in a repository to a repository populated with exact duplicate data wihtout "-append"
	 * Source contains A, B
	 * Target contains A, B
	 * Expected is A, B
	 */
	public void testArtifactMirrorToFullDuplicateWithoutAppend() {
		artifactMirrorToFullDuplicate("4.0", false);

		try {
			//verify destination's content
			assertContentEquals("4.1", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail("4.2", e);
		} catch (MalformedURLException e) {
			fail("4.3", e);
		}
	}

	/**
	 * Tests mirroring all artifacts in a repository to a repository populated with non-duplicate entries with "-append"
	 * Source contains A, B
	 * Target contains C, D
	 * Expected is A, B, C, D
	 */
	public void testArtifactMirrorToPopulatedWithAppend() {
		artifactMirrorToPopulated("5.0", true); //run the test with append set to true

		try {
			//verify destination's content
			assertContains("5.1", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
			assertContains("5.2", getArtifactRepositoryManager().loadRepository(sourceRepo2Location.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
			//checks that the destination has the correct number of keys (no extras)
			assertEquals("5.3", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURL(), null).getArtifactKeys().length + getArtifactRepositoryManager().loadRepository(sourceRepo2Location.toURL(), null).getArtifactKeys().length, getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null).getArtifactKeys().length);
		} catch (ProvisionException e) {
			fail("5.4", e);
		} catch (MalformedURLException e) {
			fail("5.5", e);
		}
	}

	/**
	 * Tests mirroring all artifacts in a repository to a repository populated with non-duplicate entries without "-append"
	 * Source contains A, B
	 * Target contains C, D
	 * Expected is A, B
	 */
	public void testArtifactMirrorToPopulatedWithoutAppend() {
		artifactMirrorToPopulated("6.0", false);

		try {
			//verify destination's content
			assertContentEquals("6.1", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail("6.2", e);
		} catch (MalformedURLException e) {
			fail("6.3", e);
		}
	}

	/**
	 * Tests mirroring all artifacts in a repository to a repository populated with partially duplicate data
	 * Source contains A, B, C, D
	 * Target contains  A, B
	 * Expected is A, B, C, D
	 */
	public void testArtifactMirrorToPartialDuplicateWithAppend() {
		artifactMirrorToPartialDuplicate("7.0", true); //run the test with append set to true

		try {
			//verify destination's content
			assertContentEquals("7.1", getArtifactRepositoryManager().loadRepository(sourceRepo3Location.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail("7.2", e);
		} catch (MalformedURLException e) {
			fail("7.3", e);
		}
	}

	/**
	 * Tests mirroring all artifacts in a repository to a repository populated with partially duplicate data
	 * Source contains A, B, C, D
	 * Target contains  A, B
	 * Expected is A, B, C, D
	 */
	public void testArtifactMirrorToPartialDuplicateWithoutAppend() {
		artifactMirrorToPartialDuplicate("8.0", false);

		try {
			//verify destination's content
			assertContentEquals("8.1", getArtifactRepositoryManager().loadRepository(sourceRepo3Location.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail("8.2", e);
		} catch (MalformedURLException e) {
			fail("8.3", e);
		}
	}

	/**
	 * Tests mirroring all artifacts in a repository to a repository populated with both full duplicate and non-duplicate data with "-append"
	 * Source contains A, B
	 * Target contains A, B, C, D
	 * Expected is A, B, C, D
	 */
	public void testArtifactMirrorToPopulatedWithFullDuplicateWithAppend() {
		artifactMirrorToPopulatedWithFullDuplicate("9.0", true); //run the test with append set to true

		try {
			//verify destination's content
			assertContentEquals("9.1", getArtifactRepositoryManager().loadRepository(sourceRepo3Location.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail("9.2", e);
		} catch (MalformedURLException e) {
			fail("9.3", e);
		}
	}

	/**
	 * Tests mirroring all artifacts in a repository to a repository populated with both full duplicate and non-duplicate data without "-append"
	 * Source contains A, B
	 * Target contains A, B, C, D
	 * Expected is A, B
	 */
	public void testArtifactMirrorToPopulatedWithFullDuplicateWithoutAppend() {
		artifactMirrorToPopulatedWithFullDuplicate("10.0", false);

		try {
			//verify destination's content
			assertContentEquals("10.1", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail("10.2", e);
		} catch (MalformedURLException e) {
			fail("10.3", e);
		}
	}

	/**
	 * Tests mirroring all artifacts in a repository to a repository populated with both partial duplicate and non-duplicate data with "-append"
	 * Source contains A, B, C, D
	 * Target contains A, B, E, F
	 * Expected is A, B, C, D, E, F
	 */
	public void testArtifactMirrorToPopulatedWithPartialDuplicateWithAppend() {
		artifactMirrorToPopulatedWithPartialDuplicate("11.0", true); //run the test with append set to true

		try {
			//verify destination's content
			assertContains("11.1", getArtifactRepositoryManager().loadRepository(sourceRepo3Location.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
			assertContains("11.2", getArtifactRepositoryManager().loadRepository(sourceRepo2Location.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
			//checks that the destination has the correct number of keys (no extras)
			assertEquals("11.3", getArtifactRepositoryManager().loadRepository(sourceRepo2Location.toURL(), null).getArtifactKeys().length + getArtifactRepositoryManager().loadRepository(sourceRepo3Location.toURL(), null).getArtifactKeys().length, getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null).getArtifactKeys().length);
		} catch (ProvisionException e) {
			fail("11.4", e);
		} catch (MalformedURLException e) {
			fail("11.5", e);
		}
	}

	/**
	 * Tests mirroring all artifacts in a repository to a repository populated with both partial duplicate and non-duplicate data without "-append"
	 * Source contains A, B, C, D
	 * Target contains A, B, E, F
	 * Expected is A, B, C, D
	 */
	public void testArtifactMirrorToPopulatedWithPartialDuplicateWithoutAppend() {
		artifactMirrorToPopulatedWithPartialDuplicate("12.0", false);

		try {
			//verify destination's content
			assertContentEquals("12.1", getArtifactRepositoryManager().loadRepository(sourceRepo3Location.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail("12.2", e);
		} catch (MalformedURLException e) {
			fail("12.3", e);
		}
	}

	/**
	 * Tests MirrorApplication's behaviour when given an invalid source repository with "-append"
	 */
	public void testArtifactMirrorFromInvalid() {
		File invalidRepository = new File(getTempFolder(), getUniqueString());
		delete(invalidRepository);

		try {
			basicRunMirrorApplication("13.1", invalidRepository.toURL(), destRepoLocation.toURL(), true);
			//we expect a provision exception to be thrown. We should never get here.
			fail("13.0 ProvisionExpection not thrown");
		} catch (ProvisionException e) {
			return; //correct type of exception has been thrown
		} catch (Exception e) {
			fail("13.2", e);
		}
	}

	/**
	 * Tests MirrorApplication's behaviour when given an invalid destination repository with "-append"
	 */
	public void testArtifactMirrorToInvalid() {
		try {
			//Setup: create a URL pointing to an unmodifiable place
			URL invalidDestRepository = new URL("http://foobar.com/abcdefg");

			//run the application with the modifiable destination
			basicRunMirrorApplication("14.1", sourceRepoLocation.toURL(), invalidDestRepository, true);
			//we're expecting an UnsupportedOperationException so we should never get here
			fail("14.0 UnsupportedOperationException not thrown");
		} catch (UnsupportedOperationException e) {
			return; //correct type of exception has been thrown
		} catch (Exception e) {
			fail("14.2", e);
		}
	}

	/**
	 * Tests MirrorApplication's behaviour when given both an invalid source and an invalid destination repository with "-append"
	 */
	public void testArtifactMirrorBothInvalid() {
		//Setup: create a file that is not a valid repository
		File invalidRepository = new File(getTempFolder(), getUniqueString());
		//Setup: delete any leftover data
		delete(invalidRepository);

		try {
			//Setup: create a URL pointing to an unmodifiable place
			URL invalidDestRepository = new URL("http://foobar.com/abcdefg");
			basicRunMirrorApplication("15.1", invalidRepository.toURL(), invalidDestRepository, true);
			//We expect the UnsupportedOperationException to be thrown
			fail("15.0 UnsupportedOperationException not thrown");
		} catch (UnsupportedOperationException e) {
			return; //correct type of exception was thrown
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
	public void testArtifactMirrorEmptyToEmpty() {
		File emptyRepository = artifactMirrorEmpty("16.0", true);

		try {
			//verify destination's content
			assertContentEquals("16.1", getArtifactRepositoryManager().loadRepository(emptyRepository.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail("16.2", e);
		} catch (MalformedURLException e) {
			fail("16.3", e);
		}

		//Cleanup
		try {
			//remove the emptyRepository
			getArtifactRepositoryManager().removeRepository(emptyRepository.toURL());
		} catch (MalformedURLException e1) {
			//delete any leftover data
			delete(emptyRepository);
			fail("16.4", e1);
		}
		//delete any left over data
		delete(emptyRepository);
	}

	/**
	 * Tests mirroring an empty repository to a populated repository with "-append"
	 * Source contains
	 * Target contains A, B
	 * Expected is A, B
	 */
	public void testArtifactMirrorEmptyToPopulatedWithAppend() {
		File emptyRepository = artifactMirrorEmptyToPopulated("17.0", true);

		try {
			//verify destination's content
			assertContains("17.1", getArtifactRepositoryManager().loadRepository(emptyRepository.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
			assertContentEquals("17.2", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail("17.3", e);
		} catch (MalformedURLException e) {
			fail("17.4", e);
		}

		//Cleanup
		try {
			//remove the empty repository
			getArtifactRepositoryManager().removeRepository(emptyRepository.toURL());
		} catch (MalformedURLException e1) {
			//delete any leftover data
			delete(emptyRepository);
			fail("17.5", e1);
		}
		//remove any leftover data
		delete(emptyRepository);
	}

	/**
	 * Tests mirroring an empty repository to a populated repository without "-append"
	 * Source contains
	 * Target contains A, B
	 * Expected is
	 */
	public void testArtifactMirrorEmptyToPopulatedWithoutAppend() {
		File emptyRepository = artifactMirrorEmptyToPopulated("18.0", false);

		try {
			//verify destination's content
			assertContentEquals("18.1", getArtifactRepositoryManager().loadRepository(emptyRepository.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
		} catch (ProvisionException e) {
			fail("18.2", e);
		} catch (MalformedURLException e) {
			fail("18.3", e);
		}

		//Cleanup
		try {
			//remove the empty repository
			getArtifactRepositoryManager().removeRepository(emptyRepository.toURL());
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

		//run the application with the source and destination specified to the same place
		runMirrorApplication("19.1", destRepoLocation, destRepoLocation, true);

		try {
			//verify destination's content
			assertContentEquals("19.2", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
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

		//run the application with the source and destination specified to the same place
		runMirrorApplication("20.1", sourceRepo4Location, destRepoLocation, true);

		try {
			//verify destination's content
			assertContains("20.2", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
			assertContains("20.3", getArtifactRepositoryManager().loadRepository(sourceRepo4Location.toURL(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null));
			//checks that the destination has the correct number of keys (no extras)
			assertEquals("20.4", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURL(), null).getArtifactKeys().length + getArtifactRepositoryManager().loadRepository(sourceRepo4Location.toURL(), null).getArtifactKeys().length, getArtifactRepositoryManager().loadRepository(destRepoLocation.toURL(), null).getArtifactKeys().length);
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
}
