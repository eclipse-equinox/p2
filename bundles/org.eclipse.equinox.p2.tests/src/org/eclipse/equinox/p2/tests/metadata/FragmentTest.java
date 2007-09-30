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
import junit.framework.TestCase;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.resolution.ResolutionHelper;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public class FragmentTest extends TestCase {
	public void testAssociation() {
		IInstallableUnit iu1 = createIU("ui.test1");
		IInstallableUnit iu2 = createIUFragment("iuFragment.test1");
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
		IInstallableUnit iu1 = createIU("ui.test1");
		IInstallableUnit iu3 = createIU("ui.test3");
		IInstallableUnit iu2 = createIUFragment("iuFragment.test1");
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
				assertEquals(iu.getTouchpointData().length, 2);

		}
	}

	public void testFragmentCapability() {
		IInstallableUnit iu = createIUFragment("iuFragment.test1");
		ProvidedCapability[] all = iu.getProvidedCapabilities();
		assertContains(all, InstallableUnitFragment.FRAGMENT_CAPABILITY);
	}

	public void testDefaultIUCapability() {
		IInstallableUnit iu = createIU("ui.test1");
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

	public static InstallableUnit createIUFragment(String name) {
		InstallableUnitFragment iu = new InstallableUnitFragment();
		iu.setId(name);
		iu.setVersion(new Version(1, 0, 0));
		iu.setTouchpointType(new TouchpointType("eclipse", new Version(1, 0, 0)));
		iu.setRequiredCapabilities(new RequiredCapability[] {new RequiredCapability("eclipse.touchpoint", "bundle", VersionRange.emptyRange, null, false, true)});
		return iu;
	}

	public static InstallableUnit createIU(String name) {
		InstallableUnit iu = new InstallableUnit();
		iu.setId(name);
		iu.setVersion(new Version(1, 0, 0));
		iu.setTouchpointType(new TouchpointType("eclipse", new Version(1, 0, 0)));

		ProvidedCapability[] cap = new ProvidedCapability[] {new ProvidedCapability("eclipse.touchpoint", "bundle", new Version(1, 0, 0))};
		iu.setCapabilities(cap);
		return iu;
	}

	private IInstallableUnit createIUWithTouchpointData() {
		InstallableUnit unit = createIU("ui.test1");
		unit.setImmutableTouchpointData(new TouchpointData(new HashMap()));
		return unit;
	}

	private IInstallableUnit createIUFragmentWithTouchpointData() {
		InstallableUnit unit = createIUFragment("iuFragment.test1");
		unit.setImmutableTouchpointData(new TouchpointData(new HashMap()));
		return unit;
	}
}
