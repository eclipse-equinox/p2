/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.p2.jarprocessor.verifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Properties;

import org.eclipse.equinox.internal.p2.jarprocessor.UnpackStep;
import org.eclipse.equinox.internal.p2.jarprocessor.Utils;
import org.eclipse.internal.provisional.equinox.p2.jarprocessor.JarProcessor;
import org.eclipse.internal.provisional.equinox.p2.jarprocessor.JarProcessorExecutor;

public class Verifier extends JarProcessorExecutor {

	private static void printUsage() {
		System.out.println("This tool verifies that unpacking a pack.gz file with the jarprocessor results in a valid jar file."); //$NON-NLS-1$
		System.out.println("Usage: java -cp jarprocessor.jar org.eclipse.update.internal.jarprocessor.verifier.Verifier -dir <workingDirectory> input [input]"); //$NON-NLS-1$
		System.out.println(""); //$NON-NLS-1$
		System.out.println("-dir : specifies a working directory where pack.gz files can be temporarily unpacked"); //$NON-NLS-1$
		System.out.println("input : a list of directories and/or pack.gz files to verify."); //$NON-NLS-1$

	}

	public static void main(String[] args) {
		if (!VerifyStep.canVerify()) {
			System.out.println("Can't find jarsigner.  Please adjust your system path or use a jdk."); //$NON-NLS-1$
			printUsage();
			return;
		}

		String workingDirectory = null;
		String [] input;
		
		if(args.length == 0) {
			workingDirectory = "."; //$NON-NLS-1$
			input = new String[] {"."}; //$NON-NLS-1$
		}else {
			int idx = 0;
			if(args[0] == "-help"){ //$NON-NLS-1$
				printUsage();
				return;
			}
			if (args[idx] == "-dir") { //$NON-NLS-1$
				workingDirectory = args[++idx];
				idx++;
			} else {
				workingDirectory = "temp"; //$NON-NLS-1$
			}

			input = new String[args.length - idx];
			System.arraycopy(args, idx, input, 0, args.length - idx);
		}
		
		File workingDir = new File(workingDirectory);
		boolean clear = false;
		if( workingDir.exists()) {
			workingDir = new File( workingDir, "jarprocessor.verifier.temp"); //$NON-NLS-1$
			clear = true;
		}
		
		Verifier verifier = new Verifier();
		verifier.verify(workingDir, input);
		
		if(clear)
			workingDir.deleteOnExit();
	}

	public void verify(final File workingDirectory, String[] input) {
		Properties options = new Properties();

		JarProcessor unpacker = new JarProcessor();
		unpacker.addProcessStep(new UnpackStep(options, false));
		unpacker.setWorkingDirectory(workingDirectory.getAbsolutePath());

		/* There is no need to use a full processor to do the verification unless we want to verify nested jars as well.
		 * So for now, use a custom processor to just call the verify step directly.
		 */
		final VerifyStep verifyStep = new VerifyStep(options, false);
		JarProcessor verifier = new JarProcessor() {
			public File processJar(File inputFile) {
				return verifyStep.postProcess(inputFile, workingDirectory, null);
			}
		};

		for (int i = 0; i < input.length; i++) {
			File inputFile = new File(input[i]);
			if (inputFile.exists()) {
				try {
					process(inputFile, Utils.PACK_GZ_FILTER, true, unpacker, verifier);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
		Utils.clear(workingDirectory);
	}
}
