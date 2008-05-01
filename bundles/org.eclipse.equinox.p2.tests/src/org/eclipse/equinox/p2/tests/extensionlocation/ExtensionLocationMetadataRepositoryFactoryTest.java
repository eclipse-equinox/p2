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
import org.eclipse.equinox.internal.p2.extensionlocation.ExtensionLocationMetadataRepositoryFactory;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class ExtensionLocationMetadataRepositoryFactoryTest extends AbstractProvisioningTest {

	private File tempDirectory;
	private ExtensionLocationMetadataRepositoryFactory factory;

	public ExtensionLocationMetadataRepositoryFactoryTest(String name) {
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
		factory = new ExtensionLocationMetadataRepositoryFactory();
	}

	public void testNonFileURL() {
		try {
			URL nonFileURL = new URL("http://www.eclipse.org");
			factory.load(nonFileURL, getMonitor());
			fail("0.1");
		} catch (ProvisionException e) {
			assertEquals("0.2", ProvisionException.REPOSITORY_NOT_FOUND, e.getStatus().getCode());
		} catch (MalformedURLException e) {
			fail("0.3", e);
		}
	}

	public void testNonExistentFile() {
		File directory = new File(tempDirectory, "nonexistent");
		delete(directory);
		try {
			factory.load(directory.toURL(), getMonitor());
			fail("0.1");
		} catch (ProvisionException e) {
			assertEquals("0.2", ProvisionException.REPOSITORY_NOT_FOUND, e.getStatus().getCode());
		} catch (MalformedURLException e) {
			fail("0.3", e);
		}
	}

	public void testNotDirectory() {
		File file = new File(tempDirectory, "exists.file");
		try {
			file.createNewFile();
			factory.load(file.toURL(), getMonitor());
			fail("0.1");
		} catch (ProvisionException e) {
			assertEquals("0.2", ProvisionException.REPOSITORY_NOT_FOUND, e.getStatus().getCode());
		} catch (MalformedURLException e) {
			fail("0.3", e);
		} catch (IOException e) {
			fail("0.4", e);
		}
	}

	public void testNoFeatureOrPluginsDirectory() {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		try {
			factory.load(directory.toURL(), getMonitor());
			fail("0.1");
		} catch (ProvisionException e) {
			assertEquals("0.2", ProvisionException.REPOSITORY_NOT_FOUND, e.getStatus().getCode());
		} catch (MalformedURLException e) {
			fail("0.3", e);
		}
	}

	public void testEmptyFeatureAndPluginsDirectory() {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		new File(directory, "plugins").mkdir();
		new File(directory, "features").mkdir();
		try {
			factory.load(directory.toURL(), getMonitor());
		} catch (ProvisionException e) {
			fail("0.1");
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
			fail("0.1");
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
			fail("0.1");
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
			fail("0.1");
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
			fail("0.1");
		} catch (ProvisionException e) {
			assertEquals("0.2", ProvisionException.REPOSITORY_NOT_FOUND, e.getStatus().getCode());
		} catch (MalformedURLException e) {
			fail("0.99", e);
		}
	}

	public void testNormalFeaturesandPluginsDirectory() {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		copy("1.0", getTestData("1.1", "/testData/extensionlocation"), directory);
		try {
			URL location = directory.toURL();
			try {
				IMetadataRepository repo = factory.load(location, getMonitor());
				if (repo.query(InstallableUnitQuery.ANY, new Collector(), null).toCollection().size() != 3)
					fail("2.99");
			} catch (ProvisionException ex) {
				fail("2.0");
			}
		} catch (MalformedURLException e) {
			fail("4.99", e);
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
				IMetadataRepository repo = factory.load(location, getMonitor());
				if (repo.query(InstallableUnitQuery.ANY, new Collector(), null).toCollection().size() != 2)
					fail("3.0");
			} catch (ProvisionException ex) {
				fail("2.0");
			}
		} catch (MalformedURLException e) {
			fail("4.99", e);
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
				IMetadataRepository repo = factory.load(location, getMonitor());
				if (repo.query(InstallableUnitQuery.ANY, new Collector(), null).toCollection().size() != 1)
					fail("3.0");
			} catch (ProvisionException ex) {
				fail("2.0");
			}
		} catch (MalformedURLException e) {
			fail("4.99", e);
		}
	}

	public void testEclipseBaseNormalFeaturesandPluginsDirectory() {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		File eclipseDirectory = new File(directory, "eclipse");
		copy("1.0", getTestData("1.1", "/testData/extensionlocation"), eclipseDirectory);
		try {
			IMetadataRepository repo = factory.load(directory.toURL(), getMonitor());
			if (repo.query(InstallableUnitQuery.ANY, new Collector(), null).toCollection().size() != 3)
				fail("3.0");
		} catch (ProvisionException e) {
			fail("2.0");
		} catch (MalformedURLException e) {
			fail("2.99", e);
		}
	}
}
