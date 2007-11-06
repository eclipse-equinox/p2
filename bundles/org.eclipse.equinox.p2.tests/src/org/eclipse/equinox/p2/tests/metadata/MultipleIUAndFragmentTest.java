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
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.resolution.ResolutionHelper;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.osgi.service.resolver.VersionRange;

public class MultipleIUAndFragmentTest extends AbstractProvisioningTest {

	IInstallableUnit iu1;
	IInstallableUnit iu2;
	IInstallableUnit iu3;
	Collection result;

	protected void setUp() throws Exception {
		iu1 = createEclipseIU("one");
		iu2 = createIUWithDependencyOn("two", "one");
		iu3 = createBundleFragment("fragment");
		HashSet set = new HashSet();
		set.add(iu1);
		set.add(iu2);
		set.add(iu3);
		result = new ResolutionHelper(new Hashtable(), null).attachCUs(set);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
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

	private static IInstallableUnit createIUWithDependencyOn(String iuName, String dependencyOn) {
		InstallableUnit iu = createEclipseIU(iuName);
		iu.setRequiredCapabilities(new RequiredCapability[] {new RequiredCapability(IInstallableUnit.IU_NAMESPACE, dependencyOn, VersionRange.emptyRange, null, false, true)});
		return iu;
	}
}
