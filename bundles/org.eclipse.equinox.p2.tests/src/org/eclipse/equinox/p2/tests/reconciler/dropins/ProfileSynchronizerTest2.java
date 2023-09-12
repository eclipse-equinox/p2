/*******************************************************************************
 *  Copyright (c) 2010 Sonatype Inc. and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  	Sonatype Inc. - Initial API and implementation
 *     IBM Corporation - Ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.reconciler.dropins;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Set;
import org.eclipse.equinox.internal.p2.reconciler.dropins.ProfileSynchronizer;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IEngine;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.query.IUProfilePropertyQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;

public class ProfileSynchronizerTest2 extends AbstractProvisioningTest {
	private IProfile sdkProfile;
	private IProvisioningAgent agent;
	private IProfileRegistry registry;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		initializeReconciler();

		File tmpFolder = getTempFolder();
		copy("copying initialProfile", getTestData("p2 folder for synchronizer test", "testData/profileSynchronizerTest2/"), tmpFolder);
		agent = getAgentProvider().createAgent(new File(tmpFolder, "p2").toURI());
		registry = agent.getService(IProfileRegistry.class);
		IEngine engine = agent.getService(IEngine.class);
		sdkProfile = registry.getProfile("SDKProfile");

		//Reset some properties to not break local install
		IProvisioningPlan plan = engine.createPlan(sdkProfile, null);
		File installFolder = getTempFolder();
		plan.setProfileProperty("org.eclipse.equinox.p2.installFolder", installFolder.getAbsolutePath());
		plan.setProfileProperty("org.eclipse.equinox.p2.cache", installFolder.getAbsolutePath());
		engine.perform(plan, null);
	}

	//In this case GEF is installed through dropins and GMF is installed strictly.
	//GEF will be removed by the reconciliation, which should cause GMF to be removed
	public void testRemovalOfStrictRoot() throws IllegalArgumentException {
		Set<IInstallableUnit> oldRoots = sdkProfile.query(new IUProfilePropertyQuery("org.eclipse.equinox.p2.type.root", Boolean.TRUE.toString()), null).toUnmodifiableSet();

		assertFalse("could not find gmf", sdkProfile.query(QueryUtil.createIUQuery("org.eclipse.gmf.sdk.feature.group"), null).isEmpty());
		ProfileSynchronizer sync = new ProfileSynchronizer(agent, sdkProfile, new ArrayList<>());
		sync.synchronize(null);
		Set<IInstallableUnit> newRoots = registry.getProfile("SDKProfile").query(new IUProfilePropertyQuery("org.eclipse.equinox.p2.type.root", Boolean.TRUE.toString()), null).toSet();
		newRoots.removeAll(oldRoots);
		assertEquals(0, newRoots.size());
		assertTrue("gmf should not be there", registry.getProfile("SDKProfile").query(QueryUtil.createIUQuery("org.eclipse.gmf.sdk.feature.group"), null).isEmpty());
	}

	//We need to initialize the reconciler this way to bypass the automatic reconciliation that happens when the bundle is started
	private void initializeReconciler() throws IllegalAccessException {
		Field[] fields = org.eclipse.equinox.internal.p2.reconciler.dropins.Activator.class.getDeclaredFields();
		for (Field field : fields) {
			if (field.getName().equals("bundleContext")) {
				field.setAccessible(true);
				field.set(org.eclipse.equinox.internal.p2.reconciler.dropins.Activator.class, TestActivator.getContext());
				break;
			}
		}
	}
}
