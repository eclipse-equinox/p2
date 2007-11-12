/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata;

import java.util.*;
import junit.framework.AssertionFailedError;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.resolution.ResolutionHelper;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class FragmentTest extends AbstractProvisioningTest {

	public void testAssociation() {
		IInstallableUnit iu1 = createEclipseIU("ui.test1");
		IInstallableUnit iu2 = createBundleFragment("iuFragment.test1");
		ResolutionHelper rh = new ResolutionHelper(new Hashtable(), null);
		HashSet set = new HashSet();
		set.add(iu1);
		set.add(iu2);
		Collection result = rh.attachCUs(set);
		for (Iterator iterator = result.iterator(); iterator.hasNext();) {
			IResolvedInstallableUnit iu = (IResolvedInstallableUnit) iterator.next();
			if (iu.getId().equals("iu1.test1")) {
				assertEquals(iu.getFragments().length, 1);
				assertEquals(iu.getFragments()[0].getId(), "iuFragment.test1");
			}
		}
	}

	public void testAssociation2() {
		IInstallableUnit iu1 = createEclipseIU("ui.test1");
		IInstallableUnit iu3 = createEclipseIU("ui.test3");
		IInstallableUnit iu2 = createBundleFragment("iuFragment.test1");
		ResolutionHelper rh = new ResolutionHelper(new Hashtable(), null);
		HashSet set = new HashSet();
		set.add(iu1);
		set.add(iu2);
		set.add(iu3);
		Collection result = rh.attachCUs(set);
		for (Iterator iterator = result.iterator(); iterator.hasNext();) {
			IResolvedInstallableUnit iu = (IResolvedInstallableUnit) iterator.next();
			if (iu.getId().equals("iu1.test1")) {
				assertEquals(iu.getFragments().length, 1);
				assertEquals(iu.getFragments()[0].getId(), "iuFragment.test1");
			}
			if (iu.getId().equals("iu1.test3")) {
				assertEquals(iu.getFragments().length, 1);
				assertEquals(iu.getFragments()[0].getId(), "iuFragment.test1");
			}
		}
	}

	public void testTouchpointData() {
		assertEquals(createIUWithTouchpointData().getTouchpointData().length, 1);
		assertEquals(createIUFragmentWithTouchpointData().getTouchpointData().length, 1);
		IInstallableUnit iu1 = createIUWithTouchpointData();
		IInstallableUnit iu2 = createIUFragmentWithTouchpointData();
		ResolutionHelper rh = new ResolutionHelper(new Hashtable(), null);
		HashSet set = new HashSet();
		set.add(iu1);
		set.add(iu2);
		Collection result = rh.attachCUs(set);
		for (Iterator iterator = result.iterator(); iterator.hasNext();) {
			IResolvedInstallableUnit iu = (IResolvedInstallableUnit) iterator.next();
			if (iu.getId().equals(iu1.getId()))
				assertEquals(2, iu.getTouchpointData().length);

		}
	}

	public void testFragmentCapability() {
		IInstallableUnit iu = createBundleFragment("iuFragment.test1");
		ProvidedCapability[] all = iu.getProvidedCapabilities();
		assertContains(all, IInstallableUnitFragment.FRAGMENT_CAPABILITY);
	}

	public void testDefaultIUCapability() {
		IInstallableUnit iu = createEclipseIU("ui.test1");
		ProvidedCapability[] cap = iu.getProvidedCapabilities();
		for (int i = 0; i < cap.length; i++) {
			if (cap[i].getNamespace().equals(IInstallableUnit.IU_NAMESPACE)) {
				assertEquals(cap[i].getNamespace(), IInstallableUnit.IU_NAMESPACE);
				assertEquals(cap[i].getName(), iu.getId());
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

	public static void assertContainsWithEquals(Object[] objects, Object searched) {
		for (int i = 0; i < objects.length; i++) {
			if (objects[i].equals(searched))
				return;
		}
		throw new AssertionFailedError("The array does not contain the searched element");
	}

	private IInstallableUnit createIUWithTouchpointData() {
		TouchpointData data = new TouchpointData(new HashMap());
		return createEclipseIU("ui.test1", DEFAULT_VERSION, NO_REQUIRES, data);
	}

	private IInstallableUnit createIUFragmentWithTouchpointData() {
		TouchpointData data = new TouchpointData(new HashMap());
		IInstallableUnitFragment unit = createBundleFragment("iuFragment.test1", DEFAULT_VERSION, data);
		return unit;
	}
}
