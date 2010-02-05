/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.updatechecker;

import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.updatechecker.IUpdateChecker;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Activator class that registers the update checker service.
 */
public class Activator implements BundleActivator {
	public static final String ID = "org.eclipse.equinox.p2.updatechecker"; //$NON-NLS-1$
	private static BundleContext context;

	public static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		context = bundleContext;
		IProvisioningAgent agent = (IProvisioningAgent) ServiceHelper.getService(bundleContext, IProvisioningAgent.SERVICE_NAME);
		agent.registerService(IUpdateChecker.SERVICE_NAME, new UpdateChecker(agent));
	}

	public void stop(BundleContext bundleContext) throws Exception {
		context = null;
	}
}
