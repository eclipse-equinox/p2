package org.eclipse.equinox.p2.tests.touchpoint.natives;

import java.io.File;
import java.util.*;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.p2.touchpoint.natives.NativeTouchpoint;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class NativeTouchpointTest extends AbstractProvisioningTest {

	public NativeTouchpointTest(String name) {
		super(name);
	}

	public NativeTouchpointTest() {
		super("");
	}

	public void testInitializeCompletePhase() {
		NativeTouchpoint touchpoint = new NativeTouchpoint();

		Map parameters = new HashMap();
		IProfile profile = createProfile("test");

		touchpoint.initializePhase(null, profile, "test", parameters);
		assertNull(parameters.get(EclipseTouchpoint.PARM_INSTALL_FOLDER));
		touchpoint.completePhase(null, profile, "test", parameters);

		parameters.clear();
		Properties profileProperties = new Properties();
		File installFolder = getTempFolder();
		profileProperties.setProperty(IProfile.PROP_INSTALL_FOLDER, installFolder.toString());
		profile = createProfile("test", null, profileProperties);

		touchpoint.initializePhase(null, profile, "test", parameters);
		assertNotNull(parameters.get(EclipseTouchpoint.PARM_INSTALL_FOLDER));
		touchpoint.completePhase(null, profile, "test", parameters);
	}

	public void testQualifyAction() {
		NativeTouchpoint touchpoint = new NativeTouchpoint();
		assertEquals("org.eclipse.equinox.p2.touchpoint.natives.chmod", touchpoint.qualifyAction("chmod"));
	}
}
