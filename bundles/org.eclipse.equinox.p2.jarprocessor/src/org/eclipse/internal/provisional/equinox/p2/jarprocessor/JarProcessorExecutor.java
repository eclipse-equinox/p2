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

package org.eclipse.internal.provisional.equinox.p2.jarprocessor;

import java.io.*;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipException;
import org.eclipse.equinox.internal.p2.jarprocessor.*;

public class JarProcessorExecutor {
	public static class Options {
		public String outputDir = "."; //$NON-NLS-1$
		public String signCommand = null;
		public boolean pack = false;
		public boolean repack = false;
		public boolean unpack = false;
		public boolean verbose = false;
		public boolean processAll = false;
		public File input = null;
	}

	protected Options options = null;
	private Set<String> packExclusions = null;
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
			} catch (ZipException e) {
				if (options.verbose)
					e.printStackTrace();
			} catch (IOException e) {
				if (options.verbose)
					e.printStackTrace();
			}
		} else {
			JarProcessor processor = new JarProcessor();

			processor.setWorkingDirectory(options.outputDir);
			processor.setProcessAll(options.processAll);
			processor.setVerbose(options.verbose);

			//load options file
			Properties properties = new Properties();
			if (options.input.isDirectory()) {
				File packProperties = new File(options.input, "pack.properties"); //$NON-NLS-1$
				if (packProperties.exists() && packProperties.isFile()) {
					InputStream in = null;
					try {
						in = new BufferedInputStream(new FileInputStream(packProperties));
						properties.load(in);
					} catch (IOException e) {
						if (options.verbose)
							e.printStackTrace();
					} finally {
						Utils.close(in);
					}
				}

				packExclusions = Utils.getPackExclusions(properties);
				signExclusions = Utils.getSignExclusions(properties);
			}

			try {
				FileFilter filter = createFileFilter(options);
				process(options.input, filter, options.verbose, processor, properties);
			} catch (FileNotFoundException e) {
				if (options.verbose)
					e.printStackTrace();
			}
		}
	}

	protected FileFilter createFileFilter(Options processOptions) {
		return processOptions.unpack ? Utils.PACK_GZ_FILTER : Utils.JAR_FILTER;
	}

	protected String getRelativeName(File file) {
		if (options.input == null)
			return file.toString();
		try {
			File input = options.input.getCanonicalFile();
			File subFile = file.getCanonicalFile();

			if (input.isFile())
				return subFile.getName();

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

	private boolean shouldPack(String name) {
		if (!options.pack)
			return false;
		return packExclusions == null ? true : !packExclusions.contains(name);
	}

	private boolean shouldSign(String name) {
		if (options.signCommand == null)
			return false;
		return signExclusions == null ? true : !signExclusions.contains(name);
	}

	private boolean shouldRepack(String name) {
		if (shouldSign(name) && shouldPack(name))
			return true;
		if (!options.repack)
			return false;
		return packExclusions == null ? true : !packExclusions.contains(name);
	}

	protected void process(File input, FileFilter filter, boolean verbose, JarProcessor processor, Properties packProperties) throws FileNotFoundException {
		if (!input.exists())
			throw new FileNotFoundException();

		File[] files = null;
		if (input.isDirectory()) {
			files = input.listFiles();
		} else if (filter.accept(input)) {
			files = new File[] {input};
		} else
			return;
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				processDirectory(files[i], filter, verbose, processor, packProperties);
			} else if (filter.accept(files[i])) {
				try {
					processor.clearProcessSteps();
					if (options.unpack) {
						addUnpackStep(processor, packProperties, options);
						processor.processJar(files[i]);
					} else {
						String name = getRelativeName(files[i]);
						boolean sign = shouldSign(name);
						boolean repack = shouldRepack(name);

						if (repack || sign) {
							processor.clearProcessSteps();
							if (repack)
								addPackUnpackStep(processor, packProperties, options);
							if (sign)
								addSignStep(processor, packProperties, options);
							files[i] = processor.processJar(files[i]);
						}

						if (shouldPack(name)) {
							processor.clearProcessSteps();
							addPackStep(processor, packProperties, options);
							processor.processJar(files[i]);
						}
					}
				} catch (IOException e) {
					if (verbose)
						e.printStackTrace();
				}
			}
		}
	}

	protected void processDirectory(File input, FileFilter filter, boolean verbose, JarProcessor processor, Properties packProperties) throws FileNotFoundException {
		if (!input.isDirectory())
			return;
		String dir = processor.getWorkingDirectory();
		processor.setWorkingDirectory(dir + "/" + input.getName()); //$NON-NLS-1$
		process(input, filter, verbose, processor, packProperties);
		processor.setWorkingDirectory(dir);
	}

	public void addPackUnpackStep(JarProcessor processor, Properties properties, JarProcessorExecutor.Options processOptions) {
		processor.addProcessStep(new PackUnpackStep(properties, processOptions.verbose));
	}

	public void addSignStep(JarProcessor processor, Properties properties, JarProcessorExecutor.Options processOptions) {
		processor.addProcessStep(new SignCommandStep(properties, processOptions.signCommand, processOptions.verbose));
	}

	public void addPackStep(JarProcessor processor, Properties properties, JarProcessorExecutor.Options processOptions) {
		processor.addProcessStep(new PackStep(properties, processOptions.verbose));
	}

	public void addUnpackStep(JarProcessor processor, Properties properties, JarProcessorExecutor.Options processOptions) {
		processor.addProcessStep(new UnpackStep(properties, processOptions.verbose));
	}
}
