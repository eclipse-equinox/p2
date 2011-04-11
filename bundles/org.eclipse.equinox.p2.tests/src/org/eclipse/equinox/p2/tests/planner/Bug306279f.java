/*******************************************************************************
 *  Copyright (c) 2010 Sonatype, Inc and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug306279f extends AbstractProvisioningTest {
	public void testInstallBabel() throws ProvisionException {
		IProvisioningAgentProvider provider = getAgentProvider();
		IProvisioningAgent agent = provider.createAgent(getTestData("bug306279f data", "testData/bug306279f/p2").toURI());
		IMetadataRepositoryManager repoMgr = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		repoMgr.addRepository(getTestData("bug306279f data", "testData/bug306279f/repo/helios/").toURI());
		repoMgr.addRepository(getTestData("bug306279f data", "testData/bug306279f/repo/babel").toURI());

		IPlanner planner = (IPlanner) agent.getService(IPlanner.SERVICE_NAME);
		IProfile sdkProfile = ((IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME)).getProfile("SDKProfile");
		IProfileChangeRequest request = planner.createChangeRequest(sdkProfile);
		request.add(repoMgr.query(QueryUtil.createIUQuery("org.eclipse.babel.nls_rt.rap_en_AA.feature.group"), null).iterator().next());

		ProvisioningContext pc = new ProvisioningContext(agent);

		IProvisioningPlan plan = planner.getProvisioningPlan(request, pc, new NullProgressMonitor());
		assertOK("plan is not ok", plan.getStatus());
		assertTrue("should not contain rap.jface", plan.getAdditions().query(QueryUtil.createIUQuery("org.eclipse.rap.jface"), null).isEmpty());
	}
}
