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
package org.eclipse.equinox.internal.p2.metadata.repository.ant;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

/**
 * Ant task for adding a child repository to a composite metadata repository.
 */
public class AddChildTask extends AbstractMDRTask {

	URI location; // location of the composite repository
	URI child; // address of the child to add

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() {
		validate();
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
		if (manager == null)
			throw new BuildException("Unable to aquire metadata repository manager service.");

		// load the composite repository
		CompositeMetadataRepository repo = null;
		try {
			repo = (CompositeMetadataRepository) manager.loadRepository(location, null);
		} catch (ClassCastException e) {
			throw new BuildException("Repository at location: " + location + " is not a composite metadata repository.");
		} catch (ProvisionException e) {
			throw new BuildException("Error occurred while loading repository.", e);
		}

		// add the child
		repo.addChild(child);
		manager.removeRepository(location);
	}

	/*
	 * Validate user input to ensure we have enough information to go forward.
	 */
	private void validate() {
		if (location == null)
			throw new BuildException("Need to specify the composite repository location.");
		if (child == null)
			throw new BuildException("Need to specify the child repository location.");
	}

	/*
	 * Set the location of the composite repository.
	 */
	public void setLocation(String value) throws URISyntaxException {
		location = URIUtil.fromString(value);
	}

	/*
	 * Set the location of the child repository.
	 */
	public void setChild(String value) throws URISyntaxException {
		child = URIUtil.fromString(value);
	}
}
