/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.simpleconfigurator;

import org.osgi.framework.*;

public class SimpleConfiguratorFactory implements ServiceFactory<Object> {
	private BundleContext context;

	public SimpleConfiguratorFactory(BundleContext context) {
		this.context = context;
	}

	public Object getService(Bundle bundle, ServiceRegistration<Object> registration) {
		return new SimpleConfiguratorImpl(context, bundle);
	}

	public void ungetService(Bundle bundle, ServiceRegistration<Object> registration, Object service) {
		// nothing to do
	}
}
