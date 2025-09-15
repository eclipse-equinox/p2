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
import java.util.concurrent.CompletableFuture;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.core.spi.IAgentService;
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

	/**
	 * Stop all registered services
	 */
	public void testStopServices() throws ProvisionException {
		URI p2location = getTempFolder().toURI();

		ServiceReference<IProvisioningAgentProvider> providerRef = TestActivator.context.getServiceReference(IProvisioningAgentProvider.class);
		IProvisioningAgentProvider provider = TestActivator.context.getService(providerRef);

		IProvisioningAgent firstAgent = provider.createAgent(p2location);


		MockService mockService1 = new MockService();
		MockService mockService2 = new MockService();

		firstAgent.registerService(IMockService.class.getName(), mockService1);
		assertTrue(mockService1.started);

		firstAgent.registerService(IMockService.class.getName(), mockService2);
		assertTrue(mockService2.started);
		assertFalse(mockService1.started);

		firstAgent.stop();

		assertFalse(mockService1.started);
		assertFalse(mockService2.started);

	}

	/**
	 * Do not expose unstarted services
	 */
	public void testStartServices() throws ProvisionException {
		URI p2location = getTempFolder().toURI();

		ServiceReference<IProvisioningAgentProvider> providerRef = TestActivator.context
				.getServiceReference(IProvisioningAgentProvider.class);
		IProvisioningAgentProvider provider = TestActivator.context.getService(providerRef);

		IProvisioningAgent firstAgent = provider.createAgent(p2location);


		CompletableFuture<Void> startStopServiceTask = CompletableFuture.runAsync(() -> {
			while (!Thread.interrupted()) {
				if (firstAgent.getService(IMockService.class) == null) {
					MockService mockService1 = new MockService();
					firstAgent.registerService(IMockService.class.getName(), mockService1);
				}
			}

		});

		long stop = System.currentTimeMillis() + 1000;
		try {
			while (System.currentTimeMillis() < stop) {
				IMockService service = firstAgent.getService(IMockService.class);
				if (service == null) {
					continue;
				}
				assertTrue(((MockService) service).started);
				firstAgent.unregisterService(IMockService.class.getName(), service);
			}

		} finally {
			startStopServiceTask.cancel(true);
			firstAgent.stop();
		}
	}

	public static interface IMockService extends IAgentService {

	}

	private static class MockService implements IMockService {
		boolean started = false;

		@Override
		public void start() {
			started = true;
		}

		@Override
		public void stop() {
			started = false;
		}
	}
}