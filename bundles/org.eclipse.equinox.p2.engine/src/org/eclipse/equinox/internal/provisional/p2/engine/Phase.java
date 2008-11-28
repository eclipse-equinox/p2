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

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.osgi.util.NLS;

public abstract class Phase {
	protected static final String PARM_OPERAND = "operand"; //$NON-NLS-1$
	protected static final String PARM_PHASE_ID = "phaseId"; //$NON-NLS-1$
	protected static final String PARM_PROFILE = "profile"; //$NON-NLS-1$
	protected static final String PARM_CONTEXT = "context"; //$NON-NLS-1$

	protected final String phaseId;
	protected final int weight;
	protected int prePerformWork = 1000;
	protected int mainPerformWork = 10000;
	protected int postPerformWork = 1000;
	private Map phaseParameters = new HashMap();
	private Map touchpointToTouchpointPhaseParameters = new HashMap();
	private Map touchpointToTouchpointOperandParameters = new HashMap();

	protected Phase(String phaseId, int weight) {
		if (phaseId == null || phaseId.length() == 0)
			throw new IllegalArgumentException(Messages.phaseid_not_set);
		if (weight <= 0)
			throw new IllegalArgumentException(Messages.phaseid_not_positive);
		this.weight = weight;
		this.phaseId = phaseId;
	}

	public String toString() {
		return getClass().getName() + " - " + this.weight; //$NON-NLS-1$
	}

	public final MultiStatus perform(EngineSession session, IProfile profile, Operand[] operands, ProvisioningContext context, IProgressMonitor monitor) {
		MultiStatus status = new MultiStatus(EngineActivator.ID, IStatus.OK, null, null);
		try {
			perform(status, session, profile, operands, context, monitor);
		} catch (OperationCanceledException e) {
			// propagate operation cancellation
			status.add(new Status(IStatus.CANCEL, EngineActivator.ID, e.getMessage(), e));
		} catch (RuntimeException e) {
			// "perform" calls user code and might throw an unchecked exception
			// we catch the error here to gather information on where the problem occurred.
			status.add(new Status(IStatus.ERROR, EngineActivator.ID, e.getMessage(), e));
		} catch (LinkageError e) {
			// Catch linkage errors as these are generally recoverable but let other Errors propagate (see bug 222001)
			status.add(new Status(IStatus.ERROR, EngineActivator.ID, e.getMessage(), e));
		}

		if (status.matches(IStatus.CANCEL)) {
			MultiStatus result = new MultiStatus(EngineActivator.ID, IStatus.CANCEL, Messages.Engine_Operation_Canceled_By_User, null);
			result.merge(status);
			return result;
		} else if (status.matches(IStatus.ERROR)) {
			MultiStatus result = new MultiStatus(EngineActivator.ID, IStatus.ERROR, getProblemMessage(), null);
			result.merge(status);
			return result;
		}
		return status;
	}

	void perform(MultiStatus status, EngineSession session, IProfile profile, Operand[] operands, ProvisioningContext context, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, prePerformWork + mainPerformWork + postPerformWork);
		prePerform(status, profile, context, subMonitor.newChild(prePerformWork));
		if (status.matches(IStatus.ERROR | IStatus.CANCEL))
			return;
		session.recordPhaseStart(this);

		subMonitor.setWorkRemaining(mainPerformWork + postPerformWork);
		mainPerform(status, session, profile, operands, context, subMonitor.newChild(mainPerformWork));
		if (status.matches(IStatus.ERROR | IStatus.CANCEL))
			return;

		session.recordPhaseEnd(this);
		subMonitor.setWorkRemaining(postPerformWork);
		postPerform(status, profile, context, subMonitor.newChild(postPerformWork));
		if (status.matches(IStatus.ERROR | IStatus.CANCEL))
			return;

		subMonitor.done();
	}

	void prePerform(MultiStatus status, IProfile profile, ProvisioningContext context, IProgressMonitor monitor) {
		phaseParameters.put(PARM_PROFILE, profile);
		phaseParameters.put(PARM_CONTEXT, context);
		phaseParameters.put(PARM_PHASE_ID, phaseId);

		mergeStatus(status, initializePhase(monitor, profile, phaseParameters));
	}

	private void mainPerform(MultiStatus status, EngineSession session, IProfile profile, Operand[] operands, ProvisioningContext context, SubMonitor subMonitor) {
		subMonitor.beginTask("", operands.length); //$NON-NLS-1$
		for (int i = 0; i < operands.length; i++) {
			subMonitor.setWorkRemaining(operands.length - i);
			if (subMonitor.isCanceled())
				throw new OperationCanceledException();
			Operand operand = operands[i];
			if (!isApplicable(operand))
				continue;

			ProvisioningAction[] actions = getActions(operand);
			Map operandParameters = new HashMap(phaseParameters);
			operandParameters.put(PARM_OPERAND, operand);
			mergeStatus(status, initializeOperand(profile, operand, operandParameters, subMonitor));
			if (status.matches(IStatus.ERROR | IStatus.CANCEL))
				return;

			operandParameters = Collections.unmodifiableMap(operandParameters);
			if (actions != null) {
				for (int j = 0; j < actions.length; j++) {
					ProvisioningAction action = actions[j];
					Map parameters = operandParameters;
					Touchpoint touchpoint = action.getTouchpoint();
					if (touchpoint != null) {
						mergeStatus(status, initializeTouchpointParameters(profile, operand, operandParameters, touchpoint, subMonitor));
						if (status.matches(IStatus.ERROR | IStatus.CANCEL))
							return;

						parameters = (Map) touchpointToTouchpointOperandParameters.get(touchpoint);
					}
					session.recordAction(action, operand);
					mergeStatus(status, action.execute(parameters));
					if (status.matches(IStatus.ERROR | IStatus.CANCEL))
						return;
				}
			}

			mergeStatus(status, completeOperand(profile, operand, operandParameters, subMonitor));
			if (status.matches(IStatus.ERROR | IStatus.CANCEL))
				return;
			subMonitor.worked(1);
		}
	}

	private IStatus initializeTouchpointParameters(IProfile profile, Operand operand, Map operandParameters, Touchpoint touchpoint, IProgressMonitor monitor) {
		if (touchpointToTouchpointOperandParameters.containsKey(touchpoint))
			return Status.OK_STATUS;

		Map touchpointPhaseParameters = (Map) touchpointToTouchpointPhaseParameters.get(touchpoint);
		if (touchpointPhaseParameters == null) {
			touchpointPhaseParameters = new HashMap(phaseParameters);
			IStatus status = touchpoint.initializePhase(monitor, profile, phaseId, touchpointPhaseParameters);
			if (status != null && status.matches(IStatus.ERROR | IStatus.CANCEL))
				return status;
			touchpointToTouchpointPhaseParameters.put(touchpoint, touchpointPhaseParameters);
		}

		Map touchpointOperandParameters = new HashMap(touchpointPhaseParameters);
		touchpointOperandParameters.putAll(operandParameters);
		IStatus status = touchpoint.initializeOperand(profile, operand, touchpointOperandParameters);
		if (status != null && status.matches(IStatus.ERROR | IStatus.CANCEL))
			return status;
		touchpointToTouchpointOperandParameters.put(touchpoint, touchpointOperandParameters);
		return Status.OK_STATUS;
	}

	/**
	 * Merges a given IStatus into a MultiStatus
	 */
	protected static void mergeStatus(MultiStatus multi, IStatus status) {
		if (status != null && !status.isOK())
			multi.merge(status);
	}

	void postPerform(MultiStatus status, IProfile profile, ProvisioningContext context, IProgressMonitor monitor) {
		mergeStatus(status, completePhase(monitor, profile, phaseParameters));
		phaseParameters.clear();
	}

	void undo(MultiStatus status, EngineSession session, IProfile profile, Operand operand, ProvisioningAction[] actions, ProvisioningContext context) {
		Map operandParameters = new HashMap(phaseParameters);
		operandParameters.put(PARM_OPERAND, operand);
		mergeStatus(status, initializeOperand(profile, operand, operandParameters, new NullProgressMonitor()));
		operandParameters = Collections.unmodifiableMap(operandParameters);
		for (int j = 0; j < actions.length; j++) {
			ProvisioningAction action = actions[j];
			Map parameters = operandParameters;
			Touchpoint touchpoint = action.getTouchpoint();
			if (touchpoint != null) {
				mergeStatus(status, initializeTouchpointParameters(profile, operand, operandParameters, touchpoint, new NullProgressMonitor()));
				if (status.matches(IStatus.ERROR))
					return;

				parameters = (Map) touchpointToTouchpointOperandParameters.get(touchpoint);
			}
			mergeStatus(status, action.undo(parameters));
			// TODO: session.removeAction(...)
		}
		mergeStatus(status, completeOperand(profile, operand, operandParameters, new NullProgressMonitor()));
	}

	protected boolean isApplicable(Operand operand) {
		return true;
	}

	protected IStatus initializePhase(IProgressMonitor monitor, IProfile profile, Map parameters) {
		return Status.OK_STATUS;
	}

	protected IStatus completePhase(IProgressMonitor monitor, IProfile profile, Map parameters) {
		if (touchpointToTouchpointPhaseParameters == null)
			return Status.OK_STATUS;

		MultiStatus status = new MultiStatus(EngineActivator.ID, IStatus.OK, null, null);
		for (Iterator it = touchpointToTouchpointPhaseParameters.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			Touchpoint touchpoint = (Touchpoint) entry.getKey();
			Map touchpointParameters = (Map) entry.getValue();
			mergeStatus(status, touchpoint.completePhase(monitor, profile, phaseId, touchpointParameters));
		}
		touchpointToTouchpointPhaseParameters.clear();
		return status;
	}

	protected IStatus completeOperand(IProfile profile, Operand operand, Map parameters, IProgressMonitor monitor) {
		if (touchpointToTouchpointOperandParameters == null)
			return Status.OK_STATUS;

		MultiStatus status = new MultiStatus(EngineActivator.ID, IStatus.OK, null, null);
		for (Iterator it = touchpointToTouchpointOperandParameters.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			Touchpoint touchpoint = (Touchpoint) entry.getKey();
			Map touchpointParameters = (Map) entry.getValue();
			mergeStatus(status, touchpoint.completeOperand(profile, operand, touchpointParameters));
		}
		touchpointToTouchpointOperandParameters.clear();
		return status;
	}

	protected IStatus initializeOperand(IProfile profile, Operand operand, Map parameters, IProgressMonitor monitor) {
		return Status.OK_STATUS;
	}

	protected abstract ProvisioningAction[] getActions(Operand operand);

	/**
	 * Returns a human-readable message to be displayed in case of an error performing
	 * this phase. Subclasses should override.
	 */
	protected String getProblemMessage() {
		return NLS.bind(Messages.phase_error, getClass().getName());
	}

}
