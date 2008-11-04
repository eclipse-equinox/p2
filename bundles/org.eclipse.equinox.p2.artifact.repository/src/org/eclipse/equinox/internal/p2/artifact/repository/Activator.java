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
package org.eclipse.equinox.internal.p2.artifact.repository;

import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.osgi.framework.*;

public class Activator implements BundleActivator {
	public static final String ID = "org.eclipse.equinox.p2.artifact.repository"; //$NON-NLS-1$
	public static final String REPO_PROVIDER_XPT = ID + '.' + "artifactRepositories"; //$NON-NLS-1$

	private static BundleContext context;
	private ServiceRegistration repositoryManagerRegistration;
	private ArtifactRepositoryManager repositoryManager;

	public static BundleContext getContext() {
		return Activator.context;
	}

	public void start(BundleContext aContext) throws Exception {
		Activator.context = aContext;
		repositoryManager = new ArtifactRepositoryManager();
		repositoryManagerRegistration = aContext.registerService(IArtifactRepositoryManager.class.getName(), repositoryManager, null);
	}

	public void stop(BundleContext aContext) throws Exception {
		Activator.context = null;
		if (repositoryManagerRegistration != null)
			repositoryManagerRegistration.unregister();
		repositoryManagerRegistration = null;
		if (repositoryManager != null) {
			repositoryManager.shutdown();
			repositoryManager = null;
		}
	}

}
