/*******************************************************************************
 * Copyright (c) 2009, 2010 Cloudsmith Inc and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 * 	Cloudsmith Inc - initial API and implementation
 * 	IBM Corporation - ongoing development
 * 	Genuitec - Bug 291926
 *  Sonatype, Inc. - transport split
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.repository;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle.
 * This activator has helper methods to get file transfer service tracker, and
 * for making sure required ECF bundles are started.
 */
public class Activator implements BundleActivator {

	public static final String ID = "org.eclipse.equinox.p2.repository"; //$NON-NLS-1$

	private static BundleContext context;

	public void start(BundleContext aContext) throws Exception {
		Activator.context = aContext;

		//Force the startup of the registry bundle to make sure that the preference scope is registered
		Class.forName("org.eclipse.core.runtime.IExtensionRegistry"); //$NON-NLS-1$
	}

	public void stop(BundleContext aContext) throws Exception {
		Activator.context = aContext;
	}

	public static BundleContext getContext() {
		return Activator.context;
	}

}
