package org.eclipse.equinox.p2.tests.artifact.optimizers;

import junit.framework.TestCase;
import org.eclipse.equinox.internal.p2.artifact.optimizers.Activator;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;

public abstract class OptimizerTest extends TestCase {
	public OptimizerTest(String name) {
		super(name);
	}

	public OptimizerTest() {
		super();
	}

	protected static IProvisioningAgent getAgent() {
		//get the global agent for the currently running system
		return (IProvisioningAgent) ServiceHelper.getService(Activator.getContext(), IProvisioningAgent.SERVICE_NAME);
	}

}
