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
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.query.QueryUtil;
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
		handler = new SurrogateProfileHandler(getAgent());
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
		assertEquals(1, queryResultSize(surrogateProfile.query(QueryUtil.createIUAnyQuery(), null)));
		assertEquals(2, queryResultSize(surrogateProfile.available(QueryUtil.createIUAnyQuery(), null)));
	}

	public void testUpdateProfile() throws ProvisionException {
		Profile profile = (Profile) registry.addProfile(PROFILE_NAME);
		profile.addInstallableUnit(createIU("test"));
		profile.setInstallableUnitProperty(createIU("test"), PROP_TYPE_ROOT, Boolean.TRUE.toString());
		saveProfile(registry, profile);
		IProfile surrogateProfile = handler.createProfile(PROFILE_NAME);
		assertEquals(2, queryResultSize(surrogateProfile.query(QueryUtil.createIUAnyQuery(), null)));
		// HashSet used here to eliminate duplicates
		assertEquals(2, queryResultUniqueSize(surrogateProfile.available(QueryUtil.createIUAnyQuery(), null)));
		handler.updateProfile(surrogateProfile);
		assertEquals(2, queryResultSize(surrogateProfile.query(QueryUtil.createIUAnyQuery(), null)));
		// HashSet used here to eliminate duplicates
		assertEquals(2, queryResultUniqueSize(surrogateProfile.available(QueryUtil.createIUAnyQuery(), null)));

		Profile writeableSurrogateProfile = (Profile) surrogateProfile;

		writeableSurrogateProfile.addInstallableUnit(createIU("surrogate.test"));
		writeableSurrogateProfile.setInstallableUnitProperty(createIU("surrogate.test"), PROP_TYPE_ROOT, Boolean.TRUE.toString());
		assertEquals(3, queryResultSize(surrogateProfile.query(QueryUtil.createIUAnyQuery(), null)));
		// HashSet used here to eliminate duplicates
		assertEquals(3, queryResultUniqueSize(surrogateProfile.available(QueryUtil.createIUAnyQuery(), null)));

		profile.addInstallableUnit(createIU("test2"));
		profile.setInstallableUnitProperty(createIU("test2"), PROP_TYPE_ROOT, Boolean.TRUE.toString());
		saveProfile(registry, profile);
		assertEquals(3, queryResultSize(surrogateProfile.query(QueryUtil.createIUAnyQuery(), null)));
		// HashSet used here to eliminate duplicates
		assertEquals(4, queryResultUniqueSize(surrogateProfile.available(QueryUtil.createIUAnyQuery(), null)));

		//Strictly speaking this should not be necessary however without resetting the timestamp this test will sometimes fail
		writeableSurrogateProfile.setProperty(PROP_SHARED_TIMESTAMP, null);
		handler.updateProfile(surrogateProfile);
		assertEquals(4, queryResultSize(surrogateProfile.query(QueryUtil.createIUAnyQuery(), null)));
		// HashSet used here to eliminate duplicates
		assertEquals(4, queryResultUniqueSize(surrogateProfile.available(QueryUtil.createIUAnyQuery(), null)));
	}
}
