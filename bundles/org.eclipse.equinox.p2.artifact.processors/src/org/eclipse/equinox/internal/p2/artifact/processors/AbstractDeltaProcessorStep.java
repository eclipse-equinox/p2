/*******************************************************************************
 * Copyright (c) 2007, 2008 compeople AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	compeople AG (Stefan Liebig) - initial API and implementation
 * 	IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.processors;

import org.eclipse.equinox.internal.provisional.p2.repository.IRepositoryManager;

import java.net.URI;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.optimizers.AbstractDeltaStep;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.*;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.processing.ProcessingStepDescriptor;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;

/**
 * The <code>AbstractDeltaPatchStep</code> is an abstract processing step that
 * retrieves a local artifact repository containing the serialized/encoded
 * artifact key. It assumes that the artifact key is stored within the data property
 * of the processing step descriptor encoding the artifact key of the base artifact.
 */
public abstract class AbstractDeltaProcessorStep extends AbstractDeltaStep {

	public void initialize(ProcessingStepDescriptor descriptor, IArtifactDescriptor context) {
		super.initialize(descriptor, context);
		if (!getStatus().isOK())
			return;
		fetchLocalArtifactRepository();
	}

	/**
	 * Fetch a local artifact repository containing the fetched artifact key.
	 */
	private void fetchLocalArtifactRepository() {
		if (repository != null)
			return;
		IArtifactRepositoryManager repoMgr = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
		if (repoMgr == null) {
			setStatus(new Status(IStatus.ERROR, Activator.ID, "Could not get artifact repository manager."));
			return;
		}

		URI[] repositories = repoMgr.getKnownRepositories(IRepositoryManager.REPOSITORIES_LOCAL);
		for (int i = 0; i < repositories.length; i++) {
			try {
				IArtifactRepository currentRepo = repoMgr.loadRepository(repositories[i], null);
				if (currentRepo.contains(key)) {
					repository = currentRepo;
					return;
				}
			} catch (ProvisionException e) {
				//just skip unreadable repositories
			}
		}
		setStatus(new Status(IStatus.ERROR, Activator.ID, "No repository available containing key " + key));
	}

}
