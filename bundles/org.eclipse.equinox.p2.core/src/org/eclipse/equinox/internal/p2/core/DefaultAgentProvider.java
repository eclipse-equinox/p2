/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.core;

import java.net.URI;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.osgi.framework.BundleContext;

/**
 * Default implementation of {@link IProvisioningAgentProvider}.
 */
public class DefaultAgentProvider implements IProvisioningAgentProvider {
	private BundleContext context;

	public void activate(BundleContext aContext) {
		this.context = aContext;
	}

	public IProvisioningAgent createAgent(URI location) {
		ProvisioningAgent result = new ProvisioningAgent();
		result.setBundleContext(context);
		result.setLocation(location);
		return result;
	}

}
