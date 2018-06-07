/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.artifact.processors;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	public static final String ID = "org.eclipse.equinox.p2.artifact.processors"; //$NON-NLS-1$
	private static BundleContext _context = null;

	public static BundleContext getContext() {
		return _context;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		Activator._context = context;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// nothing to do
	}

}
