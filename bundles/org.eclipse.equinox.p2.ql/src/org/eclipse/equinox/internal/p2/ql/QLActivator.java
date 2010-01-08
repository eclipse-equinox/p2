/*******************************************************************************
 * Copyright (c) 2009 Cloudsmith Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cloudsmith Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ql;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class QLActivator implements BundleActivator {
	public static BundleContext context;

	public static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext aContext) throws Exception {
		QLActivator.context = aContext;
	}

	public void stop(BundleContext aContext) throws Exception {
		QLActivator.context = null;
	}
}
