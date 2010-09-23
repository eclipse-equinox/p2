/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;

import java.io.File;
import java.lang.reflect.Field;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug309530 extends AbstractProvisioningTest {

	private File previousStoreValue = null;
	private IProfile profile;

	protected String getTestDataPath() {
		return "testData/bug309530";
	}

	protected void tearDown() throws Exception {
		SimpleProfileRegistry realProfileRegistry = (SimpleProfileRegistry) getProfileRegistry();

		Field profilesMapField = SimpleProfileRegistry.class.getDeclaredField("profiles"); //$NON-NLS-1$
		profilesMapField.setAccessible(true);
		profilesMapField.set(realProfileRegistry, null);

		Field profileStore = SimpleProfileRegistry.class.getDeclaredField("store");
		profileStore.setAccessible(true);
		profileStore.set(realProfileRegistry, previousStoreValue);
		super.tearDown();
	}

	/*
	 * 	/* (non-Javadoc)
	 * @see org.eclipse.equinox.p2.tests.AbstractProvisioningTest#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		File reporegistry1 = getTestData("loading planner test data", getTestDataPath());
		File tempFolder = getTempFolder();
		copy("0.2", reporegistry1, tempFolder);
		IProfileRegistry realProfileRegistry = getProfileRegistry();
		//Tweak the running profile registry
		Field profileStore = SimpleProfileRegistry.class.getDeclaredField("store");
		profileStore.setAccessible(true);
		previousStoreValue = (File) profileStore.get(realProfileRegistry);
		profileStore.set(realProfileRegistry, new File(tempFolder, "p2/org.eclipse.equinox.p2.engine/profileRegistry"));

		Field profilesMapField = SimpleProfileRegistry.class.getDeclaredField("profiles"); //$NON-NLS-1$
		profilesMapField.setAccessible(true);
		profilesMapField.set(realProfileRegistry, null);
		//End of tweaking the profile registry

		profile = realProfileRegistry.getProfile("PlatformProfile");
		assertNotNull(profile);
	}

	public void testInstall() {

		IPlanner planner = createPlanner();

		ProfileChangeRequest changeRequest = new ProfileChangeRequest(profile);

		String[] ids = new String[] {"hi", "b"};
		for (int i = 0; i < ids.length; i++) {
			String id = ids[i];
			IInstallableUnit iu = profile.query(QueryUtil.createIUQuery(id), new NullProgressMonitor()).iterator().next();
			changeRequest.remove(iu);
		}

		int origsize = changeRequest.getInstallableUnitProfilePropertiesToAdd().size();

		for (int i = 0; i < 5; i++) {
			IProvisioningPlan plan = planner.getProvisioningPlan(changeRequest, null, new NullProgressMonitor());
			IQueryable result = plan.getRemovals();
			IQueryResult<IInstallableUnit> r = result.query(QueryUtil.ALL_UNITS, getMonitor());
			assertFalse("1.0." + i, r.isEmpty());
			assertEquals("1.1." + i, origsize, changeRequest.getInstallableUnitProfilePropertiesToAdd().size());
		}
	}
}
