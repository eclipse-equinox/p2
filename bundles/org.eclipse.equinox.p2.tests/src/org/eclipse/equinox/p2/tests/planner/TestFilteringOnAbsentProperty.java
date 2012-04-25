/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.util.*;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.engine.ProvisioningPlan;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class TestFilteringOnAbsentProperty extends AbstractProvisioningTest {
	private static final String NS = IInstallableUnit.NAMESPACE_IU_ID;
	private static final String N = "theName";

	private IInstallableUnit iuA, iuABundled, iuTop;

	/*
	 * This test mimics the metadata structure used to represent the macos distro in bundled shape.
	 * The idea is that there is one IU that requires a capability. 
	 * This capability is provided by two different IUs (iuA and iuABundled) with each guarded by a different filters guaranteeing that only one of the two will ever be selected.
	 * One of the filter makes use of the (<propertyName>=*) LDAP filter that tests for the presence of a property.
	 */
	protected void setUp() throws Exception {
		//first IU
		{
			MetadataFactory.InstallableUnitDescription desc_iuA = new MetadataFactory.InstallableUnitDescription();
			desc_iuA.setId(N);
			desc_iuA.setVersion(Version.create("1.0.0"));
			Collection capabilities = new ArrayList();
			capabilities.add(MetadataFactory.createProvidedCapability(NS, N, Version.create("1.0.0")));
			desc_iuA.addProvidedCapabilities(capabilities);
			desc_iuA.setFilter("(&(osgi.os=macosx) (!(macosx-bundled=*)) )");
			iuA = MetadataFactory.createInstallableUnit(desc_iuA);
		}

		//second IU, provides the capability NS / N
		{
			MetadataFactory.InstallableUnitDescription desc_iuA_bundled = new MetadataFactory.InstallableUnitDescription();
			desc_iuA_bundled.setId("A-bundled");
			desc_iuA_bundled.setVersion(Version.create("1.0.0"));
			Collection capabilities2 = new ArrayList();
			capabilities2.add(MetadataFactory.createProvidedCapability(NS, "A-bundled", Version.create("1.0.0")));
			capabilities2.add(MetadataFactory.createProvidedCapability(NS, N, Version.create("1.0.0")));
			desc_iuA_bundled.addProvidedCapabilities(capabilities2);
			desc_iuA_bundled.setFilter("(&(osgi.os=macosx) (macosx-bundled=true))");
			iuABundled = MetadataFactory.createInstallableUnit(desc_iuA_bundled);
		}

		//top level iu
		{
			MetadataFactory.InstallableUnitDescription desc_top = new MetadataFactory.InstallableUnitDescription();
			desc_top.setId("Top");
			desc_top.setVersion(Version.create("1.0.0"));
			Collection capabilitiesTop = new ArrayList();
			capabilitiesTop.add(MetadataFactory.createProvidedCapability(NS, "Top", Version.create("1.0.0")));
			desc_top.addProvidedCapabilities(capabilitiesTop);
			desc_top.setRequirements(new IRequirement[] {MetadataFactory.createRequirement(NS, N, new VersionRange("[1.0.0, 2.0.0)"), null, false, false, true)});
			iuTop = MetadataFactory.createInstallableUnit(desc_top);
		}

		createTestMetdataRepository(new IInstallableUnit[] {iuA, iuABundled, iuTop});
	}

	public void testWithBundledProperty() {
		Map properties = new HashMap();
		properties.put("osgi.os", "macosx");
		properties.put("macosx-bundled", "true");
		IProfile profile = createProfile("TestProfile." + getName(), properties);
		IPlanner planner = createPlanner();
		ProfileChangeRequest changeRequest = new ProfileChangeRequest(profile);
		changeRequest.add(iuTop);

		ProvisioningPlan plan = (ProvisioningPlan) planner.getProvisioningPlan(changeRequest, null, null);
		IQueryResult<IInstallableUnit> futureIUs = plan.getFutureState().query(QueryUtil.ALL_UNITS, null);
		assertEquals(2, futureIUs.toUnmodifiableSet().size());
		assertContains(futureIUs, iuTop);
		assertContains(futureIUs, iuABundled);
	}

	public void testWithoutBundledProperty() {
		Map properties = new HashMap();
		properties.put("osgi.os", "macosx");
		IProfile profile = createProfile("TestProfile." + getName(), properties);
		IPlanner planner = createPlanner();
		ProfileChangeRequest changeRequest = new ProfileChangeRequest(profile);
		changeRequest.add(iuTop);

		ProvisioningPlan plan = (ProvisioningPlan) planner.getProvisioningPlan(changeRequest, null, null);
		IQueryResult<IInstallableUnit> futureIUs = plan.getFutureState().query(QueryUtil.ALL_UNITS, null);
		assertEquals(2, futureIUs.toUnmodifiableSet().size());
		assertContains(futureIUs, iuTop);
		assertContains(futureIUs, iuA);
	}
}
