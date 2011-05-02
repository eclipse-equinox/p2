/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.engine.phases;

import org.eclipse.equinox.p2.query.QueryUtil;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.*;
import org.eclipse.equinox.p2.engine.PhaseSetFactory;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.spi.ProvisioningAction;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public class Unconfigure extends InstallableUnitPhase {

	public Unconfigure(int weight, boolean forced) {
		super(PhaseSetFactory.PHASE_UNCONFIGURE, weight, forced);
	}

	public Unconfigure(int weight) {
		this(weight, false);
	}

	protected boolean isApplicable(InstallableUnitOperand op) {
		return (op.first() != null);
	}

	protected List<ProvisioningAction> getActions(InstallableUnitOperand currentOperand) {
		//TODO: monitor.subTask(NLS.bind(Messages.Engine_Unconfiguring_IU, unit.getId()));

		IInstallableUnit unit = currentOperand.first();
		if (QueryUtil.isFragment(unit))
			return null;

		return getActions(unit, phaseId);
	}

	protected String getProblemMessage() {
		return Messages.Phase_Unconfigure_Error;
	}

	protected IStatus initializeOperand(IProfile profile, InstallableUnitOperand operand, Map<String, Object> parameters, IProgressMonitor monitor) {
		IInstallableUnit iu = operand.first();
		parameters.put(PARM_IU, iu);

		Collection<IArtifactKey> artifacts = iu.getArtifacts();
		if (artifacts != null && artifacts.size() > 0)
			parameters.put(PARM_ARTIFACT, artifacts.iterator().next());

		return Status.OK_STATUS;
	}
}
