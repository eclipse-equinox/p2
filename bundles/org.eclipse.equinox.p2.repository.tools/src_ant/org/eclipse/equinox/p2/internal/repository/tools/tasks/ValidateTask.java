/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.internal.repository.tools.tasks;

import java.net.URI;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.tools.ArtifactRepositoryValidator;
import org.eclipse.equinox.p2.internal.repository.tools.Messages;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;

/**
 * Ant task for validating the contents of a composite artifact repository.
 */
public class ValidateTask extends AbstractRepositoryTask {

	private String comparatorID; // specifies the comparator we want to use.

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) getAgent().getService(IArtifactRepositoryManager.SERVICE_NAME);
		if (manager == null)
			throw new BuildException(Messages.no_artifactRepo_manager);

		ArtifactRepositoryValidator validator;
		try {
			validator = new ArtifactRepositoryValidator(comparatorID);
		} catch (ProvisionException e) {
			throw new BuildException(Messages.invalidComparatorId, e);
		}

		IArtifactRepository artifactRepository = null;
		for (DestinationRepository repo : destinations) {
			URI repoLocation = repo.getDescriptor().getRepoLocation();
			try {
				artifactRepository = manager.loadRepository(repoLocation, null);
				IStatus result = validator.validateRepository(artifactRepository);
				if (!result.isOK())
					throw new BuildException(result.getMessage());
			} catch (ProvisionException e) {
				throw new BuildException(Messages.exception_loadingRepository, e);
			}
		}
	}

	/*
	 * Set the repository location.
	 */
	public void setLocation(String value) {
		super.setDestination(value);
	}

	/*
	 * Set the ID of the comparator.
	 */
	public void setComparatorID(String value) {
		comparatorID = value;
	}
}
