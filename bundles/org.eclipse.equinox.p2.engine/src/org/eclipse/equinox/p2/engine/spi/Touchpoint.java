/*******************************************************************************
 *  Copyright (c) 2007, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.engine.spi;

import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.PhaseSet;
import org.eclipse.equinox.p2.engine.IProfile;

/**
 * A touchpoint is responsible for executing a given phase for a given 
 * targeted system (eclipse, native). The order of phases is defined in the {@link PhaseSet}.  
 * @since 2.0
 */
public abstract class Touchpoint {

	/** NOT API -- this is for backwards compatibility only */
	public String qualifyAction(String actionId) {
		return actionId;
	}

	public IStatus initializePhase(IProgressMonitor monitor, IProfile profile, String phaseId, Map<String, Object> touchpointParameters) {
		return Status.OK_STATUS;
	}

	public IStatus completePhase(IProgressMonitor monitor, IProfile profile, String phaseId, Map<String, Object> touchpointParameters) {
		return Status.OK_STATUS;
	}

	public IStatus initializeOperand(IProfile profile, Map<String, Object> parameters) {
		return Status.OK_STATUS;
	}

	public IStatus completeOperand(IProfile profile, Map<String, Object> parameters) {
		return Status.OK_STATUS;
	}

	public IStatus prepare(IProfile profile) {
		return Status.OK_STATUS;
	}

	public IStatus commit(IProfile profile) {
		return Status.OK_STATUS;
	}

	public IStatus rollback(IProfile profile) {
		return Status.OK_STATUS;
	}
}
