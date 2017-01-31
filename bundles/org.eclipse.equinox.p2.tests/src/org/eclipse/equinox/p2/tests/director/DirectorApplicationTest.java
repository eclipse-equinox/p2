/*******************************************************************************
 *  Copyright (c) 2008, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *      Cloudsmith Inc - tests for new DirectorApplication
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.director;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.app.DirectorApplication;
import org.eclipse.equinox.internal.simpleconfigurator.utils.URIUtil;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.core.UIServices.TrustInfo;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.StringBufferStream;

/**
 * Various automated tests of the {@link IDirector} API.
 */
public class DirectorApplicationTest extends AbstractProvisioningTest {

	/**
	 * runs default director app.
	 */
	private StringBuffer runDirectorApp(String message, final String[] args) throws Exception {
		PrintStream out = System.out;
		PrintStream err = System.err;
		StringBuffer buffer = new StringBuffer();
		try {
			PrintStream newStream = new PrintStream(new StringBufferStream(buffer));
			System.setOut(newStream);
			System.setErr(newStream);
			DirectorApplication application = new DirectorApplication();
			application.run(args);
		} finally {
			System.setOut(out);
			System.setErr(err);
		}
		return buffer;
	}

	/**
	 * creates the director app arguments based on the arguments submitted with bug 248045
	 */
	private String[] getSingleRepoUninstallArgs(String message, File srcRepo, File destinationRepo, String installIU) {
		String[] args = new String[0];
		try {
			args = new String[] {"-repository", srcRepo.toURL().toExternalForm(), "-uninstallIU", installIU, "-destination", destinationRepo.toURL().toExternalForm(), "-profile", "PlatformSDKProfile"};
		} catch (MalformedURLException e) {
			fail(message, e);
		}
		return args;
	}

	/**
	 * creates the director app arguments based on the arguments submitted with bug 248045
	 */
	private String[] getSingleRepoArgs(String message, File metadataRepo, File artifactRepo, File destinationRepo, String installIU) {
		return new String[] {"-metadataRepository", URIUtil.toUnencodedString(metadataRepo.toURI()), "-artifactRepository", URIUtil.toUnencodedString(artifactRepo.toURI()), "-installIU", installIU, "-destination", URIUtil.toUnencodedString(destinationRepo.toURI()), "-profile", "PlatformSDKProfile", "-profileProperties", "org.eclipse.update.install.features=true", "-bundlepool", destinationRepo.getAbsolutePath(), "-roaming"};
	}

	/**
	 * creates the director app arguments with optional parameter
	 */
	private String[] getSingleRepoArgs(String message, File metadataRepo, File artifactRepo, File destinationRepo, String installIU, String parameter) {
		return new String[] {"-metadataRepository", URIUtil.toUnencodedString(metadataRepo.toURI()), "-artifactRepository", URIUtil.toUnencodedString(artifactRepo.toURI()), "-installIU", installIU, "-destination", URIUtil.toUnencodedString(destinationRepo.toURI()), "-profile", "PlatformSDKProfile", "-profileProperties", "org.eclipse.update.install.features=true", "-bundlepool", destinationRepo.getAbsolutePath(), "-roaming", parameter};
	}

	/**
	 * creates the director app arguments with list arguments
	 */
	private String[] getSingleRepoArgsForListing(String message, File metadataRepo, File artifactRepo, String listArgument, String iuToList, String listFormatArgument, String formatString) {
		return new String[] {"-metadataRepository", URIUtil.toUnencodedString(metadataRepo.toURI()), "-artifactRepository", URIUtil.toUnencodedString(artifactRepo.toURI()), listArgument, iuToList, listFormatArgument, formatString};
	}

	/**
	 * creates the director app arguments based on the arguments submitted with bug 248045 but with multiple repositories for both  metadata and artifacts
	 */
	private String[] getMultipleRepoArgs(String message, File metadataRepo1, File metadataRepo2, File artifactRepo1, File artifactRepo2, File destinationRepo, String installIU) {
		String[] args = new String[0];
		try {
			args = new String[] {"-metadataRepository", metadataRepo1.toURL().toExternalForm() + "," + metadataRepo2.toURL().toExternalForm(), "-artifactRepository", artifactRepo1.toURL().toExternalForm() + "," + artifactRepo2.toURL().toExternalForm(), "-installIU", installIU, "-destination", destinationRepo.toURL().toExternalForm(), "-profile", "PlatformSDKProfile", "-profileProperties", "org.eclipse.update.install.features=true", "-bundlepool", destinationRepo.getAbsolutePath(), "-roaming"};
		} catch (MalformedURLException e) {
			fail(message, e);
		}
		return args;
	}

	/**
	 * Test the application's behaviour given a single metadata and artifact repository where both are invalid
	 */
	public void testSingleRepoCreationBothInvalid() {
		//Setup: Create the folders
		File metadataRepo = new File(getTempFolder(), "DirectorApp Metadata");
		File artifactRepo = new File(getTempFolder(), "DirectorApp Artifact");
		File destinationRepo = new File(getTempFolder(), "DirectorApp Destination");
		String installIU = "invalidIU";

		//Setup: ensure folders do not exist
		delete(metadataRepo);
		delete(artifactRepo);
		delete(destinationRepo);

		//Setup: use default arguments
		String[] args = getSingleRepoArgs("1.0", metadataRepo, artifactRepo, destinationRepo, installIU);

		StringBuffer outputBuffer = null;
		try {
			outputBuffer = runDirectorApp("1.1", args);
		} catch (ProvisionException e) {
			//expected, fall through
		} catch (Exception e) {
			fail("1.2", e);
		}
		String outputString = outputBuffer.toString();
		assertTrue(outputString.contains("No repository found at " + metadataRepo.toURI().toString()));
		assertTrue(outputString.contains("No repository found at " + artifactRepo.toURI().toString()));

		//remove the agent data produced by the director
		delete(new File(destinationRepo, "p2"));
		//this will only succeed if the destination is empty, which is what we expect because the install failed
		destinationRepo.delete();

		//ensures that repositories have not been mistakenly created
		assertFalse("1.3", metadataRepo.exists());
		assertFalse("1.4", artifactRepo.exists());
		assertFalse("1.5", destinationRepo.exists());

		//Cleanup: delete the folders
		delete(metadataRepo);
		delete(artifactRepo);
		delete(destinationRepo);
	}

	/**
	 * Test the application's behaviour given a single metadata and artifact repository where the metadata repo is invalid
	 */
	public void testSingleRepoCreationMetadataInvalid() {
		//Setup: create repos
		File metadataRepo = new File(getTempFolder(), "DirectorApp Metadata");
		//Valid repository
		File artifactRepo = getTestData("2.0", "/testData/mirror/mirrorSourceRepo1 with space");
		File destinationRepo = new File(getTempFolder(), "DirectorApp Destination");
		String installIU = "invalidIU";

		//Setup: ensure folders do not exist
		delete(metadataRepo);
		delete(destinationRepo);

		//Setup: use default arguments
		String[] args = getSingleRepoArgs("2.1", metadataRepo, artifactRepo, destinationRepo, installIU);

		StringBuffer outputBuffer = null;
		try {
			outputBuffer = runDirectorApp("2.2", args);
		} catch (ProvisionException e) {
			//expected, fall through
		} catch (Exception e) {
			fail("2.3", e);
		}

		assertTrue(outputBuffer.toString().contains("No repository found at " + metadataRepo.toURI().toString()));

		//remove the agent data produced by the director
		delete(new File(destinationRepo, "p2"));
		//this will only succeed if the destination is empty, which is what we expect because the install failed
		destinationRepo.delete();

		//ensures that repository has not been mistakenly created
		assertFalse("2.4", metadataRepo.exists());
		assertTrue("2.5", artifactRepo.exists());
		assertFalse("2.6", destinationRepo.exists());

		//Cleanup: delete the folders
		delete(metadataRepo);
		delete(destinationRepo);
	}

	/**
	 * Test the application's behaviour given a single metadata and artifact repository where the artifact repo is invalid
	 */
	public void testSingleRepoCreationArtifactInvalid() {
		//Setup: create repos
		//Valid repository
		File metadataRepo = getTestData("3.0", "/testData/mirror/mirrorSourceRepo1 with space");
		File artifactRepo = new File(getTempFolder(), "DirectorApp Artifact");
		File destinationRepo = new File(getTempFolder(), "DirectorApp Destination");
		String installIU = "invalidIU";

		//Setup: ensure folders do not exist
		delete(artifactRepo);
		delete(destinationRepo);

		//Setup: use default arguments
		String[] args = getSingleRepoArgs("3.1", metadataRepo, artifactRepo, destinationRepo, installIU);
		StringBuffer outputBuffer = null;
		try {
			outputBuffer = runDirectorApp("3.2", args);
		} catch (ProvisionException e) {
			//expected, fall through
		} catch (Exception e) {
			fail("3.3", e);
		}

		assertTrue(outputBuffer.toString().contains("No repository found at " + artifactRepo.toURI().toString()));

		//remove the agent data produced by the director
		delete(new File(destinationRepo, "p2"));
		//this will only succeed if the destination is empty, which is what we expect because the install failed
		destinationRepo.delete();

		//ensures that repository has not been mistakenly created
		assertFalse("3.4", artifactRepo.exists());
		assertTrue("3.5", metadataRepo.exists());
		assertFalse("3.6", destinationRepo.exists());

		//Cleanup: delete the folders
		delete(artifactRepo);
		delete(destinationRepo);
	}

	/**
	 * Test the application's behaviour given multiple metadata and artifact repositories where all are invalid
	 */
	public void testMultipleRepoCreationAllInvalid() {
		//Setup: Create the folders
		File metadataRepo1 = new File(getTempFolder(), "DirectorApp Metadata1");
		File metadataRepo2 = new File(getTempFolder(), "DirectorApp Metadata2");
		File artifactRepo1 = new File(getTempFolder(), "DirectorApp Artifact1");
		File artifactRepo2 = new File(getTempFolder(), "DirectorApp Artifact2");
		File destinationRepo = new File(getTempFolder(), "DirectorApp Destination");
		String installIU = "invalidIU";

		//Setup: ensure folders do not exist
		delete(metadataRepo1);
		delete(metadataRepo2);
		delete(artifactRepo1);
		delete(artifactRepo2);
		delete(destinationRepo);

		//Setup: create the args
		String[] args = getMultipleRepoArgs("4.0", metadataRepo1, metadataRepo2, artifactRepo1, artifactRepo2, destinationRepo, installIU);

		try {
			runDirectorApp("4.1", args);
		} catch (ProvisionException e) {
			//expected, fall through
		} catch (Exception e) {
			fail("4.3", e);
		}
		//remove the agent data produced by the director
		delete(new File(destinationRepo, "p2"));
		//this will only succeed if the destination is empty, which is what we expect because the install failed
		destinationRepo.delete();

		//ensures that repositories have not been mistakenly created
		assertFalse("4.4", metadataRepo1.exists());
		assertFalse("4.5", metadataRepo2.exists());
		assertFalse("4.6", artifactRepo1.exists());
		assertFalse("4.6", artifactRepo2.exists());
		assertFalse("4.7", destinationRepo.exists());

		//Cleanup: delete the folders
		delete(metadataRepo1);
		delete(metadataRepo2);
		delete(artifactRepo1);
		delete(artifactRepo2);
		delete(destinationRepo);
	}

	/**
	 * Test the application's behaviour given multiple metadata and artifact repositories where both metadata repos are invalid
	 */
	public void testMultipleRepoCreationAllMetadataInvalid() {
		//Setup: Create the folders
		File metadataRepo1 = new File(getTempFolder(), "DirectorApp Metadata1");
		File metadataRepo2 = new File(getTempFolder(), "DirectorApp Metadata2");
		//Valid repositories
		File artifactRepo1 = getTestData("5.0", "/testData/mirror/mirrorSourceRepo1 with space");
		File artifactRepo2 = getTestData("5.1", "/testData/mirror/mirrorSourceRepo2");
		File destinationRepo = new File(getTempFolder(), "DirectorApp Destination");
		String installIU = "invalidIU";

		//Setup: ensure folders do not exist
		delete(metadataRepo1);
		delete(metadataRepo2);
		delete(destinationRepo);

		//Setup: create the args
		String[] args = getMultipleRepoArgs("5.2", metadataRepo1, metadataRepo2, artifactRepo1, artifactRepo2, destinationRepo, installIU);

		try {
			runDirectorApp("5.3", args);
		} catch (ProvisionException e) {
			//expected, fall through
		} catch (Exception e) {
			fail("5.5", e);
		}
		//remove the agent data produced by the director
		delete(new File(destinationRepo, "p2"));
		//this will only succeed if the destination is empty, which is what we expect because the install failed
		destinationRepo.delete();

		//ensures that repositories have not been mistakenly created
		assertFalse("5.6", metadataRepo1.exists());
		assertFalse("5.7", metadataRepo2.exists());
		assertTrue("5.8", artifactRepo1.exists());
		assertTrue("5.9", artifactRepo2.exists());
		assertFalse("5.10", destinationRepo.exists());

		//Cleanup: delete the folders
		delete(metadataRepo1);
		delete(metadataRepo2);
		delete(destinationRepo);
	}

	/**
	 * Test the application's behaviour given multiple metadata and artifact repositories where only one metadata repo is invalid
	 * Note: this test should end with "The installable unit invalidIU has not been found."
	 */
	public void testMultipleRepoCreationOneMetadataInvalid() {
		//Setup: Create the folders
		File metadataRepo1 = new File(getTempFolder(), "DirectorApp Metadata1");
		//Valid repositories
		File metadataRepo2 = getTestData("6.0", "/testData/mirror/mirrorSourceRepo1 with space");
		File artifactRepo1 = getTestData("6.1", "/testData/mirror/mirrorSourceRepo1 with space");
		File artifactRepo2 = getTestData("6.2", "/testData/mirror/mirrorSourceRepo2");
		File destinationRepo = new File(getTempFolder(), "DirectorApp Destination");
		String installIU = "invalidIU";

		//Setup: ensure folders do not exist
		delete(metadataRepo1);
		delete(destinationRepo);

		//Setup: create the args
		String[] args = getMultipleRepoArgs("6.3", metadataRepo1, metadataRepo2, artifactRepo1, artifactRepo2, destinationRepo, installIU);

		try {
			runDirectorApp("6.4", args);
		} catch (ProvisionException e) {
			fail("6.5", e);
		} catch (Exception e) {
			fail("6.6", e);
		}
		//remove the agent data produced by the director
		delete(new File(destinationRepo, "p2"));
		//this will only succeed if the destination is empty, which is what we expect because the install failed
		destinationRepo.delete();

		//ensures that repositories have not been mistakenly created
		assertFalse("6.7", metadataRepo1.exists());
		assertTrue("6.8", metadataRepo2.exists());
		assertTrue("6.9", artifactRepo1.exists());
		assertTrue("6.10", artifactRepo2.exists());
		assertFalse("6.11", destinationRepo.exists());

		//Cleanup: delete the folders
		delete(metadataRepo1);
		delete(destinationRepo);
	}

	/**
	 * Test the application's behaviour given multiple metadata and artifact repositories where both artifact repos are invalid
	 */
	public void testMultipleRepoCreationAllArtifactInvalid() {
		//Setup: Create the folders
		File artifactRepo1 = new File(getTempFolder(), "DirectorApp Artifact1");
		File artifactRepo2 = new File(getTempFolder(), "DirectorApp Artifact2");
		//Valid repositories
		File metadataRepo1 = getTestData("7.0", "/testData/mirror/mirrorSourceRepo1 with space");
		File metadataRepo2 = getTestData("7.1", "/testData/mirror/mirrorSourceRepo2");
		File destinationRepo = new File(getTempFolder(), "DirectorApp Destination");
		String installIU = "invalidIU";

		//Setup: ensure folders do not exist
		delete(artifactRepo1);
		delete(artifactRepo2);
		delete(destinationRepo);

		//Setup: create the args
		String[] args = getMultipleRepoArgs("7.2", metadataRepo1, metadataRepo2, artifactRepo1, artifactRepo2, destinationRepo, installIU);

		try {
			runDirectorApp("7.3", args);
		} catch (ProvisionException e) {
			//expected, fall through
		} catch (Exception e) {
			fail("7.5", e);
		}
		//remove the agent data produced by the director
		delete(new File(destinationRepo, "p2"));
		//this will only succeed if the destination is empty, which is what we expect because the install failed
		destinationRepo.delete();

		//ensures that repositories have not been mistakenly created
		assertTrue("7.6", metadataRepo1.exists());
		assertTrue("7.7", metadataRepo2.exists());
		assertFalse("7.8", artifactRepo1.exists());
		assertFalse("7.9", artifactRepo2.exists());
		assertFalse("7.10", destinationRepo.exists());

		//Cleanup: delete the folders
		delete(artifactRepo1);
		delete(artifactRepo2);
		delete(destinationRepo);
	}

	/**
	 * Test the application's behaviour given multiple metadata and artifact repositories where only one artifact repo is invalid
	 * Note: this test should end with "The installable unit invalidIU has not been found."
	 */
	public void testMultipleRepoCreationOneArtifactInvalid() {
		//Setup: Create the folders
		File artifactRepo1 = new File(getTempFolder(), "DirectorApp Artifact1");
		//Valid repositories
		File artifactRepo2 = getTestData("8.0", "/testData/mirror/mirrorSourceRepo1 with space");
		File metadataRepo1 = getTestData("8.1", "/testData/mirror/mirrorSourceRepo1 with space");
		File metadataRepo2 = getTestData("8.2", "/testData/mirror/mirrorSourceRepo2");
		File destinationRepo = new File(getTempFolder(), "DirectorApp Destination");
		String installIU = "invalidIU";

		//Setup: ensure folders do not exist
		delete(artifactRepo1);
		delete(destinationRepo);

		//Setup: create the args
		String[] args = getMultipleRepoArgs("8.3", metadataRepo1, metadataRepo2, artifactRepo1, artifactRepo2, destinationRepo, installIU);

		try {
			runDirectorApp("8.4", args);
		} catch (ProvisionException e) {
			fail("8.5", e);
		} catch (Exception e) {
			fail("8.6", e);
		}
		//remove the agent data produced by the director
		delete(new File(destinationRepo, "p2"));
		//this will only succeed if the destination is empty, which is what we expect because the install failed
		destinationRepo.delete();
		//remove the agent data produced by the director
		delete(new File(destinationRepo, "p2"));
		//this will only succeed if the destination is empty, which is what we expect because the install failed
		destinationRepo.delete();

		//ensures that repositories have not been mistakenly created
		assertTrue("8.7", metadataRepo1.exists());
		assertTrue("8.8", metadataRepo2.exists());
		assertFalse("8.9", artifactRepo1.exists());
		assertTrue("8.10", artifactRepo2.exists());
		assertFalse("8.11", destinationRepo.exists());

		//Cleanup: delete the folders
		delete(artifactRepo1);
		delete(destinationRepo);
	}

	/**
	 * Test the application's behaviour given multiple metadata and artifact repositories where only one artifact repo and only one metadata repo are invalid
	 * Note: this test should end with "The installable unit invalidIU has not been found."
	 */
	public void testMultipleRepoCreationOneArtifactOneMetadataInvalid() {
		//Setup: Create the folders
		File artifactRepo1 = new File(getTempFolder(), "DirectorApp Artifact1");
		File metadataRepo1 = new File(getTempFolder(), "DirectorApp Metadata1");
		//Valid repositories
		File artifactRepo2 = getTestData("9.0", "/testData/mirror/mirrorSourceRepo1 with space");
		File metadataRepo2 = getTestData("9.1", "/testData/mirror/mirrorSourceRepo1 with space");
		File destinationRepo = new File(getTempFolder(), "DirectorApp Destination");
		String installIU = "invalidIU";

		//Setup: ensure folders do not exist
		delete(artifactRepo1);
		delete(destinationRepo);

		//Setup: create the args
		String[] args = getMultipleRepoArgs("9.2", metadataRepo1, metadataRepo2, artifactRepo1, artifactRepo2, destinationRepo, installIU);

		try {
			runDirectorApp("9.3", args);
		} catch (ProvisionException e) {
			fail("9.4", e);
		} catch (Exception e) {
			fail("9.5", e);
		}
		//remove the agent data produced by the director
		delete(new File(destinationRepo, "p2"));
		//this will only succeed if the destination is empty, which is what we expect because the install failed
		destinationRepo.delete();

		//ensures that repositories have not been mistakenly created
		assertFalse("9.6", metadataRepo1.exists());
		assertTrue("9.7", metadataRepo2.exists());
		assertFalse("9.8", artifactRepo1.exists());
		assertTrue("9.9", artifactRepo2.exists());
		assertFalse("9.10", destinationRepo.exists());

		//Cleanup: delete the folders
		delete(artifactRepo1);
		delete(metadataRepo1);
		delete(destinationRepo);
	}

	/**
	 * Test the application's behaviour given a single metadata and a single artifact repository where all are valid
	 * Note: this test should end with "The installable unit invalidIU has not been found."
	 */
	public void testSingleRepoCreationNoneInvalid() {
		//Setup: get repositories
		File artifactRepo = getTestData("10.0", "/testData/mirror/mirrorSourceRepo1 with space");
		File metadataRepo = getTestData("10.1", "/testData/mirror/mirrorSourceRepo1 with space");
		File destinationRepo = new File(getTempFolder(), "DirectorApp Destination");
		String installIU = "invalidIU";

		//Setup: ensure folders do not exist
		delete(destinationRepo);

		//Setup: create the args
		String[] args = getSingleRepoArgs("10.2", metadataRepo, artifactRepo, destinationRepo, installIU);

		try {
			runDirectorApp("10.3", args);
		} catch (ProvisionException e) {
			fail("10.4", e);
		} catch (Exception e) {
			fail("10.5", e);
		}
		//remove the agent data produced by the director
		delete(new File(destinationRepo, "p2"));
		//this will only succeed if the destination is empty, which is what we expect because the install failed
		destinationRepo.delete();

		//ensures that repositories have not been mistakenly created
		assertTrue("10.6", metadataRepo.exists());
		assertTrue("10.7", artifactRepo.exists());
		assertFalse("10.8", destinationRepo.exists());

		//Cleanup: delete the folders
		delete(destinationRepo);
	}

	/**
	 * Test the application's behaviour given multiple metadata and artifact repositories where all repos are valid
	 * Note: this test should end with "The installable unit invalidIU has not been found."
	 */
	public void testMultipleRepoCreationNoneInvalid() {
		//Setup: Create the folders
		//Valid repositories
		File artifactRepo1 = getTestData("11.0", "/testData/mirror/mirrorSourceRepo1 with space");
		File metadataRepo1 = getTestData("11.1", "/testData/mirror/mirrorSourceRepo1 with space");
		File artifactRepo2 = getTestData("11.2", "/testData/mirror/mirrorSourceRepo2");
		File metadataRepo2 = getTestData("11.3", "/testData/mirror/mirrorSourceRepo2");
		File destinationRepo = new File(getTempFolder(), "DirectorApp Destination");
		String installIU = "invalidIU";

		//Setup: ensure folders do not exist
		delete(destinationRepo);

		//Setup: create the args
		String[] args = getMultipleRepoArgs("11.4", metadataRepo1, metadataRepo2, artifactRepo1, artifactRepo2, destinationRepo, installIU);

		try {
			runDirectorApp("11.5", args);
		} catch (ProvisionException e) {
			fail("11.6", e);
		} catch (Exception e) {
			fail("11.7", e);
		}
		//remove the agent data produced by the director
		delete(new File(destinationRepo, "p2"));
		//this will only succeed if the destination is empty, which is what we expect because the install failed
		destinationRepo.delete();

		//ensures that repositories have not been mistakenly created
		assertTrue("11.8", metadataRepo1.exists());
		assertTrue("11.9", metadataRepo2.exists());
		assertTrue("11.10", artifactRepo1.exists());
		assertTrue("11.11", artifactRepo2.exists());
		assertFalse("11.12", destinationRepo.exists());

		//Cleanup: delete the folders
		delete(destinationRepo);
	}

	/** 
	 * Test that the application only considers repositories that are pass in and not those that are previously known
	 * by the managers
	 */
	public void testOnlyUsePassedInRepos() throws Exception {
		File artifactRepo1 = getTestData("12.0", "/testData/mirror/mirrorSourceRepo3");
		File metadataRepo1 = getTestData("12.1", "/testData/mirror/mirrorSourceRepo3");

		IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) getAgent().getService(IArtifactRepositoryManager.SERVICE_NAME);
		IMetadataRepositoryManager metadataManager = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
		assertNotNull(artifactManager);
		assertNotNull(metadataManager);

		//make repo3 known to the managers
		artifactManager.loadRepository(artifactRepo1.toURI(), new NullProgressMonitor());
		metadataManager.loadRepository(metadataRepo1.toURI(), new NullProgressMonitor());

		final URI[] knownArtifactRepos = artifactManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		final URI[] knownMetadataRepos = metadataManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);

		File artifactRepo2 = getTestData("12.2", "/testData/mirror/mirrorSourceRepo4");
		File metadataRepo2 = getTestData("12.3", "/testData/mirror/mirrorSourceRepo4");
		File destinationRepo = new File(getTempFolder(), "DirectorApp Destination");
		String[] args = getSingleRepoArgs("12.4", metadataRepo2, artifactRepo2, destinationRepo, "yetanotherplugin");

		destinationRepo.mkdirs();

		StringBuffer buffer = runDirectorApp("12.5", args);
		assertTrue(buffer.toString().contains("The installable unit yetanotherplugin has not been found."));

		final URI[] afterArtifactRepos = artifactManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		final URI[] afterMetadataRepos = metadataManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		assertEquals(knownMetadataRepos.length, afterMetadataRepos.length);
		assertEquals(knownArtifactRepos.length, afterArtifactRepos.length);

		artifactManager.removeRepository(artifactRepo1.toURI());
		metadataManager.removeRepository(metadataRepo1.toURI());
		delete(destinationRepo);
	}

	/**
	 * Test the ProvisioningContext only uses the passed in repos and not all known repos.
	 * Expect to install helloworld_1.0.0 not helloworld_1.0.1
	 * @throws Exception
	 */
	public void testPassedInRepos_ProvisioningContext() throws Exception {
		File artifactRepo1 = getTestData("13.0", "/testData/mirror/mirrorSourceRepo4");
		File metadataRepo1 = getTestData("13.1", "/testData/mirror/mirrorSourceRepo4");

		IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) getAgent().getService(IArtifactRepositoryManager.SERVICE_NAME);
		IMetadataRepositoryManager metadataManager = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
		assertNotNull(artifactManager);
		assertNotNull(metadataManager);

		//make repo4 known to the managers
		artifactManager.loadRepository(artifactRepo1.toURI(), new NullProgressMonitor());
		metadataManager.loadRepository(metadataRepo1.toURI(), new NullProgressMonitor());

		File artifactRepo2 = getTestData("13.2", "/testData/mirror/mirrorSourceRepo3");
		File metadataRepo2 = getTestData("13.3", "/testData/mirror/mirrorSourceRepo3");
		File destinationRepo = new File(getTempFolder(), "DirectorApp Destination");
		String[] args = getSingleRepoArgs("13.4", metadataRepo2, artifactRepo2, destinationRepo, "helloworld");

		destinationRepo.mkdirs();
		StringBuffer buffer = runDirectorApp("13.5", args);
		assertTrue(buffer.toString().contains("Installing helloworld 1.0.0."));

		artifactManager.removeRepository(artifactRepo1.toURI());
		metadataManager.removeRepository(metadataRepo1.toURI());
		delete(destinationRepo);
	}

	public void testDownloadOnlyFlag() {
		//Setup: get repositories
		File artifactRepo = getTestData("testDownloadOnly", "/testData/testRepos/sitewithnestedfeatures");
		File metadataRepo = getTestData("testDownloadOnly", "/testData/testRepos/sitewithnestedfeatures");
		File destinationRepo = new File(getTempFolder(), "DirectorApp Destination");
		String installIU = "fff.feature.group";

		//Setup: ensure folders do not exist
		delete(destinationRepo);

		//Setup: create the args
		String[] args = getSingleRepoArgs("testDownloadOnly", metadataRepo, artifactRepo, destinationRepo, installIU, "-downloadOnly");

		try {
			StringBuffer buffer = runDirectorApp("testDownloadOnly", args);
			assertTrue(buffer.toString().contains("Installing fff.feature.group 1.0.0."));
		} catch (Exception e) {
			fail("fail", e);
		}

		assertTrue(destinationRepo.exists());

		File features = new File(destinationRepo, "features");
		assertTrue(features.exists());
		File feature = new File(features, "fff_1.0.0");
		assertTrue(feature.exists());
		File plugins = new File(destinationRepo, "plugins");
		assertTrue(plugins.exists());
		File plugin1 = new File(plugins, "aaa_1.0.0.jar");
		assertTrue(plugin1.exists());
		File plugin2 = new File(plugins, "ccc_1.0.0");
		assertTrue(plugin2.exists());
	}

	public void testListFormatMissingListArgument() throws Exception {
		//Setup: get repositories
		File artifactRepo = getTestData("testListFormatMissingListArgument", "/testData/testRepos/updateSite");
		File metadataRepo = getTestData("testListFormatMissingListArgument", "/testData/testRepos/updateSite");

		//Setup: create the args
		String[] args = getSingleRepoArgsForListing("testListFormatMissingListArgument", metadataRepo, artifactRepo, "", "", "-listFormat", "%i=%v,%d");

		StringBuffer buffer = runDirectorApp("testListFormatMissingListArgument", args);
		assertThat(buffer.toString(), containsString("-listFormat requires"));
	}

	public void testListFormat() throws Exception {
		//Setup: get repositories
		File artifactRepo = getTestData("testListFormat", "/testData/testRepos/updateSite");
		File metadataRepo = getTestData("testListFormat", "/testData/testRepos/updateSite");

		//Setup: create the args
		String[] args = getSingleRepoArgsForListing("testListFormat", metadataRepo, artifactRepo, "-list", "", "-listFormat", "${id}_${version},${id},${org.eclipse.equinox.p2.name}");

		StringBuffer buffer = runDirectorApp("testListFormat", args);
		assertThat(buffer.toString(), containsString("org.eclipse.ui.examples.job_3.0.0,org.eclipse.ui.examples.job,Progress Examples Plug-in"));
	}

	public void testListNoExplicitFormat() throws Exception {
		//Setup: get repositories
		File artifactRepo = getTestData("testListNoExplicitFormat", "/testData/testRepos/updateSite");
		File metadataRepo = getTestData("testListNoExplicitFormat", "/testData/testRepos/updateSite");

		//Setup: create the args
		String[] args = getSingleRepoArgsForListing("testListNoExplicitFormat", metadataRepo, artifactRepo, "-list", "", "", "");

		StringBuffer buffer = runDirectorApp("testListNoExplicitFormat", args);
		assertThat(buffer.toString(), containsString("org.eclipse.ui.examples.job=3.0.0"));
	}

	/**
	 * Test the ProvisioningContext only uses the passed in repos and not all known repos.
	 * Expect to install helloworld_1.0.0 not helloworld_1.0.1
	 * @throws Exception
	 */
	public void testUninstallIgnoresPassedInRepos() throws Exception {
		File srcRepo = getTestData("14.0", "/testData/mirror/mirrorSourceRepo4");

		IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) getAgent().getService(IArtifactRepositoryManager.SERVICE_NAME);
		IMetadataRepositoryManager metadataManager = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
		assertNotNull(artifactManager);
		assertNotNull(metadataManager);

		File destinationRepo = new File(getTempFolder(), "DirectorApp Destination");
		String[] args = getSingleRepoUninstallArgs("14.1", srcRepo, destinationRepo, "helloworld");

		destinationRepo.mkdirs();

		StringBuffer buffer = runDirectorApp("14.2", args);
		assertTrue(buffer.toString().contains("The installable unit helloworld has not been found."));

		artifactManager.removeRepository(srcRepo.toURI());
		metadataManager.removeRepository(srcRepo.toURI());
		delete(destinationRepo);
	}

	private final class DummyCertificate extends Certificate {
		DummyCertificate(String type) {
			super(type);
		}

		@Override
		public void verify(PublicKey key, String sigProvider) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
			//
		}

		@Override
		public void verify(PublicKey key) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
			//
		}

		@Override
		public String toString() {
			return null;
		}

		@Override
		public PublicKey getPublicKey() {
			return null;
		}

		@Override
		public byte[] getEncoded() throws CertificateEncodingException {
			return null;
		}
	}

	public void testAvoidTrustPromptServiceNoUntrustedCertificates() {
		final TrustInfo trustInfo = getTrustInfoFor(null);
		assertNotNull(trustInfo);
		assertNull(trustInfo.getTrustedCertificates());
	}

	public void testAvoidTrustPromptServiceTrustsOneCertificate() {
		final Certificate certificate = new DummyCertificate(""); //$NON-NLS-1$
		final TrustInfo trustInfo = getTrustInfoFor(new Certificate[][] {{certificate}});
		assertNotNull(trustInfo);
		final Certificate[] trustedCertificates = trustInfo.getTrustedCertificates();
		assertEquals(1, trustedCertificates.length);
		assertSame(certificate, trustedCertificates[0]);
	}

	public void testAvoidTrustPromptServiceTrustsManyCertificates() {
		final Certificate certificate1 = new DummyCertificate(""); //$NON-NLS-1$
		final Certificate certificate2 = new DummyCertificate(""); //$NON-NLS-1$
		final TrustInfo trustInfo = getTrustInfoFor(new Certificate[][] { {certificate1}, {certificate2}});
		assertNotNull(trustInfo);
		final Certificate[] trustedCertificates = trustInfo.getTrustedCertificates();
		assertEquals(2, trustedCertificates.length);
		assertSame(certificate1, trustedCertificates[0]);
		assertSame(certificate2, trustedCertificates[1]);
	}

	private TrustInfo getTrustInfoFor(final Certificate[][] untrustedChain) {
		UIServices avoidTrustPromptService = new DirectorApplication.AvoidTrustPromptService();
		return avoidTrustPromptService.getTrustInfo(untrustedChain, null);
	}

}
