/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.update.configurator.IPlatformConfiguration;
import org.eclipse.update.configurator.IPlatformConfigurationFactory;
import org.eclipse.update.configurator.IPlatformConfiguration.IFeatureEntry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**	
 * 	This class provides a wrapper on IPlatformConfiguration to support
 * 	installing and uninstalling features in the configuration.
 * 
 * 	Only a minimal set of operations is exposed.
 */
public class PlatformConfigurationWrapper {

	private IPlatformConfiguration configuration;
	private URL configURL;
	private URL poolURL;
	private boolean serviceNotFound = false;

	public PlatformConfigurationWrapper(URL configDir, URL featurePool) {
		this.configuration = null;
		try {
			this.configURL = new URL(configDir, "org.eclipse.update/platform.xml");
		} catch (MalformedURLException mue) {
			this.configURL = configDir;
		}
		this.poolURL = featurePool;
	}

	private void loadDelegate() {
		if (configuration != null || serviceNotFound)
			return;
		// Acquire the configuration factory service first
		BundleContext context = Activator.getContext();
		ServiceReference configFactorySR = null;
		try {
			configFactorySR = context.getServiceReference(IPlatformConfigurationFactory.class.getName());
		} catch (Throwable /*ClassNotFoundException*/cnfe) {
			serviceNotFound = true;
		}
		if (configFactorySR == null) {
			serviceNotFound = true;
			return;
			// throw new IllegalStateException("Could not acquire the platform configuration factory service."); //$NON-NLS-1$
		}

		IPlatformConfigurationFactory configFactory = (IPlatformConfigurationFactory) context.getService(configFactorySR);
		if (configFactory == null)
			throw new IllegalStateException("Platform configuration service returned a null platform configuration factory."); //$NON-NLS-1$
		// Get the configuration using the factory
		try {
			configuration = configFactory.getPlatformConfiguration(configURL);
		} catch (IOException ioe) {
			try {
				configuration = configFactory.getPlatformConfiguration(null);
				IPlatformConfiguration.ISiteEntry site = configuration.createSiteEntry(poolURL, configuration.createSitePolicy(IPlatformConfiguration.ISitePolicy.USER_INCLUDE, new String[0]));
				configuration.configureSite(site);
			} catch (IOException ioe2) {
				configuration = configFactory.getCurrentPlatformConfiguration();
			}
		}
		context.ungetService(configFactorySR);
	}

	// Only expose the operations using IPlatformConfiguration that are needed
	// to install and uninstall features in the configuration.

	/*
	 * @see org.eclipse.update.configurator.IPlatformConfiguration#createFeatureEntry(java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean, java.lang.String, java.net.URL[])
	 */
	public IStatus addFeatureEntry(String id, String version, String pluginIdentifier, String pluginVersion, boolean primary, String application, URL[] root) {
		loadDelegate();
		if (configuration == null)
			return new Status(IStatus.WARNING, Activator.ID, "Platform configuration not available.", null); //$NON-NLS-1$

		IFeatureEntry entry = configuration.createFeatureEntry(id, version, pluginIdentifier, pluginVersion, primary, application, root);
		if (entry != null) {
			configuration.configureFeatureEntry(entry);
			return Status.OK_STATUS;
		}
		return new Status(IStatus.ERROR, Activator.ID, "Creating feature entry returned null.", null); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.update.configurator.IPlatformConfiguration#findConfiguredFeatureEntry(java.lang.String)
	 */
	public IStatus removeFeatureEntry(String id) {
		loadDelegate();
		if (configuration == null)
			return new Status(IStatus.WARNING, Activator.ID, "Platform configuration not available.", null); //$NON-NLS-1$

		IPlatformConfiguration.IFeatureEntry entry = configuration.findConfiguredFeatureEntry(id);
		if (entry != null) {
			configuration.unconfigureFeatureEntry(entry);
			return Status.OK_STATUS;
		}
		return new Status(IStatus.ERROR, Activator.ID, "A feature with the specified id was not found.", null); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.update.configurator.IPlatformConfiguration#save()
	 */
	public void save(URL location) throws IOException {
		if (configuration != null) {
			configuration.save(location);
		}
	}
}
