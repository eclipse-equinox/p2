/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	public void stop() {
		// Nothing to do
	}

}
