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
import org.eclipse.equinox.internal.p2.engine.EngineActivator;
import org.eclipse.equinox.internal.p2.engine.InstructionParser;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;

public abstract class InstallableUnitPhase extends Phase {
	public static final String PARM_ARTIFACT = "artifact"; //$NON-NLS-1$
	public static final String PARM_IU = "iu"; //$NON-NLS-1$

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

	final public boolean isApplicable(Operand operand) {
		if (!(operand instanceof InstallableUnitOperand))
			return false;

		InstallableUnitOperand iuOperand = (InstallableUnitOperand) operand;
		return isApplicable(iuOperand);
	}

	protected boolean isApplicable(InstallableUnitOperand operand) {
		return true;
	}

	protected final ProvisioningAction[] getActions(IInstallableUnit unit, String key) {
		ITouchpointInstruction[] instructions = getInstructions(unit, key);
		if (instructions == null || instructions.length == 0)
			return null;

		List actions = new ArrayList();
		InstructionParser instructionParser = new InstructionParser(getActionManager());
		for (int i = 0; i < instructions.length; i++) {
			actions.addAll(Arrays.asList(instructionParser.parseActions(instructions[i], unit.getTouchpointType())));
		}
		return (ProvisioningAction[]) actions.toArray(new ProvisioningAction[actions.size()]);
	}

	private final static ITouchpointInstruction[] getInstructions(IInstallableUnit unit, String key) {
		ITouchpointData[] data = unit.getTouchpointData();
		if (data == null)
			return null;

		ArrayList matches = new ArrayList(data.length);
		for (int i = 0; i < data.length; i++) {
			ITouchpointInstruction instructions = data[i].getInstruction(key);
			if (instructions != null)
				matches.add(instructions);
		}

		ITouchpointInstruction[] result = (ITouchpointInstruction[]) matches.toArray(new ITouchpointInstruction[matches.size()]);
		return result;
	}
}
