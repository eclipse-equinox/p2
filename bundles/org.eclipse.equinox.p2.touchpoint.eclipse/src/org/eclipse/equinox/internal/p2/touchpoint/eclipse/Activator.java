/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
package org.eclipse.equinox.internal.p2.touchpoint.eclipse;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
	public static final String ID = "org.eclipse.equinox.p2.touchpoint.eclipse"; //$NON-NLS-1$
	private static BundleContext context = null;

	public void start(BundleContext ctx) throws Exception {
		Activator.context = ctx;
	}

	public void stop(BundleContext ctx) throws Exception {
		Activator.context = null;
	}

	public static BundleContext getContext() {
		return Activator.context;
	}

}
