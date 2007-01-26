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
package org.eclipse.equinox.simpleconfigurator.manipulator.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.equinox.configurator.ConfiguratorManipulator;
import org.eclipse.equinox.internal.simpleconfigurator.utils.SimpleConfiguratorUtils;
import org.osgi.framework.*;

public class Activator implements BundleActivator {
	final static boolean DEBUG = true;
	static private BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	private ServiceRegistration registration;

	SimpleConfiguratorManipulatorImpl manipulator = null;

	private void registerConfiguratorManipulator() {

		Dictionary props = new Hashtable();
		props.put(ConfiguratorManipulator.SERVICE_PROP_KEY_CONFIGURATOR_BUNDLESYMBOLICNAME, SimpleConfiguratorUtils.SERVICE_PROP_VALUE_CONFIGURATOR_SYMBOLICNAME);
		props.put(Constants.SERVICE_VENDOR, "Eclipse.org");
		manipulator = new SimpleConfiguratorManipulatorImpl();
		registration = context.registerService(ConfiguratorManipulator.class.getName(), manipulator, props);

	}

	public void start(BundleContext context) throws Exception {
		Activator.context = context;

		this.registerConfiguratorManipulator();
	}

	public void stop(BundleContext context) throws Exception {
		if (registration != null) //{
			registration.unregister();
		Activator.context = null;
	}

}
