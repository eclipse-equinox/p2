/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import java.io.File;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.spi.p2.artifact.repository.SimpleArtifactRepositoryFactory;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;

/**
 * Simple test of the engine API.
 */
public class ProfileMetadataRepositoryTest extends AbstractProvisioningTest {

	public ProfileMetadataRepositoryTest() {
		super("");
	}

	public ProfileMetadataRepositoryTest(String name) {
		super(name);
	}

	public void testCreate() {
		ProfileMetadataRepositoryFactory factory = new ProfileMetadataRepositoryFactory();
		try {
			assertNull(factory.create(getTempFolder().toURI(), "", "", null));
		} catch (ProvisionException e) {
			fail();
		}
	}

	public void testValidate() {
		File testData = getTestData("0.1", "testData/sdkpatchingtest/p2/org.eclipse.equinox.p2.engine/profileRegistry");
		File tempFolder = getTempFolder();
		copy("0.2", testData, tempFolder);

		File simpleProfileFolder = new File(tempFolder, "SDKProfile.profile");
		assertTrue(simpleProfileFolder.exists());

		ProfileMetadataRepositoryFactory factory = new ProfileMetadataRepositoryFactory();
		IStatus status = factory.validate(simpleProfileFolder.toURI(), getMonitor());
		assertTrue(status.isOK());

		status = factory.validate(tempFolder.toURI(), getMonitor());
		assertFalse(status.isOK());
	}

	public void testLoad() {
		File testData = getTestData("0.1", "testData/sdkpatchingtest/p2/org.eclipse.equinox.p2.engine/profileRegistry");
		File tempFolder = getTempFolder();
		copy("0.2", testData, tempFolder);

		SimpleProfileRegistry registry = new SimpleProfileRegistry(tempFolder, null, false);
		IProfile profile = registry.getProfile("SDKProfile");
		assertNotNull(profile);

		Collector profileCollector = profile.query(InstallableUnitQuery.ANY, new Collector(), getMonitor());
		assertFalse(profileCollector.isEmpty());

		File simpleProfileFolder = new File(tempFolder, "SDKProfile.profile");
		assertTrue(simpleProfileFolder.exists());

		ProfileMetadataRepositoryFactory factory = new ProfileMetadataRepositoryFactory();
		ProfileMetadataRepository repo = null;
		try {
			repo = (ProfileMetadataRepository) factory.load(simpleProfileFolder.toURI(), 0, getMonitor());
		} catch (ProvisionException e1) {
			fail();
		}

		Collector repoCollector = repo.query(InstallableUnitQuery.ANY, new Collector(), getMonitor());
		assertFalse(repoCollector.isEmpty());
		assertTrue(repoCollector.toCollection().containsAll(profileCollector.toCollection()));
	}

	public void testLoadTimestampedProfile() {
		File testData = getTestData("0.1", "testData/sdkpatchingtest/p2/org.eclipse.equinox.p2.engine/profileRegistry");
		File tempFolder = getTempFolder();
		copy("0.2", testData, tempFolder);

		SimpleProfileRegistry registry = new SimpleProfileRegistry(tempFolder, null, false);
		IProfile profile = registry.getProfile("SDKProfile");
		assertNotNull(profile);

		Collector profileCollector = profile.query(InstallableUnitQuery.ANY, new Collector(), getMonitor());
		assertFalse(profileCollector.isEmpty());

		File simpleProfileFolder = new File(tempFolder, "SDKProfile.profile");
		assertTrue(simpleProfileFolder.exists());

		File timeStampedProfile = new File(simpleProfileFolder, "" + profile.getTimestamp() + ".profile");
		assertTrue(timeStampedProfile.exists());

		ProfileMetadataRepositoryFactory factory = new ProfileMetadataRepositoryFactory();
		ProfileMetadataRepository repo = null;
		try {
			repo = (ProfileMetadataRepository) factory.load(timeStampedProfile.toURI(), 0, getMonitor());
		} catch (ProvisionException e1) {
			fail();
		}

		Collector repoCollector = repo.query(InstallableUnitQuery.ANY, new Collector(), getMonitor());
		assertFalse(repoCollector.isEmpty());
		assertTrue(repoCollector.toCollection().containsAll(profileCollector.toCollection()));
	}

	public void testDefaultBundlePoolFromProfileRepo() {
		File testData = getTestData("0.1", "testData/sdkpatchingtest");
		// /p2/org.eclipse.equinox.p2.engine/profileRegistry");
		File tempFolder = getTempFolder();
		copy("0.2", testData, tempFolder);

		new SimpleArtifactRepositoryFactory().create(tempFolder.toURI(), "", "", null);

		File profileRegistryFolder = new File(tempFolder, "p2/org.eclipse.equinox.p2.engine/profileRegistry");
		SimpleProfileRegistry registry = new SimpleProfileRegistry(profileRegistryFolder, null, false);
		IProfile profile = registry.getProfile("SDKProfile");
		assertNotNull(profile);

		Collector profileCollector = profile.query(InstallableUnitQuery.ANY, new Collector(), getMonitor());
		assertFalse(profileCollector.isEmpty());

		File simpleProfileFolder = new File(profileRegistryFolder, "SDKProfile.profile");
		assertTrue(simpleProfileFolder.exists());

		File timeStampedProfile = new File(simpleProfileFolder, "" + profile.getTimestamp() + ".profile");
		assertTrue(timeStampedProfile.exists());

		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(TestActivator.context, IArtifactRepositoryManager.class.getName());
		assertNotNull(manager);
		assertFalse(manager.contains(tempFolder.toURI()));

		ProfileMetadataRepositoryFactory factory = new ProfileMetadataRepositoryFactory();
		ProfileMetadataRepository repo = null;
		try {
			repo = (ProfileMetadataRepository) factory.load(timeStampedProfile.toURI(), 0, getMonitor());
		} catch (ProvisionException e1) {
			fail();
		}

		Collector repoCollector = repo.query(InstallableUnitQuery.ANY, new Collector(), getMonitor());
		assertFalse(repoCollector.isEmpty());
		assertTrue(repoCollector.toCollection().containsAll(profileCollector.toCollection()));

		assertTrue(manager.contains(tempFolder.toURI()));
	}
}
