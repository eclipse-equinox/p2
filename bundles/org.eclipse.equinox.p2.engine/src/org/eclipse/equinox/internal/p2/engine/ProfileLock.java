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
public class ProfileLock {
	private static final String LOCK_FILENAME = ".lock"; //$NON-NLS-1$

	private final Location location;
	private final Object lock;
	private Thread lockHolder;
	private int waiting;

	public ProfileLock(Object lock, File profileDirectory) {
		this.lock = lock;
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

	public void checkLocked() {
		synchronized (lock) {
			if (lockHolder == null)
				throw new IllegalStateException(Messages.SimpleProfileRegistry_Profile_not_locked);

			Thread current = Thread.currentThread();
			if (lockHolder != current)
				throw new IllegalStateException(Messages.thread_not_owner);
		}
	}

	public boolean lock() {
		synchronized (lock) {
			Thread current = Thread.currentThread();
			if (lockHolder == current)
				return false;

			boolean locationLocked = false;
			while (lockHolder != null) {
				locationLocked = true;
				waiting++;
				boolean interrupted = false;
				try {
					lock.wait();
				} catch (InterruptedException e) {
					interrupted = true;
				} finally {
					waiting--;
					// if interrupted restore interrupt to thread state
					if (interrupted)
						current.interrupt();
				}
			}
			try {
				if (!locationLocked && !location.lock())
					return false;

				lockHolder = current;
			} catch (IOException e) {
				throw new IllegalStateException(NLS.bind(Messages.SimpleProfileRegistry_Profile_not_locked_due_to_exception, e.getLocalizedMessage()));
			}
			return true;
		}
	}

	public void unlock() {
		synchronized (lock) {
			if (lockHolder == null)
				throw new IllegalStateException(Messages.SimpleProfileRegistry_Profile_not_locked);

			Thread current = Thread.currentThread();
			if (lockHolder != current)
				throw new IllegalStateException(Messages.thread_not_owner);

			lockHolder = null;
			if (waiting == 0)
				location.release();
			else
				lock.notify();
		}
	}
}