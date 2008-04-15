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

	public void testNonFileURL() throws MalformedURLException {
		URL nonFileURL = new URL("http://www.eclipse.org");
		try {
			factory.load(nonFileURL, null);
		} catch (ProvisionException e) {
			if (e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND)
				return;
		}
		fail();
	}

	public void testNonExistentFile() throws MalformedURLException {
		File directory = new File(tempDirectory, "nonexistent");
		delete(directory);
		try {
			factory.load(directory.toURL(), null);
		} catch (ProvisionException e) {
			if (e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND)
				return;
		}
		fail();
	}

	public void testNotDirectory() throws IOException {
		File file = new File(tempDirectory, "exists.file");
		file.createNewFile();
		try {
			factory.load(file.toURL(), null);
		} catch (ProvisionException e) {
			if (e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND)
				return;
		}
		fail();
	}

	public void testNoFeatureOrPluginsDirectory() throws MalformedURLException {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		try {
			factory.load(directory.toURL(), null);
		} catch (ProvisionException e) {
			if (e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND)
				return;
		}
		fail();
	}

	public void testEmptyFeatureAndPluginsDirectory() throws MalformedURLException {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		new File(directory, "plugins").mkdir();
		new File(directory, "features").mkdir();
		try {
			factory.load(directory.toURL(), null);
		} catch (ProvisionException e) {
			fail();
		}
	}

	public void testEmptyFeaturesDirectory() throws MalformedURLException {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		new File(directory, "features").mkdir();
		try {
			factory.load(directory.toURL(), null);
		} catch (ProvisionException e) {
			fail();
		}
	}

	public void testEmptyPluginsDirectory() throws MalformedURLException {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		new File(directory, "plugins").mkdir();
		try {
			factory.load(directory.toURL(), null);
		} catch (ProvisionException e) {
			fail();
		}
	}

	public void testEclipseBaseEmptyFeatureAndPluginsDirectory() throws MalformedURLException {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		File eclipseDirectory = new File(directory, "eclipse");
		eclipseDirectory.mkdir();
		new File(eclipseDirectory, "plugins").mkdir();
		new File(eclipseDirectory, "features").mkdir();
		try {
			factory.load(directory.toURL(), null);
		} catch (ProvisionException e) {
			fail();
		}
	}

	public void testNotEclipseBaseEmptyFeatureAndPluginsDirectory() throws MalformedURLException {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		File eclipseDirectory = new File(directory, "noteclipse");
		eclipseDirectory.mkdir();
		new File(eclipseDirectory, "plugins").mkdir();
		new File(eclipseDirectory, "features").mkdir();
		try {
			factory.load(directory.toURL(), null);
		} catch (ProvisionException e) {
			if (e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND)
				return;
		}
		fail();
	}

	public void testNormalFeaturesandPluginsDirectory() throws IOException {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		copy("1.0", getTestData("1.1", "/testData/extensionlocation"), directory);
		IArtifactRepository repo = null;
		try {
			repo = factory.load(directory.toURL(), null);
		} catch (ProvisionException e) {
			fail();
		}
		if (repo.getArtifactKeys().length != 2)
			fail();
	}

	public void testNormalFeaturesDirectory() throws IOException {
		File directory = new File(tempDirectory, "exists/features");
		directory.mkdirs();
		File features = new File(directory, "features");
		features.mkdir();
		copy("1.0", getTestData("1.1", "/testData/extensionlocation/features"), features);
		IArtifactRepository repo = null;
		try {
			repo = factory.load(directory.toURL(), null);
		} catch (ProvisionException e) {
			fail();
		}
		if (repo.getArtifactKeys().length != 1)
			fail();
	}

	public void testNormalPluginsDirectory() throws IOException {
		File directory = new File(tempDirectory, "exists/plugins");
		directory.mkdirs();
		File plugins = new File(directory, "plugins");
		plugins.mkdir();
		copy("1.0", getTestData("1.1", "/testData/extensionlocation/plugins"), plugins);
		IArtifactRepository repo = null;
		try {
			repo = factory.load(directory.toURL(), null);
		} catch (ProvisionException e) {
			fail();
		}
		if (repo.getArtifactKeys().length != 1)
			fail();
	}

	public void testEclipseBaseNormalFeaturesandPluginsDirectory() throws IOException {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		File eclipseDirectory = new File(directory, "eclipse");
		copy("1.0", getTestData("1.1", "/testData/extensionlocation"), eclipseDirectory);
		IArtifactRepository repo = null;
		try {
			repo = factory.load(directory.toURL(), null);
		} catch (ProvisionException e) {
			fail();
		}
		if (repo.getArtifactKeys().length != 2)
			fail();
	}
}
