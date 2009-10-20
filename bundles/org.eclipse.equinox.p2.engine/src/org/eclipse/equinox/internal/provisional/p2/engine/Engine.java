/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.engine;

import java.io.File;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;

/**
 * TODO Move concrete class to non-API package
 */
public class Engine implements IEngine {
	private static final String ENGINE = "engine"; //$NON-NLS-1$

	private ActionManager actionManager;
	private final IProvisioningEventBus eventBus;

	private SimpleProfileRegistry profileRegistry;

	public Engine(IProvisioningEventBus eventBus) {
		this.eventBus = eventBus;
		this.actionManager = new ActionManager();
	}

	private void checkArguments(IProfile iprofile, PhaseSet phaseSet, Operand[] operands, ProvisioningContext context, IProgressMonitor monitor) {
		if (iprofile == null)
			throw new IllegalArgumentException(Messages.null_profile);

		if (phaseSet == null)
			throw new IllegalArgumentException(Messages.null_phaseset);

		if (operands == null)
			throw new IllegalArgumentException(Messages.null_operands);
	}

	public IStatus perform(IProfile iprofile, PhaseSet phaseSet, Operand[] operands, ProvisioningContext context, IProgressMonitor monitor) {
		checkArguments(iprofile, phaseSet, operands, context, monitor);

		if (context == null)
			context = new ProvisioningContext();

		if (monitor == null)
			monitor = new NullProgressMonitor();

		Profile profile = profileRegistry.validate(iprofile);

		profileRegistry.lockProfile(profile);
		try {
			eventBus.publishEvent(new BeginOperationEvent(profile, phaseSet, operands, this));
			if (DebugHelper.DEBUG_ENGINE)
				DebugHelper.debug(ENGINE, "Beginning engine operation for profile=" + profile.getProfileId() + " [" + profile.getTimestamp() + "]:" + DebugHelper.LINE_SEPARATOR + DebugHelper.formatOperation(phaseSet, operands, context)); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$

			File profileDataDirectory = profileRegistry.getProfileDataDirectory(profile.getProfileId());

			EngineSession session = new EngineSession(profile, profileDataDirectory, context);

			MultiStatus result = phaseSet.perform(actionManager, session, profile, operands, context, monitor);
			if (result.isOK() || result.matches(IStatus.INFO | IStatus.WARNING)) {
				if (DebugHelper.DEBUG_ENGINE)
					DebugHelper.debug(ENGINE, "Preparing to commit engine operation for profile=" + profile.getProfileId()); //$NON-NLS-1$
				result.merge(session.prepare(monitor));
			}
			if (result.matches(IStatus.ERROR | IStatus.CANCEL)) {
				if (DebugHelper.DEBUG_ENGINE)
					DebugHelper.debug(ENGINE, "Rolling back engine operation for profile=" + profile.getProfileId() + ". Reason was: " + result.toString()); //$NON-NLS-1$ //$NON-NLS-2$
				IStatus status = session.rollback(actionManager, monitor, result.getSeverity());
				if (status.matches(IStatus.ERROR))
					LogHelper.log(status);
				eventBus.publishEvent(new RollbackOperationEvent(profile, phaseSet, operands, this, result));
			} else {
				if (DebugHelper.DEBUG_ENGINE)
					DebugHelper.debug(ENGINE, "Committing engine operation for profile=" + profile.getProfileId()); //$NON-NLS-1$
				if (profile.isChanged())
					profileRegistry.updateProfile(profile);
				IStatus status = session.commit(monitor);
				if (status.matches(IStatus.ERROR))
					LogHelper.log(status);
				eventBus.publishEvent(new CommitOperationEvent(profile, phaseSet, operands, this));
			}
			//if there is only one child status, return that status instead because it will have more context
			IStatus[] children = result.getChildren();
			return children.length == 1 ? children[0] : result;
		} finally {
			profileRegistry.unlockProfile(profile);
			profile.setChanged(false);
		}
	}

	public void setProfileRegistry(IProfileRegistry registry) {
		//we can only work with our own registry implementation
		if (registry instanceof SimpleProfileRegistry)
			this.profileRegistry = (SimpleProfileRegistry) registry;
	}

	public void unsetProfileRegistry(IProfileRegistry registry) {
		if (this.profileRegistry == registry)
			this.profileRegistry = null;
	}

	public IStatus validate(IProfile iprofile, PhaseSet phaseSet, Operand[] operands, ProvisioningContext context, IProgressMonitor monitor) {
		checkArguments(iprofile, phaseSet, operands, context, monitor);

		if (context == null)
			context = new ProvisioningContext();

		if (monitor == null)
			monitor = new NullProgressMonitor();

		return phaseSet.validate(actionManager, iprofile, operands, context, monitor);
	}
}
