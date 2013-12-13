/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat, Inc (Krzysztof Daniel) - Bug 421935: Extend simpleconfigurator to
 * read .info files from many locations
 *******************************************************************************/
package org.eclipse.equinox.internal.simpleconfigurator;

import java.util.Dictionary;
import java.util.Hashtable;
import org.eclipse.equinox.internal.provisional.configurator.Configurator;
import org.eclipse.equinox.internal.simpleconfigurator.utils.EquinoxUtils;
import org.eclipse.equinox.internal.simpleconfigurator.utils.SimpleConfiguratorConstants;
import org.osgi.framework.*;

/**
 * At its start, SimpleConfigurator bundle does the followings.
 * 
 * 1. A value will be gotten by @{link BundleContext#getProperty(key)} with 
 * {@link SimpleConfiguratorConstants#PROP_KEY_CONFIGURL} as a key.
 * The value will be used for the referal URL. Under the url, there must be a simple 
 * bundles list file to be installed with thier start level and flag of marked as started.
 * 
 * 2. If the value is null, do nothing any more.
 * 3. Otherwise, retrieve the bundles list from the url and install,
 *  set start level of and start bundles, as specified.
 * 
 * 4. A value will be gotten by @{link BundleContext#getProperty(key)} with 
 * {@link SimpleConfiguratorConstants#PROP_KEY_EXCLUSIVE_INSTALLATION} as a key.
 * 
 * 5. If it equals "false", it will do exclusive installation, which means that 
 * the bundles will not be listed in the specified url but installed at the time
 * of the method call except SystemBundle will be uninstalled. 
 * Otherwise, no uninstallation will not be done.
 * 
 */
public class Activator implements BundleActivator {
	public final static boolean DEBUG = false;

	/**
	 * If this property is set to true, simpleconfigurator will attempt to read 
	 * multiple *.info files.
	 * 
	 * This field is writable only for test purposes.
	 */
	public static String EXTENSIONS = System.getProperty("p2.fragments");
	public static boolean EXTENDED = (EXTENSIONS != null);

	private ServiceRegistration<?> configuratorRegistration;
	private ServiceRegistration<?> commandRegistration;

	public void start(BundleContext context) throws Exception {
		SimpleConfiguratorImpl bundleConfigurator = new SimpleConfiguratorImpl(context, context.getBundle());
		bundleConfigurator.applyConfiguration();

		Dictionary<String, String> props = new Hashtable<String, String>();
		props.put(Constants.SERVICE_VENDOR, "Eclipse"); //$NON-NLS-1$
		props.put(Constants.SERVICE_PID, SimpleConfiguratorConstants.TARGET_CONFIGURATOR_NAME);
		ServiceFactory<?> configurationFactory = new SimpleConfiguratorFactory(context);
		configuratorRegistration = context.registerService(Configurator.class.getName(), configurationFactory, props);

		try {
			if (null != context.getBundle().loadClass("org.eclipse.osgi.framework.console.CommandProvider")) //$NON-NLS-1$
				commandRegistration = EquinoxUtils.registerConsoleCommands(context);
		} catch (ClassNotFoundException e) {
			// CommandProvider is not available
			// Ok -- optional
		}

		if (DEBUG)
			System.out.println("registered Configurator"); //$NON-NLS-1$
	}

	public void stop(BundleContext context) throws Exception {
		if (configuratorRegistration != null) {
			configuratorRegistration.unregister();
			configuratorRegistration = null;
		}
		if (commandRegistration != null) {
			commandRegistration.unregister();
			commandRegistration = null;
		}
	}
}
