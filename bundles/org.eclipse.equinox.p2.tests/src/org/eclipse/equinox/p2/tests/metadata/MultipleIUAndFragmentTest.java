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
import junit.framework.TestCase;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.resolution.ResolutionHelper;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public class MultipleIUAndFragmentTest extends TestCase {
	IInstallableUnit iu1;
	IInstallableUnit iu2;
	IInstallableUnit iu3;
	Collection result;

	protected void setUp() throws Exception {
		iu1 = createIU("one");
		iu2 = createIUWithDependencyOn("two", "one");
		iu3 = createIUFragment("fragment");
		HashSet set = new HashSet();
		set.add(iu1);
		set.add(iu2);
		set.add(iu3);
		result = new ResolutionHelper(new Hashtable(), null).attachCUs(set);
	}

	protected void tearDown() throws Exception {
		iu1 = null;
		iu2 = null;
		iu3 = null;
	}

	public void testAttachement() {
		for (Iterator iterator = result.iterator(); iterator.hasNext();) {
			IResolvedInstallableUnit iu = (IResolvedInstallableUnit) iterator.next();
			if (iu.getId().equals(iu1.getId())) {
				assertEquals(iu.getFragments().length, 1);
				assertEquals(iu.getFragments()[0].getId(), iu3.getId());
			}
			if (iu.getId().equals(iu2.getId())) {
				assertEquals(iu.getFragments().length, 1);
				assertEquals(iu.getFragments()[0].getId(), iu3.getId());
			}
			if (iu.getId().equals(iu3.getId())) {
				assertEquals(iu.getFragments().length, 0);
			}
		}

	}

	private IInstallableUnit createIUFragment(String name) {
		InstallableUnitFragment iu = new InstallableUnitFragment();
		iu.setId(name);
		iu.setVersion(new Version(1, 0, 0));
		iu.setTouchpointType(new TouchpointType("eclipse", new Version(1, 0, 0)));

		RequiredCapability[] reqs = new RequiredCapability[] {new RequiredCapability("eclipse.touchpoint", "bundle", VersionRange.emptyRange, null, false, true)};
		iu.setRequiredCapabilities(reqs);
		return iu;
	}

	private static IInstallableUnit createIU(String name) {
		InstallableUnit iu = new InstallableUnit();
		iu.setId(name);
		iu.setVersion(new Version(1, 0, 0));
		iu.setTouchpointType(new TouchpointType("eclipse", new Version(1, 0, 0)));

		ProvidedCapability[] cap = new ProvidedCapability[] {new ProvidedCapability("eclipse.touchpoint", "bundle", new Version(1, 0, 0))};
		iu.setCapabilities(cap);
		return iu;
	}

	private static IInstallableUnit createIUWithDependencyOn(String name, String dependencyOn) {
		InstallableUnit iu = new InstallableUnit();
		iu.setId(name);
		iu.setVersion(new Version(1, 0, 0));
		iu.setTouchpointType(new TouchpointType("eclipse", new Version(1, 0, 0)));
		iu.setCapabilities(new ProvidedCapability[] {new ProvidedCapability("eclipse.touchpoint", "bundle", new Version(1, 0, 0))});
		iu.setRequiredCapabilities(new RequiredCapability[] {new RequiredCapability(IInstallableUnit.IU_NAMESPACE, dependencyOn, VersionRange.emptyRange, null, false, true)});
		return iu;
	}
}
