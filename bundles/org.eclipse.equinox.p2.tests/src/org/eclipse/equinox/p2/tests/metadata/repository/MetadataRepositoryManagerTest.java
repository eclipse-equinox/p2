/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
import java.io.IOException;
import java.net.*;
import java.util.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.core.helpers.URLUtil;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.location.AgentLocation;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.tests.*;

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

	public void testBasicAddRemove() {
		File tempFile = new File(System.getProperty("java.io.tmpdir"));
		URI location = tempFile.toURI();
		assertTrue(!managerContains(location));
		manager.addRepository(location);
		assertTrue(managerContains(location));
		manager.removeRepository(location);
		assertTrue(!managerContains(location));
	}

	public void testEnablement() {
		File site = getTestData("Repositoy", "/testData/metadataRepo/good/");
		URI location = site.toURI();
		manager.addRepository(location);
		assertEquals("1.0", true, manager.isEnabled(location));
		TestRepositoryListener listener = new TestRepositoryListener(location);
		getEventBus().addListener(listener);

		manager.setEnabled(location, false);
		listener.waitForEvent();
		assertEquals("2.0", false, listener.lastEnablement);
		assertEquals("2.1", false, manager.isEnabled(location));
		listener.reset();

		manager.setEnabled(location, true);
		listener.waitForEvent();
		assertEquals("3.0", true, listener.lastEnablement);
		assertEquals("3.1", true, manager.isEnabled(location));
		listener.reset();
	}

	/**
	 * Tests that adding a repository that is already known but disabled
	 * causes the repository to be enabled. See bug 241307 for discussion.
	 */
	public void testEnablementOnAdd() {
		File site = getTestData("Repositoy", "/testData/metadataRepo/good/");
		URI location = site.toURI();
		manager.addRepository(location);
		manager.setEnabled(location, false);
		TestRepositoryListener listener = new TestRepositoryListener(location);
		getEventBus().addListener(listener);

		//adding the location again should cause it to be enabled
		manager.addRepository(location);
		listener.waitForEvent();
		assertEquals("1.0", true, listener.lastEnablement);
		assertEquals("1.1", true, manager.isEnabled(location));
	}

	public void testGetKnownRepositories() throws ProvisionException {
		int nonSystemCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_NON_SYSTEM).length;
		int systemCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_SYSTEM).length;
		int allCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL).length;
		assertEquals("1.0", allCount, nonSystemCount + systemCount);

		//create a new repository
		File repoLocation = getTempLocation();
		IMetadataRepository testRepo = manager.createRepository(repoLocation.toURI(), "MetadataRepositoryManagerTest", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		int newNonSystemCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_NON_SYSTEM).length;
		int newSystemCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_SYSTEM).length;
		int newAllCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL).length;

		//there should be one more non-system repository
		assertEquals("2.0", nonSystemCount + 1, newNonSystemCount);
		assertEquals("2.1", systemCount, newSystemCount);
		assertEquals("2.2", allCount + 1, newAllCount);

		//make the repository a system repository
		testRepo.setProperty(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());

		//there should be one more system repository
		newNonSystemCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_NON_SYSTEM).length;
		newSystemCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_SYSTEM).length;
		newAllCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL).length;
		assertEquals("3.0", nonSystemCount, newNonSystemCount);
		assertEquals("3.1", systemCount + 1, newSystemCount);
		assertEquals("3.2", allCount + 1, newAllCount);

		int disabledCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED).length;
		allCount = newAllCount;

		//mark the repository as disabled
		manager.setEnabled(testRepo.getLocation(), false);

		//should be one less enabled repository and one more disabled repository
		int newDisabledCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED).length;
		newAllCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL).length;
		assertEquals("4.0", disabledCount + 1, newDisabledCount);
		assertEquals("4.1", allCount - 1, newAllCount);

		//re-loading the repository should not change anything
		manager.loadRepository(testRepo.getLocation(), null);
		newDisabledCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED).length;
		newAllCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL).length;
		assertEquals("5.0", disabledCount + 1, newDisabledCount);
		assertEquals("5.1", allCount - 1, newAllCount);

		//re-enable the repository
		manager.setEnabled(testRepo.getLocation(), true);

		//should be back to the original counts
		newDisabledCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED).length;
		newAllCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL).length;
		assertEquals("6.0", disabledCount, newDisabledCount);
		assertEquals("6.1", allCount, newAllCount);
	}

	/**
	 * Tests loading a repository that does not exist throws an appropriate exception.
	 */
	public void testLoadMissingRepository() throws IOException {
		File tempFile = File.createTempFile("testLoadMissingArtifactRepository", null);
		URI location = tempFile.toURI();
		try {
			manager.loadRepository(location, null);
			fail("1.0");//should fail
		} catch (ProvisionException e) {
			assertEquals("1.1", IStatus.ERROR, e.getStatus().getSeverity());
			assertEquals("1.2", ProvisionException.REPOSITORY_NOT_FOUND, e.getStatus().getCode());
		}
	}

	/**
	 * Tests that we don't create a local cache when contacting a local metadata repository.
	 */
	public void testMetadataCachingLocalRepo() throws ProvisionException {
		File repoLocation = getTempLocation();
		AgentLocation agentLocation = (AgentLocation) ServiceHelper.getService(TestActivator.getContext(), AgentLocation.class.getName());
		URL dataArea = agentLocation.getDataArea("org.eclipse.equinox.p2.metadata.repository/cache/");
		File dataAreaFile = URLUtil.toFile(dataArea);
		File cacheFileXML = new File(dataAreaFile, "content" + repoLocation.hashCode() + ".xml");
		File cacheFileJAR = new File(dataAreaFile, "content" + repoLocation.hashCode() + ".jar");

		// create a local repository
		manager.createRepository(repoLocation.toURI(), "MetadataRepositoryCachingTest", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		manager.loadRepository(repoLocation.toURI(), null);

		// check that a local cache was not created
		assertFalse("Cache file was created.", cacheFileXML.exists() || cacheFileJAR.exists());
	}

	/**
	 * Tests that local caching of remote metadata repositories works, and that the
	 * cache is updated when it becomes stale.
	 */
	public void testMetadataCachingRemoteRepo() throws URISyntaxException, ProvisionException {
		URI repoLocation = new URI("http://download.eclipse.org/eclipse/updates/3.4milestones/");
		if (!repoAvailable(repoLocation))
			return;
		AgentLocation agentLocation = (AgentLocation) ServiceHelper.getService(TestActivator.getContext(), AgentLocation.class.getName());
		URL dataArea = agentLocation.getDataArea("org.eclipse.equinox.p2.metadata.repository/cache/");
		File dataAreaFile = URLUtil.toFile(dataArea);
		File cacheFileXML = new File(dataAreaFile, "content" + repoLocation.hashCode() + ".xml");
		File cacheFileJAR = new File(dataAreaFile, "content" + repoLocation.hashCode() + ".jar");
		File cacheFile;

		// load a remote repository and check that a local cache was created
		manager.loadRepository(repoLocation, null);
		assertTrue("Cache file was not created.", cacheFileXML.exists() || cacheFileJAR.exists());
		if (cacheFileXML.exists())
			cacheFile = cacheFileXML;
		else
			cacheFile = cacheFileJAR;

		// modify the last modified date to be older than the remote file
		cacheFile.setLastModified(0);
		// reload the repository and check that the cache was updated
		manager.removeRepository(repoLocation);
		manager.loadRepository(repoLocation, null);
		long lastModified = cacheFile.lastModified();
		assertTrue(0 != lastModified);

		// reload the repository and check that the cache was not updated
		manager.loadRepository(repoLocation, null);
		assertEquals(lastModified, cacheFile.lastModified());

		cacheFile.delete();
	}

	public void testPathWithSpaces() {
		File site = getTestData("Repository", "/testData/metadataRepo/good with spaces/");
		URI location = site.toURI();
		try {
			IMetadataRepository repository = manager.loadRepository(location, getMonitor());
			Collector result = repository.query(new InstallableUnitQuery("test.bundle"), new Collector(), getMonitor());
			assertEquals("1.0", 1, result.size());
		} catch (ProvisionException e) {
			fail("=.99", e);
		}
	}

	/**
	 * Tests that trailing slashes do not affect repository identity.
	 */
	public void testTrailingSlashes() {
		File site = getTestData("Repository", "/testData/metadataRepo/good/");
		URI locationSlash, locationNoSlash;
		try {
			locationSlash = site.toURI();
			String locationString = locationSlash.toString();
			locationString = locationString.substring(0, locationString.length() - 1);
			locationNoSlash = new URI(locationString);
		} catch (URISyntaxException e) {
			fail("0.99", e);
			return;
		}

		manager.addRepository(locationNoSlash);
		try {
			IMetadataRepository repoSlash = manager.loadRepository(locationSlash, null);
			IMetadataRepository repoNoSlash = manager.loadRepository(locationNoSlash, null);
			assertTrue("1.0", repoNoSlash == repoSlash);
		} catch (ProvisionException e) {
			fail("1.99", e);
		}
	}

	private boolean repoAvailable(URI repoLocation) {
		try {
			repoLocation.toURL().openStream().close();
		} catch (IOException e) {
			return false;
		}
		return true;
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
	private boolean managerContains(URI location) {
		URI[] locations = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		for (int i = 0; i < locations.length; i++) {
			if (locations[i].equals(location))
				return true;
		}
		return false;
	}
}
