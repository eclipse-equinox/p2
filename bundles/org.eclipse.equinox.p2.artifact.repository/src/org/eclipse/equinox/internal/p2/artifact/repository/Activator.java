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
package org.eclipse.equinox.internal.p2.artifact.repository;

import org.eclipse.equinox.p2.artifact.repository.IArtifactRepositoryManager;
import org.osgi.framework.*;

public class Activator implements BundleActivator {
	private static BundleContext context;
	public static final String ID = "org.eclipse.equinox.p2.artifact.repository";
	public static final String REPO_PROVIDER_XPT = ID + '.' + "artifactRepositories";
	public IArtifactRepositoryManager repoManager;
	public ServiceRegistration repoManagerRegistration;

	public void start(BundleContext context) throws Exception {
		Activator.context = context;
		//		repoManager = new ArtifactRepoManager();
		//		repoManagerRegistration = context.registerService(IArtifactRepoManager.class.getName(), repoManager, null);
	}

	public void stop(BundleContext context) throws Exception {
		Activator.context = null;
		//		repoManager = null;
		//		repoManagerRegistration = null;
	}

	public static BundleContext getContext() {
		return Activator.context;
	}

}
