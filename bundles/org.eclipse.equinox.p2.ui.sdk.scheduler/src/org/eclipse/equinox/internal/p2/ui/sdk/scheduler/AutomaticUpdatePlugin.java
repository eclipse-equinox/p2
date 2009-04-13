/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.scheduler;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

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

	public static final String PLUGIN_ID = "org.eclipse.equinox.p2.ui.sdk.scheduler"; //$NON-NLS-1$
	public static final String PREFERENCE_ROOT = "/profile/_SELF_/"; //$NON-NLS-1$

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
	
	static Preferences getPreferences() {
		return Platform.getPreferencesService().getRootNode().node(PREFERENCE_ROOT + PLUGIN_ID);
	}
	
	static Preferences getDefaultPreferences() {
		return new DefaultScope().getNode(PLUGIN_ID);
	}
	
	static void savePreferences() {
		try {
			getPreferences().flush();
		} catch (BackingStoreException e) {
			ProvUI.handleException(e, AutomaticUpdateMessages.ErrorSavingPreferences, StatusManager.LOG); 
		}
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
		
		PreferenceInitializer.migratePreferences();
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
