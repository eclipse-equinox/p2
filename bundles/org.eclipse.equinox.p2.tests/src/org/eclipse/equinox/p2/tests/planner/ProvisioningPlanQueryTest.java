/*******************************************************************************
 *  Copyright (c) 2005, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class ProvisioningPlanQueryTest extends AbstractProvisioningTest {

	private static final String TESTPROFILE = "test";

	public void testNull() {
		IQueryResult c = new ProvisioningPlan(Status.OK_STATUS, getProfile(TESTPROFILE), null, null).getAdditions().query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor());
		assertTrue(c.isEmpty());
	}

	public void testAddition() {
		Operand[] ops = new Operand[] {new InstallableUnitOperand(null, createIU("A"))};
		IQueryResult c = new ProvisioningPlan(getProfile(TESTPROFILE), ops, null).getAdditions().query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor());
		assertEquals(1, queryResultSize(c));
		assertTrue(new ProvisioningPlan(getProfile(TESTPROFILE), ops, null).getRemovals().query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor()).isEmpty());
	}

	public void testRemoval() {
		Operand[] ops = new Operand[] {new InstallableUnitOperand(createIU("A"), null)};
		IQueryResult c = new ProvisioningPlan(getProfile(TESTPROFILE), ops, null).getRemovals().query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor());
		assertEquals(1, queryResultSize(c));
		assertTrue(new ProvisioningPlan(getProfile(TESTPROFILE), ops, null).getAdditions().query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor()).isEmpty());
	}

	public void testUpdate() {
		Operand[] ops = new Operand[] {new InstallableUnitOperand(createIU("A"), createIU("B"))};
		IQueryResult c = new ProvisioningPlan(getProfile(TESTPROFILE), ops, null).getRemovals().query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor());
		assertEquals(1, queryResultSize(c));
		assertEquals(1, queryResultSize(new ProvisioningPlan(getProfile(TESTPROFILE), ops, null).getAdditions().query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())));
	}
}
