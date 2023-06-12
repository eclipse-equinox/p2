/*******************************************************************************
 *  Copyright (c) 2006, 2022 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.jarprocessor;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.eclipse.internal.provisional.equinox.p2.jarprocessor.JarProcessor;
import org.eclipse.internal.provisional.equinox.p2.jarprocessor.JarProcessorExecutor;

public class ZipProcessor {

	private JarProcessorExecutor executor = null;
	private JarProcessorExecutor.Options options = null;

	private String workingDirectory = null;
	private Properties properties = null;
	private Set<String> signExclusions = null;

	public void setExecutor(JarProcessorExecutor executor) {
		this.executor = executor;
	}

	public void setOptions(JarProcessorExecutor.Options options) {
		this.options = options;
	}

	public void setWorkingDirectory(String dir) {
		workingDirectory = dir;
	}

	public String getWorkingDirectory() {
		if (workingDirectory == null)
			workingDirectory = "."; //$NON-NLS-1$
		return workingDirectory;
	}

	public void processZip(File zipFile) throws ZipException, IOException {
		if (options.verbose)
			System.out.println("Processing " + zipFile.getPath()); //$NON-NLS-1$
		try (ZipFile zip = new ZipFile(zipFile)) {
			initialize(zip);

			String extension = ".jar"; //$NON-NLS-1$
			File tempDir = new File(getWorkingDirectory(), "temp_" + zipFile.getName()); //$NON-NLS-1$
			JarProcessor processor = new JarProcessor();
			processor.setVerbose(options.verbose);
			processor.setProcessAll(options.processAll);
			processor.setWorkingDirectory(tempDir.getCanonicalPath());

			File outputFile = new File(getWorkingDirectory(), zipFile.getName() + ".temp"); //$NON-NLS-1$
			File parent = outputFile.getParentFile();
			if (!parent.exists())
				parent.mkdirs();
			try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(outputFile))) {
				Enumeration<? extends ZipEntry> entries = zip.entries();
				if (entries.hasMoreElements()) {
					for (ZipEntry entry = entries.nextElement(); entry != null; entry = entries.hasMoreElements()
							? (ZipEntry) entries.nextElement()
							: null) {
						String name = entry.getName();

						InputStream entryStream = zip.getInputStream(entry);

						boolean sign = options.signCommand != null && !signExclusions.contains(name);

						File extractedFile = null;

						if (entry.getName().endsWith(extension) && (sign)) {
							extractedFile = createSubPathFile(tempDir, name);
							parent = extractedFile.getParentFile();
							if (!parent.exists())
								parent.mkdirs();
							if (options.verbose)
								System.out.println("Extracting " + entry.getName()); //$NON-NLS-1$
							FileOutputStream extracted = new FileOutputStream(extractedFile);
							Utils.transferStreams(entryStream, extracted, true); // this will close the stream
							entryStream = null;

							boolean skip = Utils.shouldSkipJar(extractedFile, options.processAll, options.verbose);
							if (skip) {
								// skipping this file
								entryStream = new FileInputStream(extractedFile);
								if (options.verbose)
									System.out.println(entry.getName() + " is not marked, skipping."); //$NON-NLS-1$
							} else {
								if (sign) {
									processor.clearProcessSteps();
									if (sign)
										executor.addSignStep(processor, properties, options);
									extractedFile = processor.processJar(extractedFile);
								}
								if (extractedFile.exists()) {
									try {
										entryStream = new FileInputStream(extractedFile);
									} catch (IOException e) {
										if (options.verbose) {
											e.printStackTrace();
											System.out.println(
													"Warning: Problem reading " + extractedFile.getPath() + "."); //$NON-NLS-1$//$NON-NLS-2$
										}
									}
								}

								if (options.verbose && entryStream != null) {
									System.out.println("Adding " + name + " to " + outputFile.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
								}
							}
						}
						if (entryStream != null) {
							ZipEntry newEntry = new ZipEntry(name);
							try {
								zipOut.putNextEntry(newEntry);
								Utils.transferStreams(entryStream, zipOut, false);
								zipOut.closeEntry();
							} catch (ZipException e) {
								if (options.verbose) {
									System.out.println("Warning: " + name + " already exists in " + outputFile.getName() //$NON-NLS-1$//$NON-NLS-2$
											+ ".  Skipping."); //$NON-NLS-1$
								}
							}
							entryStream.close();
						}

						if (extractedFile != null)
							Utils.clear(extractedFile);

						if (options.verbose) {
							System.out.println();
							System.out.println("Processing " + zipFile.getPath()); //$NON-NLS-1$
						}
					}
				}
			}
			File finalFile = new File(getWorkingDirectory(), zipFile.getName());
			if (finalFile.exists())
				finalFile.delete();
			outputFile.renameTo(finalFile);
			Utils.clear(tempDir);
		}

	}

	public static File createSubPathFile(File root, String subPath) throws IOException {
		File result = new File(root, subPath);
		String resultCanonical = result.getCanonicalPath();
		String rootCanonical = root.getCanonicalPath();
		if (!resultCanonical.startsWith(rootCanonical + File.separator) && !resultCanonical.equals(rootCanonical)) {
			throw new IOException("Invalid path: " + subPath); //$NON-NLS-1$
		}
		return result;
	}

	private void initialize(ZipFile zip) {
		ZipEntry entry = zip.getEntry("pack.properties"); //$NON-NLS-1$
		properties = new Properties();
		if (entry != null) {
			try (InputStream stream = zip.getInputStream(entry)) {
				properties.load(stream);
			} catch (IOException e) {
				if (options.verbose)
					e.printStackTrace();
			}
		}

		signExclusions = Utils.getSignExclusions(properties);

		if (executor == null)
			executor = new JarProcessorExecutor();
	}
}
