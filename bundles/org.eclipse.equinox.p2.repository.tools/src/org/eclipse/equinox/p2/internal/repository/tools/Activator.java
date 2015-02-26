/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	public static final String ID = "org.eclipse.equinox.p2.transformer"; //$NON-NLS-1$
	private static BundleContext bundleContext;

	/*
	 * Return the bundle context or <code>null</code>.
	 */
	public static BundleContext getBundleContext() {
		return bundleContext;
	}

	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		bundleContext = context;
	}

	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		bundleContext = null;
	}

	/*
	 * Construct and return a URI from the given String. Log
	 * and return null if there was a problem.
	 */
	public static URI getURI(String spec) {
		if (spec == null)
			return null;
		try {
			return URIUtil.fromString(spec);
		} catch (URISyntaxException e) {
			LogHelper.log(new Status(IStatus.WARNING, ID, NLS.bind(Messages.unable_to_process_uri, spec), e));
		}
		return null;
	}

	/*
	 * Return the provisioning agent. Throw an exception if it cannot be obtained.
	 */
	public static IProvisioningAgent getAgent() throws ProvisionException {
		IProvisioningAgent agent = ServiceHelper.getService(getBundleContext(), IProvisioningAgent.class);
		if (agent == null)
			throw new ProvisionException(Messages.no_provisioning_agent);
		return agent;
	}

	/*
	 * Return the artifact repository manager. Throw an exception if it cannot be obtained.
	 */
	public static IArtifactRepositoryManager getArtifactRepositoryManager() throws ProvisionException {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) getAgent().getService(IArtifactRepositoryManager.SERVICE_NAME);
		if (manager == null)
			throw new ProvisionException(Messages.no_artifactRepo_manager);
		return manager;
	}

	/*
	 * Return the profile registry. Throw an exception if it cannot be found.
	 */
	static IProfileRegistry getProfileRegistry() throws ProvisionException {
		IProfileRegistry registry = (IProfileRegistry) getAgent().getService(IProfileRegistry.SERVICE_NAME);
		if (registry == null)
			throw new ProvisionException(Messages.no_profile_registry);
		return registry;
	}

	/*
	 * Return the metadata repository manager. Throw an exception if it cannot be obtained.
	 */
	public static IMetadataRepositoryManager getMetadataRepositoryManager() throws ProvisionException {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
		if (manager == null)
			throw new ProvisionException(Messages.no_metadataRepo_manager);
		return manager;
	}
}
