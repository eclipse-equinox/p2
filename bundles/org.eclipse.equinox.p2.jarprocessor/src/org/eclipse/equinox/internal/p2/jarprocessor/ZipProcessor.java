/*******************************************************************************
 *  Copyright (c) 2006, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
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

/**
 * @author aniefer@ca.ibm.com
 *
 */
public class ZipProcessor {

	private JarProcessorExecutor executor = null;
	private JarProcessorExecutor.Options options = null;

	private String workingDirectory = null;
	private Properties properties = null;
	private Set<String> packExclusions = null;
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

	private boolean repacking() {
		return options.repack || (options.pack && options.signCommand != null);
	}

	public void processZip(File zipFile) throws ZipException, IOException {
		if (options.verbose)
			System.out.println("Processing " + zipFile.getPath()); //$NON-NLS-1$
		ZipFile zip = new ZipFile(zipFile);
		initialize(zip);

		String extension = options.unpack ? "pack.gz" : ".jar"; //$NON-NLS-1$ //$NON-NLS-2$
		File tempDir = new File(getWorkingDirectory(), "temp_" + zipFile.getName()); //$NON-NLS-1$
		JarProcessor processor = new JarProcessor();
		processor.setVerbose(options.verbose);
		processor.setProcessAll(options.processAll);
		processor.setWorkingDirectory(tempDir.getCanonicalPath());
		if (options.unpack) {
			executor.addPackUnpackStep(processor, properties, options);
		}

		File outputFile = new File(getWorkingDirectory(), zipFile.getName() + ".temp"); //$NON-NLS-1$
		File parent = outputFile.getParentFile();
		if (!parent.exists())
			parent.mkdirs();
		ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(outputFile));
		Enumeration<? extends ZipEntry> entries = zip.entries();
		if (entries.hasMoreElements()) {
			for (ZipEntry entry = entries.nextElement(); entry != null; entry = entries.hasMoreElements() ? (ZipEntry) entries.nextElement() : null) {
				String name = entry.getName();

				InputStream entryStream = zip.getInputStream(entry);

				boolean pack = options.pack && !packExclusions.contains(name);
				boolean sign = options.signCommand != null && !signExclusions.contains(name);
				boolean repack = repacking() && !packExclusions.contains(name);

				File extractedFile = null;

				if (entry.getName().endsWith(extension) && (pack || sign || repack || options.unpack)) {
					extractedFile = new File(tempDir, name);
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
						//skipping this file 
						entryStream = new FileInputStream(extractedFile);
						if (options.verbose)
							System.out.println(entry.getName() + " is not marked, skipping."); //$NON-NLS-1$
					} else {
						if (options.unpack) {
							File result = processor.processJar(extractedFile);
							name = name.substring(0, name.length() - extractedFile.getName().length()) + result.getName();
							extractedFile = result;
						} else {
							if (repack || sign) {
								processor.clearProcessSteps();
								if (repack)
									executor.addPackUnpackStep(processor, properties, options);
								if (sign)
									executor.addSignStep(processor, properties, options);
								extractedFile = processor.processJar(extractedFile);
							}
							if (pack) {
								processor.clearProcessSteps();
								executor.addPackStep(processor, properties, options);
								File modifiedFile = processor.processJar(extractedFile);
								if (modifiedFile.exists()) {
									try {
										String newName = name.substring(0, name.length() - extractedFile.getName().length()) + modifiedFile.getName();
										if (options.verbose) {
											System.out.println("Adding " + newName + " to " + outputFile.getPath()); //$NON-NLS-1$ //$NON-NLS-2$
											System.out.println();
										}
										ZipEntry zipEntry = new ZipEntry(newName);
										entryStream = new FileInputStream(modifiedFile);
										zipOut.putNextEntry(zipEntry);
										Utils.transferStreams(entryStream, zipOut, false); //we want to keep zipOut open
										entryStream.close();
										Utils.clear(modifiedFile);
									} catch (IOException e) {
										Utils.close(entryStream);
										if (options.verbose) {
											e.printStackTrace();
											System.out.println("Warning: Problem reading " + modifiedFile.getPath() + "."); //$NON-NLS-1$//$NON-NLS-2$
										}
									}
									entryStream = null;
								} else if (options.verbose) {
									System.out.println("Warning: " + modifiedFile.getPath() + " not found."); //$NON-NLS-1$//$NON-NLS-2$
								}
							}
						}
						if (extractedFile.exists()) {
							try {
								entryStream = new FileInputStream(extractedFile);
							} catch (IOException e) {
								if (options.verbose) {
									e.printStackTrace();
									System.out.println("Warning: Problem reading " + extractedFile.getPath() + "."); //$NON-NLS-1$//$NON-NLS-2$
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
							System.out.println("Warning: " + name + " already exists in " + outputFile.getName() + ".  Skipping."); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
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
		zipOut.close();
		zip.close();

		File finalFile = new File(getWorkingDirectory(), zipFile.getName());
		if (finalFile.exists())
			finalFile.delete();
		outputFile.renameTo(finalFile);
		Utils.clear(tempDir);
	}

	private void initialize(ZipFile zip) {
		ZipEntry entry = zip.getEntry("pack.properties"); //$NON-NLS-1$
		properties = new Properties();
		if (entry != null) {
			InputStream stream = null;
			try {
				stream = zip.getInputStream(entry);
				properties.load(stream);
			} catch (IOException e) {
				if (options.verbose)
					e.printStackTrace();
			} finally {
				Utils.close(stream);
			}
		}

		packExclusions = Utils.getPackExclusions(properties);
		signExclusions = Utils.getSignExclusions(properties);

		if (executor == null)
			executor = new JarProcessorExecutor();
	}
}
