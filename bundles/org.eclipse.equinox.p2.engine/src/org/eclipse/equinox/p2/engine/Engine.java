/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.engine;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningEventBus;
import org.eclipse.equinox.p2.core.helpers.MultiStatus;

public class Engine {

	private final ProvisioningEventBus eventBus;
	private List lockedProfiles = new ArrayList();

	public Engine(ProvisioningEventBus eventBus) {
		this.eventBus = eventBus;
	}

	public MultiStatus perform(Profile profile, PhaseSet phaseSet, Operand[] operands, IProgressMonitor monitor) {

		// TODO -- Messages
		if (profile == null)
			throw new IllegalArgumentException("Profile must not be null.");

		if (phaseSet == null)
			throw new IllegalArgumentException("PhaseSet must not be null.");

		if (operands == null)
			throw new IllegalArgumentException("Operands must not be null.");

		if (monitor == null)
			monitor = new NullProgressMonitor();

		if (operands.length == 0)
			return new MultiStatus(IStatus.OK, null);

		lockProfile(profile);
		try {
			eventBus.publishEvent(new BeginOperationEvent(profile, phaseSet, operands, this));

			EngineSession session = new EngineSession(profile);
			MultiStatus result = phaseSet.perform(session, profile, operands, monitor);
			if (result.isOK()) {
				eventBus.publishEvent(new CommitOperationEvent(profile, phaseSet, operands, this));
				session.commit();
			} else if (result.isErrorOrCancel()) {
				eventBus.publishEvent(new RollbackOperationEvent(profile, phaseSet, operands, this, result));
				session.rollback();
			}
			return result;
		} finally {
			unlockProfile(profile);
		}
	}

	private synchronized void unlockProfile(Profile profile) {
		lockedProfiles.remove(profile);
		notify();
	}

	private synchronized void lockProfile(Profile profile) {
		while (lockedProfiles.contains(profile)) {
			try {
				wait();
			} catch (InterruptedException e) {
				// TODO We should think about how we want to handle blocked engine requests
				Thread.currentThread().interrupt();
			}
		}
		lockedProfiles.add(profile);
	}
}
