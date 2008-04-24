/*******************************************************************************
 * Copyright (c) 2007 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import junit.framework.TestCase;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.publisher.*;
import org.eclipse.equinox.internal.p2.publisher.actions.EclipseInstallAction;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.tests.TestActivator;

public class SimpleArtifactRepositoryTest extends TestCase {

	public void testGetActualLocation1() throws MalformedURLException {
		URL base = new URL("http://localhost/artifactRepository");
		assertEquals(new URL(base + "/artifacts.xml"), SimpleArtifactRepository.getActualLocation(base, false));
	}

	public void testGetActualLocation2() throws MalformedURLException {
		URL base = new URL("http://localhost/artifactRepository/");
		assertEquals(new URL(base + "artifacts.xml"), SimpleArtifactRepository.getActualLocation(base, false));
	}

	public void testGetActualLocation3() throws MalformedURLException {
		URL base = new URL("http://localhost/artifactRepository/artifacts.xml");
		assertEquals(base, SimpleArtifactRepository.getActualLocation(base, false));
	}

	public void testGetActualLocationCompressed1() throws MalformedURLException {
		URL base = new URL("http://localhost/artifactRepository");
		assertEquals(new URL(base + "/artifacts.jar"), SimpleArtifactRepository.getActualLocation(base, true));
	}

	public void testGetActualLocationCompressed2() throws MalformedURLException {
		URL base = new URL("http://localhost/artifactRepository/");
		assertEquals(new URL(base + "artifacts.jar"), SimpleArtifactRepository.getActualLocation(base, true));
	}

	public void testGetActualLocationCompressed3() throws MalformedURLException {
		URL base = new URL("http://localhost/artifactRepository/artifacts.jar");
		assertEquals(base, SimpleArtifactRepository.getActualLocation(base, true));
	}

	private IArtifactRepositoryManager getArtifactRepositoryManager() {
		return (IArtifactRepositoryManager) ServiceHelper.getService(TestActivator.getContext(), IArtifactRepositoryManager.class.getName());
	}

	private IArtifactRepository generateTestRepo(boolean compressed) throws MalformedURLException, ProvisionException {
		IArtifactRepositoryManager artifactRepositoryManager = getArtifactRepositoryManager();
		String tempDir = System.getProperty("java.io.tmpdir");
		File repoLocation = new File(tempDir, "SimpleArtifactRepositoryTest");
		IArtifactRepository repo = artifactRepositoryManager.createRepository(repoLocation.toURL(), "artifact name", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY);
		artifactRepositoryManager.addRepository(repo.getLocation());
		repo.setProperty(IRepository.PROP_COMPRESSED, Boolean.toString(compressed));

		PublisherInfo info = new PublisherInfo();
		info.setArtifactRepository(repo);
		IPublishingAction action = new EclipseInstallAction(repoLocation.getAbsolutePath(), "sdk", "3.3", "Test repo", "tooling", null, null, false);
		// Generate the repository
		IStatus result = new Publisher(info).publish(new IPublishingAction[] {action});
		assertTrue(result.isOK());
		return repo;
	}

	public void testCompressedRepository() throws MalformedURLException, ProvisionException {
		IArtifactRepository repo = generateTestRepo(true);
		File repoLocation = new File(repo.getLocation().getPath());
		File files[] = repoLocation.listFiles();
		boolean jarFilePresent = false;
		boolean artifactFilePresent = false;
		for (int i = 0; i < files.length; i++) {
			if ("artifacts.jar".equalsIgnoreCase(files[i].getName())) {
				jarFilePresent = true;
			}
			if ("artifacts.xml".equalsIgnoreCase(files[i].getName())) {
				artifactFilePresent = false;
			}
		}
		if (!jarFilePresent)
			fail("Repository should create JAR for artifact.xml");
		if (artifactFilePresent)
			fail("Repository should not create artifact.xml");
		delete(repoLocation);
	}

	public void testUncompressedRepository() throws MalformedURLException, ProvisionException {
		IArtifactRepository repo = generateTestRepo(false);
		File repoLocation = new File(repo.getLocation().getPath());
		File files[] = repoLocation.listFiles();
		boolean jarFilePresent = false;
		boolean artifactFilePresent = false;
		for (int i = 0; i < files.length; i++) {
			if ("artifacts.jar".equalsIgnoreCase(files[i].getName())) {
				jarFilePresent = true;
			}
			if ("artifacts.xml".equalsIgnoreCase(files[i].getName())) {
				artifactFilePresent = true;
			}
		}
		if (jarFilePresent)
			fail("Repository should not create JAR for artifact.xml");
		if (!artifactFilePresent)
			fail("Repository should create artifact.xml");
		delete(repoLocation);
	}

	private boolean delete(File file) {
		if (!file.exists())
			return true;
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			for (int i = 0; i < children.length; i++)
				delete(children[i]);
		}
		return file.delete();
	}
}
