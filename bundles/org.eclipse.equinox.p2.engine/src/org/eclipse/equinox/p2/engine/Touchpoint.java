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

import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.metadata.TouchpointType;

/**
 * A touchpoint is responsible for executing a given phase for a given 
 * targeted system (eclipse, native). The order of phases is defined in the {@link PhaseSet}.  
 */
public abstract class Touchpoint {

	public abstract TouchpointType getTouchpointType();

	public abstract boolean supports(String phaseId);

	public abstract ProvisioningAction getAction(String actionId);

	public IStatus initializePhase(IProgressMonitor monitor, Profile profile, String phaseId, Map touchpointParameters) {
		return Status.OK_STATUS;
	}

	public IStatus completePhase(IProgressMonitor monitor, Profile profile, String phaseId, Map touchpointParameters) {
		return Status.OK_STATUS;
	}

	public IStatus initializeOperand(Operand operand, String phaseId, Map parameters) {
		return Status.OK_STATUS;
	}

	public IStatus completeOperand(Operand operand, String phaseId, Map parameters) {
		return Status.OK_STATUS;
	}
}
