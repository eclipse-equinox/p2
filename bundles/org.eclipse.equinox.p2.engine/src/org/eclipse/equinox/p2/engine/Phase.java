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
import org.eclipse.equinox.p2.metadata.TouchpointData;
import org.eclipse.osgi.util.NLS;

public abstract class Phase {
	protected final String phaseId;
	protected final int weight;
	protected final String phaseName;
	protected int prePerformWork = 1000;
	protected int mainPerformWork = 10000;
	protected int postPerformWork = 1000;
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

	public final MultiStatus perform(EngineSession session, Profile profile, Operand[] operands, IProgressMonitor monitor) {
		MultiStatus status = new MultiStatus();
		perform(status, session, profile, operands, monitor);
		if (status.matches(IStatus.CANCEL)) {
			status.setMessage(Messages.Engine_Operation_Canceled_By_User);
		} else if (status.matches(IStatus.ERROR)) {
			status.setMessage(NLS.bind(Messages.Engine_Error_During_Phase, this.phaseName));
		}
		return status;
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

		SubMonitor subMonitor = SubMonitor.convert(monitor, prePerformWork + mainPerformWork + postPerformWork);
		prePerform(status, profile, subMonitor.newChild(prePerformWork));
		if (status.isErrorOrCancel())
			return;
		session.recordPhaseStart(this);

		subMonitor.setWorkRemaining(mainPerformWork + postPerformWork);
		mainPerform(status, session, profile, operands, subMonitor.newChild(mainPerformWork));
		if (status.isErrorOrCancel())
			return;

		session.recordPhaseEnd(this);
		subMonitor.setWorkRemaining(postPerformWork);
		postPerform(status, profile, subMonitor.newChild(postPerformWork));
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
		subMonitor.beginTask("", operands.length); //$NON-NLS-1$
		for (int i = 0; i < operands.length; i++) {
			subMonitor.setWorkRemaining(operands.length - i);
			if (subMonitor.isCanceled())
				throw new OperationCanceledException();
			Operand operand = operands[i];
			if (!isApplicable(operand))
				continue;

			ProvisioningAction[] actions;
			try {
				actions = getActions(operand);
			} catch (Throwable t) {
				status.add(new Status(IStatus.ERROR, phaseId, t.getMessage()));
				return;
			}

			Touchpoint touchpoint = getTouchpoint(operand);
			Map touchpointParameters = (Map) touchpointToTouchpointParameters.get(touchpoint);
			Map parameters = new HashMap(touchpointParameters);
			parameters.put("operand", operand);
			status.add(initializeOperand(profile, operand, parameters, subMonitor));
			status.add(touchpoint.initializeOperand(profile, phaseId, operand, parameters));
			parameters = Collections.unmodifiableMap(parameters);
			if (actions != null) {
				for (int j = 0; j < actions.length; j++) {
					ProvisioningAction action = actions[j];
					session.recordAction(action, operand);
					status.add(action.execute(parameters));
					if (!status.isOK())
						return;
				}
			}
			status.add(touchpoint.completeOperand(profile, phaseId, operand, parameters));
			status.add(completeOperand(operand, parameters));
			subMonitor.worked(1);
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

	void undo(MultiStatus status, EngineSession session, Profile profile, Operand operand, ProvisioningAction[] actions) {
		Touchpoint touchpoint = getTouchpoint(operand);
		Map touchpointParameters = (Map) touchpointToTouchpointParameters.get(touchpoint);
		Map parameters = new HashMap(touchpointParameters);
		parameters.put("operand", operand);
		status.add(initializeOperand(profile, operand, parameters, new NullProgressMonitor()));
		status.add(touchpoint.initializeOperand(profile, phaseId, operand, parameters));
		parameters = Collections.unmodifiableMap(parameters);
		for (int j = 0; j < actions.length; j++) {
			ProvisioningAction action = actions[j];
			IStatus actionStatus = action.undo(parameters);
			status.add(actionStatus);
			// TODO: session.removeAction(...)
		}
		status.add(touchpoint.completeOperand(profile, phaseId, operand, parameters));
		status.add(completeOperand(operand, parameters));
	}

	protected final ProvisioningAction[] getActions(IInstallableUnit unit, String key) {

		String[] instructions = getInstructionsFor(unit, key);
		if (instructions == null || instructions.length == 0)
			return null;

		TouchpointManager touchpointManager = TouchpointManager.getInstance();
		Touchpoint touchpoint = touchpointManager.getTouchpoint(unit.getTouchpointType());
		InstructionParser parser = new InstructionParser(this, touchpoint);
		return parser.parseActions(instructions[0]);
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

	protected IStatus initializeOperand(Profile profile, Operand operand, Map parameters, IProgressMonitor monitor) {
		return Status.OK_STATUS;
	}

	public ProvisioningAction getAction(String actionId) {
		return null;
	}

	protected abstract ProvisioningAction[] getActions(Operand currentOperand);

	protected static Touchpoint getTouchpoint(Operand operand) {
		IInstallableUnit unit = operand.second();
		if (unit == null)
			unit = operand.first();

		if (unit == null)
			return null;
		return getTouchpoint(unit);
	}

	private static Touchpoint getTouchpoint(IInstallableUnit unit) {
		TouchpointManager touchpointManager = TouchpointManager.getInstance();
		Touchpoint touchpoint = touchpointManager.getTouchpoint(unit.getTouchpointType());
		return touchpoint;
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