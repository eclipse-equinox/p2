/*******************************************************************************
 *  Copyright (c) 2010 Sonatype and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Sonatype - initial API and implementation
 *     IBM - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.metadata.repository;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.*;

public class RepositorManagerDifferentAgentTest extends AbstractProvisioningTest {

	public void testLoadRepo() throws ProvisionException, IOException {
		IProvisioningAgentProvider agentProvider = (IProvisioningAgentProvider) ServiceHelper.getService(TestActivator.getContext(), IProvisioningAgentProvider.SERVICE_NAME);
		File testLocation = TestData.getFile("ProfilePreferencesTest", "DifferentAgent");
		IProvisioningAgent agent = agentProvider.createAgent(testLocation.toURI());
		IMetadataRepositoryManager mgr = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		URI[] repositories = mgr.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		//HERE  WE SHOULD GET MUCH MORE REPOSITORIES THAN THIS. 
		System.out.println(repositories);
		fail();
	}
}