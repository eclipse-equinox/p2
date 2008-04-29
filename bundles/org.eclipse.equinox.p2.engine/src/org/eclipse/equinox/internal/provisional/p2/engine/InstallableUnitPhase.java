/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     WindRiver - https://bugs.eclipse.org/bugs/show_bug.cgi?id=227372
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.engine;

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.osgi.util.NLS;

public abstract class InstallableUnitPhase extends Phase {
	protected static final String PARM_ARTIFACT_REQUESTS = "artifactRequests"; //$NON-NLS-1$
	protected static final String PARM_ARTIFACT = "artifact"; //$NON-NLS-1$
	protected static final String PARM_IU = "iu"; //$NON-NLS-1$
	protected static final String PARM_TOUCHPOINT = "touchpoint"; //$NON-NLS-1$

	protected ProvisioningContext provContext = null;
	private Map touchpointToTouchpointParameters;

	protected InstallableUnitPhase(String phaseId, int weight) {
		super(phaseId, weight);
	}

	void perform(MultiStatus status, EngineSession session, IProfile profile, Operand[] operands, ProvisioningContext context, IProgressMonitor monitor) {
		provContext = context;
		touchpointToTouchpointParameters = new HashMap();
		for (int i = 0; i < operands.length; i++) {
			if (!(operands[i] instanceof InstallableUnitOperand))
				continue;

			InstallableUnitOperand iuOperand = (InstallableUnitOperand) operands[i];
			TouchpointType type = getTouchpointType(iuOperand);
			Touchpoint touchpoint = TouchpointManager.getInstance().getTouchpoint(type);
			//abort the entire phase if any required touchpoint is missing
			if (touchpoint == null) {
				status.add(new Status(IStatus.ERROR, EngineActivator.ID, NLS.bind(Messages.required_touchpoint_not_found, type), null));
				return;
			}
			if (!touchpointToTouchpointParameters.containsKey(touchpoint)) {
				touchpointToTouchpointParameters.put(touchpoint, null);
			}
		}
		super.perform(status, session, profile, operands, context, monitor);
	}

	protected IStatus initializePhase(IProgressMonitor monitor, IProfile profile, Map parameters) {
		MultiStatus status = new MultiStatus(EngineActivator.ID, IStatus.OK, null, null);
		mergeStatus(status, initializeInstallableUnitPhase(monitor, profile, parameters));
		for (Iterator it = touchpointToTouchpointParameters.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			Touchpoint touchpoint = (Touchpoint) entry.getKey();
			Map touchpointParameters = new HashMap(parameters);
			touchpointParameters.put(PARM_TOUCHPOINT, touchpoint);
			mergeStatus(status, touchpoint.initializePhase(monitor, profile, phaseId, touchpointParameters));
			entry.setValue(touchpointParameters);
		}
		return status;
	}

	protected IStatus completePhase(IProgressMonitor monitor, IProfile profile, Map parameters) {
		MultiStatus status = new MultiStatus(EngineActivator.ID, IStatus.OK, null, null);
		for (Iterator it = touchpointToTouchpointParameters.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			Touchpoint touchpoint = (Touchpoint) entry.getKey();
			Map touchpointParameters = (Map) entry.getValue();
			mergeStatus(status, touchpoint.completePhase(monitor, profile, phaseId, touchpointParameters));
			entry.setValue(null);
		}
		mergeStatus(status, completeInstallableUnitPhase(monitor, profile, parameters));
		return status;
	}

	protected IStatus initializeOperand(IProfile profile, Operand operand, Map parameters, IProgressMonitor monitor) {
		if (!(operand instanceof InstallableUnitOperand))
			return null;

		InstallableUnitOperand iuOperand = (InstallableUnitOperand) operand;

		MultiStatus status = new MultiStatus(EngineActivator.ID, IStatus.OK, null, null);
		mergeStatus(status, initializeOperand(profile, iuOperand, parameters, monitor));
		Touchpoint touchpoint = getTouchpoint(iuOperand);
		Map touchpointParameters = (Map) touchpointToTouchpointParameters.get(touchpoint);
		if (touchpointParameters != null)
			parameters.putAll(touchpointParameters);
		mergeStatus(status, touchpoint.initializeOperand(profile, phaseId, iuOperand, parameters));
		return status;
	}

	protected IStatus completeOperand(IProfile profile, Operand operand, Map parameters, IProgressMonitor monitor) {
		if (!(operand instanceof InstallableUnitOperand))
			return null;

		InstallableUnitOperand iuOperand = (InstallableUnitOperand) operand;

		MultiStatus status = new MultiStatus(EngineActivator.ID, IStatus.OK, null, null);
		Touchpoint touchpoint = getTouchpoint(iuOperand);
		mergeStatus(status, touchpoint.completeOperand(profile, phaseId, iuOperand, parameters));
		mergeStatus(status, completeOperand(profile, iuOperand, parameters, monitor));
		return status;
	}

	protected ProvisioningAction[] getActions(Operand operand) {
		if (!(operand instanceof InstallableUnitOperand))
			return null;

		InstallableUnitOperand iuOperand = (InstallableUnitOperand) operand;
		return getActions(iuOperand);
	}

	protected boolean isApplicable(Operand operand) {
		if (!(operand instanceof InstallableUnitOperand))
			return false;

		InstallableUnitOperand iuOperand = (InstallableUnitOperand) operand;
		return isApplicable(iuOperand);
	}

	/**
	 * Returns the touchpoint corresponding to the operand, or null if no corresponding
	 * touchpoint is available.
	 */
	protected final static Touchpoint getTouchpoint(InstallableUnitOperand operand) {
		return TouchpointManager.getInstance().getTouchpoint(getTouchpointType(operand));
	}

	private static Touchpoint getTouchpoint(IInstallableUnit unit) {
		return TouchpointManager.getInstance().getTouchpoint(unit.getTouchpointType());
	}

	/**
	 * Returns the touchpoint type corresponding to the operand. Never returns null.
	 */
	protected final static TouchpointType getTouchpointType(InstallableUnitOperand operand) {
		IInstallableUnit unit = operand.second();
		if (unit == null)
			unit = operand.first();
		return unit.getTouchpointType();
	}

	protected final ProvisioningAction[] getActions(IInstallableUnit unit, String key) {
		String[] instructions = getInstructions(unit, key);
		if (instructions == null || instructions.length == 0)
			return null;
		Touchpoint touchpoint = getTouchpoint(unit);
		//TODO Likely need to propagate an exception if the touchpoint is not present
		if (touchpoint == null)
			return null;
		InstructionParser parser = new InstructionParser(this, touchpoint);
		List actions = new ArrayList();
		for (int i = 0; i < instructions.length; i++) {
			actions.addAll(Arrays.asList(parser.parseActions(instructions[i])));
		}
		return (ProvisioningAction[]) actions.toArray(new ProvisioningAction[actions.size()]);
	}

	private final static String[] getInstructions(IInstallableUnit unit, String key) {
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

	public ProvisioningAction getAction(String actionId) {
		return null;
	}

	protected abstract ProvisioningAction[] getActions(InstallableUnitOperand operand);

	protected boolean isApplicable(InstallableUnitOperand operand) {
		return true;
	}

	protected IStatus initializeInstallableUnitPhase(IProgressMonitor monitor, IProfile profile, Map parameters) {
		return Status.OK_STATUS;
	}

	protected IStatus completeInstallableUnitPhase(IProgressMonitor monitor, IProfile profile, Map parameters) {
		return Status.OK_STATUS;
	}

	protected IStatus initializeOperand(IProfile profile, InstallableUnitOperand operand, Map parameters, IProgressMonitor monitor) {
		return Status.OK_STATUS;
	}

	protected IStatus completeOperand(IProfile profile, InstallableUnitOperand operand, Map parameters, IProgressMonitor monitor) {
		return Status.OK_STATUS;
	}

}
