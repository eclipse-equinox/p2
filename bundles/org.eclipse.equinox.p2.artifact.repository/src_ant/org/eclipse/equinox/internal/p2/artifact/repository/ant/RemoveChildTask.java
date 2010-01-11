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
 * Ant task for removing a specific child from a composite artifact repository.
 */
public class RemoveChildTask extends Task {

	URI location; // location of the composite repository
	URI child; // location of child to remove
	boolean allChildren = false; // should we remove all children?

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.SERVICE_NAME);
		if (manager == null)
			throw new BuildException("Unable to aquire artifact repository manager service.");

		// load repository
		CompositeArtifactRepository repo;
		try {
			repo = (CompositeArtifactRepository) manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			throw new BuildException("Error occurred while loading repository.", e);
		}

		// remove all children or just a specified child
		if (allChildren)
			repo.removeAllChildren();
		else
			repo.removeChild(child);
	}

	/*
	 * Set the repository location.
	 */
	public void setLocation(String value) throws URISyntaxException {
		location = URIUtil.fromString(value);
	}

	/*
	 * Set the child repository location.
	 */
	public void setChild(String value) throws URISyntaxException {
		child = URIUtil.fromString(value);
	}

	/*
	 * Set whether or not we want to remove all the children.
	 */
	public void setAllChildren(String value) {
		allChildren = Boolean.valueOf(value).booleanValue();
	}
}
