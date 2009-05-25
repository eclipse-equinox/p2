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
package org.eclipse.equinox.p2.engine.phases;

import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.Messages;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;

public class Configure extends Phase {

	public Configure(int weight) {
		super("configure", weight); //$NON-NLS-1$
	}

	protected boolean isApplicable(Operand op) {
		return (op.second() != null);
	}

	protected ProvisioningAction[] getActions(Operand currentOperand) {
		IInstallableUnit unit = currentOperand.second();
		if (unit.isFragment())
			return null;
		return getActions(unit, phaseId);
	}

	protected String getProblemMessage() {
		return Messages.Phase_Configure_Error;
	}

	protected IStatus initializeOperand(Profile profile, Operand operand, Map parameters, IProgressMonitor monitor) {
		IInstallableUnit iu = operand.second();
		monitor.subTask(NLS.bind(Messages.Phase_Configure_Task, iu.getId()));
		parameters.put(PARM_IU, iu);

		IArtifactKey[] artifacts = iu.getArtifacts();
		if (artifacts != null && artifacts.length > 0)
			parameters.put(PARM_ARTIFACT, artifacts[0]); 

		return Status.OK_STATUS;
	}
}
