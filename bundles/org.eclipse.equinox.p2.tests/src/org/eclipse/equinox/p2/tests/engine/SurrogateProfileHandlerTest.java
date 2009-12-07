/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import java.lang.reflect.Field;
import java.util.HashSet;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class SurrogateProfileHandlerTest extends AbstractProvisioningTest {
	private static final String PROFILE_NAME = "profile.SurrogateProfileHandlerTest";
	private static final String PROP_TYPE_ROOT = "org.eclipse.equinox.p2.type.root"; //$NON-NLS-1$
	private static final String PROP_SHARED_TIMESTAMP = "org.eclipse.equinox.p2.shared.timestamp"; //$NON-NLS-1$

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

	protected void setUp() throws Exception {
		getServices();
		//ensure we start in a clean state
		registry.removeProfile(PROFILE_NAME);
		handler = new SurrogateProfileHandler();
		Field registryField = SurrogateProfileHandler.class.getDeclaredField("profileRegistry");
		registryField.setAccessible(true);
		registryField.set(handler, registry);
	}

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
		assertEquals(0, surrogateProfile.query(InstallableUnitQuery.ANY, new Collector(), null).size());
		assertEquals(1, surrogateProfile.available(InstallableUnitQuery.ANY, new Collector(), null).size());
	}

	public void testUpdateProfile() throws ProvisionException {
		Profile profile = (Profile) registry.addProfile(PROFILE_NAME);
		profile.addInstallableUnit(createIU("test"));
		profile.setInstallableUnitProperty(createIU("test"), PROP_TYPE_ROOT, Boolean.TRUE.toString());
		saveProfile(registry, profile);
		IProfile surrogateProfile = handler.createProfile(PROFILE_NAME);
		assertEquals(1, surrogateProfile.query(InstallableUnitQuery.ANY, new Collector(), null).size());
		// HashSet used here to eliminate duplicates
		assertEquals(1, new HashSet(surrogateProfile.available(InstallableUnitQuery.ANY, new Collector(), null).toCollection()).size());
		handler.updateProfile(surrogateProfile);
		assertEquals(1, surrogateProfile.query(InstallableUnitQuery.ANY, new Collector(), null).size());
		// HashSet used here to eliminate duplicates
		assertEquals(1, new HashSet(surrogateProfile.available(InstallableUnitQuery.ANY, new Collector(), null).toCollection()).size());

		Profile writeableSurrogateProfile = (Profile) surrogateProfile;

		writeableSurrogateProfile.addInstallableUnit(createIU("surrogate.test"));
		writeableSurrogateProfile.setInstallableUnitProperty(createIU("surrogate.test"), PROP_TYPE_ROOT, Boolean.TRUE.toString());
		assertEquals(2, surrogateProfile.query(InstallableUnitQuery.ANY, new Collector(), null).size());
		// HashSet used here to eliminate duplicates
		assertEquals(2, new HashSet(surrogateProfile.available(InstallableUnitQuery.ANY, new Collector(), null).toCollection()).size());

		profile.addInstallableUnit(createIU("test2"));
		profile.setInstallableUnitProperty(createIU("test2"), PROP_TYPE_ROOT, Boolean.TRUE.toString());
		saveProfile(registry, profile);
		assertEquals(2, surrogateProfile.query(InstallableUnitQuery.ANY, new Collector(), null).size());
		// HashSet used here to eliminate duplicates
		assertEquals(3, new HashSet(surrogateProfile.available(InstallableUnitQuery.ANY, new Collector(), null).toCollection()).size());

		//Strictly speaking this should not be necessary however without resetting the timestamp this test will sometimes fail
		writeableSurrogateProfile.setProperty(PROP_SHARED_TIMESTAMP, null);
		handler.updateProfile(surrogateProfile);
		assertEquals(3, surrogateProfile.query(InstallableUnitQuery.ANY, new Collector(), null).size());
		// HashSet used here to eliminate duplicates
		assertEquals(3, new HashSet(surrogateProfile.available(InstallableUnitQuery.ANY, new Collector(), null).toCollection()).size());
	}
}
