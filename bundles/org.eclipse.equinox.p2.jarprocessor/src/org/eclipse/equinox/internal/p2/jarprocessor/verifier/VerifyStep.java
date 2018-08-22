/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others. 
 *
 * This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.jarprocessor.verifier;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import org.eclipse.equinox.internal.p2.jarprocessor.CommandStep;
import org.eclipse.equinox.internal.p2.jarprocessor.Utils;

public class VerifyStep extends CommandStep {

	static String verifyCommand = "jarsigner"; //$NON-NLS-1$
	static Boolean canVerify = null;

	public static boolean canVerify() {
		if (canVerify != null)
			return canVerify.booleanValue();

		String javaHome = System.getProperty("java.home"); //$NON-NLS-1$
		String command = javaHome + "/../bin/jarsigner"; //$NON-NLS-1$
		int result = execute(new String[] {command});
		if (result < 0) {
			command = "jarsigner"; //$NON-NLS-1$
			result = execute(new String[] {command});
			if (result < 0) {
				canVerify = Boolean.FALSE;
				return false;
			}
		}
		verifyCommand = command;
		canVerify = Boolean.TRUE;
		return true;
	}

	public VerifyStep(Properties options, boolean verbose) {
		super(options, verifyCommand, ".jar", verbose); //$NON-NLS-1$
	}

	@Override
	public String getStepName() {
		return "Verify"; //$NON-NLS-1$
	}

	@Override
	public File postProcess(File input, File workingDirectory, List<Properties> containers) {
		if (canVerify() && verifyCommand != null) {
			try {
				System.out.print("Verifying " + input.getName() + ":  "); //$NON-NLS-1$ //$NON-NLS-2$
				String[] cmd = new String[] {verifyCommand, "-verify", input.getCanonicalPath()}; //$NON-NLS-1$
				int result = execute(cmd, true);
				if (result != 0 && verbose)
					System.out.println("Error: " + result + " was returned from command: " + Utils.concat(cmd)); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (IOException e) {
				if (verbose)
					e.printStackTrace();
				return null;
			}
			return input;
		}
		return null;
	}

	@Override
	public File preProcess(File input, File workingDirectory, List<Properties> containers) {
		return null;
	}

	@Override
	public String recursionEffect(String entryName) {
		return null;
	}

}
