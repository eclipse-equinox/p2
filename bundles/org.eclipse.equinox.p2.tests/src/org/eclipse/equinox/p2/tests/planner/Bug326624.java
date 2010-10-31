package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug326624 extends AbstractProvisioningTest {

	IProfile profile = null;
	IInstallableUnit toInstall = null;
	IInstallableUnit expected = null;
	IMetadataRepository repo = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		profile = createProfile(getName());

		IMetadataRepositoryManager repoMgr = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
		repo = repoMgr.loadRepository(getTestData("bug326624", "testData/bug326624").toURI(), new NullProgressMonitor());

		toInstall = repo.query(QueryUtil.createIUQuery("org.maven.ide.eclipse.log"), new NullProgressMonitor()).iterator().next();
		expected = repo.query(QueryUtil.createIUQuery("org.maven.ide.eclipse.pr"), new NullProgressMonitor()).iterator().next();
	}

	public void testOptionalGreedy() throws ProvisionException {
		IProvisioningAgent agent = getAgent();
		IPlanner planner = (IPlanner) agent.getService(IPlanner.SERVICE_NAME);

		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {toInstall});
		IProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		assertInstallOperand(plan, expected);
	}
}
