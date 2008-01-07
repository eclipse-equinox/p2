/*******************************************************************************
 * Copyright (c) 2007 compeople AG and others.
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

import java.net.URL;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.artifact.optimizers.AbstractDeltaStep;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.artifact.repository.*;
import org.eclipse.equinox.p2.artifact.repository.processing.ProcessingStepDescriptor;

/**
 * The <code>AbstractDeltaPatchStep</code> is an abstract processing step that
 * retrieves a local artifact repository containing the serialized/encoded
 * artifact key. It assumes that the artifact key is stored within the data property
 * of the processing step descriptor encoding the artifact key of the base artifact.
 */
public abstract class AbstractDeltaProcessorStep extends AbstractDeltaStep {

	public void initialize(ProcessingStepDescriptor descriptor, IArtifactDescriptor context) {
		super.initialize(descriptor, context);
		if (!status.isOK())
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
			status = new Status(IStatus.ERROR, Activator.ID, "Could not get artifact repository manager.");
			return;
		}

		URL[] repositories = repoMgr.getKnownRepositories(IArtifactRepositoryManager.REPOSITORIES_LOCAL_ONLY);
		for (int i = 0; i < repositories.length; i++) {
			IArtifactRepository currentRepo = repoMgr.loadRepository(repositories[i], null);
			if (currentRepo != null && currentRepo.contains(key)) {
				repository = currentRepo;
				return;
			}
		}
		status = new Status(IStatus.ERROR, Activator.ID, "No repository available containing key " + key);
	}

}
