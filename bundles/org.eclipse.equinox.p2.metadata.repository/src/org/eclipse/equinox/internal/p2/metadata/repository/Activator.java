/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository;

import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.osgi.framework.*;

public class Activator implements BundleActivator {

	public static final String ID = "org.eclipse.equinox.p2.metadata.repository"; //$NON-NLS-1$
	public static final String REPO_PROVIDER_XPT = ID + '.' + "metadataRepositories"; //$NON-NLS-1$

	private static BundleContext bundleContext;
	private static CacheManager cacheManager;
	private ServiceRegistration repositoryManagerRegistration;
	private MetadataRepositoryManager repositoryManager;

	public static BundleContext getContext() {
		return bundleContext;
	}

	public static CacheManager getCacheManager() {
		return cacheManager;
	}

	public void start(BundleContext context) throws Exception {
		Activator.bundleContext = context;
		cacheManager = new CacheManager();
		cacheManager.registerRepoEventListener();
		repositoryManager = new MetadataRepositoryManager();
		repositoryManagerRegistration = context.registerService(IMetadataRepositoryManager.class.getName(), repositoryManager, null);
	}

	public void stop(BundleContext context) throws Exception {
		if (cacheManager != null) {
			cacheManager.unregisterRepoEventListener();
			cacheManager = null;
		}
		Activator.bundleContext = null;
		if (repositoryManagerRegistration != null)
			repositoryManagerRegistration.unregister();
		repositoryManagerRegistration = null;
		if (repositoryManager != null) {
			repositoryManager.shutdown();
			repositoryManager = null;
		}
	}
}
