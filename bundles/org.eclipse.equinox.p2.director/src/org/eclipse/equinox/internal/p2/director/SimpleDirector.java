/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.*;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;

public class SimpleDirector implements IDirector {
	static final int PlanWork = 10;
	static final int EngineWork = 100;
	private IEngine engine;
	private IPlanner planner;

	public SimpleDirector() {
		engine = (IEngine) ServiceHelper.getService(DirectorActivator.context, IEngine.class.getName());
		if (engine == null)
			throw new IllegalStateException("Provisioning engine is not registered"); //$NON-NLS-1$
		planner = (IPlanner) ServiceHelper.getService(DirectorActivator.context, IPlanner.class.getName());
		if (planner == null)
			throw new IllegalStateException("Unable to find provisioning planner"); //$NON-NLS-1$
	}

	public IStatus revert(IProfile currentProfile, IProfile revertProfile, ProvisioningContext context, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, Messages.Director_Task_Updating, PlanWork + EngineWork);
		try {
			ProvisioningPlan plan = planner.getDiffPlan(currentProfile, revertProfile, sub.newChild(PlanWork));
			return PlanExecutionHelper.executePlan(plan, engine, context, sub.newChild(EngineWork));
		} finally {
			sub.done();
		}
	}

	public IStatus provision(ProfileChangeRequest request, ProvisioningContext context, IProgressMonitor monitor) {
		String taskName = NLS.bind(Messages.Director_Task_Installing, request.getProfile().getProperty(IProfile.PROP_INSTALL_FOLDER));
		SubMonitor sub = SubMonitor.convert(monitor, taskName, PlanWork + EngineWork);
		try {
			IInstallableUnit[] installRoots = request.getAddedInstallableUnits();
			// mark the roots as such
			for (int i = 0; i < installRoots.length; i++) {
				request.setInstallableUnitProfileProperty(installRoots[i], IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.toString(true));
			}
			ProvisioningPlan plan = planner.getProvisioningPlan(request, context, sub.newChild(PlanWork));
			return PlanExecutionHelper.executePlan(plan, engine, context, sub.newChild(EngineWork));
		} finally {
			sub.done();
		}
	}
}
