/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.extensionlocation;

import java.io.File;
import java.net.URI;
import java.net.URL;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.core.helpers.URLUtil;
import org.eclipse.equinox.internal.p2.touchpoint.eclipse.Util;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.*;

public class Activator implements BundleActivator {

	public static final String ID = "org.eclipse.equinox.p2.extensionlocation"; //$NON-NLS-1$null;
	private static volatile BundleContext bundleContext;

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		bundleContext = context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		bundleContext = null;
	}

	public static BundleContext getContext() {
		return bundleContext;
	}

	/*
	 * Helper method to get the configuration location. Return null if
	 * it is unavailable.
	 */
	public static File getConfigurationLocation() {
		Location configurationLocation = ServiceHelper.getService(getContext(), Location.class, Location.CONFIGURATION_FILTER);
		if (configurationLocation == null || !configurationLocation.isSet())
			return null;
		URL url = configurationLocation.getURL();
		if (url == null)
			return null;
		return URLUtil.toFile(url);
	}

	public static IProfile getCurrentProfile() {
		IProfileRegistry profileRegistry = (IProfileRegistry) getCurrentAgent().getService(IProfileRegistry.SERVICE_NAME);
		return profileRegistry.getProfile(IProfileRegistry.SELF);
	}

	public static IProvisioningAgent getCurrentAgent() {
		ServiceReference<IProvisioningAgent> reference = bundleContext.getServiceReference(IProvisioningAgent.class);
		if (reference == null)
			return null;
		return bundleContext.getService(reference);
	}

	public static IFileArtifactRepository getBundlePoolRepository() {
		IProvisioningAgent agent = getCurrentAgent();
		if (agent == null)
			return null;

		IProfile profile = getCurrentProfile();
		if (profile == null)
			return null;

		return Util.getAggregatedBundleRepository(agent, profile, Util.AGGREGATE_CACHE | Util.AGGREGATE_SHARED_CACHE);
	}

	/**
	 * Returns a reasonable human-readable repository name for the given location.
	 */
	public static String getRepositoryName(URI location) {
		File file = URIUtil.toFile(location);
		return file == null ? location.toString() : file.getAbsolutePath();
	}
}
