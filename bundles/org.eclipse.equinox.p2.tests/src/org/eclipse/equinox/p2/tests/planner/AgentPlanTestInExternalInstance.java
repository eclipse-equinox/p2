/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Properties;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class AgentPlanTestInExternalInstance extends AbstractProvisioningTest {
	public void setUp() throws Exception {
		super.setUp();

		SimpleProfileRegistry profileRegistry = (SimpleProfileRegistry) getProfileRegistry();
		try {
			Field selfField = SimpleProfileRegistry.class.getDeclaredField("self"); //$NON-NLS-1$
			selfField.setAccessible(true);
			previousSelfValue = selfField.get(profileRegistry);
			selfField.set(profileRegistry, "agent");
			clearProfileMap(profileRegistry);
		} catch (Throwable t) {
			fail();
		}
		createProfile("agent");
		Properties p = new Properties();
		p.setProperty("org.eclipse.equinox.p2.planner.resolveMetaRequirements", "false");
		createProfile("installation", p);
	}

	public void tearDown() throws Exception {
		SimpleProfileRegistry profileRegistry = (SimpleProfileRegistry) getProfileRegistry();
		try {
			Field selfField = SimpleProfileRegistry.class.getDeclaredField("self"); //$NON-NLS-1$
			selfField.setAccessible(true);
			Object self = selfField.get(profileRegistry);
			if (self.equals("agent"))
				selfField.set(profileRegistry, previousSelfValue);
			clearProfileMap(profileRegistry);
		} catch (Throwable t) {
			fail();
		}
		super.tearDown();
	}

	public void testGetAgentPlanActionNeededButUnavailable() {
		IRequirement[] metaReq = createRequiredCapabilities("p2.action", "action1", new VersionRange("[0.0.0, 1.0.0]"));
		IInstallableUnit a = createIUWithMetaRequirement("A", DEFAULT_VERSION, true, NO_REQUIRES, metaReq);

		IProfile profile = getProfile("installation");
		IPlanner planner = createPlanner();

		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {a});
		ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setMetadataRepositories(new URI[0]);

		IProvisioningPlan plan = planner.getProvisioningPlan(request, context, new NullProgressMonitor());
		assertNotOK(plan.getStatus());
	}

	public void testGetAgentPlanActionNeeded() {
		IProvidedCapability act1Cap = MetadataFactory.createProvidedCapability("p2.action", "action1", DEFAULT_VERSION);
		IInstallableUnit act1 = createIU("Action1", DEFAULT_VERSION, null, NO_REQUIRES, new IProvidedCapability[] {act1Cap}, NO_PROPERTIES, null, NO_TP_DATA, true);

		IRequirement[] metaReq = createRequiredCapabilities("p2.action", "action1", new VersionRange("[0.0.0, 1.0.0]"));
		IInstallableUnit a = createIUWithMetaRequirement("A", DEFAULT_VERSION, true, NO_REQUIRES, metaReq);
		createTestMetdataRepository(new IInstallableUnit[] {a, act1});

		IProfile profile = getProfile("installation");
		IPlanner planner = createPlanner();
		ProvisioningContext ctx = new ProvisioningContext(getAgent());

		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {a});
		IProvisioningPlan plan = planner.getProvisioningPlan(request, ctx, new NullProgressMonitor());
		assertTrue(plan.getStatus().isOK());
		assertNotNull(plan.getInstallerPlan());
	}

	public void testConflictBetweenActionAndThingBeingInstalled() {
		//This tests the case where the action is in conflict with the thing being installed
		//The action needs another version of A which is singleton
		IProvidedCapability act1Cap = MetadataFactory.createProvidedCapability("p2.action", "action1", DEFAULT_VERSION);
		IInstallableUnit act1 = createIU("Action1", DEFAULT_VERSION, null, createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[2.0.0, 2.0.0]")), new IProvidedCapability[] {act1Cap}, NO_PROPERTIES, null, NO_TP_DATA, true);
		IRequirement[] metaReq = createRequiredCapabilities("p2.action", "action1", new VersionRange("[0.0.0, 1.0.0]"));
		IInstallableUnit a = createIUWithMetaRequirement("A", DEFAULT_VERSION, true, NO_REQUIRES, metaReq);
		IInstallableUnit a2 = createIU("A", Version.createOSGi(2, 0, 0));

		createTestMetdataRepository(new IInstallableUnit[] {a, a2, act1});

		IProfile profile = getProfile("installation");
		IPlanner planner = createPlanner();
		ProvisioningContext ctx = new ProvisioningContext(getAgent());

		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {a});
		IProvisioningPlan plan = planner.getProvisioningPlan(request, ctx, new NullProgressMonitor());
		assertOK("Provisioning plan", plan.getStatus());
	}

	public void testSubsequentInstall() {
		IProvidedCapability act1Cap = MetadataFactory.createProvidedCapability("p2.action", "action1", DEFAULT_VERSION);
		IInstallableUnit act1 = createIU("Action1", DEFAULT_VERSION, null, NO_REQUIRES, new IProvidedCapability[] {act1Cap}, NO_PROPERTIES, null, NO_TP_DATA, true);

		IRequirement[] metaReq = createRequiredCapabilities("p2.action", "action1", new VersionRange("[0.0.0, 1.0.0]"));
		IInstallableUnit a = createIUWithMetaRequirement("A", DEFAULT_VERSION, true, NO_REQUIRES, metaReq);
		IInstallableUnit b = createEclipseIU("B");

		createTestMetdataRepository(new IInstallableUnit[] {a, act1, b});

		IPlanner planner = createPlanner();
		IEngine engine = createEngine();
		ProvisioningContext ctx = new ProvisioningContext(getAgent());

		ProfileChangeRequest request = new ProfileChangeRequest(getProfile("installation"));
		request.addInstallableUnits(new IInstallableUnit[] {a});
		IProvisioningPlan plan = planner.getProvisioningPlan(request, ctx, new NullProgressMonitor());
		//Check that the actions are installed properly
		assertOK("install actions", engine.perform(plan.getInstallerPlan(), null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("agent"), new IInstallableUnit[] {act1});
		//Check that the IUs are installed in the profile
		assertOK("install A", engine.perform(plan, null));
		assertProfileContainsAll("Checking profile after initial install", getProfile("installation"), new IInstallableUnit[] {a});

		ProfileChangeRequest request2 = new ProfileChangeRequest(getProfile("installation"));
		request2.addInstallableUnits(new IInstallableUnit[] {b});
		IProvisioningPlan plan2 = planner.getProvisioningPlan(request2, ctx, new NullProgressMonitor());
		assertNull(plan2.getInstallerPlan());
		assertOK("install b", engine.perform(plan2, null));
		assertProfileContainsAll("Checking profile after initial install", getProfile("installation"), new IInstallableUnit[] {a, b});
		assertProfileContainsAll("Checking actions are still installed", getProfile("agent"), new IInstallableUnit[] {act1});
	}

	public void testWithOveralInDependency() {
		IInstallableUnit common = createEclipseIU("Common");
		IProvidedCapability act1Cap = MetadataFactory.createProvidedCapability("p2.action", "action1", DEFAULT_VERSION);
		IInstallableUnit act1 = createIU("Action1", DEFAULT_VERSION, null, createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "Common"), new IProvidedCapability[] {act1Cap}, NO_PROPERTIES, null, NO_TP_DATA, true);

		IRequirement[] metaReq = createRequiredCapabilities("p2.action", "action1", new VersionRange("[0.0.0, 1.0.0]"));
		IInstallableUnit a = createIUWithMetaRequirement("A", DEFAULT_VERSION, true, createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "Common"), metaReq);

		createTestMetdataRepository(new IInstallableUnit[] {a, act1, common});

		IProfile profile = getProfile("installation");
		IPlanner planner = createPlanner();
		IEngine engine = createEngine();
		ProvisioningContext ctx = new ProvisioningContext(getAgent());

		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {a});
		IProvisioningPlan plan = planner.getProvisioningPlan(request, ctx, new NullProgressMonitor());
		assertOK("install actions", engine.perform(plan.getInstallerPlan(), null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("agent"), new IInstallableUnit[] {act1, common});
		assertOK("install A", engine.perform(plan, null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("installation"), new IInstallableUnit[] {a, common});
	}

	public void testTwoInstallWithActions() {
		IProvidedCapability act1Cap = MetadataFactory.createProvidedCapability("p2.action", "action1", DEFAULT_VERSION);
		IInstallableUnit act1 = createIU("Action1", DEFAULT_VERSION, null, NO_REQUIRES, new IProvidedCapability[] {act1Cap}, NO_PROPERTIES, null, NO_TP_DATA, true);

		IRequirement[] metaReq = createRequiredCapabilities("p2.action", "action1", new VersionRange("[0.0.0, 1.0.0]"));
		IInstallableUnit a = createIUWithMetaRequirement("A", DEFAULT_VERSION, true, NO_REQUIRES, metaReq);

		IProvidedCapability act2Cap = MetadataFactory.createProvidedCapability("p2.action", "action2", DEFAULT_VERSION);
		IInstallableUnit act2 = createIU("Action2", DEFAULT_VERSION, null, NO_REQUIRES, new IProvidedCapability[] {act2Cap}, NO_PROPERTIES, null, NO_TP_DATA, true);

		IRequirement[] metaReq2 = createRequiredCapabilities("p2.action", "action2", new VersionRange("[0.0.0, 1.0.0]"));
		IInstallableUnit b = createIUWithMetaRequirement("B", DEFAULT_VERSION, true, NO_REQUIRES, metaReq2);

		createTestMetdataRepository(new IInstallableUnit[] {a, b, act1, act2,});

		IPlanner planner = createPlanner();
		IEngine engine = createEngine();
		ProvisioningContext ctx = new ProvisioningContext(getAgent());

		//install A which will install Action1
		ProfileChangeRequest request = new ProfileChangeRequest(getProfile("installation"));
		request.addInstallableUnits(new IInstallableUnit[] {a});
		IProvisioningPlan plan = planner.getProvisioningPlan(request, ctx, new NullProgressMonitor());
		assertOK("install actions", engine.perform(plan.getInstallerPlan(), null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("agent"), new IInstallableUnit[] {act1});
		assertOK("install A", engine.perform(plan, null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("installation"), new IInstallableUnit[] {a});

		//install B which will install Action2
		ProfileChangeRequest request2 = new ProfileChangeRequest(getProfile("installation"));
		request2.addInstallableUnits(new IInstallableUnit[] {b});
		IProvisioningPlan plan2 = planner.getProvisioningPlan(request2, ctx, new NullProgressMonitor());
		assertOK("install actions", engine.perform(plan2.getInstallerPlan(), null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("agent"), new IInstallableUnit[] {act2, act1});
		assertOK("install A", engine.perform(plan2, null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("installation"), new IInstallableUnit[] {b, a});
	}

	public void testCompleteScenario() {
		IProvidedCapability act1Cap = MetadataFactory.createProvidedCapability("p2.action", "action1", DEFAULT_VERSION);
		IInstallableUnit act1 = createIU("Action1", DEFAULT_VERSION, null, NO_REQUIRES, new IProvidedCapability[] {act1Cap}, NO_PROPERTIES, null, NO_TP_DATA, true);

		IRequirement[] metaReq = createRequiredCapabilities("p2.action", "action1", new VersionRange("[0.0.0, 1.0.0]"));
		IInstallableUnit a = createIUWithMetaRequirement("A", DEFAULT_VERSION, true, NO_REQUIRES, metaReq);

		IProvidedCapability act1bCap = MetadataFactory.createProvidedCapability("p2.action", "action1b", DEFAULT_VERSION);
		IInstallableUnit act1b = createIU("Action1b", DEFAULT_VERSION, null, NO_REQUIRES, new IProvidedCapability[] {act1bCap}, NO_PROPERTIES, null, NO_TP_DATA, true);

		IRequirement[] metaReqb = createRequiredCapabilities("p2.action", "action1b", new VersionRange("[0.0.0, 1.0.0]"));
		IInstallableUnit a111 = createIUWithMetaRequirement("A", Version.createOSGi(1, 1, 1), true, NO_REQUIRES, metaReqb);

		IProvidedCapability act2Cap = MetadataFactory.createProvidedCapability("p2.action", "action2", DEFAULT_VERSION);
		IInstallableUnit act2 = createIU("Action2", DEFAULT_VERSION, null, NO_REQUIRES, new IProvidedCapability[] {act2Cap}, NO_PROPERTIES, null, NO_TP_DATA, true);

		IProvidedCapability act1v2Cap = MetadataFactory.createProvidedCapability("p2.action", "action2", Version.createOSGi(2, 0, 0));
		IInstallableUnit act1v2 = createIU("Action1", Version.create("2.0.0"), null, NO_REQUIRES, new IProvidedCapability[] {act1v2Cap}, NO_PROPERTIES, null, NO_TP_DATA, true);

		IRequirement[] metaReqd = createRequiredCapabilities("p2.action", "action2", new VersionRange("[2.0.0, 2.0.0]"));
		IInstallableUnit d = createIUWithMetaRequirement("D", DEFAULT_VERSION, true, NO_REQUIRES, metaReqd);

		IRequirement[] metaReq2 = createRequiredCapabilities("p2.action", "action2", new VersionRange("[0.0.0, 1.0.0]"));
		IInstallableUnit b = createIUWithMetaRequirement("B", DEFAULT_VERSION, true, NO_REQUIRES, metaReq2);

		IInstallableUnit c = createEclipseIU("C");
		createTestMetdataRepository(new IInstallableUnit[] {a, b, act1, act1v2, act2, c, act1b, d, a111});

		IPlanner planner = createPlanner();
		IEngine engine = createEngine();
		ProvisioningContext ctx = new ProvisioningContext(getAgent());

		//install A which will install Action1
		ProfileChangeRequest request = new ProfileChangeRequest(getProfile("installation"));
		request.addInstallableUnits(new IInstallableUnit[] {a});
		IProvisioningPlan plan = planner.getProvisioningPlan(request, ctx, new NullProgressMonitor());
		assertOK("install actions for A", engine.perform(plan.getInstallerPlan(), null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("agent"), new IInstallableUnit[] {act1});
		assertOK("install A", engine.perform(plan, null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("installation"), new IInstallableUnit[] {a});

		//install B which will install Action2
		ProfileChangeRequest request2 = new ProfileChangeRequest(getProfile("installation"));
		request2.addInstallableUnits(new IInstallableUnit[] {b});
		IProvisioningPlan plan2 = planner.getProvisioningPlan(request2, ctx, new NullProgressMonitor());
		assertOK("install actions for B", engine.perform(plan2.getInstallerPlan(), null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("agent"), new IInstallableUnit[] {act2});
		assertOK("install B", engine.perform(plan2, null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("installation"), new IInstallableUnit[] {b, a});

		//install C
		ProfileChangeRequest requestForC = new ProfileChangeRequest(getProfile("installation"));
		requestForC.addInstallableUnits(new IInstallableUnit[] {c});
		IProvisioningPlan planForC = planner.getProvisioningPlan(requestForC, ctx, new NullProgressMonitor());
		assertNull(planForC.getInstallerPlan());
		assertOK("install C", engine.perform(planForC, null));
		assertProfileContainsAll("Checking profile after C", getProfile("installation"), new IInstallableUnit[] {a, b, c});
		assertProfileContainsAll("Checking profile after install of actions", getProfile("agent"), new IInstallableUnit[] {act2, act1});

		//update A which will install Action1b
		ProfileChangeRequest requestUpdateA = new ProfileChangeRequest(getProfile("installation"));
		requestUpdateA.removeInstallableUnits(new IInstallableUnit[] {a});
		requestUpdateA.addInstallableUnits(new IInstallableUnit[] {a111});
		IProvisioningPlan planUpdateA = planner.getProvisioningPlan(requestUpdateA, ctx, new NullProgressMonitor());
		assertOK("install actions for A 1.1.1", engine.perform(planUpdateA.getInstallerPlan(), null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("agent"), new IInstallableUnit[] {act1, act1b, act2});
		assertOK("install A", engine.perform(planUpdateA, null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("installation"), new IInstallableUnit[] {a111, b, c});
		assertTrue(getProfile("installation").query(QueryUtil.createIUQuery("Action1", DEFAULT_VERSION), null).isEmpty());
		assertEquals(getProfile("agent").getProfileId(), plan.getInstallerPlan().getProfile().getProfileId());

		//uninstall A
		ProfileChangeRequest request3 = new ProfileChangeRequest(getProfile("installation"));
		request3.removeInstallableUnits(new IInstallableUnit[] {a111});
		IProvisioningPlan plan3 = planner.getProvisioningPlan(request3, ctx, new NullProgressMonitor());
		//		assertOK("install actions", engine.perform(getProfile("agent"), new PhaseSetFactory(), plan3.getInstallerPlan().getOperands(), null, null));
		//		assertProfileContainsAll("Checking profile after install of actions", getProfile("agent"), new IInstallableUnit[] {act1b}); //At this point there is not 
		assertOK("install A", engine.perform(plan3, null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("installation"), new IInstallableUnit[] {c, b});

		//uninstall C
		ProfileChangeRequest request4 = new ProfileChangeRequest(getProfile("installation"));
		request4.removeInstallableUnits(new IInstallableUnit[] {c});
		IProvisioningPlan uninstallC = planner.getProvisioningPlan(request4, ctx, new NullProgressMonitor());
		assertNull(uninstallC.getInstallerPlan());
		assertOK("install C", engine.perform(uninstallC, null));
		assertProfileContainsAll("Checking profile after C", getProfile("installation"), new IInstallableUnit[] {b});
		assertProfileContainsAll("Checking profile after install of actions", getProfile("agent"), new IInstallableUnit[] {act2, act1b});

		//install D, This will cause the removal of act1 and cause the addition of act1v2  from the installer
		ProfileChangeRequest requestForD = new ProfileChangeRequest(getProfile("installation"));
		requestForD.addInstallableUnits(new IInstallableUnit[] {d});
		IProvisioningPlan planForD = planner.getProvisioningPlan(requestForD, ctx, new NullProgressMonitor());
		assertNotNull(planForD.getInstallerPlan());
		assertEquals(1, queryResultSize(planForD.getInstallerPlan().getRemovals().query(QueryUtil.createIUQuery(act1b.getId()), null)));
		assertOK("install actions", engine.perform(planForD.getInstallerPlan(), null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("agent"), new IInstallableUnit[] {act1v2});
		assertOK("install D", engine.perform(planForD, null));
		assertProfileContainsAll("Checking profile after D", getProfile("installation"), new IInstallableUnit[] {b, d});
		assertProfileContainsAll("Checking profile after install of actions", getProfile("agent"), new IInstallableUnit[] {act2, act1v2});
	}

	public void testConflictBetweenActions() {
		IProvidedCapability act1Cap = MetadataFactory.createProvidedCapability("p2.action", "action1", DEFAULT_VERSION);
		IInstallableUnit act1 = createIU("Action1", DEFAULT_VERSION, null, NO_REQUIRES, new IProvidedCapability[] {act1Cap}, NO_PROPERTIES, null, NO_TP_DATA, true);

		IRequirement[] metaReq = createRequiredCapabilities("p2.action", "action1", new VersionRange("[0.0.0, 1.0.0]"));
		IInstallableUnit a = createIUWithMetaRequirement("A", DEFAULT_VERSION, true, NO_REQUIRES, metaReq);

		IProvidedCapability act1v2Cap = MetadataFactory.createProvidedCapability("p2.action", "action2", Version.createOSGi(2, 0, 0));
		IInstallableUnit act1v2 = createIU("Action1", Version.create("2.0.0"), null, NO_REQUIRES, new IProvidedCapability[] {act1v2Cap}, NO_PROPERTIES, null, NO_TP_DATA, true);

		IRequirement[] metaReqd = createRequiredCapabilities("p2.action", "action2", new VersionRange("[2.0.0, 2.0.0]"));
		IInstallableUnit d = createIUWithMetaRequirement("D", DEFAULT_VERSION, true, NO_REQUIRES, metaReqd);

		createTestMetdataRepository(new IInstallableUnit[] {a, act1, d, act1v2});

		IProfile profile = getProfile("installation");
		IPlanner planner = createPlanner();
		ProvisioningContext ctx = new ProvisioningContext(getAgent());

		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {a, d});
		IProvisioningPlan plan = planner.getProvisioningPlan(request, ctx, new NullProgressMonitor());
		assertNotOK(plan.getStatus());
		assertEquals(getProfile("agent").getProfileId(), plan.getInstallerPlan().getProfile().getProfileId());
	}
}
