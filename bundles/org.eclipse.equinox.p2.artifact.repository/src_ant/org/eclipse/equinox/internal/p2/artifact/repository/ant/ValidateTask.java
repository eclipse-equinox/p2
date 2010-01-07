/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.artifact.repository.ant;

import org.eclipse.equinox.p2.core.ProvisionException;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;

/**
 * Ant task for validating the contents of a composite artifact repository.
 */
public class ValidateTask extends Task {

	URI location; // location of the composite repository
	String comparatorID; // specifies the comparator we want to use.

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.SERVICE_NAME);
		if (manager == null)
			throw new BuildException("Unable to aquire artifact repository manager service.");

		// load the repository
		CompositeArtifactRepository repo = null;
		try {
			repo = (CompositeArtifactRepository) manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			throw new BuildException("Exception while loading repository.", e);
		}

		// perform the sanity check
		if (repo.validate(comparatorID))
			System.err.println("Valid repository at: " + location);
		else
			System.err.println("Invalid repository at: " + location);
	}

	/*
	 * Set the repository location.
	 */
	public void setLocation(String value) throws URISyntaxException {
		location = URIUtil.fromString(value);
	}

	/*
	 * Set the ID of the comparator.
	 */
	public void setComparatorID(String value) {
		comparatorID = value;
	}
}
