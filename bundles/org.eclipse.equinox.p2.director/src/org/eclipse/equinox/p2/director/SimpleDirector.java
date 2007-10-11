/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.director;

import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.director.DirectorActivator;
import org.eclipse.equinox.internal.p2.director.Messages;
import org.eclipse.equinox.internal.p2.rollback.FormerState;
import org.eclipse.equinox.p2.core.eventbus.ProvisioningEventBus;
import org.eclipse.equinox.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.location.AgentLocation;
import org.eclipse.equinox.p2.core.repository.IRepository;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IInstallableUnitConstants;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.osgi.util.NLS;

public class SimpleDirector implements IDirector {
	static final int OperationWork = 100;
	private Engine engine;
	private IPlanner planner;

	public static void tagAsImplementation(IMetadataRepository repository) {
		if (repository != null && repository.getProperties().getProperty(IRepository.IMPLEMENTATION_ONLY_KEY) == null) {
			if (repository.isModifiable())
				repository.getModifiableProperties().setProperty(IRepository.IMPLEMENTATION_ONLY_KEY, Boolean.valueOf(true).toString());
		}
	}

	public SimpleDirector() {
		URL rollbackLocation = null;
		AgentLocation agentLocation = (AgentLocation) ServiceHelper.getService(DirectorActivator.context, AgentLocation.class.getName());
		rollbackLocation = agentLocation.getTouchpointDataArea("director");
		ProvisioningEventBus eventBus = (ProvisioningEventBus) ServiceHelper.getService(DirectorActivator.context, ProvisioningEventBus.class.getName());
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(DirectorActivator.context, IMetadataRepositoryManager.class.getName());
		IMetadataRepository rollbackRepo = manager.loadRepository(rollbackLocation, null);
		if (rollbackRepo == null)
			rollbackRepo = manager.createRepository(rollbackLocation, "Agent rollback repo", "org.eclipse.equinox.p2.metadata.repository.simpleRepository"); //$NON-NLS-1$//$NON-NLS-2$
		if (rollbackRepo == null)
			throw new IllegalStateException("Unable to open or create Agent's rollback repository");
		tagAsImplementation(rollbackRepo);
		new FormerState(eventBus, rollbackRepo);
		engine = (Engine) ServiceHelper.getService(DirectorActivator.context, Engine.class.getName());
		if (engine == null)
			throw new IllegalStateException("Provisioning engine is not registered");
		planner = (IPlanner) ServiceHelper.getService(DirectorActivator.context, IPlanner.class.getName());
		if (planner == null)
			throw new IllegalStateException("Unable to find provisioning planner");
	}

	public IStatus install(IInstallableUnit[] installRoots, Profile profile, IProgressMonitor monitor) {
		ProvisioningPlan plan = planner.getInstallPlan(installRoots, profile, monitor);
		if (!plan.getStatus().isOK())
			return plan.getStatus();
		SubMonitor sub = SubMonitor.convert(monitor, OperationWork);
		sub.setTaskName(NLS.bind(Messages.Director_Task_Installing, profile.getValue(Profile.PROP_INSTALL_FOLDER)));
		try {
			IStatus engineResult = engine.perform(profile, new DefaultPhaseSet(), plan.getOperands(), sub.newChild(OperationWork));
			if (!engineResult.isOK())
				return engineResult;
			// mark the roots as such
			for (int i = 0; i < installRoots.length; i++)
				profile.setInstallableUnitProfileProperty(installRoots[i], IInstallableUnitConstants.PROFILE_ROOT_IU, Boolean.toString(true));

			return engineResult;
		} finally {
			sub.done();
		}
	}

	public IStatus become(IInstallableUnit target, Profile profile, IProgressMonitor monitor) {
		ProvisioningPlan plan = planner.getBecomePlan(target, profile, monitor);
		if (!plan.getStatus().isOK())
			return plan.getStatus();
		SubMonitor sub = SubMonitor.convert(monitor, OperationWork);
		sub.setTaskName(Messages.Director_Task_Updating);
		try {
			return engine.perform(profile, new DefaultPhaseSet(), plan.getOperands(), sub.newChild(OperationWork));
		} finally {
			sub.done();
		}
	}

	public IStatus uninstall(IInstallableUnit[] uninstallRoots, Profile profile, IProgressMonitor monitor) {
		ProvisioningPlan plan = planner.getUninstallPlan(uninstallRoots, profile, monitor);
		if (!plan.getStatus().isOK())
			return plan.getStatus();
		SubMonitor sub = SubMonitor.convert(monitor, OperationWork);
		sub.setTaskName(Messages.Director_Task_Uninstalling);
		try {
			return engine.perform(profile, new DefaultPhaseSet(), plan.getOperands(), sub.newChild(OperationWork));
		} finally {
			sub.done();
		}
	}

	public IStatus replace(IInstallableUnit[] toUninstall, IInstallableUnit[] toInstall, Profile profile, IProgressMonitor monitor) {
		ProvisioningPlan plan = planner.getReplacePlan(toUninstall, toInstall, profile, monitor);
		if (!plan.getStatus().isOK())
			return plan.getStatus();
		SubMonitor sub = SubMonitor.convert(monitor, OperationWork);
		sub.setTaskName(Messages.Director_Task_Updating);
		try {
			return engine.perform(profile, new DefaultPhaseSet(), plan.getOperands(), sub.newChild(OperationWork));
		} finally {
			sub.done();
		}
	}

}
