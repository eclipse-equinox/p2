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
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningEventBus;

public class Engine {

	private final ProvisioningEventBus eventBus;
	private List lockedProfiles = new ArrayList();

	public Engine(ProvisioningEventBus eventBus) {
		this.eventBus = eventBus;
	}

	public MultiStatus perform(Profile profile, PhaseSet phaseSet, Operand[] operands, IProgressMonitor monitor) {

		// TODO -- Messages
		if (profile == null)
			throw new IllegalArgumentException("Profile must not be null."); //$NON-NLS-1$

		if (phaseSet == null)
			throw new IllegalArgumentException("PhaseSet must not be null."); //$NON-NLS-1$

		if (operands == null)
			throw new IllegalArgumentException("Operands must not be null."); //$NON-NLS-1$

		if (monitor == null)
			monitor = new NullProgressMonitor();

		if (operands.length == 0)
			return new MultiStatus(EngineActivator.ID, IStatus.OK, null, null);

		lockProfile(profile);
		try {
			eventBus.publishEvent(new BeginOperationEvent(profile, phaseSet, operands, this));

			EngineSession session = new EngineSession(profile);
			MultiStatus result = phaseSet.perform(session, profile, operands, monitor);
			if (result.isOK()) {
				if (profile.isChanged()) {
					IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(EngineActivator.getContext(), IProfileRegistry.class.getName());
					if (profileRegistry.getProfile(profile.getProfileId()) == null)
						profileRegistry.addProfile(profile);
					else
						profileRegistry.updateProfile(profile);
				}
				eventBus.publishEvent(new CommitOperationEvent(profile, phaseSet, operands, this));
				session.commit();
			} else if (result.matches(IStatus.ERROR | IStatus.CANCEL)) {
				eventBus.publishEvent(new RollbackOperationEvent(profile, phaseSet, operands, this, result));
				session.rollback();
			}
			return result;
		} finally {
			unlockProfile(profile);
		}
	}

	private synchronized void unlockProfile(Profile profile) {
		lockedProfiles.remove(profile.getProfileId());
		notify();
	}

	private synchronized void lockProfile(Profile profile) {
		String profileId = profile.getProfileId();
		while (lockedProfiles.contains(profileId)) {
			try {
				wait();
			} catch (InterruptedException e) {
				// TODO We should think about how we want to handle blocked engine requests
				Thread.currentThread().interrupt();
			}
		}
		lockedProfiles.add(profileId);
	}
}
