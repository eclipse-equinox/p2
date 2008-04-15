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
		IMetadataRepository repo = null;
		try {
			repo = factory.load(directory.toURL(), null);
		} catch (ProvisionException e) {
			fail();
		}
		if (repo.query(InstallableUnitQuery.ANY, new Collector(), null).toCollection().size() != 3)
			fail();
	}

	public void testNormalFeaturesDirectory() throws IOException {
		File directory = new File(tempDirectory, "exists/features");
		directory.mkdirs();
		File features = new File(directory, "features");
		features.mkdir();
		copy("1.0", getTestData("1.1", "/testData/extensionlocation/features"), features);
		IMetadataRepository repo = null;
		try {
			repo = factory.load(directory.toURL(), null);
		} catch (ProvisionException e) {
			fail();
		}
		if (repo.query(InstallableUnitQuery.ANY, new Collector(), null).toCollection().size() != 2)
			fail();
	}

	public void testNormalPluginsDirectory() throws IOException {
		File directory = new File(tempDirectory, "exists/plugins");
		directory.mkdirs();
		File plugins = new File(directory, "plugins");
		plugins.mkdir();
		copy("1.0", getTestData("1.1", "/testData/extensionlocation/plugins"), plugins);
		IMetadataRepository repo = null;
		try {
			repo = factory.load(directory.toURL(), null);
		} catch (ProvisionException e) {
			fail();
		}
		if (repo.query(InstallableUnitQuery.ANY, new Collector(), null).toCollection().size() != 1)
			fail();
	}

	public void testEclipseBaseNormalFeaturesandPluginsDirectory() throws IOException {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		File eclipseDirectory = new File(directory, "eclipse");
		copy("1.0", getTestData("1.1", "/testData/extensionlocation"), eclipseDirectory);
		IMetadataRepository repo = null;
		try {
			repo = factory.load(directory.toURL(), null);
		} catch (ProvisionException e) {
			fail();
		}
		if (repo.query(InstallableUnitQuery.ANY, new Collector(), null).toCollection().size() != 3)
			fail();
	}
}
