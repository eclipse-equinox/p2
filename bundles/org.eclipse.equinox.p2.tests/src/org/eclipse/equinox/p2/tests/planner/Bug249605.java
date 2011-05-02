/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;

import java.util.HashMap;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.query.UserVisibleRootQuery;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug249605 extends AbstractProvisioningTest {
	IInstallableUnit a1;
	IInstallableUnit b1, b2, b3;
	IInstallableUnitPatch p1, p2, p3;

	IProfile profile1;
	IPlanner planner;
	IEngine engine;

	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", Version.create("1.0.0"), new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.0.0, 1.1.0)"), null, false, true)});
		b1 = createIU("B", Version.createOSGi(1, 1, 0), true);
		b2 = createIU("B", Version.createOSGi(1, 2, 0), true);
		b3 = createIU("B", Version.createOSGi(1, 3, 0), true);

		IRequirementChange change = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.1.0, 1.2.0)"), null, false, false, true));
		p1 = createIUPatch("P", Version.create("1.0.0"), true, new IRequirementChange[] {change}, new IRequirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false)}}, null);

		IRequirementChange change2 = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.2.0, 1.3.0)"), null, false, false, true));
		p2 = createIUPatch("P", Version.create("1.2.0"), null, new IRequirement[0], new IProvidedCapability[0], new HashMap(), null, null, true, MetadataFactory.createUpdateDescriptor("P", new VersionRange("[1.0.0, 1.2.0)"), 0, null), new IRequirementChange[] {change2}, new IRequirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false)}}, null, new IRequirement[0]);

		IRequirementChange change3 = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.3.0, 1.4.0)"), null, false, false, true));
		p3 = createIUPatch("P", Version.create("1.3.0"), null, new IRequirement[0], new IProvidedCapability[0], new HashMap(), null, null, true, MetadataFactory.createUpdateDescriptor("P", new VersionRange("[1.0.0, 1.3.0)"), 0, null), new IRequirementChange[] {change3}, new IRequirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false)}}, null, new IRequirement[0]);

		createTestMetdataRepository(new IInstallableUnit[] {a1, b1, b2, b3, p1, p2, p3});

		profile1 = createProfile("TestProfile." + getName());
		planner = createPlanner();
		engine = createEngine();
	}

	public void testInstall() {
		//		The requirement from A to B is broken because there is no B satisfying. Therefore A can only install if the P is installed as well
		ProfileChangeRequest req1 = new ProfileChangeRequest(profile1);
		req1.addInstallableUnits(new IInstallableUnit[] {a1, p1});
		req1.setInstallableUnitProfileProperty(p1, IProfile.PROP_PROFILE_ROOT_IU, Boolean.toString(true));
		IProvisioningPlan plan1 = planner.getProvisioningPlan(req1, null, null);
		assertEquals(IStatus.OK, plan1.getStatus().getSeverity());
		assertOK("1.1", engine.perform(plan1, new NullProgressMonitor()));
		assertProfileContains("1.2", profile1, new IInstallableUnit[] {a1, p1, b1});

		ProfileChangeRequest req2 = new ProfileChangeRequest(profile1);
		req2.addInstallableUnits(new IInstallableUnit[] {p2});
		req2.removeInstallableUnits(new IInstallableUnit[] {p1});
		IProvisioningPlan plan2 = planner.getProvisioningPlan(req2, null, new NullProgressMonitor());
		assertOK("2.0", plan2.getStatus());
		assertOK("2.1", engine.perform(plan2, new NullProgressMonitor()));
		assertProfileContains("2.2", profile1, new IInstallableUnit[] {a1, p2, b2});
		assertTrue(UserVisibleRootQuery.isUserVisible(p2, profile1));

		ProfileChangeRequest req3 = new ProfileChangeRequest(profile1);
		req3.addInstallableUnits(new IInstallableUnit[] {p3});
		req3.removeInstallableUnits(new IInstallableUnit[] {p2});
		IProvisioningPlan plan3 = planner.getProvisioningPlan(req3, null, new NullProgressMonitor());
		assertOK("3.0", plan3.getStatus());
		assertOK("3.1", engine.perform(plan3, new NullProgressMonitor()));
		assertProfileContains("3.2", profile1, new IInstallableUnit[] {a1, p3, b3});
		assertTrue(UserVisibleRootQuery.isUserVisible(p3, profile1));
	}
}
