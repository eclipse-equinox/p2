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
package org.eclipse.equinox.frameworkadmin.equinox.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.equinox.frameworkadmin.FrameworkAdmin;
import org.eclipse.equinox.frameworkadmin.Manipulator;
import org.osgi.framework.*;

/**
 * This bundle provides the {@link FrameworkAdmin} implementation for Felix.
 * 
 * This bundle registers {@link Manipulator} object with these service property values.
 *  
 *  FW_NAME = "Equinox";
 * 	FW_VERSION = "3.3M5";
 *	LAUCNHER_NAME = "Eclipse.exe";
 *  LAUNCHER_VERSION = "3.2";
 * 
 * The launching by the eclipse launcher is supported.
 * 
 * Handling plugins in non Jar format is not supported.
 * 
 * FwBundleState supports retrieving fw persistent data
 *  and  resolving bundles if running on equinox.
 * FwBundleState Does NOT support retrieving start Levels from fw persistent data location/
 *   
 */
public class Activator implements BundleActivator {
	private BundleContext context;

	private ServiceRegistration registrationFA;

	EquinoxFwAdminImpl fwAdmin = null;

	private void registerFwAdmin() {
		Dictionary props = new Hashtable();
		props.put(Constants.SERVICE_VENDOR, "Eclipse.org");

		props.put(FrameworkAdmin.SERVICE_PROP_KEY_FW_NAME, EquinoxConstants.FW_NAME);
		props.put(FrameworkAdmin.SERVICE_PROP_KEY_FW_VERSION, EquinoxConstants.FW_VERSION);
		props.put(FrameworkAdmin.SERVICE_PROP_KEY_LAUNCHER_NAME, EquinoxConstants.LAUNCHER_NAME);
		props.put(FrameworkAdmin.SERVICE_PROP_KEY_LAUNCHER_VERSION, EquinoxConstants.LAUNCHER_VERSION);

		if (!EquinoxFwAdminImpl.isRunningFw(context)) {
			props.put(FrameworkAdmin.SERVICE_PROP_KEY_RUNNING_SYSTEM_FLAG, "true");
			fwAdmin = new EquinoxFwAdminImpl(context, true);
		} else
			fwAdmin = new EquinoxFwAdminImpl(context);

		registrationFA = context.registerService(FrameworkAdmin.class.getName(), fwAdmin, props);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		this.context = context;
		Log.init(context);
		registerFwAdmin();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		this.context = null;
		if (registrationFA != null)
			registrationFA.unregister();
		if (fwAdmin != null)
			fwAdmin.deactivate();

	}

}
