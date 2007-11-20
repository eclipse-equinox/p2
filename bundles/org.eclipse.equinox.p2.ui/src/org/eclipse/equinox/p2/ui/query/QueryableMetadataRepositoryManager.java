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
package org.eclipse.equinox.p2.ui.query;

import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.ui.ProvUIMessages;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.p2.metadata.repository.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.equinox.p2.ui.ProvUI;
import org.eclipse.equinox.p2.ui.ProvUIActivator;
import org.eclipse.equinox.p2.ui.model.MetadataRepositoryElement;

/**
 * An object that adds queryable support to an artifact repository 
 * manager.
 */
public class QueryableMetadataRepositoryManager implements IQueryable {

	public Collector query(Query query, Collector result, IProgressMonitor monitor) {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(ProvUIActivator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null) {
			ProvUI.reportStatus(new Status(IStatus.ERROR, ProvUIActivator.PLUGIN_ID, ProvUIMessages.ProvisioningUtil_NoRepositoryManager));
			return result;
		}
		IMetadataRepository[] repos = manager.getKnownRepositories();
		for (int i = 0; i < repos.length; i++) {
			if (query.isMatch(repos[i]))
				result.accept(new MetadataRepositoryElement(repos[i]));
		}
		return result;
	}
}
