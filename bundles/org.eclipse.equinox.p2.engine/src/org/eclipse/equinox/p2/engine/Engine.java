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
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningEventBus;

public class Engine implements IEngine {

	private final ProvisioningEventBus eventBus;
	private List lockedProfiles = new ArrayList();

	public Engine(ProvisioningEventBus eventBus) {
		this.eventBus = eventBus;
	}

	public IStatus perform(IProfile profile, PhaseSet phaseSet, Operand[] operands, ProvisioningContext context, IProgressMonitor monitor) {
		if (operands == null)
			throw new IllegalArgumentException("Operands must not be null."); //$NON-NLS-1$

		List iuOperands = new ArrayList();
		List propertyOperands = new ArrayList();
		for (int i = 0; i < operands.length; i++) {
			Operand operand = operands[i];
			if (operand instanceof InstallableUnitOperand) {
				iuOperands.add(operand);
			} else if (operand instanceof PropertyOperand) {
				propertyOperands.add(operand);
			}
		}

		InstallableUnitOperand[] iuOperandArray = (InstallableUnitOperand[]) iuOperands.toArray(new InstallableUnitOperand[0]);
		PropertyOperand[] propertyOperandArray = (PropertyOperand[]) propertyOperands.toArray(new PropertyOperand[0]);
		return perform(profile, phaseSet, iuOperandArray, propertyOperandArray, monitor);
	}

	private IStatus perform(IProfile iprofile, PhaseSet phaseSet, InstallableUnitOperand[] iuOperands, PropertyOperand[] propertyOperands, IProgressMonitor monitor) {

		// TODO -- Messages
		if (iprofile == null)
			throw new IllegalArgumentException("Profile must not be null."); //$NON-NLS-1$

		if (phaseSet == null)
			throw new IllegalArgumentException("PhaseSet must not be null."); //$NON-NLS-1$

		if (iuOperands == null)
			throw new IllegalArgumentException("Operands must not be null."); //$NON-NLS-1$

		if (monitor == null)
			monitor = new NullProgressMonitor();

		Profile profile = (Profile) iprofile;
		lockProfile(profile);
		try {
			SimpleProfileRegistry profileRegistry = (SimpleProfileRegistry) ServiceHelper.getService(EngineActivator.getContext(), IProfileRegistry.class.getName());
			if (profileRegistry.getProfile(profile.getProfileId()) == null)
				throw new IllegalArgumentException("Profile is not registered."); //$NON-NLS-1$

			eventBus.publishEvent(new BeginOperationEvent(profile, phaseSet, iuOperands, this));

			EngineSession session = new EngineSession(profile);

			synchronizeProfileProperties(profile, propertyOperands);

			snapshotIUProperties(profile, iuOperands);
			MultiStatus result = phaseSet.perform(session, profile, iuOperands, monitor);
			if (result.isOK()) {
				if (profile.isChanged()) {
					moveIUProperties(profile, iuOperands);
					profileRegistry.updateProfile(profile);
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
			profile.setInstallableUnitProperty(iuPropertyOperand.getInstallableUnit(), iuPropertyOperand.getKey(), (String) iuPropertyOperand.second());
		} else {
			profile.setProperty(propertyOperand.getKey(), (String) propertyOperand.second());
		}

	}

	private void removeProfileProperty(Profile profile, PropertyOperand propertyOperand) {
		if (propertyOperand instanceof InstallableUnitPropertyOperand) {
			InstallableUnitPropertyOperand iuPropertyOperand = (InstallableUnitPropertyOperand) propertyOperand;
			profile.setInstallableUnitProperty(iuPropertyOperand.getInstallableUnit(), iuPropertyOperand.getKey(), null);
		} else {
			profile.setProperty(propertyOperand.getKey(), null);
		}
	}

	//Support to move the IU properties as part of the engine. This is not really clean. We will have to review this.
	//This has to be done in two calls because when we return from the phaseSet.perform the iu properties are already lost
	Map snapshot = new HashMap();

	private void snapshotIUProperties(IProfile profile, InstallableUnitOperand[] operands) {
		for (int i = 0; i < operands.length; i++) {
			if (operands[i].first() != null && operands[i].second() != null) {
				snapshot.put(operands[i].first(), profile.getInstallableUnitProperties(operands[i].first()));
			}
		}
	}

	private void moveIUProperties(Profile profile, InstallableUnitOperand[] operands) {
		for (int i = 0; i < operands.length; i++) {
			if (operands[i].first() != null && operands[i].second() != null) {
				OrderedProperties prop = (OrderedProperties) snapshot.get(operands[i].first());
				if (prop == null)
					continue;
				Enumeration enumProps = prop.keys();
				while (enumProps.hasMoreElements()) {
					String key = (String) enumProps.nextElement();
					profile.setInstallableUnitProperty(operands[i].second(), key, (String) prop.get(key));
					prop.remove(key);
				}
			}
		}
	}

	private synchronized void unlockProfile(IProfile profile) {
		lockedProfiles.remove(profile.getProfileId());
		notify();
	}

	private synchronized void lockProfile(IProfile profile) {
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
