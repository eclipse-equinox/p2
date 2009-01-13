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

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;

/**
 * Ant task for removing all the children from an existing composite artifact repository.
 */
public class RemoveAllChildrenTask extends Task {
	URI location; // location of the composite repository

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null)
			throw new BuildException("Unable to aquire artifact repository manager service.");

		// load repository
		CompositeArtifactRepository repo = null;
		try {
			repo = (CompositeArtifactRepository) manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			throw new BuildException("Error occurred while loading repository.", e);
		}

		// remove all children
		repo.removeAllChildren();
	}

	/*
	 * Set the repository location.
	 */
	public void setLocation(String value) throws URISyntaxException {
		location = URIUtil.fromString(value);
	}
}
