/*******************************************************************************
 * Copyright (c) 2007, 20016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Mikael Barbero (Eclipse Foundation) - Bug 498116
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.updatechecker;

/**
 * An IUpdateListener informs listeners that an update is available for
 * the specified profile.  Listeners should expect to receive this notification
 * from a background thread.
 */
public interface IUpdateListener {

	public void updatesAvailable(UpdateEvent event);

	public void checkingForUpdates();
}
