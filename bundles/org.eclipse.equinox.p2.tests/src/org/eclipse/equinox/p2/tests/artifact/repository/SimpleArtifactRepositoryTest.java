/*******************************************************************************
 * Copyright (c) 2007 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *		compeople AG (Stefan Liebig) - initial API and implementation
 *		Code 9 - ongoing development
 *		IBM - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.artifact.repository;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.metadata.IArtifactKey;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;
import org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.SimpleArtifactRepositoryFactory;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;

public class SimpleArtifactRepositoryTest extends AbstractProvisioningTest {
	//artifact repository to remove on tear down
	private File repositoryFile = null;
	private URI repositoryURI = null;

	protected void tearDown() throws Exception {
		super.tearDown();
		//repository location is not used by all tests
		if (repositoryURI != null) {
			getArtifactRepositoryManager().removeRepository(repositoryURI);
			repositoryURI = null;
		}
		if (repositoryFile != null) {
			delete(repositoryFile);
			repositoryFile = null;
		}
	}

	public void testGetActualLocation1() throws Exception {
		URI base = new URI("http://localhost/artifactRepository");
		assertEquals(new URI(base + "/artifacts.xml"), SimpleArtifactRepository.getActualLocation(base, false));
	}

	public void testGetActualLocation2() throws Exception {
		URI base = new URI("http://localhost/artifactRepository/");
		assertEquals(new URI(base + "artifacts.xml"), SimpleArtifactRepository.getActualLocation(base, false));
	}

	public void testGetActualLocation3() throws Exception {
		URI base = new URI("http://localhost/artifactRepository/artifacts.xml");
		assertEquals(base, SimpleArtifactRepository.getActualLocation(base, false));
	}

	public void testGetActualLocationCompressed1() throws Exception {
		URI base = new URI("http://localhost/artifactRepository");
		assertEquals(new URI(base + "/artifacts.jar"), SimpleArtifactRepository.getActualLocation(base, true));
	}

	public void testGetActualLocationCompressed2() throws Exception {
		URI base = new URI("http://localhost/artifactRepository/");
		assertEquals(new URI(base + "artifacts.jar"), SimpleArtifactRepository.getActualLocation(base, true));
	}

	public void testGetActualLocationCompressed3() throws Exception {
		URI base = new URI("http://localhost/artifactRepository/artifacts.jar");
		assertEquals(base, SimpleArtifactRepository.getActualLocation(base, true));
	}

	public void testCompressedRepository() throws URISyntaxException, ProvisionException {
		IArtifactRepositoryManager artifactRepositoryManager = getArtifactRepositoryManager();
		String tempDir = System.getProperty("java.io.tmpdir");
		repositoryFile = new File(tempDir, "SimpleArtifactRepositoryTest");
		delete(repositoryFile);
		repositoryURI = repositoryFile.toURI();
		Map properties = new HashMap();
		properties.put(IRepository.PROP_COMPRESSED, "true");
		IArtifactRepository repo = artifactRepositoryManager.createRepository(repositoryURI, "artifact name", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);

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

	public void testUncompressedRepository() throws URISyntaxException, ProvisionException {
		IArtifactRepositoryManager artifactRepositoryManager = getArtifactRepositoryManager();
		String tempDir = System.getProperty("java.io.tmpdir");
		repositoryFile = new File(tempDir, "SimpleArtifactRepositoryTest");
		delete(repositoryFile);
		repositoryURI = repositoryFile.toURI();
		Map properties = new HashMap();
		properties.put(IRepository.PROP_COMPRESSED, "false");
		IArtifactRepository repo = artifactRepositoryManager.createRepository(repositoryURI, "artifact name", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);

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

	public void testLoadInvalidLocation() {
		try {
			getArtifactRepositoryManager().loadRepository(new URI("file:d:/foo"), getMonitor());
		} catch (ProvisionException e) {
			//expected
		} catch (URISyntaxException e) {
			fail("4.99", e);
		}
	}

	public void test_248772() {
		SimpleArtifactRepositoryFactory factory = new SimpleArtifactRepositoryFactory();
		URI location = null;
		location = new File(getTempFolder(), getUniqueString()).toURI();
		factory.create(location, "test type", null, null);
		try {
			//bug 248951, ask for a modifiable repo
			IRepository repo = factory.load(location, IRepositoryManager.REPOSITORY_HINT_MODIFIABLE, new NullProgressMonitor());
			assertNotNull(repo);
			assertTrue(repo.isModifiable());
		} catch (ProvisionException e) {
			fail("2.0", e);
		}
	}

	public void testErrorStatus() {
		class TestStep extends ProcessingStep {
			IStatus myStatus;

			public TestStep(IStatus status) {
				this.myStatus = status;
			}

			public void close() throws IOException {
				setStatus(myStatus);
				super.close();
			}
		}
		repositoryURI = getTestData("Loading test data", "testData/artifactRepo/simple").toURI();
		repositoryFile = new File(getTempFolder(), getUniqueString());

		IArtifactRepository repo = null;
		try {
			repo = getArtifactRepositoryManager().loadRepository(repositoryURI, new NullProgressMonitor());
		} catch (ProvisionException e) {
			fail("Failed to create repository", e);
		}
		IArtifactDescriptor descriptor = new ArtifactDescriptor(new ArtifactKey("osgi.bundle", "aaPlugin", new Version("1.0.0")));

		OutputStream out = null;
		try {
			TestStep errStep = new TestStep(new Status(IStatus.ERROR, "plugin", "Error Step Message"));
			TestStep warnStep = new TestStep(new Status(IStatus.WARNING, "plugin", "Warning Step Message"));
			TestStep okStep = new TestStep(Status.OK_STATUS);
			out = new FileOutputStream(repositoryFile);
			(new ProcessingStepHandler()).link(new ProcessingStep[] {okStep, errStep, warnStep}, out, new NullProgressMonitor());
			IStatus status = repo.getRawArtifact(descriptor, okStep, new NullProgressMonitor());
			out.close();

			// Only the error step should be collected
			assertFalse(status.isOK());
			assertTrue("Unexpected Severity", status.matches(IStatus.ERROR));
			assertEquals(1, status.getChildren().length);

			errStep = new TestStep(new Status(IStatus.ERROR, "plugin", "Error Step Message"));
			warnStep = new TestStep(new Status(IStatus.WARNING, "plugin", "Warning Step Message"));
			TestStep warnStep2 = new TestStep(new Status(IStatus.WARNING, "plugin", "2 - Warning Step Message"));
			okStep = new TestStep(Status.OK_STATUS);
			out = new FileOutputStream(repositoryFile);
			(new ProcessingStepHandler()).link(new ProcessingStep[] {okStep, warnStep, errStep, warnStep2}, out, new NullProgressMonitor());
			status = repo.getRawArtifact(descriptor, okStep, new NullProgressMonitor());
			out.close();

			// The first warning step and the error step should be collected 
			assertFalse(status.isOK());
			assertTrue("Unexpected Severity", status.matches(IStatus.ERROR));
			assertEquals(2, status.getChildren().length);

		} catch (IOException e) {
			fail("Failed to create ouptut stream", e);
		} finally {
			if (out != null)
				try {
					out.close();
				} catch (IOException e) {
					// don't care
				}
		}

	}

	/*
	 * Test that the appropriate location for a packed feature is returned.
	 */
	public void testProperPackedFeatureLocation() {
		try {
			repositoryFile = getTempFolder();
			repositoryURI = repositoryFile.toURI();

			// Create a descriptor for a packed repo
			ArtifactDescriptor descriptor = new ArtifactDescriptor(new ArtifactKey("org.eclipse.update.feature", "test", Version.parseVersion("1.0.0")));
			descriptor.setProperty(IArtifactDescriptor.FORMAT, "packed");
			descriptor.setProcessingSteps(new ProcessingStepDescriptor[] {new ProcessingStepDescriptor("org.eclipse.equinox.p2.processing.Pack200Unpacker", null, true)});

			// Create repository
			Map properties = new HashMap();
			properties.put("publishPackFilesAsSiblings", Boolean.TRUE.toString());
			SimpleArtifactRepository repo = (SimpleArtifactRepository) getArtifactRepositoryManager().createRepository(repositoryURI, "My Repo", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, properties);

			URI location = repo.getLocation(descriptor);
			assertNotNull("Null location returned", location);
			assertTrue("Unexpected location", location.toString().endsWith(".pack.gz"));
		} catch (Exception e) {
			fail("Test failed", e);
		}
	}
}
