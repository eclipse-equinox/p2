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
import java.net.MalformedURLException;
import java.net.URL;
import junit.framework.TestCase;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.publisher.*;
import org.eclipse.equinox.internal.p2.publisher.actions.EclipseInstallAction;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.tests.TestActivator;

public class JarURLRepositoryTest extends TestCase {

	private IMetadataRepositoryManager manager;
	private File testRepoJar;

	public JarURLRepositoryTest(String name) {
		super(name);
	}

	public JarURLRepositoryTest() {
		this("");
	}

	private static boolean deleteDirectory(File directory) {
		if (directory.exists() && directory.isDirectory()) {
			File[] files = directory.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return directory.delete();
	}

	protected void setUp() throws Exception {
		manager = (IMetadataRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IMetadataRepositoryManager.class.getName());

		URL base = TestActivator.getContext().getBundle().getEntry("/testData/generator/eclipse3.3");
		String tempDir = System.getProperty("java.io.tmpdir");
		File testRepo = new File(tempDir, "testRepo");
		deleteDirectory(testRepo);
		testRepo.mkdir();
		IMetadataRepository repository = manager.createRepository(testRepo.toURL(), "testRepo", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY);
		manager.addRepository(repository.getLocation());

		PublisherInfo info = new PublisherInfo();
		info.setMetadataRepository(repository);
		IPublishingAction action = new EclipseInstallAction(FileLocator.toFileURL(base).getPath(), "sdk", "3.3", "Test repo", "jartest", null, null, false);
		// Generate the repository
		IStatus result = new Publisher(info).publish(new IPublishingAction[] {action});
		assertTrue(result.isOK());

		FileUtils.zip(new File[] {testRepo}, new File(tempDir, "testRepo.jar"));
		testRepoJar = new File(tempDir, "testRepo.jar");
		assertTrue(testRepoJar.exists());
		testRepoJar.deleteOnExit();
		deleteDirectory(testRepo);
	}

	protected void tearDown() throws Exception {
		manager = null;
	}

	public void testJarURLRepository() throws ProvisionException {
		URL jarRepoURL = null;
		try {
			jarRepoURL = new URL("jar:" + testRepoJar.toURL().toString() + "!/testRepo/");
		} catch (MalformedURLException e) {
			fail(e.getMessage());
		}
		IMetadataRepository repo = manager.loadRepository(jarRepoURL, null);
		assertTrue(!repo.query(InstallableUnitQuery.ANY, new Collector(), null).isEmpty());
		manager.removeRepository(jarRepoURL);
	}
}
