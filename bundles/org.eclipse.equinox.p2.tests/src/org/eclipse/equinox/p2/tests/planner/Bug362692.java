/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.engine.InstallableUnitOperand;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.tests.TestActivator;

public class Bug362692 extends AbstractPlannerTest {

	// path to our data
	@Override
	protected String getTestDataPath() {
		return "testData/bug362692";
	}

	// profile id
	@Override
	protected String getProfileId() {
		return "bootProfile";
	}

	/*
	 * Test data registry profiles 1320939990376
	 * Already installed - A1.1.1 B1.1.1 C1.1.2 D1.1.4 E1.1.4
	 */
	public void testInstall() {
		IPlanner planner = createPlanner();

		// this is the set of IUs we expect in the final result - highest version only
		Set<IInstallableUnit> expected = new HashSet<>();
		IQueryResult<IInstallableUnit> queryResult = repo.query(QueryUtil.createIUQuery("PluginA", Version.createOSGi(1, 1, 1, null)), new NullProgressMonitor());
		expected.addAll(queryResult.toSet());
		queryResult = repo.query(QueryUtil.createIUQuery("PluginB", Version.createOSGi(1, 1, 2, null)), new NullProgressMonitor());
		expected.addAll(queryResult.toSet());
		queryResult = repo.query(QueryUtil.createIUQuery("PluginC", Version.createOSGi(1, 1, 3, null)), new NullProgressMonitor());
		expected.addAll(queryResult.toSet());
		queryResult = repo.query(QueryUtil.createIUQuery("PluginD", Version.createOSGi(1, 1, 4, null)), new NullProgressMonitor());
		expected.addAll(queryResult.toSet());
		queryResult = repo.query(QueryUtil.createIUQuery("PluginE", Version.createOSGi(1, 1, 5, null)), new NullProgressMonitor());
		expected.addAll(queryResult.toSet());

		// create the actual plan - install everything in the repo as optional (mimic the dropins folder)
		Set<IInstallableUnit> toAdd = new HashSet<>();
		// we don't want to re-install units which are already installed in the profile so remove them. (this is what the reconciler does)
		boolean already = false;
		for (IInstallableUnit iu : repo.query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor())) {
			//toAdd.add(iu);
			queryResult = getProfile().query(QueryUtil.createIUQuery(iu.getId(), iu.getVersion()), new NullProgressMonitor());
			if (queryResult.isEmpty()) {
				toAdd.add(iu);
			} else {
				System.out.println("Already installed: " + iu.getId() + " " + iu.getVersion());
				already = true;
			}
		}
		if (!already)
			System.out.println("Already installed: None!");
		validate(expected, toAdd);

		// mimic a product "-clean" and re-install everything which is already in the profile.
		//		toAdd.addAll(getProfile().query(QueryUtil.createIUAnyQuery(), new NullProgressMonitor()).toSet());

		// set the metadata repositories on the provisioning context. one for the dropins and one for the shared area
		Collection<URI> repoURLs = new ArrayList<>();
		repoURLs.add(repo.getLocation());
		repoURLs.add(new File(getTestDataPath(), "shared").toURI());
		ProvisioningContext context = getContext(repoURLs);
		context.setExtraInstallableUnits(new ArrayList<>(toAdd));
		IProfileChangeRequest actualChangeRequest = createProfileChangeRequest(toAdd, null, null);
		IProvisioningPlan plan = planner.getProvisioningPlan(actualChangeRequest, context, new NullProgressMonitor());
		Collection<InstallableUnitOperand> compressedPlan = compress(plan);
		if (compressedPlan.isEmpty())
			System.out.println("Plan: ...is empty!");
		for (InstallableUnitOperand installableUnitOperand : compressedPlan) {
			System.out.println("Plan: " + installableUnitOperand);
		}
		validate(expected, plan);
	}

	/*
	 * All of the expected IUs should either already be installed in the profile (and not be removed)
	 * or in the list of additions.
	 */
	private void validate(Collection<IInstallableUnit> expected, Collection<IInstallableUnit> toAdd) {
		MultiStatus errors = new MultiStatus(TestActivator.PI_PROV_TESTS, IStatus.OK, "Errors while validating plan.", null);
		for (IInstallableUnit unit : expected) {
			IQuery<IInstallableUnit> query = QueryUtil.createIUQuery(unit.getId(), unit.getVersion());
			// already in the profile?
			IQueryResult<IInstallableUnit> queryResult = getProfile().query(query, new NullProgressMonitor());
			if (queryResult.isEmpty()) {
				// not in the profile, should be an incoming addition then
				if (!toAdd.contains(unit)) {
					errors.add(new Status(IStatus.ERROR, TestActivator.PI_PROV_TESTS, unit.getId() + " " + unit.getVersion() + " isn't in the profile and isn't an incoming addition."));
				}
			} else {
				// expected IU is already in the profile
			}
		}
		assertOK("Errors while validating plan.", errors);
	}

	/*
	 * All of the expected IUs should either already be installed in the profile (and not be removed)
	 * or in the plan as an addition.
	 */
	private void validate(Collection<IInstallableUnit> expected, IProvisioningPlan plan) {
		MultiStatus errors = new MultiStatus(TestActivator.PI_PROV_TESTS, IStatus.OK, "Errors while validating plan.", null);
		for (IInstallableUnit unit : expected) {
			IQuery<IInstallableUnit> query = QueryUtil.createIUQuery(unit.getId(), unit.getVersion());
			// already in the profile?
			IQueryResult<IInstallableUnit> queryResult = getProfile().query(query, new NullProgressMonitor());
			if (queryResult.isEmpty()) {
				// not in the profile, should be an incoming addition then
				if (plan.getAdditions().query(query, new NullProgressMonitor()).isEmpty()) {
					errors.add(new Status(IStatus.ERROR, TestActivator.PI_PROV_TESTS, unit.getId() + " " + unit.getVersion() + " isn't in the profile and isn't an incoming addition."));
				}
			} else {
				// IU is in the profile, ensure we aren't removing it
				if (!plan.getRemovals().query(query, new NullProgressMonitor()).isEmpty()) {
					errors.add(new Status(IStatus.ERROR, TestActivator.PI_PROV_TESTS, unit.getId() + " " + unit.getVersion() + " is in the profile but is being removed."));
				}
			}
		}
		assertOK("Errors while validating plan.", errors);
	}
}
