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
package org.eclipse.equinox.internal.provisional.p2.core.eventbus;

import java.util.EventObject;
import org.eclipse.osgi.framework.eventmgr.*;

//TODO Need to clean up the lifecycle of this class
public class ProvisioningEventBus implements EventDispatcher {
	private EventListeners syncListeners = new EventListeners();
	private EventListeners asyncListeners = new EventListeners();
	private EventManager eventManager = new EventManager("Provisioning Event Dispatcher"); //$NON-NLS-1$

	public void addListener(ProvisioningListener toAdd) {
		if (toAdd instanceof SynchronousProvisioningListener) {
			synchronized (syncListeners) {
				syncListeners.addListener(toAdd, toAdd);
			}
		} else {
			synchronized (asyncListeners) {
				asyncListeners.addListener(toAdd, toAdd);
			}
		}
	}

	public void removeListener(ProvisioningListener toRemove) {
		if (toRemove instanceof SynchronousProvisioningListener) {
			synchronized (syncListeners) {
				if (syncListeners != null) {
					syncListeners.removeListener(toRemove);
				}
			}
		} else {
			synchronized (asyncListeners) {
				if (asyncListeners != null) {
					asyncListeners.removeListener(toRemove);
				}
			}
		}
	}

	public void publishEvent(EventObject event) {
		/* queue to hold set of listeners */
		ListenerQueue listeners = new ListenerQueue(eventManager);

		/* synchronize while building the listener list */
		synchronized (syncListeners) {
			/* add set of BundleContexts w/ listeners to queue */
			listeners.queueListeners(syncListeners, this);
			/* synchronously dispatch to populate listeners queue */
			listeners.dispatchEventSynchronous(0, event);
		}

		listeners = new ListenerQueue(eventManager);
		synchronized (asyncListeners) {
			listeners.queueListeners(asyncListeners, this);
			listeners.dispatchEventAsynchronous(0, event);
		}
	}

	public void dispatchEvent(Object eventListener, Object listenerObject, int eventAction, Object eventObject) {
		try {
			((ProvisioningListener) eventListener).notify((EventObject) eventObject);
		} catch (Exception e) {
			e.printStackTrace();
			//TODO Need to do the appropriate logging
		}
	}

	public void close() {
		eventManager.close();
	}
}
