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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.director.Slicer;
import org.eclipse.equinox.internal.p2.engine.ProvisioningPlan;
import org.eclipse.equinox.internal.p2.metadata.ProvidedCapability;
import org.eclipse.equinox.internal.p2.metadata.RequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.director.PlannerStatus;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class NegationTesting extends AbstractProvisioningTest {
	private static final String NS = "theNamespace";
	private static final String N = "theName";

	//	public void testNot1() {
	//		RequiredCapability req1 = new RequiredCapability(NS, N, new VersionRange("[1.0.0, 1.2.0)"), null, 0, 0, false);
	//		ProvidedCapability prov = new ProvidedCapability(NS, N, Version.createOSGi(1, 5, 0));
	//		assertTrue(prov.satisfies(req1));
	//	}
	//
	//	public void testNot2() {
	//		RequiredCapability req = new RequiredCapability(NS, N, new VersionRange("[1.0.0, 1.2.0)"), null, 0, 0, false);
	//		ProvidedCapability prov = new ProvidedCapability(NS, N, Version.createOSGi(1, 1, 0));
	//		assertFalse(prov.satisfies(req));
	//	}
	//
	//	public void testNot3() {
	//		RequiredCapability req = new RequiredCapability(NS, N, new VersionRange("[1.0.0, 1.2.0)"), null, 0, 0, false);
	//		//		RequiredCapability req2 = new RequiredCapability(NS, N, new VersionRange("[1.5.0, 2.0.0)"), null, false, false);
	//		ProvidedCapability prov = new ProvidedCapability("foo", "bar", Version.createOSGi(1, 5, 0));
	//		assertTrue(prov.satisfies(req));
	//	}

	//Test the slicer and the resolver. 
	public void testNot4() {
		MetadataFactory.InstallableUnitDescription iud1 = new MetadataFactory.InstallableUnitDescription();
		iud1.setId("TestNegation4");
		iud1.setVersion(Version.create("1.0.0"));
		RequiredCapability req1 = new RequiredCapability(NS, N, new VersionRange("[1.1.0, 1.2.0)"), null, 0, 0, false, null);
		RequiredCapability req2 = new RequiredCapability(NS, N, new VersionRange("[1.0.0, 2.0.0)"), null, false, false);
		Collection requirements = new ArrayList();
		requirements.add(req1);
		requirements.add(req2);
		iud1.addRequirements(requirements);
		Collection capabilities = new ArrayList();
		capabilities.add(new ProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, "TestNegation4", Version.create("1.0.0")));
		iud1.addProvidedCapabilities(capabilities);
		IInstallableUnit iu1 = MetadataFactory.createInstallableUnit(iud1);

		MetadataFactory.InstallableUnitDescription iud2 = new MetadataFactory.InstallableUnitDescription();
		iud2.setId("ProviderOf1");
		iud2.setVersion(Version.create("1.0.0"));
		Collection capabilities2 = new ArrayList();
		capabilities2.add(MetadataFactory.createProvidedCapability(NS, N, Version.create("1.0.0")));
		iud2.addProvidedCapabilities(capabilities2);
		IInstallableUnit iu2 = MetadataFactory.createInstallableUnit(iud2);

		MetadataFactory.InstallableUnitDescription iud3 = new MetadataFactory.InstallableUnitDescription();
		iud3.setId("ProviderOf1_1_1");
		iud3.setVersion(Version.create("1.0.0"));
		Collection capabilities3 = new ArrayList();
		capabilities3.add(MetadataFactory.createProvidedCapability(NS, N, Version.create("1.1.1")));
		capabilities3.add(new ProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, "ProviderOf1_1_1", Version.create("1.0.0")));
		iud3.addProvidedCapabilities(capabilities3);
		IInstallableUnit iu3 = MetadataFactory.createInstallableUnit(iud3);

		IMetadataRepository repo = createTestMetdataRepository(new IInstallableUnit[] {iu1, iu2, iu3});

		// Verify that the slice includes iu3 because the requirement from iu1 is a range including the provided capability of iu3.
		Slicer slicer = new Slicer(repo, Collections.<String, String> emptyMap(), false);
		IQueryable slice = slicer.slice(new IInstallableUnit[] {iu1}, new NullProgressMonitor());
		assertEquals(3, queryResultSize(slice.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())));

		//Verify that the resolution succeeds and does not return iu3 since it is excluded by the requirement of iu1
		IProfile profile = createProfile("TestProfile." + getName());
		IPlanner planner = createPlanner();
		ProfileChangeRequest changeRequest = new ProfileChangeRequest(profile);
		changeRequest.add(iu1);
		ProvisioningPlan plan = (ProvisioningPlan) planner.getProvisioningPlan(changeRequest, null, null);
		assertEquals(0, queryResultSize(((PlannerStatus) plan.getStatus()).getPlannedState().query(QueryUtil.createIUQuery("ProviderOf1_1_1"), null)));
		assertEquals(2, queryResultSize(((PlannerStatus) plan.getStatus()).getPlannedState().query(QueryUtil.createIUAnyQuery(), null)));

		//Verify that the installing iu1 and iu3 will result in a conflict since iu3 is excluded by the requirement of iu1
		ProfileChangeRequest changeRequest2 = new ProfileChangeRequest(profile);
		Collection<IInstallableUnit> toAdd = new ArrayList<IInstallableUnit>();
		toAdd.add(iu1);
		toAdd.add(iu3);
		changeRequest2.addAll(toAdd);
		IProvisioningPlan plan2 = planner.getProvisioningPlan(changeRequest2, null, null);
		assertNotOK("The resolution should be failing because of the negation requirement.", plan2.getStatus());
	}

	public void testNot5() {
		MetadataFactory.InstallableUnitDescription iud1 = new MetadataFactory.InstallableUnitDescription();
		iud1.setId("TestNegation4");
		iud1.setVersion(Version.create("1.0.0"));
		RequiredCapability req1 = new RequiredCapability(NS, N, new VersionRange("[1.1.0, 1.2.0)"), null, 0, 0, false, null);
		RequiredCapability req2 = new RequiredCapability(NS, N, new VersionRange("[1.0.0, 1.1.0)"), null, false, false);
		Collection requirements = new ArrayList();
		requirements.add(req1);
		requirements.add(req2);
		iud1.addRequirements(requirements);
		Collection capabilities = new ArrayList();
		capabilities.add(new ProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, "TestNegation4", Version.create("1.0.0")));
		iud1.addProvidedCapabilities(capabilities);
		IInstallableUnit iu1 = MetadataFactory.createInstallableUnit(iud1);

		MetadataFactory.InstallableUnitDescription iud2 = new MetadataFactory.InstallableUnitDescription();
		iud2.setId("ProviderOf1");
		iud2.setVersion(Version.create("1.0.0"));
		Collection capabilities2 = new ArrayList();
		capabilities2.add(MetadataFactory.createProvidedCapability(NS, N, Version.create("1.0.0")));
		iud2.addProvidedCapabilities(capabilities2);
		IInstallableUnit iu2 = MetadataFactory.createInstallableUnit(iud2);

		MetadataFactory.InstallableUnitDescription iud3 = new MetadataFactory.InstallableUnitDescription();
		iud3.setId("ProviderOf1_1_1");
		iud3.setVersion(Version.create("1.0.0"));
		Collection capabilities3 = new ArrayList();
		capabilities3.add(MetadataFactory.createProvidedCapability(NS, N, Version.create("1.1.1")));
		capabilities3.add(new ProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, "ProviderOf1_1_1", Version.create("1.0.0")));
		iud3.addProvidedCapabilities(capabilities3);
		IInstallableUnit iu3 = MetadataFactory.createInstallableUnit(iud3);

		IMetadataRepository repo = createTestMetdataRepository(new IInstallableUnit[] {iu1, iu2, iu3});

		//Test the slicer. The slice will not contain iu3 because none of the range of iu1 cause it to be brought in.
		Slicer slicer = new Slicer(repo, Collections.<String, String> emptyMap(), false);
		IQueryable slice = slicer.slice(new IInstallableUnit[] {iu1}, new NullProgressMonitor());
		assertEquals(0, queryResultSize(slice.query(QueryUtil.createIUQuery("ProviderOf1_1_1"), new NullProgressMonitor())));
		assertEquals(2, queryResultSize(slice.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())));
	}

	public void testNot6() {
		MetadataFactory.InstallableUnitDescription iud1 = new MetadataFactory.InstallableUnitDescription();
		iud1.setId("TestNegation4");
		iud1.setVersion(Version.create("1.0.0"));
		RequiredCapability req1 = new RequiredCapability(NS, N, new VersionRange("[1.1.0, 1.2.0)"), null, 0, 0, false, null);
		RequiredCapability req2 = new RequiredCapability(NS, N, new VersionRange("[1.0.0, 1.1.0)"), null, false, false);
		Collection requirements = new ArrayList();
		requirements.add(req1);
		requirements.add(req2);
		iud1.addRequirements(requirements);
		Collection capabilities = new ArrayList();
		capabilities.add(new ProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, "TestNegation4", Version.create("1.0.0")));
		iud1.addProvidedCapabilities(capabilities);
		IInstallableUnit iu1 = MetadataFactory.createInstallableUnit(iud1);

		MetadataFactory.InstallableUnitDescription iud2 = new MetadataFactory.InstallableUnitDescription();
		iud2.setId("ProviderOf1");
		iud2.setVersion(Version.create("1.0.0"));
		Collection capabilities2 = new ArrayList();
		capabilities2.add(MetadataFactory.createProvidedCapability(NS, N, Version.create("1.0.0")));
		iud2.addProvidedCapabilities(capabilities2);
		IInstallableUnit iu2 = MetadataFactory.createInstallableUnit(iud2);

		IMetadataRepository repo = createTestMetdataRepository(new IInstallableUnit[] {iu1, iu2});

		//Test the slicer. The slice will not contain iu3 because none of the range of iu1 cause it to be brought in.
		Slicer slicer = new Slicer(repo, Collections.<String, String> emptyMap(), false);
		IQueryable slice = slicer.slice(new IInstallableUnit[] {iu1}, new NullProgressMonitor());
		assertEquals(0, queryResultSize(slice.query(QueryUtil.createIUQuery("ProviderOf1_1_1"), new NullProgressMonitor())));
		assertEquals(2, queryResultSize(slice.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())));

		//Verify that the negation can not fail the resolution when the IUs satisfying the negative requirement are not there 
		IProfile profile = createProfile("TestProfile." + getName());
		IPlanner planner = createPlanner();
		ProfileChangeRequest changeRequest = new ProfileChangeRequest(profile);
		changeRequest.add(iu1);
		ProvisioningPlan plan = (ProvisioningPlan) planner.getProvisioningPlan(changeRequest, null, null);
		assertEquals(0, queryResultSize(((PlannerStatus) plan.getStatus()).getPlannedState().query(QueryUtil.createIUQuery("ProviderOf1_1_1"), null)));
		assertEquals(2, queryResultSize(((PlannerStatus) plan.getStatus()).getPlannedState().query(QueryUtil.createIUAnyQuery(), null)));
	}

	//Test the slicer and the resolver. 
	public void testNot7() {
		MetadataFactory.InstallableUnitDescription iud1 = new MetadataFactory.InstallableUnitDescription();
		iud1.setId("TestNegation4");
		iud1.setVersion(Version.create("1.0.0"));
		RequiredCapability req1 = new RequiredCapability(NS, N, new VersionRange("[1.1.0, 1.2.0)"), null, 0, 0, false, null);
		RequiredCapability req2 = new RequiredCapability(NS, N, new VersionRange("[1.0.0, 2.0.0)"), null, false, false);
		Collection requirements = new ArrayList();
		requirements.add(req1);
		requirements.add(req2);
		iud1.addRequirements(requirements);
		Collection capabilities = new ArrayList();
		capabilities.add(new ProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, "TestNegation4", Version.create("1.0.0")));
		iud1.addProvidedCapabilities(capabilities);
		IInstallableUnit iu1 = MetadataFactory.createInstallableUnit(iud1);

		MetadataFactory.InstallableUnitDescription iud2 = new MetadataFactory.InstallableUnitDescription();
		iud2.setId("ProviderOf1");
		iud2.setVersion(Version.create("1.0.0"));
		Collection capabilities2 = new ArrayList();
		capabilities2.add(MetadataFactory.createProvidedCapability(NS, N, Version.create("1.0.0")));
		iud2.addProvidedCapabilities(capabilities2);
		IInstallableUnit iu2 = MetadataFactory.createInstallableUnit(iud2);

		MetadataFactory.InstallableUnitDescription iud3 = new MetadataFactory.InstallableUnitDescription();
		iud3.setId("ProviderOf1_1_1");
		iud3.setVersion(Version.create("1.0.0"));
		Collection capabilities3 = new ArrayList();
		capabilities3.add(MetadataFactory.createProvidedCapability(NS, N, Version.create("1.1.1")));
		capabilities3.add(new ProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, "ProviderOf1_1_1", Version.create("1.0.0")));
		iud3.addProvidedCapabilities(capabilities3);
		Collection requirements3 = new ArrayList();
		requirements3.add(MetadataFactory.createRequirement("DOES-NOT-EXIST", "NEVER", new VersionRange("[1.0.0, 2.0.0)"), null, false, false));
		iud3.addRequirements(requirements3);
		IInstallableUnit iu3 = MetadataFactory.createInstallableUnit(iud3);

		IMetadataRepository repo = createTestMetdataRepository(new IInstallableUnit[] {iu1, iu2, iu3});

		// Verify that the slice includes iu3 because the requirement from iu1 is a range including the provided capability of iu3.
		Slicer slicer = new Slicer(repo, Collections.<String, String> emptyMap(), false);
		IQueryable slice = slicer.slice(new IInstallableUnit[] {iu1}, new NullProgressMonitor());
		assertEquals(3, queryResultSize(slice.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())));

		//Verify that the resolution succeeds and does not return iu3 since it is excluded by the requirement of iu1
		IProfile profile = createProfile("TestProfile." + getName());
		IPlanner planner = createPlanner();
		ProfileChangeRequest changeRequest = new ProfileChangeRequest(profile);
		changeRequest.add(iu1);
		ProvisioningPlan plan = (ProvisioningPlan) planner.getProvisioningPlan(changeRequest, null, null);
		assertEquals(0, queryResultSize(((PlannerStatus) plan.getStatus()).getPlannedState().query(QueryUtil.createIUQuery("ProviderOf1_1_1"), null)));
		assertEquals(2, queryResultSize(((PlannerStatus) plan.getStatus()).getPlannedState().query(QueryUtil.createIUAnyQuery(), null)));

		//Verify that the installing iu1 and iu3 will result in a conflict since iu3 is excluded by the requirement of iu1
		ProfileChangeRequest changeRequest2 = new ProfileChangeRequest(profile);
		Collection<IInstallableUnit> toAdd = new ArrayList<IInstallableUnit>();
		toAdd.add(iu1);
		toAdd.add(iu3);
		changeRequest2.addAll(toAdd);
		IProvisioningPlan plan2 = planner.getProvisioningPlan(changeRequest2, null, null);
		assertNotOK("The resolution should be failing because of the negation requirement.", plan2.getStatus());
	}

	public void testNot8() {
		MetadataFactory.InstallableUnitDescription iud1 = new MetadataFactory.InstallableUnitDescription();
		iud1.setId("TestNegation4");
		iud1.setVersion(Version.create("1.0.0"));
		RequiredCapability req1 = new RequiredCapability(NS, N, new VersionRange("[1.1.0, 1.2.0)"), null, 0, 0, false, null);
		RequiredCapability req2 = new RequiredCapability(NS, N, new VersionRange("[1.0.0, 2.0.0)"), null, false, false);
		Collection requirements = new ArrayList();
		requirements.add(req1);
		requirements.add(req2);
		iud1.addRequirements(requirements);
		Collection capabilities = new ArrayList();
		capabilities.add(new ProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, "TestNegation4", Version.create("1.0.0")));
		iud1.addProvidedCapabilities(capabilities);
		IInstallableUnit iu1 = MetadataFactory.createInstallableUnit(iud1);

		MetadataFactory.InstallableUnitDescription iud2 = new MetadataFactory.InstallableUnitDescription();
		iud2.setId("ProviderOf1");
		iud2.setVersion(Version.create("1.0.0"));
		Collection capabilities2 = new ArrayList();
		capabilities2.add(MetadataFactory.createProvidedCapability(NS, N, Version.create("1.0.0")));
		iud2.addProvidedCapabilities(capabilities2);
		IInstallableUnit iu2 = MetadataFactory.createInstallableUnit(iud2);

		MetadataFactory.InstallableUnitDescription iud3 = new MetadataFactory.InstallableUnitDescription();
		iud3.setId("ProviderOf1_1_1");
		iud3.setVersion(Version.create("1.0.0"));
		Collection capabilities3 = new ArrayList();
		capabilities3.add(MetadataFactory.createProvidedCapability(NS, N, Version.create("1.1.1")));
		capabilities3.add(new ProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, "ProviderOf1_1_1", Version.create("1.0.0")));
		iud3.addProvidedCapabilities(capabilities3);
		IInstallableUnit iu3 = MetadataFactory.createInstallableUnit(iud3);

		MetadataFactory.InstallableUnitDescription iud4 = new MetadataFactory.InstallableUnitDescription();
		iud4.setId("AnotherRoot");
		iud4.setVersion(Version.create("1.0.0"));
		Collection capabilities4 = new ArrayList();
		capabilities4.add(new ProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, "AnotherRoot", Version.create("1.0.0")));
		iud4.addProvidedCapabilities(capabilities4);
		Collection reqs4 = new ArrayList();
		reqs4.add(new RequiredCapability(NS, N, new VersionRange("[1.1.1, 1.1.1]"), null, false, false));
		iud4.addRequirements(reqs4);
		IInstallableUnit iu4 = MetadataFactory.createInstallableUnit(iud4);

		IMetadataRepository repo = createTestMetdataRepository(new IInstallableUnit[] {iu1, iu2, iu3, iu4});

		// Verify that the slice includes iu3
		Slicer slicer = new Slicer(repo, Collections.<String, String> emptyMap(), false);
		IQueryable slice = slicer.slice(new IInstallableUnit[] {iu1, iu4}, new NullProgressMonitor());
		assertEquals(4, queryResultSize(slice.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())));

		// Verify that the slice includes iu3
		Slicer slicer2 = new Slicer(repo, Collections.<String, String> emptyMap(), false);
		IQueryable slice2 = slicer2.slice(new IInstallableUnit[] {iu4}, new NullProgressMonitor());
		assertEquals(2, queryResultSize(slice2.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())));

		//Verify that the resolution succeeds and does not return iu3 since it is excluded by the requirement of iu1
		IProfile profile = createProfile("TestProfile." + getName());
		IPlanner planner = createPlanner();
		ProfileChangeRequest changeRequest = new ProfileChangeRequest(profile);
		changeRequest.add(iu1);
		ProvisioningPlan plan = (ProvisioningPlan) planner.getProvisioningPlan(changeRequest, null, null);
		assertEquals(0, queryResultSize(((PlannerStatus) plan.getStatus()).getPlannedState().query(QueryUtil.createIUQuery("ProviderOf1_1_1"), null)));
		assertEquals(2, queryResultSize(((PlannerStatus) plan.getStatus()).getPlannedState().query(QueryUtil.createIUAnyQuery(), null)));

		//Verify that the installing iu1 and iu4 will result in a conflict since iu3 is excluded by the requirement of iu1
		ProfileChangeRequest changeRequest2 = new ProfileChangeRequest(profile);
		Collection<IInstallableUnit> toAdd = new ArrayList<IInstallableUnit>();
		toAdd.add(iu1);
		toAdd.add(iu4);
		changeRequest2.addAll(toAdd);
		IProvisioningPlan plan2 = planner.getProvisioningPlan(changeRequest2, null, null);
		assertNotOK("The resolution should be failing because of the negation requirement.", plan2.getStatus());
	}

	public void testNegationThroughExtraRequirements() {
		IInstallableUnit iu = createIU("TESTNEGATION");
		createTestMetdataRepository(new IInstallableUnit[] {iu});
		IProfile profile = createProfile("TestProfile." + getName());
		IPlanner planner = createPlanner();
		IProfileChangeRequest request = planner.createChangeRequest(profile);
		request.add(iu);

		IRequirement req1 = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, iu.getId(), new VersionRange(iu.getVersion(), true, iu.getVersion(), true), null, 0, 0, false, null);
		ArrayList<IRequirement> reqs = new ArrayList();
		reqs.add(req1);
		request.addExtraRequirements(reqs);

		IProvisioningPlan plan = planner.getProvisioningPlan(request, null, null);
		assertNotOK("plan should fail", plan.getStatus());
	}

	//This test demonstrates having capabilities that are unique 
	public void testUniqueCapability() {
		MetadataFactory.InstallableUnitDescription iud1 = new MetadataFactory.InstallableUnitDescription();
		iud1.setId("TestNegation4");
		iud1.setVersion(Version.create("1.0.0"));
		RequiredCapability req1 = new RequiredCapability(NS, N, new VersionRange("[0.0.0, 1.1.1)"), null, 0, 0, false, null);
		RequiredCapability req2 = new RequiredCapability(NS, N, new VersionRange(Version.create("1.1.1"), false, Version.MAX_VERSION, false), null, 0, 0, false, null);
		Collection requirements = new ArrayList();
		requirements.add(req1);
		requirements.add(req2);
		iud1.addRequirements(requirements);
		Collection capabilities = new ArrayList();
		capabilities.add(new ProvidedCapability(IInstallableUnit.NAMESPACE_IU_ID, "TestNegation4", Version.create("1.0.0")));
		capabilities.add(new ProvidedCapability(NS, N, Version.create("1.1.1")));
		iud1.addProvidedCapabilities(capabilities);
		IInstallableUnit iu1 = MetadataFactory.createInstallableUnit(iud1);

		IProfile profile = createProfile("TestProfile." + getName());
		IPlanner planner = createPlanner();
		ProfileChangeRequest changeRequest = new ProfileChangeRequest(profile);
		changeRequest.add(iu1);
		ProvisioningPlan plan = (ProvisioningPlan) planner.getProvisioningPlan(changeRequest, null, null);
		assertOK(plan.getStatus());
		assertFalse(plan.getAdditions().query(QueryUtil.createIUQuery(iu1), new NullProgressMonitor()).isEmpty());

		MetadataFactory.InstallableUnitDescription iud2 = new MetadataFactory.InstallableUnitDescription();
		iud2.setId("ProviderOf1");
		iud2.setVersion(Version.create("1.0.0"));
		Collection capabilities2 = new ArrayList();
		capabilities2.add(MetadataFactory.createProvidedCapability(NS, N, Version.create("1.0.0")));
		iud2.addProvidedCapabilities(capabilities2);
		IInstallableUnit iu2 = MetadataFactory.createInstallableUnit(iud2);

		ProfileChangeRequest changeRequest2 = new ProfileChangeRequest(profile);
		changeRequest2.add(iu1);
		changeRequest2.add(iu2);
		ProvisioningPlan plan2 = (ProvisioningPlan) planner.getProvisioningPlan(changeRequest2, null, null);
		assertNotOK(plan2.getStatus());

	}
}
