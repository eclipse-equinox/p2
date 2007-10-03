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

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.TouchpointManager;
import org.eclipse.equinox.p2.core.helpers.MultiStatus;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

// An operation that is applied to a set of IUs.
public abstract class IUPhase extends Phase {
	protected int PRE_PERFORM_WORK = 1000;
	protected int PERFORM_WORK = 10000;
	protected int POST_PERFORM_WORK = 1000;

	private Map phaseParameters = new HashMap();
	private Map touchpoints = new HashMap();

	protected IUPhase(String phaseId, int weight, String phaseName) {
		super(phaseId, weight, phaseName);
	}

	protected void perform(MultiStatus status, EngineSession session, Profile profile, Operand[] operands, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, PRE_PERFORM_WORK + PERFORM_WORK + POST_PERFORM_WORK);
		prePerform(status, profile, operands, subMonitor.newChild(PRE_PERFORM_WORK));
		if (status.isErrorOrCancel())
			return;

		subMonitor.setWorkRemaining(PERFORM_WORK + POST_PERFORM_WORK);
		mainPerform(status, session, profile, operands, subMonitor.newChild(PERFORM_WORK));
		if (status.isErrorOrCancel())
			return;

		subMonitor.setWorkRemaining(POST_PERFORM_WORK);
		postPerform(status, profile, operands, subMonitor.newChild(POST_PERFORM_WORK));
		if (status.isErrorOrCancel())
			return;

		subMonitor.done();
	}

	private void prePerform(MultiStatus status, Profile profile, Operand[] deltas, IProgressMonitor monitor) {
		status.add(initializePhase(monitor, profile, phaseParameters));
		// TODO: Consider moving touchpoint discovery up -- perhaps to session??
		// TODO: Support Monitor
		for (int i = 0; i < deltas.length; i++) {
			ITouchpoint touchpoint = getTouchpoint(deltas[i]);
			if (touchpoint == null)
				continue;

			if (!touchpoints.containsKey(touchpoint) && touchpoint.supports(phaseId)) {
				Map touchpointParameters = new HashMap(phaseParameters);
				status.add(touchpoint.initializePhase(monitor, profile, phaseId, touchpointParameters));
				touchpoints.put(touchpoint, touchpointParameters);
			}
		}
	}

	private void mainPerform(MultiStatus status, EngineSession session, Profile profile, Operand[] operands, SubMonitor subMonitor) {
		// TODO: Support Monitor
		// int operandWork = PERFORM_WORK / operands.length;
		for (int i = 0; i < operands.length; i++) {
			if (subMonitor.isCanceled())
				throw new OperationCanceledException();
			Operand currentOperand = operands[i];
			if (!isApplicable(currentOperand))
				continue;

			ITouchpoint touchpoint = getTouchpoint(currentOperand);
			if (touchpoint == null || !touchpoint.supports(phaseId))
				continue;

			ITouchpointAction[] actions = getActions(touchpoint, profile, currentOperand);
			Map touchpointParameters = (Map) touchpoints.get(touchpoint);
			Map parameters = new HashMap(touchpointParameters);
			for (int j = 0; j < actions.length; j++) {
				ITouchpointAction action = actions[j];
				IStatus actionStatus = action.execute(parameters);
				status.add(actionStatus);
				if (actionStatus != null && !actionStatus.isOK())
					return;

				session.record(action);
			}
		}
	}

	private void postPerform(MultiStatus status, Profile profile, Operand[] deltas, IProgressMonitor monitor) {
		for (Iterator it = touchpoints.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			ITouchpoint touchpoint = (ITouchpoint) entry.getKey();
			Map touchpointParameters = (Map) entry.getValue();
			status.add(touchpoint.completePhase(monitor, profile, phaseId, touchpointParameters));
		}
		status.add(completePhase(monitor, profile, phaseParameters));
	}

	protected ITouchpoint getTouchpoint(Operand operand) {
		IInstallableUnit unit = operand.second();
		if (unit == null)
			unit = operand.first();

		if (unit == null)
			return null;
		TouchpointManager touchpointManager = TouchpointManager.getInstance();
		ITouchpoint touchpoint = touchpointManager.getTouchpoint(unit.getTouchpointType());
		return touchpoint;
	}

	protected abstract boolean isApplicable(Operand op);

	protected abstract IStatus initializePhase(IProgressMonitor monitor, Profile profile, Map parameters);

	protected abstract IStatus completePhase(IProgressMonitor monitor, Profile profile, Map parameters);

	protected abstract ITouchpointAction[] getActions(ITouchpoint touchpoint, Profile profile, Operand currentOperand);
}
