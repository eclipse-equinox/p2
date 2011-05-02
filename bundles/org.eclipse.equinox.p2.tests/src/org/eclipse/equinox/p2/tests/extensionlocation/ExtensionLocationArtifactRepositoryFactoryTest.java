/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.extensionlocation;

import java.io.File;
import java.io.IOException;
import java.net.*;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.equinox.internal.p2.extensionlocation.Constants;
import org.eclipse.equinox.internal.p2.extensionlocation.ExtensionLocationArtifactRepositoryFactory;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
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
		factory.setAgent(getAgent());
	}

	public static File getFile(String path) throws IOException {
		URL fileURL = TestActivator.getContext().getBundle().getEntry(path);
		return new File(FileLocator.toFileURL(fileURL).getPath());
	}

	public void testNonFileURL() {
		try {
			URI nonFileURL = new URI("http://www.eclipse.org");
			factory.load(nonFileURL, 0, getMonitor());
			fail("0.1");
		} catch (ProvisionException e) {
			assertEquals("0.5", ProvisionException.REPOSITORY_NOT_FOUND, e.getStatus().getCode());
		} catch (URISyntaxException e) {
			fail("0.99", e);
		}
	}

	public void testNonExistentFile() {
		File directory = new File(tempDirectory, "nonexistent");
		delete(directory);
		try {
			factory.load(directory.toURI(), 0, getMonitor());
			fail("0.1");
		} catch (ProvisionException e) {
			assertEquals("0.5", ProvisionException.REPOSITORY_NOT_FOUND, e.getStatus().getCode());
		}
	}

	public void testNotDirectory() {
		File file = new File(tempDirectory, "exists.file");
		try {
			file.createNewFile();
			factory.load(file.toURI(), 0, getMonitor());
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
			factory.load(directory.toURI(), 0, getMonitor());
		} catch (ProvisionException e) {
			if (e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND)
				return;
		}
		fail("1.0");
	}

	public void testEmptyFeatureAndPluginsDirectory() {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		new File(directory, "plugins").mkdir();
		new File(directory, "features").mkdir();
		try {
			factory.load(directory.toURI(), 0, getMonitor());
		} catch (ProvisionException e) {
			fail("0.1", e);
		}
	}

	public void testEmptyFeaturesDirectory() {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		new File(directory, "features").mkdir();
		try {
			factory.load(directory.toURI(), 0, getMonitor());
		} catch (ProvisionException e) {
			fail("0.1", e);
		}
	}

	public void testEmptyPluginsDirectory() {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		new File(directory, "plugins").mkdir();
		try {
			factory.load(directory.toURI(), 0, getMonitor());
		} catch (ProvisionException e) {
			fail("0.1", e);
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
			factory.load(directory.toURI(), 0, getMonitor());
		} catch (ProvisionException e) {
			fail("0.1", e);
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
			factory.load(directory.toURI(), 0, getMonitor());
		} catch (ProvisionException e) {
			if (e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND)
				return;
		}
		fail("1.0");
	}

	public void testNormalFeaturesandPluginsDirectory() {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		copy("1.0", getTestData("1.1", "/testData/extensionlocation"), directory);
		URI location = directory.toURI();
		try {
			IArtifactRepository repo = factory.load(location, 0, getMonitor());
			if (getArtifactKeyCount(repo) != 2)
				fail("2.1");
		} catch (ProvisionException ex) {
			fail("2.0");
		}
	}

	public void testNormalFeaturesDirectory() {
		File directory = new File(tempDirectory, "exists/features");
		directory.mkdirs();
		File features = new File(directory, "features");
		features.mkdir();
		copy("1.0", getTestData("1.1", "/testData/extensionlocation/features"), features);
		URI location = directory.toURI();
		try {
			IArtifactRepository repo = factory.load(location, 0, getMonitor());
			if (getArtifactKeyCount(repo) != 1)
				fail("2.1");
		} catch (ProvisionException ex) {
			fail("2.0");
		}
	}

	public void testNormalPluginsDirectory() {
		File directory = new File(tempDirectory, "exists/plugins");
		directory.mkdirs();
		File plugins = new File(directory, "plugins");
		plugins.mkdir();
		copy("1.0", getTestData("1.1", "/testData/extensionlocation/plugins"), plugins);
		URI location = directory.toURI();
		try {
			IArtifactRepository repo = factory.load(location, 0, getMonitor());
			if (getArtifactKeyCount(repo) != 1)
				fail("2.1");
		} catch (ProvisionException ex) {
			fail("2.0");
		}
	}

	public void testEclipseBaseNormalFeaturesandPluginsDirectory() {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		File eclipseDirectory = new File(directory, "eclipse");
		copy("1.1", getTestData("1.1", "/testData/extensionlocation"), eclipseDirectory);
		try {
			IArtifactRepository repo = factory.load(directory.toURI(), 0, getMonitor());
			if (getArtifactKeyCount(repo) != 2)
				fail("1.0");
		} catch (ProvisionException e) {
			fail("0.5", e);
		}
	}

	public void testEclipseBaseModifiableRepository() {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		File eclipseDirectory = new File(directory, "eclipse");
		copy("1.1", getTestData("1.1", "/testData/extensionlocation"), eclipseDirectory);
		try {
			IArtifactRepository repo = factory.load(directory.toURI(), IRepositoryManager.REPOSITORY_HINT_MODIFIABLE, getMonitor());
			assertNull("1.0", repo);
		} catch (ProvisionException e) {
			fail("0.5", e);
		}
	}

	public void testUpdateSiteXMLURL() {
		File site = getTestData("0.1", "/testData/updatesite/site");
		try {
			factory.load(site.toURI(), 0, getMonitor());
		} catch (ProvisionException e) {
			if (e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND)
				return;
		}
		fail("1.0");
	}

	public void testXXXSiteXXXXMLURL() {
		File site = getTestData("0.1", "/testData/updatesite/xxxsitexxx");
		try {
			factory.load(site.toURI(), 0, getMonitor());
		} catch (ProvisionException e) {
			if (e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND)
				return;
		}
		fail("1.0");
	}

	public void testArtifactsXMLFeaturesandPluginsDirectory() throws IOException {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		File artifactsXML = new File(directory, "artifacts.xml");
		artifactsXML.createNewFile();

		copy("1.0", getTestData("1.1", "/testData/extensionlocation"), directory);
		URI location = directory.toURI();
		try {
			factory.load(location, 0, getMonitor());
		} catch (ProvisionException e) {
			if (e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND)
				return;
		}
		fail("1.0");
	}

	public void testArtifactsXMLFeaturesandPluginsDirectoryWithExtensionLocation() throws IOException {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		File artifactsXML = new File(directory, "artifacts.xml");
		artifactsXML.createNewFile();

		copy("1.0", getTestData("1.1", "/testData/extensionlocation"), directory);
		File extensionLocation = new File(tempDirectory.getAbsolutePath() + Constants.EXTENSION_LOCATION);
		URI location = extensionLocation.toURI();
		try {
			factory.load(location, 0, getMonitor());
		} catch (ProvisionException e) {
			if (e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND)
				return;
		}
		fail("1.0");
	}

	public void testContentXMLFeaturesandPluginsDirectory() throws IOException {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		File contentXML = new File(directory, "content.xml");
		contentXML.createNewFile();

		copy("1.0", getTestData("1.1", "/testData/extensionlocation"), directory);
		URI location = directory.toURI();
		try {
			factory.load(location, 0, getMonitor());
		} catch (ProvisionException e) {
			if (e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND)
				return;
		}
		fail("1.0");
	}

	public void testCompositeArtifactsXMLFeaturesandPluginsDirectory() throws IOException {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		File compositeArtifactsXML = new File(directory, "compositeArtifacts.xml");
		compositeArtifactsXML.createNewFile();

		copy("1.0", getTestData("1.1", "/testData/extensionlocation"), directory);
		URI location = directory.toURI();
		try {
			factory.load(location, 0, getMonitor());
		} catch (ProvisionException e) {
			if (e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND)
				return;
		}
		fail("1.0");
	}

	public void testCompositeContentXMLFeaturesandPluginsDirectory() throws IOException {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		File compositeContentXML = new File(directory, "compositeContent.xml");
		compositeContentXML.createNewFile();

		copy("1.0", getTestData("1.1", "/testData/extensionlocation"), directory);
		URI location = directory.toURI();
		try {
			factory.load(location, 0, getMonitor());
		} catch (ProvisionException e) {
			if (e.getStatus().getCode() == ProvisionException.REPOSITORY_NOT_FOUND)
				return;
		}
		fail("1.0");
	}
}
