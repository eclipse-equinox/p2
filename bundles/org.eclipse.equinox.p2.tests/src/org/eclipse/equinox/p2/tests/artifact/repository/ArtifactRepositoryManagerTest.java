/*******************************************************************************
 *  Copyright (c) 2008, 2026 IBM Corporation and others.
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
package org.eclipse.equinox.p2.tests.artifact.repository;

import static org.junit.Assert.assertThrows;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.StringBufferStream;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.p2.tests.TestRepositoryListener;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.osgi.framework.BundleException;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Tests API of {@link IArtifactRepositoryManager}.
 */
public class ArtifactRepositoryManagerTest extends AbstractProvisioningTest {
	private IArtifactRepositoryManager manager;

	public static Test suite() {
		return new TestSuite(ArtifactRepositoryManagerTest.class);
		//		TestSuite suite = new TestSuite();
		//		suite.addTest(new ArtifactRepositoryManagerTest("testEnablement"));
		//		return suite;
	}

	public ArtifactRepositoryManagerTest(String name) {
		super(name);
	}

	/**
	 * Returns whether {@link IArtifactRepositoryManager} contains a reference
	 * to a repository at the given location.
	 */
	private boolean managerContains(URI location) {
		URI[] locations = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		for (URI uri : locations) {
			if (uri.equals(location)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		manager = getArtifactRepositoryManager();
		assertNotNull(manager);
	}

	/**
	 * Tests loading a repository that does not exist throws an appropriate exception.
	 */
	public void testLoadMissingRepository() throws IOException {
		File tempFile = File.createTempFile("testLoadMissingArtifactRepository", null);
		URI location = tempFile.toURI();
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			ProvisionException e = assertThrows(ProvisionException.class, () -> manager.loadRepository(location, null));
			assertEquals(IStatus.ERROR, e.getStatus().getSeverity());
			assertEquals(ProvisionException.REPOSITORY_NOT_FOUND, e.getStatus().getCode());
		} finally {
			System.setOut(out);
			tempFile.delete();
		}
	}

	/**
	 * Tests loading an artifact repository that is missing the top level repository element.
	 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=246452.
	 */
	public void testLoadMissingRepositoryElement() throws IOException {
		File site = getTestData("Update site", "/testData/artifactRepo/broken/");
		URI location = site.toURI();
		assertThrows(ProvisionException.class, () -> manager.loadRepository(location, null));
	}

	/**
	 * This is regression test for bug 236437. In this bug, the repository preference
	 * file is being overwritten by an update operation, thus losing all repository state.
	 */
	public void testLostArtifactRepositories() throws IOException, BackingStoreException, BundleException {
		File site = getTestData("Repository", "/testData/artifactRepo/simple/");
		URI location = site.toURI();
		manager.addRepository(location);
		assertTrue(manager.contains(location));

		//bash the repository preference file (don't try this at home, kids)
		final String REPO_BUNDLE = "org.eclipse.equinox.p2.artifact.repository";
		IPreferencesService prefService = ServiceHelper.getService(TestActivator.getContext(), IPreferencesService.class);
		IAgentLocation agentLocation = getAgent().getService(IAgentLocation.class);
		String locationString = EncodingUtils.encodeSlashes(agentLocation.getRootLocation().toString());
		Preferences prefs = prefService.getRootNode().node("/profile/" + locationString + "/_SELF_/" + REPO_BUNDLE + "/repositories"); //$NON-NLS-1$ //$NON-NLS-2$
		String[] children = prefs.childrenNames();
		for (String child : children) {
			prefs.node(child).removeNode();
		}
		prefs.flush();

		//stop and restart the artifact repository bundle (kids, if I ever catch you doing this I'm taking PackageAdmin away)
		restartBundle(TestActivator.getBundle(REPO_BUNDLE));

		//everybody's happy again
		manager = getAgent().getService(IArtifactRepositoryManager.class);
		assertTrue("1.0", manager.contains(location));
	}

	public void testNickname() throws ProvisionException, IOException {
		File site = getTestData("Repository", "/testData/artifactRepo/simple/");
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

	public void testPathWithSpaces() throws IOException {
		File site = getTestData("Repository", "/testData/artifactRepo/simple with spaces/");
		URI location = site.toURI();
		assertEquals(2, getArtifactKeyCount(location));
	}

	/**
	 * Tests for
	 * {@link org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager#refreshRepository(URI, org.eclipse.core.runtime.IProgressMonitor)}.
	 */
	public void testRefresh() throws ProvisionException, IOException {
		File site = getTestData("Repositoy", "/testData/artifactRepo/simple/");
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

		// Set it back to null for other tests
		manager.setRepositoryProperty(location, IRepository.PROP_NICKNAME, null);
	}

	/**
	 * Tests for {@link IRepositoryManager#setRepositoryProperty}.
	 */
	public void testSetRepositoryProperty() throws IOException, ProvisionException {
		File site = getTestData("Repositoy", "/testData/artifactRepo/simple/");
		URI location = site.toURI();
		manager.removeRepository(location);
		manager.addRepository(location);

		//set some properties different from what the repository contains
		manager.setRepositoryProperty(location, IRepository.PROP_NAME, "TestName");
		manager.setRepositoryProperty(location, IRepository.PROP_DESCRIPTION, "TestDescription");
		manager.setRepositoryProperty(location, IRepository.PROP_SYSTEM, "true");
		assertEquals("TestName", manager.getRepositoryProperty(location, IRepository.PROP_NAME));
		assertEquals("TestDescription", manager.getRepositoryProperty(location, IRepository.PROP_DESCRIPTION));
		assertEquals("true", manager.getRepositoryProperty(location, IRepository.PROP_SYSTEM));

		//loading the repository should overwrite test values
		manager.loadRepository(location, getMonitor());

		assertEquals("Good Test Repository", manager.getRepositoryProperty(location, IRepository.PROP_NAME));
		assertEquals("Good test repository description",
				manager.getRepositoryProperty(location, IRepository.PROP_DESCRIPTION));
		assertEquals("false", manager.getRepositoryProperty(location, IRepository.PROP_SYSTEM));
	}

	public void testUpdateSitePathWithSpaces() throws IOException {
		File site = getTestData("Repository", "/testData/updatesite/site with spaces/");
		URI location = site.toURI();
		assertEquals(3, getArtifactKeyCount(location));
	}

	/**
	 * Tests that trailing slashes do not affect repository identity.
	 */
	public void testTrailingSlashes() throws IOException, URISyntaxException, ProvisionException {
		File site = getTestData("Repository", "/testData/artifactRepo/simple/");
		URI locationSlash = site.toURI();
		String locationString = locationSlash.toString();
		locationString = locationString.substring(0, locationString.length() - 1);
		URI locationNoSlash = new URI(locationString);

		manager.addRepository(locationNoSlash);
		IArtifactRepository repoSlash = manager.loadRepository(locationSlash, null);
		IArtifactRepository repoNoSlash = manager.loadRepository(locationNoSlash, null);
		assertTrue(repoNoSlash == repoSlash);

	}

	public void testBasicAddRemove() {
		File tempFile = new File(System.getProperty("java.io.tmpdir"));
		URI location = tempFile.toURI();
		assertFalse(managerContains(location));
		manager.addRepository(location);
		assertTrue(managerContains(location));
		manager.removeRepository(location);
		assertFalse(managerContains(location));
	}

	/**
	 * Tests for {@link IRepositoryManager#contains(URI)}.
	 */
	public void testContains() throws IOException {
		File site = getTestData("Repositoy", "/testData/artifactRepo/simple/");
		URI location = site.toURI();
		manager.removeRepository(location);
		assertFalse(manager.contains(location));
		manager.addRepository(location);
		assertTrue(manager.contains(location));
		manager.removeRepository(location);
		assertFalse(manager.contains(location));
	}

	/**
	 * Tests parsing a repository with a duplicate element. See bug 255401.
	 */
	public void testDuplicateElement() throws IOException {
		File duplicateElementXML = getTestData("testDuplicateElement", "testData/artifactRepo/duplicateElement");
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			assertEquals("Ensure correct number of artifact keys exist", 2, getArtifactKeyCount(duplicateElementXML.toURI()));
		} finally {
			System.setOut(out);
		}
	}

	public void testEnablement() throws IOException {
		File site = getTestData("Repository", "/testData/artifactRepo/simple/");
		URI location = site.toURI();
		manager.addRepository(location);
		assertTrue(manager.isEnabled(location));
		TestRepositoryListener listener = new TestRepositoryListener(location);
		getEventBus().addListener(listener);

		manager.setEnabled(location, false);
		listener.waitForEvent();
		assertFalse(listener.lastEnablement);
		assertFalse(manager.isEnabled(location));
		assertEquals(IRepository.TYPE_ARTIFACT, listener.lastRepoType);
		listener.reset();

		manager.setEnabled(location, true);
		listener.waitForEvent();
		assertTrue(manager.isEnabled(location));
		assertEquals(RepositoryEvent.ENABLEMENT, listener.lastKind);
		assertTrue(listener.lastEnablement);
		assertEquals(IRepository.TYPE_ARTIFACT, listener.lastRepoType);
		listener.reset();
	}

	/**
	 * Tests that adding a repository that is already known but disabled
	 * causes the repository to be enabled. See bug 241307 for discussion.
	 */
	public void testEnablementOnAdd() throws IOException {
		File site = getTestData("Repository", "/testData/artifactRepo/simple/");
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
		assertEquals(IRepository.TYPE_ARTIFACT, listener.lastRepoType);
	}
}