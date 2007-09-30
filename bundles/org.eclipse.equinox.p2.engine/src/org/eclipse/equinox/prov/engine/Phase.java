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
package org.eclipse.equinox.prov.engine;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.prov.engine.Messages;
import org.eclipse.equinox.prov.core.helpers.MultiStatus;
import org.eclipse.osgi.util.NLS;

public abstract class Phase {
	protected final String phaseId;
	protected final int weight;
	protected final String phaseName;

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

	protected MultiStatus perform(EngineSession session, Profile profile, Operand[] deltas, IProgressMonitor monitor) {
		MultiStatus status = new MultiStatus();
		//			log.start(log.info(Messages2.Engine_Performing_Phase, this.phaseName));
		perform(status, session, profile, deltas, monitor);
		//			log.stop();
		if (status.matches(IStatus.CANCEL)) {
			status.setMessage(Messages.Engine_Operation_Canceled_By_User);
		} else if (status.matches(IStatus.ERROR)) {
			status.setMessage(NLS.bind(Messages.Engine_Error_During_Phase, this.phaseName));
		}
		return status;
	}

	protected abstract void perform(MultiStatus status, EngineSession session, Profile profile, Operand[] deltas, IProgressMonitor monitor);

}