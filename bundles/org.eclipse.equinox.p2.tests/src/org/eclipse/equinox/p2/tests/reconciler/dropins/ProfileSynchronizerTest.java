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

public class ProfileSynchronizerTest extends AbstractProvisioningTest {
	private IProfile sdkProfile;
	private IProvisioningAgent agent;
	private IProfileRegistry registry;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		initializeReconciler();
		File tmpFolder = getTempFolder();

		copy("copying initialProfile", getTestData("p2 folder for synchronizer test", "testData/profileSynchronizerTest/"), tmpFolder);
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

	//This test case will remove the plugin aniefer,junit.headless that has been installed through the dropins.
	//The test checks that the roots properties are preserved
	public void testPropertyAreProperlyPutBack() throws IllegalArgumentException {
		Set<IInstallableUnit> oldRoots = sdkProfile.query(new IUProfilePropertyQuery("org.eclipse.equinox.p2.type.root", Boolean.TRUE.toString()), null).toUnmodifiableSet();

		assertFalse(sdkProfile.query(QueryUtil.createIUQuery("aniefer.junit.headless"), null).isEmpty());
		int originalSize = sdkProfile.query(QueryUtil.ALL_UNITS, null).toUnmodifiableSet().size();
		ProfileSynchronizer sync = new ProfileSynchronizer(agent, sdkProfile, new ArrayList<>());
		sync.synchronize(null);
		Set<IInstallableUnit> newRoots = registry.getProfile("SDKProfile").query(new IUProfilePropertyQuery("org.eclipse.equinox.p2.type.root", Boolean.TRUE.toString()), null).toSet();
		newRoots.removeAll(oldRoots);
		assertEquals(0, newRoots.size());
		assertTrue(registry.getProfile("SDKProfile").query(QueryUtil.createIUQuery("aniefer.junit.headless"), null).isEmpty());
		assertEquals(originalSize - 1, registry.getProfile("SDKProfile").query(QueryUtil.ALL_UNITS, null).toUnmodifiableSet().size());
	}

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
