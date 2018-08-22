/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.jarprocessor;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SignCommandStep extends CommandStep {
	private Set<String> exclusions = null;

	public SignCommandStep(Properties options, String command) {
		super(options, command, ".jar", false); //$NON-NLS-1$
		exclusions = Utils.getSignExclusions(options);
	}

	public SignCommandStep(Properties options, String command, boolean verbose) {
		super(options, command, ".jar", verbose); //$NON-NLS-1$
		exclusions = Utils.getSignExclusions(options);
	}

	@Override
	public String recursionEffect(String entryName) {
		if (entryName.endsWith(extension) && !exclusions.contains(entryName))
			return entryName;
		return null;
	}

	@Override
	public File preProcess(File input, File workingDirectory, List<Properties> containers) {
		return null;
	}

	@Override
	public File postProcess(File input, File workingDirectory, List<Properties> containers) {
		if (command != null && input != null && shouldSign(input, containers)) {
			try {
				String[] cmd = new String[] {command, input.getCanonicalPath()};
				int result = execute(cmd, verbose);
				if (result == 0) {
					return input;
				} else if (verbose) {
					System.out.println("Error: " + result + " was returned from command: " + Utils.concat(cmd)); //$NON-NLS-1$ //$NON-NLS-2$
				}
			} catch (IOException e) {
				if (verbose) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	public boolean shouldSign(File input, List<Properties> containers) {
		Properties inf = null;

		//1: Are we excluded from signing by our parents?
		//innermost jar is first on the list, it overrides outer jars
		for (Iterator<Properties> iterator = containers.iterator(); iterator.hasNext();) {
			inf = iterator.next();
			if (inf.containsKey(Utils.MARK_EXCLUDE_CHILDREN_SIGN)) {
				if (Boolean.parseBoolean(inf.getProperty(Utils.MARK_EXCLUDE_CHILDREN_SIGN))) {
					if (verbose)
						System.out.println(input.getName() + "is excluded from signing by its containers."); //$NON-NLS-1$ 
					return false;
				}
				break;
			}
		}

		//2: Is this jar itself marked as exclude?
		inf = Utils.getEclipseInf(input, verbose);
		if (inf != null && inf.containsKey(Utils.MARK_EXCLUDE_SIGN) && Boolean.parseBoolean(inf.getProperty(Utils.MARK_EXCLUDE_SIGN))) {
			if (verbose)
				System.out.println("Excluding " + input.getName() + " from signing."); //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}
		return true;
	}

	@Override
	public String getStepName() {
		return "Sign"; //$NON-NLS-1$
	}
}
