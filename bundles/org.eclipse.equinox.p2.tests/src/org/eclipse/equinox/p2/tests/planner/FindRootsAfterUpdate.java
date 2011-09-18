/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Sonatype, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.util.Arrays;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class FindRootsAfterUpdate extends AbstractProvisioningTest {
	IInstallableUnit b1;
	IInstallableUnit b2;
	IInstallableUnit b3;

	IProfile profile;
	IPlanner planner;

	protected void setUp() throws Exception {
		super.setUp();

		b1 = createIU("B", Version.create("1.0.0"), true);

		b2 = createIU("B", Version.create("2.0.0"), true);

		b3 = createIU("B", Version.create("3.0.0"), true);

		createTestMetdataRepository(new IInstallableUnit[] {b1, b2, b3});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();

	}

	public void testInstallA1() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		ProvisioningContext ctx = new ProvisioningContext(getAgent());
		IRequirement[] reqs = createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[2.0.0, 4.0.0]"));
		req.addExtraRequirements(Arrays.<IRequirement> asList(reqs[0]));
		IProvisioningPlan plan = planner.getProvisioningPlan(req, ctx, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		assertInstallOperand(plan, b3);
		assertContains(plan.getFutureState().query(QueryUtil.createMatchQuery(reqs[0].getMatches()), null), b3);
	}
}
