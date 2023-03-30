/*******************************************************************************
 *  Copyright (c) 2008, 2021 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.mirror;

import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.p2.artifact.repository.ArtifactRepositoryManager;
import org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.comparator.ArtifactChecksumComparator;
import org.eclipse.equinox.p2.internal.repository.tools.Messages;
import org.eclipse.equinox.p2.internal.repository.tools.MirrorApplication;
import org.eclipse.equinox.p2.internal.repository.tools.RepositoryDescriptor;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.artifact.ArtifactDescriptorQuery;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.StringBufferStream;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.util.NLS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

/*
 * Modified from ArtifactMirrorApplicationTest
 */
@RunWith(Theories.class)
public class NewMirrorApplicationArtifactTest extends AbstractProvisioningTest {
	private static final String DOWNLOAD_CHECKSUM = IArtifactDescriptor.DOWNLOAD_CHECKSUM + ".sha-256";
	private static final String MISSING_ARTIFACT = "canonical: osgi.bundle,javax.wsdl,1.4.0.v200803061811.";
	protected File destRepoLocation;
	protected File sourceRepoLocation; //helloworldfeature
	protected File sourceRepo2Location; //anotherfeature
	protected File sourceRepo3Location; //helloworldfeature + yetanotherfeature
	protected File sourceRepo4Location; //helloworldfeature v1.0.1

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		//load all the repositories
		sourceRepoLocation = getTestData("0.0", "/testData/mirror/mirrorSourceRepo1 with space");
		sourceRepo2Location = getTestData("0.1", "/testData/mirror/mirrorSourceRepo2");
		sourceRepo3Location = getTestData("0.2", "/testData/mirror/mirrorSourceRepo3");
		sourceRepo4Location = getTestData("0.3", "/testData/mirror/mirrorSourceRepo4");

		//create destination location
		destRepoLocation = new File(getTempFolder(), "BasicMirrorApplicationTest");
		delete(destRepoLocation);
	}

	@Override
	@After
	public void tearDown() throws Exception {
		//remove all the repositories
		getArtifactRepositoryManager().removeRepository(destRepoLocation.toURI());
		getArtifactRepositoryManager().removeRepository(sourceRepoLocation.toURI());
		getArtifactRepositoryManager().removeRepository(sourceRepo2Location.toURI());
		getArtifactRepositoryManager().removeRepository(sourceRepo3Location.toURI());
		getArtifactRepositoryManager().removeRepository(sourceRepo4Location.toURI());
		//delete the destination location (no left over files for the next test)
		delete(destRepoLocation);
		super.tearDown();
	}

	private StringBuffer basicRunMirrorApplication(String message, URI source, URI destination, Boolean append, Boolean formatDestination, String destName) throws Exception {
		MirrorApplication app = new MirrorApplication();

		if (destination != null) {
			RepositoryDescriptor dest = null;
			if (formatDestination != null && formatDestination)
				dest = createRepositoryDescriptor(destination, append, source, destName);
			else
				dest = createRepositoryDescriptor(destination, append, null, destName);
			app.addDestination(dest);
		}

		if (source != null) {
			RepositoryDescriptor src = createRepositoryDescriptor(source, null, null, null);
			app.addSource(src);
		}

		StringBuffer buffer = new StringBuffer();
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream(buffer)));
			app.run(null);
		} finally {
			System.setOut(out);
		}
		return buffer;
	}

	private StringBuffer basicRunMirrorApplication(String message, URI source, URI destination, Boolean append, Boolean formatDestination) throws Exception {
		return basicRunMirrorApplication(message, source, destination, append, formatDestination, null);
	}

	private StringBuffer basicRunMirrorApplication(String message, URI source, URI destination) throws Exception {
		return basicRunMirrorApplication(message, source, destination, null, null, null);
	}

	private RepositoryDescriptor createRepositoryDescriptor(URI location, Boolean append, URI format, String name) {
		RepositoryDescriptor descriptor = new RepositoryDescriptor();
		descriptor.setLocation(location);
		descriptor.setKind("artifact");
		if (append != null)
			descriptor.setAppend(append);
		if (format != null)
			descriptor.setFormat(format);
		if (name != null)
			descriptor.setName(name);
		return descriptor;
	}

	/**
	 * just a wrapper method for compatibility
	 */
	private void runMirrorApplication(String message, File source, File destination, boolean append) {
		try {
			basicRunMirrorApplication(message, source.toURI(), destination.toURI(), append, false);
		} catch (Exception e) {
			fail(message, e);
		}
	}

	/**
	 * Tests mirroring all artifacts in a repository to an empty repository
	 * Source contains A, B
	 * Target contains
	 */
	private void artifactMirrorToEmpty(String message, boolean append, boolean format) {
		try {
			//destination repo is created blank
			basicRunMirrorApplication(message, sourceRepoLocation.toURI(), destRepoLocation.toURI(), append, format);
		} catch (Exception e) {
			fail(message, e);
		}
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
			assertContentEquals(message + ".1", getArtifactRepositoryManager().loadRepository(sourceRepo2Location.toURI(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
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
			assertContentEquals(message + ".1", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
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
			assertContentEquals(message + ".1", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
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
			assertContentEquals(message + ".1", getArtifactRepositoryManager().loadRepository(sourceRepo3Location.toURI(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
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
			assertContentEquals(message + ".1", getArtifactRepositoryManager().loadRepository(sourceRepo2Location.toURI(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail(message + ".2", e);
		}

		//Setup: populate destination with duplicate artifacts
		runMirrorApplication(message + ".4", sourceRepoLocation, destRepoLocation, true);

		try {
			//Setup: verify
			assertContains(message + ".5", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
			assertContains(message + ".6", getArtifactRepositoryManager().loadRepository(sourceRepo2Location.toURI(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
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
		getArtifactRepositoryManager().removeRepository(emptyRepository.toURI());
		//Setup: delete any data that may be in the folder
		delete(emptyRepository);
		try {
			getArtifactRepositoryManager().createRepository(emptyRepository.toURI(), "Empty Repository", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
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
	 * Runs mirror app on source with missing artifact with "-ignoreErrors"
	 */
	private void mirrorWithError(boolean verbose) {
		File errorSourceLocation = getTestData("loading error data", "testData/artifactRepo/missingSingleArtifact");
		File validSourceLocation = getTestData("loading error data", "testData/artifactRepo/simple");
		//repo contains an artifact entry for a file that does not exist on disk. this should throw a file not found exception
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			MirrorApplication app = new MirrorApplication();
			app.addSource(createRepositoryDescriptor(errorSourceLocation.toURI(), null, null, null));
			app.addSource(createRepositoryDescriptor(validSourceLocation.toURI(), null, null, null));
			app.addDestination(createRepositoryDescriptor(destRepoLocation.toURI(), null, null, null));
			//Set ignoreErrors flag. Set verbose flag if verbose == true
			app.setVerbose(verbose);
			app.setIgnoreErrors(true);
			//run the mirror application
			app.run(null);
		} catch (Exception e) {
			fail("Running mirror application with errored source failed", e);
		} finally {
			System.setOut(out);
		}
	}

	/**
	 * Tests mirroring all artifacts in a repository to an empty repository
	 * Source contains A, B
	 * Target contains
	 * Expected is A, B
	 */
	@Test
	public void testArtifactMirrorToEmpty() throws ProvisionException {
		artifactMirrorToEmpty("1.0", true, false); // run the test with append set to true

		// verify destination's content
		assertContentEquals("1.1", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null),
				getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
	}

	/**
	 * Tests mirroring all artifacts in a repository to an empty repository with "-writeMode clean"
	 * Source contains A, B
	 * Target contains
	 * Expected is A, B
	 */
	@Test
	public void testArtifactMirrorToEmptyWithClean() throws ProvisionException {
		artifactMirrorToEmpty("2.0", false, false);

		// verify destination's content
		assertContentEquals("2.1", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null),
				getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
	}

	/**
	 * Tests mirroring all artifacts in a repository to a repository populated with exact duplicate data
	 * Source contains A, B
	 * Target contains A, B
	 * Expected is A, B
	 */
	@Test
	public void testArtifactMirrorToFullDuplicate() throws ProvisionException {
		artifactMirrorToFullDuplicate("3.0", true); //run the test with append set to true

		// verify destination's content
		assertContentEquals("3.1", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null),
				getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
	}

	/**
	 * Tests mirroring all artifacts in a repository to a repository populated with exact duplicate data with "-writeMode clean"
	 * Source contains A, B
	 * Target contains A, B
	 * Expected is A, B
	 */
	@Test
	public void testArtifactMirrorToFullDuplicateWithClean() throws ProvisionException {
		artifactMirrorToFullDuplicate("4.0", false);

		// verify destination's content
		assertContentEquals("4.1", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null),
				getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
	}

	/**
	 * Tests mirroring all artifacts in a repository to a repository populated with non-duplicate entries
	 * Source contains A, B
	 * Target contains C, D
	 * Expected is A, B, C, D
	 */
	@Test
	public void testArtifactMirrorToPopulated() throws ProvisionException {
		artifactMirrorToPopulated("5.0", true); //run the test with append set to true

		// verify destination's content
		assertContains("5.1", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null),
				getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		assertContains("5.2", getArtifactRepositoryManager().loadRepository(sourceRepo2Location.toURI(), null),
				getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		// checks that the destination has the correct number of keys (no extras)
		assertEquals("5.3",
				getArtifactKeyCount(sourceRepoLocation.toURI()) + getArtifactKeyCount(sourceRepo2Location.toURI()),
				getArtifactKeyCount(destRepoLocation.toURI()));
	}

	/**
	 * Tests mirroring all artifacts in a repository to a repository populated with non-duplicate entries with "-writeMode clean"
	 * Source contains A, B
	 * Target contains C, D
	 * Expected is A, B
	 */
	@Test
	public void testArtifactMirrorToPopulatedWithClean() throws ProvisionException {
		artifactMirrorToPopulated("6.0", false);

		// verify destination's content
		assertContentEquals("6.1", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null),
				getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
	}

	/**
	 * Tests mirroring all artifacts in a repository to a repository populated with partially duplicate data
	 * Source contains A, B, C, D
	 * Target contains  A, B
	 * Expected is A, B, C, D
	 */
	@Test
	public void testArtifactMirrorToPartialDuplicate() throws ProvisionException {
		artifactMirrorToPartialDuplicate("7.0", true); //run the test with append set to true

		// verify destination's content
		assertContentEquals("7.1", getArtifactRepositoryManager().loadRepository(sourceRepo3Location.toURI(), null),
				getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
	}

	/**
	 * Tests mirroring all artifacts in a repository to a repository populated with partially duplicate data with "-writeMode clean"
	 * Source contains A, B, C, D
	 * Target contains  A, B
	 * Expected is A, B, C, D
	 */
	@Test
	public void testArtifactMirrorToPartialDuplicateWithClean() throws ProvisionException {
		artifactMirrorToPartialDuplicate("8.0", false);

		// verify destination's content
		assertContentEquals("8.1", getArtifactRepositoryManager().loadRepository(sourceRepo3Location.toURI(), null),
				getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
	}

	/**
	 * Tests mirroring all artifacts in a repository to a repository populated with both full duplicate and non-duplicate data
	 * Source contains A, B
	 * Target contains A, B, C, D
	 * Expected is A, B, C, D
	 */
	@Test
	public void testArtifactMirrorToPopulatedWithFullDuplicate() throws ProvisionException {
		artifactMirrorToPopulatedWithFullDuplicate("9.0", true); //run the test with append set to true

		// verify destination's content
		assertContentEquals("9.1", getArtifactRepositoryManager().loadRepository(sourceRepo3Location.toURI(), null),
				getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
	}

	/**
	 * Tests mirroring all artifacts in a repository to a repository populated with both full duplicate and non-duplicate data with "-writeMode clean"
	 * Source contains A, B
	 * Target contains A, B, C, D
	 * Expected is A, B
	 */
	@Test
	public void testArtifactMirrorToPopulatedWithFullDuplicateWithClean() throws ProvisionException {
		artifactMirrorToPopulatedWithFullDuplicate("10.0", false);

		// verify destination's content
		assertContentEquals("10.1", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null),
				getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
	}

	/**
	 * Tests mirroring all artifacts in a repository to a repository populated with both partial duplicate and non-duplicate data
	 * Source contains A, B, C, D
	 * Target contains A, B, E, F
	 * Expected is A, B, C, D, E, F
	 */
	@Test
	public void testArtifactMirrorToPopulatedWithPartialDuplicate() throws ProvisionException {
		artifactMirrorToPopulatedWithPartialDuplicate("11.0", true); //run the test with append set to true

		// verify destination's content
		assertContains("11.1", getArtifactRepositoryManager().loadRepository(sourceRepo3Location.toURI(), null),
				getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		assertContains("11.2", getArtifactRepositoryManager().loadRepository(sourceRepo2Location.toURI(), null),
				getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		// checks that the destination has the correct number of keys (no extras)
		assertEquals("11.3",
				getArtifactKeyCount(sourceRepo2Location.toURI()) + getArtifactKeyCount(sourceRepo3Location.toURI()),
				getArtifactKeyCount(destRepoLocation.toURI()));
	}

	/**
	 * Tests mirroring all artifacts in a repository to a repository populated with both partial duplicate and non-duplicate data with "-writeMode clean"
	 * Source contains A, B, C, D
	 * Target contains A, B, E, F
	 * Expected is A, B, C, D
	 */
	@Test
	public void testArtifactMirrorToPopulatedWithPartialDuplicateWithClean() throws ProvisionException {
		artifactMirrorToPopulatedWithPartialDuplicate("12.0", false);

		// verify destination's content
		assertContentEquals("12.1", getArtifactRepositoryManager().loadRepository(sourceRepo3Location.toURI(), null),
				getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
	}

	/**
	 * Tests MirrorApplication's behaviour when given an invalid source repository
	 */
	@Test
	public void testArtifactMirrorFromInvalid() throws Exception {
		File invalidRepository = new File(getTempFolder(), getUniqueString());
		delete(invalidRepository);

		try {
			basicRunMirrorApplication("13.1", invalidRepository.toURI(), destRepoLocation.toURI(), true, false);
			//we expect a provision exception to be thrown. We should never get here.
			fail("13.0 ProvisionExpection not thrown");
		} catch (ProvisionException e) {
			return; //correct type of exception has been thrown
		}
	}

	/**
	 * Tests MirrorApplication's behaviour when given an invalid destination repository
	 */
	@Test
	public void testArtifactMirrorToInvalid() throws Exception {
		URI invalidDestRepository = null;
		try {
			//Setup: create a URI pointing to an unmodifiable place
			invalidDestRepository = new URI("http://eclipse.org/equinox/foobar/abcdefg");

			//run the application with the modifiable destination
			basicRunMirrorApplication("14.1", sourceRepoLocation.toURI(), invalidDestRepository, true, false);
			//we're expecting an UnsupportedOperationException so we should never get here
			fail("14.0 UnsupportedOperationException not thrown");
		} catch (ProvisionException e) {
			assertEquals("Unexpected error message", NLS.bind(org.eclipse.equinox.p2.internal.repository.tools.Messages.exception_invalidDestination, URIUtil.toUnencodedString(invalidDestRepository)), e.getMessage());
			return; //correct type of exception has been thrown
		} finally {
			if (invalidDestRepository != null)
				getArtifactRepositoryManager().removeRepository(invalidDestRepository);
		}
	}

	/**
	 * Tests MirrorApplication's behaviour when given both an invalid source and an invalid destination repository
	 */
	@Test
	public void testArtifactMirrorBothInvalid() throws Exception {
		//Setup: create a file that is not a valid repository
		File invalidRepository = new File(getTempFolder(), getUniqueString());
		//Setup: delete any leftover data
		delete(invalidRepository);

		try {
			//Setup: create a URI pointing to an unmodifiable place
			URI invalidDestRepository = new URI("http://eclipse.org/equinox/foobar/abcdefg");
			basicRunMirrorApplication("15.1", invalidRepository.toURI(), invalidDestRepository, true, false);
			//We expect the ProvisionException to be thrown
			fail("15.0 ProvisionException not thrown");
		} catch (ProvisionException e) {
			return; //correct type of exception was thrown
		}
	}

	/**
	 * Tests mirroring an empty repository to another empty repository
	 * Source contains
	 * Target contains
	 * Expected is
	 */
	@Test
	public void testArtifactMirrorEmptyToEmpty() throws ProvisionException {
		File emptyRepository = artifactMirrorEmpty("16.0", true);

		// verify destination's content
		assertContentEquals("16.1", getArtifactRepositoryManager().loadRepository(emptyRepository.toURI(), null),
				getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));

		//remove the emptyRepository
		getArtifactRepositoryManager().removeRepository(emptyRepository.toURI());
		//delete any left over data
		delete(emptyRepository);
	}

	/**
	 * Tests mirroring an empty repository to a populated repository
	 * Source contains
	 * Target contains A, B
	 * Expected is A, B
	 */
	@Test
	public void testArtifactMirrorEmptyToPopulated() throws ProvisionException {
		File emptyRepository = artifactMirrorEmptyToPopulated("17.0", true);

		// verify destination's content
		assertContains("17.1", getArtifactRepositoryManager().loadRepository(emptyRepository.toURI(), null),
				getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		assertContentEquals("17.2", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null),
				getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));

		//remove the empty repository
		getArtifactRepositoryManager().removeRepository(emptyRepository.toURI());
		//remove any leftover data
		delete(emptyRepository);
	}

	/**
	 * Tests mirroring an empty repository to a populated repository with "-writeMode clean"
	 * Source contains
	 * Target contains A, B
	 * Expected is
	 */
	@Test
	public void testArtifactMirrorEmptyToPopulatedWithClean() throws ProvisionException {
		File emptyRepository = artifactMirrorEmptyToPopulated("18.0", false);

		// verify destination's content
		assertContentEquals("18.1", getArtifactRepositoryManager().loadRepository(emptyRepository.toURI(), null),
				getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));

		//remove the empty repository
		getArtifactRepositoryManager().removeRepository(emptyRepository.toURI());
		//delete any leftover data
		delete(emptyRepository);
	}

	/**
	 * Tests mirroring a repository to itself
	 * Source contains A, B
	 * Target contains A, B
	 * Expected is A, B
	 */
	@Test
	public void testArtifactMirrorSourceIsDestination() throws ProvisionException {
		//Setup: Populate the repository
		runMirrorApplication("19.0", sourceRepoLocation, destRepoLocation, false);

		//run the application with the source and destination specified to the same place
		runMirrorApplication("19.1", destRepoLocation, destRepoLocation, true);

		// verify destination's content
		assertContentEquals("19.2", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null),
				getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
	}

	/**
	 * Tests mirroring a repository with a different version of the same package
	 * Source contains A, B (v1.0.1)
	 * Target contains A, B (v1.0.0)
	 * Expected is A, B (v1.0.0) and A, B (v1.0.1)
	 */
	@Test
	public void testArtifactMirrorDifferentVersions() throws ProvisionException {
		//Setup: Populate the repository
		runMirrorApplication("20.0", sourceRepoLocation, destRepoLocation, false);

		//run the application with the source and destination specified to the same place
		runMirrorApplication("20.1", sourceRepo4Location, destRepoLocation, true);

		// verify destination's content
		assertContains("20.2", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null),
				getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		assertContains("20.3", getArtifactRepositoryManager().loadRepository(sourceRepo4Location.toURI(), null),
				getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		// checks that the destination has the correct number of keys (no extras)
		assertEquals("20.4",
				getArtifactKeyCount(sourceRepoLocation.toURI()) + getArtifactKeyCount(sourceRepo4Location.toURI()),
				getArtifactKeyCount(destRepoLocation.toURI()));
	}

	/**
	 * Tests how mirror application handles an unspecified source
	 */
	@Test
	public void testArtifactMirrorNullSource() throws Exception {
		try {
			basicRunMirrorApplication("21.1", null, destRepoLocation.toURI());
			//We expect the ProvisionException to be thrown
			fail("21.3 ProvisionException not thrown");
		} catch (ProvisionException e) {
			return; //expected type of exception has been thrown
		}
	}

	/**
	 * Tests how mirror application handles an unspecified destination
	 */
	@Test
	public void testArtifactMirrorNullDestination() throws Exception {
		try {
			basicRunMirrorApplication("22.1", sourceRepoLocation.toURI(), null);
			//We expect the ProvisionException to be thrown
			fail("22.3 ProvisionException not thrown");
		} catch (ProvisionException e) {
			return; //expected type of exception has been thrown
		}
	}

	/**
	 * Tests how mirror application handles both an unspecified source and an unspecified destination
	 */
	@Test
	public void testArtifactMirrorNullBoth() throws Exception {
		try {
			basicRunMirrorApplication("23.0", null, null);
			//We expect the ProvisionException to be thrown
			fail("23.2 ProvisionException not thrown");
		} catch (ProvisionException e) {
			return; //expected type of exception has been thrown
		}
	}

	/**
	 * Ensures that a repository created by the mirror application is a copy of the source
	 */
	@Test
	public void testNewArtifactRepoProperties() throws ProvisionException {
		//run mirror application with source not preexisting
		artifactMirrorToEmpty("24.0", true, true);

		IArtifactRepository sourceRepository = getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURI(),
				null);
		IArtifactRepository destinationRepository = getArtifactRepositoryManager()
				.loadRepository(destRepoLocation.toURI(), null);
		assertEquals("24.1", sourceRepository.getName(), destinationRepository.getName());
		assertRepositoryProperties("24.2", sourceRepository.getProperties(), destinationRepository.getProperties());
	}

	/**
	 * Ensures that a repository created before the mirror application is run does not have its properties changed
	 */
	@Test
	public void testExistingArtifactRepoProperties() throws ProvisionException {
		//Setup: create the destination
		String name = "Destination Name";
		Map<String, String> properties = null; //default properties
		// create the repository and get the resulting properties
		properties = getArtifactRepositoryManager().createRepository(destRepoLocation.toURI(), name,
				IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties).getProperties();

		//run the mirror application
		artifactMirrorToEmpty("25.2", true, false);

		IArtifactRepository repository = getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null);
		assertEquals("25.3", name, repository.getName());
		assertRepositoryProperties("25.4", properties, repository.getProperties());
	}

	/**
	 *  * Ensures that a repository created by the mirror application has specified name
	 * For Bug 256909
	 */
	@Test
	public void testNewArtifactRepoWithNewName() {
		String name = "Bug 256909 test - new";
		try {
			basicRunMirrorApplication("Bug 256909 Test", sourceRepoLocation.toURI(), destRepoLocation.toURI(), true, false, name);
		} catch (MalformedURLException e) {
			fail("Error creating URLs for Source/Detination", e);
		} catch (Exception e) {
			fail("Error running mirror application", e);
		}

		try {
			assertEquals("Assert name was set correct", name, getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null).getName());
		} catch (ProvisionException e) {
			fail("Cannot obtain destination", e);
		}
	}

	/**
	 * Ensures that an existing destination used by the mirror application is given specified name
	 * For Bug 256909
	 */
	@Test
	public void testExistingArtifactRepoWithNewName() {
		String oldName = "The original naem for Bug 256909 test - existing";
		String newName = "Bug 256909 test - existing";
		//Setup create the repository
		IArtifactRepository destinationRepo = null;
		try {
			destinationRepo = getArtifactRepositoryManager().createRepository(destRepoLocation.toURI(), oldName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		} catch (ProvisionException e) {
			fail("Error creating repo at destination", e);
		}
		assertEquals("Assert name is set correctly before mirror", oldName, destinationRepo.getName());

		StringBuffer buffer = new StringBuffer();
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream(buffer)));
			MirrorApplication app = new MirrorApplication();
			app.addSource(createRepositoryDescriptor(sourceRepoLocation.toURI(), null, null, null));
			app.addDestination(createRepositoryDescriptor(destRepoLocation.toURI(), null, null, newName));
			//run the mirror application
			app.run(null);

		} catch (Exception e) {
			fail("Error running mirror application", e);
		} finally {
			System.setOut(out);
		}

		try {
			assertEquals("Assert name is set correctly after mirror", newName, getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null).getName());
		} catch (ProvisionException e) {
			fail("Error loading destination", e);
		}
	}

	/**
	 * Verifies that the mirror application copies files (including packed files) correctly
	 */
	@Test
	public void testArtifactFileCopying() throws Exception {
		//Setup: load the repository containing packed data
		File packedRepoLocation = getTestData("26.0", "/testData/mirror/mirrorSourceRepo2");

		basicRunMirrorApplication("26.1", packedRepoLocation.toURI(), destRepoLocation.toURI(), false, false);

		// Verify Contents
		assertContentEquals("26.4", getArtifactRepositoryManager().loadRepository(packedRepoLocation.toURI(), null),
				getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		// Verify files on disk
		assertEqualArtifacts("26.5",
				(SimpleArtifactRepository) getArtifactRepositoryManager().loadRepository(packedRepoLocation.toURI(),
						null),
				(SimpleArtifactRepository) getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(),
						null));
	}

	//for Bug 235683
	@Test
	public void testMirrorCompressedSource() throws Exception {
		File compressedSource = getTestData("0", "/testData/mirror/mirrorCompressedRepo");

		//Setup: get the artifacts.jar file
		File compressedArtifactsXML = new File(compressedSource.getAbsoluteFile() + "/artifacts.jar");
		//Setup: make sure artifacts.jar exists
		assertTrue("1", compressedArtifactsXML.exists());

		basicRunMirrorApplication("2", compressedSource.toURI(), destRepoLocation.toURI(), false, false);

		//get the artifacts.jar file
		File destArtifactsXML = new File(destRepoLocation.getAbsolutePath() + "/artifacts.jar");
		//make sure artifacts.jar exists
		assertTrue("5", destArtifactsXML.exists());
	}

	//for Bug 235683
	@Test
	public void testMirrorCompressedSourcetoUncompressedDestination() throws Exception {
		File compressedSource = getTestData("0", "/testData/mirror/mirrorCompressedRepo");

		//Setup: get the artifacts.jar file
		File compressedArtifactsXML = new File(compressedSource.getAbsoluteFile() + "/artifacts.jar");
		//Setup: make sure artifacts.jar exists
		assertTrue("1", compressedArtifactsXML.exists());

		//Setup: create the destination
		String name = "Destination Name " + destRepoLocation;
		getArtifactRepositoryManager().createRepository(destRepoLocation.toURI(), name,
				IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);

		assertTrue("2.1", new File(destRepoLocation, "artifacts.xml").exists());
		basicRunMirrorApplication("3", compressedSource.toURI(), destRepoLocation.toURI(), false, false);

		//get the artifacts.jar file
		File destArtifactsXML = new File(destRepoLocation.getAbsolutePath() + "/artifacts.jar");
		//make sure artifacts.jar does not exist
		assertFalse("6", destArtifactsXML.exists());
		//get the artifacts.xml file
		destArtifactsXML = new File(destRepoLocation.getAbsolutePath() + "/artifacts.xml");
		//make sure artifacts.xml exists
		assertTrue("7", destArtifactsXML.exists());
	}

	//for Bug 235683
	@Test
	public void testMirrorUncompressedSourceToCompressedDestination() throws Exception {
		File uncompressedSource = getTestData("0", "/testData/mirror/mirrorPackedRepo");

		//Setup: get the artifacts.xml file
		File artifactsXML = new File(uncompressedSource.getAbsoluteFile() + "/artifacts.xml");
		//Setup: make sure artifacts.xml exists
		assertTrue("1", artifactsXML.exists());

		//Setup: create the destination
		String name = "Destination Name " + destRepoLocation;
		Map<String, String> property = new HashMap<>();
		property.put(IRepository.PROP_COMPRESSED, "true");
		getArtifactRepositoryManager().createRepository(destRepoLocation.toURI(), name,
				IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, property);

		assertTrue("2.1", new File(destRepoLocation, "artifacts.jar").exists());
		basicRunMirrorApplication("3", uncompressedSource.toURI(), destRepoLocation.toURI(), false, false);

		//get the artifacts.jar file
		File destArtifactsXML = new File(destRepoLocation.getAbsolutePath() + "/artifacts.jar");
		//make sure artifacts.jar does exist
		assertTrue("6", destArtifactsXML.exists());
		//get the artifacts.xml file
		destArtifactsXML = new File(destRepoLocation.getAbsolutePath() + "/artifacts.xml");
		//make sure artifacts.xml does not exist
		assertFalse("7", destArtifactsXML.exists());
	}

	@Test
	public void testMirrorApplicationWithCompositeSource() {
		//Setup Make composite repository
		File repoLocation = new File(getTempFolder(), "CompositeArtifactMirrorTest");
		AbstractProvisioningTest.delete(repoLocation);
		IArtifactRepository repo = null;
		try {
			repo = getArtifactRepositoryManager().createRepository(repoLocation.toURI(), "artifact name", IArtifactRepositoryManager.TYPE_COMPOSITE_REPOSITORY, null);
		} catch (ProvisionException e) {
			fail("Could not create repository");
		}
		//ensure proper type of repository has been created
		if (!(repo instanceof CompositeArtifactRepository))
			fail("Repository is not a CompositeArtifactRepository");
		//Populate source
		File child1 = getTestData("1", "/testData/mirror/mirrorSourceRepo1 with space");
		File child2 = getTestData("2", "/testData/mirror/mirrorSourceRepo2");
		((CompositeArtifactRepository) repo).addChild(child1.toURI());
		((CompositeArtifactRepository) repo).addChild(child2.toURI());

		runMirrorApplication("Mirroring from Composite Source", repoLocation, destRepoLocation, false);

		try {
			assertContentEquals("Verifying contents", repo, getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));

			//Verify that result is the same as mirroring from the 2 repositories separately
			assertContains("3", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
			assertContains("4", getArtifactRepositoryManager().loadRepository(sourceRepo2Location.toURI(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
			//checks that the destination has the correct number of keys (no extras)
			assertEquals("5", getArtifactKeyCount(sourceRepoLocation.toURI()) + getArtifactKeyCount(sourceRepo2Location.toURI()), getArtifactKeyCount(destRepoLocation.toURI()));
		} catch (ProvisionException e) {
			fail("Could not load destination", e);
		}
	}

	//for Bug 250527
	@Test
	public void testIgnoreErrorsArgument() {
		//Error prints to stderr, redirect that to a file
		PrintStream oldErr = System.err;
		PrintStream newErr = null;
		try {
			try {
				destRepoLocation.mkdir();
				newErr = new PrintStream(new FileOutputStream(new File(destRepoLocation, "sys.err")));
			} catch (FileNotFoundException e) {
				fail("Error redirecting outputs", e);
			}
			System.setErr(newErr);

			//run test without verbose
			mirrorWithError(false);

		} finally {
			System.setErr(oldErr);
			if (newErr != null)
				newErr.close();
		}
		assertEquals("Verifying correct number of Keys", 2, getArtifactKeyCount(destRepoLocation.toURI()));
		//Because only 2 of the artifacts exists on disk, the number of artifacts in the destination should only be 1.
		//Order in which mirror application mirrors artifacts is random.

	}

	@DataPoints
	public static String[] defaultComparator = { null, ArtifactChecksumComparator.COMPARATOR_ID + ".sha-256" };

	@Theory
	public void testCompareUsingComparator(String comparator) {
		// Setup create descriptors with different checksums
		IArtifactKey dupKey = PublisherHelper.createBinaryArtifactKey("testKeyId", Version.create("1.2.3"));
		File artifact1 = getTestData("0.0", "/testData/mirror/mirrorSourceRepo1 with space/artifacts.xml");
		File artifact2 = getTestData("0.0", "/testData/mirror/mirrorSourceRepo2/artifacts.xml");
		IArtifactDescriptor descriptor1 = PublisherHelper.createArtifactDescriptor(dupKey, artifact1);
		IArtifactDescriptor descriptor2 = PublisherHelper.createArtifactDescriptor(dupKey, artifact2);

		assertEquals("Ensuring Descriptors are the same", descriptor1, descriptor2);
		assertNotEquals("Ensuring download checksums are different",
				descriptor1.getProperty(DOWNLOAD_CHECKSUM), descriptor2.getProperty(DOWNLOAD_CHECKSUM));

		//Setup make repositories
		File repo1Location = getTestFolder(getUniqueString());
		File repo2Location = getTestFolder(getUniqueString());
		IArtifactRepository repo1 = null;
		IArtifactRepository repo2 = null;
		try {
			repo1 = getArtifactRepositoryManager().createRepository(repo1Location.toURI(), "Repo 1", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
			repo1.addDescriptor(descriptor1, new NullProgressMonitor());
			repo2 = getArtifactRepositoryManager().createRepository(repo2Location.toURI(), "Repo 2", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
			repo2.addDescriptor(descriptor2, new NullProgressMonitor());
		} catch (ProvisionException e) {
			fail("Error creating repositories", e);
		}

		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			MirrorApplication app = null;
			try {
				app = new MirrorApplication();
				app.addSource(createRepositoryDescriptor(repo1Location.toURI(), null, null, null));
				app.addDestination(createRepositoryDescriptor(repo2Location.toURI(), null, null, null));
				app.setVerbose(true);
				//Call a comparator
				app.setCompare(true);
				app.setComparatorID(comparator);
				//run the mirror application
				app.run(null);
			} catch (Exception e) {
				fail("Running mirror application with duplicate descriptors with different checksums failed", e);
			}
		} finally {
			System.setOut(out);
		}

		IArtifactDescriptor[] destDescriptors = repo2.getArtifactDescriptors(descriptor2.getArtifactKey());
		assertEquals("Ensuring destination has correct number of descriptors", 1, destDescriptors.length);
		assertEquals("Ensuring proper descriptor exists in destination", descriptor2.getProperty(DOWNLOAD_CHECKSUM),
				destDescriptors[0].getProperty(DOWNLOAD_CHECKSUM));
		String msg = NLS.bind(Messages.warning_different_checksum,
				new Object[] { URIUtil.toUnencodedString(repo1.getLocation()),
						URIUtil.toUnencodedString(repo2.getLocation()), "SHA-256", descriptor1 });
		try {
			assertLogContainsLines(TestActivator.getLogFile(), msg);
		} catch (Exception e) {
			fail("error verifying output", e);
		}
	}

	@Theory
	public void testBaselineCompareUsingComparator(String comparator) {
		// Setup create descriptors with different checksums
		IArtifactKey dupKey = PublisherHelper.createBinaryArtifactKey("testKeyId", Version.create("1.2.3"));
		File artifact1 = getTestData("0.0", "/testData/mirror/mirrorSourceRepo1 with space/content.xml");
		File artifact2 = getTestData("0.0", "/testData/mirror/mirrorSourceRepo2/content.xml");

		//Setup Copy the file to the baseline
		File repoLocation = getTestFolder(getUniqueString());
		File baselineLocation = getTestFolder(getUniqueString());
		File baselineBinaryDirectory = new File(baselineLocation, "binary");
		baselineBinaryDirectory.mkdir();
		File baselineContentLocation = new File(baselineBinaryDirectory, "testKeyId_1.2.3");
		AbstractProvisioningTest.copy("Copying File to baseline", artifact2, baselineContentLocation);

		IArtifactDescriptor descriptor1 = PublisherHelper.createArtifactDescriptor(dupKey, artifact1);
		IArtifactDescriptor descriptor2 = PublisherHelper.createArtifactDescriptor(dupKey, baselineContentLocation);

		assertEquals("Ensuring Descriptors are the same", descriptor1, descriptor2);
		assertNotEquals("Ensuring download checksums are different", descriptor1.getProperty(DOWNLOAD_CHECKSUM),
				descriptor2.getProperty(DOWNLOAD_CHECKSUM));

		//Setup make repositories
		IArtifactRepository repo = null;
		IArtifactRepository baseline = null;
		try {
			repo = getArtifactRepositoryManager().createRepository(repoLocation.toURI(), "Repo 1", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
			repo.addDescriptor(descriptor1, new NullProgressMonitor());
			baseline = getArtifactRepositoryManager().createRepository(baselineLocation.toURI(), "Repo 2", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
			baseline.addDescriptor(descriptor2, new NullProgressMonitor());
		} catch (ProvisionException e) {
			fail("Error creating repositories", e);
		}

		MirrorApplication app = null;
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			app = new MirrorApplication();
			app.addSource(createRepositoryDescriptor(repoLocation.toURI(), null, null, null));
			app.addDestination(createRepositoryDescriptor(destRepoLocation.toURI(), null, null, null));
			//Set baseline
			app.setBaseline(baselineLocation.toURI());
			app.setVerbose(true);
			//Call a comparator
			app.setCompare(true);
			app.setComparatorID(comparator);
			//run the mirror application
			app.run(null);
		} catch (Exception e) {
			fail("Running mirror application with baseline compare", e);

		} finally {
			System.setOut(out);
		}

		IArtifactRepository destination = null;
		try {
			destination = getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null);
		} catch (ProvisionException e) {
			fail("Error loading destination", e);
		}

		IArtifactDescriptor[] destDescriptors = destination.getArtifactDescriptors(descriptor2.getArtifactKey());
		assertEquals("Ensuring destination has correct number of descriptors", 1, destDescriptors.length);
		assertEquals("Ensuring destination contains the descriptor from the baseline",
				descriptor2.getProperty(DOWNLOAD_CHECKSUM), destDescriptors[0].getProperty(DOWNLOAD_CHECKSUM));
		String msg = NLS.bind(Messages.warning_different_checksum,
				new Object[] { URIUtil.toUnencodedString(baseline.getLocation()),
						URIUtil.toUnencodedString(repo.getLocation()), "SHA-256", descriptor1 });
		try {
			assertLogContainsLines(TestActivator.getLogFile(), msg);
		} catch (Exception e) {
			fail("error verifying output", e);
		}
	}

	//for Bug 259111
	@Test
	public void testDownloadRetry() {
		//repository that is known to force a retry
		class TestRetryArtifactRepository extends SimpleArtifactRepository {
			public boolean firstAttempt = true;
			IArtifactRepository source;

			public TestRetryArtifactRepository(String repositoryName, URI location, URI srcLocation, Map<String, String> properties, IArtifactRepositoryManager manager) {
				super(getAgent(), repositoryName, location, properties);

				//initialize
				try {
					source = manager.loadRepository(srcLocation, null);
				} catch (ProvisionException e) {
					fail("Unable to load source for wrapping", e);
				}
				manager.removeRepository(srcLocation);
			}

			@Override
			public synchronized Iterator<IArtifactKey> everything() {
				return ((SimpleArtifactRepository) source).everything();
			}

			@Override
			public synchronized IArtifactDescriptor[] getArtifactDescriptors(IArtifactKey key) {
				return source.getArtifactDescriptors(key);
			}

			@Override
			public IStatus getRawArtifact(IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor) {
				if (firstAttempt) {
					firstAttempt = false;
					return new Status(IStatus.ERROR, Activator.ID, IArtifactRepository.CODE_RETRY, "Forcing Retry", new ProvisionException("Forcing retry"));
				}

				return source.getRawArtifact(descriptor, destination, monitor);
			}

			@Override
			public synchronized boolean contains(IArtifactDescriptor descriptor) {
				return source.contains(descriptor);
			}

			@Override
			public synchronized IQueryResult<IArtifactKey> query(IQuery<IArtifactKey> query, IProgressMonitor monitor) {
				return source.query(query, monitor);
			}
		}

		//set up test repository
		File retryRepoLoaction = new File(getTempFolder(), "259111 Repo");
		IArtifactRepository retryRepo = new TestRetryArtifactRepository("Test Repo", retryRepoLoaction.toURI(), sourceRepoLocation.toURI(), null, getArtifactRepositoryManager());
		((ArtifactRepositoryManager) getArtifactRepositoryManager()).addRepository(retryRepo);

		try {
			basicRunMirrorApplication("Forcing Retry", retryRepo.getLocation(), destRepoLocation.toURI());
		} catch (MalformedURLException e) {
			fail("Error creating arguments", e);
		} catch (Exception e) {
			fail("Error while running Mirror Application and forcing retry", e);
		}

		//ensure error was resulted
		assertFalse(((TestRetryArtifactRepository) retryRepo).firstAttempt);
		try {
			//verify destination's content
			assertContentEquals("Verifying content", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (ProvisionException e) {
			fail("Failure while verifying destination", e);
		}
	}

	//for Bug 259112
	@Test
	public void testErrorLoggingNoVerbose() {
		//initialize log file
		FrameworkLog log = ServiceHelper.getService(Activator.getContext(), FrameworkLog.class);
		assertNotNull("Assert log file is not null", log);
		assertTrue("Clearing log file", log.getFile().delete());

		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			//run test without verbose resulting in error
			mirrorWithError(false);
		} finally {
			System.setOut(out);
		}
		//verify log
		try {
			String[] parts = new String[] {"Artifact not found:", MISSING_ARTIFACT};
			assertLogContainsLine(log.getFile(), parts);
		} catch (Exception e) {
			fail("error verifying output", e);
		}

		//run without verbose
		artifactMirrorToFullDuplicate("Generating INFO entries", true);

		IArtifactRepository sourceRepository = null;
		try {
			sourceRepository = getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null);
		} catch (ProvisionException e) {
			fail("Error loading source repository for verification", e);
		}

		try {
			//Mirroring full duplicate, so any key will do.
			IQueryResult<IArtifactDescriptor> descriptors = sourceRepository.descriptorQueryable().query(ArtifactDescriptorQuery.ALL_DESCRIPTORS, null);
			IArtifactDescriptor descriptor = descriptors.iterator().next();
			//Mirroring full duplicate, so any descriptor will do.
			String message = NLS.bind(org.eclipse.equinox.internal.p2.artifact.repository.Messages.mirror_alreadyExists, descriptor, destRepoLocation.toURI());
			assertLogDoesNotContainLine(log.getFile(), message);
		} catch (Exception e) {
			fail("Error verifying log", e);
		}
	}

	//for Bug 259112
	@Test
	public void testErrorLoggingWithVerbose() {
		//initialize log file
		FrameworkLog log = ServiceHelper.getService(Activator.getContext(), FrameworkLog.class);
		assertNotNull("Assert log file is not null", log);
		assertTrue("Clearing log file", log.getFile().exists() && log.getFile().delete());

		//Comparator prints to stdout, redirect that to a file
		PrintStream oldOut = System.out;
		PrintStream newOut = null;
		PrintStream oldErr = System.err;
		PrintStream newErr = null;
		try {
			try {
				destRepoLocation.mkdir();
				newOut = new PrintStream(new FileOutputStream(new File(destRepoLocation, "sys.out")));
				newErr = new PrintStream(new FileOutputStream(new File(destRepoLocation, "sys.err")));
			} catch (FileNotFoundException e) {
				fail("Error redirecting output", e);
			}
			System.setOut(newOut);
			System.setErr(newErr);

			//run test with verbose, results in error
			mirrorWithError(true);

			//verify log
			try {
				String[] parts = { "Artifact not found:", MISSING_ARTIFACT };
				assertLogContainsLine(log.getFile(), parts);
			} catch (Exception e) {
				fail("error verifying output", e);
			}

			//run with verbose
			//populate destination with duplicate artifacts. We assume this works
			runMirrorApplication("Initializing Destiantion", sourceRepoLocation, destRepoLocation, false); //value of append should not matter

			try {
				MirrorApplication app = new MirrorApplication();
				app.addSource(createRepositoryDescriptor(sourceRepoLocation.toURI(), null, null, null));
				app.addDestination(createRepositoryDescriptor(destRepoLocation.toURI(), null, null, null));
				//set the arguments with verbose
				app.setVerbose(true);
				//run the mirror application
				app.run(null);
			} catch (Exception e) {
				fail("Error running mirror application to generate INFO items", e);
			}

		} finally {
			System.setOut(oldOut);
			if (newOut != null)
				newOut.close();
			System.setErr(oldErr);
			if (newErr != null)
				newErr.close();
		}

		IArtifactRepository sourceRepository = null;
		try {
			sourceRepository = getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null);
		} catch (ProvisionException e) {
			fail("Error loading source repository for verification", e);
		}

		try {
			//Mirroring full duplicate, so any key will do.
			IQueryResult<IArtifactDescriptor> descriptors = sourceRepository.descriptorQueryable().query(ArtifactDescriptorQuery.ALL_DESCRIPTORS, null);
			IArtifactDescriptor descriptor = descriptors.iterator().next();
			//Mirroring full duplicate, so any descriptor will do.
			String message = NLS.bind(org.eclipse.equinox.internal.p2.artifact.repository.Messages.mirror_alreadyExists, descriptor, destRepoLocation.toURI());
			assertLogContainsLines(log.getFile(), message);
		} catch (Exception e) {
			fail("Error verifying log", e);
		}
	}

	/**
	 * Test how the mirror application handles a repository specified as a local path
	 */
	@Test
	public void testArtifactMirrorNonURIDest() {
		try {
			basicRunMirrorApplication("Mirroring", sourceRepoLocation.toURI(), destRepoLocation.toURI());
			assertContentEquals("2.1", getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null), getArtifactRepositoryManager().loadRepository(destRepoLocation.toURI(), null));
		} catch (Exception e) {
			fail("Error mirroring", e);
		}
	}
}