/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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
import org.eclipse.equinox.internal.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.internal.p2.engine.Operand;
import org.eclipse.equinox.internal.p2.engine.ProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.junit.Assert;

/**
 * @since 1.0
 */
public class Bug302580 extends AbstractPlannerTest {

	@Override
	protected String getTestDataPath() {
		return "testData/bug302580";
	}

	@Override
	protected String getProfileId() {
		return "bootProfile";
	}

	public void testInstall() {
		IQueryResult<IInstallableUnit> ius = repo.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor());
		IPlanner planner = createPlanner();
		ProvisioningPlan plan = (ProvisioningPlan) planner.getProvisioningPlan(createProfileChangeRequest(ius.toSet(), null, null), null, new NullProgressMonitor());

		Operand ops[] = plan.getOperands();

		String message = "The plan:\n";
		for (Operand op : ops) {
			if (op instanceof InstallableUnitOperand iuo) {
				if (iuo.first() == null) {
					message += iuo.second() + " will be installed\n";
				}
				if (iuo.second() == null) {
					message += iuo.first() + " will be uninstalled\n";
				}
				if (iuo.first() != null && iuo.second() != null) {
					message += iuo.first() + " will be replaced with " + iuo.second() + "\n";
				}
			}
		}
		System.out.println(message);

		for (Operand op : ops) {
			if (op instanceof InstallableUnitOperand iuo) {
				if (iuo.second() == null) {
					String id = iuo.first().getId();
					if (id.equals("toolingorg.eclipse.equinox.launcher") || id.equals("toolingorg.eclipse.equinox.p2.reconciler.dropins") || id.equals("toolingorg.eclipse.equinox.simpleconfigurator")) {
						Assert.fail("Core plug-in to be unistalled");
					}
				}
			}
		}

	}

}
