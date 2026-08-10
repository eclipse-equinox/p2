/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.ui.misc;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import org.eclipse.equinox.internal.p2.ui.sdk.scheduler.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.junit.Test;

public class AutomaticUpdaterTest {

	private static final String P_FUZZY_RECURRENCE = AutomaticUpdateScheduler.P_FUZZY_RECURRENCE;

	// 30 * 86,400,000 = 2,592,000,000 > Integer.MAX_VALUE (2,147,483,647).
	@Test
	public void testComputeFuzzyPollOnceAMonthDoesNotOverflow() throws Exception {
		long result = invokeComputeFuzzyPoll(AutomaticUpdateMessages.SchedulerStartup_OnceAMonth);
		assertEquals("Once-a-month poll period must equal 30 days in ms, was negative before fix (bug #862)",
				30L * 24 * 60 * 60 * 1000, result);
	}

	@Test
	public void testSnoozeTomorrowStoresOneDayDelay() throws Exception {
		PreferenceStore prefs = new PreferenceStore();
		AutomaticUpdatesPopup popup = new AutomaticUpdatesPopup(null, false, prefs);
		long before = System.currentTimeMillis();
		invokeSnooze(popup, TimeUnit.DAYS.toMillis(1));
		long stored = prefs.getLong(PreferenceConstants.PREF_SNOOZE_UNTIL);
		assertTrue("snoozeUntil must be at least now + 1 day", stored >= before + TimeUnit.DAYS.toMillis(1));
	}

	@Test
	public void testSnoozeNextWeekStoresSevenDayDelay() throws Exception {
		PreferenceStore prefs = new PreferenceStore();
		AutomaticUpdatesPopup popup = new AutomaticUpdatesPopup(null, false, prefs);
		long before = System.currentTimeMillis();
		invokeSnooze(popup, TimeUnit.DAYS.toMillis(7));
		long stored = prefs.getLong(PreferenceConstants.PREF_SNOOZE_UNTIL);
		assertTrue("snoozeUntil must be at least now + 7 days", stored >= before + TimeUnit.DAYS.toMillis(7));
	}

	@Test
	public void testSnoozeWindowExpiredWhenNowReachesTimestamp() throws Exception {
		long now = 1_000_000L;
		assertFalse("Snooze must be inactive once now reaches the stored timestamp", invokeIsSnoozeActive(now, now));
		assertTrue("Snooze must still be active one millisecond before the stored timestamp",
				invokeIsSnoozeActive(now, now - 1));
	}

	private static long invokeComputeFuzzyPoll(String recurrence) throws Exception {
		Method m = AutomaticUpdateScheduler.class.getDeclaredMethod("computeFuzzyPoll", IPreferenceStore.class);
		m.setAccessible(true);
		PreferenceStore store = new PreferenceStore();
		store.setValue(P_FUZZY_RECURRENCE, recurrence);
		return (long) m.invoke(null, store);
	}

	private static void invokeSnooze(AutomaticUpdatesPopup popup, long delayMs) throws Exception {
		Method m = AutomaticUpdatesPopup.class.getDeclaredMethod("snooze", long.class);
		m.setAccessible(true);
		m.invoke(popup, delayMs);
	}

	private static boolean invokeIsSnoozeActive(long snoozeUntil, long now) throws Exception {
		Method m = AutomaticUpdater.class.getDeclaredMethod("isSnoozeActive", long.class, long.class);
		m.setAccessible(true);
		return (boolean) m.invoke(null, snoozeUntil, now);
	}
}