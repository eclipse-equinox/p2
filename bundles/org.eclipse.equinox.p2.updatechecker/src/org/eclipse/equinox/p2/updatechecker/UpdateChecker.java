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
package org.eclipse.equinox.p2.updatechecker;

import java.text.SimpleDateFormat;
import java.util.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.updatechecker.Activator;
import org.eclipse.equinox.p2.director.IPlanner;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.p2.query.Collector;

/**
 * An UpdateChecker periodically polls for updates to specified profiles and
 * informs listeners if updates are available.  Listeners may then determine
 * whether to retrieve the updates, inform the user, etc.
 * 
 * This implementation is not optimized.  It doesn't optimize for multiple
 * polls on the same profile, nor does it cache any info about a profile from
 * poll to poll.
 *
 * @since 3.4
 */
public class UpdateChecker {
	public static long ONE_TIME_CHECK = -1L;
	public static boolean DEBUG = false;
	public static boolean TRACE = false;
	private HashSet checkers = new HashSet(); // threads
	IProfileRegistry profileRegistry;
	IPlanner planner;

	private class UpdateCheckThread extends Thread {
		boolean done = false;
		long poll, delay;
		IUpdateListener listener;
		String profileId;

		UpdateCheckThread(String profileId, long delay, long poll, IUpdateListener listener) {
			this.poll = poll;
			this.delay = delay;
			this.profileId = profileId;
			this.listener = listener;
		}

		public void run() {
			try {
				if (delay != ONE_TIME_CHECK && delay > 0) {
					Thread.sleep(delay);
				}
				while (!done) {

					log("Checking for updates for " + profileId + " at " + getTimeStamp()); //$NON-NLS-1$ //$NON-NLS-2$
					IInstallableUnit[] iusWithUpdates = checkForUpdates(profileId);
					if (iusWithUpdates.length > 0) {
						log("Notifying listener of available updates"); //$NON-NLS-1$
						UpdateEvent event = new UpdateEvent(profileId, iusWithUpdates);
						if (!done)
							listener.updatesAvailable(event);
					} else {
						log("No updates were available"); //$NON-NLS-1$
					}
					if (delay == ONE_TIME_CHECK || delay <= 0) {
						done = true;
					} else {
						Thread.sleep(poll);
					}
				}
			} catch (InterruptedException e) {
				// nothing
			} catch (Exception e) {
				log("Exception in update check thread", e); //$NON-NLS-1$
			}
		}
	}

	public void addUpdateCheck(String profileId, long delay, long poll, IUpdateListener listener) {
		log("Adding update checker for " + profileId + " at " + getTimeStamp()); //$NON-NLS-1$ //$NON-NLS-2$
		UpdateCheckThread thread = new UpdateCheckThread(profileId, delay, poll, listener);
		checkers.add(thread);
		thread.start();
	}

	public void removeUpdateCheck(IUpdateListener listener) {
		Iterator iter = checkers.iterator();
		while (iter.hasNext()) {
			UpdateCheckThread thread = (UpdateCheckThread) iter.next();
			if (thread.listener == listener) {
				thread.done = true;
				checkers.remove(thread);
				break;
			}
		}
	}

	/*
	 * Return the array of ius in the profile that have updates
	 * available.
	 */
	IInstallableUnit[] checkForUpdates(String profileId) {
		// TODO this is naive.  We get all the ius every time whereas we
		// could monitor changes in the profile.
		IProfile profile = getProfileRegistry().getProfile(profileId);
		ArrayList iusWithUpdates = new ArrayList();
		if (profile == null)
			return new IInstallableUnit[0];
		Iterator iter = profile.query(InstallableUnitQuery.ANY, new Collector(), null).iterator();
		while (iter.hasNext()) {
			IInstallableUnit iu = (IInstallableUnit) iter.next();
			IInstallableUnit[] replacements = getPlanner().updatesFor(iu, null, null);
			if (replacements.length > 0)
				iusWithUpdates.add(iu);
		}
		return (IInstallableUnit[]) iusWithUpdates.toArray(new IInstallableUnit[iusWithUpdates.size()]);
	}

	void log(String string, Throwable e) {
		System.err.println(string + ": " + e); //$NON-NLS-1$
		if (DEBUG)
			e.printStackTrace();
	}

	void log(String string) {
		if (TRACE)
			System.out.println(string);
	}

	String getTimeStamp() {
		Date d = new Date();
		SimpleDateFormat df = new SimpleDateFormat("[MM/dd/yy;HH:mm:ss:SSS]"); //$NON-NLS-1$
		return df.format(d);
	}

	IPlanner getPlanner() {
		if (planner == null) {
			planner = (IPlanner) ServiceHelper.getService(Activator.getContext(), IPlanner.class.getName());
			if (planner == null) {
				throw new IllegalStateException("Provisioning system has not been initialized"); //$NON-NLS-1$
			}
		}
		return planner;
	}

	IProfileRegistry getProfileRegistry() {
		if (profileRegistry == null) {
			profileRegistry = (IProfileRegistry) ServiceHelper.getService(Activator.getContext(), IProfileRegistry.class.getName());
			if (profileRegistry == null) {
				throw new IllegalStateException("Provisioning system has not been initialized"); //$NON-NLS-1$
			}
		}
		return profileRegistry;
	}

}
