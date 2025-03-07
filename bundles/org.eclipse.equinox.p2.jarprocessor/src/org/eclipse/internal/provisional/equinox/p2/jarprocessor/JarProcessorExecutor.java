/*******************************************************************************
 * Copyright (c) 2007, 2021 IBM Corporation and others.
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

package org.eclipse.internal.provisional.equinox.p2.jarprocessor;

import java.io.*;
import java.util.Properties;
import java.util.Set;
import org.eclipse.equinox.internal.p2.jarprocessor.*;

public class JarProcessorExecutor {
	public static class Options {
		public String outputDir = "."; //$NON-NLS-1$
		public String signCommand = null;
		@Deprecated(forRemoval = true, since = "1.2.0")
		public boolean pack = false;
		@Deprecated(forRemoval = true, since = "1.2.0")
		public boolean repack = false;
		@Deprecated(forRemoval = true, since = "1.2.0")
		public boolean unpack = false;
		public boolean verbose = false;
		public boolean processAll = false;
		public File input = null;
	}

	protected Options options = null;
	private Set<String> signExclusions = null;

	public void runJarProcessor(Options processOptions) {
		this.options = processOptions;
		if (options.input.isFile() && options.input.getName().endsWith(".zip")) { //$NON-NLS-1$
			ZipProcessor processor = new ZipProcessor();
			processor.setWorkingDirectory(options.outputDir);
			processor.setOptions(options);
			processor.setExecutor(this);
			try {
				processor.processZip(options.input);
			} catch (IOException e) {
				if (options.verbose) {
					e.printStackTrace();
				}
			}
		} else {
			JarProcessor processor = new JarProcessor();

			processor.setWorkingDirectory(options.outputDir);
			processor.setProcessAll(options.processAll);
			processor.setVerbose(options.verbose);

			// load options file
			Properties properties = new Properties();
			if (options.input.isDirectory()) {
				signExclusions = Utils.getSignExclusions(properties);
			}

			try {
				FileFilter filter = createFileFilter(options);
				process(options.input, filter, options.verbose, processor, properties);
			} catch (FileNotFoundException e) {
				if (options.verbose) {
					e.printStackTrace();
				}
			}
		}
	}

	protected FileFilter createFileFilter(Options processOptions) {
		return Utils.JAR_FILTER;
	}

	protected String getRelativeName(File file) {
		if (options.input == null) {
			return file.toString();
		}
		try {
			File input = options.input.getCanonicalFile();
			File subFile = file.getCanonicalFile();

			if (input.isFile()) {
				return subFile.getName();
			}

			if (!subFile.toString().startsWith(input.toString())) {
				// the file is not under the base folder.
				return file.toString();
			}

			File parent = subFile.getParentFile();
			String result = subFile.getName();
			while (!parent.equals(input)) {
				result = parent.getName() + '/' + result;
				parent = parent.getParentFile();
			}
			return result;

		} catch (IOException e) {
			return file.getName();
		}
	}

	private boolean shouldSign(String name) {
		if (options.signCommand == null) {
			return false;
		}
		return signExclusions == null ? true : !signExclusions.contains(name);
	}

	protected void process(File input, FileFilter filter, boolean verbose, JarProcessor processor,
			Properties packProperties) throws FileNotFoundException {
		if (!input.exists()) {
			throw new FileNotFoundException();
		}

		File[] files = null;
		if (input.isDirectory()) {
			files = input.listFiles();
		} else if (filter.accept(input)) {
			files = new File[] { input };
		} else {
			return;
		}
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				processDirectory(files[i], filter, verbose, processor, packProperties);
			} else if (filter.accept(files[i])) {
				try {
					processor.clearProcessSteps();
					String name = getRelativeName(files[i]);
					boolean sign = shouldSign(name);

					if (sign) {
						processor.clearProcessSteps();
						if (sign) {
							addSignStep(processor, packProperties, options);
						}
						files[i] = processor.processJar(files[i]);
					}

				} catch (IOException e) {
					if (verbose) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	protected void processDirectory(File input, FileFilter filter, boolean verbose, JarProcessor processor,
			Properties packProperties) throws FileNotFoundException {
		if (!input.isDirectory()) {
			return;
		}
		String dir = processor.getWorkingDirectory();
		processor.setWorkingDirectory(dir + "/" + input.getName()); //$NON-NLS-1$
		process(input, filter, verbose, processor, packProperties);
		processor.setWorkingDirectory(dir);
	}

	@Deprecated(forRemoval = true, since = "1.2.0")
	public void addPackUnpackStep(JarProcessor processor, Properties properties,
			JarProcessorExecutor.Options processOptions) {
		// NO-OP see https://github.com/eclipse-equinox/p2/issues/40
	}

	public void addSignStep(JarProcessor processor, Properties properties,
			JarProcessorExecutor.Options processOptions) {
		processor.addProcessStep(new SignCommandStep(properties, processOptions.signCommand, processOptions.verbose));
	}

	@Deprecated(forRemoval = true, since = "1.2.0")
	public void addPackStep(JarProcessor processor, Properties properties,
			JarProcessorExecutor.Options processOptions) {
		// NO-OP see https://github.com/eclipse-equinox/p2/issues/40
	}

	@Deprecated(forRemoval = true, since = "1.2.0")
	public void addUnpackStep(JarProcessor processor, Properties properties,
			JarProcessorExecutor.Options processOptions) {
		// NO-OP see https://github.com/eclipse-equinox/p2/issues/40
	}
}
