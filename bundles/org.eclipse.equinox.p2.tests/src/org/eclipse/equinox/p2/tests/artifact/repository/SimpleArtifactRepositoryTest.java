/*******************************************************************************
 * Copyright (c) 2007 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 *     Code 9 - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.osgi.framework.Version;

public class SimpleArtifactRepositoryTest extends TestCase {
	//artifact repository to remove on tear down
	private File repositoryFile = null;
	private URL repositoryURL = null;

	protected void tearDown() throws Exception {
		super.tearDown();
		//repository location is not used by all tests
		if (repositoryURL != null) {
			getArtifactRepositoryManager().removeRepository(repositoryURL);
			repositoryURL = null;
		}
		if (repositoryFile != null) {
			delete(repositoryFile);
			repositoryFile = null;
		}
	}

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

	public void testCompressedRepository() throws MalformedURLException, ProvisionException {
		IArtifactRepositoryManager artifactRepositoryManager = getArtifactRepositoryManager();
		String tempDir = System.getProperty("java.io.tmpdir");
		repositoryFile = new File(tempDir, "SimpleArtifactRepositoryTest");
		delete(repositoryFile);
		repositoryURL = repositoryFile.toURL();
		Map properties = new HashMap();
		properties.put(IRepository.PROP_COMPRESSED, "true");
		IArtifactRepository repo = artifactRepositoryManager.createRepository(repositoryURL, "artifact name", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);

		IArtifactKey key = PublisherHelper.createBinaryArtifactKey("testKeyId", new Version("1.2.3"));
		IArtifactDescriptor descriptor = PublisherHelper.createArtifactDescriptor(key, null);
		repo.addDescriptor(descriptor);

		File files[] = repositoryFile.listFiles();
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
		delete(repositoryFile);

		if (!jarFilePresent)
			fail("Repository should create JAR for artifact.xml");
		if (artifactFilePresent)
			fail("Repository should not create artifact.xml");
	}

	public void testUncompressedRepository() throws MalformedURLException, ProvisionException {
		IArtifactRepositoryManager artifactRepositoryManager = getArtifactRepositoryManager();
		String tempDir = System.getProperty("java.io.tmpdir");
		repositoryFile = new File(tempDir, "SimpleArtifactRepositoryTest");
		delete(repositoryFile);
		repositoryURL = repositoryFile.toURL();
		Map properties = new HashMap();
		properties.put(IRepository.PROP_COMPRESSED, "false");
		IArtifactRepository repo = artifactRepositoryManager.createRepository(repositoryURL, "artifact name", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);

		IArtifactKey key = PublisherHelper.createBinaryArtifactKey("testKeyId", new Version("1.2.3"));
		IArtifactDescriptor descriptor = PublisherHelper.createArtifactDescriptor(key, null);
		repo.addDescriptor(descriptor);

		File files[] = repositoryFile.listFiles();
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
		delete(repositoryFile);

		if (jarFilePresent)
			fail("Repository should not create JAR for artifact.xml");
		if (!artifactFilePresent)
			fail("Repository should create artifact.xml");
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
