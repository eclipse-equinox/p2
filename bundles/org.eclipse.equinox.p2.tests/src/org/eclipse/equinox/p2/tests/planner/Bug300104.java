/*******************************************************************************
 *  Copyright (c) 2010 IBM Corporation and others.
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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.ProfileInclusionRules;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug300104 extends AbstractProvisioningTest {
	String profileLoadedId = "SDKProfile";
	IMetadataRepository repo = null;
	IProvisioningAgent agent = null;
	private IProfileRegistry profileRegistry;

	protected void setUp() throws Exception {
		super.setUp();
		File reporegistry1 = getTestData("test data bug 300104", "testData/bug300104/p2");
		File tempFolder = new File(getTempFolder(), "p2");
		copy("0.2", reporegistry1, tempFolder);

		IProvisioningAgentProvider provider = getAgentProvider();
		agent = provider.createAgent(tempFolder.toURI());
		profileRegistry = ((IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME));
		assertNotNull(profileRegistry.getProfile(profileLoadedId));
	}

	IInstallableUnit getIU(IMetadataRepository source, String id, String version) {
		IQueryResult c = repo.query(QueryUtil.createIUQuery(id, Version.create(version)), new NullProgressMonitor());
		assertEquals(1, queryResultSize(c));
		return (IInstallableUnit) c.iterator().next();
	}

	public void testInstallPatchesInOrder() throws ProvisionException {

		// generated metadata was massaged to remove artifact aspects which would require tests to also
		// provide artifacts.

		boolean optional = true;
		installHello(optional);

		applyHelloPatch1();

		applyHelloPatch2();

	}

	public void testInstallPatchesInOrderStricy() throws ProvisionException {

		// generated metadata was massaged to remove artifact aspects which would require tests to also
		// provide artifacts.

		boolean optional = false;
		installHello(optional);

		applyHelloPatch1();

		applyHelloPatch2();

	}

	public void testInstallOnlyLaterPatch() throws ProvisionException {
		boolean optional = true;
		installHello(optional);

		applyHelloPatch2();
	}

	private void installHello(boolean optional) throws ProvisionException {
		// install hello 1.0 and related feature.
		repo = loadMetadataRepository(getTestData("Repository for 300104", "testData/bug300104/hello1.0").toURI());
		ProfileChangeRequest installFeature1 = new ProfileChangeRequest(profileRegistry.getProfile(profileLoadedId));
		IInstallableUnit featureGroup = getIU(repo, "hellofeature.feature.group", "1.0.0.200911201237");
		IInstallableUnit featureJar = getIU(repo, "hellofeature.feature.jar", "1.0.0.200911201237");
		IInstallableUnit helloIU = getIU(repo, "hello", "1.0.0.200911201237");

		installFeature1.addInstallableUnits(new IInstallableUnit[] {featureGroup, featureJar, helloIU});

		installFeature1.setInstallableUnitInclusionRules(featureGroup, createInclusionRule(featureGroup, optional));
		installFeature1.setInstallableUnitInclusionRules(featureJar, ProfileInclusionRules.createOptionalInclusionRule(featureJar));
		installFeature1.setInstallableUnitInclusionRules(helloIU, ProfileInclusionRules.createOptionalInclusionRule(helloIU));

		IProvisioningPlan feature1Plan = getPlannerService().getProvisioningPlan(installFeature1, new ProvisioningContext(getAgent()), null);
		assertOK("installation of feature1", getEngineService().perform(feature1Plan, new NullProgressMonitor()));
		assertEquals(1, queryResultSize(profileRegistry.getProfile(profileLoadedId).query(QueryUtil.createIUQuery("hello", Version.create("1.0.0.200911201237")), new NullProgressMonitor())));
	}

	private IEngine getEngineService() {
		return ((IEngine) agent.getService(IEngine.SERVICE_NAME));
	}

	private IPlanner getPlannerService() {
		return ((IPlanner) agent.getService(IPlanner.SERVICE_NAME));
	}

	private String createInclusionRule(IInstallableUnit unit, boolean optional) {
		return optional ? ProfileInclusionRules.createOptionalInclusionRule(unit) : ProfileInclusionRules.createStrictInclusionRule(unit);
	}

	private void applyHelloPatch1() throws ProvisionException {
		// install first feature patch which updates hello to version 1.0.1
		/*
		    <unit id='hellopatch.feature.group' version='1.0.0' singleton='false'>
		    <unit id='hellopatch.feature.jar' version='1.0.0'>
		    <unit id='hello' version='1.0.1.200911201237'>
		*/

		repo = loadMetadataRepository(getTestData("Repository for 300104", "testData/bug300104/hellopatch1").toURI());

		ProfileChangeRequest installFeature1 = new ProfileChangeRequest(profileRegistry.getProfile(profileLoadedId));
		IInstallableUnit featureGroup = getIU(repo, "hellopatch.feature.group", "1.0.0");
		IInstallableUnit featureJar = getIU(repo, "hellopatch.feature.jar", "1.0.0");
		IInstallableUnit helloIU = getIU(repo, "hello", "1.0.1.200911201237");

		installFeature1.addInstallableUnits(new IInstallableUnit[] {featureGroup, featureJar, helloIU});

		installFeature1.setInstallableUnitInclusionRules(featureGroup, ProfileInclusionRules.createOptionalInclusionRule(featureGroup));
		installFeature1.setInstallableUnitInclusionRules(featureJar, ProfileInclusionRules.createOptionalInclusionRule(featureJar));
		installFeature1.setInstallableUnitInclusionRules(helloIU, ProfileInclusionRules.createOptionalInclusionRule(helloIU));

		IProvisioningPlan feature1Plan = getPlannerService().getProvisioningPlan(installFeature1, new ProvisioningContext(getAgent()), null);
		assertOK("installation of feature1", getEngineService().perform(feature1Plan, new NullProgressMonitor()));
		assertEquals(0, queryResultSize(profileRegistry.getProfile(profileLoadedId).query(QueryUtil.createIUQuery("hello", Version.create("1.0.0.200911201237")), new NullProgressMonitor())));
		assertEquals(1, queryResultSize(profileRegistry.getProfile(profileLoadedId).query(QueryUtil.createIUQuery("hello", Version.create("1.0.1.200911201237")), new NullProgressMonitor())));
	}

	private void applyHelloPatch2() throws ProvisionException {
		// install newer version of feature patch which updates 
		// hello to version 1.0.1 with later qualifier and 
		// adds unit hello2 1.0.0
		/*
		    <unit id='hellopatch.feature.group' version='1.0.1.200911201358' singleton='false'>
		    <unit id='hellopatch.feature.jar' version='1.0.1.200911201358'>
			<unit id='hello' version='1.0.1.200911201358'>
			<unit id='hello2' version='1.0.0.200911201358'>
		*/

		repo = loadMetadataRepository(getTestData("Repository for 300104", "testData/bug300104/hellopatch2").toURI());

		ProfileChangeRequest installFeature1 = new ProfileChangeRequest(profileRegistry.getProfile(profileLoadedId));
		IInstallableUnit featureGroup = getIU(repo, "hellopatch.feature.group", "1.0.1.200911201358");
		IInstallableUnit featureJar = getIU(repo, "hellopatch.feature.jar", "1.0.1.200911201358");
		IInstallableUnit helloIU = getIU(repo, "hello", "1.0.1.200911201358");
		IInstallableUnit hello2IU = getIU(repo, "hello2", "1.0.0.200911201358");

		installFeature1.addInstallableUnits(new IInstallableUnit[] {featureGroup, featureJar, helloIU, hello2IU});

		installFeature1.setInstallableUnitInclusionRules(featureGroup, ProfileInclusionRules.createOptionalInclusionRule(featureGroup));
		installFeature1.setInstallableUnitInclusionRules(featureJar, ProfileInclusionRules.createOptionalInclusionRule(featureJar));
		installFeature1.setInstallableUnitInclusionRules(helloIU, ProfileInclusionRules.createOptionalInclusionRule(helloIU));
		installFeature1.setInstallableUnitInclusionRules(hello2IU, ProfileInclusionRules.createOptionalInclusionRule(hello2IU));

		IProvisioningPlan feature1Plan = getPlannerService().getProvisioningPlan(installFeature1, new ProvisioningContext(getAgent()), null);
		assertOK("installation of feature1", getEngineService().perform(feature1Plan, new NullProgressMonitor()));
		assertEquals(1, queryResultSize(profileRegistry.getProfile(profileLoadedId).query(QueryUtil.createIUQuery("hello2", Version.create("1.0.0.200911201358")), new NullProgressMonitor())));

		assertEquals(0, queryResultSize(profileRegistry.getProfile(profileLoadedId).query(QueryUtil.createIUQuery("hello", Version.create("1.0.0.200911201237")), new NullProgressMonitor())));
		assertEquals(0, queryResultSize(profileRegistry.getProfile(profileLoadedId).query(QueryUtil.createIUQuery("hello", Version.create("1.0.1.200911201237")), new NullProgressMonitor())));
		assertEquals(1, queryResultSize(profileRegistry.getProfile(profileLoadedId).query(QueryUtil.createIUQuery("hello", Version.create("1.0.1.200911201358")), new NullProgressMonitor())));
	}
}
