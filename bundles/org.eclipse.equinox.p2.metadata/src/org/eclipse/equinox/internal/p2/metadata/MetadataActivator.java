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

	@Override
	public void start(BundleContext aContext) throws Exception {
		context = aContext;
		instance = this;
	}

	@Override
	public void stop(BundleContext aContext) throws Exception {
		instance = null;
	}
}
