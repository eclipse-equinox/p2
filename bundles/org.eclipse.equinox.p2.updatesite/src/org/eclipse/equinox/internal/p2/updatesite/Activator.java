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
package org.eclipse.equinox.internal.p2.updatesite;

import java.io.File;
import java.net.URL;
import org.eclipse.equinox.internal.p2.core.helpers.URLUtil;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	public static String ID = "org.eclipse.equinox.p2.updatesite"; //$NON-NLS-1$
	private static BundleContext bundleContext;

	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		setBundleContext(context);
	}

	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		setBundleContext(null);
	}

	public synchronized static void setBundleContext(BundleContext bundleContext) {
		Activator.bundleContext = bundleContext;
	}

	public synchronized static BundleContext getBundleContext() {
		return bundleContext;
	}

	/**
	 * Returns a reasonable human-readable repository name for the given location.
	 */
	public static String getRepositoryName(URL location) {
		File file = URLUtil.toFile(location);
		return file == null ? location.toExternalForm() : file.getAbsolutePath();
	}

}
