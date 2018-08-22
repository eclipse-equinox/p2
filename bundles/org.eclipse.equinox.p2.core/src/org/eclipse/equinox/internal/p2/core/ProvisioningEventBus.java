/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.core;

import java.util.EventObject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.*;
import org.eclipse.equinox.p2.core.spi.IAgentService;
import org.eclipse.osgi.framework.eventmgr.*;

/**
 * Default implementation of the {@link IProvisioningEventBus} service.
 */
public class ProvisioningEventBus implements EventDispatcher<ProvisioningListener, ProvisioningListener, EventObject>, IProvisioningEventBus, IAgentService {
	private final CopyOnWriteIdentityMap<ProvisioningListener, ProvisioningListener> syncListeners = new CopyOnWriteIdentityMap<>();
	private final CopyOnWriteIdentityMap<ProvisioningListener, ProvisioningListener> asyncListeners = new CopyOnWriteIdentityMap<>();
	private EventManager eventManager = new EventManager("Provisioning Event Dispatcher"); //$NON-NLS-1$

	private Object dispatchEventLock = new Object();
	/* @GuardedBy("dispatchEventLock") */
	private boolean closed = false;
	/* @GuardedBy("dispatchEventLock") */
	private int dispatchingEvents = 0;

	public ProvisioningEventBus() {
		super();
	}

	@Override
	public void addListener(ProvisioningListener toAdd) {
		if (toAdd instanceof SynchronousProvisioningListener) {
			synchronized (syncListeners) {
				syncListeners.put(toAdd, toAdd);
			}
		} else {
			synchronized (asyncListeners) {
				asyncListeners.put(toAdd, toAdd);
			}
		}
	}

	@Override
	public void removeListener(ProvisioningListener toRemove) {
		if (toRemove instanceof SynchronousProvisioningListener) {
			synchronized (syncListeners) {
				syncListeners.remove(toRemove);
			}
		} else {
			synchronized (asyncListeners) {
				asyncListeners.remove(toRemove);
			}
		}
	}

	@Override
	public void publishEvent(EventObject event) {
		synchronized (dispatchEventLock) {
			if (closed)
				return;
		}
		/* queue to hold set of listeners */
		ListenerQueue<ProvisioningListener, ProvisioningListener, EventObject> listeners = new ListenerQueue<>(eventManager);

		/* synchronize while building the listener list */
		synchronized (syncListeners) {
			/* add set of BundleContexts w/ listeners to queue */
			listeners.queueListeners(syncListeners.entrySet(), this);
			/* synchronously dispatch to populate listeners queue */
			listeners.dispatchEventSynchronous(0, event);
		}

		listeners = new ListenerQueue<>(eventManager);
		synchronized (asyncListeners) {
			listeners.queueListeners(asyncListeners.entrySet(), this);
			synchronized (dispatchEventLock) {
				if (!closed)
					listeners.dispatchEventAsynchronous(0, event);
			}
		}
	}

	@Override
	public void dispatchEvent(ProvisioningListener eventListener, ProvisioningListener listenerObject, int eventAction, EventObject eventObject) {
		synchronized (dispatchEventLock) {
			if (closed)
				return;
			dispatchingEvents++;
		}
		try {
			eventListener.notify(eventObject);
		} catch (Exception e) {
			LogHelper.log(new Status(IStatus.ERROR, Activator.ID, "Exception during event notification", e)); //$NON-NLS-1$
		} finally {
			synchronized (dispatchEventLock) {
				dispatchingEvents--;
				if (dispatchingEvents == 0)
					dispatchEventLock.notifyAll();
			}
		}
	}

	@Override
	public void close() {
		boolean interrupted = false;
		synchronized (dispatchEventLock) {
			eventManager.close();
			closed = true;
			while (dispatchingEvents != 0) {
				try {
					dispatchEventLock.wait(30000); // we're going to cap waiting time at 30s
					break;
				} catch (InterruptedException e) {
					// keep waiting but flag interrupted
					interrupted = true;
				}
			}
		}
		if (interrupted)
			Thread.currentThread().interrupt();
	}

	@Override
	public void start() {
		//nothing to do
	}

	@Override
	public void stop() {
		close();
	}
}
