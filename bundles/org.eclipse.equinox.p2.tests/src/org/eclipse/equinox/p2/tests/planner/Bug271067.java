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

import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;

import java.io.File;
import java.lang.reflect.Field;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.planner.ProfileInclusionRules;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug271067 extends AbstractProvisioningTest {
	private IProfile profile;
	private File previousStoreValue = null;
	String profileLoadedId = "bootProfile";
	IMetadataRepository repo = null;

	protected void setUp() throws Exception {
		super.setUp();
		File reporegistry1 = getTestData("test data bug 271067", "testData/bug271067/profileRegistry");
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

		profile = realProfileRegistry.getProfile(profileLoadedId);
		assertNotNull(profile);
		repo = loadMetadataRepository(getTestData("Repository for 271067", "testData/bug271067/").toURI());
	}

	@Override
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

	IInstallableUnit getIU(IMetadataRepository source, String id, String version) {
		IQueryResult c = repo.query(QueryUtil.createIUQuery(id, Version.create(version)), new NullProgressMonitor());
		assertEquals(1, queryResultSize(c));
		return (IInstallableUnit) c.iterator().next();
	}

	public void testInstallFeaturePatch() {
		// hello.feature.2.feature.group 1.0.0, , hello 1.0.2, hello 1.0.0,  hello.feature.2.feature.jar 1.0.0]
		ProfileChangeRequest installFeature1 = new ProfileChangeRequest(getProfile(profileLoadedId));
		IInstallableUnit featureGroup = getIU(repo, "hello.feature.1.feature.group", "1.0.0");
		IInstallableUnit featureJar = getIU(repo, "hello.feature.1.feature.jar", "1.0.0");
		IInstallableUnit helloIU = getIU(repo, "hello", "1.0.0");
		IInstallableUnit patch = getIU(repo, "hello.patch.feature.group", "1.0.0");
		IInstallableUnit helloPatch = getIU(repo, "hello", "1.0.0.1");
		IInstallableUnit patchJar = getIU(repo, "hello.patch.feature.jar", "1.0.0");

		installFeature1.addInstallableUnits(new IInstallableUnit[] {featureGroup, featureJar, helloIU, patch, helloPatch, patchJar});

		installFeature1.setInstallableUnitInclusionRules(featureGroup, ProfileInclusionRules.createOptionalInclusionRule(featureGroup));
		installFeature1.setInstallableUnitInclusionRules(featureJar, ProfileInclusionRules.createOptionalInclusionRule(featureJar));
		installFeature1.setInstallableUnitInclusionRules(helloIU, ProfileInclusionRules.createOptionalInclusionRule(helloIU));
		installFeature1.setInstallableUnitInclusionRules(patch, ProfileInclusionRules.createOptionalInclusionRule(patch));
		installFeature1.setInstallableUnitInclusionRules(helloPatch, ProfileInclusionRules.createOptionalInclusionRule(helloPatch));
		installFeature1.setInstallableUnitInclusionRules(patchJar, ProfileInclusionRules.createOptionalInclusionRule(patchJar));

		IProvisioningPlan feature1Plan = createPlanner().getProvisioningPlan(installFeature1, new ProvisioningContext(getAgent()), null);
		assertOK("installation of feature1 and patch", createEngine().perform(feature1Plan, new NullProgressMonitor()));
		assertEquals(1, queryResultSize(getProfile(profileLoadedId).query(QueryUtil.createIUQuery("hello", Version.create("1.0.0.1")), new NullProgressMonitor())));

		IInstallableUnit featureGroup2 = getIU(repo, "hello.feature.2.feature.group", "1.0.0");
		IInstallableUnit helloIU2 = getIU(repo, "hello", "1.0.2");
		IInstallableUnit featureJar2 = getIU(repo, "hello.feature.2.feature.jar", "1.0.0");

		ProfileChangeRequest installFeature2 = new ProfileChangeRequest(getProfile(profileLoadedId));
		installFeature2.addInstallableUnits(new IInstallableUnit[] {featureGroup2, helloIU2, featureJar2});
		installFeature2.setInstallableUnitInclusionRules(featureGroup2, ProfileInclusionRules.createOptionalInclusionRule(featureGroup2));
		installFeature2.setInstallableUnitInclusionRules(helloIU2, ProfileInclusionRules.createOptionalInclusionRule(helloIU2));
		installFeature2.setInstallableUnitInclusionRules(featureJar2, ProfileInclusionRules.createOptionalInclusionRule(featureJar2));

		IProvisioningPlan feature2Plan = createPlanner().getProvisioningPlan(installFeature2, new ProvisioningContext(getAgent()), null);
		assertOK("installation of feature2", createEngine().perform(feature2Plan, new NullProgressMonitor()));
		assertEquals(1, queryResultSize(getProfile(profileLoadedId).query(QueryUtil.createIUQuery("hello", Version.create("1.0.0.1")), new NullProgressMonitor())));
	}

	public void testInstallFeaturePatchReverseOrder() {
		IInstallableUnit featureGroup2 = getIU(repo, "hello.feature.2.feature.group", "1.0.0");
		IInstallableUnit helloIU2 = getIU(repo, "hello", "1.0.2");
		IInstallableUnit featureJar2 = getIU(repo, "hello.feature.2.feature.jar", "1.0.0");

		ProfileChangeRequest installFeature2 = new ProfileChangeRequest(getProfile(profileLoadedId));
		installFeature2.addInstallableUnits(new IInstallableUnit[] {featureGroup2, helloIU2, featureJar2});
		installFeature2.setInstallableUnitInclusionRules(featureGroup2, ProfileInclusionRules.createOptionalInclusionRule(featureGroup2));
		installFeature2.setInstallableUnitInclusionRules(helloIU2, ProfileInclusionRules.createOptionalInclusionRule(helloIU2));
		installFeature2.setInstallableUnitInclusionRules(featureJar2, ProfileInclusionRules.createOptionalInclusionRule(featureJar2));

		IProvisioningPlan feature2Plan = createPlanner().getProvisioningPlan(installFeature2, new ProvisioningContext(getAgent()), null);
		assertOK("installation of feature2", createEngine().perform(feature2Plan, new NullProgressMonitor()));
		assertEquals(1, queryResultSize(getProfile(profileLoadedId).query(QueryUtil.createIUQuery("hello", Version.create("1.0.2")), new NullProgressMonitor())));

		ProfileChangeRequest installFeature1 = new ProfileChangeRequest(getProfile(profileLoadedId));
		IInstallableUnit featureGroup = getIU(repo, "hello.feature.1.feature.group", "1.0.0");
		IInstallableUnit featureJar = getIU(repo, "hello.feature.1.feature.jar", "1.0.0");
		IInstallableUnit helloIU = getIU(repo, "hello", "1.0.0");
		IInstallableUnit patch = getIU(repo, "hello.patch.feature.group", "1.0.0");
		IInstallableUnit helloPatch = getIU(repo, "hello", "1.0.0.1");
		IInstallableUnit patchJar = getIU(repo, "hello.patch.feature.jar", "1.0.0");

		installFeature1.addInstallableUnits(new IInstallableUnit[] {featureGroup, featureJar, helloIU, patch, helloPatch, patchJar});

		installFeature1.setInstallableUnitInclusionRules(featureGroup, ProfileInclusionRules.createOptionalInclusionRule(featureGroup));
		installFeature1.setInstallableUnitInclusionRules(featureJar, ProfileInclusionRules.createOptionalInclusionRule(featureJar));
		installFeature1.setInstallableUnitInclusionRules(helloIU, ProfileInclusionRules.createOptionalInclusionRule(helloIU));
		installFeature1.setInstallableUnitInclusionRules(patch, ProfileInclusionRules.createOptionalInclusionRule(patch));
		installFeature1.setInstallableUnitInclusionRules(helloPatch, ProfileInclusionRules.createOptionalInclusionRule(helloPatch));
		installFeature1.setInstallableUnitInclusionRules(patchJar, ProfileInclusionRules.createOptionalInclusionRule(patchJar));

		IProvisioningPlan feature1Plan = createPlanner().getProvisioningPlan(installFeature1, new ProvisioningContext(getAgent()), null);
		assertOK("installation of feature1 and patch", createEngine().perform(feature1Plan, new NullProgressMonitor()));
		assertEquals(1, queryResultSize(getProfile(profileLoadedId).query(QueryUtil.createIUQuery("hello", Version.create("1.0.0.1")), new NullProgressMonitor())));
	}

	public void installTogether() {
		IInstallableUnit featureGroup2 = getIU(repo, "hello.feature.2.feature.group", "1.0.0");
		IInstallableUnit helloIU2 = getIU(repo, "hello", "1.0.2");
		IInstallableUnit featureJar2 = getIU(repo, "hello.feature.2.feature.jar", "1.0.0");

		IInstallableUnit featureGroup = getIU(repo, "hello.feature.1.feature.group", "1.0.0");
		IInstallableUnit featureJar = getIU(repo, "hello.feature.1.feature.jar", "1.0.0");
		IInstallableUnit helloIU = getIU(repo, "hello", "1.0.0");
		IInstallableUnit patch = getIU(repo, "hello.patch.feature.group", "1.0.0");
		IInstallableUnit helloPatch = getIU(repo, "hello", "1.0.0.1");
		IInstallableUnit patchJar = getIU(repo, "hello.patch.feature.jar", "1.0.0");

		ProfileChangeRequest installEverything = new ProfileChangeRequest(getProfile(profileLoadedId));
		installEverything.addInstallableUnits(new IInstallableUnit[] {featureGroup2, helloIU2, featureJar2, featureGroup, featureJar, helloIU, patch, helloPatch, patchJar});
		installEverything.setInstallableUnitInclusionRules(featureGroup2, ProfileInclusionRules.createOptionalInclusionRule(featureGroup2));
		installEverything.setInstallableUnitInclusionRules(helloIU2, ProfileInclusionRules.createOptionalInclusionRule(helloIU2));
		installEverything.setInstallableUnitInclusionRules(featureJar2, ProfileInclusionRules.createOptionalInclusionRule(featureJar2));

		installEverything.setInstallableUnitInclusionRules(featureGroup, ProfileInclusionRules.createOptionalInclusionRule(featureGroup));
		installEverything.setInstallableUnitInclusionRules(featureJar, ProfileInclusionRules.createOptionalInclusionRule(featureJar));
		installEverything.setInstallableUnitInclusionRules(helloIU, ProfileInclusionRules.createOptionalInclusionRule(helloIU));
		installEverything.setInstallableUnitInclusionRules(patch, ProfileInclusionRules.createOptionalInclusionRule(patch));
		installEverything.setInstallableUnitInclusionRules(helloPatch, ProfileInclusionRules.createOptionalInclusionRule(helloPatch));
		installEverything.setInstallableUnitInclusionRules(patchJar, ProfileInclusionRules.createOptionalInclusionRule(patchJar));

		IProvisioningPlan plan = createPlanner().getProvisioningPlan(installEverything, new ProvisioningContext(getAgent()), null);
		assertOK("installation of feature1 and patch", createEngine().perform(plan, new NullProgressMonitor()));
		assertEquals(1, queryResultSize(getProfile(profileLoadedId).query(QueryUtil.createIUQuery("hello", Version.create("1.0.0.1")), new NullProgressMonitor())));
	}
}
