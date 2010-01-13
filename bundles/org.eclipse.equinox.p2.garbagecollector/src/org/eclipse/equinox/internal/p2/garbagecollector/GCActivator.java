/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.garbagecollector;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.SynchronousProvisioningListener;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.osgi.framework.*;
import org.osgi.service.prefs.Preferences;

public class GCActivator implements BundleActivator {
	public static final String ID = "org.eclipse.equinox.p2.garbagecollector"; //$NON-NLS-1$
	public static final String GC_ENABLED = "gc_enabled"; //$NON-NLS-1$
	private static final String DEBUG_STRING = GCActivator.ID + "/debug"; //$NON-NLS-1$
	private static final boolean DEFAULT_DEBUG = false;

	static BundleContext context;

	private SynchronousProvisioningListener busListener;

	static Object getService(BundleContext ctx, String name) {
		ServiceReference reference = ctx.getServiceReference(name);
		if (reference == null)
			return null;
		Object result = ctx.getService(reference);
		ctx.ungetService(reference);
		return result;
	}

	public void start(BundleContext inContext) throws Exception {
		GCActivator.context = inContext;
		DebugOptions debug = (DebugOptions) getService(inContext, DebugOptions.class.getName());
		if (debug != null) {
			CoreGarbageCollector.setDebugMode(debug.getBooleanOption(DEBUG_STRING, DEFAULT_DEBUG));
		}
		registerGCTrigger();
	}

	//	Register the listener used to trigger the GC.
	private void registerGCTrigger() {
		IProvisioningEventBus eventBus = (IProvisioningEventBus) getService(GCActivator.context, IProvisioningEventBus.SERVICE_NAME);
		if (eventBus == null) {
			LogHelper.log(new Status(IStatus.ERROR, GCActivator.ID, Messages.Missing_bus));
			return;
		}
		eventBus.addListener(busListener = new SynchronousProvisioningListener() {
			//The GC is triggered when an uninstall event occurred during a "transaction" and the transaction is committed.   
			private boolean uninstallEventOccurred = false;

			public void notify(EventObject o) {
				if (o instanceof InstallableUnitEvent) {
					InstallableUnitEvent event = (InstallableUnitEvent) o;
					if (event.isUninstall() && event.isPost()) {
						uninstallEventOccurred = true;
					}
				} else if (o instanceof CommitOperationEvent) {
					if (uninstallEventOccurred == true) {
						CommitOperationEvent event = (CommitOperationEvent) o;
						if (getBooleanPreference(GC_ENABLED, true)) {
							new GarbageCollector().runGC(event.getProfile());
						}
						uninstallEventOccurred = false;
					}
				} else if (o instanceof RollbackOperationEvent) {
					uninstallEventOccurred = false;
				}
			}
		});
	}

	protected boolean getBooleanPreference(String key, boolean defaultValue) {
		IPreferencesService prefService = (IPreferencesService) getService(context, IPreferencesService.class.getName());
		if (prefService == null)
			return defaultValue;
		List<IEclipsePreferences> nodes = new ArrayList<IEclipsePreferences>();
		// todo we should look in the instance scope as well but have to be careful that the instance location has been set
		nodes.add(new ConfigurationScope().getNode(ID));
		nodes.add(new DefaultScope().getNode(ID));
		return Boolean.valueOf(prefService.get(key, Boolean.toString(defaultValue), nodes.toArray(new Preferences[nodes.size()]))).booleanValue();
	}

	private void unregisterGCTrigger() {
		IProvisioningEventBus eventBus = (IProvisioningEventBus) getService(GCActivator.context, IProvisioningEventBus.SERVICE_NAME);
		if (eventBus != null && busListener != null)
			eventBus.removeListener(busListener);
	}

	public void stop(BundleContext inContext) throws Exception {
		unregisterGCTrigger();
		GCActivator.context = null;
	}

	public static BundleContext getContext() {
		return context;
	}
}
