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

import java.util.*;
import junit.framework.AssertionFailedError;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class FragmentTest extends AbstractProvisioningTest {

	/**
	 * Tests that a fragment gets attached to host.
	 */
	public void disabledtestAssociation2Fragments() {
		String ID1 = "iu.1";
		String IDF1 = "iu.fragment.1";
		String IDF2 = "iu.fragment.2";
		IInstallableUnit iu1 = createEclipseIU(ID1);
		IInstallableUnit iuf1 = createBundleFragment(IDF1);
		IInstallableUnit iuf2 = createBundleFragment(IDF2);
		ProfileChangeRequest req = new ProfileChangeRequest(createProfile(getName()));
		req.addInstallableUnits(iu1, iuf1, iuf2);
		createTestMetdataRepository(new IInstallableUnit[] {iu1, iuf1});
		IQueryable<IInstallableUnit> additions = createPlanner().getProvisioningPlan(req, null, null).getAdditions();
		{
			Iterator<IInstallableUnit> iterator = additions.query(QueryUtil.createIUQuery(ID1), null).iterator();
			assertTrue("Solution contains IU " + ID1, iterator.hasNext());
			IInstallableUnit iu = iterator.next();
			assertEquals("Number of attached fragments to IU " + ID1, 2, iu.getFragments().size());
		}
	}

	/**
	 * Tests that a fragment gets attached to host.
	 */
	public void testAssociation() {
		String ID1 = "iu.1";
		String IDF1 = "iu.fragment.1";
		IInstallableUnit iu1 = createEclipseIU(ID1);
		IInstallableUnit iuf1 = createBundleFragment(IDF1);
		ProfileChangeRequest req = new ProfileChangeRequest(createProfile(getName()));
		req.addInstallableUnits(iu1, iuf1);
		createTestMetdataRepository(new IInstallableUnit[] {iu1, iuf1});
		IQueryable<IInstallableUnit> additions = createPlanner().getProvisioningPlan(req, null, null).getAdditions();
		{
			Iterator<IInstallableUnit> iterator = additions.query(QueryUtil.createIUQuery(ID1), null).iterator();
			assertTrue("Solution contains IU " + ID1, iterator.hasNext());
			IInstallableUnit iu = iterator.next();
			assertEquals("Number of attached fragments to IU " + ID1, 1, iu.getFragments().size());
			assertEquals("Attached fragment to IU " + ID1, IDF1, iu.getFragments().iterator().next().getId());
		}
		{
			Iterator<IInstallableUnit> iterator = additions.query(QueryUtil.createIUQuery(IDF1), null).iterator();
			assertTrue("Solution contains IU " + IDF1, iterator.hasNext());
			IInstallableUnit iu = iterator.next();
			assertEquals("Number of attached fragments to IU " + IDF1, 0, iu.getFragments().size());
		}
		//		ResolutionHelper rh = new ResolutionHelper(new Hashtable(), null);
		//		HashSet set = new HashSet();
		//		set.add(iu1);
		//		set.add(iu2);
		//		Collection result = rh.attachCUs(set);
	}

	/**
	 * Tests that a fragment gets attached to multiple hosts that matches the host requirements.
	 */
	public void testAssociation2() {
		String ID1 = "iu.1";
		String ID2 = "iu.2";
		String IDF1 = "iu.fragment.1";
		IInstallableUnit iu1 = createEclipseIU(ID1);
		IInstallableUnit iu2 = createEclipseIU(ID2);
		IInstallableUnit iuf1 = createBundleFragment(IDF1);
		ProfileChangeRequest req = new ProfileChangeRequest(createProfile(getName()));
		req.addInstallableUnits(iu1, iuf1, iu2);
		createTestMetdataRepository(new IInstallableUnit[] {iu1, iuf1, iu2});
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

	/**
	 * Tests that when a fragment is attached the touchpoint data from host IU gets merged with touchpoint data from fragment.
	 */
	public void testTouchpointData() {
		String IDF1 = "iu.fragment.1";
		IInstallableUnit iu1 = createIUWithTouchpointData();
		IInstallableUnit iuf1 = createBundleFragment(IDF1);
		String ID1 = iu1.getId();

		assertEquals("Number of touchpoint instructions of IU " + ID1, 1, iu1.getTouchpointData().size());
		assertEquals("Number of touchpoint instructions of IU " + IDF1, 1, iuf1.getTouchpointData().size());

		ProfileChangeRequest req = new ProfileChangeRequest(createProfile(getName()));
		req.addInstallableUnits(iu1, iuf1);
		createTestMetdataRepository(new IInstallableUnit[] {iu1, iuf1});
		IQueryable<IInstallableUnit> additions = createPlanner().getProvisioningPlan(req, null, null).getAdditions();
		{
			Iterator<IInstallableUnit> iterator = additions.query(QueryUtil.createIUQuery(ID1), null).iterator();
			assertTrue("Solution contains IU " + ID1, iterator.hasNext());
			IInstallableUnit iu = iterator.next();
			assertEquals("Number of attached fragments to IU " + ID1, 1, iu.getFragments().size());
			assertEquals("Attached fragment to IU " + ID1, IDF1, iu.getFragments().iterator().next().getId());
			assertEquals("Number of touchpoint instructions of IU " + ID1, 2, iu.getTouchpointData().size());
		}
		{
			Iterator<IInstallableUnit> iterator = additions.query(QueryUtil.createIUQuery(IDF1), null).iterator();
			assertTrue("Solution contains IU " + IDF1, iterator.hasNext());
			IInstallableUnit iu = iterator.next();
			assertEquals("Number of attached fragments to IU " + IDF1, 0, iu.getFragments().size());
		}
	}

	public void testFragmentCapability() {
		IInstallableUnit iuf1 = createBundleFragment("iu.fragment.1");
		assertTrue(QueryUtil.isFragment(iuf1));
	}

	public void testDefaultIUCapability() {
		IInstallableUnit iu1 = createEclipseIU("iu.1");
		Collection<IProvidedCapability> capabilities = iu1.getProvidedCapabilities();
		for (IProvidedCapability c : capabilities) {
			if (c.getNamespace().equals(IInstallableUnit.NAMESPACE_IU_ID)) {
				assertEquals(c.getNamespace(), IInstallableUnit.NAMESPACE_IU_ID);
				assertEquals(c.getName(), iu1.getId());
				return;
			}
		}
		throw new AssertionFailedError("No capability for the iu id");
	}

	public static void assertContains(Object[] objects, Object searched) {
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] == searched)
				return;
		}
		throw new AssertionFailedError("The array does not contain the searched element");
	}

	public static void assertContainsWithEquals(Collection<? extends Object> objects, Object searched) {
		if (objects.contains(searched))
			return;

		throw new AssertionFailedError("The array does not contain the searched element");
	}

	private IInstallableUnit createIUWithTouchpointData() {
		ITouchpointData data = MetadataFactory.createTouchpointData(new HashMap());
		return createEclipseIU("iu.1", DEFAULT_VERSION, NO_REQUIRES, data);
	}

	//	private IInstallableUnit createIUFragmentWithTouchpointData() {
	//		TouchpointData data = MetadataFactory.createTouchpointData(new HashMap());
	//		IInstallableUnitFragment unit = createBundleFragment("iuFragment.test1");
	//		return unit;
	//	}
}
