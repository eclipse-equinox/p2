/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 - ongoing development
 *     Red Hat Inc. - Bug 460967
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.p2.directorywatcher;

import java.io.File;
import java.net.URI;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Bundle activator for directory watcher bundle.
 */
public class Activator implements BundleActivator {
	public static final String ID = "org.eclipse.equinox.p2.directorywatcher"; //$NON-NLS-1$

	private static BundleContext context;

	public static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext aContext) throws Exception {
		context = aContext;
	}

	public void stop(BundleContext aContext) throws Exception {
		context = null;
	}

	public static IArtifactRepositoryManager getArtifactRepositoryManager() {
		return (IArtifactRepositoryManager) ServiceHelper.getService(context, IProvisioningAgent.class).getService(IArtifactRepositoryManager.SERVICE_NAME);
	}

	public static IMetadataRepositoryManager getMetadataRepositoryManager() {
		return (IMetadataRepositoryManager) ServiceHelper.getService(context, IProvisioningAgent.class).getService(IMetadataRepositoryManager.SERVICE_NAME);
	}

	public static URI getDefaultRepositoryLocation(Object object, String repositoryName) {
		PackageAdmin packageAdmin = ServiceHelper.getService(context, PackageAdmin.class);
		Bundle bundle = packageAdmin.getBundle(object.getClass());
		BundleContext context = bundle.getBundleContext();
		File base = context.getDataFile(""); //$NON-NLS-1$
		File result = new File(base, "listener_" + repositoryName.hashCode()); //$NON-NLS-1$
		result.mkdirs();
		return result.toURI();
	}
}
