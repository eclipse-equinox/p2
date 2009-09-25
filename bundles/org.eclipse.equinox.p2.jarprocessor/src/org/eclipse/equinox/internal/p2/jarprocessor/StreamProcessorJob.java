/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.jarprocessor;

import java.io.InputStream;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;

public class StreamProcessorJob extends Job {
	private InputStream inputStream = null;
	private String name = null;
	private boolean verbose = false;

	public StreamProcessorJob(InputStream stream, String name, boolean verbose) {
		super(StreamProcessor.STREAM_PROCESSOR + " : " + name); //$NON-NLS-1$
		setPriority(Job.SHORT);
		setSystem(true);
		this.inputStream = stream;
		this.name = name;
		this.verbose = verbose;
	}

	protected IStatus run(IProgressMonitor monitor) {
		StreamProcessor.run(inputStream, name, verbose);
		inputStream = null;
		return Status.OK_STATUS;
	}
}
