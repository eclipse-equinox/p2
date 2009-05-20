/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.simpleconfigurator.utils;

public class SimpleConfiguratorConstants {
	/**
	 * If BundleContext#getProperty(PROP_KEY_EXCLUSIVE_INSTALLATION) equals "true" ignoring case, 
	 * Configurator.applyConfiguration(url) will uninstall the installed bundles which are not 
	 * listed in the simpleconfigurator config file after install bundles listed.
	 * Otherwise, it never uninstall any bundles. 
	 * 
	 * Default: true
	 */
	public static final String PROP_KEY_EXCLUSIVE_INSTALLATION = "org.eclipse.equinox.simpleconfigurator.exclusiveInstallation"; //$NON-NLS-1$

	/**
	 * If BundleContext#getProperty(PROP_KEY_USE_REFERENCE) does not equal "false" ignoring case, 
	 * when a SimpleConfigurator installs a bundle, "reference:" is added to its bundle location in order to avoid
	 * caching its bundle jar.  Otherwise, it will add nothing to any bundle location.
	 * 	 
	 * Default: true
	 */
	public static final String PROP_KEY_USE_REFERENCE = "org.eclipse.equinox.simpleconfigurator.useReference"; //$NON-NLS-1$

	/**
	 * BundleContext#getProperty(PROP_KEY_CONFIGURL) is used for SimpleConfigurator to do life cycle control of bundles.
	 * The file specified by the returned url is read by SimpleConfigurator and do life cycle control according to it.
	 * If improper value or null is returned, SimpleConfigurator doesn't do it.
	 * 
	 * Default: null
	 */
	public static final String PROP_KEY_CONFIGURL = "org.eclipse.equinox.simpleconfigurator.configUrl"; //$NON-NLS-1$

	public static final String CONFIG_LIST = "bundles.info"; //$NON-NLS-1$
	public static final String CONFIGURATOR_FOLDER = "org.eclipse.equinox.simpleconfigurator"; //$NON-NLS-1$

	public static final String TARGET_CONFIGURATOR_NAME = "org.eclipse.equinox.simpleconfigurator"; //$NON-NLS-1$

	public static final String PARAMETER_BASEURL = "org.eclipse.equinox.simpleconfigurator.baseUrl"; //$NON-NLS-1$

}
