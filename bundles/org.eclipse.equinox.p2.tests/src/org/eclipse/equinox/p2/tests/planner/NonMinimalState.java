/*******************************************************************************
 *  Copyright (c) 2005, 2017 IBM Corporation and others.
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

import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.engine.query.IUProfilePropertyQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnitFragment;
import org.eclipse.equinox.p2.metadata.IRequirement;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

/*
 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=276133 for details.
 */
public class NonMinimalState extends AbstractProvisioningTest {
	IProfile profile = null;
	IMetadataRepository repo = null;
	private String searchedId;
	private Set<IInstallableUnit> visited = new HashSet<>();

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		File reporegistry1 = getTestData("Non Minimal state", "testData/nonMinimalState/");
		File tempFolder = getTempFolder();
		copy("0.2", reporegistry1, tempFolder);
		SimpleProfileRegistry registry = new SimpleProfileRegistry(getAgent(), tempFolder, null, false);
		profile = registry.getProfile("NonMinimalState");
		getMetadataRepositoryManager().addRepository(getTestData("nonMinimalState-galileoM7", "testData/galileoM7/").toURI());
		assertNotNull(profile);
	}

	public void testValidateProfileWithRepository() {
		IPlanner planner = createPlanner();
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		IProvisioningPlan plan = planner.getProvisioningPlan(request, null, new NullProgressMonitor());
		assertOK("Plan OK", plan.getStatus());
		assertTrue(plan.getAdditions().query(QueryUtil.createIUQuery("org.eclipse.tptp.platform.agentcontroller"), null).isEmpty());
		why("slf4j.api");
		why("slf4j.jcl");
		why("org.eclipse.tptp.platform.iac.administrator");
		why("org.eclipse.tptp.platform.agentcontroller");
	}

	public void testValidateProfileWithoutRepo() {
		IPlanner planner = createPlanner();
		ProfileChangeRequest request = new ProfileChangeRequest(profile);
		ProvisioningContext ctx = new ProvisioningContext(getAgent());
		ctx.setMetadataRepositories(new URI[0]);
		IProvisioningPlan plan = planner.getProvisioningPlan(request, ctx, new NullProgressMonitor());
		assertOK("Plan OK", plan.getStatus());
		assertTrue(plan.getAdditions().query(QueryUtil.createIUQuery("org.eclipse.tptp.platform.agentcontroller"), null).isEmpty());
	}

	private void why(String id) {
		System.out.println("=-=-=" + id + "=-=-=");
		visited = new HashSet<>();
		IQueryResult<IInstallableUnit> roots = profile.query(new IUProfilePropertyQuery("org.eclipse.equinox.p2.type.root", "true"), null);
		searchedId = id;
		for (IInstallableUnit type : roots) {
			if (type instanceof IInstallableUnitFragment) {
				visited.add(type);
				continue;
			}
			if (processIU(type)) {
				return;
			}
		}
	}

	public boolean processIU(IInstallableUnit iu) {
		if (iu.getId().equals("toolingorg.eclipse.equinox.launcher") || iu.getId().equals("tooling.osgi.bundle.default") || iu.getId().startsWith("tooling")) {
			//		if (iu instanceof IInstallableUnitFragment) {
			visited.add(iu);
			return false;
		}
		for (IRequirement req : iu.getRequirements()) {
			boolean result = expandRequirement(iu, req);
			if (result) {
				System.out.println(iu + " because " + req.toString());
				return true;
			}
		}
		return false;
	}

	private boolean expandRequirement(IInstallableUnit iu, IRequirement req) {
		for (IInstallableUnit match : profile.query(QueryUtil.createMatchQuery(req.getMatches()), null)) {
			if (match.getId().equals(searchedId)) {
				return true;
			}
			if (!visited.contains(match)) {
				visited.add(match);
				if (processIU(match)) {
					return true;
				}
			}
		}
		return false;
	}
}
