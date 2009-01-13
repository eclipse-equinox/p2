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
import java.util.HashMap;
import java.util.Map;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.internal.p2.metadata.repository.Activator;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.core.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepositoryManager;

/**
 * Ant task for creating a new composite metadata repository.
 */
public class CreateCompositeMetadataRepositoryTask extends Task {

	URI location; // desired location of the composite repository
	String name = "Composite Metadata Repository";
	boolean compressed = true; // compress by default
	Map properties = new HashMap();

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() {
		IMetadataRepositoryManager manager = (IMetadataRepositoryManager) ServiceHelper.getService(Activator.getContext(), IMetadataRepositoryManager.class.getName());
		if (manager == null)
			throw new BuildException("Unable to aquire metadata repository manager service.");

		// remove the repo first.
		manager.removeRepository(location);

		// create the properties
		if (compressed)
			properties.put(IRepository.PROP_COMPRESSED, Boolean.toString(true));

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

}
