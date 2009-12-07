/*******************************************************************************
 *  Copyright (c) 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URI;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.ProvisioningContext;
import org.eclipse.equinox.internal.provisional.p2.metadata.Version;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug271954 extends AbstractProvisioningTest {
	private static final String profileLoadedId = "SDKProfile";
	private IProfile profile;
	private File previousStoreValue = null;
	private Object previousSelfProfile = null;

	protected void setUp() throws Exception {
		super.setUp();
		File reporegistry1 = getTestData("test data bug 271954", "testData/bug271954");
		File tempFolder = getTempFolder();
		copy("0.2", reporegistry1, tempFolder);

		SimpleProfileRegistry realProfileRegistry = (SimpleProfileRegistry) getProfileRegistry();
		//Tweak the running profile registry
		Field profileStore = SimpleProfileRegistry.class.getDeclaredField("store");
		profileStore.setAccessible(true);
		previousStoreValue = (File) profileStore.get(realProfileRegistry);
		profileStore.set(realProfileRegistry, tempFolder);

		Field profilesMapField = SimpleProfileRegistry.class.getDeclaredField("profiles"); //$NON-NLS-1$
		profilesMapField.setAccessible(true);
		profilesMapField.set(realProfileRegistry, null);
		//End of tweaking the profile registry

		Field selfField = SimpleProfileRegistry.class.getDeclaredField("self"); //$NON-NLS-1$
		selfField.setAccessible(true);
		previousSelfProfile = selfField.get(realProfileRegistry);
		selfField.set(realProfileRegistry, "SDKProfile");

		profile = realProfileRegistry.getProfile(profileLoadedId);
	}

	protected void tearDown() throws Exception {
		SimpleProfileRegistry realProfileRegistry = (SimpleProfileRegistry) getProfileRegistry();

		Field profilesMapField = SimpleProfileRegistry.class.getDeclaredField("profiles"); //$NON-NLS-1$
		profilesMapField.setAccessible(true);
		profilesMapField.set(realProfileRegistry, null);

		Field profileStore = SimpleProfileRegistry.class.getDeclaredField("store");
		profileStore.setAccessible(true);
		profileStore.set(realProfileRegistry, previousStoreValue);

		Field selfField = SimpleProfileRegistry.class.getDeclaredField("self"); //$NON-NLS-1$
		selfField.setAccessible(true);
		selfField.set(realProfileRegistry, previousSelfProfile);

		super.tearDown();
	}

	public void testUninstallMyBundle() {
		Collector c = profile.available(new InstallableUnitQuery("A"), new Collector(), new NullProgressMonitor());
		assertEquals(1, c.size());
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.removeInstallableUnits((IInstallableUnit[]) c.toArray(IInstallableUnit.class));

		ProvisioningContext ctx = new ProvisioningContext(new URI[0]);
		ctx.setArtifactRepositories(new URI[0]);
		IProvisioningPlan plan = createPlanner().getProvisioningPlan(req, ctx, new NullProgressMonitor());
		assertOK("Uninstall plan for myBundle", plan.getStatus());
		assertEquals(0, plan.getInstallerPlan().getAdditions().query(InstallableUnitQuery.ANY, new Collector(), new NullProgressMonitor()).size());
		assertEquals(0, plan.getInstallerPlan().getRemovals().query(InstallableUnitQuery.ANY, new Collector(), new NullProgressMonitor()).size());
		assertUninstallOperand(plan, (IInstallableUnit) c.iterator().next());
		assertEquals(2, plan.getRemovals().query(InstallableUnitQuery.ANY, new Collector(), new NullProgressMonitor()).size());
		assertEquals(1, plan.getRemovals().query(new InstallableUnitQuery("A", Version.createOSGi(1, 0, 0)), new Collector(), new NullProgressMonitor()).size());
		assertEquals(1, plan.getRemovals().query(new InstallableUnitQuery("Action1", Version.createOSGi(1, 0, 0)), new Collector(), new NullProgressMonitor()).size());
	}
}
