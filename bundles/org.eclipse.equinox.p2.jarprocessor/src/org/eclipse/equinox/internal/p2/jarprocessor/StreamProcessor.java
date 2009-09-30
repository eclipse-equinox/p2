/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.jarprocessor;

import java.io.*;

public class StreamProcessor {
	private static final String JOBS = "org.eclipse.core.runtime.jobs.Job"; //$NON-NLS-1$
	public static final String STREAM_PROCESSOR = "Stream Processor"; //$NON-NLS-1$
	public static final String STDERR = "STDERR"; //$NON-NLS-1$
	public static final String STDOUT = "STDOUT"; //$NON-NLS-1$

	static private boolean haveJobs = false;

	static {
		try {
			haveJobs = (Class.forName(JOBS) != null);
		} catch (ClassNotFoundException e) {
			//no jobs
		}
	}

	static public void start(final InputStream is, final String name, final boolean verbose) {
		if (haveJobs) {
			new StreamProcessorJob(is, name, verbose).schedule();
		} else {
			Thread job = new Thread(STREAM_PROCESSOR) {
				public void run() {
					StreamProcessor.run(is, name, verbose);
				}
			};
			job.start();
		}
	}

	static public void run(InputStream inputStream, String name, boolean verbose) {
		try {
			InputStreamReader isr = new InputStreamReader(inputStream);
			BufferedReader br = new BufferedReader(isr);
			while (true) {
				String s = br.readLine();
				if (s == null) {
					break;
				}
				if (verbose) {
					if (STDERR.equals(name))
						System.err.println(name + ": " + s); //$NON-NLS-1$
					else
						System.out.println(name + ": " + s); //$NON-NLS-1$
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
