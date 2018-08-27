/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;

public class Bug365124 extends AbstractPlannerTest {

	// path to our data
	@Override
	protected String getTestDataPath() {
		return "testData/bug365124";
	}

	// profile id
	@Override
	protected String getProfileId() {
		return "PlatformProfile";
	}

	public void testInstall() {
		IPlanner planner = createPlanner();

		IQueryResult<IInstallableUnit> allIUs = repo.query(QueryUtil.createIUQuery("f1.feature.group"), new NullProgressMonitor());
		IProfileChangeRequest actualChangeRequest = createProfileChangeRequest(null, allIUs.toSet(), null);
		IProvisioningPlan plan = planner.getProvisioningPlan(actualChangeRequest, null, new NullProgressMonitor());
		Operand[] operands = ((ProvisioningPlan) plan).getOperands();
		for (Operand operand : operands) {
			if (operand instanceof InstallableUnitOperand)
				fail("1.0: " + operand);
		}
	}

}
