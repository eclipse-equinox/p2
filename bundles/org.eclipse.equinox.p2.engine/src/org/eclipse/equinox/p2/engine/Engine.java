/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.engine;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.OrderedProperties;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningEventBus;

public class Engine {

	private final ProvisioningEventBus eventBus;
	private List lockedProfiles = new ArrayList();

	public Engine(ProvisioningEventBus eventBus) {
		this.eventBus = eventBus;
	}

	public IStatus perform(Profile profile, PhaseSet phaseSet, Operand[] iuOperands, PropertyOperand[] propertyOperands, IProgressMonitor monitor) {

		// TODO -- Messages
		if (profile == null)
			throw new IllegalArgumentException("Profile must not be null."); //$NON-NLS-1$

		if (phaseSet == null)
			throw new IllegalArgumentException("PhaseSet must not be null."); //$NON-NLS-1$

		if (iuOperands == null)
			throw new IllegalArgumentException("Operands must not be null."); //$NON-NLS-1$

		if (monitor == null)
			monitor = new NullProgressMonitor();

		lockProfile(profile);
		try {
			eventBus.publishEvent(new BeginOperationEvent(profile, phaseSet, iuOperands, this));

			EngineSession session = new EngineSession(profile);

			synchronizeProfileProperties(profile, propertyOperands);

			snapshotIUProperties(profile, iuOperands);
			MultiStatus result = phaseSet.perform(session, profile, iuOperands, monitor);
			if (result.isOK()) {
				if (profile.isChanged()) {
					IProfileRegistry profileRegistry = (IProfileRegistry) ServiceHelper.getService(EngineActivator.getContext(), IProfileRegistry.class.getName());
					if (profileRegistry.getProfile(profile.getProfileId()) == null)
						profileRegistry.addProfile(profile);
					else {
						moveIUProperties(profile, iuOperands);
						profileRegistry.updateProfile(profile);
					}
				}
				eventBus.publishEvent(new CommitOperationEvent(profile, phaseSet, iuOperands, this));
				session.commit();
			} else if (result.matches(IStatus.ERROR | IStatus.CANCEL)) {
				eventBus.publishEvent(new RollbackOperationEvent(profile, phaseSet, iuOperands, this, result));
				session.rollback();
			}
			//if there is only one child status, return that status instead because it will have more context
			IStatus[] children = result.getChildren();
			return children.length == 1 ? children[0] : result;
		} finally {
			unlockProfile(profile);
		}
	}

	private void synchronizeProfileProperties(Profile profile, PropertyOperand[] propertyOperands) {
		if (propertyOperands == null)
			return;

		for (int i = 0; i < propertyOperands.length; i++) {
			PropertyOperand propertyOperand = propertyOperands[i];
			if (propertyOperand.first() != null) {
				removeProfileProperty(profile, propertyOperand);
			}

			if (propertyOperand.second() != null) {
				addProfileProperty(profile, propertyOperand);
			}
		}
	}

	private void addProfileProperty(Profile profile, PropertyOperand propertyOperand) {

		if (propertyOperand instanceof InstallableUnitPropertyOperand) {
			InstallableUnitPropertyOperand iuPropertyOperand = (InstallableUnitPropertyOperand) propertyOperand;
			profile.internalSetInstallableUnitProfileProperty(iuPropertyOperand.getInstallableUnit(), iuPropertyOperand.getKey(), (String) iuPropertyOperand.second());
		} else {
			profile.internalSetValue(propertyOperand.getKey(), (String) propertyOperand.second());
		}

	}

	private void removeProfileProperty(Profile profile, PropertyOperand propertyOperand) {
		if (propertyOperand instanceof InstallableUnitPropertyOperand) {
			InstallableUnitPropertyOperand iuPropertyOperand = (InstallableUnitPropertyOperand) propertyOperand;
			profile.internalSetInstallableUnitProfileProperty(iuPropertyOperand.getInstallableUnit(), iuPropertyOperand.getKey(), null);
		} else {
			profile.internalSetValue(propertyOperand.getKey(), null);
		}
	}

	//Support to move the IU properties as part of the engine. This is not really clean. We will have to review this.
	//This has to be done in two calls because when we return from the phaseSet.perform the iu properties are already lost
	Map snapshot = new HashMap();

	private void snapshotIUProperties(Profile profile, Operand[] operands) {
		for (int i = 0; i < operands.length; i++) {
			if (operands[i].first() != null && operands[i].second() != null) {
				snapshot.put(operands[i].first(), profile.getInstallableUnitProfileProperties(operands[i].first()));
			}
		}
	}

	private void moveIUProperties(Profile profile, Operand[] operands) {
		for (int i = 0; i < operands.length; i++) {
			if (operands[i].first() != null && operands[i].second() != null) {
				OrderedProperties prop = (OrderedProperties) snapshot.get(operands[i].first());
				if (prop == null)
					continue;
				Enumeration enumProps = prop.keys();
				while (enumProps.hasMoreElements()) {
					String key = (String) enumProps.nextElement();
					profile.internalSetInstallableUnitProfileProperty(operands[i].second(), key, (String) prop.get(key));
					prop.remove(key);
				}
			}
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
