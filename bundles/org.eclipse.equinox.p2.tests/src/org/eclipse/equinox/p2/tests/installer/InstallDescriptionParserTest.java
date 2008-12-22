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
package org.eclipse.equinox.p2.tests.installer;

import java.io.File;
import java.io.IOException;
import java.net.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.internal.p2.installer.InstallDescriptionParser;
import org.eclipse.equinox.internal.p2.installer.VersionedName;
import org.eclipse.equinox.internal.provisional.p2.installer.InstallDescription;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestData;
import org.eclipse.equinox.internal.provisional.p2.core.Version;

/**
 * Tests install description parser.
 */
public class InstallDescriptionParserTest extends AbstractProvisioningTest {

	protected void assertEquals(String message, InstallDescription expected, InstallDescription actual) {
		assertEquals(message, expected.getLauncherName(), actual.getLauncherName());
		assertEquals(message, expected.getProductName(), actual.getProductName());
		assertEquals(message, expected.getAgentLocation(), actual.getAgentLocation());
		assertEquals(message, expected.getArtifactRepositories(), actual.getArtifactRepositories());
		assertEquals(message, expected.getBundleLocation(), actual.getBundleLocation());
		assertEquals(message, expected.getInstallLocation(), actual.getInstallLocation());
		assertEquals(message, expected.getMetadataRepositories(), actual.getMetadataRepositories());
		assertEquals(message, expected.getProfileProperties(), actual.getProfileProperties());
		assertEquals(message, expected.getRoots(), actual.getRoots());
		assertEquals(message, expected.isAutoStart(), actual.isAutoStart());
	}

	private InstallDescription loadDescription(String filename) {
		URL location = null;
		try {
			location = TestData.getFile("installer", filename).toURL();
		} catch (IOException e) {
			fail("0.99", e);
		}
		InstallDescription description = null;
		try {
			description = InstallDescriptionParser.createDescription(location.toExternalForm(), SubMonitor.convert(getMonitor()));
		} catch (IOException e) {
			fail("1.99", e);
		}
		return description;
	}

	public void testGetArrayFromString() {
		String[] elements = InstallDescriptionParser.getArrayFromString(null, ".");
		assertEquals("1.0", 0, elements.length);
	}

	/**
	 * Tests loading an empty install-description file
	 */
	public void testLoadEmpty() {
		InstallDescription description = loadDescription("empty.properties");
		//assert it is the same as a new empty description
		assertEquals("1.0", new InstallDescription(), description);
	}

	/**
	 * Tests loading an install description file relative to the running instance.
	 */
	public void testLoadRelativeDescription() {
		try {
			InstallDescription description = InstallDescriptionParser.createDescription(null, SubMonitor.convert(getMonitor()));
			//shouldn't find anything, so equal to default description
			assertEquals("1.0", new InstallDescription(), description);
		} catch (IOException e) {
			fail("0.99", e);
		}
	}

	/**
	 * Tests loading a well-formed description file where all properties are specified.
	 */
	public void testLoadGoodDescription() {
		InstallDescription description = loadDescription("good.properties");
		URI[] artifactRepositories = description.getArtifactRepositories();
		assertEquals("1.0", 2, artifactRepositories.length);
		assertEquals("1.1", "http://update.eclipse.org/eclipse/someUpdateSite/", artifactRepositories[0].toString());
		assertEquals("1.1", "http://update.eclipse.org/eclipse/someArtifacts/", artifactRepositories[1].toString());
		URI[] metadataRepositories = description.getMetadataRepositories();
		assertEquals("1.2", 2, metadataRepositories.length);
		assertEquals("1.3", "http://update.eclipse.org/eclipse/someUpdateSite/", metadataRepositories[0].toString());
		assertEquals("1.3", "http://update.eclipse.org/eclipse/someMetadata/", metadataRepositories[1].toString());
		assertEquals("1.4", "testFlavor", description.getProfileProperties().get("eclipse.p2.flavor"));
		assertEquals("1.5", "Test Profile Name", description.getProductName());
		assertEquals("1.5", "testLauncherName", description.getLauncherName());
		VersionedName[] roots = description.getRoots();
		assertEquals("1.7", 2, roots.length);
		assertEquals("1.8", "testRoot", roots[0].getId());
		assertEquals("1.9", new Version("2.0"), roots[0].getVersion());
		assertEquals("1.8", "anotherRoot", roots[1].getId());
		assertEquals("1.9", new Version("1.0.1"), roots[1].getVersion());
		assertTrue("1.10", !description.isAutoStart());
		assertEquals("1.11", new Path("/tmp/agent/"), description.getAgentLocation());
		assertEquals("1.12", new Path("/tmp/bundles/"), description.getBundleLocation());
		assertEquals("1.13", new Path("/tmp/install/"), description.getInstallLocation());
	}

	public void testLoadBadDescription() {
		InstallDescription description = loadDescription("bad.properties");
		//nothing in this description is valid, so it should be the same as an empty description
		assertEquals("1.0", new InstallDescription(), description);
	}

	/**
	 * Tests loading a missing install description
	 */
	public void testLoadMissing() {
		try {
			InstallDescriptionParser.createDescription(new File("/does/not/exist/InstallDescriptionParserTest").toURL().toExternalForm(), SubMonitor.convert(getMonitor()));
			fail("1.0");//should have failed
		} catch (MalformedURLException e) {
			fail("0.99", e);
		} catch (IOException e) {
			//expected
		}
	}

	/**
	 * Tests loading the install description file for the Eclipse project SDK.
	 */
	public void testLoadSDKDescription() {
		InstallDescription description = loadDescription("sdk-installer.properties");
		URI[] artifactRepositories = description.getArtifactRepositories();
		assertEquals("1.0", 1, artifactRepositories.length);
		assertEquals("1.1", "http://update.eclipse.org/eclipse/testUpdates/", artifactRepositories[0].toString());
		URI[] metadataRepositories = description.getMetadataRepositories();
		assertEquals("1.2", 1, metadataRepositories.length);
		assertEquals("1.3", "http://update.eclipse.org/eclipse/testUpdates/", metadataRepositories[0].toString());
		assertEquals("1.4", "tooling", description.getProfileProperties().get("eclipse.p2.flavor"));
		assertEquals("1.5", "Eclipse SDK", description.getProductName());
		assertEquals("1.5", "eclipse", description.getLauncherName());
		VersionedName[] roots = description.getRoots();
		assertEquals("1.7", 1, roots.length);
		assertEquals("1.8", "sdk", roots[0].getId());
		assertTrue("1.9", description.isAutoStart());

	}
}
