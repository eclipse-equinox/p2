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

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.director.PlanExecutionHelper;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.ProfileInclusionRules;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class SimulatedSharedInstallTest extends AbstractProvisioningTest {

	IInstallableUnit a1;
	IInstallableUnit b1;
	IInstallableUnit c1;

	IPlanner planner;
	IProfile profile;
	IEngine engine;

	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", Version.parseVersion("1.0.0"), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.0.0, 1.0.0]")));
		b1 = createIU("B", Version.parseVersion("1.0.0"));
		// Note: C has an "optional" dependency on "B"
		c1 = createIU("C", Version.parseVersion("1.0.0"), new IRequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.0.0, 1.0.0]"), null, true, false)});
		profile = createProfile(SimulatedSharedInstallTest.class.getName());
		planner = createPlanner();
		engine = createEngine();
	}

	public void testRemoveUnresolvedIU() {
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.setAbsoluteMode(true);
		request.addInstallableUnits(new IInstallableUnit[] {a1});
		request.setInstallableUnitInclusionRules(a1, ProfileInclusionRules.createStrictInclusionRule(a1));
		final ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setMetadataRepositories(new URI[0]);
		IProvisioningPlan plan = planner.getProvisioningPlan(request, context, new NullProgressMonitor());
		assertEquals(IStatus.OK, engine.perform(plan, new NullProgressMonitor()).getSeverity());
		assertContains(profile.query(QueryUtil.createIUAnyQuery(), null), a1);

		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.removeInstallableUnits(new IInstallableUnit[] {a1});

		IProvisioningPlan plan2 = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan2.getStatus().getSeverity());
		assertEquals(IStatus.OK, PlanExecutionHelper.executePlan(plan2, engine, context, new NullProgressMonitor()).getSeverity());
		assertNotContains(profile.query(QueryUtil.createIUAnyQuery(), null), a1);
	}

	public void testAvailableVsQueryInProfile() {
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		request.setAbsoluteMode(true);
		request.addInstallableUnits(new IInstallableUnit[] {c1});
		request.setInstallableUnitInclusionRules(c1, ProfileInclusionRules.createStrictInclusionRule(c1));
		final ProvisioningContext context = new ProvisioningContext(getAgent());
		context.setMetadataRepositories(new URI[0]);
		IProvisioningPlan plan = planner.getProvisioningPlan(request, context, new NullProgressMonitor());
		assertEquals(IStatus.OK, engine.perform(plan, new NullProgressMonitor()).getSeverity());
		assertContains(profile.query(QueryUtil.createIUAnyQuery(), null), c1);

		IProfile availableWrapper = new IProfile() {
			public IQueryResult<IInstallableUnit> available(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
				IQueryResult queryResult = profile.query(query, monitor);
				Collector collector = new Collector();
				collector.addAll(queryResult);

				Collection ius = new ArrayList();
				ius.add(b1);
				collector.addAll(query.perform(ius.iterator()));
				return collector;
			}

			// everything else is delegated
			public Map getInstallableUnitProperties(IInstallableUnit iu) {
				return profile.getInstallableUnitProperties(iu);
			}

			public String getInstallableUnitProperty(IInstallableUnit iu, String key) {
				return profile.getInstallableUnitProperty(iu, key);
			}

			public String getProfileId() {
				return profile.getProfileId();
			}

			public Map getProperties() {
				return profile.getProperties();
			}

			public String getProperty(String key) {
				return profile.getProperty(key);
			}

			public long getTimestamp() {
				return profile.getTimestamp();
			}

			public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
				return profile.query(query, monitor);
			}

			public IProvisioningAgent getProvisioningAgent() {
				return profile.getProvisioningAgent();
			}
		};

		ProfileChangeRequest req = new ProfileChangeRequest(availableWrapper);
		req.addInstallableUnits(new IInstallableUnit[] {a1});
		IProvisioningPlan plan2 = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan2.getStatus().getSeverity());

		//expect to have both (a1+inclusion rule) and b1 added
		assertEquals(2, countPlanElements(plan2));
	}
}
