/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata;

import java.util.Collection;
import java.util.Iterator;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class MultipleIUAndFragmentTest extends AbstractProvisioningTest {

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
		iu1 = createEclipseIU("one");
		iu2 = createIUWithDependencyOn("two", "one");
		iu3 = createBundleFragment("fragment");
		ProfileChangeRequest req = new ProfileChangeRequest(createProfile(getName()));
		createTestMetdataRepository(new IInstallableUnit[] {iu1, iu2, iu3});
		Iterator iterator = createPlanner().getProvisioningPlan(req, null, null).getAdditions().query(QueryUtil.createIUAnyQuery(), null).iterator();
		for (; iterator.hasNext();) {
			IInstallableUnit iu = (IInstallableUnit) iterator.next();
			if (iu.getId().equals(iu1.getId())) {
				assertEquals(1, iu.getFragments().size());
				assertEquals(iu.getFragments().iterator().next().getId(), iu3.getId());
			}
			if (iu.getId().equals(iu2.getId())) {
				assertEquals(1, iu.getFragments().size());
				assertEquals(iu.getFragments().iterator().next().getId(), iu3.getId());
			}
			if (iu.getId().equals(iu3.getId())) {
				//fragments don't have fragments
				assertNull(iu.getFragments());
			}
		}

	}

	private static IInstallableUnit createIUWithDependencyOn(String iuName, String dependencyOn) {
		IRequiredCapability[] requires = new IRequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, dependencyOn, VersionRange.emptyRange, null, false, true)};
		return createEclipseIU(iuName, DEFAULT_VERSION, requires, NO_TP_DATA);
	}
}
