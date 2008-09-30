/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.p2.tests.*;

/**
 * Tests API of {@link IArtifactRepositoryManager}.
 */
public class ArtifactRepositoryManagerTest extends AbstractProvisioningTest {
	private IArtifactRepositoryManager manager;

	public static Test suite() {
		return new TestSuite(ArtifactRepositoryManagerTest.class);
	}

	/**
	 * Returns whether {@link IArtifactRepositoryManager} contains a reference
	 * to a repository at the given location.
	 */
	private boolean managerContains(URL location) {
		URL[] locations = manager.getKnownRepositories(IArtifactRepositoryManager.REPOSITORIES_ALL);
		for (int i = 0; i < locations.length; i++) {
			if (locations[i].equals(location))
				return true;
		}
		return false;
	}

	protected void setUp() throws Exception {
		super.setUp();
		manager = (IArtifactRepositoryManager) ServiceHelper.getService(TestActivator.context, IArtifactRepositoryManager.class.getName());
	}

	/**
	 * Tests loading a repository that does not exist throws an appropriate exception.
	 */
	public void testLoadMissingRepository() throws IOException {
		File tempFile = File.createTempFile("testLoadMissingArtifactRepository", null);
		URL location = tempFile.toURL();
		try {
			manager.loadRepository(location, null);
			fail("1.0");//should fail
		} catch (ProvisionException e) {
			assertEquals("1.1", IStatus.ERROR, e.getStatus().getSeverity());
			assertEquals("1.2", ProvisionException.REPOSITORY_NOT_FOUND, e.getStatus().getCode());
		}
	}

	/**
	 * Tests loading an artifact repository that is missing the top level repository element.
	 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=246452.
	 */
	public void testLoadMissingRepositoryElement() {
		File site = getTestData("Update site", "/testData/artifactRepo/broken/");
		try {
			URL location = site.toURL();
			manager.loadRepository(location, null);
			//should have failed
			fail("1.0");
		} catch (ProvisionException e) {
			//expected
		} catch (MalformedURLException e) {
			fail("2.99", e);
		}

	}

	/**
	 * Tests that trailing slashes do not affect repository identity.
	 */
	public void testTrailingSlashes() {
		File site = getTestData("Repository", "/testData/artifactRepo/simple/");
		URL locationSlash, locationNoSlash;
		try {
			locationSlash = site.toURL();
			String locationString = locationSlash.toExternalForm();
			locationString = locationString.substring(0, locationString.length() - 1);
			locationNoSlash = new URL(locationString);
		} catch (MalformedURLException e) {
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

	public void testBasicAddRemove() throws MalformedURLException {
		File tempFile = new File(System.getProperty("java.io.tmpdir"));
		URL location = tempFile.toURL();
		assertTrue(!managerContains(location));
		manager.addRepository(location);
		assertTrue(managerContains(location));
		manager.removeRepository(location);
		assertTrue(!managerContains(location));
	}

	public void testEnablement() throws MalformedURLException {
		File site = getTestData("Repository", "/testData/artifactRepo/simple/");
		URL location = site.toURL();
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
	public void testEnablementOnAdd() throws MalformedURLException {
		File site = getTestData("Repository", "/testData/artifactRepo/simple/");
		URL location = site.toURL();
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
}