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
import org.eclipse.equinox.internal.p2.extensionlocation.Constants;
import org.eclipse.equinox.internal.p2.extensionlocation.ExtensionLocationMetadataRepositoryFactory;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
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
		factory.setAgent(getAgent());
	}

	public void testNonFileURL() {
		try {
			URI nonFileURL = new URI("http://www.eclipse.org");
			factory.load(nonFileURL, 0, getMonitor());
			fail("0.1");
		} catch (ProvisionException e) {
			assertEquals("0.2", ProvisionException.REPOSITORY_NOT_FOUND, e.getStatus().getCode());
		} catch (URISyntaxException e) {
			fail("0.3", e);
		}
	}

	public void testNonExistentFile() {
		File directory = new File(tempDirectory, "nonexistent");
		delete(directory);
		try {
			factory.load(directory.toURI(), 0, getMonitor());
			fail("0.1");
		} catch (ProvisionException e) {
			assertEquals("0.2", ProvisionException.REPOSITORY_NOT_FOUND, e.getStatus().getCode());
		}
	}

	public void testNotDirectory() {
		File file = new File(tempDirectory, "exists.file");
		try {
			file.createNewFile();
			factory.load(file.toURI(), 0, getMonitor());
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
			factory.load(directory.toURI(), 0, getMonitor());
			fail("0.1");
		} catch (ProvisionException e) {
			assertEquals("0.2", ProvisionException.REPOSITORY_NOT_FOUND, e.getStatus().getCode());
		}
	}

	public void testEmptyFeatureAndPluginsDirectory() {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		new File(directory, "plugins").mkdir();
		new File(directory, "features").mkdir();
		try {
			factory.load(directory.toURI(), 0, getMonitor());
		} catch (ProvisionException e) {
			fail("0.1");
		}
	}

	public void testEmptyFeaturesDirectory() {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		new File(directory, "features").mkdir();
		try {
			factory.load(directory.toURI(), 0, getMonitor());
		} catch (ProvisionException e) {
			fail("0.1");
		}
	}

	public void testEmptyPluginsDirectory() {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		new File(directory, "plugins").mkdir();
		try {
			factory.load(directory.toURI(), 0, getMonitor());
		} catch (ProvisionException e) {
			fail("0.1");
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
			fail("0.1");
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
			fail("0.1");
		} catch (ProvisionException e) {
			assertEquals("0.2", ProvisionException.REPOSITORY_NOT_FOUND, e.getStatus().getCode());
		}
	}

	public void testNormalFeaturesandPluginsDirectory() {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		copy("1.0", getTestData("1.1", "/testData/extensionlocation"), directory);
		URI location = directory.toURI();
		try {
			IMetadataRepository repo = factory.load(location, 0, getMonitor());
			if (queryResultSize(repo.query(QueryUtil.createIUAnyQuery(), null)) != 3)
				fail("2.99");
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
			IMetadataRepository repo = factory.load(location, 0, getMonitor());
			if (queryResultSize(repo.query(QueryUtil.createIUAnyQuery(), null)) != 2)
				fail("3.0");
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
			IMetadataRepository repo = factory.load(location, 0, getMonitor());
			if (queryResultSize(repo.query(QueryUtil.createIUAnyQuery(), null)) != 1)
				fail("3.0");
		} catch (ProvisionException ex) {
			fail("2.0");
		}
	}

	public void testEclipseBaseNormalFeaturesandPluginsDirectory() {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		File eclipseDirectory = new File(directory, "eclipse");
		copy("1.0", getTestData("1.1", "/testData/extensionlocation"), eclipseDirectory);
		try {
			IMetadataRepository repo = factory.load(directory.toURI(), 0, getMonitor());
			if (queryResultSize(repo.query(QueryUtil.createIUAnyQuery(), null)) != 3)
				fail("3.0");
		} catch (ProvisionException e) {
			fail("2.0");
		}
	}

	public void testEclipseBaseModifiableRepository() {
		File directory = new File(tempDirectory, "exists");
		directory.mkdirs();
		File eclipseDirectory = new File(directory, "eclipse");
		copy("1.0", getTestData("1.1", "/testData/extensionlocation"), eclipseDirectory);
		try {
			IMetadataRepository repo = factory.load(directory.toURI(), IRepositoryManager.REPOSITORY_HINT_MODIFIABLE, getMonitor());
			assertNull("3.0", repo);
		} catch (ProvisionException e) {
			fail("2.0");
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
