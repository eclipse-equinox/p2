/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.rollback.FormerState;
import org.eclipse.equinox.p2.core.location.AgentLocation;
import org.eclipse.equinox.p2.director.*;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.osgi.util.NLS;

public class SimpleDirector implements IDirector {
	private static final String ROLLBACK_LOCATION = "rollback"; //$NON-NLS-1$
	static final int PlanWork = 10;
	static final int EngineWork = 100;
	private Engine engine;
	private IPlanner planner;

	public SimpleDirector() {
		initializeRollbackRepository();
		engine = (Engine) ServiceHelper.getService(DirectorActivator.context, Engine.class.getName());
		if (engine == null)
			throw new IllegalStateException("Provisioning engine is not registered"); //$NON-NLS-1$
		planner = (IPlanner) ServiceHelper.getService(DirectorActivator.context, IPlanner.class.getName());
		if (planner == null)
			throw new IllegalStateException("Unable to find provisioning planner"); //$NON-NLS-1$
	}

	private void initializeRollbackRepository() {
		new FormerState(getRollbackRepositoryLocation());
	}

	public URL getRollbackRepositoryLocation() {
		AgentLocation agentLocation = (AgentLocation) ServiceHelper.getService(DirectorActivator.context, AgentLocation.class.getName());
		try {
			return new URL(agentLocation.getDataArea(DirectorActivator.PI_DIRECTOR), ROLLBACK_LOCATION);
		} catch (MalformedURLException e) {
			//we know this can't happen because the above URL is well-formed
			return null;
		}
	}

	public IStatus install(IInstallableUnit[] installRoots, Profile profile, URL[] metadataRepositories, IProgressMonitor monitor) {
		String taskName = NLS.bind(Messages.Director_Task_Installing, profile.getValue(Profile.PROP_INSTALL_FOLDER));
		SubMonitor sub = SubMonitor.convert(monitor, taskName, PlanWork + EngineWork);
		try {
			ProvisioningPlan plan = planner.getInstallPlan(installRoots, profile, metadataRepositories, sub.newChild(PlanWork));
			if (!plan.getStatus().isOK())
				return plan.getStatus();
			// mark the roots as such
			for (int i = 0; i < installRoots.length; i++) {
				profile.setInstallableUnitProfileProperty(installRoots[i], IInstallableUnit.PROP_PROFILE_ROOT_IU, Boolean.toString(true));
			}
			IStatus engineResult = engine.perform(profile, new DefaultPhaseSet(), plan.getOperands(), sub.newChild(EngineWork));
			return engineResult;
		} finally {
			sub.done();
		}
	}

	public IStatus become(IInstallableUnit target, Profile profile, URL[] metadataRepositories, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, Messages.Director_Task_Updating, PlanWork + EngineWork);
		try {
			ProvisioningPlan plan = planner.getBecomePlan(target, profile, metadataRepositories, sub.newChild(PlanWork));
			if (!plan.getStatus().isOK())
				return plan.getStatus();
			return engine.perform(profile, new DefaultPhaseSet(), plan.getOperands(), sub.newChild(EngineWork));
		} finally {
			sub.done();
		}
	}

	public IStatus uninstall(IInstallableUnit[] uninstallRoots, Profile profile, URL[] metadataRepositories, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, Messages.Director_Task_Uninstalling, PlanWork + EngineWork);
		try {
			ProvisioningPlan plan = planner.getUninstallPlan(uninstallRoots, profile, metadataRepositories, sub.newChild(PlanWork));
			if (!plan.getStatus().isOK())
				return plan.getStatus();
			return engine.perform(profile, new DefaultPhaseSet(), plan.getOperands(), sub.newChild(EngineWork));
		} finally {
			sub.done();
		}
	}

	public IStatus replace(IInstallableUnit[] toUninstall, IInstallableUnit[] toInstall, Profile profile, URL[] metadataRepositories, IProgressMonitor monitor) {
		SubMonitor sub = SubMonitor.convert(monitor, Messages.Director_Task_Updating, PlanWork + EngineWork);
		try {
			ProvisioningPlan plan = planner.getReplacePlan(toUninstall, toInstall, profile, metadataRepositories, sub.newChild(PlanWork));
			if (!plan.getStatus().isOK())
				return plan.getStatus();
			return engine.perform(profile, new DefaultPhaseSet(), plan.getOperands(), sub.newChild(EngineWork));
		} finally {
			sub.done();
		}
	}

	public IStatus revert(IInstallableUnit previous, Profile profile, URL[] metadataRepositories, IProgressMonitor monitor) {
		return become(previous, profile, metadataRepositories, monitor);
	}

}
