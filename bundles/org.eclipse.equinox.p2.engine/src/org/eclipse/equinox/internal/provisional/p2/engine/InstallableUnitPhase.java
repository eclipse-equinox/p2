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
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.osgi.util.NLS;

public abstract class InstallableUnitPhase extends Phase {
	public static final String PARM_ARTIFACT_REQUESTS = "artifactRequests"; //$NON-NLS-1$
	public static final String PARM_ARTIFACT = "artifact"; //$NON-NLS-1$
	public static final String PARM_IU = "iu"; //$NON-NLS-1$
	public static final String PARM_TOUCHPOINT = "touchpoint"; //$NON-NLS-1$

	protected InstallableUnitPhase(String phaseId, int weight) {
		super(phaseId, weight);
	}

	protected IStatus initializeOperand(IProfile profile, Operand operand, Map parameters, IProgressMonitor monitor) {
		InstallableUnitOperand iuOperand = (InstallableUnitOperand) operand;
		MultiStatus status = new MultiStatus(EngineActivator.ID, IStatus.OK, null, null);
		mergeStatus(status, initializeOperand(profile, iuOperand, parameters, monitor));
		mergeStatus(status, super.initializeOperand(profile, operand, parameters, monitor));
		return status;
	}

	protected IStatus initializeOperand(IProfile profile, InstallableUnitOperand operand, Map parameters, IProgressMonitor monitor) {
		return Status.OK_STATUS;
	}

	protected IStatus completeOperand(IProfile profile, Operand operand, Map parameters, IProgressMonitor monitor) {
		InstallableUnitOperand iuOperand = (InstallableUnitOperand) operand;

		MultiStatus status = new MultiStatus(EngineActivator.ID, IStatus.OK, null, null);
		mergeStatus(status, super.completeOperand(profile, iuOperand, parameters, monitor));
		mergeStatus(status, completeOperand(profile, iuOperand, parameters, monitor));
		return status;
	}

	protected IStatus completeOperand(IProfile profile, InstallableUnitOperand operand, Map parameters, IProgressMonitor monitor) {
		return Status.OK_STATUS;
	}

	final protected ProvisioningAction[] getActions(Operand operand) {
		if (!(operand instanceof InstallableUnitOperand))
			return null;

		InstallableUnitOperand iuOperand = (InstallableUnitOperand) operand;
		return getActions(iuOperand);
	}

	protected abstract ProvisioningAction[] getActions(InstallableUnitOperand operand);

	final protected boolean isApplicable(Operand operand) {
		if (!(operand instanceof InstallableUnitOperand))
			return false;

		InstallableUnitOperand iuOperand = (InstallableUnitOperand) operand;
		return isApplicable(iuOperand);
	}

	protected boolean isApplicable(InstallableUnitOperand operand) {
		return true;
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
	private final static TouchpointType getTouchpointType(InstallableUnitOperand operand) {
		IInstallableUnit unit = operand.second();
		if (unit == null)
			unit = operand.first();
		return unit.getTouchpointType();
	}

	protected final ProvisioningAction[] getActions(IInstallableUnit unit, String key) {
		TouchpointInstruction[] instructions = getInstructions(unit, key);
		if (instructions == null || instructions.length == 0)
			return null;
		Touchpoint touchpoint = getTouchpoint(unit);
		//TODO Need to propagate an exception if the touchpoint is not present
		if (touchpoint == null) {
			throw new IllegalStateException(NLS.bind(Messages.required_touchpoint_not_found, unit.getTouchpointType()));
		}
		InstructionParser parser = new InstructionParser(this, touchpoint);
		List actions = new ArrayList();
		for (int i = 0; i < instructions.length; i++) {
			actions.addAll(Arrays.asList(parser.parseActions(instructions[i])));
		}
		return (ProvisioningAction[]) actions.toArray(new ProvisioningAction[actions.size()]);
	}

	private final static TouchpointInstruction[] getInstructions(IInstallableUnit unit, String key) {
		TouchpointData[] data = unit.getTouchpointData();
		if (data == null)
			return null;

		ArrayList matches = new ArrayList(data.length);
		for (int i = 0; i < data.length; i++) {
			TouchpointInstruction instructions = data[i].getInstruction(key);
			if (instructions != null)
				matches.add(instructions);
		}

		TouchpointInstruction[] result = (TouchpointInstruction[]) matches.toArray(new TouchpointInstruction[matches.size()]);
		return result;
	}
}
