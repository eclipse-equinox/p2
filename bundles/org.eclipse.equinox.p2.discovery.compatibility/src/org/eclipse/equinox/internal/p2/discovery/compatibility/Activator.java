/*******************************************************************************
 * Copyright (c) 2010, 2017 Sonatype, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.discovery.compatibility;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.equinox.internal.p2.discovery.compatibility.util.CacheManager;
import org.eclipse.equinox.internal.p2.transport.ecf.RepositoryTransport;
import org.osgi.framework.BundleContext;

public class Activator extends Plugin {

	private static Activator plugin;

	private CacheManager manager;

	public static final String ID = "org.eclipse.equinox.p2.discovery.compatibility"; //$NON-NLS-1$

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
	}

	public static Activator getDefault() {
		return plugin;
	}

	public synchronized CacheManager getCacheManager() {
		if (manager == null) {
			manager = new CacheManager(new RepositoryTransport());
		}
		return manager;
	}
}
