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

public class Configure extends Phase {

	public Configure(int weight) {
		super("configure", weight, Messages.Engine_Configure_Phase);
	}

	protected boolean isApplicable(Operand op) {
		return (op.second() != null);
	}

	protected ProvisioningAction[] getActions(Operand currentOperand) {
		//TODO: monitor.subTask(NLS.bind(Messages.Engine_Configuring_IU, unit.getId()));

		IInstallableUnit unit = currentOperand.second();
		if (unit.isFragment())
			return null;

		return getActions(unit, phaseId);
	}

	protected IStatus initializeOperand(Operand operand, Map parameters) {
		IResolvedInstallableUnit iu = operand.second();
		parameters.put("iu", iu);

		IArtifactKey[] artifacts = iu.getArtifacts();
		if (artifacts != null && artifacts.length > 0)
			parameters.put("artifactId", artifacts[0].getId());

		return Status.OK_STATUS;
	}
}
