/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
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
import java.io.PrintStream;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.tools.MirrorApplication;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.StringBufferStream;

/**
 * Test to ensure MirrorApplication handles loading an removing repositories correctly
 */
public class ArtifactRepositoryCleanupTest extends AbstractProvisioningTest {
	protected File destRepoLocation;
	protected File sourceRepoLocation; //helloworldfeature
	protected File bystanderRepoLocation; //anotherfeature

	protected void setUp() throws Exception {
		super.setUp();
		//load all the repositories
		sourceRepoLocation = getTestData("1.0", "/testData/mirror/mirrorSourceRepo1 with space");
		bystanderRepoLocation = getTestData("2.0", "/testData/mirror/mirrorSourceRepo2");

		//create destination location
		String tempDir = System.getProperty("java.io.tmpdir");
		destRepoLocation = new File(tempDir, "BasicMirrorApplicationTest");
		AbstractProvisioningTest.delete(destRepoLocation);
	}

	protected void tearDown() throws Exception {
		//remove all the repositories
		getArtifactRepositoryManager().removeRepository(destRepoLocation.toURI());
		getArtifactRepositoryManager().removeRepository(sourceRepoLocation.toURI());
		getArtifactRepositoryManager().removeRepository(bystanderRepoLocation.toURI());

		//delete the destination location (no left over files for the next test)
		delete(destRepoLocation);
		super.tearDown();
	}

	/**
	 * runs default mirror. source is the source repo, destination is the destination repo
	 */
	private void runMirrorApplication(final File source, final File destination, final boolean append) throws Exception {
		MirrorApplication application = new MirrorApplication();
		String[] args = new String[] {"-source", source.toURL().toExternalForm(), "-destination", destination.toURL().toExternalForm(), append ? "-append" : ""};
		application.initializeFromArguments(args);

		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			application.run(null);
		} finally {
			System.setOut(out);
		}
	}

	/**
	 * Ensures that if the mirror application is run with neither source nor destination loaded
	 * then neither will remain loaded after the application finishes
	 */
	public void testArtifactMirrorRemovesRepos() {
		try {
			runMirrorApplication(sourceRepoLocation, destRepoLocation, true);
		} catch (Exception e) {
			fail("1.0", e);
		}

		assertFalse(getArtifactRepositoryManager().contains(sourceRepoLocation.toURI()));
		assertFalse(getArtifactRepositoryManager().contains(destRepoLocation.toURI()));
	}

	/**
	 * Ensures that if the mirror application is run with only the source loaded
	 * then the source, and only the source, remains loaded after the application finishes
	 */
	public void testArtifactMirrorRemovesReposWithSourceLoaded() {
		try {
			//Load the source
			getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null);
		} catch (ProvisionException e) {
			fail("2.0", e);
		}

		try {
			runMirrorApplication(sourceRepoLocation, destRepoLocation, true);
		} catch (Exception e) {
			fail("2.2", e);
		}

		assertTrue(getArtifactRepositoryManager().contains(sourceRepoLocation.toURI()));
		assertFalse(getArtifactRepositoryManager().contains(destRepoLocation.toURI()));
	}

	/**
	 * Ensures that if the mirror application is run with only the destination loaded
	 * then the destination, and only the destination, remains loaded after the application finishes
	 */
	public void testArtifactMirrorRemovesReposWithDestinationLoaded() {
		try {
			//Load (by creating) the destination
			String repositoryName = destRepoLocation.toURI() + " - artifacts"; //$NON-NLS-1$
			getArtifactRepositoryManager().createRepository(destRepoLocation.toURI(), repositoryName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		} catch (ProvisionException e) {
			fail("3.0", e);
		}

		try {
			runMirrorApplication(sourceRepoLocation, destRepoLocation, true);
		} catch (Exception e) {
			fail("3.2", e);
		}

		assertTrue(getArtifactRepositoryManager().contains(destRepoLocation.toURI()));
		assertFalse(getArtifactRepositoryManager().contains(sourceRepoLocation.toURI()));
	}

	/**
	 * Ensures that is the mirror application is run with both the source and destination loaded
	 * then both will remain loaded after the application has finished
	 */
	public void testArtifactMirrorRemovesReposWithBothLoaded() {
		try {
			//Load (by creating) the destination
			String repositoryName = destRepoLocation.toURI() + " - artifacts"; //$NON-NLS-1$
			getArtifactRepositoryManager().createRepository(destRepoLocation.toURI(), repositoryName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
			//Load the source
			getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null);
		} catch (ProvisionException e) {
			fail("4.0", e);
		}

		try {
			runMirrorApplication(sourceRepoLocation, destRepoLocation, true);
		} catch (Exception e) {
			fail("4.2", e);
		}

		assertTrue(getArtifactRepositoryManager().contains(destRepoLocation.toURI()));
		assertTrue(getArtifactRepositoryManager().contains(sourceRepoLocation.toURI()));
	}

	/**
	 * Ensures that if the mirror application is run with neither source nor destination loaded
	 * then neither will remain loaded after the application finishes
	 * Also ensures that the mirror application does not alter the state of any repository other
	 * than the source or destination
	 */
	public void testArtifactMirrorRemovesReposWithBystanderLoaded() {
		try {
			//Load the bystander repository. This should not be effected by the mirror application
			getArtifactRepositoryManager().loadRepository(bystanderRepoLocation.toURI(), null);
		} catch (ProvisionException e) {
			fail("5.0", e);
		}

		try {
			runMirrorApplication(sourceRepoLocation, destRepoLocation, true);
		} catch (Exception e) {
			fail("5.2", e);
		}

		assertFalse(getArtifactRepositoryManager().contains(sourceRepoLocation.toURI()));
		assertFalse(getArtifactRepositoryManager().contains(destRepoLocation.toURI()));
		//Ensure bystander was not effected by the mirror application
		assertTrue(getArtifactRepositoryManager().contains(bystanderRepoLocation.toURI()));
	}

	/**
	 * Ensures that if the mirror application is run with only the source loaded
	 * then the source, and only the source, remains loaded after the application finishes
	 * Also ensures that the mirror application does not alter the state of any repository other
	 * than the source or destination
	 */
	public void testArtifactMirrorRemovesReposWithSourceAndBystanderLoaded() {
		try {
			//Load the bystander repository. This should not be effected by the mirror application
			getArtifactRepositoryManager().loadRepository(bystanderRepoLocation.toURI(), null);
			//Load the source
			getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null);
		} catch (ProvisionException e) {
			fail("6.0", e);
		}

		try {
			runMirrorApplication(sourceRepoLocation, destRepoLocation, true);
		} catch (Exception e) {
			fail("6.2", e);
		}

		assertTrue(getArtifactRepositoryManager().contains(sourceRepoLocation.toURI()));
		assertFalse(getArtifactRepositoryManager().contains(destRepoLocation.toURI()));
		//Ensure bystander was not effected by the mirror application
		assertTrue(getArtifactRepositoryManager().contains(bystanderRepoLocation.toURI()));
	}

	/**
	 * Ensures that if the mirror application is run with the destination loaded
	 * then the destination, and only the destination, remains loaded after the application finishes
	 * Also ensures that the mirror application does not alter the state of any repository other
	 * than the source or destination
	 */
	public void testArtifactMirrorRemovesReposWithDestinationAndBystanderLoaded() {
		try {
			//Load the bystander repository. This should not be effected by the mirror application
			getArtifactRepositoryManager().loadRepository(bystanderRepoLocation.toURI(), null);
			//Load (by creating) the destination
			String repositoryName = destRepoLocation.toURI() + " - artifacts"; //$NON-NLS-1$
			getArtifactRepositoryManager().createRepository(destRepoLocation.toURI(), repositoryName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		} catch (ProvisionException e) {
			fail("7.0", e);
		}

		try {
			runMirrorApplication(sourceRepoLocation, destRepoLocation, true);
		} catch (Exception e) {
			fail("7.2", e);
		}

		assertTrue(getArtifactRepositoryManager().contains(destRepoLocation.toURI()));
		assertFalse(getArtifactRepositoryManager().contains(sourceRepoLocation.toURI()));
		//Ensure bystander was not effected by the mirror application
		assertTrue(getArtifactRepositoryManager().contains(bystanderRepoLocation.toURI()));
	}

	/**
	 * Ensures that is the mirror application is run with both the source and destination loaded
	 * then both will remain loaded after the application has finished
	 * Also ensures that the mirror application does not alter the state of any repository other
	 * than the source or destination
	 */
	public void testArtifactMirrorRemovesReposWithBothAndBystanderLoaded() {
		try {
			//Load the bystander repository. This should not be effected by the mirror application
			getArtifactRepositoryManager().loadRepository(bystanderRepoLocation.toURI(), null);
			//Load (by creating) the destination
			String repositoryName = destRepoLocation.toURI() + " - artifacts"; //$NON-NLS-1$
			getArtifactRepositoryManager().createRepository(destRepoLocation.toURI(), repositoryName, IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
			//Load the source
			getArtifactRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null);
		} catch (ProvisionException e) {
			fail("8.0", e);
		}

		try {
			runMirrorApplication(sourceRepoLocation, destRepoLocation, true);
		} catch (Exception e) {
			fail("8.2", e);
		}

		assertTrue(getArtifactRepositoryManager().contains(destRepoLocation.toURI()));
		assertTrue(getArtifactRepositoryManager().contains(sourceRepoLocation.toURI()));
		//Ensure bystander was not effected by the mirror application
		assertTrue(getArtifactRepositoryManager().contains(bystanderRepoLocation.toURI()));
	}
}
