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
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;
import org.eclipse.osgi.util.NLS;

public abstract class Phase {
	protected static final String PARM_OPERAND = "operand"; //$NON-NLS-1$
	protected static final String PARM_TOUCHPOINT = "touchpoint"; //$NON-NLS-1$
	protected static final String PARM_PHASE_ID = "phaseId"; //$NON-NLS-1$
	protected static final String PARM_PROFILE = "profile"; //$NON-NLS-1$
	protected static final String PARM_ARTIFACT_REQUESTS = "artifactRequests"; //$NON-NLS-1$
	protected static final String PARM_ARTIFACT = "artifact"; //$NON-NLS-1$
	protected static final String PARM_IU = "iu"; //$NON-NLS-1$

	protected final String phaseId;
	protected final int weight;
	protected int prePerformWork = 1000;
	protected int mainPerformWork = 10000;
	protected int postPerformWork = 1000;
	private Map phaseParameters;
	private Map touchpointToTouchpointParameters;

	protected Phase(String phaseId, int weight) {
		if (phaseId == null || phaseId.length() == 0)
			throw new IllegalArgumentException("Phase id must be set."); //$NON-NLS-1$
		if (weight <= 0)
			throw new IllegalArgumentException("Phase weight must be positive."); //$NON-NLS-1$
		this.weight = weight;
		this.phaseId = phaseId;
	}

	public String toString() {
		return getClass().getName() + " - " + this.weight; //$NON-NLS-1$
	}

	public final MultiStatus perform(EngineSession session, IProfile profile, InstallableUnitOperand[] operands, IProgressMonitor monitor) {
		MultiStatus status = new MultiStatus(EngineActivator.ID, IStatus.OK, null, null);
		perform(status, session, profile, operands, monitor);
		if (status.matches(IStatus.CANCEL)) {
			MultiStatus result = new MultiStatus(EngineActivator.ID, IStatus.CANCEL, Messages.Engine_Operation_Canceled_By_User, null);
			result.merge(status);
			return result;
		} else if (status.matches(IStatus.ERROR)) {
			MultiStatus result = new MultiStatus(EngineActivator.ID, IStatus.CANCEL, getProblemMessage(), null);
			result.merge(status);
			return result;
		}
		return status;
	}

	void perform(MultiStatus status, EngineSession session, IProfile profile, InstallableUnitOperand[] operands, IProgressMonitor monitor) {
		touchpointToTouchpointParameters = new HashMap();
		for (int i = 0; i < operands.length; i++) {
			TouchpointType type = getTouchpointType(operands[i]);
			Touchpoint touchpoint = TouchpointManager.getInstance().getTouchpoint(type);
			//abort the entire phase if any required touchpoint is missing
			if (touchpoint == null) {
				status.add(new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(Messages.TouchpointManager_Required_Touchpoint_Not_Found, type), null));
				return;
			}
			if (!touchpointToTouchpointParameters.containsKey(touchpoint)) {
				touchpointToTouchpointParameters.put(touchpoint, null);
			}
		}

		SubMonitor subMonitor = SubMonitor.convert(monitor, prePerformWork + mainPerformWork + postPerformWork);
		prePerform(status, profile, subMonitor.newChild(prePerformWork));
		if (status.matches(IStatus.ERROR | IStatus.CANCEL))
			return;
		session.recordPhaseStart(this);

		subMonitor.setWorkRemaining(mainPerformWork + postPerformWork);
		mainPerform(status, session, profile, operands, subMonitor.newChild(mainPerformWork));
		if (status.matches(IStatus.ERROR | IStatus.CANCEL))
			return;

		session.recordPhaseEnd(this);
		subMonitor.setWorkRemaining(postPerformWork);
		postPerform(status, profile, subMonitor.newChild(postPerformWork));
		if (status.matches(IStatus.ERROR | IStatus.CANCEL))
			return;

		subMonitor.done();
	}

	void prePerform(MultiStatus status, IProfile profile, IProgressMonitor monitor) {
		phaseParameters = new HashMap();
		phaseParameters.put(PARM_PROFILE, profile);
		phaseParameters.put(PARM_PHASE_ID, phaseId);
		mergeStatus(status, initializePhase(monitor, profile, phaseParameters));

		for (Iterator it = touchpointToTouchpointParameters.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			Touchpoint touchpoint = (Touchpoint) entry.getKey();
			Map touchpointParameters = new HashMap(phaseParameters);
			touchpointParameters.put(PARM_TOUCHPOINT, touchpoint);
			mergeStatus(status, touchpoint.initializePhase(monitor, profile, phaseId, touchpointParameters));
			entry.setValue(touchpointParameters);
		}
	}

	private void mainPerform(MultiStatus status, EngineSession session, IProfile profile, InstallableUnitOperand[] operands, SubMonitor subMonitor) {
		subMonitor.beginTask("", operands.length); //$NON-NLS-1$
		for (int i = 0; i < operands.length; i++) {
			subMonitor.setWorkRemaining(operands.length - i);
			if (subMonitor.isCanceled())
				throw new OperationCanceledException();
			InstallableUnitOperand operand = operands[i];
			if (!isApplicable(operand))
				continue;

			ProvisioningAction[] actions;
			try {
				actions = getActions(operand);
			} catch (Throwable t) {
				//TODO Should never catch throwable. Use SafeRunner if calling third party code
				status.add(new Status(IStatus.ERROR, phaseId, t.getMessage(), t));
				return;
			}

			Touchpoint touchpoint = getTouchpoint(operand);
			Map parameters = (touchpoint != null) ? new HashMap((Map) touchpointToTouchpointParameters.get(touchpoint)) : new HashMap(phaseParameters);
			parameters.put(PARM_OPERAND, operand);
			mergeStatus(status, initializeOperand(profile, operand, parameters, subMonitor));
			if (touchpoint != null)
				mergeStatus(status, touchpoint.initializeOperand(profile, phaseId, operand, parameters));
			parameters = Collections.unmodifiableMap(parameters);
			if (actions != null) {
				for (int j = 0; j < actions.length; j++) {
					ProvisioningAction action = actions[j];
					session.recordAction(action, operand);
					mergeStatus(status, action.execute(parameters));
					if (!status.isOK())
						return;
				}
			}
			if (touchpoint != null)
				mergeStatus(status, touchpoint.completeOperand(profile, phaseId, operand, parameters));
			mergeStatus(status, completeOperand(operand, parameters));
			subMonitor.worked(1);
		}
	}

	/**
	 * Merges a given IStatus into a MultiStatus
	 */
	private void mergeStatus(MultiStatus multi, IStatus status) {
		if (status != null && !status.isOK())
			multi.add(status);
	}

	void postPerform(MultiStatus status, IProfile profile, IProgressMonitor monitor) {
		for (Iterator it = touchpointToTouchpointParameters.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			Touchpoint touchpoint = (Touchpoint) entry.getKey();
			Map touchpointParameters = (Map) entry.getValue();
			mergeStatus(status, touchpoint.completePhase(monitor, profile, phaseId, touchpointParameters));
			entry.setValue(null);
		}
		mergeStatus(status, completePhase(monitor, profile, phaseParameters));
		phaseParameters = null;
	}

	void undo(MultiStatus status, EngineSession session, IProfile profile, InstallableUnitOperand operand, ProvisioningAction[] actions) {
		Touchpoint touchpoint = getTouchpoint(operand);
		Map touchpointParameters = (Map) touchpointToTouchpointParameters.get(touchpoint);
		Map parameters = new HashMap(touchpointParameters);
		parameters.put(PARM_OPERAND, operand);
		mergeStatus(status, initializeOperand(profile, operand, parameters, new NullProgressMonitor()));
		mergeStatus(status, touchpoint.initializeOperand(profile, phaseId, operand, parameters));
		parameters = Collections.unmodifiableMap(parameters);
		for (int j = 0; j < actions.length; j++) {
			ProvisioningAction action = actions[j];
			mergeStatus(status, action.undo(parameters));
			// TODO: session.removeAction(...)
		}
		mergeStatus(status, touchpoint.completeOperand(profile, phaseId, operand, parameters));
		mergeStatus(status, completeOperand(operand, parameters));
	}

	protected final ProvisioningAction[] getActions(IInstallableUnit unit, String key) {
		String[] instructions = getInstructionsFor(unit, key);
		if (instructions == null || instructions.length == 0)
			return null;
		Touchpoint touchpoint = getTouchpoint(unit);
		//TODO Likely need to propagate an exception if the touchpoint is not present
		if (touchpoint == null)
			return null;
		InstructionParser parser = new InstructionParser(this, touchpoint);
		return parser.parseActions(instructions[0]);
	}

	protected boolean isApplicable(InstallableUnitOperand op) {
		return true;
	}

	protected IStatus initializePhase(IProgressMonitor monitor, IProfile profile, Map parameters) {
		return Status.OK_STATUS;
	}

	protected IStatus completePhase(IProgressMonitor monitor, IProfile profile, Map parameters) {
		return Status.OK_STATUS;
	}

	protected IStatus completeOperand(InstallableUnitOperand operand, Map parameters) {
		return Status.OK_STATUS;
	}

	protected IStatus initializeOperand(IProfile profile, InstallableUnitOperand operand, Map parameters, IProgressMonitor monitor) {
		return Status.OK_STATUS;
	}

	public ProvisioningAction getAction(String actionId) {
		return null;
	}

	protected abstract ProvisioningAction[] getActions(InstallableUnitOperand currentOperand);

	/**
	 * Returns a human-readable message to be displayed in case of an error performing
	 * this phase. Subclasses should override.
	 */
	protected String getProblemMessage() {
		return Messages.Phase_Error;
	}

	/**
	 * Returns the touchpoint corresponding to the operand, or null if no corresponding
	 * touchpoint is available.
	 */
	protected static Touchpoint getTouchpoint(InstallableUnitOperand operand) {
		return TouchpointManager.getInstance().getTouchpoint(getTouchpointType(operand));
	}

	/**
	 * Returns the touchpoint type corresponding to the operand. Never returns null.
	 */
	protected static TouchpointType getTouchpointType(InstallableUnitOperand operand) {
		IInstallableUnit unit = operand.second();
		if (unit == null)
			unit = operand.first();
		return unit.getTouchpointType();
	}

	private static Touchpoint getTouchpoint(IInstallableUnit unit) {
		return TouchpointManager.getInstance().getTouchpoint(unit.getTouchpointType());
	}

	private static String[] getInstructionsFor(IInstallableUnit unit, String key) {
		TouchpointData[] data = unit.getTouchpointData();
		if (data == null)
			return null;

		String[] matches = new String[data.length];
		int count = 0;
		for (int i = 0; i < data.length; i++) {
			matches[count] = data[i].getInstructions(key);
			if (matches[count] != null)
				count++;
		}
		if (count == data.length)
			return matches;
		String[] result = new String[count];
		System.arraycopy(matches, 0, result, 0, count);
		return result;
	}
}
