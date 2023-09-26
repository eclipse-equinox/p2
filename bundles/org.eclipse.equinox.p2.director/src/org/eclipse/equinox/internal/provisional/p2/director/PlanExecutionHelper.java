/*******************************************************************************
 * Copyright (c) 2009, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *     Christoph Läubrich - access activator static singelton in a safe way
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.director;

import java.io.IOException;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.director.DirectorActivator;
import org.eclipse.equinox.internal.p2.director.Messages;
import org.eclipse.equinox.internal.provisional.configurator.Configurator;
import org.eclipse.equinox.p2.engine.*;

public class PlanExecutionHelper {
	public static IStatus executePlan(IProvisioningPlan result, IEngine engine, ProvisioningContext context, IProgressMonitor progress) {
		return executePlan(result, engine, PhaseSetFactory.createDefaultPhaseSet(), context, progress);
	}

	public static IStatus executePlan(IProvisioningPlan result, IEngine engine, IPhaseSet phaseSet, ProvisioningContext context, IProgressMonitor progress) {
		if (!result.getStatus().isOK())
			return result.getStatus();

		if (result.getInstallerPlan() != null) {
			IStatus installerPlanStatus = result.getInstallerPlan().getProfile().getProvisioningAgent()
					.getService(IEngine.class).perform(result.getInstallerPlan(), phaseSet, progress);
			if (!installerPlanStatus.isOK())
				return installerPlanStatus;
			Configurator configChanger = DirectorActivator.context
					.map(ctx -> ServiceHelper.getService(ctx, Configurator.class)).orElse(null);
			try {
				configChanger.applyConfiguration();
			} catch (IOException e) {
				return Status.error(Messages.Director_error_applying_configuration, e);
			}
		}
		return engine.perform(result, phaseSet, progress);
	}
}
