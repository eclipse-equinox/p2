/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.engine;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.util.NLS;

/**
 * The purpose of this class is to enable cross process locking.
 * See 257654 for more details.
 */
final class ProfileLock {
	private static final String LOCK_FILENAME = ".lock"; //$NON-NLS-1$

	private final Location location;
	private Thread lockHolder;
	private int lockedCount;

	protected ProfileLock(File profileDirectory) {
		location = createLockLocation(profileDirectory);
	}

	private static Location createLockLocation(File parent) {
		Location anyLoc = (Location) ServiceHelper.getService(EngineActivator.getContext(), Location.class.getName());
		try {
			final URL url = parent.toURL();
			Location location = anyLoc.createLocation(null, url, false);
			location.set(url, false, LOCK_FILENAME);
			return location;
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(NLS.bind(Messages.SimpleProfileRegistry_Bad_profile_location, e.getLocalizedMessage()));
		} catch (IllegalStateException e) {
			throw e;
		} catch (IOException e) {
			throw new IllegalStateException(e.getLocalizedMessage());
		}
	}

	protected synchronized void checkLocked() {
		Thread current = Thread.currentThread();
		if (lockHolder != current)
			throw new IllegalStateException(Messages.thread_not_owner);
		try {
			if (!location.isLocked())
				throw new IllegalStateException(Messages.SimpleProfileRegistry_Profile_not_locked);
		} catch (IOException e) {
			throw new IllegalStateException(NLS.bind(Messages.SimpleProfileRegistry_Profile_not_locked_due_to_exception, e.getLocalizedMessage()));
		}
	}

	protected boolean lock() {
		Thread current = Thread.currentThread();
		try {
			if (lockHolder == null && location.lock())
				lockHolder = current;

			if (lockHolder != current)
				return false;
		} catch (IOException e) {
			throw new IllegalStateException(NLS.bind(Messages.SimpleProfileRegistry_Profile_not_locked_due_to_exception, e.getLocalizedMessage()));
		}
		lockedCount++;
		return true;
	}

	protected void unlock() {
		Thread current = Thread.currentThread();
		if (lockHolder != current)
			throw new IllegalStateException(Messages.SimpleProfileRegistry_Profile_not_locked);

		lockedCount--;
		if (lockedCount == 0) {
			lockHolder = null;
			location.release();
		}
	}
}