/*******************************************************************************
 *  Copyright (c) 2010, 2026 Sonatype, Inc and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Sonatype, Inc. - initial API and implementation
 *     IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import java.io.IOException;
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

	public void testUpdate() throws ProvisionException, OperationCanceledException, URISyntaxException, IOException {
		IProvisioningAgent agent = getAgentProvider().createAgent(getTestData("test data bug309717", "testData/bug309717/p2").toURI());

		IMetadataRepository repo1 = agent.getService(IMetadataRepositoryManager.class)
				.loadRepository(new URI("https://download.eclipse.org/releases/2021-09"), null);
		// assertFalse(repo1.query(QueryUtil.createIUQuery("org.eclipse.rap.jface.databinding"), new NullProgressMonitor()).isEmpty());
		assertNotNull(repo1);

		URI jazz = getTestData("repo for bug309717", "testData/bug309717/repo/jazz").toURI();
		IMetadataRepository repo2 = agent.getService(IMetadataRepositoryManager.class).loadRepository(jazz, null);
		assertNotNull(repo2);
		URI jdojo = getTestData("repo for bug309717", "testData/bug309717/repo/jdojo").toURI();
		IMetadataRepository repo3 = agent.getService(IMetadataRepositoryManager.class).loadRepository(jdojo, null);
		assertNotNull(repo3);
		IMetadataRepository repo4 = agent.getService(IMetadataRepositoryManager.class)
				.loadRepository(new URI("https://download.eclipse.org/eclipse/updates/4.21"), null);
		// assertFalse(repo1.query(QueryUtil.createIUQuery("org.eclipse.rap.jface.databinding"), new NullProgressMonitor()).isEmpty());
		assertNotNull(repo4);
		IPlanner planner = getPlanner(agent);
		IProfile profile = agent.getService(IProfileRegistry.class).getProfile("PlatformProfile");
		IProfileChangeRequest request = planner.createChangeRequest(profile);
		assertNotNull(request);
	}
}
