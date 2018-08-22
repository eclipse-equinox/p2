/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
package org.eclipse.equinox.internal.provisional.p2.core.eventbus;

import java.util.EventListener;
import java.util.EventObject;
import org.eclipse.equinox.internal.p2.core.ProvisioningEventBus;

/**
 * A listener that is notified about events related to provisioning.
 * @see ProvisioningEventBus
 */
public interface ProvisioningListener extends EventListener {
	//TODO: rename this interface to match eclipse conventions (IProvisioningListener)

	/**
	 * Notifies the listener about a provisioning event.
	 */
	public void notify(EventObject o);
}
