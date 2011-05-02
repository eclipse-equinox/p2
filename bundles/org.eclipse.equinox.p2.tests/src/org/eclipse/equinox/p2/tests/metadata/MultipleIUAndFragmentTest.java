/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata;

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;

import java.util.Collection;
import java.util.Iterator;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class MultipleIUAndFragmentTest extends AbstractProvisioningTest {

	private static final String ID1 = "iu.1";
	private static final String ID2 = "iu.2";
	private static final String IDF1 = "iu.fragment.1";

	IInstallableUnit iu1;
	IInstallableUnit iu2;
	IInstallableUnit iu3;

	Collection result;

	protected void tearDown() throws Exception {
		super.tearDown();
		iu1 = null;
		iu2 = null;
		iu3 = null;
	}

	public void testAttachment() {
		iu1 = createEclipseIU(ID1);
		iu2 = createIUWithDependencyOn(ID2, ID1);
		iu3 = createBundleFragment(IDF1);
		ProfileChangeRequest req = new ProfileChangeRequest(createProfile(getName()));
		req.addInstallableUnits(iu1, iu2, iu3);
		createTestMetdataRepository(new IInstallableUnit[] {iu1, iu2, iu3});
		IQueryable<IInstallableUnit> additions = createPlanner().getProvisioningPlan(req, null, null).getAdditions();
		{
			Iterator<IInstallableUnit> iterator = additions.query(QueryUtil.createIUQuery(ID1), null).iterator();
			assertTrue("Solution contains IU " + ID1, iterator.hasNext());
			IInstallableUnit iu = iterator.next();
			assertEquals("Number of attached fragments to IU " + ID1, 1, iu.getFragments().size());
			assertEquals("Attached fragment to IU " + ID1, IDF1, iu.getFragments().iterator().next().getId());
		}
		{
			Iterator<IInstallableUnit> iterator = additions.query(QueryUtil.createIUQuery(ID2), null).iterator();
			assertTrue("Solution contains IU " + ID2, iterator.hasNext());
			IInstallableUnit iu = iterator.next();
			assertEquals("Number of attached fragments to IU " + ID2, 1, iu.getFragments().size());
			assertEquals("Attached fragment to IU " + ID2, IDF1, iu.getFragments().iterator().next().getId());
		}
		{
			Iterator<IInstallableUnit> iterator = additions.query(QueryUtil.createIUQuery(IDF1), null).iterator();
			assertTrue("Solution contains IU " + IDF1, iterator.hasNext());
			IInstallableUnit iu = iterator.next();
			assertEquals("Number of attached fragments to IU " + IDF1, 0, iu.getFragments().size());
		}
	}

	private static IInstallableUnit createIUWithDependencyOn(String iuName, String dependencyOn) {
		IRequirement[] requires = new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, dependencyOn, VersionRange.emptyRange, null, false, true)};
		return createEclipseIU(iuName, DEFAULT_VERSION, requires, NO_TP_DATA);
	}
}
