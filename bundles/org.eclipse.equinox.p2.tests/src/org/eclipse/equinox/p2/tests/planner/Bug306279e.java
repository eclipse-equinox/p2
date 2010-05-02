package org.eclipse.equinox.p2.tests.planner;

import java.net.URI;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug306279e extends AbstractProvisioningTest {
	public void testValidateProfile() throws ProvisionException {
		IProvisioningAgentProvider provider = getAgentProvider();
		IProvisioningAgent agent = provider.createAgent(getTestData("bug306279e data", "testData/bug306279e/p2").toURI());
		IPlanner planner = (IPlanner) agent.getService(IPlanner.SERVICE_NAME);
		IProfileChangeRequest request = planner.createChangeRequest(((IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME)).getProfile("SDKProfile"));

		// Force negation of rwt.
		//		RequiredCapability req1 = new RequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "org.eclipse.rap.jface", new VersionRange("[1.1.0, 1.4.0)"), null, 0, 0, false, null);
		//		ArrayList<IRequirement> reqs = new ArrayList();
		//		reqs.add(req1);
		//		request.addExtraRequirements(reqs);

		ProvisioningContext pc = new ProvisioningContext(agent);
		pc.setMetadataRepositories(new URI[0]);
		pc.setArtifactRepositories(new URI[0]);

		IProvisioningPlan plan = planner.getProvisioningPlan(request, pc, new NullProgressMonitor());
		assertOK("plan is not ok", plan.getStatus());
	}
}
