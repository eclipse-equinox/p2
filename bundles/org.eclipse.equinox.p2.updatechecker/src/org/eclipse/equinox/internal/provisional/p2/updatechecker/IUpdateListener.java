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
package org.eclipse.equinox.internal.provisional.p2.updatechecker;

/**
 * An IUpdateListener informs listeners that an update is available for
 * the specified profile.  Listeners should expect to receive this notification
 * from a background thread.
 */
public interface IUpdateListener {

	public void updatesAvailable(UpdateEvent event);
}
