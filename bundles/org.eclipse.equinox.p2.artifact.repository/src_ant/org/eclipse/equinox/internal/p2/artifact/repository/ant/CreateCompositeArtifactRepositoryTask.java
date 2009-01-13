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
import java.util.HashMap;
import java.util.Map;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.artifact.repository.Activator;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactRepositoryManager;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;

/**
 * Ant task for creating a new composite artifact repository.
 */
public class CreateCompositeArtifactRepositoryTask extends Task {

	URI location; // desired location of the composite repository
	String name = "Composite Artifact Repository";
	boolean compressed = true;
	Map properties = new HashMap();

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() {
		validate();
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.class.getName());
		if (manager == null)
			throw new BuildException("Unable to aquire artifact repository manager service.");

		// remove the repo first.
		manager.removeRepository(location);

		// set the properties
		if (compressed)
			properties.put(IRepository.PROP_COMPRESSED, Boolean.toString(true));

		// create the repository
		try {
			manager.createRepository(location, name, IArtifactRepositoryManager.TYPE_COMPOSITE_REPOSITORY, properties);
		} catch (ProvisionException e) {
			throw new BuildException("Error occurred while creating composite artifact repository.", e);
		}
	}

	/*
	 * Perform basic sanity checking of some of the parameters.
	 */
	private void validate() {
		if (location == null)
			throw new BuildException("Must specify repository location.");
		if (name == null)
			throw new BuildException("Must specify a repository name.");
	}

	/*
	 * Set the name of the composite repository.
	 */
	public void setName(String value) {
		name = value;
	}

	/*
	 * Set the location of the repository.
	 */
	public void setLocation(String value) throws URISyntaxException {
		location = URIUtil.fromString(value);
	}

	/*
	 * Set a value indicating whether or not the repository should be compressed.
	 */
	public void setCompressed(boolean value) {
		compressed = value;
	}

}
