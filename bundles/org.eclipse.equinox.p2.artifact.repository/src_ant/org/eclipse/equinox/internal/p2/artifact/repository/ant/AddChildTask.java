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
 * Ant task to add a child artifact repository to an already-existing composite artifact repository.
 */
public class AddChildTask extends Task {

	URI location; // location of the composite repository
	URI child; // address of the child to add
	String comparatorID; // comparator to use for compare (optional)

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() {
		IArtifactRepositoryManager manager = (IArtifactRepositoryManager) ServiceHelper.getService(Activator.getContext(), IArtifactRepositoryManager.SERVICE_NAME);
		if (manager == null)
			throw new BuildException("Unable to acquire artifact repository manager service.");

		// get the composite repository
		CompositeArtifactRepository repo = null;
		try {
			repo = (CompositeArtifactRepository) manager.loadRepository(location, null);
		} catch (ProvisionException e) {
			throw new BuildException("Exception while loading repository.", e);
		}

		// just do a straight add if the user didn't specify a comparator.
		if (comparatorID == null) {
			repo.addChild(child);
			return;
		}

		// otherwise run the comparator when we try and add the child and print out the result.
		if (repo.addChild(child, comparatorID))
			System.out.println(child + " was added successfully.");
		else
			System.out.println(child + " was not added.");
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

	/*
	 * Set the identifier of the comparator to use.
	 */
	public void setComparatorID(String value) {
		comparatorID = value;
	}
}
