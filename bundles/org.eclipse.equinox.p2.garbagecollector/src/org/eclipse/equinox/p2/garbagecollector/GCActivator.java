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
package org.eclipse.equinox.p2.garbagecollector;

import java.util.EventObject;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningEventBus;
import org.eclipse.equinox.p2.core.eventbus.SynchronousProvisioningListener;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.garbagecollector.prefs.GCPreferenceConstants;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.osgi.framework.*;

public class GCActivator implements BundleActivator {
	public static final String ID = "org.eclipse.equinox.p2.garbagecollector"; //$NON-NLS-1$
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
		ProvisioningEventBus eventBus = (ProvisioningEventBus) getService(GCActivator.context, ProvisioningEventBus.class.getName());
		if (eventBus == null) {
			System.err.println(Messages.GCActivator_0); //TODO Clean up.
		}
		eventBus.addListener(busListener = new SynchronousProvisioningListener() {
			//The GC is triggered when an uninstall event occured during a "transaction" and the transaction is committed.   
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
						IPreferencesService prefService = (IPreferencesService) getService(context, IPreferencesService.class.getName());
						if (!(prefService.getBoolean(ID, GCPreferenceConstants.DISABLE_GARBAGE_COLLECT, false, null))) {
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

	private void unregisterGCTrigger() {
		ProvisioningEventBus eventBus = (ProvisioningEventBus) getService(GCActivator.context, ProvisioningEventBus.class.getName());
		if (eventBus != null && busListener != null)
			eventBus.removeListener(busListener);
	}

	public void stop(BundleContext inContext) throws Exception {
		GCActivator.context = null;
		unregisterGCTrigger();
	}

	public static BundleContext getContext() {
		return context;
	}
}
