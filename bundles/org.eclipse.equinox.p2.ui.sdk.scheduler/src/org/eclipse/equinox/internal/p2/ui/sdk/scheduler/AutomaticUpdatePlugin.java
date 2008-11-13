/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.scheduler;

import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.updatechecker.IUpdateChecker;
import org.eclipse.equinox.internal.provisional.p2.updatechecker.UpdateChecker;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Activator class for the automatic updates plugin
 */
public class AutomaticUpdatePlugin extends AbstractUIPlugin {

	private static AutomaticUpdatePlugin plugin;
	private static BundleContext context;
	private static PackageAdmin packageAdmin = null;
	private static ServiceReference packageAdminRef = null;

	private AutomaticUpdateScheduler scheduler;
	private AutomaticUpdater updater;
	private ServiceRegistration registrationChecker;

	public static final String PLUGIN_ID = "org.eclipse.equinox.p2.ui.sdk.scheduler"; //$NON-NLS-1$

	public static BundleContext getContext() {
		return context;
	}
	
	public static Bundle getBundle(String symbolicName) {
		if (packageAdmin == null)
			return null;
		Bundle[] bundles = packageAdmin.getBundles(symbolicName, null);
		if (bundles == null)
			return null;
		// Return the first bundle that is not installed or uninstalled
		for (int i = 0; i < bundles.length; i++) {
			if ((bundles[i].getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
				return bundles[i];
			}
		}
		return null;
	}

	/**
	 * Returns the singleton plugin instance
	 * 
	 * @return the instance
	 */
	public static AutomaticUpdatePlugin getDefault() {
		return plugin;
	}

	public AutomaticUpdatePlugin() {
		// constructor
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
		plugin = this;
		context = bundleContext;
		packageAdminRef = bundleContext.getServiceReference(PackageAdmin.class.getName());
		packageAdmin = (PackageAdmin) bundleContext.getService(packageAdminRef);

		// TODO for now we need to manually start up the provisioning infrastructure
		// and the update checker, because the Eclipse Application launch config won't 
		// let me specify bundles to start.
		getBundle("org.eclipse.equinox.p2.exemplarysetup").start(Bundle.START_TRANSIENT); //$NON-NLS-1$
		getBundle("org.eclipse.equinox.frameworkadmin.equinox").start(Bundle.START_TRANSIENT); //$NON-NLS-1$
		getBundle("org.eclipse.equinox.simpleconfigurator.manipulator").start(Bundle.START_TRANSIENT); //$NON-NLS-1$
		getBundle("org.eclipse.equinox.p2.updatechecker").start(Bundle.START_TRANSIENT); //$NON-NLS-1$

		registrationChecker = context.registerService(IUpdateChecker.SERVICE_NAME, new UpdateChecker(), null);
	}

	public void stop(BundleContext bundleContext) throws Exception {
		if (scheduler != null) {
			scheduler.shutdown();
			scheduler = null;
		}
		if (updater != null) {
			updater.shutdown();
			updater = null;
		}
		registrationChecker.unregister();
		registrationChecker = null;
		packageAdmin = null;
		packageAdminRef = null;
		plugin = null;
		super.stop(bundleContext);
		context = null;
	}

	public AutomaticUpdateScheduler getScheduler() {
		// If the scheduler was disabled, it does not get initialized
		if (scheduler == null)
			scheduler = new AutomaticUpdateScheduler();
		return scheduler;
	}

	public AutomaticUpdater getAutomaticUpdater() {
		if (updater == null)
			updater = new AutomaticUpdater();
		return updater;
	}

	void setScheduler(AutomaticUpdateScheduler scheduler) {
		this.scheduler = scheduler;
	}

	public IProvisioningEventBus getProvisioningEventBus() {
		ServiceReference busReference = context.getServiceReference(IProvisioningEventBus.SERVICE_NAME);
		if (busReference == null)
			return null;
		return (IProvisioningEventBus) context.getService(busReference);
	}
}
