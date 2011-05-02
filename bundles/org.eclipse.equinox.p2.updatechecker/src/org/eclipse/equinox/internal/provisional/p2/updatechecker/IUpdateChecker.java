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
package org.eclipse.equinox.internal.provisional.p2.updatechecker;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;

/**
 * An update checker periodically polls for updates to specified profiles and
 * informs listeners if updates are available.  Listeners may then determine
 * whether to retrieve the updates, inform the user, etc.
 */
public interface IUpdateChecker {
	public static final String SERVICE_NAME = IUpdateChecker.class.getName();
	public static long ONE_TIME_CHECK = -1L;

	/**
	 * Adds an update listener that will be notified when updates are available for all
	 * installable units that satisfy the given query. The listener will remain
	 * registered until removed using the {@link #removeUpdateCheck(IUpdateListener)}
	 * method. Adding a listener that is identical to a listener that is already registered
	 * has no effect.
	 * <p>
	 * Once the listener is registered, it will continue to receive notification of updates
	 * based on the specified polling frequency. However, if a delay value of {@link #ONE_TIME_CHECK}
	 * is used, only a single update check will occur for that listener. If this delay value
	 * is used, the specified polling frequency is ignored.
	 * 
	 * @param profileId The profile id to check for updates
	 * @param iusToCheckQuery An installable unit query that matches the units to check for updates
	 * @param delay The delay in milliseconds before the first query should occur, or {@link #ONE_TIME_CHECK}
	 * to indicate that a single update check should occur immediately
	 * @param poll The polling frequency, in milliseconds, between checks for updates
	 * @param listener The listener to be notified of updates
	 * @see #removeUpdateCheck(IUpdateListener)
	 */
	public abstract void addUpdateCheck(String profileId, IQuery<IInstallableUnit> iusToCheckQuery, long delay, long poll, IUpdateListener listener);

	/**
	 * Removes an update listener from the set of listeners registered with this update
	 * checker. If an update check is currently in progress the listener may still receive
	 * events after this method returns. Removing a listener that is not registered has
	 * no effect.
	 * 
	 * @param listener The listener to remove
	 * @see #addUpdateCheck(String, IQuery, long, long, IUpdateListener)
	 */
	public abstract void removeUpdateCheck(IUpdateListener listener);

}