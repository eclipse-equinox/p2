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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.internal.p2.metadata.mirror.MirrorApplication;
import org.osgi.framework.Bundle;

/**
 * Ant task for running the metadata mirror application.
 */
public class MirrorApplicationTask extends Task {

	private static final String EMPTY_STRING = ""; //$NON-NLS-1$
	private static final String ARG_DESTINATION = "-destination"; //$NON-NLS-1$
	private static final String ARG_SOURCE = "-source"; //$NON-NLS-1$
	private static final String ARG_WRITE_MODE = "-writeMode"; //$NON-NLS-1$
	URL source;
	URL destination;
	String writeMode;

	//TODO add transitive and roots attributed when implemented.

	/*
	 * Run the mirror application with the given arguments.
	 */
	private void runMirrorApplication(final String[] args) throws Exception {
		new MirrorApplication().start(new IApplicationContext() {

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
		// create arguments
		String[] args = new String[] { //
		ARG_SOURCE, source.toExternalForm(), //
				ARG_DESTINATION, destination.toExternalForm(), // 
				ARG_WRITE_MODE, writeMode == null ? EMPTY_STRING : writeMode};

		try {
			runMirrorApplication(args);
		} catch (Exception e) {
			throw new BuildException("Error occurred while running metadata mirror application.", e);
		}
	}

	/*
	 * Set the source location.
	 */
	public void setSource(String value) throws MalformedURLException {
		source = new URL(value);
	}

	/*
	 * Set the destination location.
	 */
	public void setDestination(String value) throws MalformedURLException {
		destination = new URL(value);
	}

	/*
	 * Set the write mode for the application. (e.g. clean or append)
	 */
	public void setWriteMode(String value) {
		writeMode = value;
	}
}