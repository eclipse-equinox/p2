/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.util.Properties;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.operations.RequestFlexer;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.*;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.*;

public class TestRequestFlexerProduct2 extends AbstractProvisioningTest {
	public IInstallableUnit sdk1;

	@IUDescription(content = "package: platform \n" + "singleton: true\n" + "version: 1 \n")
	public IInstallableUnit platform1;

	public IInstallableUnit sdk2;

	@IUDescription(content = "package: platform \n" + "singleton: true\n" + "version: 2 \n")
	public IInstallableUnit platform2;

	@IUDescription(content = "package: eppPackage \n" + "singleton: true\n" + "version: 1 \n" + "depends: SDK = 1")
	public IInstallableUnit eppPackage1;

	@IUDescription(content = "package: eppPackage \n" + "singleton: true\n" + "version: 2 \n" + "depends: SDK = 2")
	public IInstallableUnit eppPackage2;

	IProfile profile;

	private IPlanner planner;

	private IEngine engine;

	private ProvisioningContext context;

	private void setupSDK1() {
		IRequirement[] reqPlatform1 = new IRequirement[1];
		reqPlatform1[0] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "platform", new VersionRange("[1.0.0,1.0.0]"), null, false, false, true);
		Properties p = new Properties();
		p.setProperty(MetadataFactory.InstallableUnitDescription.PROP_TYPE_PRODUCT, Boolean.TRUE.toString());
		sdk1 = createIU("SDK", Version.create("1.0.0"), null, reqPlatform1, new IProvidedCapability[0], p, null, null, true);
	}

	private void setupSDK2() {
		IRequirement[] reqPlatform1 = new IRequirement[1];
		reqPlatform1[0] = MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "platform", new VersionRange("[2.0.0,2.0.0]"), null, false, false, true);
		Properties p = new Properties();
		p.setProperty(MetadataFactory.InstallableUnitDescription.PROP_TYPE_PRODUCT, Boolean.TRUE.toString());
		IUpdateDescriptor update = MetadataFactory.createUpdateDescriptor("SDK", new VersionRange("[1.0.0,2.0.0)"), 0, "description");
		sdk2 = createIU("SDK", Version.create("2.0.0"), null, reqPlatform1, new IProvidedCapability[0], p, null, null, true, update, null);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		context = new ProvisioningContext(getAgent());
		profile = createProfile("TestProfile." + getName());
		IULoader.loadIUs(this);
		setupSDK1();
		setupSDK2();
		createTestMetdataRepository(new IInstallableUnit[] {sdk1, platform1, sdk2, platform2, eppPackage1, eppPackage2});
		planner = createPlanner();
		engine = createEngine();
		assertOK(installAsRoots(profile, new IInstallableUnit[] {eppPackage1}, true, planner, engine));
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		getProfileRegistry().removeProfile(profile.getProfileId());
	}

	public void testReplaceNonRootProduct() {
		RequestFlexer av = new RequestFlexer(planner);
		av.setAllowPartialInstall(false);
		av.setAllowDifferentVersion(false);
		av.setAllowInstalledElementRemoval(true);
		av.setAllowInstalledElementChange(true);
		av.setProvisioningContext(context);

		//Here we don't use the originalPRofileChangeRequest
		IProfileChangeRequest specialRequest = planner.createChangeRequest(profile);
		specialRequest.add(eppPackage2);
		specialRequest.setInstallableUnitInclusionRules(eppPackage2, ProfileInclusionRules.createStrictInclusionRule(eppPackage2));

		IProfileChangeRequest realRequest = av.getChangeRequest(specialRequest, profile, new NullProgressMonitor());

		//Check that we can update to something that is not flagged as product
		assertTrue(realRequest.getAdditions().contains(eppPackage2));

		assertFalse(getPlanner(getAgent()).getProvisioningPlan(realRequest, context, new NullProgressMonitor()).getAdditions().query(QueryUtil.createIUQuery("SDK", Version.create("2.0.0")), new NullProgressMonitor()).isEmpty());

	}
}
