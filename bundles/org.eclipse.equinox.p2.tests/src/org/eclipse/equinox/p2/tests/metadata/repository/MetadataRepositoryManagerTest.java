/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata.repository;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.repository.SimpleMetadataRepositoryFactory;
import org.eclipse.equinox.internal.p2.repository.helpers.AbstractRepositoryManager;
import org.eclipse.equinox.internal.p2.updatesite.metadata.UpdateSiteMetadataRepositoryFactory;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.ProvisioningListener;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.SynchronousProvisioningListener;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.spi.MetadataRepositoryFactory;
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
		manager = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
		//only enable the failing repository factory for this test to avoid noise in other tests.
		FailingMetadataRepositoryFactory.FAIL = true;
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		for (Iterator it = toDelete.iterator(); it.hasNext();)
			delete((File) it.next());
		toDelete.clear();
		FailingMetadataRepositoryFactory.FAIL = false;
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

	/**
	 * Tests for {@link IRepositoryManager#contains(URI).
	 */
	public void testContains() {
		File site = getTestData("Repositoy", "/testData/metadataRepo/good/");
		URI location = site.toURI();
		manager.removeRepository(location);
		assertEquals("1.0", false, manager.contains(location));
		manager.addRepository(location);
		assertEquals("1.1", true, manager.contains(location));
		manager.removeRepository(location);
		assertEquals("1.2", false, manager.contains(location));
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
	 * Adds a repository that has a non-standard (non ECF) scheme.  This should
	 * return REPOSITORY_NOT_FOUND, since any other status code gets logged.
	 * 
	 * @throws URISyntaxException
	 */
	public void testFailedConnection() throws URISyntaxException {
		URI location = new URI("invalid://example");
		MetadataRepositoryFactory factory;

		factory = new SimpleMetadataRepositoryFactory();
		factory.setAgent(getAgent());
		try {
			factory.load(location, 0, new NullProgressMonitor());
		} catch (ProvisionException e) {
			assertEquals(ProvisionException.REPOSITORY_NOT_FOUND, e.getStatus().getCode());
		}
		factory = new UpdateSiteMetadataRepositoryFactory();
		factory.setAgent(getAgent());
		try {
			factory.load(location, 0, new NullProgressMonitor());
		} catch (ProvisionException e) {
			assertEquals(ProvisionException.REPOSITORY_NOT_FOUND, e.getStatus().getCode());
		}
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
	 * Tests contention for the repository load lock
	 */
	public void testLoadContention() {
		File site = getTestData("Repositoy", "/testData/metadataRepo/good/");
		final URI location = site.toURI();
		final List<Exception> failures = new ArrayList<Exception>();
		final IMetadataRepositoryManager repoManager = getMetadataRepositoryManager();
		class LoadJob extends Job {
			LoadJob() {
				super("");
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				for (int i = 0; i < 100; i++) {
					try {
						repoManager.loadRepository(location, null);
					} catch (Exception e) {
						failures.add(e);
					}
				}
				return Status.OK_STATUS;
			}
		}
		Job job1 = new LoadJob();
		Job job2 = new LoadJob();
		job1.schedule();
		job2.schedule();
		try {
			job1.join();
			job2.join();
		} catch (InterruptedException e) {
			fail("4.99", e);
		}
		if (!failures.isEmpty())
			fail("1.0", failures.iterator().next());
	}

	/**
	 * Tests loading a repository that does not exist throws an appropriate exception.
	 */
	public void testLoadMissingRepository() throws IOException {
		File tempFile = File.createTempFile("testLoadMissingArtifactRepository", null);
		tempFile.delete();
		URI location = tempFile.toURI();
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			manager.loadRepository(location, null);
			fail("1.0");//should fail
		} catch (ProvisionException e) {
			assertEquals("1.1", IStatus.ERROR, e.getStatus().getSeverity());
			assertEquals("1.2", ProvisionException.REPOSITORY_NOT_FOUND, e.getStatus().getCode());
		} finally {
			System.setOut(out);
		}
	}

	/**
	 * Tests that loading a disabled system repository does not damage its properties.
	 * This is a regression test for bug 267707.
	 */
	public void testLoadDisabledSystemRepository() throws ProvisionException, SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		File site = getTestData("Repositoy", "/testData/metadataRepo/goodNonSystem/");
		URI location = site.toURI();
		manager.removeRepository(location);
		manager.addRepository(location);
		manager.setEnabled(location, false);
		manager.setRepositoryProperty(location, IRepository.PROP_SYSTEM, String.valueOf(true));
		manager.loadRepository(location, getMonitor());

		//simulate shutdown/restart by bashing repository manager field
		Field field = AbstractRepositoryManager.class.getDeclaredField("repositories");
		field.setAccessible(true);
		field.set(manager, null);

		String system = manager.getRepositoryProperty(location, IRepository.PROP_SYSTEM);
		assertEquals("true", system);
		assertFalse(manager.isEnabled(location));

	}

	/**
	 * Tests loading a repository that is malformed
	 */
	public void testLoadBrokenRepository() {
		File site = getTestData("Repository", "/testData/metadataRepo/bad/");
		URI location = site.toURI();
		PrintStream err = System.err;
		try {
			System.setErr(new PrintStream(new StringBufferStream()));
			manager.loadRepository(location, null);
			fail("1.0");//should fail
		} catch (ProvisionException e) {
			assertEquals("1.1", IStatus.ERROR, e.getStatus().getSeverity());
			assertEquals("1.2", ProvisionException.REPOSITORY_FAILED_READ, e.getStatus().getCode());
		} finally {
			System.setErr(err);
		}
	}

	/**
	 * Tests loading a repository that is malformed, that is co-located with a well-formed
	 * update site repository. The load should fail due to the malformed simple repository,
	 * and not fall back to the well-formed update site repository. See bug 247566 for details.
	 */
	public void testLoadBrokenSimpleRepositoryWithGoodUpdateSite() {
		File site = getTestData("Repository", "/testData/metadataRepo/badSimpleGoodUpdateSite/");
		URI location = site.toURI();
		PrintStream err = System.err;
		try {
			System.setErr(new PrintStream(new StringBufferStream()));
			manager.loadRepository(location, null);
			fail("1.0");//should fail
		} catch (ProvisionException e) {
			assertEquals("1.1", IStatus.ERROR, e.getStatus().getSeverity());
			assertEquals("1.2", ProvisionException.REPOSITORY_FAILED_READ, e.getStatus().getCode());
		} finally {
			System.setErr(err);
		}
	}

	/**
	 * Tests that we don't create a local cache when contacting a local metadata repository.
	 */
	public void testMetadataCachingLocalRepo() throws ProvisionException {
		File repoLocation = getTempLocation();
		IAgentLocation agentLocation = ServiceHelper.getService(TestActivator.getContext(), IAgentLocation.class);
		URI dataArea = agentLocation.getDataArea("org.eclipse.equinox.p2.metadata.repository/cache/");
		File dataAreaFile = URIUtil.toFile(dataArea);
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
		IAgentLocation agentLocation = ServiceHelper.getService(TestActivator.getContext(), IAgentLocation.class);
		URI dataArea = agentLocation.getDataArea("org.eclipse.equinox.p2.metadata.repository/cache/");
		File dataAreaFile = URIUtil.toFile(dataArea);
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

	public void testNickname() throws ProvisionException {
		File site = getTestData("Repositoy", "/testData/metadataRepo/good/");
		URI location = site.toURI();
		manager.addRepository(location);
		String nick = manager.getRepositoryProperty(location, IRepository.PROP_NICKNAME);
		assertNull(nick);
		nick = "Nick";
		manager.setRepositoryProperty(location, IRepository.PROP_NICKNAME, nick);
		nick = manager.getRepositoryProperty(location, IRepository.PROP_NICKNAME);
		assertEquals("Nick", nick);
		//ensure loading the repository doesn't affect the nickname
		manager.loadRepository(location, getMonitor());
		nick = manager.getRepositoryProperty(location, IRepository.PROP_NICKNAME);
		assertEquals("Nick", nick);

		//remove and re-add the repository should lose the nickname
		manager.removeRepository(location);
		manager.loadRepository(location, getMonitor());
		nick = manager.getRepositoryProperty(location, IRepository.PROP_NICKNAME);
		assertNull(nick);
	}

	public void testPathWithSpaces() {
		File site = getTestData("Repository", "/testData/metadataRepo/good with spaces/");
		URI location = site.toURI();
		try {
			IMetadataRepository repository = manager.loadRepository(location, getMonitor());
			IQueryResult result = repository.query(QueryUtil.createIUQuery("test.bundle"), getMonitor());
			assertEquals("1.0", 1, queryResultSize(result));
		} catch (ProvisionException e) {
			fail("=.99", e);
		}
	}

	public void testRelativePath() throws URISyntaxException {
		URI location = new URI("test");
		try {
			manager.loadRepository(location, getMonitor());
			fail();
		} catch (IllegalArgumentException e) {
			//expected
		} catch (ProvisionException e) {
			fail("4.99", e);
		}
	}

	/**
	 * Tests for {@link IMetadataRepositoryManager#refreshRepository(URI, org.eclipse.core.runtime.IProgressMonitor)}.
	 */
	public void testRefresh() throws ProvisionException {
		File site = getTestData("Repositoy", "/testData/metadataRepo/good/");
		URI location = site.toURI();
		manager.addRepository(location);
		manager.refreshRepository(location, getMonitor());
		assertTrue("1.0", manager.contains(location));
		assertTrue("1.1", manager.isEnabled(location));

		//tests that refreshing doesn't lose repository properties
		manager.setEnabled(location, false);
		manager.setRepositoryProperty(location, IRepository.PROP_NICKNAME, "MyNick");
		manager.refreshRepository(location, getMonitor());
		assertTrue("2.0", manager.contains(location));
		assertFalse("2.1", manager.isEnabled(location));
		assertEquals("2.2", "MyNick", manager.getRepositoryProperty(location, IRepository.PROP_NICKNAME));

		// Set the nickname back to null for other tests
		manager.setRepositoryProperty(location, IRepository.PROP_NICKNAME, null);
	}

	/**
	 * Repository references were originally encoded as URL, but we now encode
	 * as URI. This test ensures we handle both old and new references.
	 */
	public void testRepositoryReferenceCompatibility() throws URISyntaxException {
		File site = getTestData("Repository", "/testData/metadataRepo/unencodedreporeferences/");
		URI location = site.toURI();
		final List references = new ArrayList();
		ProvisioningListener referenceCollector = new SynchronousProvisioningListener() {
			public void notify(EventObject o) {
				if (!(o instanceof RepositoryEvent))
					return;
				RepositoryEvent event = (RepositoryEvent) o;
				if (event.getKind() == RepositoryEvent.DISCOVERED)
					references.add(event.getRepositoryLocation());
			}
		};
		getEventBus().addListener(referenceCollector);
		try {
			manager.loadRepository(location, getMonitor());
		} catch (ProvisionException e) {
			fail("=.99", e);
		} finally {
			getEventBus().removeListener(referenceCollector);
		}
		assertEquals("1.0", 4, references.size());
		assertTrue("1.1", references.contains(new URI("http://download.eclipse.org/url/with/spaces/a%20b")));
		assertTrue("1.2", references.contains(new URI("file:/c:/tmp/url%20with%20spaces/")));
		assertTrue("1.3", references.contains(new URI("http://download.eclipse.org/uri/with/spaces/a%20b")));
		assertTrue("1.4", references.contains(new URI("file:/c:/tmp/uri%20with%20spaces/")));
	}

	/**
	 * Tests for {@link IRepositoryManager#setRepositoryProperty}.
	 */
	public void testSetRepositoryProperty() {
		File site = getTestData("Repositoy", "/testData/metadataRepo/good/");
		URI location = site.toURI();
		manager.removeRepository(location);
		manager.addRepository(location);

		//set some properties different from what the repository contains
		manager.setRepositoryProperty(location, IRepository.PROP_NAME, "TestName");
		manager.setRepositoryProperty(location, IRepository.PROP_DESCRIPTION, "TestDescription");
		manager.setRepositoryProperty(location, IRepository.PROP_SYSTEM, "false");
		assertEquals("1.0", "TestName", manager.getRepositoryProperty(location, IRepository.PROP_NAME));
		assertEquals("1.1", "TestDescription", manager.getRepositoryProperty(location, IRepository.PROP_DESCRIPTION));
		assertEquals("1.2", "false", manager.getRepositoryProperty(location, IRepository.PROP_SYSTEM));

		//loading the repository should overwrite test values
		try {
			manager.loadRepository(location, getMonitor());
		} catch (ProvisionException e) {
			fail("1.99", e);
		}

		assertEquals("2.0", "Good Test Repository", manager.getRepositoryProperty(location, IRepository.PROP_NAME));
		assertEquals("2.1", "Good test repository description", manager.getRepositoryProperty(location, IRepository.PROP_DESCRIPTION));
		assertEquals("2.2", "true", manager.getRepositoryProperty(location, IRepository.PROP_SYSTEM));
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

	public void testReadableFilter() throws ProvisionException {
		File site = getTestData("readable", "/testData/metadataRepo/badFilter/readable");
		IMetadataRepository loadRepository = manager.loadRepository(site.toURI(), null);
		assertEquals(1, loadRepository.query(QueryUtil.createIUAnyQuery(), null).toSet().size());
	}

	public void testUnreadableFailingFilter() {
		File site = getTestData("unreadable", "/testData/metadataRepo/badFilter/unreadable");
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			manager.loadRepository(site.toURI(), null);
		} catch (ProvisionException e) {
			return;
		} finally {
			System.setOut(out);
		}
		fail("Unexpected code path, the unreadable repo should not have loaded");

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
		toDelete.add(tempFile);
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
