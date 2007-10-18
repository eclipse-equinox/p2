/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.updatechecker;

import org.eclipse.equinox.p2.updatechecker.UpdateChecker;
import org.osgi.framework.*;

/**
 * Activator class that registers the update checker service.
 * 
 * @since 3.4
 */
public class Activator implements BundleActivator {

	private static BundleContext context;
	private UpdateChecker updateChecker;
	private ServiceRegistration registrationChecker;

	public static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext context) throws Exception {
		this.context = context;

		updateChecker = new UpdateChecker();
		registrationChecker = context.registerService(UpdateChecker.class.getName(), updateChecker, null);

	}

	public void stop(BundleContext context) throws Exception {
		registrationChecker.unregister();
		updateChecker = null;
	}

}
