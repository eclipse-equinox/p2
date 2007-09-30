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
package org.eclipse.equinox.internal.p2.metadata;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class MetadataActivator implements BundleActivator {
	public static final String PI_METADATA = "org.eclipse.equinox.p2.metadata"; //$NON-NLS-1$
	public static BundleContext context;

	public static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext context) throws Exception {
		MetadataActivator.context = context;
	}

	public void stop(BundleContext context) throws Exception {
		MetadataActivator.context = null;
	}

}
