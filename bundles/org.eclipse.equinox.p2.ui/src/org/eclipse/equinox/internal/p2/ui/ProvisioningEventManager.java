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

package org.eclipse.equinox.internal.p2.ui;

import java.util.EventObject;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.equinox.p2.ui.IProvisioningListener;

/**
 * ProvisioningEventManager can notify clients of changes to the properties
 * of provisioning objects.
 * 
 * @since 3.4
 */

// TODO This is a HACK class.
// This class should go away and instead these kinds of events should be handled
// by the provisioning event bus.  See bug #197052 and #197701
// 
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
