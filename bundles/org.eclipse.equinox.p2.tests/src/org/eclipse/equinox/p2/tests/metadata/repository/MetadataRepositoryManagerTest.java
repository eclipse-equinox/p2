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
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;

/**
 * Tests for API of {@link IMetadataRepositoryManager}.
 */
public class MetadataRepositoryManagerTest extends AbstractProvisioningTest {
	protected IMetadataRepositoryManager manager;

	public static Test suite() {
		return new TestSuite(MetadataRepositoryManagerTest.class);
	}

	protected void setUp() throws Exception {
		super.setUp();
		manager = (IMetadataRepositoryManager) ServiceHelper.getService(TestActivator.context, IMetadataRepositoryManager.class.getName());
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
		URL[] locations = manager.getKnownRepositories();
		for (int i = 0; i < locations.length; i++) {
			if (locations[i].equals(location))
				return true;
		}
		return false;
	}
}
