package org.eclipse.equinox.p2.tests.planner;

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class EPPPackageInstallStability extends AbstractProvisioningTest {

	public void testInstallEppJavaPackage() throws ProvisionException {
		IProvisioningAgentProvider provider = getAgentProvider();
		IProvisioningAgent agent = provider.createAgent(getTempFolder().toURI());
		IMetadataRepositoryManager repoMgr = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		repoMgr.addRepository(URI.create("http://download.eclipse.org/releases/helios"));
		//		repoMgr.addRepository(getTestData("bug306279f data", "testData/bug306279f/repo/babel").toURI());

		IPlanner planner = (IPlanner) agent.getService(IPlanner.SERVICE_NAME);
		Map<String, String> profileArgs = new HashMap<String, String>();
		profileArgs.put("osgi.os", "linux");
		profileArgs.put("osgi.ws", "gtk");
		profileArgs.put("osgi.arch", "x86");

		Set<IInstallableUnit> iusFromFirstResolution = new HashSet<IInstallableUnit>();
		{
			IProfile eppProfile1 = ((IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME)).addProfile("epp.install.1", profileArgs);
			IProfileChangeRequest request = planner.createChangeRequest(eppProfile1);
			request.add(repoMgr.query(QueryUtil.createIUQuery("epp.package.java"), null).iterator().next());

			ProvisioningContext pc = new ProvisioningContext(agent);

			IProvisioningPlan plan = planner.getProvisioningPlan(request, pc, new NullProgressMonitor());
			IInstallableUnit iuf = plan.getAdditions().query(QueryUtil.createIUQuery("org.eclipse.net4j.jms.api"), new NullProgressMonitor()).iterator().next();
			System.out.println(iuf);
			assertOK("plan is not ok", plan.getStatus());

			//Extract all the unresolved IUs.
			Set tmp = plan.getAdditions().query(QueryUtil.ALL_UNITS, new NullProgressMonitor()).query(QueryUtil.ALL_UNITS, null).toSet();
			for (Iterator iterator = tmp.iterator(); iterator.hasNext();) {
				IInstallableUnit iu = (IInstallableUnit) iterator.next();
				iusFromFirstResolution.add(iu.unresolved());
			}
		}

		{
			IProfile eppProfile2 = ((IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME)).addProfile("epp.install.2", profileArgs);
			IProfileChangeRequest request = planner.createChangeRequest(eppProfile2);
			request.add(repoMgr.query(QueryUtil.createIUQuery("epp.package.java"), null).iterator().next());

			ProvisioningContext pc = new ProvisioningContext(agent);
			pc.setMetadataRepositories(new URI[0]);
			pc.setArtifactRepositories(new URI[0]);
			pc.setExtraInstallableUnits(new ArrayList(iusFromFirstResolution));

			IProvisioningPlan plan = planner.getProvisioningPlan(request, pc, new NullProgressMonitor());
			assertOK("plan is not ok", plan.getStatus());

			Set tmp = plan.getAdditions().query(QueryUtil.ALL_UNITS, new NullProgressMonitor()).query(QueryUtil.ALL_UNITS, null).toSet();
			Set<IInstallableUnit> iusFromSecondResolution = new HashSet<IInstallableUnit>();
			for (Iterator iterator = tmp.iterator(); iterator.hasNext();) {
				IInstallableUnit iu = (IInstallableUnit) iterator.next();
				iusFromSecondResolution.add(iu.unresolved());
			}

			iusFromFirstResolution.removeAll(iusFromSecondResolution);
			assertEquals(0, iusFromFirstResolution.size());
		}

	}

	//IF YOU MAKE THE getSolutionFor method public then you can run this tests. It is identical to the previous one but a bit more straightforward since you then avoid the phase where we create the operands
	//	public void test2() throws ProvisionException {
	//
	//		IProvisioningAgentProvider provider = getAgentProvider();
	//		IProvisioningAgent agent = provider.createAgent(getTempFolder().toURI());
	//		IMetadataRepositoryManager repoMgr = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
	//		repoMgr.addRepository(URI.create("http://download.eclipse.org/releases/helios"));
	//		//		repoMgr.addRepository(getTestData("bug306279f data", "testData/bug306279f/repo/babel").toURI());
	//
	//		IPlanner planner = (IPlanner) agent.getService(IPlanner.SERVICE_NAME);
	//		Map<String, String> profileArgs = new HashMap<String, String>();
	//		profileArgs.put("osgi.os", "linux");
	//		profileArgs.put("osgi.ws", "gtk");
	//		profileArgs.put("osgi.arch", "x86");
	//
	//		Collection<IInstallableUnit> iusFromFirstResolution = new HashSet<IInstallableUnit>();
	//		{
	//			IProfile eppProfile1 = ((IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME)).addProfile("epp.install.1", profileArgs);
	//			IProfileChangeRequest request = planner.createChangeRequest(eppProfile1);
	//			request.add(repoMgr.query(QueryUtil.createIUQuery("epp.package.java"), null).iterator().next());
	//
	//			ProvisioningContext pc = new ProvisioningContext(agent);
	//
	//			Projector plan = (Projector) ((SimplePlanner) planner).getSolutionFor((ProfileChangeRequest) request, pc, new NullProgressMonitor());
	//			iusFromFirstResolution = plan.extractSolution();
	//		}
	//
	//		{
	//			IProfile eppProfile2 = ((IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME)).addProfile("epp.install.2", profileArgs);
	//			IProfileChangeRequest request = planner.createChangeRequest(eppProfile2);
	//			request.add(repoMgr.query(QueryUtil.createIUQuery("epp.package.java"), null).iterator().next());
	//
	//			ProvisioningContext pc = new ProvisioningContext(agent);
	//			pc.setMetadataRepositories(new URI[0]);
	//			pc.setArtifactRepositories(new URI[0]);
	//			pc.setExtraInstallableUnits(new ArrayList(iusFromFirstResolution));
	//
	//			Projector plan = (Projector) ((SimplePlanner) planner).getSolutionFor((ProfileChangeRequest) request, pc, new NullProgressMonitor());
	//			Collection<IInstallableUnit> secondPlan = plan.extractSolution();
	//			iusFromFirstResolution.removeAll(secondPlan);
	//			assertEquals(0, iusFromFirstResolution.size());
	//		}
	//
	//	}
}
