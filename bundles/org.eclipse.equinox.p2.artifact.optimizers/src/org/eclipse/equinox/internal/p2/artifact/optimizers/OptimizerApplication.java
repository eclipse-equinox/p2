/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Cloudsmith - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.optimizers;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;

public abstract class OptimizerApplication implements IApplication {

	public static IProvisioningAgent getAgent() {
		return ServiceHelper.getService(Activator.getContext(), IProvisioningAgent.class);
	}

	public static IArtifactRepositoryManager getArtifactRepositoryManager() {
		return (IArtifactRepositoryManager) getAgent().getService(IArtifactRepositoryManager.SERVICE_NAME);
	}

	@Override
	public void stop() {
		// Nothing to do
	}

}
