/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ericsson AB - (Pascal Rapicault)
 *     Ericsson AB   (Hamdan Msheik) - Bug 398833
 *     Red Hat Inc. - Bug 460967
 *     Mikael Barbero (Eclipse Foundation) - Bug 498116
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.scheduler;

import java.util.Date;
import java.util.Random;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.garbagecollector.GarbageCollector;
import org.eclipse.equinox.internal.p2.ui.sdk.scheduler.migration.MigrationSupport;
import org.eclipse.equinox.internal.provisional.p2.updatechecker.*;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.query.IUProfilePropertyQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * This plug-in is loaded on startup to register with the update checker.
 *
 * @since 3.5
 */
public class AutomaticUpdateScheduler implements IStartup {
	public static final String MIGRATION_DIALOG_SHOWN = "migrationDialogShown"; //$NON-NLS-1$

	public static final String P_FUZZY_RECURRENCE = "fuzzy_recurrence"; //$NON-NLS-1$

	public static final String[] FUZZY_RECURRENCE = {AutomaticUpdateMessages.SchedulerStartup_OnceADay, AutomaticUpdateMessages.SchedulerStartup_OnceAWeek, AutomaticUpdateMessages.SchedulerStartup_OnceAMonth};

	private static final int ONE_HOUR_IN_MS = 60 * 60 * 1000;
	private static final int ONE_DAY_IN_MS = 24 * ONE_HOUR_IN_MS;

	private IUpdateListener listener = null;
	private IUpdateChecker checker = null;
	String profileId;

	/**
	 * The constructor.
	 */
	public AutomaticUpdateScheduler() {
		AutomaticUpdatePlugin.getDefault().setScheduler(this);
		IProvisioningAgent agent = ServiceHelper.getService(AutomaticUpdatePlugin.getContext(), IProvisioningAgent.class);
		checker = (IUpdateChecker) agent.getService(IUpdateChecker.SERVICE_NAME);
		if (checker == null) {
			// Something did not initialize properly
			IStatus status = new Status(IStatus.ERROR, AutomaticUpdatePlugin.PLUGIN_ID, AutomaticUpdateMessages.AutomaticUpdateScheduler_UpdateNotInitialized);
			StatusManager.getManager().handle(status, StatusManager.LOG);
			return;
		}
		profileId = IProfileRegistry.SELF;
	}

	public void earlyStartup() {
		IProvisioningAgent agent = ServiceHelper.getService(AutomaticUpdatePlugin.getContext(), IProvisioningAgent.class);
		IProfileRegistry registry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
		IProfile currentProfile = registry.getProfile(profileId);
		if (currentProfile != null && new MigrationSupport().performMigration(agent, registry, currentProfile))
			return;

		garbageCollect();
		scheduleUpdate();
	}

	/**
	 * Invokes the garbage collector to discard unused plugins, if specified by a
	 * corresponding preference.
	 */
	private void garbageCollect() {
		// Nothing to do if we don't know what profile we are checking
		if (profileId == null)
			return;
		//check if gc is enabled
		IPreferenceStore pref = AutomaticUpdatePlugin.getDefault().getPreferenceStore();
		if (!pref.getBoolean(PreferenceConstants.PREF_GC_ON_STARTUP))
			return;
		IProvisioningAgent agent = ServiceHelper.getService(AutomaticUpdatePlugin.getContext(), IProvisioningAgent.class);
		GarbageCollector collector = (GarbageCollector) agent.getService(GarbageCollector.SERVICE_NAME);
		if (collector == null)
			return;
		IProfileRegistry registry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
		if (registry == null)
			return;
		IProfile profile = registry.getProfile(profileId);
		if (profile == null)
			return;
		collector.runGC(profile);
	}

	public void shutdown() {
		removeUpdateListener();
	}

	public void rescheduleUpdate() {
		removeUpdateListener();
		IPreferenceStore pref = AutomaticUpdatePlugin.getDefault().getPreferenceStore();
		String schedule = pref.getString(PreferenceConstants.PREF_AUTO_UPDATE_SCHEDULE);
		// See if we have a scheduled check or startup only.  If it is
		// startup only, there is nothing more to do now, a listener will
		// be created on the next startup.
		if (schedule.equals(PreferenceConstants.PREF_UPDATE_ON_STARTUP)) {
			return;
		}
		scheduleUpdate();
	}

	private void scheduleUpdate() {
		// Nothing to do if we don't know what profile we are checking
		if (profileId == null)
			return;
		IPreferenceStore pref = AutomaticUpdatePlugin.getDefault().getPreferenceStore();
		// See if automatic search is enabled at all
		if (!pref.getBoolean(PreferenceConstants.PREF_AUTO_UPDATE_ENABLED))
			return;
		String schedule = pref.getString(PreferenceConstants.PREF_AUTO_UPDATE_SCHEDULE);
		long delay = IUpdateChecker.ONE_TIME_CHECK;
		long poll = IUpdateChecker.ONE_TIME_CHECK;
		if (!schedule.equals(PreferenceConstants.PREF_UPDATE_ON_STARTUP)) {
			if (schedule.equals(PreferenceConstants.PREF_UPDATE_ON_FUZZY_SCHEDULE)) {
				delay = computeFuzzyDelay(pref);
				poll = computeFuzzyPoll(pref);
			}
		}
		// We do not access the AutomaticUpdater directly when we register
		// the listener. This prevents the UI classes from being started up
		// too soon.
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=227582
		listener = new IUpdateListener() {
			public void updatesAvailable(UpdateEvent event) {
				AutomaticUpdatePlugin.getDefault().getAutomaticUpdater().updatesAvailable(event);
			}

			public void checkingForUpdates() {
				AutomaticUpdatePlugin.getDefault().getAutomaticUpdater().checkingForUpdates();
			}
		};
		checker.addUpdateCheck(profileId, getProfileQuery(), delay, poll, listener);

	}

	private IQuery<IInstallableUnit> getProfileQuery() {
		// We specifically avoid using the default policy's root property so that we don't load all the
		// p2 UI classes in doing so.
		return new IUProfilePropertyQuery(IProfile.PROP_PROFILE_ROOT_IU, Boolean.TRUE.toString());
	}

	private static long computeFuzzyDelay(IPreferenceStore pref) {
		Date nowDate = java.util.Calendar.getInstance().getTime();
		long now = nowDate.getTime();
		long lastCheckForUpdateSinceEpoch = new LastAutoCheckForUpdateMemo(AutomaticUpdatePlugin.getDefault().getAgentLocation()).readAndStoreIfAbsent(nowDate).getTime();
		long poll = computeFuzzyPoll(pref);
		if (now - lastCheckForUpdateSinceEpoch >= poll + getMaxDelay(pref)) {
			// Last check for update has exceeded the max delay we allow,
			// let's do it sometime in the next hour.
			return new Random().nextInt(ONE_HOUR_IN_MS);
		}
		long delay = now - lastCheckForUpdateSinceEpoch;
		// We do delay the next check sometime in the 8 hours after the computed schedule
		return poll - delay + new Random().nextInt(8 * ONE_HOUR_IN_MS);
	}

	private static long getMaxDelay(IPreferenceStore pref) {
		String recurrence = pref.getString(P_FUZZY_RECURRENCE);
		if (AutomaticUpdateMessages.SchedulerStartup_OnceADay.equals(recurrence)) {
			return 6 * ONE_HOUR_IN_MS;
		} else if (AutomaticUpdateMessages.SchedulerStartup_OnceAWeek.equals(recurrence)) {
			return 2 * ONE_DAY_IN_MS;
		} else { // Once a month
			return 6 * ONE_DAY_IN_MS;
		}
	}

	private static long computeFuzzyPoll(IPreferenceStore pref) {
		String recurrence = pref.getString(P_FUZZY_RECURRENCE);
		if (AutomaticUpdateMessages.SchedulerStartup_OnceADay.equals(recurrence)) {
			return ONE_DAY_IN_MS;
		} else if (AutomaticUpdateMessages.SchedulerStartup_OnceAWeek.equals(recurrence)) {
			return 7 * ONE_DAY_IN_MS;
		} else { // Once a month
			// It's not rocket science we're doing here,
			// let's approximate that a month is always 30 days long
			return 30 * ONE_DAY_IN_MS;
		}
	}

	private void removeUpdateListener() {
		// Remove the current listener if there is one
		if (listener != null && checker != null) {
			checker.removeUpdateCheck(listener);
			listener = null;
		}
	}
}
