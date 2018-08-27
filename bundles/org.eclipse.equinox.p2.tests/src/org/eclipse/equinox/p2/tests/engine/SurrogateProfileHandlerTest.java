/*******************************************************************************
 *  Copyright (c) 2005, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import java.io.File;
import java.lang.reflect.Field;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.internal.p2.engine.Profile;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.internal.p2.engine.SurrogateProfileHandler;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.reconciler.dropins.SharedInstallTests;
import org.eclipse.equinox.p2.tests.sharedinstall.AbstractSharedInstallTest;

public class SurrogateProfileHandlerTest extends AbstractProvisioningTest {
	private static final String PROFILE_NAME = "profile.SurrogateProfileHandlerTest";

	public static Test suite() {
		return new TestSuite(SurrogateProfileHandlerTest.class);
	}

	private IProfileRegistry registry;
	private SurrogateProfileHandler handler;

	private static void saveProfile(IProfileRegistry iRegistry, Profile profile) {
		SimpleProfileRegistry registry = (SimpleProfileRegistry) iRegistry;
		profile.setChanged(false);
		registry.lockProfile(profile);
		try {
			profile.setChanged(true);
			registry.updateProfile(profile);
		} finally {
			registry.unlockProfile(profile);
			profile.setChanged(false);
		}
	}

	protected void getServices() {
		registry = getProfileRegistry();
	}

	private void ungetServices() {
		registry = null;
	}

	@Override
	protected void setUp() throws Exception {
		getServices();
		//ensure we start in a clean state
		registry.removeProfile(PROFILE_NAME);
		handler = new SurrogateProfileHandler(getAgent());
		Field registryField = SurrogateProfileHandler.class.getDeclaredField("profileRegistry");
		registryField.setAccessible(true);
		registryField.set(handler, registry);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		ungetServices();
	}

	public void testIsSurrogate() throws ProvisionException {
		Profile profile = (Profile) registry.addProfile(PROFILE_NAME);
		assertFalse(handler.isSurrogate(profile));
		IProfile surrogateProfile = handler.createProfile(PROFILE_NAME);
		assertTrue(handler.isSurrogate(surrogateProfile));
	}

	public void testCreateProfile() throws ProvisionException {
		assertNull(handler.createProfile(PROFILE_NAME));
		Profile profile = (Profile) registry.addProfile(PROFILE_NAME);
		profile.addInstallableUnit(createIU("test"));
		saveProfile(registry, profile);
		IProfile surrogateProfile = handler.createProfile(PROFILE_NAME);
		assertTrue(handler.isSurrogate(surrogateProfile));
		assertEquals(1, queryResultSize(surrogateProfile.query(QueryUtil.createIUAnyQuery(), null)));
		assertEquals(2, queryResultSize(surrogateProfile.available(QueryUtil.createIUAnyQuery(), null)));
	}

	public void testDropletsCanDetectFeatureGroup() throws ProvisionException {
		// Droplet containing 'org.foo.bar', 'org.foo.bar.feature.feature.jar' and 'org.foo.bar.feature.feature.group'
		File fragTestData = getTestData("0.1", "/testData/testRepos/foo-droplet");
		File fragDir = getTempFolder();
		copy("Copying ..", fragTestData, fragDir);
		SharedInstallTests.setReadOnly(fragDir, true);
		AbstractSharedInstallTest.reallyReadOnly(fragDir, true);
		EngineActivator.EXTENDED = true;
		EngineActivator.EXTENSIONS = fragDir.getAbsolutePath();

		Profile profile = (Profile) registry.addProfile(PROFILE_NAME);
		saveProfile(registry, profile);
		IProfile surrogateProfile = handler.createProfile(PROFILE_NAME);
		IQueryResult<IInstallableUnit> qRes = surrogateProfile.available(QueryUtil.createIUPropertyQuery(QueryUtil.PROP_TYPE_GROUP, "true"), null);
		assertFalse(qRes.isEmpty());

		AbstractSharedInstallTest.removeReallyReadOnly(fragDir, true);
		SharedInstallTests.setReadOnly(fragDir, false);
	}
}
