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
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.Messages;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public class Unconfigure extends Phase {

	public Unconfigure(int weight) {
		super("unconfigure", weight); //$NON-NLS-1$
	}

	protected boolean isApplicable(Operand op) {
		return (op.first() != null);
	}

	protected ProvisioningAction[] getActions(Operand currentOperand) {
		//TODO: monitor.subTask(NLS.bind(Messages.Engine_Unconfiguring_IU, unit.getId()));

		IInstallableUnit unit = currentOperand.first();
		if (unit.isFragment())
			return null;

		return getActions(unit, phaseId);
	}

	protected String getProblemMessage() {
		return Messages.Phase_Unconfigure_Error;
	}

	protected IStatus initializeOperand(Profile profile, Operand operand, Map parameters, IProgressMonitor monitor) {
		IInstallableUnit iu = operand.first();
		parameters.put("iu", iu); //$NON-NLS-1$

		IArtifactKey[] artifacts = iu.getArtifacts();
		if (artifacts != null && artifacts.length > 0)
			parameters.put("artifact", artifacts[0]); //$NON-NLS-1$

		return Status.OK_STATUS;
	}
}
