/*******************************************************************************
 * Copyright (c) 2011, 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tobias Oberlies (SAP AG) - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests;

import java.io.File;
import java.io.IOException;
import org.eclipse.equinox.p2.core.*;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.*;

/**
 * Provides {@link IProvisioningAgent} instances for tests. Use as JUnit 4 {@link Rule}.
 */
public final class TestAgentProvider extends ExternalResource {
	private final TemporaryFolder tempManager;
	private IProvisioningAgent agent;

	public TestAgentProvider() {
		this.tempManager = new TemporaryFolder();
	}

	@Override
	protected void before() throws Throwable {
		tempManager.create();
	}

	@Override
	protected void after() {
		if (agent != null) {
			agent.stop();
		}
		tempManager.delete();
	}

	/**
	 * Returns an instance of an {@link IProvisioningAgent}. If this class is used as a JUnit method
	 * {@link Rule}, a separate instance is returned per test method.
	 */
	public IProvisioningAgent getAgent() throws ProvisionException {
		if (agent == null) {
			try {
				agent = createProvisioningAgent(tempManager.newFolder("p2agent"));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return agent;
	}

	private IProvisioningAgent createProvisioningAgent(File location) throws ProvisionException {
		BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
		ServiceReference<IProvisioningAgentProvider> serviceReference = bundleContext.getServiceReference(IProvisioningAgentProvider.class);
		IProvisioningAgentProvider agentFactory = bundleContext.getService(serviceReference);
		try {
			return agentFactory.createAgent(location.toURI());
		} finally {
			bundleContext.ungetService(serviceReference);
		}
	}

	/**
	 * Returns a service from the current agent.
	 * 
	 * @see #getAgent()
	 */
	public <T> T getService(Class<T> type) throws ProvisionException {
		return type.cast(getAgent().getService(type.getName()));
	}

}
