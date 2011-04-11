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

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

//This test verify that one patch can replace another one. 
public class Bug300572Small5 extends AbstractProvisioningTest {
	IInstallableUnit a1;
	IInstallableUnitPatch p1, p2;

	IProfile profile1;
	IPlanner planner;
	IEngine engine;

	protected void setUp() throws Exception {
		super.setUp();
		//A depends on B
		//It is important to note that B 1.0.0 is not included in the repo on purpose.
		a1 = createIU("A", Version.create("1.0.0"), new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.0.0, 1.0.0]"), null, false, true)});

		//Patch 1 changes B
		//However IU satisfying the new requirement is not available
		IRequirementChange change = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[2.0.0, 2.0.0]"), null, false, false, true));
		p1 = createIUPatch("P", Version.create("1.0.0"), true, new IRequirementChange[] {change}, new IRequirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false)}}, null);

		//Patch 2 changes B
		//However IU satisfying the new requirement is not available
		IRequirementChange changeP2 = MetadataFactory.createRequirementChange(MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, false), MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[3.0.0,3.0.0]"), null, false, false, true));
		p2 = createIUPatch("P", Version.create("2.0.0"), true, new IRequirementChange[] {changeP2}, new IRequirement[][] {{MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false)}}, null);

		createTestMetdataRepository(new IInstallableUnit[] {a1, p1, p2});

		planner = createPlanner();
		engine = createEngine();
	}

	public void testInstallA() {
		profile1 = createProfile("TestProfile." + getName());
		ProfileChangeRequest req1 = new ProfileChangeRequest(profile1);
		req1.addInstallableUnits(new IInstallableUnit[] {a1});
		IProvisioningPlan plan1 = planner.getProvisioningPlan(req1, null, null);
		assertEquals(IStatus.ERROR, plan1.getStatus().getSeverity());
	}

	public void testInstallAandP1() {
		profile1 = createProfile("TestProfile." + getName());
		ProfileChangeRequest req1 = new ProfileChangeRequest(profile1);
		req1.addInstallableUnits(new IInstallableUnit[] {a1, p1});
		IProvisioningPlan plan1 = planner.getProvisioningPlan(req1, null, null);
		assertEquals(IStatus.ERROR, plan1.getStatus().getSeverity());
	}

	public void testInstallAandP1AndP2() {
		profile1 = createProfile("TestProfile." + getName());
		ProfileChangeRequest req1 = new ProfileChangeRequest(profile1);
		req1.addInstallableUnits(new IInstallableUnit[] {a1, p1, p2});
		IProvisioningPlan plan1 = planner.getProvisioningPlan(req1, null, null);
		assertEquals(IStatus.ERROR, plan1.getStatus().getSeverity());
	}

}
