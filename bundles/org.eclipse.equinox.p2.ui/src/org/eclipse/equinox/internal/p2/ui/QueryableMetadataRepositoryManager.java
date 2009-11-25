/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui;

import java.net.URI;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.ui.ProvisioningUI;

/**
 * An object that queries a particular set of metadata repositories.
 */
public class QueryableMetadataRepositoryManager extends QueryableRepositoryManager {

	public QueryableMetadataRepositoryManager(ProvisioningUI ui, boolean includeDisabledRepos) {
		super(ui, includeDisabledRepos);
	}

	protected IRepository getRepository(IRepositoryManager manager, URI location) {
		// note the use of MetadataRepositoryManager (the concrete implementation).
		if (manager instanceof MetadataRepositoryManager) {
			return ((MetadataRepositoryManager) manager).getRepository(location);
		}
		return super.getRepository(manager, location);
	}

	protected IRepositoryManager getRepositoryManager() {
		return getSession().getMetadataRepositoryManager();
	}

	protected IRepository doLoadRepository(IRepositoryManager manager, URI location, IProgressMonitor monitor) throws ProvisionException {
		if (manager instanceof IMetadataRepositoryManager) {
			((IMetadataRepositoryManager) manager).loadRepository(location, monitor);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.p2.ui.QueryableRepositoryManager#getRepositoryFlags(org.eclipse.equinox.p2.ui.RepositoryManipulator)
	 */
	protected int getRepositoryFlags(RepositoryTracker repositoryManipulator) {
		return repositoryManipulator.getMetadataRepositoryFlags();
	}
}
