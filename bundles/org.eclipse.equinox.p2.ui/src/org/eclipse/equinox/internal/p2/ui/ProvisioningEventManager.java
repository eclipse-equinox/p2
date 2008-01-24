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

package org.eclipse.equinox.internal.p2.ui;

import java.util.EventObject;
import org.eclipse.core.runtime.ListenerList;

/**
 * ProvisioningEventManager can notify clients of changes to 
 * provisioning objects.  It is used for events that are not provided
 * by the underlying event bus.
 * 
 * @since 3.4
 */

public class ProvisioningEventManager {

	private ListenerList listeners = new ListenerList();

	public void addListener(IProvisioningListener listener) {
		listeners.add(listener);
	}

	public void removeListener(IProvisioningListener listener) {
		listeners.remove(listener);
	}

	public void notifyListeners(EventObject event) {
		final Object[] listenerArray = listeners.getListeners();
		for (int i = 0; i < listenerArray.length; i++) {
			final IProvisioningListener listener = (IProvisioningListener) listenerArray[i];
			listener.notify(event);
		}

	}
}
