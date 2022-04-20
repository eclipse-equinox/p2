/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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
import org.eclipse.internal.provisional.equinox.p2.jarprocessor.JarProcessorExecutor;

public class Main {

	private static void printUsage() {
		System.out.println("[-option ...]... input"); //$NON-NLS-1$
		System.out.println("The following options are supported:"); //$NON-NLS-1$
		System.out.println("-processAll     process all jars, regardless of whether they were previously normalized"); //$NON-NLS-1$
		System.out.println("                By default only normalized jars will be processed."); //$NON-NLS-1$
		System.out.println("-sign <command> sign jars using <command>"); //$NON-NLS-1$
		System.out.println();
		System.out.println("-outputDir <dir>  the output directory"); //$NON-NLS-1$
		System.out.println("-verbose        verbose mode "); //$NON-NLS-1$
	}

	public static JarProcessorExecutor.Options processArguments(String[] args) {
		if (args.length == 0) {
			printUsage();
			return null;
		}

		JarProcessorExecutor.Options options = new JarProcessorExecutor.Options();
		int i = 0;
		for (; i < args.length - 1; i++) {
			if (args[i].equals("-sign") && i < args.length - 2) { //$NON-NLS-1$
				if (args[i + 1].startsWith("-")) { //$NON-NLS-1$
					printUsage();
					return null;
				}
				options.signCommand = args[++i];
			} else if (args[i].equals("-outputDir") && i < args.length - 2) { //$NON-NLS-1$
				if (args[i + 1].startsWith("-")) { //$NON-NLS-1$
					printUsage();
					return null;
				}
				options.outputDir = args[++i];
			} else if (args[i].equals("-verbose")) { //$NON-NLS-1$
				options.verbose = true;
			} else if (args[i].equals("-processAll")) { //$NON-NLS-1$
				options.processAll = true;
			}
		}

		options.input = new File(args[i]);

		String problemMessage = null;
		String inputName = options.input.getName();
		if (options.input.isFile() && !inputName.endsWith(".zip") && !inputName.endsWith(".jar")) { //$NON-NLS-1$ //$NON-NLS-2$
			problemMessage = "Input file is not a jar file."; //$NON-NLS-1$
		}
		if (problemMessage != null) {
			System.out.println(problemMessage);
			System.out.println();
			printUsage();
			return null;
		}

		return options;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		JarProcessorExecutor.Options options = processArguments(args);
		if (options == null)
			return;
		new JarProcessorExecutor().runJarProcessor(options);
	}

}
