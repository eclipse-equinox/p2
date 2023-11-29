/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
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
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.core;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
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
	 */
	public void testMultipleAgents() throws ProvisionException, URISyntaxException {
		URI repoLocation = new URI("https://download.eclipse.org/eclipse/updates/latest");
		URI p2location = getTempFolder().toURI();
		String PROFILE_ID = "testMultipleAgents";

		ServiceReference<IProvisioningAgentProvider> providerRef = TestActivator.context.getServiceReference(IProvisioningAgentProvider.class);
		IProvisioningAgentProvider provider = TestActivator.context.getService(providerRef);

		IProvisioningAgent firstAgent = provider.createAgent(p2location);
		IProfileRegistry firstProfileRegistry = firstAgent.getService(IProfileRegistry.class);
		firstProfileRegistry.removeProfile(PROFILE_ID);
		firstProfileRegistry.addProfile(PROFILE_ID);
		IMetadataRepositoryManager firstMdrMgr = firstAgent.getService(IMetadataRepositoryManager.class);
		firstMdrMgr.addRepository(repoLocation);
		firstMdrMgr.setEnabled(repoLocation, false);
		firstAgent.stop();

		IProvisioningAgent secondAgent = provider.createAgent(p2location);
		IProfileRegistry secondProfileRegistry = secondAgent.getService(IProfileRegistry.class);
		secondProfileRegistry.removeProfile(PROFILE_ID);
		secondProfileRegistry.addProfile(PROFILE_ID);
		IMetadataRepositoryManager secondMdrMgr = secondAgent.getService(IMetadataRepositoryManager.class);
		secondMdrMgr.removeRepository(repoLocation);
		secondAgent.stop();

		TestActivator.context.ungetService(providerRef);

	}
}