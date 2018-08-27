/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
package org.eclipse.equinox.p2.tests.updatechecker;

import java.util.Arrays;
import org.eclipse.equinox.internal.provisional.p2.updatechecker.IUpdateListener;
import org.eclipse.equinox.internal.provisional.p2.updatechecker.UpdateEvent;
import org.junit.Assert;

/**
 * Test implementation of {@link IUpdateListener}.
 */
public class TestUpdateListener implements IUpdateListener {
	private static final long MAX_WAIT = 5000;

	private UpdateEvent expectedEvent;

	int expectedCount = 0;
	int unexpectedCount = 0;

	public TestUpdateListener(UpdateEvent event) {
		this.expectedEvent = event;
	}

	public boolean matches(Object o) {
		if (!(o instanceof UpdateEvent))
			return false;
		UpdateEvent actual = (UpdateEvent) o;
		return Arrays.equals(expectedEvent.getIUs().toArray(), actual.getIUs().toArray()) && expectedEvent.getProfileId().equals(actual.getProfileId());
	}

	@Override
	public void updatesAvailable(UpdateEvent event) {
		if (matches(event))
			expectedCount++;
		else
			unexpectedCount++;
	}

	/**
	 * Verifies that the given number of matching events occurred, and that no
	 * unmatching events occurred.
	 */
	public void verify(int expected) {
		Assert.assertEquals(expected, expectedCount);
		Assert.assertEquals(0, unexpectedCount);
	}

	public void waitForEvent() {
		long waitStart = System.currentTimeMillis();
		while (expectedCount == 0 && unexpectedCount == 0 && ((System.currentTimeMillis() - waitStart) < MAX_WAIT)) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				//ignore
			}
		}
	}

	public void reset() {
		expectedCount = unexpectedCount = 0;
	}

	@Override
	public void checkingForUpdates() {
		//do nothing
	}
}
