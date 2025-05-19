/*******************************************************************************
 *  Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.touchpoint.natives;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
	public static final String ID = "org.eclipse.equinox.p2.touchpoint.natives"; //$NON-NLS-1$
	private static BundleContext context = null;

	public static BundleContext getContext() {
		return context;
	}

	@Override
	public void start(BundleContext aContext) throws Exception {
		Activator.context = aContext;
	}

	@Override
	public void stop(BundleContext aContext) throws Exception {
		context = null;
	}

}
