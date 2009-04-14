/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Properties;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;

public class AgentPlanTestInExternalInstance extends AbstractProvisioningTest {
	Object previousSelfValue;

	public void setUp() throws Exception {
		super.setUp();

		SimpleProfileRegistry profileRegistry = (SimpleProfileRegistry) ServiceHelper.getService(TestActivator.getContext(), IProfileRegistry.class.getName());
		try {
			Field selfField = SimpleProfileRegistry.class.getDeclaredField("self"); //$NON-NLS-1$
			selfField.setAccessible(true);
			previousSelfValue = selfField.get(profileRegistry);
			if (previousSelfValue == null)
				selfField.set(profileRegistry, "agent");
			clearProfileMap(profileRegistry);
		} catch (Throwable t) {
			fail();
		}
		createProfile("agent");
		Properties p = new Properties();
		p.setProperty("org.eclipse.equinox.p2.planner.resolveMetaRequirements", "false");
		createProfile("installation", null, p);
	}

	public void tearDown() throws Exception {
		SimpleProfileRegistry profileRegistry = (SimpleProfileRegistry) ServiceHelper.getService(TestActivator.getContext(), IProfileRegistry.class.getName());
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
		IRequiredCapability[] metaReq = createRequiredCapabilities("p2.action", "action1", new VersionRange("[0.0.0, 1.0.0]"), null);
		IInstallableUnit a = createIUWithMetaRequirement("A", DEFAULT_VERSION, true, NO_REQUIRES, metaReq);

		IProfile profile = getProfile("installation");
		IPlanner planner = createPlanner();

		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {a});
		ProvisioningContext context = new ProvisioningContext(new URI[0]);

		ProvisioningPlan plan = planner.getProvisioningPlan(request, context, new NullProgressMonitor());
		assertNotOK(plan.getStatus());
	}

	public void testGetAgentPlanActionNeeded() {
		IProvidedCapability act1Cap = MetadataFactory.createProvidedCapability("p2.action", "action1", DEFAULT_VERSION);
		IInstallableUnit act1 = createIU("Action1", DEFAULT_VERSION, null, NO_REQUIRES, new IProvidedCapability[] {act1Cap}, NO_PROPERTIES, null, NO_TP_DATA, true);

		IRequiredCapability[] metaReq = createRequiredCapabilities("p2.action", "action1", new VersionRange("[0.0.0, 1.0.0]"), null);
		IInstallableUnit a = createIUWithMetaRequirement("A", DEFAULT_VERSION, true, NO_REQUIRES, metaReq);
		createTestMetdataRepository(new IInstallableUnit[] {a, act1});

		IProfile profile = getProfile("installation");
		IPlanner planner = createPlanner();
		ProvisioningContext ctx = new ProvisioningContext();

		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {a});
		ProvisioningPlan plan = planner.getProvisioningPlan(request, ctx, new NullProgressMonitor());
		assertTrue(plan.getStatus().isOK());
		assertNotNull(plan.getInstallerPlan());
	}

	public void testConflictBetweenActionAndThingBeingInstalled() {
		//This tests the case where the action is in conflict with the thing being installed
		//The action needs another version of A which is singleton
		IProvidedCapability act1Cap = MetadataFactory.createProvidedCapability("p2.action", "action1", DEFAULT_VERSION);
		IInstallableUnit act1 = createIU("Action1", DEFAULT_VERSION, null, createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "A", new VersionRange("[2.0.0, 2.0.0]"), null), new IProvidedCapability[] {act1Cap}, NO_PROPERTIES, null, NO_TP_DATA, true);
		IRequiredCapability[] metaReq = createRequiredCapabilities("p2.action", "action1", new VersionRange("[0.0.0, 1.0.0]"), null);
		IInstallableUnit a = createIUWithMetaRequirement("A", DEFAULT_VERSION, true, NO_REQUIRES, metaReq);
		IInstallableUnit a2 = createIU("A", new Version(2, 0, 0));

		createTestMetdataRepository(new IInstallableUnit[] {a, a2, act1});

		IProfile profile = getProfile("installation");
		IPlanner planner = createPlanner();
		ProvisioningContext ctx = new ProvisioningContext();

		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {a});
		ProvisioningPlan plan = planner.getProvisioningPlan(request, ctx, new NullProgressMonitor());
		assertOK("Provisioning plan", plan.getStatus());
	}

	public void testSubsequentInstall() {
		IProvidedCapability act1Cap = MetadataFactory.createProvidedCapability("p2.action", "action1", DEFAULT_VERSION);
		IInstallableUnit act1 = createIU("Action1", DEFAULT_VERSION, null, NO_REQUIRES, new IProvidedCapability[] {act1Cap}, NO_PROPERTIES, null, NO_TP_DATA, true);

		IRequiredCapability[] metaReq = createRequiredCapabilities("p2.action", "action1", new VersionRange("[0.0.0, 1.0.0]"), null);
		IInstallableUnit a = createIUWithMetaRequirement("A", DEFAULT_VERSION, true, NO_REQUIRES, metaReq);
		IInstallableUnit b = createEclipseIU("B");

		createTestMetdataRepository(new IInstallableUnit[] {a, act1, b});

		IPlanner planner = createPlanner();
		IEngine engine = createEngine();
		ProvisioningContext ctx = new ProvisioningContext();

		ProfileChangeRequest request = new ProfileChangeRequest(getProfile("installation"));
		request.addInstallableUnits(new IInstallableUnit[] {a});
		ProvisioningPlan plan = planner.getProvisioningPlan(request, ctx, new NullProgressMonitor());
		//Check that the actions are installed properly
		assertOK("install actions", engine.perform(getProfile("agent"), new DefaultPhaseSet(), plan.getInstallerPlan().getOperands(), null, null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("agent"), new IInstallableUnit[] {act1});
		//Check that the IUs are installed in the profile
		assertOK("install A", engine.perform(getProfile("installation"), new DefaultPhaseSet(), plan.getOperands(), null, null));
		assertProfileContainsAll("Checking profile after initial install", getProfile("installation"), new IInstallableUnit[] {a});

		ProfileChangeRequest request2 = new ProfileChangeRequest(getProfile("installation"));
		request2.addInstallableUnits(new IInstallableUnit[] {b});
		ProvisioningPlan plan2 = planner.getProvisioningPlan(request2, ctx, new NullProgressMonitor());
		assertNull(plan2.getInstallerPlan());
		assertOK("install b", engine.perform(getProfile("installation"), new DefaultPhaseSet(), plan2.getOperands(), null, null));
		assertProfileContainsAll("Checking profile after initial install", getProfile("installation"), new IInstallableUnit[] {a, b});
		assertProfileContainsAll("Checking actions are still installed", getProfile("agent"), new IInstallableUnit[] {act1});
	}

	public void testWithOveralInDependency() {
		IInstallableUnit common = createEclipseIU("Common");
		IProvidedCapability act1Cap = MetadataFactory.createProvidedCapability("p2.action", "action1", DEFAULT_VERSION);
		IInstallableUnit act1 = createIU("Action1", DEFAULT_VERSION, null, createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "Common", null), new IProvidedCapability[] {act1Cap}, NO_PROPERTIES, null, NO_TP_DATA, true);

		IRequiredCapability[] metaReq = createRequiredCapabilities("p2.action", "action1", new VersionRange("[0.0.0, 1.0.0]"), null);
		IInstallableUnit a = createIUWithMetaRequirement("A", DEFAULT_VERSION, true, createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "Common", null), metaReq);

		createTestMetdataRepository(new IInstallableUnit[] {a, act1, common});

		IProfile profile = getProfile("installation");
		IPlanner planner = createPlanner();
		IEngine engine = createEngine();
		ProvisioningContext ctx = new ProvisioningContext();

		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {a});
		ProvisioningPlan plan = planner.getProvisioningPlan(request, ctx, new NullProgressMonitor());
		assertOK("install actions", engine.perform(getProfile("agent"), new DefaultPhaseSet(), plan.getInstallerPlan().getOperands(), null, null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("agent"), new IInstallableUnit[] {act1, common});
		assertOK("install A", engine.perform(getProfile("installation"), new DefaultPhaseSet(), plan.getOperands(), null, null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("installation"), new IInstallableUnit[] {a, common});
	}

	public void testTwoInstallWithActions() {
		IProvidedCapability act1Cap = MetadataFactory.createProvidedCapability("p2.action", "action1", DEFAULT_VERSION);
		IInstallableUnit act1 = createIU("Action1", DEFAULT_VERSION, null, NO_REQUIRES, new IProvidedCapability[] {act1Cap}, NO_PROPERTIES, null, NO_TP_DATA, true);

		IRequiredCapability[] metaReq = createRequiredCapabilities("p2.action", "action1", new VersionRange("[0.0.0, 1.0.0]"), null);
		IInstallableUnit a = createIUWithMetaRequirement("A", DEFAULT_VERSION, true, NO_REQUIRES, metaReq);

		IProvidedCapability act2Cap = MetadataFactory.createProvidedCapability("p2.action", "action2", DEFAULT_VERSION);
		IInstallableUnit act2 = createIU("Action2", DEFAULT_VERSION, null, NO_REQUIRES, new IProvidedCapability[] {act2Cap}, NO_PROPERTIES, null, NO_TP_DATA, true);

		IRequiredCapability[] metaReq2 = createRequiredCapabilities("p2.action", "action2", new VersionRange("[0.0.0, 1.0.0]"), null);
		IInstallableUnit b = createIUWithMetaRequirement("B", DEFAULT_VERSION, true, NO_REQUIRES, metaReq2);

		createTestMetdataRepository(new IInstallableUnit[] {a, b, act1, act2,});

		IPlanner planner = createPlanner();
		IEngine engine = createEngine();
		ProvisioningContext ctx = new ProvisioningContext();

		//install A which will install Action1
		ProfileChangeRequest request = new ProfileChangeRequest(getProfile("installation"));
		request.addInstallableUnits(new IInstallableUnit[] {a});
		ProvisioningPlan plan = planner.getProvisioningPlan(request, ctx, new NullProgressMonitor());
		assertOK("install actions", engine.perform(getProfile("agent"), new DefaultPhaseSet(), plan.getInstallerPlan().getOperands(), null, null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("agent"), new IInstallableUnit[] {act1});
		assertOK("install A", engine.perform(getProfile("installation"), new DefaultPhaseSet(), plan.getOperands(), null, null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("installation"), new IInstallableUnit[] {a});

		//install B which will install Action2
		ProfileChangeRequest request2 = new ProfileChangeRequest(getProfile("installation"));
		request2.addInstallableUnits(new IInstallableUnit[] {b});
		ProvisioningPlan plan2 = planner.getProvisioningPlan(request2, ctx, new NullProgressMonitor());
		assertOK("install actions", engine.perform(getProfile("agent"), new DefaultPhaseSet(), plan2.getInstallerPlan().getOperands(), null, null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("agent"), new IInstallableUnit[] {act2, act1});
		assertOK("install A", engine.perform(getProfile("installation"), new DefaultPhaseSet(), plan2.getOperands(), null, null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("installation"), new IInstallableUnit[] {b, a});
	}

	public void testCompleteScenario() {
		IProvidedCapability act1Cap = MetadataFactory.createProvidedCapability("p2.action", "action1", DEFAULT_VERSION);
		IInstallableUnit act1 = createIU("Action1", DEFAULT_VERSION, null, NO_REQUIRES, new IProvidedCapability[] {act1Cap}, NO_PROPERTIES, null, NO_TP_DATA, true);

		IRequiredCapability[] metaReq = createRequiredCapabilities("p2.action", "action1", new VersionRange("[0.0.0, 1.0.0]"), null);
		IInstallableUnit a = createIUWithMetaRequirement("A", DEFAULT_VERSION, true, NO_REQUIRES, metaReq);

		IProvidedCapability act1bCap = MetadataFactory.createProvidedCapability("p2.action", "action1b", DEFAULT_VERSION);
		IInstallableUnit act1b = createIU("Action1b", DEFAULT_VERSION, null, NO_REQUIRES, new IProvidedCapability[] {act1bCap}, NO_PROPERTIES, null, NO_TP_DATA, true);

		IRequiredCapability[] metaReqb = createRequiredCapabilities("p2.action", "action1b", new VersionRange("[0.0.0, 1.0.0]"), null);
		IInstallableUnit a111 = createIUWithMetaRequirement("A", new Version(1, 1, 1), true, NO_REQUIRES, metaReqb);

		IProvidedCapability act2Cap = MetadataFactory.createProvidedCapability("p2.action", "action2", DEFAULT_VERSION);
		IInstallableUnit act2 = createIU("Action2", DEFAULT_VERSION, null, NO_REQUIRES, new IProvidedCapability[] {act2Cap}, NO_PROPERTIES, null, NO_TP_DATA, true);

		IProvidedCapability act1v2Cap = MetadataFactory.createProvidedCapability("p2.action", "action2", new Version(2, 0, 0));
		IInstallableUnit act1v2 = createIU("Action1", new Version("2.0.0"), null, NO_REQUIRES, new IProvidedCapability[] {act1v2Cap}, NO_PROPERTIES, null, NO_TP_DATA, true);

		IRequiredCapability[] metaReqd = createRequiredCapabilities("p2.action", "action2", new VersionRange("[2.0.0, 2.0.0]"), null);
		IInstallableUnit d = createIUWithMetaRequirement("D", DEFAULT_VERSION, true, NO_REQUIRES, metaReqd);

		IRequiredCapability[] metaReq2 = createRequiredCapabilities("p2.action", "action2", new VersionRange("[0.0.0, 1.0.0]"), null);
		IInstallableUnit b = createIUWithMetaRequirement("B", DEFAULT_VERSION, true, NO_REQUIRES, metaReq2);

		IInstallableUnit c = createEclipseIU("C");
		createTestMetdataRepository(new IInstallableUnit[] {a, b, act1, act1v2, act2, c, act1b, d, a111});

		IPlanner planner = createPlanner();
		IEngine engine = createEngine();
		ProvisioningContext ctx = new ProvisioningContext();

		//install A which will install Action1
		ProfileChangeRequest request = new ProfileChangeRequest(getProfile("installation"));
		request.addInstallableUnits(new IInstallableUnit[] {a});
		ProvisioningPlan plan = planner.getProvisioningPlan(request, ctx, new NullProgressMonitor());
		assertOK("install actions for A", engine.perform(getProfile("agent"), new DefaultPhaseSet(), plan.getInstallerPlan().getOperands(), null, null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("agent"), new IInstallableUnit[] {act1});
		assertOK("install A", engine.perform(getProfile("installation"), new DefaultPhaseSet(), plan.getOperands(), null, null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("installation"), new IInstallableUnit[] {a});

		//install B which will install Action2
		ProfileChangeRequest request2 = new ProfileChangeRequest(getProfile("installation"));
		request2.addInstallableUnits(new IInstallableUnit[] {b});
		ProvisioningPlan plan2 = planner.getProvisioningPlan(request2, ctx, new NullProgressMonitor());
		assertOK("install actions for B", engine.perform(getProfile("agent"), new DefaultPhaseSet(), plan2.getInstallerPlan().getOperands(), null, null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("agent"), new IInstallableUnit[] {act2});
		assertOK("install B", engine.perform(getProfile("installation"), new DefaultPhaseSet(), plan2.getOperands(), null, null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("installation"), new IInstallableUnit[] {b, a});

		//install C
		ProfileChangeRequest requestForC = new ProfileChangeRequest(getProfile("installation"));
		requestForC.addInstallableUnits(new IInstallableUnit[] {c});
		ProvisioningPlan planForC = planner.getProvisioningPlan(requestForC, ctx, new NullProgressMonitor());
		assertNull(planForC.getInstallerPlan());
		assertOK("install C", engine.perform(getProfile("installation"), new DefaultPhaseSet(), planForC.getOperands(), null, null));
		assertProfileContainsAll("Checking profile after C", getProfile("installation"), new IInstallableUnit[] {a, b, c});
		assertProfileContainsAll("Checking profile after install of actions", getProfile("agent"), new IInstallableUnit[] {act2, act1});

		//update A which will install Action1b
		ProfileChangeRequest requestUpdateA = new ProfileChangeRequest(getProfile("installation"));
		requestUpdateA.removeInstallableUnits(new IInstallableUnit[] {a});
		requestUpdateA.addInstallableUnits(new IInstallableUnit[] {a111});
		ProvisioningPlan planUpdateA = planner.getProvisioningPlan(requestUpdateA, ctx, new NullProgressMonitor());
		assertOK("install actions for A 1.1.1", engine.perform(getProfile("agent"), new DefaultPhaseSet(), planUpdateA.getInstallerPlan().getOperands(), null, null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("agent"), new IInstallableUnit[] {act1, act1b, act2});
		assertOK("install A", engine.perform(getProfile("installation"), new DefaultPhaseSet(), planUpdateA.getOperands(), null, null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("installation"), new IInstallableUnit[] {a111, b, c});
		assertEquals(0, getProfile("installation").query(new InstallableUnitQuery("Action1", DEFAULT_VERSION), new Collector(), null).size());
		assertEquals(requestUpdateA, planUpdateA.getProfileChangeRequest());
		assertEquals(getProfile("agent").getProfileId(), plan.getInstallerPlan().getProfileChangeRequest().getProfile().getProfileId());

		//uninstall A
		ProfileChangeRequest request3 = new ProfileChangeRequest(getProfile("installation"));
		request3.removeInstallableUnits(new IInstallableUnit[] {a111});
		ProvisioningPlan plan3 = planner.getProvisioningPlan(request3, ctx, new NullProgressMonitor());
		//		assertOK("install actions", engine.perform(getProfile("agent"), new DefaultPhaseSet(), plan3.getInstallerPlan().getOperands(), null, null));
		//		assertProfileContainsAll("Checking profile after install of actions", getProfile("agent"), new IInstallableUnit[] {act1b}); //At this point there is not 
		assertOK("install A", engine.perform(getProfile("installation"), new DefaultPhaseSet(), plan3.getOperands(), null, null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("installation"), new IInstallableUnit[] {c, b});

		//uninstall C
		ProfileChangeRequest request4 = new ProfileChangeRequest(getProfile("installation"));
		request4.removeInstallableUnits(new IInstallableUnit[] {c});
		ProvisioningPlan uninstallC = planner.getProvisioningPlan(request4, ctx, new NullProgressMonitor());
		assertNull(uninstallC.getInstallerPlan());
		assertOK("install C", engine.perform(getProfile("installation"), new DefaultPhaseSet(), uninstallC.getOperands(), null, null));
		assertProfileContainsAll("Checking profile after C", getProfile("installation"), new IInstallableUnit[] {b});
		assertProfileContainsAll("Checking profile after install of actions", getProfile("agent"), new IInstallableUnit[] {act2, act1b});

		//install D, This will cause the removal of act1 and cause the addition of act1v2  from the installer
		ProfileChangeRequest requestForD = new ProfileChangeRequest(getProfile("installation"));
		requestForD.addInstallableUnits(new IInstallableUnit[] {d});
		ProvisioningPlan planForD = planner.getProvisioningPlan(requestForD, ctx, new NullProgressMonitor());
		assertNotNull(planForD.getInstallerPlan());
		assertEquals(1, planForD.getInstallerPlan().getRemovals().query(new InstallableUnitQuery(act1b.getId()), new Collector(), null).size());
		assertOK("install actions", engine.perform(getProfile("agent"), new DefaultPhaseSet(), planForD.getInstallerPlan().getOperands(), null, null));
		assertProfileContainsAll("Checking profile after install of actions", getProfile("agent"), new IInstallableUnit[] {act1v2});
		assertOK("install D", engine.perform(getProfile("installation"), new DefaultPhaseSet(), planForD.getOperands(), null, null));
		assertProfileContainsAll("Checking profile after D", getProfile("installation"), new IInstallableUnit[] {b, d});
		assertProfileContainsAll("Checking profile after install of actions", getProfile("agent"), new IInstallableUnit[] {act2, act1v2});
	}

	public void testConflictBetweenActions() {
		IProvidedCapability act1Cap = MetadataFactory.createProvidedCapability("p2.action", "action1", DEFAULT_VERSION);
		IInstallableUnit act1 = createIU("Action1", DEFAULT_VERSION, null, NO_REQUIRES, new IProvidedCapability[] {act1Cap}, NO_PROPERTIES, null, NO_TP_DATA, true);

		IRequiredCapability[] metaReq = createRequiredCapabilities("p2.action", "action1", new VersionRange("[0.0.0, 1.0.0]"), null);
		IInstallableUnit a = createIUWithMetaRequirement("A", DEFAULT_VERSION, true, NO_REQUIRES, metaReq);

		IProvidedCapability act1v2Cap = MetadataFactory.createProvidedCapability("p2.action", "action2", new Version(2, 0, 0));
		IInstallableUnit act1v2 = createIU("Action1", new Version("2.0.0"), null, NO_REQUIRES, new IProvidedCapability[] {act1v2Cap}, NO_PROPERTIES, null, NO_TP_DATA, true);

		IRequiredCapability[] metaReqd = createRequiredCapabilities("p2.action", "action2", new VersionRange("[2.0.0, 2.0.0]"), null);
		IInstallableUnit d = createIUWithMetaRequirement("D", DEFAULT_VERSION, true, NO_REQUIRES, metaReqd);

		createTestMetdataRepository(new IInstallableUnit[] {a, act1, d, act1v2});

		IProfile profile = getProfile("installation");
		IPlanner planner = createPlanner();
		ProvisioningContext ctx = new ProvisioningContext();

		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.addInstallableUnits(new IInstallableUnit[] {a, d});
		ProvisioningPlan plan = planner.getProvisioningPlan(request, ctx, new NullProgressMonitor());
		assertNotOK(plan.getStatus());
		assertEquals(request, plan.getProfileChangeRequest());
		assertEquals(getProfile("agent").getProfileId(), plan.getInstallerPlan().getProfileChangeRequest().getProfile().getProfileId());
	}
}
