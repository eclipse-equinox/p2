/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
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
import org.eclipse.osgi.framework.eventmgr.EventDispatcher;

/**
 * The bus for events related to provisioning. This service can be used to register
 * a listener to receive provisioning events, or to broadcast events.
 */
public interface IProvisioningEventBus extends EventDispatcher<ProvisioningListener, ProvisioningListener, EventObject> {
	/**
	 * The name used for obtaining a reference to the event bus service.
	 */
	public static final String SERVICE_NAME = IProvisioningEventBus.class.getName();

	public abstract void addListener(ProvisioningListener toAdd);

	public abstract void removeListener(ProvisioningListener toRemove);

	public abstract void publishEvent(EventObject event);

	/**
	 * Closes the event bus.  This will stop dispatching of any events currently
	 * being processed by the bus. Events published after the bus is closed
	 * are ignored.
	 */
	public abstract void close();

}