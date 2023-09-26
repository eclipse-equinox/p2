/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
package org.eclipse.equinox.internal.p2.engine;

import java.util.Arrays;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.equinox.internal.provisional.p2.core.eventbus.IProvisioningEventBus;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.*;

/**
 * Concrete implementation of the {@link IEngine} API.
 */
public class Engine implements IEngine {

	private static final String ENGINE = "engine"; //$NON-NLS-1$
	private IProvisioningAgent agent;

	public Engine(IProvisioningAgent agent) {
		this.agent = agent;
		agent.registerService(ActionManager.SERVICE_NAME, new ActionManager());
	}

	private void checkArguments(IProfile iprofile, PhaseSet phaseSet, Operand[] operands) {
		if (iprofile == null)
			throw new IllegalArgumentException(Messages.null_profile);

		if (phaseSet == null)
			throw new IllegalArgumentException(Messages.null_phaseset);

		if (operands == null)
			throw new IllegalArgumentException(Messages.null_operands);
	}

	@Override
	public IStatus perform(IProvisioningPlan plan, IPhaseSet phaseSet, IProgressMonitor monitor) {
		return perform(plan.getProfile(), phaseSet, ((ProvisioningPlan) plan).getOperands(), plan.getContext(), monitor);
	}

	@Override
	public IStatus perform(IProvisioningPlan plan, IProgressMonitor monitor) {
		return perform(plan, PhaseSetFactory.createDefaultPhaseSet(), monitor);
	}

	public IStatus perform(IProfile iprofile, IPhaseSet phases, Operand[] operands, ProvisioningContext context, IProgressMonitor monitor) {
		PhaseSet phaseSet = (PhaseSet) phases;
		checkArguments(iprofile, phaseSet, operands);
		if (operands.length == 0)
			return Status.OK_STATUS;
		SimpleProfileRegistry profileRegistry = (SimpleProfileRegistry) agent.getService(IProfileRegistry.class);
		IProvisioningEventBus eventBus = agent.getService(IProvisioningEventBus.class);

		if (context == null)
			context = new ProvisioningContext(agent);

		if (monitor == null)
			monitor = new NullProgressMonitor();

		Profile profile = profileRegistry.validate(iprofile);

		profileRegistry.lockProfile(profile);
		SubMonitor subMon = SubMonitor.convert(monitor, 3);
		try {
			eventBus.publishEvent(new BeginOperationEvent(profile, phaseSet, operands, this));
			if (DebugHelper.DEBUG_ENGINE)
				DebugHelper.debug(ENGINE, "Beginning engine operation for profile=" + profile.getProfileId() + " [" + profile.getTimestamp() + "]:" + DebugHelper.LINE_SEPARATOR + DebugHelper.formatOperation(phaseSet, operands, context)); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$

			EngineSession session = new EngineSession(agent, profile, context);

			// If the property is set already in the context, respect that value.
			String property = context.getProperty(ProvisioningContext.CHECK_AUTHORITIES);
			if (property == null) {
				// Allow a system property to force the property.
				property = EngineActivator.getContext().getProperty(ProvisioningContext.CHECK_AUTHORITIES);
				if (property == null) {
					// Otherwise, if we are checking trust, also check the authorities.
					if (Arrays.asList(phases.getPhaseIds()).contains(PhaseSetFactory.PHASE_CHECK_TRUST)) {
						property = Boolean.TRUE.toString();
					}
				}
				context.setProperty(ProvisioningContext.CHECK_AUTHORITIES, property);
			}
			MultiStatus result = phaseSet.perform(session, operands, subMon.split(1));
			if (result.isOK() || result.matches(IStatus.INFO | IStatus.WARNING)) {
				if (DebugHelper.DEBUG_ENGINE)
					DebugHelper.debug(ENGINE, "Preparing to commit engine operation for profile=" + profile.getProfileId()); //$NON-NLS-1$
				result.merge(session.prepare(subMon.split(1)));
			}
			subMon.setWorkRemaining(1);
			if (result.matches(IStatus.ERROR | IStatus.CANCEL)) {
				if (DebugHelper.DEBUG_ENGINE)
					DebugHelper.debug(ENGINE, "Rolling back engine operation for profile=" + profile.getProfileId() + ". Reason was: " + result.toString()); //$NON-NLS-1$ //$NON-NLS-2$
				IStatus status = session.rollback(subMon.split(1), result.getSeverity());
				if (status.matches(IStatus.ERROR))
					LogHelper.log(status);
				eventBus.publishEvent(new RollbackOperationEvent(profile, phaseSet, operands, this, result));
			} else {
				if (DebugHelper.DEBUG_ENGINE)
					DebugHelper.debug(ENGINE, "Committing engine operation for profile=" + profile.getProfileId()); //$NON-NLS-1$
				if (profile.isChanged())
					profileRegistry.updateProfile(profile);
				IStatus status = session.commit(subMon.split(1));
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
			monitor.done();
		}
	}

	protected IStatus validate(IProfile iprofile, PhaseSet phaseSet, Operand[] operands, ProvisioningContext context, IProgressMonitor monitor) {
		checkArguments(iprofile, phaseSet, operands);

		if (context == null)
			context = new ProvisioningContext(agent);

		if (monitor == null)
			monitor = new NullProgressMonitor();

		ActionManager actionManager = agent.getService(ActionManager.class);
		return phaseSet.validate(actionManager, iprofile, operands, context, monitor);
	}

	@Override
	public IProvisioningPlan createPlan(IProfile profile, ProvisioningContext context) {
		return new ProvisioningPlan(profile, null, context);
	}
}
