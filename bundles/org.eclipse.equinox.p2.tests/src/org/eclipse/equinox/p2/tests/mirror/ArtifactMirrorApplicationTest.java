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
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.artifact.mirror.MirrorApplication;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
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

	private IArtifactRepositoryManager getManager() {
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
		getManager().removeRepository(destRepoLocation.toURI());
		getManager().removeRepository(sourceRepoLocation.toURI());
		getManager().removeRepository(sourceRepo2Location.toURI());
		getManager().removeRepository(sourceRepo3Location.toURI());
		getManager().removeRepository(sourceRepo4Location.toURI());

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
	private void basicRunMirrorApplication(String message, URI source, URI destination, boolean append) throws Exception {
		//set the default arguments
		String[] args = new String[] {"-source", source.toString(), "-destination", destination.toString(), append ? "-append" : ""};
		//run the mirror application
		runMirrorApplication(message, args);
	}

	/**
	 * just a wrapper method for compatibility
	 */
	private void runMirrorApplication(String message, File source, File destination, boolean append) {
		try {
			basicRunMirrorApplication(message, source.toURI(), destination.toURI(), append);
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
			assertContentEquals(message + ".1", getManager().loadRepository(sourceRepo2Location.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail(message + ".2", e);
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
			assertContentEquals(message + ".1", getManager().loadRepository(sourceRepoLocation.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail(message + ".2", e);
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
			assertContentEquals(message + ".1", getManager().loadRepository(sourceRepoLocation.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail(message + ".2", e);
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
			assertContentEquals(message + ".1", getManager().loadRepository(sourceRepo3Location.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail(message + ".2", e);
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
			assertContentEquals(message + ".1", getManager().loadRepository(sourceRepo2Location.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail(message + ".2", e);
		}

		//Setup: populate destination with duplicate artifacts
		runMirrorApplication(message + ".4", sourceRepoLocation, destRepoLocation, true);

		try {
			//Setup: verify
			assertContains(message + ".5", getManager().loadRepository(sourceRepoLocation.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
			assertContains(message + ".6", getManager().loadRepository(sourceRepo2Location.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail(message + ".7", e);
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
		//Setup: remove repository if it exists
		getManager().removeRepository(emptyRepository.toURI());
		//Setup: delete any data that may be in the folder
		delete(emptyRepository);
		try {
			getManager().createRepository(emptyRepository.toURI(), "Empty Repository", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		} catch (ProvisionException e) {
			fail(message + ".1", e);
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
			assertContentEquals("1.1", getManager().loadRepository(sourceRepoLocation.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("1.2", e);
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
			assertContentEquals("2.1", getManager().loadRepository(sourceRepoLocation.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("2.2", e);
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
			assertContentEquals("3.1", getManager().loadRepository(sourceRepoLocation.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("3.2", e);
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
			assertContentEquals("4.1", getManager().loadRepository(sourceRepoLocation.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("4.2", e);
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
			assertContains("5.1", getManager().loadRepository(sourceRepoLocation.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
			assertContains("5.2", getManager().loadRepository(sourceRepo2Location.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
			//checks that the destination has the correct number of keys (no extras)
			assertEquals("5.3", getManager().loadRepository(sourceRepoLocation.toURI(), null).getArtifactKeys().length + getManager().loadRepository(sourceRepo2Location.toURI(), null).getArtifactKeys().length, getManager().loadRepository(destRepoLocation.toURI(), null).getArtifactKeys().length);
		} catch (ProvisionException e) {
			fail("5.4", e);
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
			assertContentEquals("6.1", getManager().loadRepository(sourceRepoLocation.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("6.2", e);
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
			assertContentEquals("7.1", getManager().loadRepository(sourceRepo3Location.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("7.2", e);
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
			assertContentEquals("8.1", getManager().loadRepository(sourceRepo3Location.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("8.2", e);
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
			assertContentEquals("9.1", getManager().loadRepository(sourceRepo3Location.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("9.2", e);
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
			assertContentEquals("10.1", getManager().loadRepository(sourceRepoLocation.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("10.2", e);
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
			assertContains("11.1", getManager().loadRepository(sourceRepo3Location.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
			assertContains("11.2", getManager().loadRepository(sourceRepo2Location.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
			//checks that the destination has the correct number of keys (no extras)
			assertEquals("11.3", getManager().loadRepository(sourceRepo2Location.toURI(), null).getArtifactKeys().length + getManager().loadRepository(sourceRepo3Location.toURI(), null).getArtifactKeys().length, getManager().loadRepository(destRepoLocation.toURI(), null).getArtifactKeys().length);
		} catch (ProvisionException e) {
			fail("11.4", e);
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
			assertContentEquals("12.1", getManager().loadRepository(sourceRepo3Location.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("12.2", e);
		}
	}

	/**
	 * Tests MirrorApplication's behaviour when given an invalid source repository with "-append"
	 */
	public void testArtifactMirrorFromInvalid() {
		File invalidRepository = new File(getTempFolder(), getUniqueString());
		delete(invalidRepository);

		try {
			basicRunMirrorApplication("13.1", invalidRepository.toURI(), destRepoLocation.toURI(), true);
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
			URI invalidDestRepository = new URI("http://foobar.com/abcdefg");

			//run the application with the modifiable destination
			basicRunMirrorApplication("14.1", sourceRepoLocation.toURI(), invalidDestRepository, true);
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
			URI invalidDestRepository = new URI("http://foobar.com/abcdefg");
			basicRunMirrorApplication("15.1", invalidRepository.toURI(), invalidDestRepository, true);
			//We expect the ProvisionException to be thrown
			fail("15.0 ProvisionException not thrown");
		} catch (ProvisionException e) {
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
			assertContentEquals("16.1", getManager().loadRepository(emptyRepository.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("16.2", e);
		}

		//remove the emptyRepository
		getManager().removeRepository(emptyRepository.toURI());
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
			assertContains("17.1", getManager().loadRepository(emptyRepository.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
			assertContentEquals("17.2", getManager().loadRepository(sourceRepoLocation.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("17.3", e);
		}

		//remove the empty repository
		getManager().removeRepository(emptyRepository.toURI());
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
			assertContentEquals("18.1", getManager().loadRepository(emptyRepository.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("18.2", e);
		}

		//remove the empty repository
		getManager().removeRepository(emptyRepository.toURI());
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
			assertContentEquals("19.2", getManager().loadRepository(sourceRepoLocation.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
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

		//run the application with the source and destination specified to the same place
		runMirrorApplication("20.1", sourceRepo4Location, destRepoLocation, true);

		try {
			//verify destination's content
			assertContains("20.2", getManager().loadRepository(sourceRepoLocation.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
			assertContains("20.3", getManager().loadRepository(sourceRepo4Location.toURI(), null), getManager().loadRepository(destRepoLocation.toURI(), null));
			//checks that the destination has the correct number of keys (no extras)
			assertEquals("20.4", getManager().loadRepository(sourceRepoLocation.toURI(), null).getArtifactKeys().length + getManager().loadRepository(sourceRepo4Location.toURI(), null).getArtifactKeys().length, getManager().loadRepository(destRepoLocation.toURI(), null).getArtifactKeys().length);
		} catch (ProvisionException e) {
			fail("20.5", e);
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
		artifactMirrorToEmpty("24.0", true);

		try {
			IArtifactRepository sourceRepository = getManager().loadRepository(sourceRepoLocation.toURI(), null);
			IArtifactRepository destinationRepository = getManager().loadRepository(destRepoLocation.toURI(), null);
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
			properties = getManager().createRepository(destRepoLocation.toURI(), name, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties).getProperties();
		} catch (ProvisionException e) {
			fail("25.0", e);
		}

		//run the mirror application
		artifactMirrorToEmpty("25.2", true);

		try {
			IArtifactRepository repository = getManager().loadRepository(destRepoLocation.toURI(), null);
			assertEquals("25.3", name, repository.getName());
			assertRepositoryProperties("25.4", properties, repository.getProperties());
		} catch (ProvisionException e) {
			fail("25.5", e);
		}
	}
}
