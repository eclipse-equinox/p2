package org.eclipse.equinox.p2.tests.planner;

import java.io.File;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.director.IPlanner;
import org.eclipse.equinox.internal.provisional.p2.director.ProvisioningPlan;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug262580 extends AbstractProvisioningTest {
	public void testRevertFeaturePatch() {

		File testData = getTestData("test data bug 262580", "testData/bug262580/dataSet/p2/org.eclipse.equinox.p2.engine/profileRegistry");
		File tempFolder = getTempFolder();
		copy("0.2", testData, tempFolder);

		SimpleProfileRegistry testRregistry = new SimpleProfileRegistry(tempFolder, null, false);
		IProfile currentProfile = testRregistry.getProfile("SDKProfile");
		IProfile revertProfile = testRregistry.getProfile("SDKProfile", 1233157854281L);
		assertNotNull(currentProfile);
		assertNotNull(revertProfile);
		IPlanner planner = createPlanner();

		ProvisioningPlan plan = planner.getDiffPlan(currentProfile, revertProfile, getMonitor());
		assertTrue(plan.getStatus().isOK());
	}
}
