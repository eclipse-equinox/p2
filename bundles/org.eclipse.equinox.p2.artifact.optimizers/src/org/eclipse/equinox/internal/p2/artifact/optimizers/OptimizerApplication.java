package org.eclipse.equinox.internal.p2.artifact.optimizers;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;

public abstract class OptimizerApplication implements IApplication {

	public static IProvisioningAgent getAgent() {
		return (IProvisioningAgent) ServiceHelper.getService(Activator.getContext(), IProvisioningAgent.SERVICE_NAME);
	}

	public static IArtifactRepositoryManager getArtifactRepositoryManager() {
		return (IArtifactRepositoryManager) getAgent().getService(IArtifactRepositoryManager.SERVICE_NAME);
	}

	public void stop() {
		// Nothing to do
	}

}
