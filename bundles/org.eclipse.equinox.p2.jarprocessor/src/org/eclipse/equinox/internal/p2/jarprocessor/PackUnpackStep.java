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

/**
 * @author aniefer@ca.ibm.com
 *
 */
public class PackUnpackStep extends PackStep {
	private Set<String> exclusions = null;

	public PackUnpackStep(Properties options) {
		super(options);
		exclusions = Utils.getPackExclusions(options);
	}

	public PackUnpackStep(Properties options, boolean verbose) {
		super(options, verbose);
		exclusions = Utils.getPackExclusions(options);
	}

	@Override
	public String recursionEffect(String entryName) {
		if (canPack() && entryName.endsWith(".jar") && !exclusions.contains(entryName)) { //$NON-NLS-1$
			return entryName;
		}
		return null;
	}

	@Override
	public File postProcess(File input, File workingDirectory, List<Properties> containers) {
		if (canPack() && packCommand != null && input != null) {
			Properties inf = Utils.getEclipseInf(input, verbose);
			if (!shouldPack(input, containers, inf))
				return null;
			File tempFile = new File(workingDirectory, "temp_" + input.getName()); //$NON-NLS-1$
			try {
				String[] tmp = getCommand(input, tempFile, inf, containers);
				String[] cmd = new String[tmp.length + 1];
				cmd[0] = tmp[0];
				cmd[1] = "-r"; //$NON-NLS-1$
				System.arraycopy(tmp, 1, cmd, 2, tmp.length - 1);

				int result = execute(cmd, verbose);
				if (result == 0 && tempFile.exists()) {
					File finalFile = new File(workingDirectory, input.getName());
					if (finalFile.exists())
						finalFile.delete();
					tempFile.renameTo(finalFile);
					return finalFile;
				} else if (verbose) {
					System.out.println("Error: " + result + " was returned from command: " + Utils.concat(cmd)); //$NON-NLS-1$ //$NON-NLS-2$
				}
			} catch (IOException e) {
				if (verbose)
					e.printStackTrace();
				return null;
			}
		}
		return null;
	}

	@Override
	public File preProcess(File input, File workingDirectory, List<Properties> containers) {
		return null;
	}

	@Override
	public String getStepName() {
		return "Repack"; //$NON-NLS-1$
	}
}
