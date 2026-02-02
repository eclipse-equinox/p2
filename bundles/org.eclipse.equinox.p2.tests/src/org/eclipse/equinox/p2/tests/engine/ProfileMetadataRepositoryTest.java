/*******************************************************************************
 *  Copyright (c) 2007, 2026 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import java.io.File;
import java.io.IOException;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepositoryFactory;
import org.eclipse.equinox.internal.p2.engine.ProfileMetadataRepository;
import org.eclipse.equinox.internal.p2.engine.ProfileMetadataRepositoryFactory;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
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

	public void testLoad() throws IOException, ProvisionException {
		File testData = getTestData("0.1", "testData/sdkpatchingtest");
		//assert that test data is intact (see bug 285158)
		File profileFile = new File(new File(testData, "SDKPatchingTest.profile"), "1228337371455.profile.gz");
		assertTrue(profileFile.exists());
		File tempFolder = getTempFolder();
		copy(testData, tempFolder);

		SimpleProfileRegistry registry = new SimpleProfileRegistry(getAgent(), tempFolder, null, false);
		IProfile profile = registry.getProfile("SDKPatchingTest");
		assertNotNull(profile);

		IQueryResult<IInstallableUnit> profileCollector = profile.query(QueryUtil.createIUAnyQuery(), getMonitor());
		assertFalse(profileCollector.isEmpty());

		File simpleProfileFolder = new File(tempFolder, "SDKPatchingTest.profile");
		assertTrue(simpleProfileFolder.exists());

		ProfileMetadataRepositoryFactory factory = new ProfileMetadataRepositoryFactory();
		factory.setAgent(getAgent());
		ProfileMetadataRepository repo = (ProfileMetadataRepository) factory.load(simpleProfileFolder.toURI(), 0,
				getMonitor());

		IQueryResult<IInstallableUnit> repoCollector = repo.query(QueryUtil.createIUAnyQuery(), getMonitor());
		assertFalse(repoCollector.isEmpty());
		assertContains("1.1", repoCollector, profileCollector);
	}

	public void testLoadTimestampedProfile() throws IOException, ProvisionException {
		File testData = getTestData("0.1", "testData/sdkpatchingtest");
		File tempFolder = getTempFolder();
		copy(testData, tempFolder);

		SimpleProfileRegistry registry = new SimpleProfileRegistry(getAgent(), tempFolder, null, false);
		IProfile profile = registry.getProfile("SDKPatchingTest");
		assertNotNull(profile);

		IQueryResult<IInstallableUnit> profileCollector = profile.query(QueryUtil.createIUAnyQuery(), getMonitor());
		assertFalse(profileCollector.isEmpty());

		File simpleProfileFolder = new File(tempFolder, "SDKPatchingTest.profile");
		assertTrue(simpleProfileFolder.exists());

		File timeStampedProfile = new File(simpleProfileFolder, "" + profile.getTimestamp() + ".profile.gz");
		assertTrue(timeStampedProfile.exists());

		ProfileMetadataRepositoryFactory factory = new ProfileMetadataRepositoryFactory();
		factory.setAgent(getAgent());
		ProfileMetadataRepository repo = (ProfileMetadataRepository) factory.load(timeStampedProfile.toURI(), 0,
				getMonitor());

		IQueryResult<IInstallableUnit> repoCollector = repo.query(QueryUtil.createIUAnyQuery(), getMonitor());
		assertFalse(repoCollector.isEmpty());
		assertContains("1.1", repoCollector, profileCollector);
	}

	public void DISABLED_testDefaultAgentRepoAndBundlePoolFromProfileRepo()
			throws InterruptedException, IOException, ProvisionException {
		File testData = getTestData("0.1", "testData/sdkpatchingtest");
		// /p2/org.eclipse.equinox.p2.engine/profileRegistry");
		File tempFolder = getTempFolder();
		copy(testData, tempFolder);
		final SimpleArtifactRepositoryFactory simpleFactory = new SimpleArtifactRepositoryFactory();
		simpleFactory.setAgent(getAgent());
		simpleFactory.create(tempFolder.toURI(), "", "", null);

		File defaultAgenRepositoryDirectory = new File(tempFolder, "p2/org.eclipse.equinox.p2.core/cache");
		simpleFactory.create(defaultAgenRepositoryDirectory.toURI(), "", "", null);

		File profileRegistryFolder = new File(tempFolder, "p2/org.eclipse.equinox.p2.engine/profileRegistry");
		SimpleProfileRegistry registry = new SimpleProfileRegistry(getAgent(), profileRegistryFolder, null, false);
		IProfile profile = registry.getProfile("SDKPatchingTest");
		assertNotNull(profile);

		IQueryResult<IInstallableUnit> profileCollector = profile.query(QueryUtil.createIUAnyQuery(), getMonitor());
		assertFalse(profileCollector.isEmpty());

		File simpleProfileFolder = new File(profileRegistryFolder, "SDKPatchingTest.profile");
		assertTrue(simpleProfileFolder.exists());

		File timeStampedProfile = new File(simpleProfileFolder, "" + profile.getTimestamp() + ".profile");
		assertTrue(timeStampedProfile.exists());

		IArtifactRepositoryManager manager = getArtifactRepositoryManager();
		assertNotNull(manager);
		assertFalse(manager.contains(tempFolder.toURI()));

		ProfileMetadataRepositoryFactory factory = new ProfileMetadataRepositoryFactory();
		factory.setAgent(getAgent());
		ProfileMetadataRepository repo = (ProfileMetadataRepository) factory.load(timeStampedProfile.toURI(), 0, getMonitor());

		IQueryResult<IInstallableUnit> repoCollector = repo.query(QueryUtil.createIUAnyQuery(), getMonitor());
		assertFalse(repoCollector.isEmpty());
		assertContains("3.1", repoCollector, profileCollector);

		int maxTries = 20;
		int current = 0;
		while (true) {
			if (manager.contains(tempFolder.toURI()) && manager.contains(defaultAgenRepositoryDirectory.toURI())) {
				break;
			}
			if (++current == maxTries) {
				fail("profile artifact repos not added");
			}
			Thread.sleep(100);
		}
	}
}
