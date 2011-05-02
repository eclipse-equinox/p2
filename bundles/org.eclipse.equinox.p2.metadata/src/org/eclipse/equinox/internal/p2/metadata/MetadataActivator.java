/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class MetadataActivator implements BundleActivator {
	public static final String PI_METADATA = "org.eclipse.equinox.p2.metadata"; //$NON-NLS-1$

	public static MetadataActivator instance;

	private BundleContext context;

	public static BundleContext getContext() {
		MetadataActivator activator = instance;
		return activator == null ? null : activator.context;
	}

	public void start(BundleContext aContext) throws Exception {
		context = aContext;
		instance = this;
	}

	public void stop(BundleContext aContext) throws Exception {
		instance = null;
	}
}
