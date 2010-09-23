/*******************************************************************************
 *  Copyright (c) 2010 Sonatype, Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.expression.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class ORTesting extends AbstractProvisioningTest {

	public void testOr() {
		String orExpression = "providedCapabilities.exists(pc | pc.namespace == 'org.eclipse.equinox.p2.iu' && (pc.name == 'org.eclipse.mylyn34' || pc.name == 'org.eclipse.mylyn35'))";
		IExpression expr = ExpressionUtil.parse(orExpression);
		IMatchExpression matchExpression = ExpressionUtil.getFactory().matchExpression(expr);
		IRequirement orRequirement = MetadataFactory.createRequirement(matchExpression, null, 0, 1, true);

		InstallableUnitDescription iudA = new MetadataFactory.InstallableUnitDescription();
		iudA.setId("A");
		iudA.setVersion(Version.parseVersion("1.0.0"));
		iudA.setCapabilities(new IProvidedCapability[] {MetadataFactory.createProvidedCapability("org.eclipse.equinox.p2.iu", "A", Version.parseVersion("1.0.0"))});
		Collection reqs = new ArrayList();
		reqs.add(orRequirement);
		iudA.addRequirements(reqs);

		IInstallableUnit mylyn34 = createIU("org.eclipse.mylyn34");
		IInstallableUnit mylyn35 = createIU("org.eclipse.mylyn35");
		IInstallableUnit iuA = MetadataFactory.createInstallableUnit(iudA);

		createTestMetdataRepository(new IInstallableUnit[] {mylyn34, mylyn35, iuA});
		IProfile profile = createProfile("TestProfile." + getName());
		IPlanner planner = createPlanner();

		ProfileChangeRequest changeRequest = new ProfileChangeRequest(profile);
		changeRequest.add(iuA);
		IProvisioningPlan plan = planner.getProvisioningPlan(changeRequest, null, null);
		System.out.println(plan);
	}
}
