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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.p2.internal.repository.tools.MirrorApplication;
import org.eclipse.equinox.p2.internal.repository.tools.RepositoryDescriptor;

/**
 * Ant task for running the artifact repository mirroring application.
 */
public class MirrorArtifactsTask extends Task {
	URI source;
	URI destination;
	String destinationName;
	URI baseline; // location of known good repository for compare against (optional)
	File mirrorLog; // file to log mirror output to (optional)
	File comparatorLog; // file to comparator output to (optional)
	String comparatorID; // specifies a comparator (optional)
	String writeMode;
	boolean compare = false;
	boolean ignoreErrors = false;
	boolean raw = false; // use raw artifact descriptors?
	boolean verbose = false;
	boolean references = true;

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() {
		// Compare against if baseline specified
		RepositoryDescriptor destinationRepo = new RepositoryDescriptor();
		destinationRepo.setName(destinationName);
		destinationRepo.setLocation(destination);
		destinationRepo.setKind(RepositoryDescriptor.KIND_ARTIFACT);
		destinationRepo.setFormat(source);
		if (writeMode != null && writeMode.equals("clean")) //$NON-NLS-1$
			destinationRepo.setAppend(false);

		RepositoryDescriptor sourceRepo = new RepositoryDescriptor();
		sourceRepo.setLocation(source);
		sourceRepo.setKind(RepositoryDescriptor.KIND_ARTIFACT);

		MirrorApplication app = new MirrorApplication();
		app.addDestination(destinationRepo);
		app.addSource(sourceRepo);
		app.setRaw(raw);
		app.setIgnoreErrors(ignoreErrors);
		app.setVerbose(verbose);
		app.setCompare(compare);
		app.setComparatorID(comparatorID);
		app.setBaseline(baseline);
		app.setReferences(references);
		if (comparatorLog != null)
			app.setComparatorLog(comparatorLog);
		if (mirrorLog != null)
			app.setLog(mirrorLog);
		else {
			try {
				app.setLog(new AntMirrorLog(this));
			} catch (NoSuchMethodException e) {
				//shouldn't happen
			}
		}

		try {
			app.run(null);
		} catch (Exception e) {
			throw new BuildException("Exception while running mirror application.", e); //$NON-NLS-1$
		}
	}

	/*
	 * Set the location of the source.
	 */
	public void setSource(String value) {
		try {
			source = URIUtil.fromString(value);
		} catch (URISyntaxException e) {
			throw new BuildException(e);
		}
	}

	/*
	 * Set the location of the destination.
	 */
	public void setDestination(String value) {
		try {
			destination = URIUtil.fromString(value);
		} catch (URISyntaxException e) {
			throw new BuildException(e);
		}
	}

	/*
	 * Set the name of the destination repository.
	 */
	public void setDestinationName(String value) {
		destinationName = value;
	}

	/*
	 * Set the location of the baseline repository. (used in comparison)
	 */
	public void setBaseline(String value) {
		try {
			baseline = URIUtil.fromString(value);
		} catch (URISyntaxException e) {
			throw new BuildException(e);
		}
		compare = true;
	}

	/*
	 * Set the identifier of the comparator to use.
	 */
	public void setComparatorID(String value) {
		comparatorID = value;
		compare = true;
	}

	/*
	 * Set the location of the comparator log
	 */
	public void setComparatorLog(String value) {
		comparatorLog = new File(value);
	}

	/*
	 * Set the write mode. (e.g. clean or append)
	 */
	public void setWriteMode(String value) {
		writeMode = value;
	}

	/*
	 * Set the log location if applicable
	 */
	public void setLog(String value) {
		mirrorLog = new File(value);
	}

	/*
	 * Set whether or not the application should be calling a comparator when mirroring.
	 */
	public void setCompare(boolean value) {
		compare = value;
	}

	/*
	 * Set whether or not we should ignore errors when running the mirror application.
	 */
	public void setIgnoreErrors(boolean value) {
		ignoreErrors = value;
	}

	/*
	 * Set whether or not the the artifacts are raw.
	 */
	public void setRaw(boolean value) {
		raw = value;
	}

	/*
	 * Set whether or not the mirror application should be run in verbose mode.
	 */
	public void setVerbose(boolean value) {
		verbose = value;
	}

	/*
	 * Set whether the repository references should be copied to the new repository
	 */
	public void setReferences(boolean value) {
		references = value;
	}
}
