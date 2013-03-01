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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug306279c extends AbstractProvisioningTest {

	public void testGreedy() throws OperationCanceledException {
		IInstallableUnit x, y, a1, a2, b;
		x = createIU("X");

		//Y -ng & op-> X 
		IRequirement[] reqY = new IRequirement[1];
		reqY[0] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "X", VersionRange.emptyRange, null, true, false, false);
		y = createIU("Y", Version.create("1.0.0"), reqY);

		//B --> As
		IRequirement[] reqB = new IRequirement[1];
		reqB[0] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "A", VersionRange.emptyRange, null, false, false, true);
		b = createIU("B", Version.create("1.0.0"), reqB);

		//Av1 -> X
		IRequirement[] reqXfromA1 = new IRequirement[1];
		reqXfromA1[0] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "X", VersionRange.emptyRange, null, false, false, true);
		a1 = createIU("A", Version.create("1.0.0"), reqXfromA1);

		//Av2 -ng-> X
		IRequirement[] reqXfromA2 = new IRequirement[1];
		reqXfromA2[0] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "X", VersionRange.emptyRange, null, true, false, false);
		a2 = createIU("A", Version.create("2.0.0"), reqXfromA2);

		IPlanner planner = createPlanner();
		IProfile profile = createProfile(getUniqueString());

		IProfileChangeRequest pcr = planner.createChangeRequest(profile);
		pcr.add(y);
		pcr.add(b);

		//Adding the following negative requirement on X shows that the problem has a solution without X being part of the solution
		//		IRequirement negation = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "X", VersionRange.emptyRange, null, 0, 0, false);
		//		Collection<IRequirement> req = new ArrayList<IRequirement>();
		//		req.add(negation);
		//		pcr.addExtraRequirements(req);

		createTestMetdataRepository(new IInstallableUnit[] {x, y, a1, a2, b});
		IProvisioningPlan result = planner.getProvisioningPlan(pcr, null, null);
		assertEquals(IStatus.OK, result.getStatus().getCode());

		//verify that x is not there.
		assertTrue(result.getAdditions().query(QueryUtil.createIUQuery(x), null).isEmpty());

	}
}
