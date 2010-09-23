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
import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug306424 extends AbstractProvisioningTest {

	private File previousStoreValue = null;
	private IProfile profile;

	protected String getTestDataPath() {
		return "testData/bug306424";
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

		IInstallableUnit b = profile.query(QueryUtil.createIUQuery("b"), new NullProgressMonitor()).iterator().next();
		IProfileChangeRequest changeRequest = new ProfileChangeRequest(profile);
		changeRequest.remove(b);

		IRequirement negateB = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, b.getId(), new VersionRange(b.getVersion(), true, b.getVersion(), true), null, 0, 0, false);
		Collection<IRequirement> extraReqs = new ArrayList<IRequirement>();
		extraReqs.add(negateB);
		changeRequest.addExtraRequirements(extraReqs);

		IProvisioningPlan plan = planner.getProvisioningPlan(changeRequest, null, new NullProgressMonitor());
		IQueryable result = plan.getRemovals();
		IQueryResult<IInstallableUnit> r = result.query(QueryUtil.ALL_UNITS, getMonitor());
		assertFalse("1.0", r.isEmpty());
	}
}
