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
 * Ant task to remove a specific child repository (or all the children repositories)
 * from a composite metadata repository.
 */
public class RemoveChildTask extends AbstractMDRTask {

	@SuppressWarnings("hiding")
	URI location; // location of the composite repository
	URI child; // address of the child to be removed
	boolean allChildren; // should we remove all the children?

	@Override
	public void execute() {
		IMetadataRepositoryManager manager = getAgent().getService(IMetadataRepositoryManager.class);
		if (manager == null)
			throw new BuildException("Unable to aquire metadata repository manager service."); //$NON-NLS-1$

		CompositeMetadataRepository repo;
		try {
			repo = (CompositeMetadataRepository) manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			throw new BuildException("Error occurred while loading repository.", e); //$NON-NLS-1$
		}

		// remove all the children repositories if requested, otherwise
		// just remove the specific child
		if (allChildren)
			repo.removeAllChildren();
		else
			repo.removeChild(child);
	}

	/*
	 * Set the location of the composite repository.
	 */
	public void setLocation(String value) throws URISyntaxException {
		location = URIUtil.fromString(value);
	}

	/*
	 * Set the location of the child repository to remove.
	 */
	public void setChild(String value) throws URISyntaxException {
		child = URIUtil.fromString(value);
	}

	/*
	 * Set whether or not we should remove all the children.
	 */
	public void setAllChildren(String value) {
		allChildren = Boolean.parseBoolean(value);
	}
}
