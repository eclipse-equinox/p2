/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.simpleconfigurator.manipulator;

import java.util.Dictionary;
import java.util.Hashtable;
import org.eclipse.equinox.internal.provisional.configuratormanipulator.ConfiguratorManipulator;
import org.eclipse.equinox.internal.provisional.simpleconfigurator.manipulator.SimpleConfiguratorManipulator;
import org.osgi.framework.*;

public class Activator implements BundleActivator {
	final static boolean DEBUG = true;
	private static volatile BundleContext context;
	private ServiceRegistration registration;

	static BundleContext getContext() {
		return context;
	}

	private void registerConfiguratorManipulator() {
		Dictionary props = new Hashtable();
		props.put(ConfiguratorManipulator.SERVICE_PROP_KEY_CONFIGURATOR_BUNDLESYMBOLICNAME, SimpleConfiguratorManipulator.SERVICE_PROP_VALUE_CONFIGURATOR_SYMBOLICNAME);
		props.put(Constants.SERVICE_VENDOR, "Eclipse.org"); //$NON-NLS-1$
		SimpleConfiguratorManipulatorImpl manipulator = new SimpleConfiguratorManipulatorImpl();
		String[] clazzes = new String[] {ConfiguratorManipulator.class.getName(), SimpleConfiguratorManipulator.class.getName()};
		registration = context.registerService(clazzes, manipulator, props);
	}

	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		registerConfiguratorManipulator();
	}

	public void stop(BundleContext bundleContext) throws Exception {
		if (registration != null)
			registration.unregister();
		Activator.context = null;
	}
}
