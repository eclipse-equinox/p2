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
import java.util.Iterator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.IProvisioningPlan;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.ProfileInclusionRules;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug302582d extends AbstractProvisioningTest {
	String profileLoadedId = "bootProfile";
	IMetadataRepository repo = null;
	IProvisioningAgent agent = null;
	private IProfileRegistry profileRegistry;

	protected void setUp() throws Exception {
		super.setUp();
		File reporegistry1 = getTestData("test data bug 302582d", "testData/bug302582d/p2");
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

	public void testInstall() {
		IMetadataRepositoryManager mgr = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		try {
			repo = mgr.loadRepository(getTestData("test data bug bug302582d repo", "testData/bug302582d/repo").toURI(), null);
		} catch (ProvisionException e) {
			assertNull(e); //This guarantees that the error does not go unnoticed
		}
		IQueryResult<IInstallableUnit> ius = repo.query(QueryUtil.createIUAnyQuery(), null);
		IPlanner planner = getPlanner(agent);
		IProvisioningPlan plan = planner.getProvisioningPlan(createRequest(ius), null, new NullProgressMonitor());

		IProvisioningPlan expected = planner.getProvisioningPlan(createFilteredRequest(ius), null, new NullProgressMonitor());

		assertEquals("Plan comparison", expected.getAdditions().query(QueryUtil.createIUAnyQuery(), null).toArray(IInstallableUnit.class), plan.getAdditions().query(QueryUtil.createIUAnyQuery(), null).toArray(IInstallableUnit.class), false);
	}

	private ProfileChangeRequest createFilteredRequest(IQueryResult<IInstallableUnit> ius) {
		ProfileChangeRequest pcr = new ProfileChangeRequest(profileRegistry.getProfile(profileLoadedId));
		Iterator it = ius.iterator();
		while (it.hasNext()) {
			IInstallableUnit iu = (IInstallableUnit) it.next();
			if ((iu.getId().equals("com.dcns.rsm.rda") && iu.getVersion().equals(Version.create("5.1.0.v20100112"))) || (iu.getId().equals("com.dcns.rsm.profile.equipment") && iu.getVersion().equals(Version.create("1.2.2.v20100108"))) || (iu.getId().equals("com.dcns.rsm.profile.gemo") && iu.getVersion().equals(Version.create("3.7.2.v20100108"))) || (iu.getId().equals("com.dcns.rsm.profile.system") && iu.getVersion().equals(Version.create("4.2.2.v20100112")))) {
				pcr.addInstallableUnits(new IInstallableUnit[] {iu});
			}
		}
		return pcr;

	}

	private ProfileChangeRequest createRequest(IQueryResult<IInstallableUnit> ius) {
		ProfileChangeRequest pcr = new ProfileChangeRequest(profileRegistry.getProfile(profileLoadedId));
		pcr.addInstallableUnits(ius.toArray(IInstallableUnit.class));
		Iterator it = ius.iterator();
		while (it.hasNext()) {
			IInstallableUnit iu = (IInstallableUnit) it.next();
			pcr.setInstallableUnitInclusionRules(iu, ProfileInclusionRules.createOptionalInclusionRule(iu));
		}
		return pcr;
	}
}
