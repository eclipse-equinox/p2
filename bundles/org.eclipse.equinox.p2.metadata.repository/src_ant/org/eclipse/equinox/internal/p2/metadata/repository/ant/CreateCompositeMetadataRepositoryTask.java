/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     SAP AG - repository atomic loading
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.metadata.repository.ant;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

/**
 * Ant task for creating a new composite metadata repository.
 */
public class CreateCompositeMetadataRepositoryTask extends AbstractMDRTask {

	URI location; // desired location of the composite repository
	String name = "Composite Metadata Repository";
	boolean atomic = false;
	boolean compressed = true; // compress by default
	boolean failOnExists = false; // should we fail if one already exists?
	Map<String, String> properties = new HashMap<String, String>();

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
		if (manager == null)
			throw new BuildException("Unable to aquire metadata repository manager service.");

		// remove the repo first.
		manager.removeRepository(location);

		// first try and load to see if one already exists at that location.
		// if we have an already existing repository at that location, then throw an error
		// if the user told us to
		try {
			IMetadataRepository repository = manager.loadRepository(location, null);
			if (repository instanceof CompositeMetadataRepository) {
				if (failOnExists)
					throw new BuildException("Composite repository already exists at location: " + location);
				return;
			} else {
				// we have a non-composite repo at this location. that is ok because we can co-exist.
			}
		} catch (ProvisionException e) {
			// re-throw the exception if we got anything other than "repo not found"
			if (e.getStatus().getCode() != ProvisionException.REPOSITORY_NOT_FOUND)
				throw new BuildException("Exception while trying to read repository at: " + location, e);
		}

		// create the properties
		if (compressed)
			properties.put(IRepository.PROP_COMPRESSED, Boolean.toString(true));
		if (atomic)
			properties.put(CompositeMetadataRepository.PROP_ATOMIC_LOADING, Boolean.toString(true));

		// create the repository
		try {
			manager.createRepository(location, name, IMetadataRepositoryManager.TYPE_COMPOSITE_REPOSITORY, properties);
		} catch (ProvisionException e) {
			throw new BuildException("Error occurred while creating composite metadata repository.", e);
		}
	}

	/*
	 * Set the name of the composite repository.
	 */
	public void setName(String value) {
		name = value;
	}

	/*
	 * Set the repository location.
	 */
	public void setLocation(String value) throws URISyntaxException {
		location = URIUtil.fromString(value);
	}

	/*
	 * Set whether or not this repository should be compressed.
	 */
	public void setCompressed(boolean value) {
		compressed = value;
	}

	/*
	 * Set a value indicating whether or not the repository should be atomic.
	 */
	public void setAtomic(boolean value) {
		atomic = value;
	}

	/*
	 * Set whether or not we should fail the operation if a repository
	 * already exists at the location.
	 */
	public void setFailOnExists(boolean value) {
		failOnExists = value;
	}
}
