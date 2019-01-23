/*******************************************************************************
 *  Copyright (c) 2010, 2017 Sonatype, Inc and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.net.URI;
import java.util.ArrayList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug311330 extends AbstractProvisioningTest {
	public void testValidateProfile() throws ProvisionException {
		IProvisioningAgentProvider provider = getAgentProvider();
		IProvisioningAgent agent = provider.createAgent(getTestData("bug311330 data", "testData/bug311330/p2").toURI());
		IPlanner planner = agent.getService(IPlanner.class);
		IProfile sdkProfile = agent.getService(IProfileRegistry.class).getProfile("SDKProfile");
		IProfileChangeRequest request = planner.createChangeRequest(sdkProfile);
		assertFalse("rap.jface not found", sdkProfile.available(QueryUtil.createIUQuery("org.eclipse.rap.jface"), null).isEmpty());

		// Force negation of rwt.
		IRequirement req1 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "org.eclipse.rap.jface", new VersionRange("[1.1.0, 1.4.0)"), null, 0, 0, false, null);
		ArrayList<IRequirement> reqs = new ArrayList<>();
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
