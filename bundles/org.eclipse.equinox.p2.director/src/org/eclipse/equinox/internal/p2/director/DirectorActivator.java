/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.director;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class DirectorActivator implements BundleActivator {
	public static final String PI_DIRECTOR = "org.eclipse.equinox.p2.director"; //$NON-NLS-1$
	public static BundleContext context;

	@Override
	public void start(BundleContext ctx) throws Exception {
		context = ctx;
	}

	@Override
	public void stop(BundleContext ctx) throws Exception {
		DirectorActivator.context = null;
	}

}
