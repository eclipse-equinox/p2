/*******************************************************************************
 * Copyright (c) 2010, 2015 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.repository.Messages;
import org.eclipse.equinox.internal.p2.repository.Activator;
import org.eclipse.equinox.p2.core.IProvisioningAgent;

public class AbstractMDRTask extends Task {
	/*
	 * Return the provisioning agent. Throw an exception if it cannot be obtained.
	 */
	public static IProvisioningAgent getAgent() throws BuildException {
		IProvisioningAgent agent = ServiceHelper.getService(Activator.getContext(), IProvisioningAgent.class);
		if (agent == null)
			throw new BuildException(Messages.no_provisioning_agent);
		return agent;
	}

}
