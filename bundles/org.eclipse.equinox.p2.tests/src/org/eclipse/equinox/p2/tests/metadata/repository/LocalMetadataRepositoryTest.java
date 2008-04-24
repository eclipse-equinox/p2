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
package org.eclipse.equinox.p2.tests.metadata.repository;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.publisher.*;
import org.eclipse.equinox.internal.p2.publisher.actions.EclipseInstallAction;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;

/**
 * Test API of the local metadata repository implementation.
 */
public class LocalMetadataRepositoryTest extends AbstractProvisioningTest {
	private static final String TEST_KEY = "TestKey";
	private static final String TEST_VALUE = "TestValue";
	protected File repoLocation;

	private IMetadataRepositoryManager getMetadataRepositoryManager() {
		return (IMetadataRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IMetadataRepositoryManager.class.getName());
	}

	protected void setUp() throws Exception {
		super.setUp();
		String tempDir = System.getProperty("java.io.tmpdir");
		repoLocation = new File(tempDir, "LocalMetadataRepositoryTest");
		AbstractProvisioningTest.delete(repoLocation);
		repoLocation.mkdir();
	}

	protected void tearDown() throws Exception {
		delete(repoLocation);
		super.tearDown();
	}

	public void testCompressedRepository() throws MalformedURLException, ProvisionException {
		generateTestRepo(true);

		File[] files = repoLocation.listFiles();
		boolean jarFilePresent = false;
		boolean xmlFilePresent = false;
		// one of the files in the repository should be the content.xml.jar
		for (int i = 0; i < files.length; i++) {
			if ("content.jar".equalsIgnoreCase(files[i].getName())) {
				jarFilePresent = true;
			}
			if ("content.xml".equalsIgnoreCase(files[i].getName())) {
				xmlFilePresent = true;
			}
		}
		if (!jarFilePresent) {
			fail("Repository did not create JAR for content.xml");
		}
		if (xmlFilePresent) {
			fail("Repository should not create content.xml");
		}
	}

	private IMetadataRepository generateTestRepo(boolean compressed) throws MalformedURLException, ProvisionException {
		IMetadataRepositoryManager manager = getMetadataRepositoryManager();
		IMetadataRepository repo = manager.createRepository(repoLocation.toURL(), "TestRepo", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY);
		manager.addRepository(repo.getLocation());
		repo.setProperty(IRepository.PROP_COMPRESSED, Boolean.toString(compressed));

		PublisherInfo info = new PublisherInfo();
		info.setMetadataRepository(repo);
		IPublishingAction action = new EclipseInstallAction(repoLocation.getAbsolutePath(), "sdk", "3.3", "Test repo", "tooling", null, null, false);
		// Generate the repository
		IStatus result = new Publisher(info).publish(new IPublishingAction[] {action});
		assertTrue(result.isOK());
		return repo;
	}

	public void testGetProperties() throws MalformedURLException, ProvisionException {
		IMetadataRepositoryManager manager = getMetadataRepositoryManager();
		IMetadataRepository repo = manager.createRepository(repoLocation.toURL(), "TestRepo", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY);
		Map properties = repo.getProperties();
		//attempting to modify the properties should fail
		try {
			properties.put(TEST_KEY, TEST_VALUE);
			fail("Should not allow setting property");
		} catch (RuntimeException e) {
			//expected
		}
	}

	public void testSetProperty() throws MalformedURLException, ProvisionException {
		IMetadataRepositoryManager manager = getMetadataRepositoryManager();
		IMetadataRepository repo = manager.createRepository(repoLocation.toURL(), "TestRepo", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY);
		manager.addRepository(repo.getLocation());
		Map properties = repo.getProperties();
		assertTrue("1.0", !properties.containsKey(TEST_KEY));
		repo.setProperty(TEST_KEY, TEST_VALUE);

		//the previously obtained properties should not be affected by subsequent changes
		assertTrue("1.1", !properties.containsKey(TEST_KEY));
		properties = repo.getProperties();
		assertTrue("1.2", properties.containsKey(TEST_KEY));

		//going back to repo manager, should still get the new property
		repo = manager.loadRepository(repoLocation.toURL(), null);
		properties = repo.getProperties();
		assertTrue("1.3", properties.containsKey(TEST_KEY));

		//setting a null value should remove the key
		repo.setProperty(TEST_KEY, null);
		properties = repo.getProperties();
		assertTrue("1.4", !properties.containsKey(TEST_KEY));
	}

	public void testUncompressedRepository() throws MalformedURLException, ProvisionException {
		generateTestRepo(false);

		File[] files = repoLocation.listFiles();
		boolean jarFilePresent = false;
		// none of the files in the repository should be the content.xml.jar
		for (int i = 0; i < files.length; i++) {
			if ("content.jar".equalsIgnoreCase(files[i].getName())) {
				jarFilePresent = true;
			}
		}
		if (jarFilePresent) {
			fail("Repository should not create JAR for content.xml");
		}

	}
}
