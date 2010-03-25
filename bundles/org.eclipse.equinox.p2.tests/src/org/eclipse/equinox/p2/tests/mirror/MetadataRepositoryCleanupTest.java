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
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.StringBufferStream;

/**
 * Test to ensure MirrorApplication handles loading an removing repositories correctly
 */
public class MetadataRepositoryCleanupTest extends AbstractProvisioningTest {
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
		getMetadataRepositoryManager().removeRepository(destRepoLocation.toURI());
		getMetadataRepositoryManager().removeRepository(sourceRepoLocation.toURI());
		getMetadataRepositoryManager().removeRepository(bystanderRepoLocation.toURI());

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
	public void testMetadataMirrorRemovesRepos() {
		try {
			runMirrorApplication(sourceRepoLocation, destRepoLocation, true);
		} catch (Exception e) {
			fail("1.0", e);
		}

		assertFalse(getMetadataRepositoryManager().contains(sourceRepoLocation.toURI()));
		assertFalse(getMetadataRepositoryManager().contains(destRepoLocation.toURI()));
	}

	/**
	 * Ensures that if the mirror application is run with only the source loaded
	 * then the source, and only the source, remains loaded after the application finishes
	 */
	public void testMetadataMirrorRemovesReposWithSourceLoaded() {
		try {
			//Load the source
			getMetadataRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null);
		} catch (ProvisionException e) {
			fail("2.0", e);
		}

		try {
			runMirrorApplication(sourceRepoLocation, destRepoLocation, true);
		} catch (Exception e) {
			fail("2.2", e);
		}

		assertTrue(getMetadataRepositoryManager().contains(sourceRepoLocation.toURI()));
		assertFalse(getMetadataRepositoryManager().contains(destRepoLocation.toURI()));
	}

	/**
	 * Ensures that if the mirror application is run with only the destination loaded
	 * then the destination, and only the destination, remains loaded after the application finishes
	 */
	public void testMetadataMirrorRemovesReposWithDestinationLoaded() {
		try {
			//Load (by creating) the destination
			String repositoryName = destRepoLocation.toURI() + " - metadata"; //$NON-NLS-1$
			getMetadataRepositoryManager().createRepository(destRepoLocation.toURI(), repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		} catch (ProvisionException e) {
			fail("3.0", e);
		}

		try {
			runMirrorApplication(sourceRepoLocation, destRepoLocation, true);
		} catch (Exception e) {
			fail("3.2", e);
		}

		assertTrue(getMetadataRepositoryManager().contains(destRepoLocation.toURI()));
		assertFalse(getMetadataRepositoryManager().contains(sourceRepoLocation.toURI()));
	}

	/**
	 * Ensures that is the mirror application is run with both the source and destination loaded
	 * then both will remain loaded after the application has finished
	 */
	public void testMetadataMirrorRemovesReposWithBothLoaded() {
		try {
			//Load (by creating) the destination
			String repositoryName = destRepoLocation.toURI() + " - metadata"; //$NON-NLS-1$
			getMetadataRepositoryManager().createRepository(destRepoLocation.toURI(), repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
			//Load the source
			getMetadataRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null);
		} catch (ProvisionException e) {
			fail("4.0", e);
		}

		try {
			runMirrorApplication(sourceRepoLocation, destRepoLocation, true);
		} catch (Exception e) {
			fail("4.2", e);
		}

		assertTrue(getMetadataRepositoryManager().contains(destRepoLocation.toURI()));
		assertTrue(getMetadataRepositoryManager().contains(sourceRepoLocation.toURI()));
	}

	/**
	 * Ensures that if the mirror application is run with neither source nor destination loaded
	 * then neither will remain loaded after the application finishes
	 * Also ensures that the mirror application does not alter the state of any repository other
	 * than the source or destination
	 */
	public void testMetadataMirrorRemovesReposWithBystanderLoaded() {
		//Load the bystander repository. This should not be effected by the mirror application
		try {
			getMetadataRepositoryManager().loadRepository(bystanderRepoLocation.toURI(), null);
		} catch (ProvisionException e) {
			// TODO Auto-generated catch block
			fail("5.0", e);
		}

		try {
			runMirrorApplication(sourceRepoLocation, destRepoLocation, true);
		} catch (Exception e) {
			fail("5.2", e);
		}

		assertFalse(getMetadataRepositoryManager().contains(sourceRepoLocation.toURI()));
		assertFalse(getMetadataRepositoryManager().contains(destRepoLocation.toURI()));
		//Ensure bystander was not effected by the mirror application
		assertTrue(getMetadataRepositoryManager().contains(bystanderRepoLocation.toURI()));
	}

	/**
	 * Ensures that if the mirror application is run with only the source loaded
	 * then the source, and only the source, remains loaded after the application finishes
	 * Also ensures that the mirror application does not alter the state of any repository other
	 * than the source or destination
	 */
	public void testMetadataMirrorRemovesReposWithSourceAndBystanderLoaded() {
		try {
			//Load the bystander repository. This should not be effected by the mirror application
			getMetadataRepositoryManager().loadRepository(bystanderRepoLocation.toURI(), null);
			//Load the source
			getMetadataRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null);
		} catch (ProvisionException e) {
			fail("6.0", e);
		}

		try {
			runMirrorApplication(sourceRepoLocation, destRepoLocation, true);
		} catch (Exception e) {
			fail("6.2", e);
		}

		assertTrue(getMetadataRepositoryManager().contains(sourceRepoLocation.toURI()));
		assertFalse(getMetadataRepositoryManager().contains(destRepoLocation.toURI()));
		//Ensure bystander was not effected by the mirror application
		assertTrue(getMetadataRepositoryManager().contains(bystanderRepoLocation.toURI()));
	}

	/**
	 * Ensures that if the mirror application is run with the destination loaded
	 * then the destination, and only the destination, remains loaded after the application finishes
	 * Also ensures that the mirror application does not alter the state of any repository other
	 * than the source or destination
	 */
	public void testMetadataMirrorRemovesReposWithDestinationAndBystanderLoaded() {
		try {
			//Load the bystander repository. This should not be effected by the mirror application
			getMetadataRepositoryManager().loadRepository(bystanderRepoLocation.toURI(), null);
			//Load (by creating) the destination
			String repositoryName = destRepoLocation.toURI() + " - metadata"; //$NON-NLS-1$
			getMetadataRepositoryManager().createRepository(destRepoLocation.toURI(), repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		} catch (ProvisionException e) {
			fail("7.0", e);
		}

		try {
			runMirrorApplication(sourceRepoLocation, destRepoLocation, true);
		} catch (Exception e) {
			fail("7.2", e);
		}

		assertTrue(getMetadataRepositoryManager().contains(destRepoLocation.toURI()));
		assertFalse(getMetadataRepositoryManager().contains(sourceRepoLocation.toURI()));
		//Ensure bystander was not effected by the mirror application
		assertTrue(getMetadataRepositoryManager().contains(bystanderRepoLocation.toURI()));
	}

	/**
	 * Ensures that is the mirror application is run with both the source and destination loaded
	 * then both will remain loaded after the application has finished
	 * Also ensures that the mirror application does not alter the state of any repository other
	 * than the source or destination
	 */
	public void testMetadataMirrorRemovesReposWithBothAndBystanderLoaded() {
		try {
			//Load the bystander repository. This should not be effected by the mirror application
			getMetadataRepositoryManager().loadRepository(bystanderRepoLocation.toURI(), null);
			//Load (by creating) the destination
			String repositoryName = destRepoLocation.toURI() + " - metadata"; //$NON-NLS-1$
			getMetadataRepositoryManager().createRepository(destRepoLocation.toURI(), repositoryName, IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
			//Load the source
			getMetadataRepositoryManager().loadRepository(sourceRepoLocation.toURI(), null);
		} catch (ProvisionException e) {
			fail("8.0", e);
		}

		try {
			runMirrorApplication(sourceRepoLocation, destRepoLocation, true);
		} catch (Exception e) {
			fail("8.2", e);
		}

		assertTrue(getMetadataRepositoryManager().contains(destRepoLocation.toURI()));
		assertTrue(getMetadataRepositoryManager().contains(sourceRepoLocation.toURI()));
		//Ensure bystander was not effected by the mirror application
		assertTrue(getMetadataRepositoryManager().contains(bystanderRepoLocation.toURI()));
	}
}