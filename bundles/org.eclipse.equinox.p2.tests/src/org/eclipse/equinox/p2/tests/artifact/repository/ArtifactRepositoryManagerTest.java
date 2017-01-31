/*******************************************************************************
 *  Copyright (c) 2008, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.*;
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
import org.eclipse.equinox.p2.tests.*;
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
		for (int i = 0; i < locations.length; i++) {
			if (locations[i].equals(location))
				return true;
		}
		return false;
	}

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
			manager.loadRepository(location, null);
			fail("1.0");//should fail
		} catch (ProvisionException e) {
			assertEquals("1.1", IStatus.ERROR, e.getStatus().getSeverity());
			assertEquals("1.2", ProvisionException.REPOSITORY_NOT_FOUND, e.getStatus().getCode());
		} finally {
			System.setOut(out);
			tempFile.delete();
		}
	}

	/**
	 * Tests loading an artifact repository that is missing the top level repository element.
	 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=246452.
	 */
	public void testLoadMissingRepositoryElement() {
		File site = getTestData("Update site", "/testData/artifactRepo/broken/");
		try {
			URI location = site.toURI();
			manager.loadRepository(location, null);
			//should have failed
			fail("1.0");
		} catch (ProvisionException e) {
			//expected
		}
	}

	/**
	 * This is regression test for bug 236437. In this bug, the repository preference
	 * file is being overwritten by an update operation, thus losing all repository state.
	 */
	public void testLostArtifactRepositories() {
		File site = getTestData("Repository", "/testData/artifactRepo/simple/");
		URI location = site.toURI();
		manager.addRepository(location);
		assertTrue("0.1", manager.contains(location));

		//bash the repository preference file (don't try this at home, kids)
		final String REPO_BUNDLE = "org.eclipse.equinox.p2.artifact.repository";
		IPreferencesService prefService = ServiceHelper.getService(TestActivator.getContext(), IPreferencesService.class);
		IAgentLocation agentLocation = (IAgentLocation) getAgent().getService(IAgentLocation.SERVICE_NAME);
		String locationString = EncodingUtils.encodeSlashes(agentLocation.getRootLocation().toString());
		Preferences prefs = prefService.getRootNode().node("/profile/" + locationString + "/_SELF_/" + REPO_BUNDLE + "/repositories"); //$NON-NLS-1$ //$NON-NLS-2$
		try {
			String[] children = prefs.childrenNames();
			for (int i = 0; i < children.length; i++)
				prefs.node(children[i]).removeNode();
			prefs.flush();
		} catch (BackingStoreException e) {
			fail("0.99", e);
		}

		//stop and restart the artifact repository bundle (kids, if I ever catch you doing this I'm taking PackageAdmin away)
		try {
			restartBundle(TestActivator.getBundle(REPO_BUNDLE));
		} catch (BundleException e) {
			fail("1.99", e);
		}

		//everybody's happy again
		manager = (IArtifactRepositoryManager) getAgent().getService(IArtifactRepositoryManager.SERVICE_NAME);
		assertTrue("1.0", manager.contains(location));
	}

	public void testNickname() throws ProvisionException {
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

	public void testPathWithSpaces() {
		File site = getTestData("Repository", "/testData/artifactRepo/simple with spaces/");
		URI location = site.toURI();
		assertEquals("1.0", 2, getArtifactKeyCount(location));
	}

	/**
	 * Tests for {@link IMetadataRepositoryManager#refreshRepository(URI, org.eclipse.core.runtime.IProgressMonitor)}.
	 */
	public void testRefresh() throws ProvisionException {
		File site = getTestData("Repositoy", "/testData/artifactRepo/simple/");
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

		// Set it back to null for other tests
		manager.setRepositoryProperty(location, IRepository.PROP_NICKNAME, null);
	}

	/**
	 * Tests for {@link IRepositoryManager#setRepositoryProperty}.
	 */
	public void testSetRepositoryProperty() {
		File site = getTestData("Repositoy", "/testData/artifactRepo/simple/");
		URI location = site.toURI();
		manager.removeRepository(location);
		manager.addRepository(location);

		//set some properties different from what the repository contains
		manager.setRepositoryProperty(location, IRepository.PROP_NAME, "TestName");
		manager.setRepositoryProperty(location, IRepository.PROP_DESCRIPTION, "TestDescription");
		manager.setRepositoryProperty(location, IRepository.PROP_SYSTEM, "true");
		assertEquals("1.0", "TestName", manager.getRepositoryProperty(location, IRepository.PROP_NAME));
		assertEquals("1.1", "TestDescription", manager.getRepositoryProperty(location, IRepository.PROP_DESCRIPTION));
		assertEquals("1.2", "true", manager.getRepositoryProperty(location, IRepository.PROP_SYSTEM));

		//loading the repository should overwrite test values
		try {
			manager.loadRepository(location, getMonitor());
		} catch (ProvisionException e) {
			fail("1.99", e);
		}

		assertEquals("2.0", "Good Test Repository", manager.getRepositoryProperty(location, IRepository.PROP_NAME));
		assertEquals("2.1", "Good test repository description", manager.getRepositoryProperty(location, IRepository.PROP_DESCRIPTION));
		assertEquals("2.2", "false", manager.getRepositoryProperty(location, IRepository.PROP_SYSTEM));
	}

	public void testUpdateSitePathWithSpaces() {
		File site = getTestData("Repository", "/testData/updatesite/site with spaces/");
		URI location = site.toURI();
		assertEquals("1.0", 3, getArtifactKeyCount(location));
	}

	/**
	 * Tests that trailing slashes do not affect repository identity.
	 */
	public void testTrailingSlashes() {
		File site = getTestData("Repository", "/testData/artifactRepo/simple/");
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
			IArtifactRepository repoSlash = manager.loadRepository(locationSlash, null);
			IArtifactRepository repoNoSlash = manager.loadRepository(locationNoSlash, null);
			assertTrue("1.0", repoNoSlash == repoSlash);
		} catch (ProvisionException e) {
			fail("1.99", e);
		}

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
		File site = getTestData("Repositoy", "/testData/artifactRepo/simple/");
		URI location = site.toURI();
		manager.removeRepository(location);
		assertEquals("1.0", false, manager.contains(location));
		manager.addRepository(location);
		assertEquals("1.1", true, manager.contains(location));
		manager.removeRepository(location);
		assertEquals("1.2", false, manager.contains(location));
	}

	/**
	 * Tests parsing a repository with a duplicate element. See bug 255401.
	 */
	public void testDuplicateElement() {
		File duplicateElementXML = getTestData("testDuplicateElement", "testData/artifactRepo/duplicateElement");
		PrintStream out = System.out;
		try {
			System.setOut(new PrintStream(new StringBufferStream()));
			assertEquals("Ensure correct number of artifact keys exist", 2, getArtifactKeyCount(duplicateElementXML.toURI()));
		} finally {
			System.setOut(out);
		}
	}

	public void testEnablement() {
		File site = getTestData("Repository", "/testData/artifactRepo/simple/");
		URI location = site.toURI();
		manager.addRepository(location);
		assertEquals("1.0", true, manager.isEnabled(location));
		TestRepositoryListener listener = new TestRepositoryListener(location);
		getEventBus().addListener(listener);

		manager.setEnabled(location, false);
		listener.waitForEvent();
		assertEquals("2.0", false, listener.lastEnablement);
		assertEquals("2.1", false, manager.isEnabled(location));
		assertEquals("2.2", IRepository.TYPE_ARTIFACT, listener.lastRepoType);
		listener.reset();

		manager.setEnabled(location, true);
		listener.waitForEvent();
		assertEquals("3.0", true, manager.isEnabled(location));
		assertEquals("3.1", RepositoryEvent.ENABLEMENT, listener.lastKind);
		assertEquals("3.2", true, listener.lastEnablement);
		assertEquals("3.3", IRepository.TYPE_ARTIFACT, listener.lastRepoType);
		listener.reset();
	}

	/**
	 * Tests that adding a repository that is already known but disabled
	 * causes the repository to be enabled. See bug 241307 for discussion.
	 */
	public void testEnablementOnAdd() {
		File site = getTestData("Repository", "/testData/artifactRepo/simple/");
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
		assertEquals("1.2", IRepository.TYPE_ARTIFACT, listener.lastRepoType);
	}
}