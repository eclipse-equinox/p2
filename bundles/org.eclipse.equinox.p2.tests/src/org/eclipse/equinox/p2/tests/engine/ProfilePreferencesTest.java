package org.eclipse.equinox.p2.tests.engine;

import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
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

	public void testInvalidProfile() {

		boolean exceptionThrown = false;
		try {
			prefServ.getRootNode().node("/profile/NonExistantProfile/testing");
		} catch (IllegalArgumentException e) {
			exceptionThrown = true;
		}
		assertTrue("IllegalArgumentException not thrown for non-existant profile", exceptionThrown);
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

		try {
			pref.parent().removeNode();
		} catch (BackingStoreException e) {
			//
		}
		pref = prefServ.getRootNode().node("/profile/_SELF_/testing");
		assertTrue("Value not present after load", value.equals(pref.get(key, null)));
	}
}
