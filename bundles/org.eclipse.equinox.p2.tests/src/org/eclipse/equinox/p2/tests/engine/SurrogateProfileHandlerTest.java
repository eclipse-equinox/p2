/*******************************************************************************
 *  Copyright (c) 2005, 2010 IBM Corporation and others.
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
}
