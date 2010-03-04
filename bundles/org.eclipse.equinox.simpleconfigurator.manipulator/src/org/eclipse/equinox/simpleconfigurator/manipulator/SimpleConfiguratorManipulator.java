/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.simpleconfigurator.manipulator;

import java.io.*;
import java.net.URI;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.osgi.framework.BundleContext;

/**
 * A ConfiguratorManipulator Service used to read and write bundles.info configuration files
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface SimpleConfiguratorManipulator {
	/**
	 * A common location to find the bundle configuration file
	 */
	public static final String BUNDLES_INFO_PATH = "org.eclipse.equinox.simpleconfigurator/bundles.info"; //$NON-NLS-1$

	/**
	 * A common location to find the source configuration file
	 */
	public static final String SOURCE_INFO_PATH = "org.eclipse.equinox.source/source.info"; //$NON-NLS-1$

	/**
	 * Pass this to {@link #loadConfiguration(BundleContext, String)} to read the default source configuration file
	 */
	public static final String SOURCE_INFO = new String("source.info"); //$NON-NLS-1$

	/**
	 * An instance of an ISimpleConfiguratorManipulator is registered as a ConfiguratorManipulator 
	 * service with ConfiguratorManipulator.SERVICE_PROP_KEY_CONFIGURATOR_BUNDLESYMBOLICNAME =
	 * SERVICE_PROP_VALUE_CONFIGURATOR_SYMBOLICNAME.
	 */
	public static final String SERVICE_PROP_VALUE_CONFIGURATOR_SYMBOLICNAME = "org.eclipse.equinox.simpleconfigurator"; //$NON-NLS-1$

	/**
	 * Load the configuration file for the currently running system.
	 * <p>
	 * Pass null for bundleInfoPath to read the bundle configuration file for the running system as 
	 * specified by the "org.eclipse.equinox.simpleconfigurator.configUrl" system property.
	 * </p>
	 * <p>If bundleInfoPath == {@link #SOURCE_INFO} the default source configuration file for the running
	 * system will be loaded.
	 * </p>
	 * @param context - the BundleContext for the running system
	 * @param bundleInfoPath - pass null or a path to the bundle info file to read, relative to the configuration location.  Pass {@link #SOURCE_INFO}
	 * to load the default source configuration.  Common locations are {@link #BUNDLES_INFO_PATH} and {@link #SOURCE_INFO_PATH}.
	 * @return The loaded configuration.  Bundles will have at least symbolic name, version and location information.
	 * @throws IOException
	 */
	public BundleInfo[] loadConfiguration(BundleContext context, String bundleInfoPath) throws IOException;

	/**
	 *	Load the configuration from the given input stream
	 *  The supplied input stream is consumed by this method and will be closed.
	 *  
	 * @param configuration - the input stream to read the configuration from.  The stream will be closed even if an exception is thrown
	 * @param installArea - Relative URIs from the configuration file will be resolved relative to here
	 * @return The loaded configuration.   Bundles will have at least symbolic name, version and location information.
	 * @throws IOException
	 */
	public BundleInfo[] loadConfiguration(InputStream configuration, URI installArea) throws IOException;

	/**
	 * Save the configuration to the given output stream
	 * The output stream is flushed and left open.
	 * To be persisted, bundles are required to have at least a symbolic name, version and location.
	 * 
	 * @param configuration - bundle information to save
	 * @param outputStream - the output stream to write to.  Stream is not closed
	 * @param installArea - bundle locations are written as relative to this URI
	 * @throws IOException
	 */
	public void saveConfiguration(BundleInfo[] configuration, OutputStream outputStream, URI installArea) throws IOException;

	/**
	 * Save the configuration to the specified file
	 * To be persisted, bundles are required to have at least a symbolic name, version and location.
	 * 
	 * @param configuration - bundle information to save
	 * @param configurationFile - file to save the configuration in
	 * @param installArea - bundle locations are written as relative to this URI
	 * 
	 * @throws IOException
	 */
	public void saveConfiguration(BundleInfo[] configuration, File configurationFile, URI installArea) throws IOException;
}
