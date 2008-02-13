/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.core;

import java.util.EventObject;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.*;
import org.eclipse.osgi.framework.eventmgr.*;

/**
 * Default implementation of the {@link IProvisioningEventBus} service.
 */
public class ProvisioningEventBus implements EventDispatcher, IProvisioningEventBus {
	private EventListeners syncListeners = new EventListeners();
	private EventListeners asyncListeners = new EventListeners();
	private EventManager eventManager = new EventManager("Provisioning Event Dispatcher"); //$NON-NLS-1$

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus#addListener(org.eclipse.equinox.internal.provisional.p2.core.eventbus.ProvisioningListener)
	 */
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

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus#removeListener(org.eclipse.equinox.internal.provisional.p2.core.eventbus.ProvisioningListener)
	 */
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

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus#publishEvent(java.util.EventObject)
	 */
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

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus#dispatchEvent(java.lang.Object, java.lang.Object, int, java.lang.Object)
	 */
	public void dispatchEvent(Object eventListener, Object listenerObject, int eventAction, Object eventObject) {
		try {
			((ProvisioningListener) eventListener).notify((EventObject) eventObject);
		} catch (Exception e) {
			e.printStackTrace();
			//TODO Need to do the appropriate logging
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus#close()
	 */
	public void close() {
		eventManager.close();
	}
}
