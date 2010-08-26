/*******************************************************************************
 * Copyright (c) 2010 Sonatype Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sonatype - initial API and implementation
 *     IBM Corporation - ongoing development
 *******************************************************************************/
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

public class EPPPackageInstallStability_bug323322 extends AbstractProvisioningTest {

	public void testInstallEppJavaPackage() throws ProvisionException {
		IProvisioningAgentProvider provider = getAgentProvider();
		IProvisioningAgent agent = provider.createAgent(getTempFolder().toURI());
		IMetadataRepositoryManager repoMgr = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		repoMgr.addRepository(getTestData("Helios SR0", "testData/helios-sr0/").toURI());

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

			assertEquals(iusFromFirstResolution.size(), iusFromSecondResolution.size());
		}

	}
}
