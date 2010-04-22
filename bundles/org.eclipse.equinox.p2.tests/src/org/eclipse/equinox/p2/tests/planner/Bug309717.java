/*******************************************************************************
 *  Copyright (c) 2010 Sonatype, Inc and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Sonatype, Inc. - initial API and implementation
 *     IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.planner.IProfileChangeRequest;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;

public class Bug309717 extends AbstractProvisioningTest {

	public void testUpdate() throws ProvisionException, OperationCanceledException, URISyntaxException {
		IProvisioningAgent agent = getAgentProvider().createAgent(getTestData("test data bug309717", "testData/bug309717/p2").toURI());

		IMetadataRepository repo1 = ((IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME)).loadRepository(new URI("http://download.eclipse.org/releases/helios"), null);
		// assertFalse(repo1.query(QueryUtil.createIUQuery("org.eclipse.rap.jface.databinding"), new NullProgressMonitor()).isEmpty());
		assertNotNull(repo1);

		URI jazz = getTestData("repo for bug309717", "testData/bug309717/repo/jazz").toURI();
		IMetadataRepository repo2 = ((IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME)).loadRepository(jazz, null);
		assertNotNull(repo2);
		URI jdojo = getTestData("repo for bug309717", "testData/bug309717/repo/jdojo").toURI();
		IMetadataRepository repo3 = ((IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME)).loadRepository(jdojo, null);
		assertNotNull(repo3);
		IMetadataRepository repo4 = ((IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME)).loadRepository(new URI("http://download.eclipse.org/eclipse/updates/3.6"), null);
		// assertFalse(repo1.query(QueryUtil.createIUQuery("org.eclipse.rap.jface.databinding"), new NullProgressMonitor()).isEmpty());
		assertNotNull(repo4);
		IPlanner planner = getPlanner(agent);
		IProfile profile = ((IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME)).getProfile("PlatformProfile");
		IProfileChangeRequest request = planner.createChangeRequest(profile);
		assertNotNull(request);
		//		Set<IInstallableUnit> ius = repo2.query(QueryUtil.createIUQuery("org.eclipse.riena.toolbox.feature.feature.group", Version.create("2.0.0.201003181312")), new NullProgressMonitor()).toUnmodifiableSet();
		//		request.addAll(ius);
		//		ProvisioningContext ctx = new ProvisioningContext(getAgent());
		//		ctx.setMetadataRepositories(new URI[] {new URI("http://download.eclipse.org/releases/helios"), rienaRepo});
		//		IProvisioningPlan plan = planner.getProvisioningPlan(request, ctx, new NullProgressMonitor());
		//
		//		assertOK("resolution failed", plan.getStatus());
		//		assertEquals(0, plan.getAdditions().query(QueryUtil.createIUQuery("org.eclipse.rap.jface.databinding"), new NullProgressMonitor()).toUnmodifiableSet().size());
		//		System.out.println(plan);
	}
}
