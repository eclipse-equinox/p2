/*******************************************************************************
 *  Copyright (c) 2007, 2026 IBM Corporation and others.
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
package org.eclipse.equinox.p2.tests.metadata.repository;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.repository.SimpleMetadataRepositoryFactory;
import org.eclipse.equinox.internal.p2.repository.helpers.AbstractRepositoryManager;
import org.eclipse.equinox.internal.p2.updatesite.metadata.UpdateSiteMetadataRepositoryFactory;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.SynchronousProvisioningListener;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.spi.MetadataRepositoryFactory;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.FailingMetadataRepositoryFactory;
import org.eclipse.equinox.p2.tests.StringBufferStream;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.p2.tests.TestRepositoryListener;

/**
 * Tests for API of {@link IMetadataRepositoryManager}.
 */
public class MetadataRepositoryManagerTest extends AbstractProvisioningTest {
	protected IMetadataRepositoryManager manager;
	/**
	 * Contains temp File handles that should be deleted at the end of the test.
	 */
	private final List<File> toDelete = new ArrayList<>();

	public static Test suite() {
		return new TestSuite(MetadataRepositoryManagerTest.class);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		manager = getAgent().getService(IMetadataRepositoryManager.class);
		//only enable the failing repository factory for this test to avoid noise in other tests.
		FailingMetadataRepositoryFactory.FAIL = true;
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		for (File file : toDelete) {
			delete(file);
		}
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
	 * Tests for {@link IRepositoryManager#contains(URI)}.
	 */
	public void testContains() throws IOException {
		File site = getTestData("Repositoy", "/testData/metadataRepo/good/");
		URI location = site.toURI();
		manager.removeRepository(location);
		assertFalse(manager.contains(location));
		manager.addRepository(location);
		assertTrue(manager.contains(location));
		manager.removeRepository(location);
		assertFalse(manager.contains(location));
	}

	public void testEnablement() throws IOException {
		File site = getTestData("Repositoy", "/testData/metadataRepo/good/");
		URI location = site.toURI();
		manager.addRepository(location);
		assertTrue(manager.isEnabled(location));
		TestRepositoryListener listener = new TestRepositoryListener(location);
		getEventBus().addListener(listener);

		manager.setEnabled(location, false);
		listener.waitForEvent();
		assertFalse(listener.lastEnablement);
		assertFalse(manager.isEnabled(location));
		listener.reset();

		manager.setEnabled(location, true);
		listener.waitForEvent();
		assertTrue(listener.lastEnablement);
		assertTrue(manager.isEnabled(location));
		listener.reset();
	}

	/**
	 * Adds a repository that has a non-standard (non ECF) scheme.  This should
	 * return REPOSITORY_NOT_FOUND, since any other status code gets logged.
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
	public void testEnablementOnAdd() throws IOException {
		File site = getTestData("Repositoy", "/testData/metadataRepo/good/");
		URI location = site.toURI();
		manager.addRepository(location);
		manager.setEnabled(location, false);
		TestRepositoryListener listener = new TestRepositoryListener(location);
		getEventBus().addListener(listener);

		//adding the location again should cause it to be enabled
		manager.addRepository(location);
		listener.waitForEvent();
		assertTrue(listener.lastEnablement);
		assertTrue(manager.isEnabled(location));
	}

	public void testGetKnownRepositories() throws ProvisionException {
		int nonSystemCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_NON_SYSTEM).length;
		int systemCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_SYSTEM).length;
		int allCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL).length;
		assertEquals(allCount, nonSystemCount + systemCount);

		//create a new repository
		File repoLocation = getTempLocation();
		IMetadataRepository testRepo = manager.createRepository(repoLocation.toURI(), "MetadataRepositoryManagerTest", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null);
		int newNonSystemCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_NON_SYSTEM).length;
		int newSystemCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_SYSTEM).length;
		int newAllCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL).length;

		//there should be one more non-system repository
		assertEquals(nonSystemCount + 1, newNonSystemCount);
		assertEquals(systemCount, newSystemCount);
		assertEquals(allCount + 1, newAllCount);

		//make the repository a system repository
		testRepo.setProperty(IRepository.PROP_SYSTEM, Boolean.TRUE.toString());

		//there should be one more system repository
		newNonSystemCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_NON_SYSTEM).length;
		newSystemCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_SYSTEM).length;
		newAllCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL).length;
		assertEquals(nonSystemCount, newNonSystemCount);
		assertEquals(systemCount + 1, newSystemCount);
		assertEquals(allCount + 1, newAllCount);

		int disabledCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED).length;
		allCount = newAllCount;

		//mark the repository as disabled
		manager.setEnabled(testRepo.getLocation(), false);

		//should be one less enabled repository and one more disabled repository
		int newDisabledCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED).length;
		newAllCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL).length;
		assertEquals(disabledCount + 1, newDisabledCount);
		assertEquals(allCount - 1, newAllCount);

		//re-loading the repository should not change anything
		manager.loadRepository(testRepo.getLocation(), null);
		newDisabledCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED).length;
		newAllCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL).length;
		assertEquals(disabledCount + 1, newDisabledCount);
		assertEquals(allCount - 1, newAllCount);

		//re-enable the repository
		manager.setEnabled(testRepo.getLocation(), true);

		//should be back to the original counts
		newDisabledCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_DISABLED).length;
		newAllCount = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL).length;
		assertEquals(disabledCount, newDisabledCount);
		assertEquals(allCount, newAllCount);
	}

	/**
	 * Tests contention for the repository load lock
	 */
	public void testLoadContention() throws IOException, InterruptedException {
		File site = getTestData("Repositoy", "/testData/metadataRepo/good/");
		final URI location = site.toURI();
		final List<Exception> failures = new ArrayList<>();
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
		job1.join();
		job2.join();
		if (!failures.isEmpty()) {
			fail("1.0", failures.iterator().next());
		}
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
			assertEquals(IStatus.ERROR, e.getStatus().getSeverity());
			assertEquals(ProvisionException.REPOSITORY_NOT_FOUND, e.getStatus().getCode());
		} finally {
			System.setOut(out);
		}
	}

	/**
	 * Tests that loading a disabled system repository does not damage its properties.
	 * This is a regression test for bug 267707.
	 */
	public void testLoadDisabledSystemRepository() throws ProvisionException, SecurityException, NoSuchFieldException,
			IllegalArgumentException, IllegalAccessException, IOException {
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
	public void testLoadBrokenRepository() throws IOException {
		File site = getTestData("Repository", "/testData/metadataRepo/bad/");
		URI location = site.toURI();
		PrintStream err = System.err;
		try {
			System.setErr(new PrintStream(new StringBufferStream()));
			manager.loadRepository(location, null);
			fail("1.0");//should fail
		} catch (ProvisionException e) {
			assertEquals(IStatus.ERROR, e.getStatus().getSeverity());
			assertEquals(ProvisionException.REPOSITORY_FAILED_READ, e.getStatus().getCode());
		} finally {
			System.setErr(err);
		}
	}

	/**
	 * Tests loading a repository that is malformed, that is co-located with a well-formed
	 * update site repository. The load should fail due to the malformed simple repository,
	 * and not fall back to the well-formed update site repository. See bug 247566 for details.
	 */
	public void testLoadBrokenSimpleRepositoryWithGoodUpdateSite() throws IOException {
		File site = getTestData("Repository", "/testData/metadataRepo/badSimpleGoodUpdateSite/");
		URI location = site.toURI();
		PrintStream err = System.err;
		try {
			System.setErr(new PrintStream(new StringBufferStream()));
			manager.loadRepository(location, null);
			fail("1.0");//should fail
		} catch (ProvisionException e) {
			assertEquals(IStatus.ERROR, e.getStatus().getSeverity());
			assertEquals(ProvisionException.REPOSITORY_FAILED_READ, e.getStatus().getCode());
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
		URI repoLocation = new URI("https://download.eclipse.org/eclipse/updates/4.21/R-4.21-202109060500/");
		if (!repoAvailable(repoLocation)) {
			return;
		}
		IAgentLocation agentLocation = ServiceHelper.getService(TestActivator.getContext(), IAgentLocation.class);
		URI dataArea = agentLocation.getDataArea("org.eclipse.equinox.p2.repository/cache/");
		File dataAreaFile = URIUtil.toFile(dataArea);
		File cacheFile = new File(dataAreaFile,
				Integer.toString(URIUtil.append(repoLocation, "content.xml.xz").hashCode())); // as implemented in
																							// XZedSimpleMetadataRepository
																							// and CacheManager

		// load a remote repository and check that a local cache was created
		manager.loadRepository(repoLocation, null);
		assertTrue("Cache file was not created.", cacheFile.exists());

		// modify the last modified date to be older than the remote file
		cacheFile.setLastModified(0);
		// reload the repository and check that the cache was updated
		manager.removeRepository(repoLocation);
		manager.loadRepository(repoLocation, null);
		long lastModified = cacheFile.lastModified();
		assertNotEquals(0, lastModified);

		// reload the repository and check that the cache was not updated
		manager.loadRepository(repoLocation, null);
		assertEquals(lastModified, cacheFile.lastModified());

		cacheFile.delete();
	}

	public void testNickname() throws ProvisionException, IOException {
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

	public void testPathWithSpaces() throws IOException, ProvisionException, OperationCanceledException {
		File site = getTestData("Repository", "/testData/metadataRepo/good with spaces/");
		URI location = site.toURI();
		IMetadataRepository repository = manager.loadRepository(location, getMonitor());
		IQueryResult<IInstallableUnit> result = repository.query(QueryUtil.createIUQuery("test.bundle"), getMonitor());
		assertEquals(1, queryResultSize(result));
	}

	public void testRelativePath() throws URISyntaxException {
		URI location = new URI("test");
		assertThrows(IllegalArgumentException.class, () -> manager.loadRepository(location, getMonitor()));
	}

	/**
	 * Tests for {@link IMetadataRepositoryManager#refreshRepository(URI, org.eclipse.core.runtime.IProgressMonitor)}.
	 */
	public void testRefresh() throws ProvisionException, IOException {
		File site = getTestData("Repositoy", "/testData/metadataRepo/good/");
		URI location = site.toURI();
		manager.addRepository(location);
		manager.refreshRepository(location, getMonitor());
		assertTrue(manager.contains(location));
		assertTrue(manager.isEnabled(location));

		//tests that refreshing doesn't lose repository properties
		manager.setEnabled(location, false);
		manager.setRepositoryProperty(location, IRepository.PROP_NICKNAME, "MyNick");
		manager.refreshRepository(location, getMonitor());
		assertTrue(manager.contains(location));
		assertFalse(manager.isEnabled(location));
		assertEquals("MyNick", manager.getRepositoryProperty(location, IRepository.PROP_NICKNAME));

		// Set the nickname back to null for other tests
		manager.setRepositoryProperty(location, IRepository.PROP_NICKNAME, null);
	}

	/**
	 * Repository references were originally encoded as URL, but we now encode
	 * as URI. This test ensures we handle both old and new references.
	 */
	public void testRepositoryReferenceCompatibility()
			throws URISyntaxException, IOException, ProvisionException, OperationCanceledException {
		File site = getTestData("Repository", "/testData/metadataRepo/unencodedreporeferences/");
		URI location = site.toURI();
		final List<URI> references = new ArrayList<>();
		SynchronousProvisioningListener referenceCollector = o -> {
			if (!(o instanceof RepositoryEvent event)) {
				return;
			}
			if (event.getKind() == RepositoryEvent.DISCOVERED) {
				references.add(event.getRepositoryLocation());
			}
		};
		getEventBus().addListener(referenceCollector);
		try {
			manager.loadRepository(location, getMonitor());
		} finally {
			getEventBus().removeListener(referenceCollector);
		}
		assertEquals(4, references.size());
		assertTrue(references.contains(new URI("https://download.eclipse.org/url/with/spaces/a%20b")));
		assertTrue(references.contains(new URI("file:/c:/tmp/url%20with%20spaces/")));
		assertTrue(references.contains(new URI("https://download.eclipse.org/uri/with/spaces/a%20b")));
		assertTrue(references.contains(new URI("file:/c:/tmp/uri%20with%20spaces/")));
	}

	/**
	 * Tests for {@link IRepositoryManager#setRepositoryProperty}.
	 */
	public void testSetRepositoryProperty() throws IOException, ProvisionException, OperationCanceledException {
		File site = getTestData("Repositoy", "/testData/metadataRepo/good/");
		URI location = site.toURI();
		manager.removeRepository(location);
		manager.addRepository(location);

		//set some properties different from what the repository contains
		manager.setRepositoryProperty(location, IRepository.PROP_NAME, "TestName");
		manager.setRepositoryProperty(location, IRepository.PROP_DESCRIPTION, "TestDescription");
		manager.setRepositoryProperty(location, IRepository.PROP_SYSTEM, "false");
		assertEquals("TestName", manager.getRepositoryProperty(location, IRepository.PROP_NAME));
		assertEquals("TestDescription", manager.getRepositoryProperty(location, IRepository.PROP_DESCRIPTION));
		assertEquals("false", manager.getRepositoryProperty(location, IRepository.PROP_SYSTEM));

		//loading the repository should overwrite test values
		manager.loadRepository(location, getMonitor());

		assertEquals("Good Test Repository", manager.getRepositoryProperty(location, IRepository.PROP_NAME));
		assertEquals("Good test repository description",
				manager.getRepositoryProperty(location, IRepository.PROP_DESCRIPTION));
		assertEquals("true", manager.getRepositoryProperty(location, IRepository.PROP_SYSTEM));
	}

	/**
	 * Tests that trailing slashes do not affect repository identity.
	 */
	public void testTrailingSlashes()
			throws IOException, URISyntaxException, ProvisionException, OperationCanceledException {
		File site = getTestData("Repository", "/testData/metadataRepo/good/");
		URI locationSlash = site.toURI();
		String locationString = locationSlash.toString();
		locationString = locationString.substring(0, locationString.length() - 1);
		URI locationNoSlash = new URI(locationString);

		manager.addRepository(locationNoSlash);
		IMetadataRepository repoSlash = manager.loadRepository(locationSlash, null);
		IMetadataRepository repoNoSlash = manager.loadRepository(locationNoSlash, null);
		assertSame(repoNoSlash, repoSlash);
	}

	public void testReadableFilter() throws ProvisionException, IOException {
		File site = getTestData("readable", "/testData/metadataRepo/badFilter/readable");
		IMetadataRepository loadRepository = manager.loadRepository(site.toURI(), null);
		assertEquals(1, loadRepository.query(QueryUtil.createIUAnyQuery(), null).toSet().size());
	}

	public void testUnreadableFailingFilter() throws IOException {
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
		assertThrows(RuntimeException.class, () -> manager.addRepository(null));
		assertThrows(RuntimeException.class, () -> manager.removeRepository(null));
	}

	/**
	 * Returns whether {@link IMetadataRepositoryManager} contains a reference
	 * to a repository at the given location.
	 */
	private boolean managerContains(URI location) {
		URI[] locations = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		for (URI location2 : locations) {
			if (location2.equals(location)) {
				return true;
			}
		}
		return false;
	}
}
