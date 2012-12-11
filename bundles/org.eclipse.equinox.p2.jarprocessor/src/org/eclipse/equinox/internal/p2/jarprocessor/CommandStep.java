/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.jarprocessor;

import java.io.File;
import java.util.List;
import java.util.Properties;
import org.eclipse.internal.provisional.equinox.p2.jarprocessor.IProcessStep;

public abstract class CommandStep implements IProcessStep {
	protected String command = null;
	protected String extension = null;
	private Properties options = null;
	protected boolean verbose = false;

	public CommandStep(Properties options, String command, String extension, boolean verbose) {
		this.command = command;
		this.extension = extension;
		this.options = options;
		this.verbose = verbose;
	}

	protected static int execute(String[] cmd) {
		return execute(cmd, false);
	}

	protected static int execute(String[] cmd, boolean verbose) {
		Runtime runtime = Runtime.getRuntime();
		Process proc = null;
		try {
			proc = runtime.exec(cmd);
			StreamProcessor.start(proc.getErrorStream(), StreamProcessor.STDERR, verbose);
			StreamProcessor.start(proc.getInputStream(), StreamProcessor.STDOUT, verbose);
		} catch (Exception e) {
			if (verbose) {
				System.out.println("Error executing command " + Utils.concat(cmd)); //$NON-NLS-1$
				e.printStackTrace();
			}
			return -1;
		}
		try {
			int result = proc.waitFor();
			return result;
		} catch (InterruptedException e) {
			if (verbose)
				e.printStackTrace();
		}
		return -1;
	}

	public Properties getOptions() {
		if (options == null)
			options = new Properties();
		return options;
	}

	public boolean adjustInf(File input, Properties inf, List<Properties> containers) {
		//do nothing by default
		return false;
	}
}
