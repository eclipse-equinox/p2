/*******************************************************************************
 *  Copyright (c) 2005, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class ProvisioningPlanQueryTest extends AbstractProvisioningTest {

	private static final String TESTPROFILE = "test";

	public void testNull() {
		IQueryResult<IInstallableUnit> c = new ProvisioningPlan(Status.OK_STATUS, getProfile(TESTPROFILE), null, null).getAdditions().query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor());
		assertTrue(c.isEmpty());
	}

	public void testAddition() {
		Operand[] ops = new Operand[] {new InstallableUnitOperand(null, createIU("A"))};
		IQueryResult<IInstallableUnit> c = new ProvisioningPlan(getProfile(TESTPROFILE), ops, null).getAdditions().query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor());
		assertEquals(1, queryResultSize(c));
		assertTrue(new ProvisioningPlan(getProfile(TESTPROFILE), ops, null).getRemovals().query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor()).isEmpty());
	}

	public void testRemoval() {
		Operand[] ops = new Operand[] {new InstallableUnitOperand(createIU("A"), null)};
		IQueryResult<IInstallableUnit> c = new ProvisioningPlan(getProfile(TESTPROFILE), ops, null).getRemovals().query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor());
		assertEquals(1, queryResultSize(c));
		assertTrue(new ProvisioningPlan(getProfile(TESTPROFILE), ops, null).getAdditions().query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor()).isEmpty());
	}

	public void testUpdate() {
		Operand[] ops = new Operand[] {new InstallableUnitOperand(createIU("A"), createIU("B"))};
		IQueryResult<IInstallableUnit> c = new ProvisioningPlan(getProfile(TESTPROFILE), ops, null).getRemovals().query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor());
		assertEquals(1, queryResultSize(c));
		assertEquals(1, queryResultSize(new ProvisioningPlan(getProfile(TESTPROFILE), ops, null).getAdditions().query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())));
	}
}
