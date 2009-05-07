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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.artifact.mirror.MirrorApplication;
import org.osgi.framework.Bundle;

/**
 * Ant task for running the artifact repository mirroring application.
 */
public class MirrorApplicationTask extends Task {

	private static final String EMPTY_STRING = ""; //$NON-NLS-1$
	private static final String ARG_COMPARATOR = "-comparator"; //$NON-NLS-1$
	private static final String ARG_COMPARE = "-compare"; //$NON-NLS-1$
	private static final String ARG_COMPARE_AGAINST = "-compareAgainst"; //$NON-NLS-1$
	private static final String ARG_COMPARATOR_LOG = "-comparatorLog"; //$NON-NLS-1$
	private static final String ARG_DESTINATION = "-destination"; //$NON-NLS-1$
	private static final String ARG_DESTINATION_NAME = "-destinationName"; //$NON-NLS-1$
	private static final String ARG_IGNORE_ERRORS = "-ignoreErrors"; //$NON-NLS-1$
	private static final String ARG_LOG = "-log"; //$NON-NLS-1$
	private static final String ARG_RAW = "-raw"; //$NON-NLS-1$
	private static final String ARG_SOURCE = "-source"; //$NON-NLS-1$
	private static final String ARG_VERBOSE = "-verbose"; //$NON-NLS-1$
	private static final String ARG_WRITE_MODE = "-writeMode"; //$NON-NLS-1$

	URL source;
	URL destination;
	String destinationName;
	URL baseline; // location of known good repository for compare against (optional)
	File mirrorLog; // file to log mirror output to (optional)
	File comparatorLog; // file to comparator output to (optional)
	String comparatorID; // specifies a comparator (optional)
	String writeMode;
	boolean compare = false;
	boolean ignoreErrors = false;
	boolean raw = false; // use raw artifact descriptors?
	boolean verbose = false;

	/*
	 * Runs the mirror application with the given arguments.
	 */
	private void runMirrorApplication(final String[] args) throws Exception {
		MirrorApplication app = new MirrorApplication();
		if (mirrorLog == null)
			app.setLog(new AntMirrorLog(this));
		app.start(new IApplicationContext() {

			public void applicationRunning() {
				// nothing to do
			}

			public Map getArguments() {
				Map arguments = new HashMap();
				arguments.put(IApplicationContext.APPLICATION_ARGS, args);
				return arguments;
			}

			public String getBrandingApplication() {
				return null;
			}

			public Bundle getBrandingBundle() {
				return null;
			}

			public String getBrandingDescription() {
				return null;
			}

			public String getBrandingId() {
				return null;
			}

			public String getBrandingName() {
				return null;
			}

			public String getBrandingProperty(String key) {
				return null;
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() {
		// Compare against if baseline specified
		boolean compareAgainst = baseline != null;
		boolean comparator = comparatorID != null;

		// create arguments
		String[] args = new String[] { //
		ARG_SOURCE, source.toExternalForm(), //
				ARG_DESTINATION, destination.toExternalForm(), //
				ARG_WRITE_MODE, writeMode == null ? EMPTY_STRING : writeMode, //
				compare ? ARG_COMPARE : EMPTY_STRING, //
				ignoreErrors ? ARG_IGNORE_ERRORS : EMPTY_STRING, //
				raw ? ARG_RAW : EMPTY_STRING, //
				verbose ? ARG_VERBOSE : EMPTY_STRING, //
				compareAgainst ? ARG_COMPARE_AGAINST : EMPTY_STRING, //
				compareAgainst ? baseline.toExternalForm() : EMPTY_STRING, //
				comparator ? ARG_COMPARATOR : EMPTY_STRING, //
				comparator ? comparatorID : EMPTY_STRING, //
				mirrorLog != null ? ARG_LOG : EMPTY_STRING, //
				mirrorLog != null ? mirrorLog.getAbsolutePath() : EMPTY_STRING, //
				comparatorLog != null ? ARG_COMPARATOR_LOG : EMPTY_STRING, //
				comparatorLog != null ? comparatorLog.getAbsolutePath() : EMPTY_STRING, //
				destinationName != null ? ARG_DESTINATION_NAME : EMPTY_STRING, //
				destinationName != null ? destinationName : EMPTY_STRING};

		try {
			runMirrorApplication(args);
		} catch (Exception e) {
			throw new BuildException("Exception while running mirror application.", e);
		}
	}

	/*
	 * Set the location of the source.
	 */
	public void setSource(String value) throws MalformedURLException {
		source = new URL(value);
	}

	/*
	 * Set the location of the destination.
	 */
	public void setDestination(String value) throws MalformedURLException {
		destination = new URL(value);
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
	public void setBaseline(String value) throws MalformedURLException {
		baseline = new URL(value);
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
}
