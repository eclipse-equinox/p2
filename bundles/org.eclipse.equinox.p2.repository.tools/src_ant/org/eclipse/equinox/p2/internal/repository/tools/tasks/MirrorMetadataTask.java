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
package org.eclipse.equinox.p2.internal.repository.tools.tasks;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.internal.repository.tools.MirrorApplication;
import org.eclipse.equinox.p2.internal.repository.tools.RepositoryDescriptor;

/**
 * Ant task for running the metadata mirror application.
 */
public class MirrorMetadataTask extends Task {
	URI source;
	URI destination;
	String destinationName;
	String writeMode;
	boolean references = true;

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() {
		RepositoryDescriptor destinationRepo = new RepositoryDescriptor();
		destinationRepo.setName(destinationName);
		destinationRepo.setLocation(destination);
		destinationRepo.setKind(RepositoryDescriptor.KIND_METADATA);
		destinationRepo.setFormat(source);
		if (writeMode != null && writeMode.equals("clean")) //$NON-NLS-1$
			destinationRepo.setAppend(false);

		RepositoryDescriptor sourceRepo = new RepositoryDescriptor();
		sourceRepo.setLocation(source);
		sourceRepo.setKind(RepositoryDescriptor.KIND_METADATA);

		MirrorApplication app = new MirrorApplication();
		app.addDestination(destinationRepo);
		app.addSource(sourceRepo);
		app.setReferences(references);
		try {
			IStatus result = app.run(null);
			if (result.getSeverity() != IStatus.OK)
				log(result.getMessage());
		} catch (ProvisionException e) {
			throw new BuildException(e);
		}
	}

	/*
	 * Set the source location.
	 */
	public void setSource(String value) {
		try {
			source = URIUtil.fromString(value);
		} catch (URISyntaxException e) {
			throw new BuildException(e);
		}
	}

	/*
	 * Set the destination location.
	 */
	public void setDestination(String value) {
		try {
			destination = URIUtil.fromString(value);
		} catch (URISyntaxException e) {
			throw new BuildException(e);
		}
	}

	/*
	 * Set the destination name.
	 */
	public void setDestinationName(String value) {
		destinationName = value;
	}

	/*
	 * Set the write mode for the application. (e.g. clean or append)
	 */
	public void setWriteMode(String value) {
		writeMode = value;
	}

	public void setReferences(boolean value) {
		references = value;
	}
}