/*******************************************************************************
 *  Copyright (c) 2009, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.director.SimplePlanner;
import org.eclipse.equinox.internal.provisional.p2.director.PlanExecutionHelper;
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

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		a1 = createIU("A", Version.parseVersion("1.0.0"), createRequiredCapabilities(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.0.0, 1.0.0]")));
		b1 = createIU("B", Version.parseVersion("1.0.0"));
		// Note: C has an "optional" dependency on "B"
		c1 = createIU("C", Version.parseVersion("1.0.0"), new IRequirement[] {MetadataFactory.createRequirement(IInstallableUnit.NAMESPACE_IU_ID, "B", new VersionRange("[1.0.0, 1.0.0]"), null, true, false)});
		profile = createProfile(SimulatedSharedInstallTest.class.getName());
		planner = createPlanner();
		engine = createEngine();
	}

	public void testRemoveUnresolvedIU() {
		final ProvisioningContext context = new ProvisioningContext(getAgent());
		IProvisioningPlan plan = engine.createPlan(profile, context);
		plan.addInstallableUnit(a1);
		plan.setInstallableUnitProfileProperty(a1, SimplePlanner.INCLUSION_RULES, ProfileInclusionRules.createStrictInclusionRule(a1));
		context.setMetadataRepositories(new URI[0]);
		assertEquals(IStatus.OK, engine.perform(plan, new NullProgressMonitor()).getSeverity());
		assertContains(profile.query(QueryUtil.createIUAnyQuery(), null), a1);

		IProvisioningPlan plan2 = engine.createPlan(profile, context);
		plan2.removeInstallableUnit(a1);

		assertEquals(IStatus.OK, plan2.getStatus().getSeverity());
		assertEquals(IStatus.OK, PlanExecutionHelper.executePlan(plan2, engine, context, new NullProgressMonitor()).getSeverity());
		assertNotContains(profile.query(QueryUtil.createIUAnyQuery(), null), a1);
	}

	public void testAvailableVsQueryInProfile() {
		final ProvisioningContext context = new ProvisioningContext(getAgent());
		IProvisioningPlan plan = engine.createPlan(profile, context);
		plan.addInstallableUnit(c1);
		plan.setInstallableUnitProfileProperty(c1, SimplePlanner.INCLUSION_RULES, ProfileInclusionRules.createStrictInclusionRule(c1));
		context.setMetadataRepositories(new URI[0]);
		assertEquals(IStatus.OK, engine.perform(plan, new NullProgressMonitor()).getSeverity());
		assertContains(profile.query(QueryUtil.createIUAnyQuery(), null), c1);

		IProfile availableWrapper = new IProfile() {
			@Override
			public IQueryResult<IInstallableUnit> available(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
				IQueryResult<IInstallableUnit> queryResult = profile.query(query, monitor);
				Collector<IInstallableUnit> collector = new Collector<>();
				collector.addAll(queryResult);

				Collection<IInstallableUnit> ius = new ArrayList<>();
				ius.add(b1);
				collector.addAll(query.perform(ius.iterator()));
				return collector;
			}

			// everything else is delegated
			@Override
			public Map<String, String> getInstallableUnitProperties(IInstallableUnit iu) {
				return profile.getInstallableUnitProperties(iu);
			}

			@Override
			public String getInstallableUnitProperty(IInstallableUnit iu, String key) {
				return profile.getInstallableUnitProperty(iu, key);
			}

			@Override
			public String getProfileId() {
				return profile.getProfileId();
			}

			@Override
			public Map<String, String> getProperties() {
				return profile.getProperties();
			}

			@Override
			public String getProperty(String key) {
				return profile.getProperty(key);
			}

			@Override
			public long getTimestamp() {
				return profile.getTimestamp();
			}

			@Override
			public IQueryResult<IInstallableUnit> query(IQuery<IInstallableUnit> query, IProgressMonitor monitor) {
				return profile.query(query, monitor);
			}

			@Override
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
