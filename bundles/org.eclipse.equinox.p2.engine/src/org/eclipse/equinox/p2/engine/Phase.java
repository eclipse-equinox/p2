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
import org.eclipse.equinox.internal.p2.engine.Messages;
import org.eclipse.equinox.internal.p2.engine.TouchpointManager;
import org.eclipse.equinox.p2.core.helpers.MultiStatus;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;

public abstract class Phase {
	protected final String phaseId;
	protected final int weight;
	protected final String phaseName;
	protected int PRE_PERFORM_WORK = 1000;
	protected int PERFORM_WORK = 10000;
	protected int POST_PERFORM_WORK = 1000;
	private Map phaseParameters;
	private Map touchpointToTouchpointParameters;

	protected Phase(String phaseId, int weight, String phaseName) {
		if (phaseId == null || phaseId.length() == 0) {
			throw new IllegalArgumentException("Phase id must be set.");
		}

		if (weight <= 0) {
			throw new IllegalArgumentException("Phase weight must be positive.");
		}

		if (phaseName == null || phaseName.length() == 0) {
			throw new IllegalArgumentException("Phase name must be set.");
		}

		this.weight = weight;
		this.phaseName = phaseName;
		this.phaseId = phaseId;
	}

	public String toString() {
		return "Phase: " + this.phaseName + " - " + this.weight; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public final MultiStatus perform(EngineSession session, Profile profile, Operand[] deltas, IProgressMonitor monitor) {
		MultiStatus status = new MultiStatus();
		perform(status, session, profile, deltas, monitor);
		if (status.matches(IStatus.CANCEL)) {
			status.setMessage(Messages.Engine_Operation_Canceled_By_User);
		} else if (status.matches(IStatus.ERROR)) {
			status.setMessage(NLS.bind(Messages.Engine_Error_During_Phase, this.phaseName));
		}
		return status;
	}

	void undoActions(MultiStatus status, ProvisioningAction[] actions, Operand operand) {
		Touchpoint touchpoint = getTouchpoint(operand);
		Map touchpointParameters = (Map) touchpointToTouchpointParameters.get(touchpoint);
		Map parameters = new HashMap(touchpointParameters);
		parameters.put("operand", operand);
		status.add(initializeOperand(operand, parameters));
		status.add(touchpoint.initializeOperand(operand, phaseId, parameters));
		parameters = Collections.unmodifiableMap(parameters);
		for (int j = 0; j < actions.length; j++) {
			ProvisioningAction action = actions[j];
			IStatus actionStatus = action.undo(parameters);
			status.add(actionStatus);
		}
		status.add(touchpoint.completeOperand(operand, phaseId, parameters));
		status.add(completeOperand(operand, parameters));
	}

	void perform(MultiStatus status, EngineSession session, Profile profile, Operand[] operands, IProgressMonitor monitor) {
		touchpointToTouchpointParameters = new HashMap();
		for (int i = 0; i < operands.length; i++) {
			Touchpoint touchpoint = getTouchpoint(operands[i]);
			if (touchpoint == null)
				continue;

			if (!touchpointToTouchpointParameters.containsKey(touchpoint)) {
				touchpointToTouchpointParameters.put(touchpoint, null);
			}
		}

		SubMonitor subMonitor = SubMonitor.convert(monitor, PRE_PERFORM_WORK + PERFORM_WORK + POST_PERFORM_WORK);
		prePerform(status, profile, subMonitor.newChild(PRE_PERFORM_WORK));
		if (status.isErrorOrCancel())
			return;
		session.recordPhaseStart(this);

		subMonitor.setWorkRemaining(PERFORM_WORK + POST_PERFORM_WORK);
		mainPerform(status, session, profile, operands, subMonitor.newChild(PERFORM_WORK));
		if (status.isErrorOrCancel())
			return;

		session.recordPhaseEnd(this);
		subMonitor.setWorkRemaining(POST_PERFORM_WORK);
		postPerform(status, profile, subMonitor.newChild(POST_PERFORM_WORK));
		if (status.isErrorOrCancel())
			return;

		subMonitor.done();
	}

	void prePerform(MultiStatus status, Profile profile, IProgressMonitor monitor) {
		phaseParameters = new HashMap();
		phaseParameters.put("profile", profile);
		phaseParameters.put("phaseId", phaseId);
		status.add(initializePhase(monitor, profile, phaseParameters));

		for (Iterator it = touchpointToTouchpointParameters.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			Touchpoint touchpoint = (Touchpoint) entry.getKey();
			Map touchpointParameters = new HashMap(phaseParameters);
			touchpointParameters.put("touchpoint", touchpoint);
			status.add(touchpoint.initializePhase(monitor, profile, phaseId, touchpointParameters));
			entry.setValue(touchpointParameters);
		}
	}

	private void mainPerform(MultiStatus status, EngineSession session, Profile profile, Operand[] operands, SubMonitor subMonitor) {
		// TODO: Support Monitor
		// int operandWork = PERFORM_WORK / operands.length;
		for (int i = 0; i < operands.length; i++) {
			if (subMonitor.isCanceled())
				throw new OperationCanceledException();
			Operand operand = operands[i];
			if (!isApplicable(operand))
				continue;

			Touchpoint touchpoint = getTouchpoint(operand);

			ProvisioningAction[] actions;
			try {
				actions = getActions(touchpoint, operand);
			} catch (Throwable t) {
				status.add(new Status(IStatus.ERROR, phaseId, t.getMessage()));
				return;
			}

			Map touchpointParameters = (Map) touchpointToTouchpointParameters.get(touchpoint);
			Map parameters = new HashMap(touchpointParameters);
			parameters.put("operand", operand);
			status.add(initializeOperand(operand, parameters));
			status.add(touchpoint.initializeOperand(operand, phaseId, parameters));
			parameters = Collections.unmodifiableMap(parameters);
			if (actions != null) {
				for (int j = 0; j < actions.length; j++) {
					ProvisioningAction action = actions[j];
					status.add(action.execute(parameters));
					if (!status.isOK())
						return;

					session.recordAction(action, operand);
				}
			}
			status.add(touchpoint.completeOperand(operand, phaseId, parameters));
			status.add(completeOperand(operand, parameters));
		}
	}

	void postPerform(MultiStatus status, Profile profile, IProgressMonitor monitor) {
		for (Iterator it = touchpointToTouchpointParameters.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			Touchpoint touchpoint = (Touchpoint) entry.getKey();
			Map touchpointParameters = (Map) entry.getValue();
			status.add(touchpoint.completePhase(monitor, profile, phaseId, touchpointParameters));
			entry.setValue(null);
		}
		status.add(completePhase(monitor, profile, phaseParameters));
		phaseParameters = null;
	}

	private static Touchpoint getTouchpoint(Operand operand) {
		IInstallableUnit unit = operand.second();
		if (unit == null)
			unit = operand.first();

		if (unit == null)
			return null;
		TouchpointManager touchpointManager = TouchpointManager.getInstance();
		Touchpoint touchpoint = touchpointManager.getTouchpoint(unit.getTouchpointType());
		return touchpoint;
	}

	protected boolean isApplicable(Operand op) {
		return true;
	}

	protected IStatus initializePhase(IProgressMonitor monitor, Profile profile, Map parameters) {
		return Status.OK_STATUS;
	}

	protected IStatus completePhase(IProgressMonitor monitor, Profile profile, Map parameters) {
		return Status.OK_STATUS;
	}

	protected IStatus completeOperand(Operand operand, Map parameters) {
		return Status.OK_STATUS;
	}

	protected IStatus initializeOperand(Operand operand, Map parameters) {
		return Status.OK_STATUS;
	}

	public ProvisioningAction getAction(String actionId) {
		return null;
	}

	protected abstract ProvisioningAction[] getActions(Touchpoint touchpoint, Operand currentOperand);
}