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

import java.io.File;
import java.util.Iterator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.director.PlannerHelper;
import org.eclipse.equinox.internal.provisional.p2.director.ProfileChangeRequest;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug300104bis extends AbstractProvisioningTest {
	String profileLoadedId = "bootProfile";
	IMetadataRepository repo = null;
	IProvisioningAgent agent = null;
	private IProfileRegistry profileRegistry;

	protected void setUp() throws Exception {
		super.setUp();
		File reporegistry1 = getTestData("test data bug 300104bis", "testData/bug300104bis/p2");
		File tempFolder = new File(getTempFolder(), "p2");
		copy("0.2", reporegistry1, tempFolder);

		IProvisioningAgentProvider provider = getAgentProvider();
		agent = provider.createAgent(tempFolder.toURI());
		profileRegistry = ((IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME));
		assertNotNull(profileRegistry.getProfile(profileLoadedId));
	}

	IInstallableUnit getIU(IMetadataRepository source, String id, String version) {
		IQueryResult c = repo.query(new InstallableUnitQuery(id, Version.create(version)), new NullProgressMonitor());
		assertEquals(1, queryResultSize(c));
		return (IInstallableUnit) c.iterator().next();
	}

	public void testInstall() {
		IMetadataRepositoryManager mgr = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		IMetadataRepository repo = null;
		try {
			repo = mgr.loadRepository(getTestData("test data bug 300104bis repo", "testData/bug300104bis/repo").toURI(), null);
		} catch (ProvisionException e) {
			assertNull(e); //This guarantees that the error does not go unoticed
		}
		IQueryResult<IInstallableUnit> ius = repo.query(InstallableUnitQuery.ANY, null);
		createRequest(ius);

		//		com.dcns.rsm.profile.equipment_1.0.3.v20090513.jar
		//		com.dcns.rsm.rda_5.1.0.v20100106.jar
		//		com.dcns.rsm.profile.equipment_1.0.4.v20090831.jar
		//		com.dcns.rsm.profile.equipment_1.2.1.v20100106.jar
		//		com.dcns.rsm.profile.gemo_3.2.1.v20090206.jar
		//		com.dcns.rsm.profile.gemo_3.3.0.v20090429.jar
		//		com.dcns.rsm.profile.gemo_3.4.0.v20090831.jar
		//		com.dcns.rsm.profile.gemo_3.5.0.v20091030.jar
		//		com.dcns.rsm.profile.gemo_3.7.1.v20100106.jar
		//		com.dcns.rsm.profile.system_3.1.1.v20090205.jar
		//		com.dcns.rsm.profile.system_3.2.0.v20090430.jar
		//		com.dcns.rsm.profile.system_3.3.0.v20090831.jar
		//		com.dcns.rsm.profile.system_4.0.0.v20091030.jar
		//		com.dcns.rsm.profile.system_4.2.1.v20100106.jar
	}

	private ProfileChangeRequest createRequest(IQueryResult<IInstallableUnit> ius) {
		ProfileChangeRequest pcr = new ProfileChangeRequest(profileRegistry.getProfile(profileLoadedId));
		pcr.addInstallableUnits(ius);
		Iterator it = ius.iterator();
		while (it.hasNext()) {
			IInstallableUnit iu = (IInstallableUnit) it.next();
			pcr.setInstallableUnitInclusionRules(iu, PlannerHelper.createOptionalInclusionRule(iu));
		}
		return pcr;
	}
}
