/*******************************************************************************
 *  Copyright (c) 2007, 2016 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 *     Mikael Barbero (Eclipse Foundation) - Bug 498116
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.updatechecker;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.internal.provisional.p2.updatechecker.*;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

/**
 * Default implementation of {@link IUpdateChecker}.
 * <p>
 * This implementation is not optimized.  It doesn't optimize for multiple
 * polls on the same profile, nor does it cache any info about a profile from
 * poll to poll.
 */
public class UpdateChecker implements IUpdateChecker {
	public static boolean DEBUG = false;
	public static boolean TRACE = false;
	/**
	 * Map of IUpdateListener->UpdateCheckThread.
	 */
	private HashMap<IUpdateListener, UpdateCheckThread> checkers = new HashMap<IUpdateListener, UpdateCheckThread>();

	private final IProvisioningAgent agent;
	IProfileRegistry profileRegistry;
	IPlanner planner;

	private class UpdateCheckThread extends Thread {
		boolean done = false;
		long poll, delay;
		IUpdateListener listener;
		String profileId;
		IQuery<IInstallableUnit> query;

		UpdateCheckThread(String profileId, IQuery<IInstallableUnit> query, long delay, long poll, IUpdateListener listener) {
			this.poll = poll;
			this.delay = delay;
			this.profileId = profileId;
			this.query = query;
			this.listener = listener;
		}

		public void run() {
			try {
				if (delay != ONE_TIME_CHECK && delay > 0) {
					Thread.sleep(delay);
				}
				while (!done) {
					listener.checkingForUpdates();
					trace("Checking for updates for " + profileId + " at " + getTimeStamp()); //$NON-NLS-1$ //$NON-NLS-2$
					Collection<IInstallableUnit> iusWithUpdates = checkForUpdates(profileId, query);
					if (iusWithUpdates.size() > 0) {
						trace("Notifying listener of available updates"); //$NON-NLS-1$
						UpdateEvent event = new UpdateEvent(profileId, iusWithUpdates);
						if (!done)
							listener.updatesAvailable(event);
					} else {
						trace("No updates were available"); //$NON-NLS-1$
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
				LogHelper.log(new Status(IStatus.ERROR, UpdateCheckerComponent.BUNDLE_ID, "Exception in update check thread", e)); //$NON-NLS-1$
			}
		}
	}

	public UpdateChecker(IProvisioningAgent agent) {
		this.agent = agent;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.updatechecker.IUpdateChecker#addUpdateCheck(java.lang.String, long, long, org.eclipse.equinox.internal.provisional.p2.updatechecker.IUpdateListener)
	 */
	public void addUpdateCheck(String profileId, IQuery<IInstallableUnit> query, long delay, long poll, IUpdateListener listener) {
		if (checkers.containsKey(listener))
			return;
		trace("Adding update checker for " + profileId + " at " + getTimeStamp()); //$NON-NLS-1$ //$NON-NLS-2$
		UpdateCheckThread thread = new UpdateCheckThread(profileId, query, delay, poll, listener);
		checkers.put(listener, thread);
		thread.start();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.provisional.p2.updatechecker.IUpdateChecker#removeUpdateCheck(org.eclipse.equinox.internal.provisional.p2.updatechecker.IUpdateListener)
	 */
	public void removeUpdateCheck(IUpdateListener listener) {
		checkers.remove(listener);
	}

	/*
	 * Return the array of ius in the profile that have updates
	 * available.
	 */
	Collection<IInstallableUnit> checkForUpdates(String profileId, IQuery<IInstallableUnit> query) {
		IProfile profile = getProfileRegistry().getProfile(profileId);
		ArrayList<IInstallableUnit> iusWithUpdates = new ArrayList<IInstallableUnit>();
		if (profile == null)
			return Collections.<IInstallableUnit> emptyList();
		ProvisioningContext context = new ProvisioningContext(agent);
		context.setMetadataRepositories(getAvailableRepositories());
		if (query == null)
			query = QueryUtil.createIUAnyQuery();
		Iterator<IInstallableUnit> iter = profile.query(query, null).iterator();
		while (iter.hasNext()) {
			IInstallableUnit iu = iter.next();
			IQueryResult<IInstallableUnit> replacements = getPlanner().updatesFor(iu, context, null);
			if (!replacements.isEmpty())
				iusWithUpdates.add(iu);
		}
		return iusWithUpdates;
	}

	/**
	 * Returns the list of metadata repositories that are currently available.
	 */
	private URI[] getAvailableRepositories() {
		IMetadataRepositoryManager repoMgr = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		URI[] repositories = repoMgr.getKnownRepositories(IRepositoryManager.REPOSITORIES_ALL);
		ArrayList<URI> available = new ArrayList<URI>();
		for (int i = 0; i < repositories.length; i++) {
			try {
				repoMgr.loadRepository(repositories[i], null);
				available.add(repositories[i]);
			} catch (ProvisionException e) {
				LogHelper.log(e.getStatus());
			}
		}
		return available.toArray(new URI[available.size()]);
	}

	void trace(String message) {
		if (Tracing.DEBUG_UPDATE_CHECK)
			Tracing.debug(message);
	}

	String getTimeStamp() {
		Date d = new Date();
		SimpleDateFormat df = new SimpleDateFormat("[MM/dd/yy;HH:mm:ss:SSS]"); //$NON-NLS-1$
		return df.format(d);
	}

	IPlanner getPlanner() {
		if (planner == null) {
			planner = (IPlanner) agent.getService(IPlanner.SERVICE_NAME);
			if (planner == null) {
				throw new IllegalStateException("Provisioning system has not been initialized"); //$NON-NLS-1$
			}
		}
		return planner;
	}

	IProfileRegistry getProfileRegistry() {
		if (profileRegistry == null) {
			profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
			if (profileRegistry == null) {
				throw new IllegalStateException("Provisioning system has not been initialized"); //$NON-NLS-1$
			}
		}
		return profileRegistry;
	}

}
