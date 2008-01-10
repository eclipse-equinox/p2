/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata.repository;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;

/**
 * Tests for API of {@link IMetadataRepositoryManager}.
 */
public class MetadataRepositoryManagerTest extends AbstractProvisioningTest {
	protected IMetadataRepositoryManager manager;
	/**
	 * Contains temp File handles that should be deleted at the end of the test.
	 */
	private final List toDelete = new ArrayList();

	public static Test suite() {
		return new TestSuite(MetadataRepositoryManagerTest.class);
	}

	protected void setUp() throws Exception {
		super.setUp();
		manager = (IMetadataRepositoryManager) ServiceHelper.getService(TestActivator.context, IMetadataRepositoryManager.class.getName());
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		for (Iterator it = toDelete.iterator(); it.hasNext();)
			delete((File) it.next());
		toDelete.clear();
	}

	public void testBasicAddRemove() throws MalformedURLException {
		File tempFile = new File(System.getProperty("java.io.tmpdir"));
		URL location = tempFile.toURL();
		assertTrue(!managerContains(location));
		manager.addRepository(location);
		assertTrue(managerContains(location));
		manager.removeRepository(location);
		assertTrue(!managerContains(location));
	}

	public void testGetKnownRepositories() throws MalformedURLException {
		int nonSystemCount = manager.getKnownRepositories(IMetadataRepositoryManager.REPOSITORIES_NON_SYSTEM).length;
		int systemCount = manager.getKnownRepositories(IMetadataRepositoryManager.REPOSITORIES_SYSTEM).length;
		int allCount = manager.getKnownRepositories(IMetadataRepositoryManager.REPOSITORIES_ALL).length;
		assertEquals("1.0", allCount, nonSystemCount + systemCount);

		//create a new repository
		File repoLocation = getTempLocation();
		IMetadataRepository testRepo = manager.createRepository(repoLocation.toURL(), "MetadataRepositoryManagerTest", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY);
		int newNonSystemCount = manager.getKnownRepositories(IMetadataRepositoryManager.REPOSITORIES_NON_SYSTEM).length;
		int newSystemCount = manager.getKnownRepositories(IMetadataRepositoryManager.REPOSITORIES_SYSTEM).length;
		int newAllCount = manager.getKnownRepositories(IMetadataRepositoryManager.REPOSITORIES_ALL).length;

		//there should be one more non-system repository
		assertEquals("2.0", nonSystemCount + 1, newNonSystemCount);
		assertEquals("2.1", systemCount, newSystemCount);
		assertEquals("2.2", allCount + 1, newAllCount);

		//make the repository a system repository
		testRepo.setProperty(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());

		//there should be one more system repository
		newNonSystemCount = manager.getKnownRepositories(IMetadataRepositoryManager.REPOSITORIES_NON_SYSTEM).length;
		newSystemCount = manager.getKnownRepositories(IMetadataRepositoryManager.REPOSITORIES_SYSTEM).length;
		newAllCount = manager.getKnownRepositories(IMetadataRepositoryManager.REPOSITORIES_ALL).length;
		assertEquals("3.0", nonSystemCount, newNonSystemCount);
		assertEquals("3.1", systemCount + 1, newSystemCount);
		assertEquals("3.2", allCount + 1, newAllCount);

	}

	/**
	 * Returns a non-existent file that can be used to write a temporary
	 * file or directory. The location will be deleted in the test tearDown method.
	 */
	private File getTempLocation() {
		File tempDir = new File(System.getProperty("java.io.tmpdir"));
		File tempFile = new File(tempDir, "MetadataRepositoryManagerTest");
		delete(tempFile);
		assertTrue(!tempFile.exists());
		return tempFile;
	}

	public void testFailureAddRemove() {
		try {
			manager.addRepository(null);
			fail();
		} catch (RuntimeException e) {
			//expected
		}
		try {
			manager.removeRepository(null);
			fail();
		} catch (RuntimeException e) {
			//expected
		}
	}

	/**
	 * Returns whether {@link IMetadataRepositoryManager} contains a reference
	 * to a repository at the given location.
	 */
	private boolean managerContains(URL location) {
		URL[] locations = manager.getKnownRepositories(IMetadataRepositoryManager.REPOSITORIES_ALL);
		for (int i = 0; i < locations.length; i++) {
			if (locations[i].equals(location))
				return true;
		}
		return false;
	}
}
