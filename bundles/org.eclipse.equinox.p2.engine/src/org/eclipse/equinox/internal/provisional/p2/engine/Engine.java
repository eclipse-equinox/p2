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
package org.eclipse.equinox.internal.provisional.p2.engine;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;

public class Engine implements IEngine {

	private final IProvisioningEventBus eventBus;
	private ActionManager actionManager;

	public Engine(IProvisioningEventBus eventBus) {
		this.eventBus = eventBus;
		this.actionManager = new ActionManager();
	}

	public IStatus perform(IProfile iprofile, PhaseSet phaseSet, Operand[] operands, ProvisioningContext context, IProgressMonitor monitor) {

		// TODO -- Messages
		if (iprofile == null)
			throw new IllegalArgumentException(Messages.null_profile);

		if (phaseSet == null)
			throw new IllegalArgumentException(Messages.null_phaseset);

		if (operands == null)
			throw new IllegalArgumentException(Messages.null_operands);

		if (context == null)
			context = new ProvisioningContext();

		if (monitor == null)
			monitor = new NullProgressMonitor();

		SimpleProfileRegistry profileRegistry = (SimpleProfileRegistry) ServiceHelper.getService(EngineActivator.getContext(), IProfileRegistry.class.getName());

		Profile profile = profileRegistry.validate(iprofile);

		profileRegistry.lockProfile(profile);
		try {
			eventBus.publishEvent(new BeginOperationEvent(profile, phaseSet, operands, this));

			EngineSession session = new EngineSession(profile, context, actionManager);

			MultiStatus result = phaseSet.perform(session, profile, operands, context, monitor);
			if (result.matches(IStatus.ERROR | IStatus.CANCEL)) {
				eventBus.publishEvent(new RollbackOperationEvent(profile, phaseSet, operands, this, result));
				IStatus status = session.rollback();
				if (status.matches(IStatus.ERROR))
					LogHelper.log(status);
			} else {
				if (profile.isChanged())
					profileRegistry.updateProfile(profile);
				eventBus.publishEvent(new CommitOperationEvent(profile, phaseSet, operands, this));
				IStatus status = session.commit();
				if (status.matches(IStatus.ERROR))
					LogHelper.log(status);
			}
			//if there is only one child status, return that status instead because it will have more context
			IStatus[] children = result.getChildren();
			return children.length == 1 ? children[0] : result;
		} finally {
			profileRegistry.unlockProfile(profile);
			profile.setChanged(false);
		}
	}
}
