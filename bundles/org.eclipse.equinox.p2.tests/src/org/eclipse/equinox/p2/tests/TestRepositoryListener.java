/*******************************************************************************
 *  Copyright (c) 2008, 2017 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.equinox.p2.tests;

import java.net.URI;
import java.util.EventObject;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.SynchronousProvisioningListener;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.junit.Assert;

/**
 * A provisioning event listener used for testing purposes. If a location is provided,
 * the listener only tracks events on repositories at that location.
 */
public class TestRepositoryListener implements SynchronousProvisioningListener {
	private static final long MAX_WAIT = 10000;//ten seconds
	public boolean lastEnablement;
	public int lastKind;
	public int lastRepoType;
	private URI repoLocation;
	private boolean wasCalled;

	public TestRepositoryListener(URI location) {
		this.repoLocation = location;
	}

	@Override
	public void notify(EventObject o) {
		if (!(o instanceof RepositoryEvent))
			return;
		RepositoryEvent event = (RepositoryEvent) o;
		if (repoLocation != null && !event.getRepositoryLocation().equals(repoLocation))
			return;
		setCalled(true);
		lastKind = event.getKind();
		lastRepoType = event.getRepositoryType();
		lastEnablement = event.isRepositoryEnabled();
	}

	public void reset() {
		lastKind = lastRepoType = 0;
		setCalled(lastEnablement = false);
	}

	public synchronized void setCalled(boolean wasCalled) {
		this.wasCalled = wasCalled;
		notifyAll();
	}

	/**
	 * Waits a reasonable amount of time for an expected provisioning event.
	 * Throw an assertion failure if the event does not occur, to prevent test deadlock.
	 */
	public synchronized void waitForEvent() {
		long waitStart = System.currentTimeMillis();
		while (!wasCalled()) {
			Assert.assertTrue("Timeout waiting for repository event", System.currentTimeMillis() - waitStart < MAX_WAIT);
			try {
				wait(100);
			} catch (InterruptedException e) {
				//ignore
			}
		}
	}

	public synchronized boolean wasCalled() {
		return wasCalled;
	}
}
