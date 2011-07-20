/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk.scheduler;

import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.ULocale;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.garbagecollector.GarbageCollector;
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
	// values are to be picked up from the arrays DAYS and HOURS
	public static final String P_DAY = "day"; //$NON-NLS-1$

	public static final String P_HOUR = "hour"; //$NON-NLS-1$

	public static final String[] DAYS;

	public static final String[] HOURS = {AutomaticUpdateMessages.SchedulerStartup_1AM, AutomaticUpdateMessages.SchedulerStartup_2AM, AutomaticUpdateMessages.SchedulerStartup_3AM, AutomaticUpdateMessages.SchedulerStartup_4AM, AutomaticUpdateMessages.SchedulerStartup_5AM, AutomaticUpdateMessages.SchedulerStartup_6AM, AutomaticUpdateMessages.SchedulerStartup_7AM, AutomaticUpdateMessages.SchedulerStartup_8AM, AutomaticUpdateMessages.SchedulerStartup_9AM, AutomaticUpdateMessages.SchedulerStartup_10AM, AutomaticUpdateMessages.SchedulerStartup_11AM, AutomaticUpdateMessages.SchedulerStartup_12PM, AutomaticUpdateMessages.SchedulerStartup_1PM, AutomaticUpdateMessages.SchedulerStartup_2PM, AutomaticUpdateMessages.SchedulerStartup_3PM, AutomaticUpdateMessages.SchedulerStartup_4PM,
			AutomaticUpdateMessages.SchedulerStartup_5PM, AutomaticUpdateMessages.SchedulerStartup_6PM, AutomaticUpdateMessages.SchedulerStartup_7PM, AutomaticUpdateMessages.SchedulerStartup_8PM, AutomaticUpdateMessages.SchedulerStartup_9PM, AutomaticUpdateMessages.SchedulerStartup_10PM, AutomaticUpdateMessages.SchedulerStartup_11PM, AutomaticUpdateMessages.SchedulerStartup_12AM,};

	private IUpdateListener listener = null;
	private IUpdateChecker checker = null;
	String profileId;

	static {
		Calendar calendar = Calendar.getInstance(new ULocale(Platform.getNL()));
		String[] daysAsStrings = {AutomaticUpdateMessages.SchedulerStartup_day, AutomaticUpdateMessages.SchedulerStartup_Sunday, AutomaticUpdateMessages.SchedulerStartup_Monday, AutomaticUpdateMessages.SchedulerStartup_Tuesday, AutomaticUpdateMessages.SchedulerStartup_Wednesday, AutomaticUpdateMessages.SchedulerStartup_Thursday, AutomaticUpdateMessages.SchedulerStartup_Friday, AutomaticUpdateMessages.SchedulerStartup_Saturday};
		int firstDay = calendar.getFirstDayOfWeek();
		DAYS = new String[8];
		DAYS[0] = daysAsStrings[0];
		int countDays = 0;
		for (int i = firstDay; i <= 7; i++) {
			DAYS[++countDays] = daysAsStrings[i];
		}
		for (int i = 1; i < firstDay; i++) {
			DAYS[++countDays] = daysAsStrings[i];
		}
	}

	/**
	 * The constructor.
	 */
	public AutomaticUpdateScheduler() {
		AutomaticUpdatePlugin.getDefault().setScheduler(this);
		IProvisioningAgent agent = (IProvisioningAgent) ServiceHelper.getService(AutomaticUpdatePlugin.getContext(), IProvisioningAgent.SERVICE_NAME);
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
		IProvisioningAgent agent = (IProvisioningAgent) ServiceHelper.getService(AutomaticUpdatePlugin.getContext(), IProvisioningAgent.SERVICE_NAME);
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
			delay = computeDelay(pref);
			poll = computePoll(pref);
		}
		// We do not access the AutomaticUpdater directly when we register 
		// the listener. This prevents the UI classes from being started up
		// too soon.
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=227582
		listener = new IUpdateListener() {
			public void updatesAvailable(UpdateEvent event) {
				AutomaticUpdatePlugin.getDefault().getAutomaticUpdater().updatesAvailable(event);
			}

		};
		checker.addUpdateCheck(profileId, getProfileQuery(), delay, poll, listener);

	}

	private IQuery<IInstallableUnit> getProfileQuery() {
		// We specifically avoid using the default policy's root property so that we don't load all the
		// p2 UI classes in doing so.  
		return new IUProfilePropertyQuery(IProfile.PROP_PROFILE_ROOT_IU, Boolean.TRUE.toString());
	}

	private int getDay(IPreferenceStore pref) {
		String day = pref.getString(P_DAY);
		for (int d = 0; d < DAYS.length; d++)
			if (DAYS[d].equals(day))
				switch (d) {
					case 0 :
						return -1;
					case 1 :
						return Calendar.MONDAY;
					case 2 :
						return Calendar.TUESDAY;
					case 3 :
						return Calendar.WEDNESDAY;
					case 4 :
						return Calendar.THURSDAY;
					case 5 :
						return Calendar.FRIDAY;
					case 6 :
						return Calendar.SATURDAY;
					case 7 :
						return Calendar.SUNDAY;
				}
		return -1;
	}

	private int getHour(IPreferenceStore pref) {
		String hour = pref.getString(P_HOUR);
		for (int h = 0; h < HOURS.length; h++)
			if (HOURS[h].equals(hour))
				return h + 1;
		return 1;
	}

	/*
	 * Computes the number of milliseconds from this moment to the next
	 * scheduled update check. If that moment has already passed, returns 0L (start
	 * immediately).
	 */
	private long computeDelay(IPreferenceStore pref) {

		int target_d = getDay(pref);
		int target_h = getHour(pref);

		Calendar calendar = Calendar.getInstance();
		// may need to use the BootLoader locale
		int current_d = calendar.get(Calendar.DAY_OF_WEEK);
		// starts with SUNDAY
		int current_h = calendar.get(Calendar.HOUR_OF_DAY);
		int current_m = calendar.get(Calendar.MINUTE);
		int current_s = calendar.get(Calendar.SECOND);
		int current_ms = calendar.get(Calendar.MILLISECOND);

		long delay = 0L; // milliseconds

		if (target_d == -1) {
			// Compute the delay for "every day at x o'clock"
			// Is it now ?
			if (target_h == current_h && current_m == 0 && current_s == 0)
				return delay;

			int delta_h = target_h - current_h;
			if (target_h <= current_h)
				delta_h += 24;
			delay = ((delta_h * 60 - current_m) * 60 - current_s) * 1000 - current_ms;
			return delay;
		}
		// Compute the delay for "every Xday at x o'clock"
		// Is it now ?
		if (target_d == current_d && target_h == current_h && current_m == 0 && current_s == 0)
			return delay;

		int delta_d = target_d - current_d;
		if (target_d < current_d || target_d == current_d && (target_h < current_h || target_h == current_h && current_m > 0))
			delta_d += 7;

		delay = (((delta_d * 24 + target_h - current_h) * 60 - current_m) * 60 - current_s) * 1000 - current_ms;
		return delay;
	}

	/*
	 * Computes the number of milliseconds for the polling frequency.
	 * We have already established that there is a schedule, vs. only
	 * on startup.
	 */
	private long computePoll(IPreferenceStore pref) {

		int target_d = getDay(pref);
		if (target_d == -1) {
			// Every 24 hours
			return 24 * 60 * 60 * 1000;
		}
		return 7 * 24 * 60 * 60 * 1000;
	}

	private void removeUpdateListener() {
		// Remove the current listener if there is one
		if (listener != null && checker != null) {
			checker.removeUpdateCheck(listener);
			listener = null;
		}
	}
}
