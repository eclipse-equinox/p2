/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import java.io.File;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepositoryFactory;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

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
		factory.setAgent(getAgent());
		try {
			assertNull("1.0", factory.create(getTempFolder().toURI(), "", "", null));
		} catch (ProvisionException e) {
			fail("1.99", e);
		}
	}

	public void testLoad() {
		File testData = getTestData("0.1", "testData/sdkpatchingtest");
		//assert that test data is intact (see bug 285158)
		File profileFile = new File(new File(testData, "SDKPatchingTest.profile"), "1228337371455.profile.gz");
		assertTrue("0.15", profileFile.exists());
		File tempFolder = getTempFolder();
		copy("0.2", testData, tempFolder);

		SimpleProfileRegistry registry = new SimpleProfileRegistry(getAgent(), tempFolder, null, false);
		IProfile profile = registry.getProfile("SDKPatchingTest");
		assertNotNull("0.3", profile);

		IQueryResult profileCollector = profile.query(QueryUtil.createIUAnyQuery(), getMonitor());
		assertFalse("0.4", profileCollector.isEmpty());

		File simpleProfileFolder = new File(tempFolder, "SDKPatchingTest.profile");
		assertTrue("0.5", simpleProfileFolder.exists());

		ProfileMetadataRepositoryFactory factory = new ProfileMetadataRepositoryFactory();
		factory.setAgent(getAgent());
		ProfileMetadataRepository repo = null;
		try {
			repo = (ProfileMetadataRepository) factory.load(simpleProfileFolder.toURI(), 0, getMonitor());
		} catch (ProvisionException e1) {
			fail("0.99", e1);
		}

		IQueryResult repoCollector = repo.query(QueryUtil.createIUAnyQuery(), getMonitor());
		assertFalse("1.0", repoCollector.isEmpty());
		assertContains("1.1", repoCollector, profileCollector);
	}

	public void testLoadTimestampedProfile() {
		File testData = getTestData("0.1", "testData/sdkpatchingtest");
		File tempFolder = getTempFolder();
		copy("0.2", testData, tempFolder);

		SimpleProfileRegistry registry = new SimpleProfileRegistry(getAgent(), tempFolder, null, false);
		IProfile profile = registry.getProfile("SDKPatchingTest");
		assertNotNull("0.3", profile);

		IQueryResult profileCollector = profile.query(QueryUtil.createIUAnyQuery(), getMonitor());
		assertFalse("0.4", profileCollector.isEmpty());

		File simpleProfileFolder = new File(tempFolder, "SDKPatchingTest.profile");
		assertTrue("0.5", simpleProfileFolder.exists());

		File timeStampedProfile = new File(simpleProfileFolder, "" + profile.getTimestamp() + ".profile.gz");
		assertTrue("0.6", timeStampedProfile.exists());

		ProfileMetadataRepositoryFactory factory = new ProfileMetadataRepositoryFactory();
		factory.setAgent(getAgent());
		ProfileMetadataRepository repo = null;
		try {
			repo = (ProfileMetadataRepository) factory.load(timeStampedProfile.toURI(), 0, getMonitor());
		} catch (ProvisionException e1) {
			fail("0.99", e1);
		}

		IQueryResult repoCollector = repo.query(QueryUtil.createIUAnyQuery(), getMonitor());
		assertFalse("1.0", repoCollector.isEmpty());
		assertContains("1.1", repoCollector, profileCollector);
	}

	public void DISABLED_testDefaultAgentRepoAndBundlePoolFromProfileRepo() throws InterruptedException {
		File testData = getTestData("0.1", "testData/sdkpatchingtest");
		// /p2/org.eclipse.equinox.p2.engine/profileRegistry");
		File tempFolder = getTempFolder();
		copy("0.2", testData, tempFolder);
		final SimpleArtifactRepositoryFactory simpleFactory = new SimpleArtifactRepositoryFactory();
		simpleFactory.setAgent(getAgent());
		simpleFactory.create(tempFolder.toURI(), "", "", null);

		File defaultAgenRepositoryDirectory = new File(tempFolder, "p2/org.eclipse.equinox.p2.core/cache");
		simpleFactory.create(defaultAgenRepositoryDirectory.toURI(), "", "", null);

		File profileRegistryFolder = new File(tempFolder, "p2/org.eclipse.equinox.p2.engine/profileRegistry");
		SimpleProfileRegistry registry = new SimpleProfileRegistry(getAgent(), profileRegistryFolder, null, false);
		IProfile profile = registry.getProfile("SDKPatchingTest");
		assertNotNull("1.0", profile);

		IQueryResult profileCollector = profile.query(QueryUtil.createIUAnyQuery(), getMonitor());
		assertFalse("1.1", profileCollector.isEmpty());

		File simpleProfileFolder = new File(profileRegistryFolder, "SDKPatchingTest.profile");
		assertTrue("1.2", simpleProfileFolder.exists());

		File timeStampedProfile = new File(simpleProfileFolder, "" + profile.getTimestamp() + ".profile");
		assertTrue("1.3", timeStampedProfile.exists());

		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		assertNotNull("2.0", manager);
		assertFalse("2.1", manager.contains(tempFolder.toURI()));

		ProfileMetadataRepositoryFactory factory = new ProfileMetadataRepositoryFactory();
		factory.setAgent(getAgent());
		ProfileMetadataRepository repo = null;
		try {
			repo = (ProfileMetadataRepository) factory.load(timeStampedProfile.toURI(), 0, getMonitor());
		} catch (ProvisionException e1) {
			fail("2.99", e1);
		}

		IQueryResult repoCollector = repo.query(QueryUtil.createIUAnyQuery(), getMonitor());
		assertFalse("3.0", repoCollector.isEmpty());
		assertContains("3.1", repoCollector, profileCollector);

		int maxTries = 20;
		int current = 0;
		while (true) {
			if (manager.contains(tempFolder.toURI()) && manager.contains(defaultAgenRepositoryDirectory.toURI()))
				break;
			if (++current == maxTries)
				fail("profile artifact repos not added");
			Thread.sleep(100);
		}
	}
}
