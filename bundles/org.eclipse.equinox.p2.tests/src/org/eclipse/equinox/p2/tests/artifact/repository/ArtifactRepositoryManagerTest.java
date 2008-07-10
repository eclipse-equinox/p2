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
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;

/**
 * Tests API of {@link IArtifactRepositoryManager}.
 */
public class ArtifactRepositoryManagerTest extends AbstractProvisioningTest {
	private IArtifactRepositoryManager manager;

	public static Test suite() {
		return new TestSuite(ArtifactRepositoryManagerTest.class);
	}

	/**
	 * Returns whether {@link IMetadataRepositoryManager} contains a reference
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

	public void testBasicAddRemove() throws MalformedURLException {
		File tempFile = new File(System.getProperty("java.io.tmpdir"));
		URL location = tempFile.toURL();
		assertTrue(!managerContains(location));
		manager.addRepository(location);
		assertTrue(managerContains(location));
		manager.removeRepository(location);
		assertTrue(!managerContains(location));
	}
}