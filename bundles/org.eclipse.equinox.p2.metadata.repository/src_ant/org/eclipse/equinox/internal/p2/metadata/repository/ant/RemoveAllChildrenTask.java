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
package org.eclipse.equinox.internal.p2.metadata.repository.ant;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.repository.Activator;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;

/**
 * Ant task to remove all the children repositories from a composite metadata repository.
 */
public class RemoveAllChildrenTask extends Task {
	URI location; // location of the composite repository

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			throw new BuildException("Unable to aquire metadata repository manager service.");

		// load the repository
		CompositeMetadataRepository repo = null;
		try {
			repo = (CompositeMetadataRepository) manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			throw new BuildException("Exception while loading repository.", e);
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
