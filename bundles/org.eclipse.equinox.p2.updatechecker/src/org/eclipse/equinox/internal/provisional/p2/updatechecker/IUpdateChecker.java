/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.updatechecker;

import org.eclipse.equinox.internal.provisional.p2.query.Query;

/**
 * An UpdateChecker periodically polls for updates to specified profiles and
 * informs listeners if updates are available.  Listeners may then determine
 * whether to retrieve the updates, inform the user, etc.
 */
public interface IUpdateChecker {
	public static final String SERVICE_NAME = IUpdateChecker.class.getName();
	public static long ONE_TIME_CHECK = -1L;

	public abstract void addUpdateCheck(String profileId, Query iusToCheckQuery, long delay, long poll, IUpdateListener listener);

	public abstract void removeUpdateCheck(IUpdateListener listener);

}