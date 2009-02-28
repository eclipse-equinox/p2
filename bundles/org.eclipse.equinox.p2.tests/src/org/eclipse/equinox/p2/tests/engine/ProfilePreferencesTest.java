/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.ProfilePreferences;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class ProfilePreferencesTest extends AbstractProvisioningTest {
	private IPreferencesService prefServ;

	protected void setUp() throws Exception {
		super.setUp();

		prefServ = (IPreferencesService) ServiceHelper.getService(TestActivator.context, IPreferencesService.class.getName());
	}

	/**
	 * Tests that a node corresponding to a non-existent profile cannot be persisted.
	 */
	public void testInvalidProfile() {
		try {
			//reading and storing for a non-existent profile shouldn't cause any errors
			Preferences node = prefServ.getRootNode().node("/profile/NonExistantProfile/testing");
			node.sync();
		} catch (BackingStoreException e) {
			fail("1.0", e);
		}
	}

	public void testProfilePreference() {
		Preferences pref = null;
		String key = "Test";
		String value = "Value";

		try {
			pref = prefServ.getRootNode().node("/profile/_SELF_/testing");
		} catch (IllegalArgumentException e) {
			fail("IllegalArgumentException when accessing preferences for self profile");
		}

		pref.put(key, value);
		assertTrue("Unable to retrieve value from preferences", value.equals(pref.get(key, null)));

		try {
			pref.flush();
		} catch (BackingStoreException e) {
			fail("Unable to write to preferences: " + e.getMessage());
		}
		waitForSave();

		try {
			pref.parent().removeNode();
		} catch (BackingStoreException e) {
			//
		}
		waitForSave();
		pref = prefServ.getRootNode().node("/profile/_SELF_/testing");
		assertEquals("Value not present after load", value, pref.get(key, null));
	}

	/**
	 * Wait for preferences to be flushed to disk
	 */
	private void waitForSave() {
		try {
			Job.getJobManager().join(ProfilePreferences.PROFILE_SAVE_JOB_FAMILY, null);
		} catch (InterruptedException e) {
			fail("4.99", e);
		}
	}
}
