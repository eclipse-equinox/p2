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

import java.net.URI;
import java.util.ArrayList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug311330 extends AbstractProvisioningTest {
	public void testValidateProfile() throws ProvisionException {
		IProvisioningAgentProvider provider = getAgentProvider();
		IProvisioningAgent agent = provider.createAgent(getTestData("bug311330 data", "testData/bug311330/p2").toURI());
		IPlanner planner = (IPlanner) agent.getService(IPlanner.SERVICE_NAME);
		IProfile sdkProfile = ((IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME)).getProfile("SDKProfile");
		IProfileChangeRequest request = planner.createChangeRequest(sdkProfile);
		assertFalse("rap.jface not found", sdkProfile.available(QueryUtil.createIUQuery("org.eclipse.rap.jface"), null).isEmpty());

		// Force negation of rwt.
		RequiredCapability req1 = new RequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "org.eclipse.rap.jface", new VersionRange("[1.1.0, 1.4.0)"), null, 0, 0, false, null);
		ArrayList<IRequirement> reqs = new ArrayList();
		reqs.add(req1);
		request.addExtraRequirements(reqs);

		ProvisioningContext pc = new ProvisioningContext(agent);
		pc.setMetadataRepositories(new URI[0]);
		pc.setArtifactRepositories(new URI[0]);

		IProvisioningPlan plan = planner.getProvisioningPlan(request, pc, new NullProgressMonitor());
		assertFalse("should remove rap.jface", plan.getRemovals().query(QueryUtil.createIUQuery("org.eclipse.rap.jface"), null).isEmpty());
		assertOK("plan is not ok", plan.getStatus());
	}
}
