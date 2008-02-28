/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
import org.eclipse.equinox.internal.p2.resolution.ResolutionHelper;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class FragmentTest extends AbstractProvisioningTest {

	public void testAssociation() {
		String ID = "ui.test1";
		IInstallableUnit iu1 = createEclipseIU(ID);
		IInstallableUnit iu2 = createBundleFragment("iuFragment.test1");
		ResolutionHelper rh = new ResolutionHelper(new Hashtable(), null);
		HashSet set = new HashSet();
		set.add(iu1);
		set.add(iu2);
		Collection result = rh.attachCUs(set);
		for (Iterator iterator = result.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			if (iu.getId().equals(ID)) {
				assertEquals(iu.getFragments().length, 1);
				assertEquals(iu.getFragments()[0].getId(), "iuFragment.test1");
			}
		}
	}

	public void testAssociation2() {
		String ID1 = "ui.test1";
		String ID3 = "ui.test3";
		IInstallableUnit iu1 = createEclipseIU(ID1);
		IInstallableUnit iu3 = createEclipseIU(ID3);
		IInstallableUnit iu2 = createBundleFragment("iuFragment.test1");
		ResolutionHelper rh = new ResolutionHelper(new Hashtable(), null);
		HashSet set = new HashSet();
		set.add(iu1);
		set.add(iu2);
		set.add(iu3);
		Collection result = rh.attachCUs(set);
		for (Iterator iterator = result.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			if (iu.getId().equals(ID1)) {
				assertEquals(iu.getFragments().length, 1);
				assertEquals(iu.getFragments()[0].getId(), "iuFragment.test1");
			}
			if (iu.getId().equals(ID3)) {
				assertEquals(iu.getFragments().length, 1);
				assertEquals(iu.getFragments()[0].getId(), "iuFragment.test1");
			}
		}
	}

	public void testTouchpointData() {
		assertEquals(createIUWithTouchpointData().getTouchpointData().length, 1);
		assertEquals(createBundleFragment("iuFragment.test1").getTouchpointData().length, 1);
		IInstallableUnit iu1 = createIUWithTouchpointData();
		IInstallableUnit iu2 = createBundleFragment("iuFragment.test1");
		ResolutionHelper rh = new ResolutionHelper(new Hashtable(), null);
		HashSet set = new HashSet();
		set.add(iu1);
		set.add(iu2);
		Collection result = rh.attachCUs(set);
		for (Iterator iterator = result.iterator(); iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			if (iu.getId().equals(iu1.getId()))
				assertEquals(2, iu.getTouchpointData().length);

		}
	}

	public void testFragmentCapability() {
		IInstallableUnit iu = createBundleFragment("iuFragment.test1");
		assertEquals(Boolean.TRUE.toString(), iu.getProperty(IInstallableUnit.PROP_TYPE_FRAGMENT));
	}

	public void testDefaultIUCapability() {
		IInstallableUnit iu = createEclipseIU("ui.test1");
		ProvidedCapability[] cap = iu.getProvidedCapabilities();
		for (int i = 0; i < cap.length; i++) {
			if (cap[i].getNamespace().equals(IInstallableUnit.NAMESPACE_IU_ID)) {
				assertEquals(cap[i].getNamespace(), IInstallableUnit.NAMESPACE_IU_ID);
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
		TouchpointData data = MetadataFactory.createTouchpointData(new HashMap());
		return createEclipseIU("ui.test1", DEFAULT_VERSION, NO_REQUIRES, data);
	}

	//	private IInstallableUnit createIUFragmentWithTouchpointData() {
	//		TouchpointData data = MetadataFactory.createTouchpointData(new HashMap());
	//		IInstallableUnitFragment unit = createBundleFragment("iuFragment.test1");
	//		return unit;
	//	}
}
