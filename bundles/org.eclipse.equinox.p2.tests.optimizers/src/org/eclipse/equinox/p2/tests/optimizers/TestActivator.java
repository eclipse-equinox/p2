/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.optimizers;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @since 1.0
 */
public class TestActivator implements BundleActivator {

	public static final String PI = "org.eclipse.equinox.p2.tests.optimizers"; //$NON-NLS-1$
	private static BundleContext bundleContext;

	@Override
	public void start(BundleContext context) throws Exception {
		bundleContext = context;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		bundleContext = null;
	}

	public static BundleContext getContext() {
		return bundleContext;
	}

}
