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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.metadata.TouchpointType;

/**
 * A touchpoint is responsible for executing a given phase for a given 
 * targeted system (eclipse, native). The order of phases is defined in the {@link PhaseSet}.  
 */
public interface ITouchpoint {

	public TouchpointType getTouchpointType();

	public boolean supports(String phaseId);

	public ITouchpointAction[] getActions(String phaseId, Profile profile, Operand operand);

	public IStatus initializePhase(IProgressMonitor monitor, Profile profile, String phaseId, Map touchpointParameters);

	public IStatus completePhase(IProgressMonitor monitor, Profile profile, String phaseId, Map touchpointParameters);
}
