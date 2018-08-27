/*******************************************************************************
 *  Copyright (c) 2011, 2017 SAP AG. and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      SAP AG. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class ExplanationTest extends AbstractProvisioningTest {
	IInstallableUnit root;
	IInstallableUnit b;
	IInstallableUnit c;
	IPlanner planner;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		root = createIU("root", Version.create("1.0.0"), new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.0.0, 1.0.0]"), null, false, false, true), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "missing", new VersionRange("[1.0.0, 1.0.0]"), null, false, false, true), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "C", new VersionRange("[1.0.0, 1.0.0]"), null, false, false, true)}, NO_PROPERTIES, true);

		b = createIU("B", Version.create("1.0.0"), true);

		c = createIU("C", Version.create("1.0.0"), new IRequirement[] {MetadataFactory.createRequirement("java.package", "a.b.c", new VersionRange("[1.0.0, 1.0.0]"), null, false, false, true)}, NO_PROPERTIES, true);

		createTestMetdataRepository(new IInstallableUnit[] {root, b, c});

		planner = createPlanner();
	}

	public void testExplanation() {
		IProfile profile1 = createProfile("TestProfile." + getName());
		ProfileChangeRequest req = new ProfileChangeRequest(profile1);
		req.addInstallableUnits(new IInstallableUnit[] {root});
		ProvisioningContext context = new ProvisioningContext(getAgent());
		//context.setProperty("org.eclipse.equinox.p2.director.explain", "false");
		IProvisioningPlan plan = planner.getProvisioningPlan(req, context, null);
		assertEquals("Explanation contains " + plan.getStatus().getChildren().length + " instead of 4", true, plan.getStatus().getChildren().length == 4);
	}
}
