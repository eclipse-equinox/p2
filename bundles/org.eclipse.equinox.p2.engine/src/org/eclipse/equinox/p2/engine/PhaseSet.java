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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.p2.core.helpers.MultiStatus;

public abstract class PhaseSet {
	private final Phase[] phases;

	public PhaseSet(Phase[] phases) {
		if (phases == null)
			throw new IllegalArgumentException("Phases must not be null");

		this.phases = phases;
	}

	public MultiStatus perform(EngineSession session, Profile profile, Operand[] deltas, IProgressMonitor monitor) {
		MultiStatus result = new MultiStatus();
		int[] weights = getProgressWeights();
		int totalWork = getTotalWork(weights);
		SubMonitor pm = SubMonitor.convert(monitor, totalWork);
		try {
			for (int i = 0; i < phases.length; i++) {
				if (pm.isCanceled()) {
					result.setCanceled();
					return result;
				}
				Phase phase = phases[i];
				result.add(phase.perform(session, profile, deltas, pm.newChild(weights[i])));
				if (result.isErrorOrCancel())
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

	private int[] getProgressWeights() {
		int[] weights = new int[phases.length];
		for (int i = 0; i < phases.length; i += 1) {
			weights[i] = phases[i].weight;
		}
		return weights;
	}
}