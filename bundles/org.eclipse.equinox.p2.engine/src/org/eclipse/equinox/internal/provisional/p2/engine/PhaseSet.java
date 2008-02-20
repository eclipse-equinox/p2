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

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.EngineActivator;

public abstract class PhaseSet {
	private final Phase[] phases;

	public PhaseSet(Phase[] phases) {
		if (phases == null)
			throw new IllegalArgumentException("Phases must not be null"); //$NON-NLS-1$

		this.phases = phases;
	}

	public final MultiStatus perform(EngineSession session, IProfile profile, Operand[] operands, ProvisioningContext context, IProgressMonitor monitor) {
		MultiStatus result = new MultiStatus(EngineActivator.ID, IStatus.OK, null, null);
		int[] weights = getProgressWeights(operands);
		int totalWork = getTotalWork(weights);
		SubMonitor pm = SubMonitor.convert(monitor, totalWork);
		try {
			for (int i = 0; i < phases.length; i++) {
				if (pm.isCanceled()) {
					result.add(Status.CANCEL_STATUS);
					return result;
				}
				Phase phase = phases[i];
				MultiStatus performResult = phase.perform(session, profile, operands, context, pm.newChild(weights[i]));
				if (!performResult.isOK())
					result.add(performResult);
				if (result.matches(IStatus.ERROR | IStatus.CANCEL))
					return result;
			}
		} finally {
			pm.done();
		}
		return result;
	}

	private int getTotalWork(int[] weights) {
		int sum = 0;
		for (int i = 0; i < weights.length; i++)
			sum += weights[i];
		return sum;
	}

	private int[] getProgressWeights(Operand[] operands) {
		int[] weights = new int[phases.length];
		for (int i = 0; i < phases.length; i += 1) {
			if (operands.length > 0)
				//alter weights according to the number of operands applicable to that phase
				weights[i] = (phases[i].weight * countApplicable(phases[i], operands) / operands.length);
			else
				weights[i] = phases[i].weight;
		}
		return weights;
	}

	private int countApplicable(Phase phase, Operand[] operands) {
		int count = 0;
		for (int i = 0; i < operands.length; i++) {
			if (phase.isApplicable(operands[i]))
				count++;
		}
		return count;
	}
}
