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

import org.eclipse.equinox.internal.provisional.p2.updatechecker.IUpdateChecker;
import org.osgi.framework.*;

/**
 * Activator class that registers the update checker service.
 */
public class Activator implements BundleActivator {
	public static final String ID = "org.eclipse.equinox.p2.updatechecker"; //$NON-NLS-1$
	private static BundleContext context;
	private ServiceRegistration registrationChecker;

	public static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
		registrationChecker = context.registerService(IUpdateChecker.SERVICE_NAME, new UpdateChecker(), null);
	}

	public void stop(BundleContext bundleContext) throws Exception {
		registrationChecker.unregister();
	}
}
