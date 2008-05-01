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
package org.eclipse.equinox.p2.tests.extensionlocation;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.equinox.internal.p2.extensionlocation.ExtensionLocationArtifactRepositoryFactory;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;

public class ExtensionLocationArtifactRepositoryFactoryTest extends AbstractProvisioningTest {

	private File tempDirectory;
	private ExtensionLocationArtifactRepositoryFactory factory;

	public ExtensionLocationArtifactRepositoryFactoryTest(String name) {
		super(name);
	}

	protected void tearDown() throws Exception {
		delete(tempDirectory);
		super.tearDown();
	}

	protected void setUp() throws Exception {
		super.setUp();
		String tempDir = System.getProperty("java.io.tmpdir");
		tempDirectory = new File(tempDir, "extensionlocationtest");
		delete(tempDirectory);
		tempDirectory.mkdirs();
		factory = new ExtensionLocationArtifactRepositoryFactory();

	}

	public static File getFile(String path) throws IOException {
		URL fileURL = TestActivator.getContext().getBundle().getEntry(path);
		return new File(FileLocator.toFileURL(fileURL).getPath());
	}

	public void testNonFileURL() {
		try {
			URL nonFileURL = new URL("http://www.eclipse.org");
			factory.load(nonFileURL, getMonitor());
			fail("0.1");
		} catch (ProvisionException e) {
			assertEquals("0.5", ProvisionException.REPOSITORY_NOT_FOUND, e.getStatus().getCode());
		} catch (MalformedURLException e) {
			fail("0.99", e);
		}
	}

	public void testNonExistentFile() {
		File directory = new File(tempDirectory, "nonexistent");
		delete(directory);
		try {
			factory.load(directory.toURL(), getMonitor());
			fail("0.1");
		} catch (ProvisionException e) {
			assertEquals("0.5", ProvisionException.REPOSITORY_NOT_FOUND, e.getStatus().getCode());
		} catch (MalformedURLException e) {
			fail("0.99", e);
		}
	}

	public void testNotDirectory() {
		File file = new File(tempDirectory, "exists.file");
		try {
			file.createNewFile();
			factory.load(file.toURL(), getMonitor());
			fail("0.1");
		} catch (ProvisionException e) {
			assertEquals("0.5", ProvisionException.REPOSITORY_NOT_FOUND, e.getStatus().getCode());
		} catch (MalformedURLException e) {
			fail("0.99", e);
		} catch (IOException e) {
			fail("0.100", e);
		}
	}

	public void testNoFeatureOrPluginsDirectory() {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		try {
			factory.load(directory.toURL(), getMonitor());
		} catch (ProvisionException e) {
			if (e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND)
				return;
		} catch (MalformedURLException e) {
			fail("0.99", e);
		}
		fail("1.0");
	}

	public void testEmptyFeatureAndPluginsDirectory() {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		new File(directory, "plugins").mkdir();
		new File(directory, "features").mkdir();
		try {
			factory.load(directory.toURL(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.1", e);
		} catch (MalformedURLException e) {
			fail("0.99", e);
		}
	}

	public void testEmptyFeaturesDirectory() {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		new File(directory, "features").mkdir();
		try {
			factory.load(directory.toURL(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.1", e);
		} catch (MalformedURLException e) {
			fail("0.99", e);
		}
	}

	public void testEmptyPluginsDirectory() {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		new File(directory, "plugins").mkdir();
		try {
			factory.load(directory.toURL(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.1", e);
		} catch (MalformedURLException e) {
			fail("0.99", e);
		}
	}

	public void testEclipseBaseEmptyFeatureAndPluginsDirectory() {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		File eclipseDirectory = new File(directory, "eclipse");
		eclipseDirectory.mkdir();
		new File(eclipseDirectory, "plugins").mkdir();
		new File(eclipseDirectory, "features").mkdir();
		try {
			factory.load(directory.toURL(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.1", e);
		} catch (MalformedURLException e) {
			fail("0.99", e);
		}
	}

	public void testNotEclipseBaseEmptyFeatureAndPluginsDirectory() {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		File eclipseDirectory = new File(directory, "noteclipse");
		eclipseDirectory.mkdir();
		new File(eclipseDirectory, "plugins").mkdir();
		new File(eclipseDirectory, "features").mkdir();
		try {
			factory.load(directory.toURL(), getMonitor());
		} catch (ProvisionException e) {
			if (e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND)
				return;
		} catch (MalformedURLException e) {
			fail("0.99", e);
		}
		fail("1.0");
	}

	public void testNormalFeaturesandPluginsDirectory() {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		copy("1.0", getTestData("1.1", "/testData/extensionlocation"), directory);
		try {
			URL location = directory.toURL();
			try {
				IArtifactRepository repo = factory.load(location, getMonitor());
				if (repo.getArtifactKeys().length != 2)
					fail("2.1");
			} catch (ProvisionException ex) {
				fail("2.0");
			}
		} catch (MalformedURLException e) {
			fail("3.99", e);
		}
	}

	public void testNormalFeaturesDirectory() {
		File directory = new File(tempDirectory, "exists/features");
		directory.mkdirs();
		File features = new File(directory, "features");
		features.mkdir();
		copy("1.0", getTestData("1.1", "/testData/extensionlocation/features"), features);
		try {
			URL location = directory.toURL();
			try {
				IArtifactRepository repo = factory.load(location, getMonitor());
				if (repo.getArtifactKeys().length != 1)
					fail("2.1");
			} catch (ProvisionException ex) {
				fail("2.0");
			}
		} catch (MalformedURLException e) {
			fail("3.99", e);
		}
	}

	public void testNormalPluginsDirectory() {
		File directory = new File(tempDirectory, "exists/plugins");
		directory.mkdirs();
		File plugins = new File(directory, "plugins");
		plugins.mkdir();
		copy("1.0", getTestData("1.1", "/testData/extensionlocation/plugins"), plugins);
		try {
			URL location = directory.toURL();
			try {
				IArtifactRepository repo = factory.load(location, getMonitor());
				if (repo.getArtifactKeys().length != 1)
					fail("2.1");
			} catch (ProvisionException ex) {
				fail("2.0");
			}
		} catch (MalformedURLException e) {
			fail("3.99", e);
		}
	}

	public void testEclipseBaseNormalFeaturesandPluginsDirectory() {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		File eclipseDirectory = new File(directory, "eclipse");
		copy("1.1", getTestData("1.1", "/testData/extensionlocation"), eclipseDirectory);
		try {
			IArtifactRepository repo = factory.load(directory.toURL(), getMonitor());
			if (repo.getArtifactKeys().length != 2)
				fail("1.0");
		} catch (ProvisionException e) {
			fail("0.5", e);
		} catch (MalformedURLException e) {
			fail("0.99", e);
		}
	}
}
