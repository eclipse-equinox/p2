/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk;

import java.util.Calendar;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.ui.sdk.prefs.PreferenceConstants;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.p2.updatechecker.IUpdateListener;
import org.eclipse.equinox.p2.updatechecker.UpdateChecker;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * This plug-in is loaded on startup to fork a job that searches for new
 * plug-ins.
 */
public class AutomaticUpdateScheduler implements IStartup {
	// values are to be picked up from the arrays DAYS and HOURS
	public static final String P_DAY = "day"; //$NON-NLS-1$

	public static final String P_HOUR = "hour"; //$NON-NLS-1$

	public static final String[] DAYS = {ProvSDKMessages.SchedulerStartup_day, ProvSDKMessages.SchedulerStartup_Monday, ProvSDKMessages.SchedulerStartup_Tuesday, ProvSDKMessages.SchedulerStartup_Wednesday, ProvSDKMessages.SchedulerStartup_Thursday, ProvSDKMessages.SchedulerStartup_Friday, ProvSDKMessages.SchedulerStartup_Saturday, ProvSDKMessages.SchedulerStartup_Sunday};

	public static final String[] HOURS = {ProvSDKMessages.SchedulerStartup_1AM, ProvSDKMessages.SchedulerStartup_2AM, ProvSDKMessages.SchedulerStartup_3AM, ProvSDKMessages.SchedulerStartup_4AM, ProvSDKMessages.SchedulerStartup_5AM, ProvSDKMessages.SchedulerStartup_6AM, ProvSDKMessages.SchedulerStartup_7AM, ProvSDKMessages.SchedulerStartup_8AM, ProvSDKMessages.SchedulerStartup_9AM, ProvSDKMessages.SchedulerStartup_10AM, ProvSDKMessages.SchedulerStartup_11AM, ProvSDKMessages.SchedulerStartup_12PM, ProvSDKMessages.SchedulerStartup_1PM, ProvSDKMessages.SchedulerStartup_2PM, ProvSDKMessages.SchedulerStartup_3PM, ProvSDKMessages.SchedulerStartup_4PM, ProvSDKMessages.SchedulerStartup_5PM, ProvSDKMessages.SchedulerStartup_6PM, ProvSDKMessages.SchedulerStartup_7PM,
			ProvSDKMessages.SchedulerStartup_8PM, ProvSDKMessages.SchedulerStartup_9PM, ProvSDKMessages.SchedulerStartup_10PM, ProvSDKMessages.SchedulerStartup_11PM, ProvSDKMessages.SchedulerStartup_12AM,};

	private IUpdateListener listener = null;
	private UpdateChecker checker = null;
	private String profileId;

	/**
	 * The constructor.
	 */
	public AutomaticUpdateScheduler() {
		ProvSDKUIActivator.getDefault().setScheduler(this);
		checker = (UpdateChecker) ServiceHelper.getService(ProvSDKUIActivator.getContext(), UpdateChecker.class.getName());
		if (checker == null) {
			// Something did not initialize properly
			IStatus status = new Status(IStatus.ERROR, ProvSDKUIActivator.PLUGIN_ID, ProvSDKMessages.AutomaticUpdateScheduler_UpdateNotInitialized);
			StatusManager.getManager().handle(status, StatusManager.LOG);
			return;
		}
		try {
			profileId = ProvSDKUIActivator.getProfileId();
		} catch (ProvisionException e) {
			profileId = null;
			IStatus status = new Status(IStatus.ERROR, ProvSDKUIActivator.PLUGIN_ID, ProvSDKMessages.UpdateHandler_NoProfilesDefined, e);
			StatusManager.getManager().handle(status, StatusManager.LOG);
			return;
		}

	}

	public void earlyStartup() {
		scheduleUpdate();
	}

	public void shutdown() {
		removeUpdateListener();
	}

	public void rescheduleUpdate() {
		removeUpdateListener();
		Preferences pref = ProvSDKUIActivator.getDefault().getPluginPreferences();
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
		Preferences pref = ProvSDKUIActivator.getDefault().getPluginPreferences();
		// See if automatic search is enabled at all
		if (pref.getBoolean(PreferenceConstants.PREF_AUTO_UPDATE_ENABLED) == false)
			return;
		String schedule = pref.getString(PreferenceConstants.PREF_AUTO_UPDATE_SCHEDULE);
		long delay = UpdateChecker.ONE_TIME_CHECK;
		long poll = UpdateChecker.ONE_TIME_CHECK;
		if (!schedule.equals(PreferenceConstants.PREF_UPDATE_ON_STARTUP)) {
			delay = computeDelay(pref);
			poll = computePoll(pref);
		}
		listener = ProvSDKUIActivator.getDefault().getAutomaticUpdater();
		checker.addUpdateCheck(profileId, delay, poll, listener);

	}

	private int getDay(Preferences pref) {
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

	private int getHour(Preferences pref) {
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
	private long computeDelay(Preferences pref) {

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
	private long computePoll(Preferences pref) {

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
