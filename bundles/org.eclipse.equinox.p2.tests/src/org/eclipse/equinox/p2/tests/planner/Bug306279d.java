/*******************************************************************************
 *  Copyright (c) 2010 Sonatype, Inc and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Sonatype, Inc. - initial API and implementation
 *     IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.*;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug306279d extends AbstractProvisioningTest {
	IInstallableUnit a, b1, b2, b3, x, y;

	@Override
	protected void setUp() throws Exception {
		b1 = createIU("B", Version.createOSGi(1, 0, 0));
		b2 = createIU("B", Version.createOSGi(2, 0, 0));
		b3 = createIU("B", Version.createOSGi(3, 0, 0));

		//A -ng-> B 
		IRequirement[] reqB = new IRequirement[1];
		reqB[0] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, true, false, false);
		a = createIU("A", Version.create("1.0.0"), reqB);

		//X -> B1, B2
		IRequirement[] reqX = new IRequirement[1];
		reqX[0] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange(Version.createOSGi(1, 0, 0), true, Version.createOSGi(2, 1, 0), false), null, false, false, true);
		x = createIU("X", Version.create("1.0.0"), reqX);

		//Y -> B3
		IRequirement[] reqY = new IRequirement[1];
		reqY[0] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange(Version.createOSGi(3, 0, 0), true, Version.createOSGi(3, 1, 0), false), null, false, false, true);
		y = createIU("Y", Version.create("1.0.0"), reqY);

		createTestMetdataRepository(new IInstallableUnit[] {x, y, a, b1, b2, b3});
	}

	public void testNoBInstalled() throws OperationCanceledException {
		IPlanner planner = createPlanner();
		IProfile profile = createProfile(getUniqueString());

		//Here we create a request that request the installation of A, X and Y, but where we negate X and Y. Therefore only A should be installed
		IProfileChangeRequest pcr = planner.createChangeRequest(profile);
		pcr.add(a);
		pcr.add(x);
		pcr.add(y);
		pcr.setInstallableUnitInclusionRules(x, ProfileInclusionRules.createOptionalInclusionRule(x));
		pcr.setInstallableUnitInclusionRules(y, ProfileInclusionRules.createOptionalInclusionRule(y));

		IRequirement negationX = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "X", VersionRange.emptyRange, null, 0, 0, false);
		IRequirement negationY = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "Y", VersionRange.emptyRange, null, 0, 0, false);
		Collection<IRequirement> req = new ArrayList<IRequirement>();
		req.add(negationX);
		req.add(negationY);
		pcr.addExtraRequirements(req);

		IProvisioningPlan plan = planner.getProvisioningPlan(pcr, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getCode());

		assertEquals(0, plan.getAdditions().query(QueryUtil.createIUQuery("B"), new NullProgressMonitor()).toUnmodifiableSet().size());
	}

	public void testBFromXInstalled() throws OperationCanceledException {
		IPlanner planner = createPlanner();
		IProfile profile = createProfile(getUniqueString());

		//Here we create a request that request the installation of A, X and Y. 
		IProfileChangeRequest pcr = planner.createChangeRequest(profile);
		pcr.add(a);
		pcr.add(x);
		pcr.add(y);
		pcr.setInstallableUnitInclusionRules(x, ProfileInclusionRules.createOptionalInclusionRule(x));
		pcr.setInstallableUnitInclusionRules(y, ProfileInclusionRules.createOptionalInclusionRule(y));

		//Negate Y to prevent its installation
		IRequirement negationY = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "Y", VersionRange.emptyRange, null, 0, 0, false);
		Collection<IRequirement> req = new ArrayList<IRequirement>();
		req.add(negationY);
		pcr.addExtraRequirements(req);

		IProvisioningPlan plan = planner.getProvisioningPlan(pcr, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getCode());

		assertEquals(1, plan.getAdditions().query(QueryUtil.createIUQuery(b2), new NullProgressMonitor()).toUnmodifiableSet().size());
	}

	public void testBFromYInstalled() throws OperationCanceledException {
		IPlanner planner = createPlanner();
		IProfile profile = createProfile(getUniqueString());

		//Here we create a request that request the installation of A, X and Y. 
		IProfileChangeRequest pcr = planner.createChangeRequest(profile);
		pcr.add(a);
		pcr.add(x);
		pcr.add(y);
		pcr.setInstallableUnitInclusionRules(x, ProfileInclusionRules.createOptionalInclusionRule(x));
		pcr.setInstallableUnitInclusionRules(y, ProfileInclusionRules.createOptionalInclusionRule(y));

		//Negate X to prevent its installation
		IRequirement negationX = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "X", VersionRange.emptyRange, null, 0, 0, false);
		Collection<IRequirement> req = new ArrayList<IRequirement>();
		req.add(negationX);
		pcr.addExtraRequirements(req);

		IProvisioningPlan plan = planner.getProvisioningPlan(pcr, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getCode());

		assertEquals(1, plan.getAdditions().query(QueryUtil.createIUQuery(b3), new NullProgressMonitor()).toUnmodifiableSet().size());
	}
}
