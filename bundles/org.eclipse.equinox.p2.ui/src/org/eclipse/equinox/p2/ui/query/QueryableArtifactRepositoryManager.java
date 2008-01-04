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
package org.eclipse.equinox.p2.ui.query;

import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.internal.p2.ui.model.ArtifactRepositoryElement;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepository;
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.ProvUIActivator;

/**
 * An object that adds queryable support to an artifact repository 
 * manager.
 */
public class QueryableArtifactRepositoryManager implements IQueryable {

	public Collector query(Query query, Collector result, IProgressMonitor monitor) {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null) {
			ProvUI.reportStatus(new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, ProvUIMessages.ProvisioningUtil_NoRepositoryManager));
			return result;
		}
		URL[] repos = manager.getKnownRepositories();
		if (monitor == null)
			monitor = new NullProgressMonitor();
		monitor.beginTask(ProvUIMessages.QueryableArtifactRepositoryManager_RepositoryQueryProgress, repos.length * 2);
		for (int i = 0; i < repos.length; i++) {
			IArtifactRepository repo = manager.loadRepository(repos[i], new SubProgressMonitor(monitor, 1));
			if (repo != null && query.isMatch(repo))
				result.accept(new ArtifactRepositoryElement(repo));
			monitor.worked(1);
		}
		monitor.done();
		return result;
	}
}
