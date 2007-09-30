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

package org.eclipse.equinox.prov.ui.internal;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

/**
 * ProvisioningPropertyManager can notify clients of changes to the properties
 * of provisioning objects.
 * 
 * @since 3.4
 */

// TODO This is a HACK class.
// This class should go away and instead these kinds of events should be handled
// by the provisioning event bus.  See bug #197052 and #197701
// 
// TODO Some of these aren't even truly property changes but rather just notification
// that something happened.  Since this class is (hoped to be) temporary, I did not 
// want to define new event types and just used what was already available.
public class ProvisioningPropertyManager {

	private ListenerList listeners = new ListenerList();

	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		listeners.add(listener);
	}

	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		listeners.remove(listener);
	}

	public void notifyListeners(PropertyChangeEvent event) {
		final Object[] listenerArray = listeners.getListeners();
		for (int i = 0; i < listenerArray.length; i++) {
			final IPropertyChangeListener listener = (IPropertyChangeListener) listenerArray[i];
			listener.propertyChange(event);
		}

	}
}
