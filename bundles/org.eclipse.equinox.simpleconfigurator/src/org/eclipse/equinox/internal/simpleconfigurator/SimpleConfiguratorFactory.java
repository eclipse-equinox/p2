/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
package org.eclipse.equinox.internal.simpleconfigurator;

import org.osgi.framework.*;

public class SimpleConfiguratorFactory implements ServiceFactory<Object> {
	private BundleContext context;

	public SimpleConfiguratorFactory(BundleContext context) {
		this.context = context;
	}

	@Override
	public Object getService(Bundle bundle, ServiceRegistration<Object> registration) {
		return new SimpleConfiguratorImpl(context, bundle);
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<Object> registration, Object service) {
		// nothing to do
	}
}
