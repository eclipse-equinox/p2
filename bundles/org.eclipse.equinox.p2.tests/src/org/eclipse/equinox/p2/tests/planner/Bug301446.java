/*******************************************************************************
 *  Copyright (c) 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;

public class Bug301446 extends AbstractPlannerTest {

	static final Map EXPECTED_VERSIONS = new HashMap();
	static {
		EXPECTED_VERSIONS.put("com.ibm.commerce.toolkit.internal.common", Version.create("7.0.0.1"));
		EXPECTED_VERSIONS.put("com.ibm.commerce.toolkit.internal.dataaccess", Version.create("7.0.0.1"));
		EXPECTED_VERSIONS.put("com.ibm.commerce.toolkit.internal.feature.enablement", Version.create("7.0.0.5"));
		EXPECTED_VERSIONS.put("com.ibm.commerce.toolkit.internal.openlaszlo", Version.create("7.0.0.6"));
		EXPECTED_VERSIONS.put("com.ibm.commerce.toolkit.internal.openlaszlo.migration", Version.create("7.0.2.0"));
		EXPECTED_VERSIONS.put("com.ibm.commerce.toolkit.internal.openlaszlo.migration.validation", Version.create("7.0.2.0"));
		EXPECTED_VERSIONS.put("com.ibm.commerce.toolkit.internal.plugin", Version.create("7.0.0.7"));
	}

	// path to our data
	protected String getTestDataPath() {
		return "testData/bug301446";
	}

	// profile id
	protected String getProfileId() {
		return "bootProfile";
	}

	public void testInstall() {
		IPlanner planner = createPlanner();

		// create the actual plan - install everything in the repo as optional (mimic the dropins folder)
		IQueryResult allIUs = repo.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor());
		IProfileChangeRequest actualChangeRequest = createProfileChangeRequest(allIUs.toSet(), null, null);
		// TODO: verify that we are going to try and install the highest version of everything
		IProvisioningPlan actualPlan = planner.getProvisioningPlan(actualChangeRequest, null, new NullProgressMonitor());

		// this is the plan that we expect - highest version only
		Operand[] operands = ((ProvisioningPlan) actualPlan).getOperands();
		for (int i = 0; i < operands.length; i++) {
			Operand o = operands[i];
			if (!(o instanceof InstallableUnitOperand))
				continue;
			IInstallableUnit iu = ((InstallableUnitOperand) o).second();
			if (iu == null) {
				// we are un-installing an IU, is it interesting?
				iu = ((InstallableUnitOperand) o).first();
				Version expected = (Version) EXPECTED_VERSIONS.get(iu.getId());
				if (expected == null)
					continue;
				Version actual = iu.getVersion();
				assertFalse("Removing IU: " + iu.getId() + " Version: " + expected, actual.equals(expected));
				continue;
			}
			// we are installing an IU
			Version expected = (Version) EXPECTED_VERSIONS.get(iu.getId());
			if (expected == null)
				continue;
			Version actual = iu.getVersion();
			assertTrue("Adding IU: " + iu.getId() + " Actual: " + actual + " Expected: " + expected, actual.equals(expected));
		}
	}

}
