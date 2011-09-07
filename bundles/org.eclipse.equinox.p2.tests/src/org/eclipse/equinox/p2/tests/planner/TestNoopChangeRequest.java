package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.director.PlannerStatus;
import org.eclipse.equinox.internal.provisional.p2.director.RequestStatus;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class TestNoopChangeRequest extends AbstractProvisioningTest {

	private IInstallableUnit a;
	private IPlanner planner;
	private IEngine engine;
	private IProfile profile;

	@Override
	protected void setUp() throws Exception {
		// TODO Auto-generated method stub
		super.setUp();

		a = createIU("A");

		createTestMetdataRepository(new IInstallableUnit[] {a});

		planner = createPlanner();
		engine = createEngine();
		profile = createProfile("TestProfile." + getName());
		assertOK(install(profile, new IInstallableUnit[] {a}, true, planner, engine));
	}

	public void testNoopInstall() {
		ProfileChangeRequest pcr = new ProfileChangeRequest(profile);
		pcr.add(a);
		pcr.remove(a);
		IProvisioningPlan plan = planner.getProvisioningPlan(pcr, null, new NullProgressMonitor());
		PlannerStatus plannerStatus = plan.getStatus() instanceof PlannerStatus ? (PlannerStatus) plan.getStatus() : null;
		RequestStatus rs = plannerStatus.getRequestChanges().get(a);
		assertEquals(IStatus.OK, rs.getSeverity());
	}
}
