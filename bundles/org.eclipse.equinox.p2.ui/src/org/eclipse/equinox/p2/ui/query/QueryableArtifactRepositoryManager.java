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
import org.eclipse.equinox.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.ProvUIActivator;

/**
 * An object that adds queryable support to an artifact repository 
 * manager.  The object being queried is the repositry URL, not the 
 * repository instance itself.  Callers should load the repository
 * if necessary to complete the query.
 */
public class QueryableArtifactRepositoryManager implements IQueryable {

	public Collector query(Query query, Collector result, IProgressMonitor monitor) {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null) {
			ProvUI.reportStatus(new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, ProvUIMessages.ProvisioningUtil_NoRepositoryManager));
			return result;
		}
		int flags = IArtifactRepositoryManager.REPOSITORIES_ALL;
		if (query instanceof FilteredRepositoryQuery)
			flags = ((FilteredRepositoryQuery) query).getFlags();
		URL[] repoURLs = manager.getKnownRepositories(flags);
		if (monitor == null)
			monitor = new NullProgressMonitor();
		monitor.beginTask(ProvUIMessages.QueryableArtifactRepositoryManager_RepositoryQueryProgress, repoURLs.length * 2);
		for (int i = 0; i < repoURLs.length; i++) {
			if (query.isMatch(repoURLs[i]))
				result.accept(new ArtifactRepositoryElement(repoURLs[i]));
			monitor.worked(1);
		}
		monitor.done();
		return result;
	}
}
