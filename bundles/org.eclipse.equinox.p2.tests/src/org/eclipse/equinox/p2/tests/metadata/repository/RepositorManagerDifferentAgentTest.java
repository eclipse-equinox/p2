/*******************************************************************************
 *  Copyright (c) 2010, 2015 Sonatype and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Sonatype - initial API and implementation
 *     IBM - ongoing development
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata.repository;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.eclipse.equinox.p2.tests.TestData;

public class RepositorManagerDifferentAgentTest extends AbstractProvisioningTest {

	public void testLoadRepo() throws ProvisionException, IOException {
		IProvisioningAgentProvider agentProvider = ServiceHelper.getService(TestActivator.getContext(), IProvisioningAgentProvider.class);
		File testLocation = TestData.getFile("ProfilePreferencesTest", "DifferentAgent");
		IProvisioningAgent agent = agentProvider.createAgent(testLocation.toURI());
		IMetadataRepositoryManager mgr = agent.getService(IMetadataRepositoryManager.class);
		URI[] repositories = mgr.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		//HERE  WE SHOULD GET MUCH MORE REPOSITORIES THAN THIS.
		System.out.println(repositories);
		fail();
	}
}