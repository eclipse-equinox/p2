/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

	@Override
	public void execute() {
		validate();
		IMetadataRepositoryManager manager = getAgent().getService(IMetadataRepositoryManager.class);
		if (manager == null)
			throw new BuildException("Unable to aquire metadata repository manager service."); //$NON-NLS-1$

		// load the composite repository
		CompositeMetadataRepository repo = null;
		try {
			repo = (CompositeMetadataRepository) manager.loadRepository(location, null);
		} catch (ClassCastException e) {
			throw new BuildException(
					"Repository at location: " + location + " is not a composite metadata repository."); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (ProvisionException e) {
			throw new BuildException("Error occurred while loading repository.", e); //$NON-NLS-1$
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
			throw new BuildException("Need to specify the composite repository location."); //$NON-NLS-1$
		if (child == null)
			throw new BuildException("Need to specify the child repository location."); //$NON-NLS-1$
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
