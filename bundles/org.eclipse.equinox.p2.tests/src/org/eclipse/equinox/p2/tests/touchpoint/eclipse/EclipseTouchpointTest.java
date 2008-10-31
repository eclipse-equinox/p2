package org.eclipse.equinox.p2.tests.touchpoint.eclipse;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.EclipseTouchpoint;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class EclipseTouchpointTest extends AbstractProvisioningTest {

	public EclipseTouchpointTest(String name) {
		super(name);
	}

	public EclipseTouchpointTest() {
		super("");
	}

	public void testInitializeCompletePhase() {
		EclipseTouchpoint touchpoint = new EclipseTouchpoint();

		Map parameters = new HashMap();
		IProfile profile = createProfile("test");

		touchpoint.initializePhase(null, profile, "test", parameters);
		assertNull(parameters.get(EclipseTouchpoint.PARM_INSTALL_FOLDER));
		Object manipulator = parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		assertNotNull(manipulator);
		assertNotNull(parameters.get(EclipseTouchpoint.PARM_SOURCE_BUNDLES));
		assertNotNull(parameters.get(EclipseTouchpoint.PARM_PLATFORM_CONFIGURATION));
		touchpoint.completePhase(null, profile, "test", parameters);

		// checking that the manipulator is carried from phases to phase
		parameters.clear();
		touchpoint.initializePhase(null, profile, "test2", parameters);
		Object testManipulator = parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		assertEquals(manipulator, testManipulator);
		touchpoint.completePhase(null, profile, "test2", parameters);

		// re: "uninstall" this is necessary for now for coverage until we have formal commit and rollback events
		// this test should be revisited then
		parameters.clear();
		touchpoint.initializePhase(null, profile, "uninstall", parameters);
		testManipulator = parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		assertEquals(manipulator, testManipulator);
		// this will save the manipulator and remove it from the set of tracked manipulators
		touchpoint.completePhase(null, profile, "uninstall", parameters);
		touchpoint.initializePhase(null, profile, "test2", parameters);
		testManipulator = parameters.get(EclipseTouchpoint.PARM_MANIPULATOR);
		assertNotSame(manipulator, testManipulator);
	}

	public void testQualifyAction() {
		EclipseTouchpoint touchpoint = new EclipseTouchpoint();
		assertEquals("org.eclipse.equinox.p2.touchpoint.eclipse.installBundle", touchpoint.qualifyAction("installBundle"));
	}

	public void testInitializeCompleteOperand() {
		EclipseTouchpoint touchpoint = new EclipseTouchpoint();
		Map parameters = new HashMap();
		IProfile profile = createProfile("test");
		Operand operand = new InstallableUnitOperand(null, createIU("test"));

		// need a partial iu test here
		touchpoint.initializeOperand(profile, operand, parameters);
		touchpoint.completeOperand(profile, operand, parameters);
	}
}
