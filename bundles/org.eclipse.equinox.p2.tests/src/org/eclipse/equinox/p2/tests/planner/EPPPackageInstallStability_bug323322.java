/*******************************************************************************
 * Copyright (c) 2010, 2017 Sonatype Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sonatype - initial API and implementation
 *     IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
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
		IMetadataRepositoryManager repoMgr = agent.getService(IMetadataRepositoryManager.class);
		repoMgr.addRepository(getTestData("Helios SR0", "testData/helios-sr0/").toURI());

		IPlanner planner = agent.getService(IPlanner.class);
		Map<String, String> profileArgs = new HashMap<>();
		profileArgs.put("osgi.os", "linux");
		profileArgs.put("osgi.ws", "gtk");
		profileArgs.put("osgi.arch", "x86");

		Set<IInstallableUnit> iusFromFirstResolution = new HashSet<>();
		{
			IProfile eppProfile1 = agent.getService(IProfileRegistry.class).addProfile("epp.install.1", profileArgs);
			IProfileChangeRequest request = planner.createChangeRequest(eppProfile1);
			request.add(repoMgr.query(QueryUtil.createIUQuery("epp.package.java"), null).iterator().next());

			ProvisioningContext pc = new ProvisioningContext(agent);

			IProvisioningPlan plan = planner.getProvisioningPlan(request, pc, new NullProgressMonitor());
			assertOK("plan is not ok", plan.getStatus());

			//Extract all the unresolved IUs.
			Set<IInstallableUnit> tmp = plan.getAdditions().query(QueryUtil.ALL_UNITS, new NullProgressMonitor()).query(QueryUtil.ALL_UNITS, null).toSet();
			for (Iterator<IInstallableUnit> iterator = tmp.iterator(); iterator.hasNext();) {
				IInstallableUnit iu = iterator.next();
				iusFromFirstResolution.add(iu.unresolved());
			}
		}

		{
			IProfile eppProfile2 = agent.getService(IProfileRegistry.class).addProfile("epp.install.2", profileArgs);
			IProfileChangeRequest request = planner.createChangeRequest(eppProfile2);
			request.add(repoMgr.query(QueryUtil.createIUQuery("epp.package.java"), null).iterator().next());

			ProvisioningContext pc = new ProvisioningContext(agent);
			pc.setMetadataRepositories(new URI[0]);
			pc.setArtifactRepositories(new URI[0]);
			pc.setExtraInstallableUnits(new ArrayList<>(iusFromFirstResolution));

			IProvisioningPlan plan = planner.getProvisioningPlan(request, pc, new NullProgressMonitor());
			assertOK("plan is not ok", plan.getStatus());

			Set<IInstallableUnit> tmp = plan.getAdditions().query(QueryUtil.ALL_UNITS, new NullProgressMonitor()).query(QueryUtil.ALL_UNITS, null).toSet();
			Set<IInstallableUnit> iusFromSecondResolution = new HashSet<>();
			for (Iterator<IInstallableUnit> iterator = tmp.iterator(); iterator.hasNext();) {
				IInstallableUnit iu = iterator.next();
				iusFromSecondResolution.add(iu.unresolved());
			}

			assertEquals(iusFromFirstResolution.size(), iusFromSecondResolution.size());
		}

	}
}
