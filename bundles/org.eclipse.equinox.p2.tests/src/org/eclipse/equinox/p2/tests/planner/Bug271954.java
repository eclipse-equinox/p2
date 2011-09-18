/*******************************************************************************
 *  Copyright (c) 2009, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URI;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
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
		IQueryResult c = profile.available(QueryUtil.createIUQuery("A"), new NullProgressMonitor());
		assertEquals(1, queryResultSize(c));
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.removeInstallableUnits((IInstallableUnit[]) c.toArray(IInstallableUnit.class));

		ProvisioningContext ctx = new ProvisioningContext(getAgent());
		ctx.setMetadataRepositories(new URI[0]);
		ctx.setArtifactRepositories(new URI[0]);
		IProvisioningPlan plan = createPlanner().getProvisioningPlan(req, ctx, new NullProgressMonitor());
		assertOK("Uninstall plan for myBundle", plan.getStatus());
		assertNotNull(plan.getInstallerPlan().getFutureState());
		assertEquals(0, queryResultSize(plan.getInstallerPlan().getAdditions().query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())));
		assertEquals(0, queryResultSize(plan.getInstallerPlan().getRemovals().query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())));
		assertUninstallOperand(plan, (IInstallableUnit) c.iterator().next());
		assertEquals(2, queryResultSize(plan.getRemovals().query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())));
		assertEquals(1, queryResultSize(plan.getRemovals().query(QueryUtil.createIUQuery("A", Version.createOSGi(1, 0, 0)), new NullProgressMonitor())));
		assertEquals(1, queryResultSize(plan.getRemovals().query(QueryUtil.createIUQuery("Action1", Version.createOSGi(1, 0, 0)), new NullProgressMonitor())));
	}
}
