package org.eclipse.equinox.internal.p2.transport.ecf;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;

public class ECFTransportComponent implements IAgentServiceFactory {

	public Object createService(IProvisioningAgent agent) {
		return new RepositoryTransport();
	}
	
}
