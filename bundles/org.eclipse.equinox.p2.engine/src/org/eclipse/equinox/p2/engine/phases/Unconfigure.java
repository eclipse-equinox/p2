/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.engine.phases;

import java.util.Map;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.*;

public class Unconfigure extends Phase {

	public Unconfigure(int weight) {
		super("unconfigure", weight, Messages.Engine_Unconfigure_Phase);
	}

	protected boolean isApplicable(Operand op) {
		return (op.first() != null);
	}

	protected ProvisioningAction[] getActions(Touchpoint touchpoint, Operand currentOperand) {
		//TODO: monitor.subTask(NLS.bind(Messages.Engine_Unconfiguring_IU, unit.getId()));

		IInstallableUnit unit = currentOperand.first();
		if (unit.isFragment())
			return null;

		TouchpointData[] data = unit.getTouchpointData();
		if (data == null)
			return null;

		String[] instructions = getInstructionsFor("unconfigure", data);
		if (instructions.length == 0)
			return null;

		InstructionParser parser = new InstructionParser(this, touchpoint);
		return parser.parseActions(instructions[0]);
	}

	// We could put this in a utility class, Phase or perhaps refactor touchpoint data
	static private String[] getInstructionsFor(String key, TouchpointData[] data) {
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

	protected IStatus initializeOperand(Operand operand, Map parameters) {
		IResolvedInstallableUnit iu = operand.first();
		parameters.put("iu", iu);

		IArtifactKey[] artifacts = iu.getArtifacts();
		if (artifacts != null && artifacts.length > 0)
			parameters.put("artifactId", artifacts[0].getId());

		return Status.OK_STATUS;
	}
}
