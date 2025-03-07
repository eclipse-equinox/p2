/*******************************************************************************
 *  Copyright (c) 2007, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
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
import java.util.function.IntSupplier;
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
	private final HashMap<IUpdateListener, UpdateCheckThread> checkers = new HashMap<>();

	private final IProvisioningAgent agent;
	IProfileRegistry profileRegistry;
	IPlanner planner;

	private class UpdateCheckThread extends Thread {
		boolean done = false;
		long poll, delay;
		IUpdateListener listener;
		String profileId;
		IQuery<IInstallableUnit> query;
		private final IntSupplier repositoryFlags;

		UpdateCheckThread(String profileId, IQuery<IInstallableUnit> query, long delay, long poll,
				IntSupplier repositoryFlags,
				IUpdateListener listener) {
			this.poll = poll;
			this.delay = delay;
			this.profileId = profileId;
			this.query = query;
			this.repositoryFlags = repositoryFlags;
			this.listener = listener;
		}

		@Override
		public void run() {
			try {
				if (delay != ONE_TIME_CHECK && delay > 0) {
					Thread.sleep(delay);
				}
				while (!done) {
					listener.checkingForUpdates();
					trace("Checking for updates for " + profileId + " at " + getTimeStamp()); //$NON-NLS-1$ //$NON-NLS-2$
					Collection<IInstallableUnit> iusWithUpdates = checkForUpdates(profileId, query,
							repositoryFlags.getAsInt());
					if (iusWithUpdates.size() > 0) {
						trace("Notifying listener of available updates"); //$NON-NLS-1$
						UpdateEvent event = new UpdateEvent(profileId, iusWithUpdates);
						if (!done) {
							listener.updatesAvailable(event);
						}
					} else {
						trace("No updates were available"); //$NON-NLS-1$
					}
					if (delay == ONE_TIME_CHECK || delay <= 0 || poll <= 0) {
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

	@Override
	public void addUpdateCheck(String profileId, IQuery<IInstallableUnit> query, long delay, long poll, IUpdateListener listener) {
		addUpdateCheck(profileId, query, delay, poll, () -> IRepositoryManager.REPOSITORIES_ALL, listener);
	}

	@Override
	public void addUpdateCheck(String profileId, IQuery<IInstallableUnit> query, long delay, long poll,
			IntSupplier repositoryFlags, IUpdateListener listener) {
		if (checkers.containsKey(listener)) {
			return;
		}
		trace("Adding update checker for " + profileId + " at " + getTimeStamp()); //$NON-NLS-1$ //$NON-NLS-2$
		UpdateCheckThread thread = new UpdateCheckThread(profileId, query, delay, poll, repositoryFlags, listener);
		checkers.put(listener, thread);
		thread.start();
	}

	@Override
	public void removeUpdateCheck(IUpdateListener listener) {
		checkers.remove(listener);
	}

	/*
	 * Return the array of ius in the profile that have updates
	 * available.
	 */
	Collection<IInstallableUnit> checkForUpdates(String profileId, IQuery<IInstallableUnit> query,
			int repositoryFlags) {
		IProfile profile = getProfileRegistry().getProfile(profileId);
		ArrayList<IInstallableUnit> iusWithUpdates = new ArrayList<>();
		if (profile == null) {
			return Collections.emptyList();
		}
		ProvisioningContext context = new ProvisioningContext(agent);
		context.setMetadataRepositories(getAvailableRepositories(repositoryFlags));
		if (query == null) {
			query = QueryUtil.createIUAnyQuery();
		}
		for (IInstallableUnit iu : profile.query(query, null)) {
			IQueryResult<IInstallableUnit> replacements = getPlanner().updatesFor(iu, context, null);
			if (!replacements.isEmpty()) {
				iusWithUpdates.add(iu);
			}
		}
		return iusWithUpdates;
	}

	/**
	 * Returns the list of metadata repositories that are currently available.
	 */
	private URI[] getAvailableRepositories(int repositoryFlags) {
		IMetadataRepositoryManager repoMgr = agent.getService(IMetadataRepositoryManager.class);
		URI[] repositories = repoMgr.getKnownRepositories(repositoryFlags);
		ArrayList<URI> available = new ArrayList<>();
		for (URI repositorie : repositories) {
			try {
				repoMgr.loadRepository(repositorie, null);
				available.add(repositorie);
			} catch (ProvisionException e) {
				LogHelper.log(e.getStatus());
			}
		}
		return available.toArray(new URI[available.size()]);
	}

	void trace(String message) {
		if (Tracing.DEBUG_UPDATE_CHECK) {
			Tracing.debug(message);
		}
	}

	String getTimeStamp() {
		Date d = new Date();
		SimpleDateFormat df = new SimpleDateFormat("[MM/dd/yy;HH:mm:ss:SSS]"); //$NON-NLS-1$
		return df.format(d);
	}

	IPlanner getPlanner() {
		if (planner == null) {
			planner = agent.getService(IPlanner.class);
			if (planner == null) {
				throw new IllegalStateException("Provisioning system has not been initialized"); //$NON-NLS-1$
			}
		}
		return planner;
	}

	IProfileRegistry getProfileRegistry() {
		if (profileRegistry == null) {
			profileRegistry = agent.getService(IProfileRegistry.class);
			if (profileRegistry == null) {
				throw new IllegalStateException("Provisioning system has not been initialized"); //$NON-NLS-1$
			}
		}
		return profileRegistry;
	}

}
