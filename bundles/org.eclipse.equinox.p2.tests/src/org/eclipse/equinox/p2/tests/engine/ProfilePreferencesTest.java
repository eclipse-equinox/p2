/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.engine;

import java.io.File;
import java.net.URI;
import java.util.Hashtable;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.equinox.internal.p2.core.ProvisioningAgent;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.ProfilePreferences;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.ProfileScope;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class ProfilePreferencesTest extends AbstractProvisioningTest {
	private IPreferencesService prefServ;

	protected void setUp() throws Exception {
		super.setUp();

		prefServ = ServiceHelper.getService(TestActivator.context, IPreferencesService.class);
	}

	/**
	 * Tests that a node corresponding to a non-existent profile cannot be persisted.
	 */
	public void testInvalidProfile() {
		try {
			//reading and storing for a non-existent profile shouldn't cause any errors
			IAgentLocation agentLocation = (IAgentLocation) getAgent().getService(IAgentLocation.SERVICE_NAME);
			String locationString = EncodingUtils.encodeSlashes(agentLocation.getRootLocation().toString());
			Preferences node = prefServ.getRootNode().node("/profile/" + locationString + "/NonExistantProfile/testing");
			node.sync();
		} catch (BackingStoreException e) {
			fail("1.0", e);
		}
	}

	/**
	 * Profile preferences looks up the agent location using an LDAP filter. Make
	 * sure it can handle an agent location that contains characters that are not valid in an LDAP filter
	 */
	public void testInvalidFilterChars() {
		File folder = getTestData("Prefs", "/testData/ProfilePreferencesTest/with(invalid)chars/");
		URI location = folder.toURI();
		ProvisioningAgent agent = new ProvisioningAgent();
		agent.setLocation(location);
		agent.setBundleContext(TestActivator.getContext());
		IAgentLocation agentLocation = (IAgentLocation) agent.getService(IAgentLocation.SERVICE_NAME);
		Hashtable props = new Hashtable();
		props.put("locationURI", location.toString());
		ServiceRegistration reg = TestActivator.getContext().registerService(IProvisioningAgent.SERVICE_NAME, agent, props);
		try {
			Preferences prefs = new ProfileScope(agentLocation, "TestProfile").getNode("org.eclipse.equinox.p2.ui.sdk");
			assertEquals("1.0", "always", prefs.get("allowNonOKPlan", ""));
		} finally {
			reg.unregister();
		}

	}

	public void testProfilePreference() {
		Preferences pref = null;
		String key = "Test";
		String value = "Value";

		IAgentLocation agentLocation = (IAgentLocation) getAgent().getService(IAgentLocation.SERVICE_NAME);
		String locationString = EncodingUtils.encodeSlashes(agentLocation.getRootLocation().toString());
		try {
			pref = prefServ.getRootNode().node("/profile/" + locationString + "/_SELF_/testing");
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
		pref = prefServ.getRootNode().node("/profile/" + locationString + "/_SELF_/testing");
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
