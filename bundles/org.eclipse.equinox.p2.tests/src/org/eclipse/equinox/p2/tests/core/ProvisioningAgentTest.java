/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.core;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.equinox.p2.core.*;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.p2.tests.TestActivator;
import org.osgi.framework.ServiceReference;

/**
 * Tests for {@link IProvisioningAgent} and related API.
 */
public class ProvisioningAgentTest extends AbstractProvisioningTest {
	/**
	 * See bug 307151 and bug 304899.
	 * @throws ProvisionException
	 * @throws URISyntaxException
	 */
	public void testMultipleAgents() throws ProvisionException, URISyntaxException {
		URI repoLocation = new URI("http://download.eclipse.org/eclipse/updates/3.6");
		URI p2location = getTempFolder().toURI();
		String PROFILE_ID = "testMultipleAgents";

		ServiceReference<IProvisioningAgentProvider> providerRef = TestActivator.context.getServiceReference(IProvisioningAgentProvider.class);
		IProvisioningAgentProvider provider = TestActivator.context.getService(providerRef);

		IProvisioningAgent firstAgent = provider.createAgent(p2location);
		IProfileRegistry firstProfileRegistry = (IProfileRegistry) firstAgent.getService(IProfileRegistry.SERVICE_NAME);
		firstProfileRegistry.removeProfile(PROFILE_ID);
		firstProfileRegistry.addProfile(PROFILE_ID);
		IMetadataRepositoryManager firstMdrMgr = (IMetadataRepositoryManager) firstAgent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		firstMdrMgr.addRepository(repoLocation);
		firstMdrMgr.setEnabled(repoLocation, false);
		firstAgent.stop();

		IProvisioningAgent secondAgent = provider.createAgent(p2location);
		IProfileRegistry secondProfileRegistry = (IProfileRegistry) secondAgent.getService(IProfileRegistry.SERVICE_NAME);
		secondProfileRegistry.removeProfile(PROFILE_ID);
		secondProfileRegistry.addProfile(PROFILE_ID);
		IMetadataRepositoryManager secondMdrMgr = (IMetadataRepositoryManager) secondAgent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		secondMdrMgr.removeRepository(repoLocation);
		secondAgent.stop();

		TestActivator.context.ungetService(providerRef);

	}
}